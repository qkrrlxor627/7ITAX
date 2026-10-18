"""PreToolUse(Edit|Write): AI 로직·시크릿·초기화 SQL 수정 차단."""
import posixpath
import sys

from _common import block, read_input, rel_path

AI_LOGIC = ("ai/app/services/", "ai/app/models/", "ai/models/")


def main():
    file_path = (read_input().get("tool_input") or {}).get("file_path") or ""
    if not file_path:
        return 0
    rel = rel_path(file_path)
    if rel is None:
        return 0
    rel_lower = rel.lower()

    if rel_lower.startswith(AI_LOGIC):
        block(f"차단: {rel} 은 AI 로직이다. 이 작업 범위(docs/PRE_CICD.md 0-9단계)에서는 수정하지 않는다. "
              "서빙 레이어(routers/health.py, main.py 계측)만 허용.")

    name = posixpath.basename(rel_lower)
    if (name == ".env" or name.startswith(".env.")) and name != ".env.example":
        block(f"차단: {rel} 은 시크릿 파일이다. 값은 사람이 채운다. 키 목록 변경은 .env.example에서 한다.")

    if rel_lower.startswith("db/init/"):
        block(f"차단: {rel} 은 신규 볼륨 초기화 SQL이다. 스키마 변경은 Flyway 마이그레이션(IMPROVEMENT_PLAN 5번)으로 한다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
