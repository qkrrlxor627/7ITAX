#!/usr/bin/env bash
# RAG 평가 파이프라인 실행 스크립트
# 사용법:
#   bash ai/tests/evaluation/run_eval.sh          # plain + optimized 모두 실행 후 비교
#   bash ai/tests/evaluation/run_eval.sh plain     # plain만 실행
#   bash ai/tests/evaluation/run_eval.sh optimized # optimized만 실행
#   bash ai/tests/evaluation/run_eval.sh no_rag    # No-RAG (LLM만) 실행
#   bash ai/tests/evaluation/run_eval.sh all       # no_rag + plain + optimized 실행 후 비교
#   bash ai/tests/evaluation/run_eval.sh both      # plain + optimized 실행 후 비교

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -W 2>/dev/null || pwd)"
AI_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -W 2>/dev/null || pwd)"
PROJECT_ROOT="$(cd "${AI_ROOT}/.." && pwd -W 2>/dev/null || pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/docdoc/02_ai/07_evaluation"
MODE="${1:-both}"

echo "====================================="
echo " tax7i RAG 평가 파이프라인 (RAGAS)"
echo " 모드: ${MODE}"
echo "====================================="

# 골든셋 존재 확인
GOLDEN_SET="${SCRIPT_DIR}/golden_set.json"
if [ ! -f "${GOLDEN_SET}" ]; then
    echo "[ERROR] golden_set.json 이 없습니다: ${GOLDEN_SET}"
    exit 1
fi
QA_COUNT=$(python -c "import json; print(len(json.load(open('${GOLDEN_SET}', encoding='utf-8'))))")
echo "[INFO] 골든셋 QA 수: ${QA_COUNT}쌍"

# RAGAS 설치 확인
python -c "import ragas" 2>/dev/null || {
    echo "[INFO] ragas 패키지 설치 중..."
    pip install ragas datasets pyyaml pandas -q
}

# --- 평가 실행 함수 ---
run_eval() {
    local config_file=$1
    local label=$2
    echo ""
    echo "[INFO] ${label} 평가 시작 (config: ${config_file})..."
    cd "${AI_ROOT}"
    python tests/evaluation/test_ragas_eval.py --config "${config_file}"
}

# --- 비교 출력 함수 ---
compare_results() {
    local compare_mode=$1   # "both" 또는 "all"
    echo ""
    echo "========================================="
    echo " RAG 평가 비교 결과"
    echo "========================================="
    cd "${AI_ROOT}"
    python -c "
import sys
from pathlib import Path
import pandas as pd

eval_dir = Path('${OUTPUT_DIR}')
configs = {}

no_rag_path = eval_dir / 'no_rag_results.csv'
plain_path  = eval_dir / 'plain_results.csv'
opt_path    = eval_dir / 'optimized_results.csv'

if plain_path.exists():
    configs['Plain RAG'] = pd.read_csv(plain_path)
if opt_path.exists():
    configs['Optimized'] = pd.read_csv(opt_path)
if '${compare_mode}' == 'all' and no_rag_path.exists():
    configs['No-RAG'] = pd.read_csv(no_rag_path)

if len(configs) < 2:
    print('[WARN] 비교 대상 CSV 파일이 부족합니다.')
    sys.exit(0)

metrics = ['faithfulness', 'answer_relevancy', 'context_recall', 'context_precision']
targets = {'faithfulness': 0.95, 'context_recall': 0.85, 'answer_relevancy': 0.80}

names = list(configs.keys())
header = f\"{'지표':<22}\" + ''.join(f'{n:>12}' for n in names)
print(header)
print('-' * (22 + 12 * len(names)))
for m in metrics:
    vals = []
    for n in names:
        df = configs[n]
        if m in df.columns:
            vals.append(df[m].mean())
        else:
            vals.append(float('nan'))
    row = f'{m:<22}'
    for v in vals:
        row += f'{v:>12.4f}'
    target = targets.get(m)
    if target:
        statuses = []
        for v in vals:
            statuses.append('PASS' if v >= target else 'FAIL')
        row += '  ' + ' -> '.join(statuses)
    print(row)
"
}

# --- 모드별 실행 ---
if [[ "$MODE" == "no_rag" || "$MODE" == "all" ]]; then
    run_eval "no_rag_config.yaml" "No-RAG (LLM only)"
fi

if [[ "$MODE" == "plain" || "$MODE" == "both" || "$MODE" == "all" ]]; then
    run_eval "plain_config.yaml" "Plain RAG"
fi

if [[ "$MODE" == "optimized" || "$MODE" == "both" || "$MODE" == "all" ]]; then
    run_eval "optimized_config.yaml" "Optimized RAG"
fi

if [[ "$MODE" == "both" ]]; then
    compare_results "both"
fi

if [[ "$MODE" == "all" ]]; then
    compare_results "all"
fi

echo ""
echo "[INFO] 완료."
