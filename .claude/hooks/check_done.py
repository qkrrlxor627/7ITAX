"""Stop: 끝내기 전에 WORK_LOG 기록과 BE 테스트 통과를 확인한다."""
import os
import re
import subprocess
import sys

from _common import block, gradle_env, gradlew, project_dir, read_input

CODE_PATH = re.compile(r"^(BE|ai|FE|scripts|docker-compose|\.github|monitoring)(/|\.|$)")
# ./gradlew test 중 SampleExportGenerator가 다시 쓰는 산출물
TEST_OUTPUT_DIR = "docs/samples/"


def changed_paths(root):
    out = subprocess.run(["git", "-c", "core.quotepath=off", "status", "--porcelain"],
                         cwd=root, stdout=subprocess.PIPE, universal_newlines=True,
                         encoding="utf-8", errors="replace").stdout
    paths = []
    for line in out.splitlines():
        path = line[3:].split(" -> ")[-1].strip('"')
        if path:
            paths.append(path)
    return paths


def main():
    if read_input().get("stop_hook_active"):
        return 0  # 이미 한 번 되돌려 보낸 뒤 다시 멈추는 경우 무한 반복 방지

    root = project_dir()
    changed = changed_paths(root)
    code_changed = [p for p in changed if CODE_PATH.match(p)]
    if not code_changed:
        return 0

    if "WORK_LOG.md" not in changed:
        block("아직 끝내지 말 것: 코드가 바뀌었는데 WORK_LOG.md에 항목이 없다. CLAUDE.md 형식"
              "(배경/작업 내역/결과/변경된 파일 목록, 근거는 파일:줄·명령·수치)으로 맨 위에 추가한다.")

    if any(p.startswith("BE/") for p in code_changed):
        dirty_before = {p for p in changed if p.startswith(TEST_OUTPUT_DIR)}
        be_dir = os.path.join(root, "BE")
        proc = subprocess.run(gradlew(be_dir) + ["test", "-q"], cwd=be_dir,
                              env=gradle_env(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              universal_newlines=True, encoding="utf-8", errors="replace")
        # 테스트가 새로 더럽힌 샘플 산출물만 되돌린다(원래 수정 중이던 파일은 건드리지 않음)
        regenerated = [p for p in changed_paths(root)
                       if p.startswith(TEST_OUTPUT_DIR) and p not in dirty_before]
        if regenerated:
            subprocess.run(["git", "restore", "--"] + regenerated, cwd=root)
        if proc.returncode != 0:
            tail = "\n".join(proc.stdout.splitlines()[-60:])
            block("아직 끝내지 말 것: BE 테스트 실패. 기준선은 308개 통과다.\n" + tail)
    return 0


if __name__ == "__main__":
    sys.exit(main())
