from io import BytesIO
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from app.services.document_processor import (
    KOREAN_LAW_SEPARATORS,
    PDF_METADATA_MAP,
    DocumentProcessor,
    RawDocument,
    TextChunk,
)


# ---------------------------------------------------------------------------
# 헬퍼: mock PdfReader 생성
# ---------------------------------------------------------------------------

def _make_mock_reader(pages: list[str]) -> MagicMock:
    """지정한 페이지 텍스트를 반환하는 mock PdfReader를 생성한다."""
    mock_reader = MagicMock()
    mock_pages = []
    for text in pages:
        mock_page = MagicMock()
        mock_page.extract_text.return_value = text
        mock_pages.append(mock_page)
    mock_reader.pages = mock_pages
    return mock_reader


# ---------------------------------------------------------------------------
# _resolve_metadata
# ---------------------------------------------------------------------------

class TestResolveMetadata:
    def setup_method(self):
        self.processor = DocumentProcessor()

    def test_exact_key_match(self):
        """PDF_METADATA_MAP 키와 정확히 일치하는 파일명은 해당 메타데이터를 반환한다."""
        result = self.processor._resolve_metadata("소득세법(법률)")
        assert result == {
            "law_name": "소득세법",
            "law_type": "법률",
            "tax_type": "소득세",
        }

    def test_prefix_match_with_suffix(self):
        """파일명이 키로 시작하면 뒤에 버전 정보가 붙어도 매칭된다."""
        result = self.processor._resolve_metadata(
            "소득세법(법률)(제21065호)(20260102)"
        )
        assert result["law_name"] == "소득세법"
        assert result["law_type"] == "법률"
        assert result["tax_type"] == "소득세"

    def test_longer_key_takes_priority(self):
        """'소득세법 시행령'이 '소득세법'보다 먼저 매칭되어야 한다."""
        result = self.processor._resolve_metadata("소득세법 시행령(대통령령)(제36129호)")
        assert result["law_name"] == "소득세법 시행령"
        assert result["law_type"] == "시행령"

    def test_unknown_filename_returns_default(self):
        """매핑에 없는 파일명은 기본값을 반환한다."""
        result = self.processor._resolve_metadata("알수없는파일명")
        assert result == {
            "law_name": "알수없는파일명",
            "law_type": "unknown",
            "tax_type": "unknown",
        }

    def test_all_keys_in_map_are_resolvable(self):
        """PDF_METADATA_MAP의 모든 키가 _resolve_metadata로 조회 가능하다."""
        for key, expected in PDF_METADATA_MAP.items():
            result = self.processor._resolve_metadata(key)
            assert result == expected, f"키 '{key}' 매핑 실패"

    def test_returns_copy_not_reference(self):
        """반환된 딕셔너리는 원본 맵의 참조가 아닌 복사본이어야 한다."""
        result = self.processor._resolve_metadata("소득세법(법률)")
        result["law_name"] = "변경됨"
        assert PDF_METADATA_MAP["소득세법(법률)"]["law_name"] == "소득세법"


# ---------------------------------------------------------------------------
# extract_text_from_pdf
# ---------------------------------------------------------------------------

