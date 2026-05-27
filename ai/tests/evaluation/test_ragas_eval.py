# ai/tests/evaluation/test_ragas_eval.py
"""RAGAS 기반 RAG 평가 스크립트.

사용법:
    cd ai
    python tests/evaluation/test_ragas_eval.py --config plain_config.yaml
    python tests/evaluation/test_ragas_eval.py --config optimized_config.yaml
"""

import argparse
import asyncio
import json
import os
import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import settings

# RAGAS가 LLM-as-judge를 위해 OpenAI API를 사용할 때 GMS 프록시를 사용하도록 환경변수 주입
# (RAGAS는 내부적으로 langchain ChatOpenAI를 사용하며, OPENAI_API_KEY와 OPENAI_BASE_URL을 참조)
os.environ["OPENAI_API_KEY"] = settings.gms_api_key
os.environ["OPENAI_BASE_URL"] = settings.gms_base_url

from datasets import Dataset
from ragas import evaluate
from ragas.metrics import (
    answer_relevancy,
    context_precision,
    context_recall,
    faithfulness,
)

from app.core.prompts import build_intent_prompt
from app.services.embedding_service import EmbeddingService
from app.services.intent_classifier import IntentClassifier
from app.services.reranker_service import RerankerService
from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.vectorstore import VectorStoreService
from app.utils.query_rewriter import QueryRewriter
from app.utils.text_utils import format_search_results

GOLDEN_SET_PATH = Path(__file__).parent / "golden_set.json"
EVAL_DIR = Path(__file__).parent
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent
OUTPUT_DIR = PROJECT_ROOT / "docdoc" / "02_ai" / "07_evaluation"
INTENTS_PATH = "app/data/intents/tax_intents.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="RAGAS RAG 평가 스크립트")
    parser.add_argument(
        "--config",
        type=str,
        default="plain_config.yaml",
        help="RAG config YAML 파일명 (ai/tests/evaluation/ 기준)",
    )
    return parser.parse_args()


def load_config(config_file: str) -> dict:
    config_path = EVAL_DIR / config_file
    with open(config_path, encoding="utf-8") as f:
        return yaml.safe_load(f)


def load_golden_set(path: Path) -> list[dict]:
    with open(path, encoding="utf-8") as f:
        return json.load(f)


async def run_rag_pipeline(
    retrieval_service: RetrievalService | None,
    intent_classifier: IntentClassifier | None,
    question: str,
    config: dict,
    llm,
) -> dict:
    """단일 질문에 대해 RAG 파이프라인 실행. 반환: {"answer": str, "contexts": list[str]}

    프로덕션(ChatService)과 동일한 흐름:
    1. IntentClassifier로 인텐트 분류
    2. 인텐트 기반 검색 (metadata_filter, search_strategy)
    3. format_search_results()로 컨텍스트 포맷팅
    4. build_intent_prompt()로 시스템 프롬프트 생성
    5. SystemMessage + HumanMessage로 LLM 호출
    """
    from langchain_core.messages import HumanMessage, SystemMessage

    use_rag = config.get("use_rag", True)

    # 1. 인텐트 분류
    intent_name = "GENERAL"
    search_strategy = config.get("search_strategy", "hybrid")
    metadata_filter = None
    if intent_classifier is not None:
        intent_result = await intent_classifier.classify(question)
        intent_name = intent_result.intent
        search_strategy = intent_result.search_strategy
        metadata_filter = intent_result.metadata_filter or None

    if use_rag and retrieval_service is not None:
        # 2. 쿼리 변환 + 인텐트 기반 검색 (프로덕션 ChatService와 동일)
        query_rewriter = QueryRewriter()
        search_query = await query_rewriter.rewrite(question)
        results = await retrieval_service.retrieve(
            query=search_query,
            top_k=config.get("retrieval_top_k", 5),
            search_strategy=search_strategy,
            metadata_filter=metadata_filter,
        )
        contexts = [r.content for r in results]
        # 3. 프로덕션과 동일한 포맷팅
        context_text = format_search_results(results)
    else:
        contexts = [""]
        context_text = ""

    # 4. 프로덕션과 동일한 프롬프트 생성
    system_prompt = build_intent_prompt(
        intent_name=intent_name,
        context=context_text,
    )

    # 5. LLM 호출
    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=question),
    ]

    response = await llm.ainvoke(messages)
    return {"answer": response.content, "contexts": contexts}


async def build_ragas_dataset(
    golden_set: list[dict],
    retrieval_service: RetrievalService,
    intent_classifier: IntentClassifier | None,
    config: dict,
    llm,
) -> Dataset:
    """golden_set + RAG 응답을 결합해 RAGAS Dataset 생성."""
    questions, answers, contexts, ground_truths = [], [], [], []

    for i, item in enumerate(golden_set):
        q = item["question"]
        print(f"  [{i+1}/{len(golden_set)}] {q[:40]}...")
        rag_result = await run_rag_pipeline(
            retrieval_service, intent_classifier, q, config, llm
        )
        questions.append(q)
        answers.append(rag_result["answer"])
        contexts.append(rag_result["contexts"])
        ground_truths.append(item["ground_truth"])

    return Dataset.from_dict(
        {
            "question": questions,
            "answer": answers,
            "contexts": contexts,
            "ground_truth": ground_truths,
        }
    )


