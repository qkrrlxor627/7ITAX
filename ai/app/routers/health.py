from fastapi import APIRouter

router = APIRouter(prefix="/api/v1/health", tags=["health"])


@router.get("/")
async def health_check() -> dict[str, str]:
    """서버 헬스 체크를 수행한다.

    API 서버의 정상 작동 여부를 확인한다.
    \f

    Returns:
        dict[str, str]: 상태 정보를 포함한 딕셔너리.
    """
    return {"status": "ok"}
