import logging
from dataclasses import dataclass
from pathlib import Path

from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader

from app.utils.legal_parser import LegalParser

logger = logging.getLogger(__name__)


KOREAN_LAW_SEPARATORS: list[str] = [
    r"\n제\d+편",  # 편
    r"\n제\d+장",  # 장
    r"\n제\d+절",  # 절
    r"\n제\d+조",  # 조
    "\n\n",  # 빈 줄
    "\n",  # 줄바꿈
    ".",  # 문장
]

DEFAULT_CHUNK_SIZE: int = 1000
DEFAULT_CHUNK_OVERLAP: int = 200

# 임베딩 모델 토큰 한도(8192) 기준: 한국어 1자 ≈ 1.5토큰 → 안전 마진 포함 4000자
MAX_EMBED_CHARS: int = 4000


@dataclass
class RawDocument:
    """PDF에서 추출한 원본 텍스트 + 메타데이터."""

    content: str
    metadata: dict
    page_count: int
    source_path: str


@dataclass
class TextChunk:
    """청크 분할된 텍스트 + 메타데이터."""

    content: str
    metadata: dict


PDF_METADATA_MAP: dict[str, dict[str, str]] = {
    "소득세법(법률)": {
        "law_name": "소득세법",
        "law_type": "법률",
        "tax_type": "소득세",
    },
    "소득세법 시행령": {
        "law_name": "소득세법 시행령",
        "law_type": "시행령",
        "tax_type": "소득세",
    },
    "소득세법 시행규칙": {
        "law_name": "소득세법 시행규칙",
        "law_type": "시행규칙",
        "tax_type": "소득세",
    },
    "부가가치세법(법률)": {
        "law_name": "부가가치세법",
        "law_type": "법률",
        "tax_type": "부가가치세",
    },
    "부가가치세법 시행령": {
        "law_name": "부가가치세법 시행령",
        "law_type": "시행령",
        "tax_type": "부가가치세",
    },
    "세무사법(법률)": {
        "law_name": "세무사법",
        "law_type": "법률",
        "tax_type": "세무사",
    },
    "조세특례제한법(법률)": {
        "law_name": "조세특례제한법",
        "law_type": "법률",
        "tax_type": "조세특례",
    },
    "조세특례제한법 시행령": {
        "law_name": "조세특례제한법 시행령",
        "law_type": "시행령",
        "tax_type": "조세특례",
    },
    "기준(단순)경비율": {
        "law_name": "기준경비율표",
        "law_type": "테이블",
        "tax_type": "소득세",
    },
}