def run_evaluation(dataset: Dataset, llm=None, embeddings=None):
    return evaluate(
        dataset,
        metrics=[faithfulness, answer_relevancy, context_recall, context_precision],
        llm=llm,
        embeddings=embeddings,
    )


async def main():
    args = parse_args()
    config = load_config(args.config)
    config_name = config.get("name", Path(args.config).stem)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    results_path = OUTPUT_DIR / f"{config_name}_results.csv"

    golden_set = load_golden_set(GOLDEN_SET_PATH)
    use_rag = config.get("use_rag", True)
    print(f"[INFO] config: {config_name}")
    print(f"[INFO] RAG 사용: {use_rag}")
    print(f"[INFO] 골든셋 로드: {len(golden_set)}쌍")
    print(f"[INFO] Judge 모델: gpt-4.1-mini (temperature=0.0)")
    if use_rag:
        print(
            f"[INFO] 파라미터: top_k={config.get('retrieval_top_k', 5)}, "
            f"strategy={config.get('search_strategy', 'hybrid')}, "
            f"rrf_k={config.get('rrf_k', 60)}, "
            f"temperature={config.get('temperature', 0.7)}"
        )

    # RAG 서비스 초기화 (use_rag=false이면 건너뜀)
    retrieval_service = None
    intent_classifier = None
    if use_rag:
        vectorstore = VectorStoreService(settings)
        bm25_index = BM25Index()
        reranker = None
        try:
            reranker = RerankerService()
            print("[INFO] RerankerService 초기화 완료")
        except Exception as e:
            print(f"[WARN] RerankerService 초기화 실패 (Reranker 비활성): {e}")

        retrieval_service = RetrievalService(
            vectorstore_service=vectorstore,
            bm25_index=bm25_index,
            reranker=reranker,
        )
        if "rrf_k" in config:
            retrieval_service.RRF_K = config["rrf_k"]

        # IntentClassifier 초기화 (프로덕션과 동일)
        embedding_service = EmbeddingService(settings)
        intent_classifier = IntentClassifier(INTENTS_PATH, embedding_service)
        await intent_classifier.initialize()
        print("[INFO] IntentClassifier 초기화 완료")

    # LLM 초기화 (GMS 프록시 사용)
    from langchain_openai import ChatOpenAI, OpenAIEmbeddings

    llm = ChatOpenAI(
        base_url=settings.gms_base_url,
        api_key=settings.gms_api_key,
        model=settings.llm_model_standard,
        temperature=config.get("temperature", 0.0),
    )

    # RAGAS judge LLM — gpt-4.1-mini로 분리 (답변 생성 LLM과 독립)
    judge_llm = ChatOpenAI(
        base_url=settings.gms_base_url,
        api_key=settings.gms_api_key,
        model="gpt-4.1-mini",
        temperature=0.0,
    )

    # RAGAS answer_relevancy 메트릭이 임베딩을 필요로 함 — GMS 프록시 경유
    embeddings = OpenAIEmbeddings(
        model=settings.embedding_model,
        openai_api_key=settings.gms_api_key,
        openai_api_base=settings.gms_base_url,
    )

    print("[INFO] RAG 파이프라인 실행 중...")
    dataset = await build_ragas_dataset(
        golden_set, retrieval_service, intent_classifier, config, llm
    )

    print("[INFO] RAGAS 평가 실행 중...")
    result = run_evaluation(dataset, llm=judge_llm, embeddings=embeddings)

    df = result.to_pandas()
    print(f"\n===== RAGAS 평가 결과 ({config_name}) =====")
    print(f"Faithfulness:      {df['faithfulness'].mean():.4f}  (목표 ≥ 0.95)")
    print(f"Answer Relevancy:  {df['answer_relevancy'].mean():.4f}  (목표 ≥ 0.80)")
    print(f"Context Recall:    {df['context_recall'].mean():.4f}  (목표 ≥ 0.85)")
    print(f"Context Precision: {df['context_precision'].mean():.4f}")

    print("\n===== 목표치 달성 여부 =====")
    print(
        f"Faithfulness >= 0.95:     {'PASS' if df['faithfulness'].mean() >= 0.95 else 'FAIL'}"
    )
    print(
        f"Context Recall >= 0.85:   {'PASS' if df['context_recall'].mean() >= 0.85 else 'FAIL'}"
    )
    print(
        f"Answer Relevancy >= 0.80: {'PASS' if df['answer_relevancy'].mean() >= 0.80 else 'FAIL'}"
    )

    df.to_csv(results_path, index=False, encoding="utf-8-sig")
    print(f"\n[INFO] 결과 저장: {results_path}")


if __name__ == "__main__":
    asyncio.run(main())
