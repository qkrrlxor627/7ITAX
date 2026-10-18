"""PreToolUse(Bash): 되돌리기 어려운 명령과 이 환경에서 거짓 음성을 내는 명령 차단."""
import os
import re
import sys

from _common import block, read_input


def main():
    cmd = (read_input().get("tool_input") or {}).get("command") or ""

    if re.search(r"git\s+push.*(--force|-f(\s|$))", cmd):
        block("차단: force push 금지. 브랜치 + PR로 진행한다(CLAUDE.md Git 규칙).")
    if re.search(r"git\s+push\s+\S+\s+(HEAD:)?main(\s|:|$)", cmd):
        block("차단: main 직접 푸시 금지. 작업 브랜치로 푸시하고 PR을 연다.")
    if re.search(r"docker\s+compose.*down.*-v", cmd) and os.environ.get("CI") != "true":
        block("차단: 로컬 볼륨(postgres-data 등) 삭제 명령. 정말 필요하면 사람에게 요청한다.")
    if re.search(r"(^|\s)(grep|find)\s", cmd) and not re.search(r"command\s+(grep|find)", cmd):
        block("차단: 이 환경에서 맨 grep/find는 셸 함수에 가로채져 빈 결과를 낸다(CLAUDE.md 환경 함정). "
              "command grep / command find 를 쓴다.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
