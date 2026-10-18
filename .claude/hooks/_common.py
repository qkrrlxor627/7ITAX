"""훅 공통 유틸. stdin JSON 읽기, 프로젝트 기준 상대경로 계산, 차단 출력."""
import json
import os
import sys


def read_input():
    # Windows 콘솔 기본 인코딩(cp949)이 아니라 UTF-8로 읽는다. 한글 경로가 깨져 JSON 파싱이 실패하면 훅이 그대로 통과해 버린다
    try:
        return json.loads(sys.stdin.buffer.read().decode("utf-8"))
    except ValueError:
        return {}


def project_dir():
    return os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()


def rel_path(file_path):
    """프로젝트 루트 기준 '/' 구분 상대경로. 프로젝트 밖이면 None.

    Windows에서 file_path가 C:\\... 백슬래시·대소문자 혼용으로 와도 비교되도록 정규화한다.
    """
    root = project_dir()
    path = file_path if os.path.isabs(file_path) else os.path.join(root, file_path)
    try:
        # Windows의 relpath는 대소문자를 무시하고 비교하되 결과는 원래 대소문자를 유지한다(normcase를 쓰면 소문자로 바뀐다)
        rel = os.path.relpath(os.path.abspath(path), os.path.abspath(root))
    except ValueError:  # Windows에서 드라이브가 다름
        return None
    rel = rel.replace("\\", "/")
    if rel == ".." or rel.startswith("../"):
        return None
    return rel


def gradle_env():
    """JAVA17_HOME이 있으면 JAVA_HOME으로 주입한다(시스템 기본 JDK가 17이 아닌 PC용)."""
    env = dict(os.environ)
    if env.get("JAVA17_HOME"):
        env["JAVA_HOME"] = env["JAVA17_HOME"]
    return env


def gradlew(be_dir):
    # 절대경로로 지정한다. Windows에서 cmd /c gradlew.bat은 cwd 기준으로 찾지 못하는 경우가 있다
    if os.name == "nt":
        return ["cmd", "/c", os.path.join(be_dir, "gradlew.bat")]
    return [os.path.join(be_dir, "gradlew")]


def block(message):
    sys.stderr.buffer.write((message + "\n").encode("utf-8"))
    sys.stderr.flush()
    sys.exit(2)
