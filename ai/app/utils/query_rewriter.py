"""세법 도메인 특화 쿼리 변환기.

구어체 질문을 법률 용어로 확장하여 검색 품질을 향상시킨다.
2단계 하이브리드: 동의어 사전 (0ms) + LLM fallback (선택적, ~300ms).
"""

TAX_SYNONYM_MAP: dict[str, list[str]] = {
    "프리랜서": ["사업소득자", "인적용역"],
    "세금": ["소득세", "부가가치세", "원천징수"],
    "세율": ["세율", "원천징수세율", "과세표준"],
    "알바": ["일용근로소득", "일용직"],
    "월급": ["근로소득", "급여"],
    "부업": ["겸업", "복수사업장", "종합소득"],
    "경비": ["필요경비", "경비"],
    "환급": ["세액환급", "경정청구"],
    "공제": ["세액공제", "소득공제"],
    "신고": ["확정신고", "종합소득세 신고"],
    "사업자등록": ["사업자등록", "사업개시"],
    "간이과세": ["간이과세자", "간이과세"],
    "일반과세": ["일반과세자", "일반과세"],
    "카드": ["신용카드매출전표", "신용카드"],
    "임대료": ["임차료", "지급임차료"],
    "서버비": ["지급수수료", "전산 관련 비용"],
    "구독료": ["지급수수료", "소프트웨어 사용료"],
    "식대": ["복리후생비", "식비"],
    "택시비": ["여비교통비", "교통비"],
    "접대": ["접대비", "교제비"],
}


class QueryRewriter:
    """검색 전 쿼리 변환기.

    2단계 하이브리드:
    1. 동의어 사전 기반 키워드 확장 (지연 0ms)
    2. LLM 기반 쿼리 변환 (지연 ~300ms, 옵션)
    """

    def __init__(self, llm=None) -> None:
        self.llm = llm

    def rewrite_with_synonyms(self, query: str) -> str:
        """동의어 사전으로 쿼리를 확장한다.

        원본 쿼리에 법률 용어를 append하여 검색 범위 확대.
        예: "프리랜서 세율" -> "프리랜서 세율 사업소득자 인적용역 원천징수세율"
        """
        expansions: list[str] = []
        for keyword, synonyms in TAX_SYNONYM_MAP.items():
            if keyword in query:
                expansions.extend(synonyms)

        if expansions:
            unique = list(dict.fromkeys(expansions))
            return f"{query} {' '.join(unique)}"
        return query

    async def rewrite_with_llm(self, query: str) -> str:
        """LLM으로 쿼리를 법률 용어로 변환한다.

        동의어 사전 매칭 실패 시 fallback으로 사용.
        Phase 3에서 활성화 예정.
        """
        if self.llm is None:
            return query

        prompt = (
            "다음 사용자 질문을 한국 세법 검색에 최적화된 검색 쿼리로 변환하세요.\n"
            "- 구어체를 법률 용어로 변환\n"
            "- 관련 법률명과 조문 번호가 있다면 추가\n"
            "- 원본 의미를 보존하면서 검색 키워드 확장\n"
            "- 변환된 쿼리만 출력 (설명 없이)\n\n"
            f"사용자 질문: {query}\n"
            "검색 쿼리:"
        )
        response = await self.llm.ainvoke(prompt)
        return response.content.strip()

    async def rewrite(self, query: str) -> str:
        """2단계 하이브리드 쿼리 변환.

        1. 동의어 사전 확장
        2. 확장이 없으면 LLM fallback (LLM 미설정 시 원본 반환)
        """
        expanded = self.rewrite_with_synonyms(query)
        if expanded != query:
            return expanded
        return await self.rewrite_with_llm(query)
