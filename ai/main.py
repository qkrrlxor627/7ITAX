"""편의용 엔트리포인트 리다이렉트.

uvicorn main:app 으로도 실행 가능하도록 app.main.app을 re-export한다.
권장 실행 명령: uvicorn app.main:app --reload --port 8000
"""

from app.main import app  # noqa: F401