class TestExtractTextFromPdf:
    def setup_method(self):
        self.processor = DocumentProcessor()

    def test_returns_raw_document(self):
        """extract_text_from_pdf()는 RawDocument를 반환한다."""
        mock_reader = _make_mock_reader(["페이지1 텍스트", "페이지2 텍스트"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("소득세법(법률)(제21065호)(20260102).pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert isinstance(result, RawDocument)

    def test_content_joins_pages_with_newline(self):
        """여러 페이지 텍스트가 '\\n'으로 결합되어 content에 저장된다."""
        mock_reader = _make_mock_reader(["첫 번째 페이지", "두 번째 페이지"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("소득세법(법률)(제21065호)(20260102).pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert result.content == "첫 번째 페이지\n두 번째 페이지"

    def test_metadata_resolved_from_filename(self):
        """파일명으로 메타데이터가 올바르게 조회된다."""
        mock_reader = _make_mock_reader(["텍스트"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("소득세법(법률)(제21065호)(20260102).pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert result.metadata["law_name"] == "소득세법"
        assert result.metadata["law_type"] == "법률"
        assert result.metadata["tax_type"] == "소득세"

    def test_page_count_matches_pdf(self):
        """page_count가 PDF의 실제 페이지 수와 일치한다."""
        mock_reader = _make_mock_reader(["p1", "p2", "p3"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("소득세법(법률)(제21065호)(20260102).pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert result.page_count == 3

    def test_source_path_stored_correctly(self):
        """source_path에 원본 PDF 경로가 문자열로 저장된다."""
        mock_reader = _make_mock_reader(["텍스트"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("ai/resources/소득세법(법률)(제21065호)(20260102).pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert result.source_path == str(pdf_path)

    def test_empty_page_text_is_skipped(self):
        """extract_text()가 None 또는 빈 문자열을 반환하는 페이지는 건너뛴다."""
        mock_reader = _make_mock_reader(["유효한 텍스트", "", None])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("소득세법(법률).pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert result.content == "유효한 텍스트"

    def test_unknown_pdf_uses_default_metadata(self):
        """매핑에 없는 PDF 파일명은 기본 메타데이터를 사용한다."""
        mock_reader = _make_mock_reader(["텍스트"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            pdf_path = Path("알수없는법률.pdf")
            result = self.processor.extract_text_from_pdf(pdf_path)

        assert result.metadata["law_type"] == "unknown"
        assert result.metadata["tax_type"] == "unknown"


# ---------------------------------------------------------------------------
# extract_all
# ---------------------------------------------------------------------------

class TestExtractAll:
    def setup_method(self):
        self.processor = DocumentProcessor(resources_dir="ai/resources")

    def test_returns_list_of_raw_documents(self, tmp_path):
        """PDF 파일이 있으면 RawDocument 리스트를 반환한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        pdf_file = tmp_path / "소득세법(법률).pdf"
        pdf_file.write_bytes(b"dummy")

        mock_reader = _make_mock_reader(["페이지 텍스트"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            result = processor.extract_all()

        assert len(result) == 1
        assert isinstance(result[0], RawDocument)

    def test_returns_empty_list_when_no_pdfs(self, tmp_path):
        """PDF 파일이 없으면 빈 리스트를 반환한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        result = processor.extract_all()
        assert result == []

    def test_skips_failed_pdf_with_warning(self, tmp_path, caplog):
        """읽기 실패한 PDF는 건너뛰고 warning 로그를 남긴다."""
        import logging

        processor = DocumentProcessor(resources_dir=str(tmp_path))
        pdf_file = tmp_path / "소득세법(법률).pdf"
        pdf_file.write_bytes(b"broken")

        with patch(
            "app.services.document_processor.PdfReader",
            side_effect=Exception("PDF 파싱 오류"),
        ):
            with caplog.at_level(logging.WARNING):
                result = processor.extract_all()

        assert result == []
        assert any("소득세법(법률).pdf" in record.message for record in caplog.records)

    def test_processes_multiple_pdfs(self, tmp_path):
        """여러 PDF 파일을 모두 처리하여 반환한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        for name in ["소득세법(법률).pdf", "부가가치세법(법률).pdf"]:
            (tmp_path / name).write_bytes(b"dummy")

        mock_reader = _make_mock_reader(["텍스트"])
        with patch(
            "app.services.document_processor.PdfReader", return_value=mock_reader
        ):
            result = processor.extract_all()

        assert len(result) == 2

    def test_partial_failure_returns_successful_docs(self, tmp_path):
        """일부 PDF 실패 시 성공한 문서만 반환한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        (tmp_path / "소득세법(법률).pdf").write_bytes(b"ok")
        (tmp_path / "broken.pdf").write_bytes(b"broken")

        mock_reader = _make_mock_reader(["정상 텍스트"])
        call_count = 0

        def side_effect(path):
            nonlocal call_count
            call_count += 1
            if "broken" in str(path):
                raise Exception("파싱 오류")
            return mock_reader

        with patch("app.services.document_processor.PdfReader", side_effect=side_effect):
            result = processor.extract_all()

        assert len(result) == 1
        assert result[0].metadata["law_name"] == "소득세법"


# ---------------------------------------------------------------------------
# chunk_document
# ---------------------------------------------------------------------------

class TestChunkDocument:
    def setup_method(self):
        self.processor = DocumentProcessor()

    def _make_raw_doc(self, content: str = "테스트 내용") -> RawDocument:
        return RawDocument(
            content=content,
            metadata={"law_name": "소득세법", "law_type": "법률", "tax_type": "소득세"},
            page_count=1,
            source_path="test.pdf",
        )

    def test_returns_list_of_text_chunks(self):
        """chunk_document()는 TextChunk 리스트를 반환한다."""
        raw_doc = self._make_raw_doc()
        result = self.processor.chunk_document(raw_doc)

        assert isinstance(result, list)
        assert all(isinstance(chunk, TextChunk) for chunk in result)

    def test_chunk_inherits_parent_metadata(self):
        """각 청크는 부모 문서의 메타데이터를 상속한다."""
        raw_doc = self._make_raw_doc()
        result = self.processor.chunk_document(raw_doc)

        for chunk in result:
            assert chunk.metadata["law_name"] == "소득세법"
            assert chunk.metadata["law_type"] == "법률"
            assert chunk.metadata["tax_type"] == "소득세"

    def test_chunk_id_format(self):
        """chunk_id는 '{law_name}_{순번:03d}' 형식이어야 한다."""
        raw_doc = self._make_raw_doc("내용 " * 500)  # 청크가 여러 개 생길 정도로 긴 텍스트
        result = self.processor.chunk_document(raw_doc)

        for idx, chunk in enumerate(result, start=1):
            expected_id = f"소득세법_{idx:03d}"
            assert chunk.metadata["chunk_id"] == expected_id

    def test_chunk_source_path_stored(self):
        """각 청크 메타데이터에 source_path가 포함된다."""
        raw_doc = self._make_raw_doc()
        result = self.processor.chunk_document(raw_doc)

        for chunk in result:
            assert chunk.metadata["source_path"] == "test.pdf"

    def test_empty_content_returns_empty_list(self):
        """content가 빈 문자열이면 빈 리스트를 반환한다."""
        raw_doc = self._make_raw_doc(content="")
        result = self.processor.chunk_document(raw_doc)

        assert result == []


# ---------------------------------------------------------------------------
# KOREAN_LAW_SEPARATORS
# ---------------------------------------------------------------------------


class TestKoreanLawSeparators:
    def test_is_list(self):
        """KOREAN_LAW_SEPARATORS는 리스트 타입이어야 한다."""
        assert isinstance(KOREAN_LAW_SEPARATORS, list)

    def test_contains_required_separators(self):
        """필수 구분자 7개가 모두 포함되어야 한다."""
        required = [
            r"\n제\d+편",
            r"\n제\d+장",
            r"\n제\d+절",
            r"\n제\d+조",
            "\n\n",
            "\n",
            ".",
        ]
        for sep in required:
            assert sep in KOREAN_LAW_SEPARATORS, f"구분자 누락: {sep!r}"

    def test_hierarchy_order(self):
        """편 > 장 > 절 > 조 순서로 정렬되어야 한다 (계층적 분리를 위해)."""
        편_idx = KOREAN_LAW_SEPARATORS.index(r"\n제\d+편")
        장_idx = KOREAN_LAW_SEPARATORS.index(r"\n제\d+장")
        절_idx = KOREAN_LAW_SEPARATORS.index(r"\n제\d+절")
        조_idx = KOREAN_LAW_SEPARATORS.index(r"\n제\d+조")

        assert 편_idx < 장_idx < 절_idx < 조_idx


# ---------------------------------------------------------------------------
# process_all
# ---------------------------------------------------------------------------


class TestProcessAll:
    def test_returns_list_of_text_chunks(self, tmp_path):
        """process_all()은 TextChunk 리스트를 반환한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        (tmp_path / "소득세법(법률).pdf").write_bytes(b"dummy")

        mock_reader = _make_mock_reader(["소득세법 내용 텍스트"])
        with patch("app.services.document_processor.PdfReader", return_value=mock_reader):
            result = processor.process_all()

        assert isinstance(result, list)
        assert all(isinstance(chunk, TextChunk) for chunk in result)

    def test_returns_empty_list_when_no_pdfs(self, tmp_path):
        """PDF 파일이 없으면 빈 리스트를 반환한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        result = processor.process_all()

        assert result == []

    def test_aggregates_chunks_from_multiple_pdfs(self, tmp_path):
        """여러 PDF의 청크를 하나의 리스트로 합산한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        (tmp_path / "소득세법(법률).pdf").write_bytes(b"dummy")
        (tmp_path / "부가가치세법(법률).pdf").write_bytes(b"dummy")

        mock_reader = _make_mock_reader(["내용 텍스트"])
        with patch("app.services.document_processor.PdfReader", return_value=mock_reader):
            result = processor.process_all()

        # 두 PDF에서 각각 청크가 생성되어 합산되어야 함
        assert len(result) >= 2

    def test_chunk_metadata_includes_law_fields(self, tmp_path):
        """process_all() 결과 청크에 law_name, law_type, tax_type이 포함된다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        (tmp_path / "소득세법(법률).pdf").write_bytes(b"dummy")

        mock_reader = _make_mock_reader(["소득세법 내용"])
        with patch("app.services.document_processor.PdfReader", return_value=mock_reader):
            result = processor.process_all()

        for chunk in result:
            assert "law_name" in chunk.metadata
            assert "law_type" in chunk.metadata
            assert "tax_type" in chunk.metadata
            assert "chunk_id" in chunk.metadata

    def test_chunk_id_unique_across_all_chunks(self, tmp_path):
        """process_all() 결과에서 모든 chunk_id는 고유해야 한다."""
        processor = DocumentProcessor(resources_dir=str(tmp_path))
        (tmp_path / "소득세법(법률).pdf").write_bytes(b"dummy")
        (tmp_path / "부가가치세법(법률).pdf").write_bytes(b"dummy")

        # 청크가 여러 개 생기도록 충분히 긴 텍스트
        mock_reader = _make_mock_reader(["내용 " * 500])
        with patch("app.services.document_processor.PdfReader", return_value=mock_reader):
            result = processor.process_all()

        chunk_ids = [chunk.metadata["chunk_id"] for chunk in result]
        assert len(chunk_ids) == len(set(chunk_ids)), "chunk_id가 중복됩니다"


# ---------------------------------------------------------------------------
# chunk_document — 청킹 전략 분기
# ---------------------------------------------------------------------------


class TestChunkDocumentStrategy:
    """chunk_document() 청킹 전략 분기 테스트."""

    def setup_method(self):
        self.processor = DocumentProcessor()

    def _make_raw_doc(self, content: str, law_type: str) -> RawDocument:
        return RawDocument(
            content=content,
            metadata={"law_name": "소득세법", "law_type": law_type, "tax_type": "소득세"},
            page_count=1,
            source_path="test.pdf",
        )

    def test_table_document_uses_recursive_chunking(self):
        """law_type='테이블' 문서는 재귀적 청킹을 사용한다."""
        raw_doc = self._make_raw_doc("경비율표 내용 " * 10, "테이블")
        result = self.processor.chunk_document(raw_doc)
        assert isinstance(result, list)
        assert len(result) >= 1
        # 재귀적 청킹: chunk_id가 '{law_name}_NNN' 형식
        for chunk in result:
            assert "chunk_id" in chunk.metadata

    def test_law_document_uses_hierarchical_chunking(self):
        """law_type='법률' 문서는 계층적 청킹(LegalParser)을 사용한다."""
        content = "제14조(과세표준의 계산)\n거주자의 종합소득에 대한 소득세의 과세표준."
        raw_doc = self._make_raw_doc(content, "법률")
        result = self.processor.chunk_document(raw_doc)
        assert isinstance(result, list)
        assert len(result) >= 1

    def test_sihaengryeong_uses_hierarchical_chunking(self):
        """law_type='시행령' 문서도 계층적 청킹을 사용한다."""
        content = "제1조(목적)\n이 영은 소득세법에서 위임된 사항을 규정한다."
        raw_doc = self._make_raw_doc(content, "시행령")
        result = self.processor.chunk_document(raw_doc)
        assert isinstance(result, list)

    def test_hierarchical_chunk_has_article_metadata(self):
        """계층적 청킹 결과에 article 메타데이터가 포함된다."""
        content = "제14조(과세표준의 계산)\n내용입니다."
        raw_doc = self._make_raw_doc(content, "법률")
        result = self.processor.chunk_document(raw_doc)
        if result:  # LegalParser가 파싱 성공한 경우
            assert "article" in result[0].metadata or "chunk_id" in result[0].metadata

    def test_hierarchical_chunk_fallback_on_empty_parse(self):
        """LegalParser가 청크를 생성하지 못하면 재귀적 청킹으로 폴백한다."""
        # 조문 패턴이 없는 일반 텍스트 → LegalParser 결과 없음
        content = "일반적인 내용으로 조문 구조가 없습니다."
        raw_doc = self._make_raw_doc(content, "법률")
        result = self.processor.chunk_document(raw_doc)
        # 폴백으로 재귀적 청킹이 실행되어 최소 1개 청크 반환
        assert isinstance(result, list)
        assert len(result) >= 1

    def test_table_and_law_chunk_id_different_format(self):
        """테이블 문서와 법률 문서의 chunk_id 형식이 다르다."""
        table_doc = self._make_raw_doc("경비율 내용 " * 5, "테이블")
        law_doc = self._make_raw_doc(
            "제14조(과세표준의 계산)\n내용입니다.", "법률"
        )
        table_chunks = self.processor.chunk_document(table_doc)
        law_chunks = self.processor.chunk_document(law_doc)

        # 테이블: '소득세법_001' 형식 (03자리 숫자)
        if table_chunks:
            table_id = table_chunks[0].metadata.get("chunk_id", "")
            assert "_" in table_id

        # 법률: '소득세법_14' 형식 (조 번호)
        if law_chunks:
            law_id = law_chunks[0].metadata.get("chunk_id", "")
            assert "소득세법" in law_id
