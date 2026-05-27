from __future__ import annotations

from typing import TYPE_CHECKING

from app.services.retrieval_service import SearchResult

if TYPE_CHECKING:
    from app.services.backend_client import BusinessInfo, Transaction


def format_transactions(transactions: list[Transaction]) -> str:
    """거래 내역을 프롬프트용 텍스트로 포맷팅한다.

    포맷 예시:
    [사용자 거래 내역]
    1. 2026-02-15 | 스타벅스 강남점 | 45,000원 | 카페/음식점 (MCC: 5812)

    빈 리스트 입력 시 빈 문자열 반환.
    """
    if not transactions:
        return ""

    lines = ["[사용자 거래 내역]"]
    for i, t in enumerate(transactions, start=1):
        category_part = f" | {t.category}" if t.category else ""
        lines.append(
            f"{i}. {t.date} | {t.merchant_name} | {t.amount:,}원{category_part} (MCC: {t.mcc})"
        )
    return "\n".join(lines)


def format_business_info(info: BusinessInfo | None) -> str:
    """사업자 정보를 프롬프트용 텍스트로 포맷팅한다.

    포맷 예시:
    [사용자 사업자 정보]
    - 사업자 유형: 개인사업자
    - 업종코드: 701101
    - 과세 유형: 간이과세
    - 사업 개시일: 2024-06-01

    None 입력 시 빈 문자열 반환.
    """
    if info is None:
        return ""

    return (
        "[사용자 사업자 정보]\n"
        f"- 사업자 유형: {info.business_type}\n"
        f"- 업종코드: {info.industry_code}\n"
        f"- 과세 유형: {info.tax_type}\n"
        f"- 사업 개시일: {info.establishment_date}"
    )


def format_search_results(results: list[SearchResult]) -> str:
    """검색 결과를 LLM 프롬프트용 텍스트로 포맷팅한다.

    포맷 예시:
    [출처: 소득세법 (법률)]
    제19조(사업소득) ① ...

    [출처: 소득세법 시행령 (시행령)]
    제40조(필요경비의 계산) ...
    """
    if not results:
        return ""

    parts = []
    for result in results:
        law_name = result.metadata.get("law_name", "알 수 없음")
        law_type = result.metadata.get("law_type", "알 수 없음")
        header = f"[출처: {law_name} ({law_type})]"
        parts.append(f"{header}\n{result.content}")

    return "\n\n".join(parts)


def truncate_text(text: str, max_length: int = 3000) -> str:
    """텍스트를 최대 길이로 자른다.

    LLM 컨텍스트 윈도우 초과 방지용.
    문장 단위로 자르되, max_length를 초과하지 않도록 한다.
    """
    if len(text) <= max_length:
        return text

    truncated = text[:max_length]
    # 마지막 문장 종결 부호 위치에서 자름
    for sep in (".", "。", "!", "?", "\n"):
        pos = truncated.rfind(sep)
        if pos != -1:
            return truncated[: pos + 1]

    return truncated