class DocumentProcessor:
    """PDF 문서에서 텍스트를 추출하는 프로세서."""

    def __init__(
        self,
        resources_dir: str = "resources",
        chunk_size: int = DEFAULT_CHUNK_SIZE,
        chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
    ) -> None:
        self.resources_dir = Path(resources_dir)
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=KOREAN_LAW_SEPARATORS,
            is_separator_regex=True,
        )
        self.legal_parser = LegalParser()

    def extract_text_from_pdf(self, pdf_path: Path) -> RawDocument:
        """단일 PDF에서 텍스트를 추출한다.

        Args:
            pdf_path: PDF 파일 경로.

        Returns:
            추출된 텍스트와 메타데이터를 담은 RawDocument.

        Raises:
            FileNotFoundError: PDF 파일이 존재하지 않을 때.
            Exception: PDF 읽기에 실패했을 때.
        """
        reader = PdfReader(pdf_path)
        pages: list[str] = []

        for page in reader.pages:
            text = page.extract_text()
            if text:
                pages.append(text)

        content = "\n".join(pages)
        metadata = self._resolve_metadata(pdf_path.stem)

        return RawDocument(
            content=content,
            metadata=metadata,
            page_count=len(reader.pages),
            source_path=str(pdf_path),
        )

    def extract_all(self) -> list[RawDocument]:
        """resources 디렉토리의 모든 PDF를 추출한다.

        Returns:
            추출된 RawDocument 리스트. 실패한 파일은 건너뛴다.
        """
        pdf_files = sorted(self.resources_dir.glob("*.pdf"))

        if not pdf_files:
            logger.warning("PDF 파일을 찾을 수 없습니다: %s", self.resources_dir)
            return []

        documents: list[RawDocument] = []

        for pdf_path in pdf_files:
            try:
                doc = self.extract_text_from_pdf(pdf_path)
                logger.info(
                    "PDF 추출 완료: %s (%d페이지)", pdf_path.name, doc.page_count
                )
                documents.append(doc)
            except Exception:
                logger.exception("PDF 추출 실패: %s", pdf_path.name)

        logger.info("총 %d/%d개 PDF 추출 완료", len(documents), len(pdf_files))
        return documents

    def _resolve_metadata(self, filename: str) -> dict[str, str]:
        """파일명으로 PDF_METADATA_MAP에서 메타데이터를 조회한다.

        파일명이 매핑 키로 시작하는지 확인하여 매칭한다.
        예: '소득세법(법률)(제21065호)(20260102)' -> '소득세법(법률)' 키로 매칭.

        Args:
            filename: 확장자를 제외한 PDF 파일명.

        Returns:
            법률명, 법 유형, 세목이 담긴 메타데이터 딕셔너리.
        """
        # 긴 키부터 매칭하여 '소득세법 시행령'이 '소득세법'보다 먼저 매칭되도록 함
        for key in sorted(PDF_METADATA_MAP, key=len, reverse=True):
            if filename.startswith(key):
                return dict(PDF_METADATA_MAP[key])

        logger.warning("메타데이터 매핑 실패, 기본값 사용: %s", filename)
        return {
            "law_name": filename,
            "law_type": "unknown",
            "tax_type": "unknown",
        }

    def chunk_document(self, raw_doc: RawDocument) -> list[TextChunk]:
        """문서 유형에 따라 청킹 전략을 분기한다.

        - law_type == "테이블": 재귀적 청킹 (RecursiveCharacterTextSplitter)
        - law_type in ["법률", "시행령", "시행규칙"]: 계층적 청킹 (LegalParser)

        Args:
            raw_doc: PDF에서 추출한 원본 문서.

        Returns:
            청크 분할된 TextChunk 리스트.
        """
        if raw_doc.metadata.get("law_type") == "테이블":
            return self._recursive_chunk(raw_doc)
        return self._hierarchical_chunk(raw_doc)

    def _recursive_chunk(self, raw_doc: RawDocument) -> list[TextChunk]:
        """재귀적 청킹 (경비율표 등 테이블 문서용)."""
        texts = self.splitter.split_text(raw_doc.content)
        law_name = raw_doc.metadata.get("law_name", "unknown")
        chunks: list[TextChunk] = []

        for idx, text in enumerate(texts, start=1):
            chunk_metadata = dict(raw_doc.metadata)
            chunk_metadata["chunk_id"] = f"{law_name}_{idx:03d}"
            chunk_metadata["source_path"] = raw_doc.source_path
            chunks.append(TextChunk(content=text, metadata=chunk_metadata))

        logger.info("재귀적 청크 분할 완료: %s -> %d개 청크", law_name, len(chunks))
        return chunks

    def _build_chunk_header(self, metadata: dict) -> str:
        """청크에 prepend할 구조 경로 헤더를 생성한다.

        예시 출력:
        [소득세법 (법률)] > 제3편 소득금액의 계산 > 제1장 총칙 > 제19조(사업소득)
        """
        parts = []

        law_name = metadata.get("law_name", "")
        law_type = metadata.get("law_type", "")
        if law_name:
            parts.append(f"[{law_name} ({law_type})]")

        if metadata.get("part"):
            title = metadata.get("part_title", "")
            parts.append(f"제{metadata['part']}편 {title}".strip())

        if metadata.get("chapter"):
            title = metadata.get("chapter_title", "")
            parts.append(f"제{metadata['chapter']}장 {title}".strip())

        if metadata.get("section"):
            title = metadata.get("section_title", "")
            parts.append(f"제{metadata['section']}절 {title}".strip())

        if metadata.get("article"):
            title = metadata.get("article_title", "")
            parts.append(f"제{metadata['article']}조({title})")

        return " > ".join(parts)

    def _hierarchical_chunk(self, raw_doc: RawDocument) -> list[TextChunk]:
        """계층적 청킹 (법률 문서용, LegalParser 사용)."""
        law_name = raw_doc.metadata.get("law_name", "unknown")
        law_type = raw_doc.metadata.get("law_type", "unknown")
        tax_type = raw_doc.metadata.get("tax_type", "unknown")

        legal_chunks = self.legal_parser.parse(
            text=raw_doc.content,
            law_name=law_name,
            law_type=law_type,
            tax_type=tax_type,
        )

        if not legal_chunks:
            logger.warning("계층적 청킹 결과 없음, 재귀적 청킹으로 폴백: %s", law_name)
            return self._recursive_chunk(raw_doc)

        chunks = []
        for lc in legal_chunks:
            merged_meta = {**raw_doc.metadata, **lc.metadata, "source_path": raw_doc.source_path}
            header = self._build_chunk_header(merged_meta)
            content_with_header = f"{header}\n{lc.content}" if header else lc.content
            chunks.append(TextChunk(content=content_with_header, metadata=merged_meta))

        logger.info("계층적 청크 분할 완료: %s -> %d개 청크", law_name, len(chunks))
        return chunks

    def _split_oversized(self, chunks: list[TextChunk]) -> list[TextChunk]:
        """임베딩 토큰 한도를 초과하는 청크를 재분할한다.

        한국어 1자 ≈ 1.5 토큰 기준, MAX_EMBED_CHARS(4000자) 초과 시
        RecursiveCharacterTextSplitter로 추가 분할한다.
        """
        result: list[TextChunk] = []
        oversized = 0

        for chunk in chunks:
            if len(chunk.content) <= MAX_EMBED_CHARS:
                result.append(chunk)
            else:
                oversized += 1
                sub_texts = self.splitter.split_text(chunk.content)
                header = self._build_chunk_header(chunk.metadata)
                for i, text in enumerate(sub_texts):
                    meta = dict(chunk.metadata)
                    meta["chunk_id"] = f"{meta['chunk_id']}_s{i:02d}"
                    if header and not text.startswith(header):
                        text = f"{header}\n{text}"
                    result.append(TextChunk(content=text, metadata=meta))

        if oversized:
            logger.info("초과 청크 재분할: %d개 → %d개", oversized, len(result))
        return result

    def process_all(self) -> list[TextChunk]:
        """전체 파이프라인: PDF 추출 -> 청킹 -> TextChunk 리스트 반환.

        1. extract_all()로 모든 PDF 추출
        2. 각 RawDocument에 대해 chunk_document() 호출
        3. 모든 청크를 하나의 리스트로 결합
        4. _split_oversized()로 토큰 한도 초과 청크 재분할
        5. 총 청크 수를 로그로 출력

        Returns:
            모든 문서의 TextChunk를 합친 리스트.
        """
        raw_docs = self.extract_all()
        all_chunks: list[TextChunk] = []

        for raw_doc in raw_docs:
            chunks = self.chunk_document(raw_doc)
            all_chunks.extend(chunks)

        all_chunks = self._split_oversized(all_chunks)
        logger.info("전체 청크 수: %d", len(all_chunks))
        return all_chunks
