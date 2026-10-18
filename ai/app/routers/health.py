from fastapi import APIRouter, Response, status

from app.core.config import settings

router = APIRouter(prefix="/api/v1/health", tags=["health"])


@router.get("/")
async def health_check() -> dict[str, str]:
    """프로세스 생존 확인(liveness). 컨테이너 재시작 판단에만 쓴다."""
    return {"status": "ok"}


@router.get("/ready")
async def readiness_check(response: Response) -> dict[str, str]:
    """요청 처리 가능 여부(readiness). 필수 설정이 없으면 503을 반환한다."""
    if not settings.anthropic_api_key:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return {"status": "not_ready", "reason": "ANTHROPIC_API_KEY is not set"}
    return {"status": "ready"}
