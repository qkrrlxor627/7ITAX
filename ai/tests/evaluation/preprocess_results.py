"""RAG 평가 결과 전처리 — 보고서 작성용 요약 데이터 추출."""
import json
import sys
from pathlib import Path

import pandas as pd

EVAL_DIR = Path(__file__).parent
GOLDEN_SET_PATH = EVAL_DIR / "golden_set.json"
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent
OUTPUT_DIR = PROJECT_ROOT / "docdoc" / "02_ai" / "07_evaluation"

METRICS = ["faithfulness", "answer_relevancy", "context_recall", "context_precision"]
TARGETS = {"faithfulness": 0.95, "context_recall": 0.85, "answer_relevancy": 0.80}


def load_results():
    """CSV 결과 파일 로드."""
    results = {}
    for name in ["no_rag", "plain", "optimized"]:
        path = OUTPUT_DIR / f"{name}_results.csv"
        if path.exists():
            results[name] = pd.read_csv(path)
    return results


def load_golden_set():
    with open(GOLDEN_SET_PATH, encoding="utf-8") as f:
        return json.load(f)


def metric_summary(results: dict):
    """각 config별 메트릭 요약."""
    print("=" * 60)
    print("1. 메트릭 요약 (mean / std / min / max)")
    print("=" * 60)
    for name, df in results.items():
        print(f"\n--- {name} ---")
        for m in METRICS:
            if m not in df.columns:
                continue
            s = df[m].dropna()
            print(f"  {m}: mean={s.mean():.4f}  std={s.std():.4f}  min={s.min():.4f}  max={s.max():.4f}  valid={len(s)}/{len(df)}")


def comparison_table(results: dict):
    """No-RAG vs Plain vs Optimized 비교 테이블."""
    print("\n" + "=" * 60)
    print("2. 비교 테이블")
    print("=" * 60)
    names = list(results.keys())
    header = f"{'metric':<22}" + "".join(f"{n:>12}" for n in names)
    if len(names) >= 2:
        header += f"{'delta':>10}"
    print(header)
    print("-" * len(header))
    for m in METRICS:
        vals = []
        for n in names:
            df = results[n]
            if m in df.columns:
                vals.append(df[m].dropna().mean())
            else:
                vals.append(float("nan"))
        row = f"{m:<22}" + "".join(f"{v:>12.4f}" for v in vals)
        if len(vals) >= 2:
            first, last = vals[0], vals[-1]
            if first > 0:
                delta = (last - first) / first * 100
                row += f"{delta:>+9.1f}%"
        target = TARGETS.get(m)
        if target:
            statuses = ["PASS" if v >= target else "FAIL" for v in vals]
            row += "  " + " → ".join(statuses)
        print(row)


def top_bottom_qa(results: dict, golden_set: list, n: int = 5):
    """메트릭별 상/하위 N개 QA."""
    print("\n" + "=" * 60)
    print(f"3. 상/하위 {n}개 QA (Plain RAG 기준)")
    print("=" * 60)
    if "plain" not in results:
        print("  plain 결과 없음")
        return
    df = results["plain"].copy()
    questions = [item["question"] for item in golden_set]
    if len(questions) != len(df):
        print(f"  WARN: golden_set({len(questions)}) != results({len(df)})")
        return
    df["question"] = questions

    for m in METRICS:
        if m not in df.columns:
            continue
        valid = df[df[m].notna()].copy()
        if valid.empty:
            continue
        print(f"\n--- {m} ---")

        top = valid.nlargest(n, m)
        print(f"  상위 {n}개:")
        for _, row in top.iterrows():
            print(f"    {row[m]:.4f} | {row['question'][:50]}")

        bottom = valid.nsmallest(n, m)
        print(f"  하위 {n}개:")
        for _, row in bottom.iterrows():
            print(f"    {row[m]:.4f} | {row['question'][:50]}")


def per_qa_table(results: dict, golden_set: list):
    """전체 QA별 메트릭 테이블 (부록용)."""
    print("\n" + "=" * 60)
    print("4. 전체 QA별 메트릭 (부록)")
    print("=" * 60)
    for name in ["no_rag", "plain", "optimized"]:
        if name not in results:
            continue
        df = results[name]
        questions = [item["question"] for item in golden_set]
        if len(questions) != len(df):
            continue
        print(f"\n--- {name} ---")
        header = f"{'#':>3} {'question':<40}" + "".join(f"{m[:12]:>13}" for m in METRICS)
        print(header)
        for i, (_, row) in enumerate(df.iterrows()):
            q = questions[i][:38] + ".." if len(questions[i]) > 38 else questions[i]
            vals = "".join(f"{row.get(m, float('nan')):>13.4f}" for m in METRICS)
            print(f"{i+1:>3} {q:<40}{vals}")


def env_info():
    """테스트 환경 정보."""
    print("\n" + "=" * 60)
    print("5. 환경 정보")
    print("=" * 60)
    import platform
    print(f"  Python: {platform.python_version()}")
    print(f"  OS: {platform.system()} {platform.release()}")

    try:
        import ragas
        print(f"  ragas: {ragas.__version__}")
    except Exception:
        print("  ragas: unknown")

    try:
        import chromadb
        print(f"  chromadb: {chromadb.__version__}")
    except Exception:
        print("  chromadb: unknown")

    try:
        import langchain
        print(f"  langchain: {langchain.__version__}")
    except Exception:
        print("  langchain: unknown")

    # config 파일 정보
    for cfg_name in ["no_rag_config.yaml", "plain_config.yaml", "optimized_config.yaml"]:
        cfg_path = EVAL_DIR / cfg_name  # config는 여전히 eval 디렉토리에서 읽음
        if cfg_path.exists():
            print(f"\n  --- {cfg_name} ---")
            print(f"  {cfg_path.read_text(encoding='utf-8').strip()}")


def main():
    results = load_results()
    golden_set = load_golden_set()

    print(f"골든셋: {len(golden_set)}쌍")
    print(f"로드된 결과: {list(results.keys())}")

    metric_summary(results)
    comparison_table(results)
    top_bottom_qa(results, golden_set)
    per_qa_table(results, golden_set)
    env_info()


if __name__ == "__main__":
    main()
