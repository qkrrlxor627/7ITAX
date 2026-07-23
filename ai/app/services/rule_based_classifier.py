"""세목 분류 규칙기반 폴백.

파인튜닝 모델(`models/tax_classifier`)이 없을 때 키워드 매칭으로 거래 설명을
20개 세목 중 하나로 분류한다. 완벽한 분류기가 아니라 휴리스틱이며,
사용자가 최종 세목을 확정하는 제품 특성상 '제안' 용도로 충분하다.

`TaxClassifierService.classify()`와 동일한 dict 형태를 반환한다:
    {category, confidence, method, needs_fallback}
"""

import logging

logger = logging.getLogger(__name__)

DEFAULT_CATEGORY = "기타경비"
CONFIDENCE_THRESHOLD = 0.7

# 세목 -> 거래 설명/가맹점명에 흔히 나타나는 키워드(부분일치, 소문자 비교).
# "기타경비"는 아무 키워드도 매칭되지 않을 때의 기본값이라 별도 키워드가 없다.
CATEGORY_KEYWORDS: dict[str, list[str]] = {
    "매출": ["매출", "판매대금", "용역비", "프로젝트 대금", "컨설팅", "강의료", "수입", "대금 수령"],
    "상품·원재료 매입": ["매입", "원재료", "재료비", "부자재", "상품 구매", "도매", "사입"],
    "급료": ["급여", "월급", "임금", "인건비", "급료", "상여", "일당", "아르바이트", "알바", "4대보험"],
    "제세공과금": ["재산세", "자동차세", "지방세", "면허세", "인지세", "공과금", "환경부담금", "협회비", "회비", "과태료"],
    "임차료": ["임차료", "월세", "임대료", "사무실 임대", "공유오피스", "렌트", "전세", "서버 임대"],
    "지급이자": ["대출이자", "이자", "리스 이자", "차입금 이자"],
    "접대비": ["접대", "회식", "거래처 식사", "고객 접대", "유흥", "노래방", "룸살롱", "거래처 선물"],
    "기부금": ["기부", "후원금", "성금", "기부금"],
    "감가상각비": ["감가상각"],
    "차량유지비": ["주유", "주유소", "기름값", "하이패스", "톨게이트", "주차", "세차", "자동차 보험", "타이어", "엔진오일", "정비", "차량 수리", "충전"],
    "지급수수료": ["수수료", "구독료", "구독", "aws", "github", "api", "라이선스", "플랫폼 이용료", "중개 수수료", "결제대행", "saas", "클라우드", "figma", "notion"],
    "소모품비": ["소모품", "문구", "사무용품", "토너", "카트리지", "usb", "비품", "용지", "잉크"],
    "복리후생비": ["복리후생", "직원 식대", "간식", "커피", "건강검진", "명절 선물", "경조사"],
    "운반비": ["택배", "퀵서비스", "퀵", "배송비", "운송", "화물", "운반"],
    "광고선전비": ["광고", "홍보", "마케팅", "키워드 광고", "sns", "현수막", "전단", "명함 인쇄", "간판"],
    "여비교통비": ["택시", "ktx", "기차", "항공권", "비행기", "고속버스", "숙박", "호텔", "출장", "교통비", "지하철", "버스"],
    "고정자산 매입": ["노트북", "컴퓨터", "서버 구매", "장비 구매", "기계 구매", "설비", "모니터", "맥북", "가구 구매", "에어컨 구매"],
    "고정자산 매도": ["매각", "처분", "중고 판매", "자산 매도"],
    "기초/기말 재고": ["기말 재고", "기초 재고", "재고 평가"],
}


def classify_by_rules(user_input: str) -> dict:
    """키워드 매칭으로 거래 설명을 세목으로 분류한다.

    카테고리별 키워드 부분일치 개수를 세어 가장 많이 매칭된 세목을 선택한다.
    매칭이 없으면 기본값("기타경비")과 needs_fallback=True를 반환한다.
    """
    text = (user_input or "").lower()
    if not text.strip():
        return {
            "category": DEFAULT_CATEGORY,
            "confidence": 0.0,
            "method": "rule_based",
            "needs_fallback": True,
        }

    best_category = DEFAULT_CATEGORY
    best_hits = 0
    for category, keywords in CATEGORY_KEYWORDS.items():
        hits = sum(1 for kw in keywords if kw.lower() in text)
        if hits > best_hits:
            best_hits = hits
            best_category = category

    if best_hits == 0:
        return {
            "category": DEFAULT_CATEGORY,
            "confidence": 0.0,
            "method": "rule_based",
            "needs_fallback": True,
        }

    confidence = round(min(0.5 + 0.1 * best_hits, CONFIDENCE_THRESHOLD), 4)
    logger.debug(
        "규칙기반 분류: '%s' -> %s (hits=%d, confidence=%.2f)",
        user_input[:50],
        best_category,
        best_hits,
        confidence,
    )
    return {
        "category": best_category,
        "confidence": confidence,
        "method": "rule_based",
        "needs_fallback": confidence < CONFIDENCE_THRESHOLD,
    }
