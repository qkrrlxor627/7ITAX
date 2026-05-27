"""Batch 7 평가 결과 분석 스크립트.

50쌍 결과 CSV를 읽어 보고서 작성에 필요한 핵심 메트릭만 추출한다.
context size 절약을 위해 이 스크립트 출력만으로 보고서를 작성할 수 있도록 설계.

사용법:
    cd ai && python tests/evaluation/analyze_batch7.py
"""

import json
import sys
from pathlib import Path

import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent
OUTPUT_DIR = PROJECT_ROOT / "docdoc" / "02_ai" / "07_evaluation"
GOLDEN_SET_PATH = Path(__file__).parent / "golden_set.json"

METRICS = ["faithfulness", "answer_relevancy", "context_recall", "context_precision"]

# Phase 1~3 결과 (기존 보고서 기준, gpt-4.1-nano judge)
PREV_RESULTS = {
    "No-RAG (nano)": {"faithfulness": 0.7264, "answer_relevancy": 0.6543, "context_recall": None, "context_precision": None},
    "Phase1 (nano)": {"faithfulness": 0.8059, "answer_relevancy": 0.7146, "context_recall": 0.9815, "context_precision": 0.0300},
    "Phase2 (nano)": {"faithfulness": 0.7195, "answer_relevancy": 0.4565, "context_recall": 1.0000, "context_precision": 0.0219},
    "Phase3 (nano)": {"faithfulness": 0.6494, "answer_relevancy": 0.2641, "context_recall": 0.9815, "context_precision": 0.0382},
}


def load_golden_set() -> list[dict]:
    with open(GOLDEN_SET_PATH, encoding="utf-8") as f:
        return json.load(f)


def load_result(name: str) -> pd.DataFrame | None:
    path = OUTPUT_DIR / f"{name}_results.csv"
    if path.exists():
        return pd.read_csv(path)
    return None


def section_overall(results: dict[str, pd.DataFrame]):
    """전체 메트릭 비교표 출력."""
    print("=" * 70)
    print("1. 전체 메트릭 비교 (50쌍, gpt-4.1-mini judge, temperature=0.0)")
    print("=" * 70)

    header = f"{'메트릭':<22}" + "".join(f"{n:>14}" for n in results)
    print(header)
    print("-" * (22 + 14 * len(results)))

    for m in METRICS:
        row = f"{m:<22}"
        for name, df in results.items():
            if m in df.columns:
                val = df[m].mean()
                row += f"{val:>14.4f}"
            else:
                row += f"{'N/A':>14}"
        print(row)

    # 표준편차
    print()
    print("표준편차:")
    for m in METRICS:
        row = f"  {m:<20}"
        for name, df in results.items():
            if m in df.columns:
                row += f"{df[m].std():>14.4f}"
            else:
                row += f"{'N/A':>14}"
        print(row)


def section_vs_previous(results: dict[str, pd.DataFrame]):
    """이전 Phase 대비 비교표."""
    print()
    print("=" * 70)
    print("2. 이전 Phase(27쌍, nano judge) vs Batch 7(50쌍, mini judge) 비교")
    print("=" * 70)
    print("  ※ judge 모델/샘플 수가 다르므로 직접 비교는 참고용")
    print()

    opt = results.get("optimized")
    if opt is None:
        print("  [SKIP] optimized 결과 없음")
        return

    # 27쌍 서브셋 (인덱스 0~26)
    opt_27 = opt.iloc[:27] if len(opt) >= 27 else opt

    print(f"{'메트릭':<22}{'P3(nano,27)':>14}{'B7(mini,50)':>14}{'B7(mini,27서브셋)':>18}{'Δ(B7-P3)':>12}")
    print("-" * 80)
    for m in METRICS:
        p3 = PREV_RESULTS["Phase3 (nano)"].get(m)
        b7_50 = opt[m].mean() if m in opt.columns else None
        b7_27 = opt_27[m].mean() if m in opt_27.columns else None

        p3_str = f"{p3:.4f}" if p3 is not None else "N/A"
        b7_50_str = f"{b7_50:.4f}" if b7_50 is not None else "N/A"
        b7_27_str = f"{b7_27:.4f}" if b7_27 is not None else "N/A"
        delta = f"{b7_50 - p3:+.4f}" if (p3 is not None and b7_50 is not None) else "N/A"

        print(f"{m:<22}{p3_str:>14}{b7_50_str:>14}{b7_27_str:>18}{delta:>12}")


def section_category_difficulty(results: dict[str, pd.DataFrame]):
    """카테고리/난이도별 분석."""
    golden = load_golden_set()

    print()
    print("=" * 70)
    print("3. 카테고리별/난이도별 Faithfulness 분석 (Optimized 설정)")
    print("=" * 70)

    opt = results.get("optimized")
    if opt is None or len(opt) != len(golden):
        print(f"  [SKIP] optimized 결과({len(opt) if opt is not None else 0}) != golden set({len(golden)})")
        return

    # golden set에서 category, difficulty 매핑
    opt = opt.copy()
    opt["category"] = [g["category"] for g in golden]
    opt["difficulty"] = [g["difficulty"] for g in golden]

    for group_col, label in [("category", "카테고리"), ("difficulty", "난이도")]:
        print(f"\n--- {label}별 ---")
        grouped = opt.groupby(group_col)
        header = f"  {label:<16}{'N':>4}" + "".join(f"{m:>18}" for m in METRICS)
        print(header)
        print("  " + "-" * (16 + 4 + 18 * len(METRICS)))
        for name, group in sorted(grouped):
            row = f"  {name:<16}{len(group):>4}"
            for m in METRICS:
                if m in group.columns:
                    row += f"{group[m].mean():>18.4f}"
                else:
                    row += f"{'N/A':>18}"
            print(row)


