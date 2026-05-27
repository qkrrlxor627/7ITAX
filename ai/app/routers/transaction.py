import logging
from typing import Optional

from fastapi import APIRouter, Depends

from app.core.dependencies import get_explanation_service, get_tax_classifier_service
from app.models.transaction import (
    TransactionClassifyRequest,
    TransactionClassifyResponse,
)
from app.services.explanation_service import ExplanationService
from app.services.tax_classifier_service import TaxClassifierService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/transaction", tags=["transaction"])


@router.post("/classify", response_model=TransactionClassifyResponse)
async def classify_transaction(
    request: TransactionClassifyRequest,
    service: TaxClassifierService = Depends(get_tax_classifier_service),
    explanation_service: Optional[ExplanationService] = Depends(get_explanation_service),
) -> TransactionClassifyResponse:
    """거래 내역을 세목으로 분류한다. (로컬 fine-tuned 모델)"""
    result = service.classify(request.description)

    reason = ""
    legal_basis = ""
    if explanation_service is not None:
        try:
            explanation = await explanation_service.generate_explanation(
                description=request.description,
                category=result["category"],
                confidence=result["confidence"],
            )
            reason = explanation.get("reason", "")
            legal_basis = explanation.get("legal_basis", "")
        except Exception as e:
            logger.warning("설명 생성 실패 (무시): %s", e)

    return TransactionClassifyResponse(
        category=result["category"],
        confidence=result["confidence"],
        method=result["method"],
        reason=reason,
        legal_basis=legal_basis,
    )
