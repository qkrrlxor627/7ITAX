"""세법 PDF 문서를 ChromaDB에 인덱싱하는 스크립트.

사용법:
    python -m app.scripts.index_documents          # 인덱싱
    python -m app.scripts.index_documents --force   # 기존 컬렉션 삭제 후 재인덱싱

`init_services()`는 ChromaDB가 비어 있으면 `run_indexing()`을 자동 호출한다.
"""

import argparse
import logging
import time
from collections import Counter

from app.core.config import settings
from app.services.document_processor import DocumentProcessor
from app.services.vectorstore import VectorStoreService

logger = logging.getLogger(__name__)

BATCH_SIZE = 500


def run_indexing(vectorstore_service: VectorStoreService) -> int:
    """세법 PDF 문서를 처리해 전달된 벡터 저장소에 인덱싱한다.

    전달된 `vectorstore_service` 인스턴스를 그대로 사용한다(임베딩 모델 재로드 방지).
    CLI(main)와 `init_services()`의 자동 인덱싱이 이 함수를 공유한다.

    Args:
        vectorstore_service: 초기화된 VectorStoreService 인스턴스.

    Returns:
        인덱싱된 청크 수(처리할 문서가 없으면 0).
    """
    logger.info("문서 처리를 시작합니다.")
    start_time = time.time()

    processor = DocumentProcessor()
    chunks = processor.process_all()

    if not chunks:
        logger.warning("처리된 청크가 없습니다. 인덱싱을 중단합니다.")
        return 0

    # 중복 chunk_id 제거 (첫 번째 우선)
    seen_ids: set[str] = set()
    deduped: list = []
    for chunk in chunks:
        cid = chunk.metadata["chunk_id"]
        if cid not in seen_ids:
            seen_ids.add(cid)
            deduped.append(chunk)
    if len(deduped) < len(chunks):
        logger.warning("중복 chunk_id 제거: %d개 → %d개", len(chunks), len(deduped))
    chunks = deduped

    texts = [chunk.content for chunk in chunks]
    metadatas = [chunk.metadata for chunk in chunks]
    ids = [chunk.metadata["chunk_id"] for chunk in chunks]

    total = len(chunks)
    logger.info("벡터 저장소에 %d개 청크를 %d개씩 배치 저장합니다.", total, BATCH_SIZE)

    for i in range(0, total, BATCH_SIZE):
        batch_end = min(i + BATCH_SIZE, total)
        vectorstore_service.add_documents(
            texts=texts[i:batch_end],
            metadatas=metadatas[i:batch_end],
            ids=ids[i:batch_end],
        )
        logger.info("  배치 저장 완료: %d / %d", batch_end, total)

    elapsed = time.time() - start_time

    law_name_counter: Counter[str] = Counter(
        chunk.metadata.get("law_name", "unknown") for chunk in chunks
    )
    logger.info("=" * 50)
    logger.info("인덱싱 완료")
    logger.info("  총 PDF 수      : %d", len(law_name_counter))
    logger.info("  총 청크 수     : %d", total)
    logger.info("  법률별 청크 수 :")
    for law_name, count in sorted(law_name_counter.items()):
        logger.info("    %-30s : %d", law_name, count)
    logger.info("  소요 시간      : %.2f초", elapsed)
    logger.info("=" * 50)
    return total


def main() -> None:
    """CLI 진입점. --force 옵션을 지원한다."""
    parser = argparse.ArgumentParser(
        description="세법 PDF 문서를 ChromaDB에 인덱싱합니다."
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="기존 컬렉션을 삭제하고 재인덱싱합니다.",
    )
    args = parser.parse_args()

    vectorstore_service = VectorStoreService(settings)

    if args.force:
        logger.info("--force 옵션 감지: 기존 컬렉션을 삭제합니다.")
        vectorstore_service.delete_collection()
        logger.info("컬렉션 삭제 완료.")
        vectorstore_service = VectorStoreService(settings)  # 컬렉션 재초기화

    run_indexing(vectorstore_service)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
