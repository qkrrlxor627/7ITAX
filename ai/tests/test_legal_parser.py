import pytest
from app.utils.legal_parser import (
    CIRCLED_NUMBER_MAP,
    LEGAL_PATTERNS,
    LegalChunk,
    LegalParser,
)


class TestLegalPatterns:
    def test_article_pattern_matches(self):
        """조 패턴이 '제14조(과세표준의 계산)' 형태를 매칭한다."""
        m = LEGAL_PATTERNS["article"].match("제14조(과세표준의 계산)")
        assert m is not None
        assert m.group(1) == "14"
        assert m.group(2) == "과세표준의 계산"

    def test_part_pattern_matches(self):
        """편 패턴이 '제3편 종합소득세액의 계산' 형태를 매칭한다."""
        m = LEGAL_PATTERNS["part"].match("제3편 종합소득세액의 계산")
        assert m is not None
        assert m.group(1) == "3"

    def test_chapter_pattern_matches(self):
        """장 패턴이 '제2장 세율' 형태를 매칭한다."""
        m = LEGAL_PATTERNS["chapter"].match("제2장 세율")
        assert m is not None
        assert m.group(1) == "2"

    def test_section_pattern_matches(self):
        """절 패턴이 '제1절 세율 적용' 형태를 매칭한다."""
        m = LEGAL_PATTERNS["section"].match("제1절 세율 적용")
        assert m is not None

    def test_article_alt_pattern_matches(self):
        """조의N 패턴이 '제14조의2(특례)' 형태를 매칭한다."""
        m = LEGAL_PATTERNS["article_alt"].match("제14조의2(특례)")
        assert m is not None
        assert m.group(1) == "14"
        assert m.group(2) == "2"
        # group(3)이 제목
        assert m.group(3) == "특례"

    def test_paragraph_pattern_matches_circled(self):
        """항 패턴이 원문자(①②...)를 매칭한다."""
        m = LEGAL_PATTERNS["paragraph"].search("① 거주자의 종합소득에 대한")
        assert m is not None

    def test_circled_number_map_has_15_entries(self):
        """CIRCLED_NUMBER_MAP에 ①~⑮ 15개 항목이 있다."""
        assert len(CIRCLED_NUMBER_MAP) == 15
        assert CIRCLED_NUMBER_MAP["①"] == 1
        assert CIRCLED_NUMBER_MAP["⑮"] == 15


class TestLegalParserSplitIntoArticles:
    def setup_method(self):
        self.parser = LegalParser()

    def test_single_article_parsed(self):
        """단일 조문 텍스트에서 하나의 article이 파싱된다."""
        text = "제14조(과세표준의 계산)\n거주자의 종합소득에 대한 소득세의 과세표준은 다음과 같이 계산한다."
        articles = self.parser._split_into_articles(text)
        assert len(articles) == 1
        assert articles[0]["article"] == 14
        assert articles[0]["article_title"] == "과세표준의 계산"

    def test_multiple_articles_parsed(self):
        """여러 조문이 올바르게 분리된다."""
        text = (
            "제14조(과세표준의 계산)\n첫 번째 조문 내용.\n"
            "제15조(세율 적용)\n두 번째 조문 내용."
        )
        articles = self.parser._split_into_articles(text)
        assert len(articles) == 2
        assert articles[0]["article"] == 14
        assert articles[1]["article"] == 15

    def test_part_chapter_section_context_tracked(self):
        """편/장/절 컨텍스트가 조에 부여된다."""
        text = (
            "제3편 종합소득세액의 계산\n"
            "제2장 세율\n"
            "제14조(과세표준의 계산)\n조문 내용."
        )
        articles = self.parser._split_into_articles(text)
        assert len(articles) == 1
        assert articles[0]["part"] == 3
        assert articles[0]["chapter"] == 2

    def test_empty_text_returns_empty_list(self):
        """빈 텍스트는 빈 리스트를 반환한다."""
        articles = self.parser._split_into_articles("")
        assert articles == []

    def test_article_alt_splits_article_and_sub(self):
        """'제14조의2' 파싱 시 article=14, article_sub=2로 분리된다."""
        text = "제14조의2(세액공제의 특례)\n특례 내용."
        articles = self.parser._split_into_articles(text)
        assert len(articles) == 1
        assert articles[0]["article"] == 14
        assert articles[0]["article_sub"] == 2
        assert articles[0]["article_title"] == "세액공제의 특례"

    def test_article_alt_does_not_collide_with_regular_article(self):
        """'제14조의2'와 '제142조'가 서로 다른 article로 분리된다."""
        text = (
            "제14조의2(세액공제의 특례)\n특례 내용.\n"
            "제142조(가산세)\n가산세 내용."
        )
        articles = self.parser._split_into_articles(text)
        assert len(articles) == 2
        # 제14조의2
        assert articles[0]["article"] == 14
        assert articles[0]["article_sub"] == 2
        # 제142조
        assert articles[1]["article"] == 142
        assert articles[1]["article_sub"] is None

    def test_regular_article_has_no_sub(self):
        """일반 조문은 article_sub가 None이다."""
        text = "제14조(과세표준의 계산)\n내용."
        articles = self.parser._split_into_articles(text)
        assert articles[0]["article_sub"] is None


