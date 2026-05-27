from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """애플리케이션 설정.

    환경 변수에서 설정값을 로드한다.
    """
    app_name: str = "tax7i-ai"
    app_version: str = "0.1.0"
    debug: bool = False

    # Anthropic Claude
    anthropic_api_key: str

    # 모델 티어링 (standard=메인 챗봇, mini=쿼리 재작성·분류 설명)
    claude_model_opus: str = "claude-opus-4-7"
    claude_model_haiku: str = "claude-haiku-4-5-20251001"

    # 캐시 설정
    cache_enabled: bool = True
    cache_threshold: float = 0.95
    cache_max_entries: int = 10000
    cache_ttl_hours: int = 24

    # RAG 검색 활성화 (False 시 LLM만으로 응답)
    rag_enabled: bool = True
    chroma_persist_directory: str = "./data/chroma"
    # 임베딩: 로컬 HuggingFace 모델 (Claude는 임베딩 API 미제공)
    embedding_model: str = "BAAI/bge-m3"
    embedding_device: str = "cpu"

    # 백엔드 API
    backend_base_url: str = "http://localhost:8080"
    backend_api_key: str = ""

    # CORS
    allowed_origins: list[str] = ["http://localhost:3000"]

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
