from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """애플리케이션 설정.

    환경 변수에서 설정값을 로드한다.
    """
    app_name: str = "tax7i-ai"
    app_version: str = "0.1.0"
    debug: bool = False

    # GMS 프록시 (SSAFY OpenAI API)
    gms_api_key: str
    gms_base_url: str
    llm_model: str

    # 모델 티어링
    llm_model_mini: str = "gpt-4o-mini"
    llm_model_standard: str = "gpt-4o"

    # 캐시 설정
    cache_enabled: bool = True
    cache_threshold: float = 0.95
    cache_max_entries: int = 10000
    cache_ttl_hours: int = 24

    # RAG 검색 활성화 (False 시 LLM만으로 응답)
    rag_enabled: bool = True
    chroma_persist_directory: str = "./data/chroma"
    embedding_model: str = "text-embedding-3-small"

    # 백엔드 API
    backend_base_url: str = "http://localhost:8080"
    backend_api_key: str = ""

    # CORS
    allowed_origins: list[str] = ["http://localhost:3000"]

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
