# ai/tests/evaluation/analyze_comparison.py
"""Plain vs Optimized RAG 평가 결과 비교 분석 스크립트.

사용법:
    cd ai
    python tests/evaluation/analyze_comparison.py
"""

import sys
from pathlib import Path

import pandas as pd

EVAL_DIR = Path(__file__).parent
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent
OUTPUT_DIR = PROJECT_ROOT / "docdoc" / "02_ai" / "07_evaluation"


def main():
    plain_path = OUTPUT_DIR / "plain_results.csv"
    opt_path = OUTPUT_DIR / "optimized_results.csv"

    if not plain_path.exists():
        print(f"[ERROR] {plain_path} 없음. 먼저 평가를 실행하세요:")
        print("  bash ai/tests/evaluation/run_eval.sh plain")
        sys.exit(1)
    if not opt_path.exists():
        print(f"[ERROR] {opt_path} 없음. 먼저 평가를 실행하세요:")
        print("  bash ai/tests/evaluation/run_eval.sh optimized")
        sys.exit(1)

    plain = pd.read_csv(plain_path)
    optimized = pd.read_csv(opt_path)

    metrics = ["faithfulness", "answer_relevancy", "context_recall", "context_precision"]
    targets = {"faithfulness": 0.95, "context_recall": 0.85, "answer_relevancy": 0.80}

    print("=" * 70)
    print("Plain RAG vs Optimized RAG - 지표 비교")
    print("=" * 70)

    for m in metrics:
        if m not in plain.columns or m not in optimized.columns:
            print(f"[WARN] {m} 컬럼 없음. 스킵.")
            continue
        p_val = plain[m].mean()
        o_val = optimized[m].mean()
        if pd.isna(p_val) or pd.isna(o_val):
            print(f"[{m}]")
            print(f"  Plain:     N/A")
            print(f"  Optimized: N/A")
            print()
            continue
        diff = o_val - p_val
        pct = (diff / p_val * 100) if p_val > 0 else 0
        target = targets.get(m)
        p_status = ("PASS" if p_val >= target else "FAIL") if target else "-"
        o_status = ("PASS" if o_val >= target else "FAIL") if target else "-"
        print(f"[{m}]")
        print(f"  Plain:     {p_val:.4f}  ({p_status})")
        print(f"  Optimized: {o_val:.4f}  ({o_status})")
        print(f"  변화: {diff:+.4f} ({pct:+.1f}%)")
        print()

    # 하위 성능 질문 파악 (컬럼명: user_input)
    q_col = "user_input" if "user_input" in optimized.columns else "question"
    if "faithfulness" in optimized.columns and q_col in optimized.columns:
        print("=== Faithfulness 하위 5개 (optimized) ===")
        print(optimized.nsmallest(5, "faithfulness")[[q_col, "faithfulness"]].to_string())

    if "context_recall" in optimized.columns and q_col in optimized.columns:
        print("\n=== Context Recall 하위 5개 (optimized) ===")
        print(optimized.nsmallest(5, "context_recall")[[q_col, "context_recall"]].to_string())

    # 목표치 달성 요약
    print("\n" + "=" * 70)
    print("목표치 달성 요약")
    print("=" * 70)
    for m, target in targets.items():
        if m not in plain.columns:
            continue
        p_val = plain[m].mean()
        o_val = optimized[m].mean()
        print(
            f"{m:<25} 목표 ≥ {target:.2f}  "
            f"Plain: {'PASS' if p_val >= target else 'FAIL'} ({p_val:.4f})  "
            f"Optimized: {'PASS' if o_val >= target else 'FAIL'} ({o_val:.4f})"
        )


if __name__ == "__main__":
    main()
