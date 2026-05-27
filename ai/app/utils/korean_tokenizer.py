import re


KOREAN_STOPWORDS = {
    "은",
    "는",
    "이",
    "가",
    "을",
    "를",
    "의",
    "에",
    "와",
    "과",
    "도",
    "로",
    "으로",
    "에서",
    "에게",
    "부터",
    "까지",
    "만",
    "도",
    "라도",
}


class KoreanTokenizer:
    """한국어 법률 문서용 토크나이저.

    전략:
    1. 법률 참조 패턴("제14조", "제3항")을 하나의 토큰으로 유지
    2. 한글 단어/영문/숫자 추출
    3. 불용어 제거 (tokenize_for_bm25)
    """

    # 법률 참조 패턴 (하나의 토큰으로 유지)
    LEGAL_REF_PATTERN = re.compile(r"제\d+[조항호편장절](?:의\d+)?")

    # 한글 단어 + 영문 + 숫자 패턴
    WORD_PATTERN = re.compile(r"[가-힣]+|[a-zA-Z]+|\d+")

    def tokenize(self, text: str) -> list[str]:
        """텍스트를 토큰 리스트로 분할한다.

        1. 법률 참조 패턴 먼저 추출
        2. 나머지에서 한글/영문/숫자 추출
        3. 영문 소문자 변환
        """
        tokens = []

        # 법률 참조 패턴 추출 후 해당 부분 제거
        for match in self.LEGAL_REF_PATTERN.finditer(text):
            tokens.append(match.group())

        # 법률 참조 제거 후 나머지 토크나이징
        remaining = self.LEGAL_REF_PATTERN.sub(" ", text)
        for match in self.WORD_PATTERN.finditer(remaining):
            token = match.group()
            if token.isascii():
                token = token.lower()
            tokens.append(token)

        return tokens

    def tokenize_for_bm25(self, text: str) -> list[str]:
        """BM25 인덱싱/검색용 토크나이징.

        tokenize()에 불용어 제거 추가.
        """
        tokens = self.tokenize(text)
        return [t for t in tokens if t not in KOREAN_STOPWORDS and len(t) > 1]