class TestLegalParserSplitByParagraph:
    def setup_method(self):
        self.parser = LegalParser()

    def test_splits_by_circled_numbers(self):
        """원문자(①②)를 경계로 항이 분리된다."""
        text = "제14조(과세표준)\n① 첫 번째 항.\n② 두 번째 항."
        paragraphs = self.parser._split_article_by_paragraph(text)
        assert len(paragraphs) == 2
        assert paragraphs[0][1] == 1  # 항 번호
        assert paragraphs[1][1] == 2

    def test_no_paragraphs_returns_empty(self):
        """원문자가 없으면 빈 리스트를 반환한다."""
        text = "일반 텍스트 내용입니다."
        paragraphs = self.parser._split_article_by_paragraph(text)
        assert paragraphs == []


class TestLegalParserParse:
    def setup_method(self):
        self.parser = LegalParser()

    def test_returns_list_of_legal_chunks(self):
        """parse()는 LegalChunk 리스트를 반환한다."""
        text = "제14조(과세표준의 계산)\n거주자의 종합소득에 대한 소득세의 과세표준."
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        assert isinstance(result, list)
        assert all(isinstance(c, LegalChunk) for c in result)

    def test_metadata_contains_required_fields(self):
        """각 청크의 메타데이터에 필수 필드가 포함된다."""
        text = "제14조(과세표준의 계산)\n내용."
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        assert len(result) >= 1
        meta = result[0].metadata
        assert meta["law_name"] == "소득세법"
        assert meta["law_type"] == "법률"
        assert meta["tax_type"] == "소득세"
        assert "article" in meta
        assert "chunk_id" in meta

    def test_chunk_id_format(self):
        """chunk_id는 '{law_name}_{article}' 형식이다."""
        text = "제14조(과세표준의 계산)\n내용."
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        assert result[0].metadata["chunk_id"] == "소득세법_14"

    def test_long_article_splits_by_paragraph(self):
        """1500자 초과 조문은 항 단위로 추가 분할된다."""
        long_content = "① " + "가" * 800 + "\n② " + "나" * 800
        text = f"제14조(과세표준의 계산)\n{long_content}"
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        # 항 단위로 분할되어 2개 이상이어야 함
        assert len(result) >= 2
        # 항 번호가 메타데이터에 있어야 함
        paragraphs_with_num = [c for c in result if c.metadata.get("paragraph") is not None]
        assert len(paragraphs_with_num) >= 2

    def test_article_alt_chunk_id_format(self):
        """article_alt chunk_id는 '{law_name}_{article}_sub{article_sub}' 형식이다."""
        text = "제14조의2(세액공제의 특례)\n특례 내용."
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        assert result[0].metadata["chunk_id"] == "소득세법_14_sub2"
        assert result[0].metadata["article"] == 14
        assert result[0].metadata["article_sub"] == 2

    def test_article_alt_chunk_id_no_collision(self):
        """'제14조의2'와 '제142조'의 chunk_id가 충돌하지 않는다."""
        text = (
            "제14조의2(세액공제의 특례)\n특례 내용.\n"
            "제142조(가산세)\n가산세 내용."
        )
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        chunk_ids = [c.metadata["chunk_id"] for c in result]
        assert "소득세법_14_sub2" in chunk_ids
        assert "소득세법_142" in chunk_ids
        assert len(chunk_ids) == len(set(chunk_ids)), "chunk_id가 중복됩니다"

    def test_article_alt_long_splits_with_sub_in_chunk_id(self):
        """article_alt 조문이 항 단위 분할될 때도 chunk_id에 sub가 포함된다."""
        long_content = "① " + "가" * 800 + "\n② " + "나" * 800
        text = f"제14조의2(세액공제의 특례)\n{long_content}"
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        assert len(result) >= 2
        for chunk in result:
            assert chunk.metadata["chunk_id"].startswith("소득세법_14_sub2_")

    def test_regular_article_has_article_sub_none(self):
        """일반 조문의 metadata에서 article_sub는 None이다."""
        text = "제14조(과세표준의 계산)\n내용."
        result = self.parser.parse(text, "소득세법", "법률", "소득세")
        assert result[0].metadata["article_sub"] is None

    def test_empty_text_returns_empty_list(self):
        """빈 텍스트는 빈 리스트를 반환한다."""
        result = self.parser.parse("", "소득세법", "법률", "소득세")
        assert result == []
