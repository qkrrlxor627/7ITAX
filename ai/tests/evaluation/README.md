# RAG 평가 파이프라인 (RAGAS)

세법 QA 골든셋 기반 RAG 성능을 RAGAS 프레임워크로 측정한다.

## 빠른 시작

```bash
cd ai

# Plain + Optimized 비교 (기본)
bash tests/evaluation/run_eval.sh

# 개별 실행
bash tests/evaluation/run_eval.sh plain       # Plain RAG
bash tests/evaluation/run_eval.sh optimized   # Optimized RAG
bash tests/evaluation/run_eval.sh no_rag      # LLM만 (검색 없음)

# 전체 비교 (no_rag + plain + optimized)
bash tests/evaluation/run_eval.sh all
```

Python 직접 실행:

```bash
cd ai
python tests/evaluation/test_ragas_eval.py --config plain_config.yaml
python tests/evaluation/test_ragas_eval.py --config optimized_config.yaml
python tests/evaluation/test_ragas_eval.py --config no_rag_config.yaml
```

## 전제 조건

- `ai/` 디렉토리에서 실행
- GMS API 키 등 환경변수 설정 완료
- 필요 패키지: `ragas`, `datasets`, `pyyaml`, `pandas` (`run_eval.sh`가 자동 설치)

## 파일 구성

| 파일 | 역할 |
|------|------|
| `golden_set.json` | 26개 세법 QA 쌍 (질문 + 정답 + 근거 조문) |
| `plain_config.yaml` | 기본 설정 (top_k=5, temp=0.7) |
| `optimized_config.yaml` | 튜닝 설정 (top_k=7, temp=0.3) |
| `no_rag_config.yaml` | RAG 미사용, LLM만 |
| `test_ragas_eval.py` | RAGAS 평가 실행 스크립트 |
| `run_eval.sh` | 실행 + 비교 래퍼 스크립트 |
| `analyze_comparison.py` | 결과 비교 분석 |
| `preprocess_results.py` | 결과 전처리 |

## 측정 메트릭

| 메트릭 | 의미 | 목표 |
|--------|------|------|
| Faithfulness | 답변이 검색 문서에 기반하는지 | ≥ 0.95 |
| Answer Relevancy | 답변이 질문에 관련되는지 | ≥ 0.80 |
| Context Recall | 필요한 문서를 검색했는지 | ≥ 0.85 |
| Context Precision | 검색 문서의 정밀도 | — |

## 설정 커스터마이징

YAML config 파일을 복사하여 파라미터를 변경할 수 있다.

```yaml
name: my_experiment        # 결과 CSV 파일명에 사용

use_rag: true              # false로 설정 시 LLM만 사용
retrieval_top_k: 5         # 검색 문서 수
search_strategy: hybrid    # hybrid | vector | bm25
rrf_k: 60                  # RRF 파라미터
temperature: 0.7           # LLM temperature
```

```bash
python tests/evaluation/test_ragas_eval.py --config my_experiment_config.yaml
```

## 결과 저장 위치

`docdoc/02_ai/07_evaluation/{config_name}_results.csv`
