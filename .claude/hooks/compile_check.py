"""PostToolUse(Edit|Write): BE 자바 파일을 고친 직후 컴파일만 빠르게 확인. 성공은 조용히."""
import os
import subprocess
import sys

from _common import block, gradle_env, gradlew, project_dir, read_input, rel_path


def main():
    file_path = (read_input().get("tool_input") or {}).get("file_path") or ""
    rel = rel_path(file_path) if file_path else None
    if not rel or not rel.lower().startswith("be/src/") or not rel.lower().endswith(".java"):
        return 0

    be_dir = os.path.join(project_dir(), "BE")
    proc = subprocess.run(gradlew(be_dir) + ["compileJava", "compileTestJava", "-q"],
                          cwd=be_dir, env=gradle_env(),
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                          universal_newlines=True, encoding="utf-8", errors="replace")
    if proc.returncode != 0:
        tail = "\n".join(proc.stdout.splitlines()[-40:])
        block("컴파일 실패 — 다음 수정 전에 먼저 고칠 것:\n" + tail)
    return 0


if __name__ == "__main__":
    sys.exit(main())