def section_ar_zero(results: dict[str, pd.DataFrame]):
    """AR=0 비율 분석 (Judge LLM 안정성 지표)."""
    print()
    print("=" * 70)
    print("4. Answer Relevancy = 0.0 비율 (Judge LLM 안정성)")
    print("=" * 70)

    for name, df in results.items():
        if "answer_relevancy" in df.columns:
            ar_zero = (df["answer_relevancy"] == 0.0).sum()
            total = len(df)
            print(f"  {name:<16}: {ar_zero}/{total} ({ar_zero/total*100:.1f}%)")


def section_worst_qa(results: dict[str, pd.DataFrame]):
    """Faithfulness 최저 QA 목록."""
    golden = load_golden_set()

    print()
    print("=" * 70)
    print("5. Faithfulness 최저 QA (Optimized, 하위 10개)")
    print("=" * 70)

    opt = results.get("optimized")
    if opt is None or len(opt) != len(golden):
        print("  [SKIP]")
        return

    opt = opt.copy()
    opt["question"] = [g["question"][:40] for g in golden]
    opt["category"] = [g["category"] for g in golden]
    opt["difficulty"] = [g["difficulty"] for g in golden]
    opt["qa_index"] = range(1, len(opt) + 1)

    worst = opt.nsmallest(10, "faithfulness")
    for _, row in worst.iterrows():
        print(
            f"  #{row['qa_index']:>2} [{row['category']:<12} {row['difficulty']:<6}] "
            f"Faith={row['faithfulness']:.2f} AR={row['answer_relevancy']:.2f} "
            f"CR={row['context_recall']:.2f} CP={row['context_precision']:.2f} "
            f"| {row['question']}"
        )


def section_best_qa(results: dict[str, pd.DataFrame]):
    """Faithfulness 최고 QA 목록."""
    golden = load_golden_set()

    print()
    print("=" * 70)
    print("6. Faithfulness 최고 QA (Optimized, 상위 10개)")
    print("=" * 70)

    opt = results.get("optimized")
    if opt is None or len(opt) != len(golden):
        print("  [SKIP]")
        return

    opt = opt.copy()
    opt["question"] = [g["question"][:40] for g in golden]
    opt["category"] = [g["category"] for g in golden]
    opt["difficulty"] = [g["difficulty"] for g in golden]
    opt["qa_index"] = range(1, len(opt) + 1)

    best = opt.nlargest(10, "faithfulness")
    for _, row in best.iterrows():
        print(
            f"  #{row['qa_index']:>2} [{row['category']:<12} {row['difficulty']:<6}] "
            f"Faith={row['faithfulness']:.2f} AR={row['answer_relevancy']:.2f} "
            f"CR={row['context_recall']:.2f} CP={row['context_precision']:.2f} "
            f"| {row['question']}"
        )


def section_goal_check(results: dict[str, pd.DataFrame]):
    """목표 달성 여부."""
    print()
    print("=" * 70)
    print("7. 목표 달성 여부")
    print("=" * 70)

    no_rag = results.get("no_rag")
    opt = results.get("optimized")

    if opt is None:
        print("  [SKIP]")
        return

    no_rag_faith = no_rag["faithfulness"].mean() if no_rag is not None else None
    opt_faith = opt["faithfulness"].mean()
    opt_ar = opt["answer_relevancy"].mean()
    opt_cp = opt["context_precision"].mean()

    goals = [
        ("RAG Faith > No-RAG", opt_faith > no_rag_faith if no_rag_faith else None,
         f"{opt_faith:.4f} vs {no_rag_faith:.4f}" if no_rag_faith else "N/A"),
        ("Answer Relevancy >= 0.50", opt_ar >= 0.50, f"{opt_ar:.4f}"),
        ("Context Precision 개선", opt_cp >= 0.04, f"{opt_cp:.4f} (Phase3: 0.0382)"),
    ]

    for name, passed, detail in goals:
        status = "PASS" if passed else ("FAIL" if passed is not None else "N/A")
        print(f"  {status:>4} | {name:<30} | {detail}")


def main():
    # 결과 로드
    results = {}
    for name in ["no_rag", "plain", "optimized"]:
        df = load_result(name)
        if df is not None:
            results[name] = df
            print(f"[INFO] {name}: {len(df)}행 로드")
        else:
            print(f"[WARN] {name}_results.csv 없음")

    if not results:
        print("[ERROR] 결과 CSV 없음. 평가를 먼저 실행하세요.")
        sys.exit(1)

    section_overall(results)
    section_vs_previous(results)
    section_category_difficulty(results)
    section_ar_zero(results)
    section_worst_qa(results)
    section_best_qa(results)
    section_goal_check(results)


if __name__ == "__main__":
    main()
