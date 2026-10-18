"""부하 테스트 중 BE 서버 지표를 주기적으로 기록한다(/actuator/prometheus). k6는 클라이언트 쪽 수치만 보므로 병목 판단용.

사용: python scripts/perf/sample_metrics.py [BE 주소] [초 간격] [최대 초]
     기본 http://localhost:8080, 5초, 1800초. Ctrl+C 또는 최대 시간에 끝나며 마지막에 요약(최대값)을 출력한다
기록: scripts/perf/results/metrics-<시각>.csv
"""
import csv
import os
import re
import sys
import time
import urllib.request

# (CSV 열 이름, Prometheus 지표 이름, 라벨 필터 — 여러 행이면 합산)
METRICS = [
    ("hikari_active", "hikaricp_connections_active", ""),
    ("hikari_pending", "hikaricp_connections_pending", ""),
    ("hikari_max", "hikaricp_connections_max", ""),
    ("tomcat_busy", "tomcat_threads_busy_threads", ""),
    ("heap_used_mb", "jvm_memory_used_bytes", 'area="heap"'),
    ("cpu_process", "process_cpu_usage", ""),
    ("cpu_system", "system_cpu_usage", ""),
    ("gc_pause_s_total", "jvm_gc_pause_seconds_sum", ""),
    ("async_active", "executor_active_threads", ""),
    ("async_queued", "executor_queued_tasks", ""),
]


def scrape(base):
    with urllib.request.urlopen(base + "/actuator/prometheus", timeout=5) as res:
        text = res.read().decode("utf-8")
    row = {}
    for col, name, label in METRICS:
        total, seen = 0.0, False
        for m in re.finditer(r"^" + name + r"(\{[^}]*\})?\s+([0-9eE.+-]+)$", text, re.M):
            if label and label not in (m.group(1) or ""):
                continue
            total += float(m.group(2))
            seen = True
        if not seen:
            row[col] = ""
        elif col == "heap_used_mb":
            row[col] = round(total / 1024 / 1024, 1)
        elif col.startswith("cpu_"):
            row[col] = round(total * 100, 1)  # %
        else:
            row[col] = round(total, 3)
    return row


def main():
    base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
    interval = float(sys.argv[2]) if len(sys.argv) > 2 else 5
    limit = float(sys.argv[3]) if len(sys.argv) > 3 else 1800
    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, time.strftime("metrics-%Y%m%d-%H%M%S.csv"))
    cols = ["t"] + [c for c, _, _ in METRICS]
    peaks = {}
    start = time.time()
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=cols)
        w.writeheader()
        try:
            while time.time() - start < limit:
                try:
                    row = scrape(base)
                except Exception as e:  # 과부하로 actuator 응답이 늦어도 기록은 계속
                    print(f"scrape 실패: {e}", file=sys.stderr)
                    time.sleep(interval)
                    continue
                row["t"] = round(time.time() - start)
                w.writerow(row)
                f.flush()
                for k, v in row.items():
                    if k != "t" and v != "":
                        peaks[k] = max(peaks.get(k, v), v)
                time.sleep(interval)
        except KeyboardInterrupt:
            pass
    print(f"기록: {path}")
    print("최대값: " + ", ".join(f"{k}={v}" for k, v in peaks.items()))


if __name__ == "__main__":
    main()
