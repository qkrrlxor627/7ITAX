class AIServiceError(Exception):
    """AI 서비스 공통 예외."""
    def __init__(self, message: str, status_code: int = 500):
        self.message = message
        self.status_code = status_code


class LLMTimeoutError(AIServiceError):
    """LLM API 응답 시간 초과 예외."""
    def __init__(self, message: str = "LLM API 응답 시간 초과"):
        super().__init__(message, status_code=504)


class LLMRateLimitError(AIServiceError):
    """LLM API 요청 한도 초과 예외."""
    def __init__(self, message: str = "API 요청 한도 초과"):
        super().__init__(message, status_code=429)


class LLMAuthError(AIServiceError):
    """LLM API 인증 실패 예외."""
    def __init__(self, message: str = "API 인증 실패"):
        super().__init__(message, status_code=401)


class BackendClientError(AIServiceError):
    def __init__(self, message: str = "백엔드 서버 오류"):
        super().__init__(message, status_code=503)


class VectorStoreError(AIServiceError):
    def __init__(self, message: str = "벡터 저장소 오류"):
        super().__init__(message, status_code=503)


class EmbeddingError(AIServiceError):
    def __init__(self, message: str = "임베딩 생성 오류"):
        super().__init__(message, status_code=503)
