# 파이썬 가상 환경(Virtual Environment) 및 의존성 관리 가이드

이 문서는 파이썬 가상 환경을 구성하고 실행하며, 패키지를 효율적으로 관리하는 방법을 설명합니다. (Windows 환경 기준)

## 1. 가상 환경 구성 방법 (생성)

가상 환경을 만들고자 하는 프로젝트 루트 폴더(예: `ai/`) 터미널에서 아래 명령어를 실행합니다.
파이썬 내장 모듈인 `venv`를 사용하여 `venv`라는 이름의 폴더(가상 환경)를 생성합니다.

```bash
python -m venv venv
```

## 2. 가상 환경 실행 방법 (활성화)

가상 환경을 생성한 후, 패키지를 설치하거나 코드를 실행하기 전에 반드시 활성화해야 합니다.
사용 중인 터미널 종류에 맞는 명령어를 입력하세요.

*   **명령 프롬프트 (CMD):**
    ```cmd
    venv\Scripts\activate.bat
    ```
*   **PowerShell:**
    ```powershell
    venv\Scripts\Activate.ps1
    ```
    *(보안 오류 발생 시 관리자 권한으로 PowerShell 실행 후 `Set-ExecutionPolicy Unrestricted` 진행)*
*   **Git Bash 등 호환 터미널:**
    ```bash
    source venv/Scripts/activate
    ```

**확인:** 활성화가 성공하면 명령어 입력 줄 맨 앞에 `(venv)` 가 표시됩니다.

## 3. 의존성 관리 방법 (Freeze 및 Reinstall)

가상 환경 상에서 설치된 패키지들을 파일로 기록하고 나중에 복원할 수 있습니다.

### 3.1. requirements.txt에서 패키지 설치하기 (초기 세팅 시)
프로젝트를 처음 다운로드하거나 새로운 컴퓨터에서 시작할 때, `requirements.txt`에 명시된 필수 패키지들을 한 번에 설치해야 합니다. (이 과정 없이 스크립트를 실행하면 `ModuleNotFoundError` 에러가 발생합니다.)
```bash
pip install -r requirements.txt
```

### 3.2. 현재 설치된 환경 저장하기 (Freeze)
FastAPI, requests 등 프로젝트에 필요한 패키지 추가 설치 후, 현재 상태를 파일로 기록하려면 사용합니다.
```bash
pip freeze > requirements.txt
```

## 4. 자주 발생하는 에러 해결

#### `ModuleNotFoundError: No module named 'requests'` 등 에러 
가상 환경은 활성화되었으나 필수 패키지가 설치되지 않아 발생하는 문제입니다. 위 3.1 항목에 따라 터미널에 `pip install -r requirements.txt` 명령어를 실행하여 외부 라이브러리(`requests`, `fastapi` 등)를 먼저 설치해 주세요.

## 5. 가상 환경 종료 명령 (비활성화)
다른 프로젝트로 넘어가거나 가상 환경 사용을 종료할 때 사용합니다.
```bash
deactivate
```
