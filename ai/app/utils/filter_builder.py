# ai/app/utils/filter_builder.py

# 인텐트별 기본 메타데이터 필터 매핑
# 아키텍처 설계 문서 4.1절의 각 인텐트 metadata_filter
INTENT_FILTER_MAP: dict[str, dict] = {
    "TAX_RATE_LOOKUP": {
        "topics": {"$in": ["세율", "과세표준", "세율표"]},
    },
    "EXPENSE_CLASSIFICATION": {
        "topics": {"$in": ["경비", "필요경비", "경비율", "접대비", "감가상각"]},
    },
    "DEDUCTION_ELIGIBILITY": {
        "topics": {"$in": ["공제", "감면", "세액공제", "소득공제"]},
    },
    "PROCEDURE_GUIDE": {
        "topics": {"$in": ["신고", "절차", "등록", "발급"]},
    },
    "CONCEPT_EXPLANATION": {},
    "CALCULATION": {
        "topics": {"$in": ["세율", "계산", "과세표준"]},
    },
    "COMPARISON": {},
    "GENERAL": {},
}


def build_filter_for_intent(
    intent_name: str,
    additional_filter: dict | None = None,
) -> dict | None:
    """인텐트 이름으로 메타데이터 필터를 생성한다.

    입력:
    - intent_name: 인텐트 이름 (예: "EXPENSE_CLASSIFICATION")
    - additional_filter: 추가 필터 (사용자 컨텍스트 등)

    출력: dict | None (필터가 없으면 None)
    """
    base_filter = dict(INTENT_FILTER_MAP.get(intent_name, {}))

    if additional_filter:
        base_filter.update(additional_filter)

    return base_filter if base_filter else None
