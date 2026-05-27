import pytest
from app.utils.korean_tokenizer import KoreanTokenizer, KOREAN_STOPWORDS


class TestKoreanTokenizerTokenize:
    def setup_method(self):
        self.tokenizer = KoreanTokenizer()

    def test_legal_ref_kept_as_single_token(self):
        """법률 참조 패턴(제14조)이 하나의 토큰으로 유지된다."""
        tokens = self.tokenizer.tokenize("제14조 과세표준의 계산")
        assert "제14조" in tokens

    def test_korean_words_extracted(self):
        """한글 단어가 토큰으로 추출된다."""
        tokens = self.tokenizer.tokenize("과세표준 계산 방법")
        assert "과세표준" in tokens
        assert "계산" in tokens

    def test_english_lowercased(self):
        """영문은 소문자로 변환된다."""
        tokens = self.tokenizer.tokenize("Tax 신고")
        assert "tax" in tokens

    def test_numbers_extracted(self):
        """숫자가 토큰으로 추출된다."""
        tokens = self.tokenizer.tokenize("2024년 신고")
        assert "2024" in tokens

    def test_legal_ref_variants(self):
        """제N항, 제N호 등 변형 패턴도 하나의 토큰으로 처리된다."""
        tokens = self.tokenizer.tokenize("제3항에 따라 제1호 적용")
        assert "제3항" in tokens
        assert "제1호" in tokens

    def test_empty_string_returns_empty_list(self):
        """빈 문자열은 빈 리스트를 반환한다."""
        tokens = self.tokenizer.tokenize("")
        assert tokens == []


class TestKoreanTokenizerTokenizeForBm25:
    def setup_method(self):
        self.tokenizer = KoreanTokenizer()

    def test_stopwords_removed(self):
        """한국어 불용어가 제거된다."""
        tokens = self.tokenizer.tokenize_for_bm25("소득세는 과세표준에 따라 계산된다")
        stopwords_found = [t for t in tokens if t in KOREAN_STOPWORDS]
        assert stopwords_found == []

    def test_single_char_tokens_removed(self):
        """길이 1 이하의 토큰은 제거된다."""
        tokens = self.tokenizer.tokenize_for_bm25("제1조 가 나 다")
        assert all(len(t) > 1 for t in tokens)

    def test_legal_terms_preserved(self):
        """법률 참조 패턴은 불용어 제거 후에도 유지된다."""
        tokens = self.tokenizer.tokenize_for_bm25("제14조 과세표준 계산")
        assert "제14조" in tokens
        assert "과세표준" in tokens

    def test_result_is_subset_of_tokenize(self):
        """tokenize_for_bm25 결과는 tokenize 결과의 부분집합이다."""
        text = "제14조 과세표준의 계산은 이렇게 한다"
        all_tokens = set(self.tokenizer.tokenize(text))
        bm25_tokens = set(self.tokenizer.tokenize_for_bm25(text))
        assert bm25_tokens.issubset(all_tokens)
