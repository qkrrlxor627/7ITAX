# tax7i AI 서버

세금 관련 AI 챗봇 서비스의 백엔드 서버입니다. FastAPI 기반으로 LLM 채팅 API를 제공합니다.

## 1. 환경 설정

### 가상 환경 구성

가상 환경 생성 및 활성화 방법은 [`venv_guide.md`](./venv_guide.md)를 참고하세요.

### 의존성 설치

가상 환경 활성화 후 패키지를 설치합니다.

```bash
pip install -r requirements.txt
```

### 환경 변수 설정

`.env.example`을 복사하여 `.env` 파일을 생성하고, API 키를 입력합니다.

```bash
cp .env.example .env
```

`.env` 파일에서 아래 항목을 설정하세요.

```dotenv
# Anthropic Claude
ANTHROPIC_API_KEY=실제_API_키

# 모델 티어링 (standard=메인 챗봇=Opus, mini=가벼운 작업=Haiku)
CLAUDE_MODEL_OPUS=claude-opus-4-7
CLAUDE_MODEL_HAIKU=claude-haiku-4-5-20251001

# RAG (비활성화 시 LLM만 사용)
RAG_ENABLED=false
CHROMA_PERSIST_DIRECTORY=./data/chroma

# 임베딩: 로컬 HuggingFace 모델 (외부 API 키 불필요, 첫 실행 시 다운로드)
EMBEDDING_MODEL=BAAI/bge-m3
EMBEDDING_DEVICE=cpu

# 시맨틱 캐시
CACHE_ENABLED=true
CACHE_THRESHOLD=0.95
CACHE_MAX_ENTRIES=10000
CACHE_TTL_HOURS=24

# 백엔드 API 연동
BACKEND_BASE_URL=http://localhost:8080
BACKEND_API_KEY=

# 서버
DEBUG=false
ALLOWED_ORIGINS=["http://localhost:3000"]
```

## 2. 서버 실행

```bash
uvicorn app.main:app --reload --port 8000
```

서버 구동 후 `http://localhost:8000/docs`에서 Swagger UI로 API를 확인할 수 있습니다.

## 3. API 명세

### 3.1 헬스 체크

```
GET /api/v1/health/
```

**Response**
```json
{ "status": "ok" }
```

---

### 3.2 채팅

```
POST /api/v1/chat/
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `message` | string | O | 사용자 메시지 (1~2000자) |
| `session_id` | string \| null | X | 세션 ID. 없으면 자동 생성 |

```json
{
  "message": "종합소득세가 뭐야?",
  "session_id": null
}
```

**Response**

| 필드 | 타입 | 설명 |
|------|------|------|
| `answer` | string | AI 응답 텍스트 |
| `model` | string | 사용된 모델명 (`claude-haiku-4-5-20251001` 또는 `claude-opus-4-7`) |
| `session_id` | string | 세션 ID (이후 대화에 재사용) |
| `usage` | dict \| null | 토큰 사용량 |

```json
{
  "answer": "종합소득세는 개인이 1년간 얻은 모든 소득을 합산하여...",
  "model": "claude-haiku-4-5-20251001",
  "session_id": "addaa282b8cd46fda54bd798a054d8a6",
  "usage": null
}
```

---

### 3.3 대화 히스토리 조회

```
GET /api/v1/chat/history/{session_id}
```

**Path Parameter**

| 필드 | 타입 | 설명 |
|------|------|------|
| `session_id` | string | 조회할 세션 ID |

**Response**

| 필드 | 타입 | 설명 |
|------|------|------|
| `session_id` | string | 세션 ID |
| `messages` | list | 메시지 목록 (`role`, `content`) |
| `message_count` | int | 총 메시지 수 |

```json
{
  "session_id": "addaa282b8cd46fda54bd798a054d8a6",
  "messages": [
    { "role": "user", "content": "종합소득세가 뭐야?" },
    { "role": "assistant", "content": "종합소득세는..." }
  ],
  "message_count": 2
}
```

---

### 3.4 curl 예시

```bash
# 헬스 체크
curl http://localhost:8000/api/v1/health/

# 채팅 (새 세션)
curl -X POST http://localhost:8000/api/v1/chat/ \
  -H "Content-Type: application/json" \
  -d '{"message": "종합소득세가 뭐야?"}'

# 채팅 (세션 유지)
curl -X POST http://localhost:8000/api/v1/chat/ \
  -H "Content-Type: application/json" \
  -d '{"message": "더 자세히 알려줘", "session_id": "응답에서_받은_session_id"}'

# 히스토리 조회
curl http://localhost:8000/api/v1/chat/history/{session_id}
```

## 4. 인텐트 라우팅

사용자 메시지는 코사인 유사도 기반으로 8개 인텐트 중 하나로 분류됩니다.

| 인텐트 | 설명 | 검색 전략 | LLM 모델 |
|--------|------|---------|---------|
| `TAX_RATE_LOOKUP` | 세율 조회 | 메타데이터 필터 | claude-haiku-4-5-20251001 |
| `EXPENSE_CLASSIFICATION` | 경비 분류 | 하이브리드 + 백엔드 데이터 | claude-opus-4-7 |
| `DEDUCTION_ELIGIBILITY` | 공제/감면 적격 | 하이브리드 | claude-opus-4-7 |
| `PROCEDURE_GUIDE` | 신고 절차 안내 | 하이브리드 | claude-haiku-4-5-20251001 |
| `CONCEPT_EXPLANATION` | 세무 개념 설명 | 벡터 | claude-haiku-4-5-20251001 |
| `CALCULATION` | 세금 계산 | 하이브리드 + 백엔드 데이터 | claude-opus-4-7 |
| `COMPARISON` | 세금 제도 비교 | 멀티 쿼리 | claude-opus-4-7 |
| `GENERAL` | 일반 질문 | 검색 없음 | claude-haiku-4-5-20251001 |

- 유사도 임계값: **0.7** 이상이면 인텐트 매칭, 미만이면 `GENERAL`

## 5. RAG 파이프라인

`RAG_ENABLED=true`로 설정하면 ChromaDB 기반 문서 검색이 활성화됩니다.

**검색 전략**

| 전략 | 동작 |
|------|------|
| `hybrid` | BM25 키워드 검색 + 벡터 검색 → RRF 퓨전 |
| `hybrid_with_be_data` | hybrid + 백엔드 거래 내역/사업자 정보 보강 |
| `vector` | 벡터 유사도 검색만 |
| `metadata_filter` | 메타데이터 필터 중심 검색 |
| `multi_query` | 다각도 쿼리 변환 후 병합 |
| `none` | 검색 생략 (GENERAL 인텐트) |

**문서 인덱싱**

```bash
python -m app.scripts.index_documents
```

> ⚠️ 임베딩 모델을 OpenAI(1536차원)에서 로컬 `BAAI/bge-m3`(1024차원)로 교체했으므로,
> 기존 ChromaDB 벡터는 무효입니다. 최초 1회 `CHROMA_PERSIST_DIRECTORY`(기본 `./data/chroma`)와
> docker 볼륨 `ai-chroma-data`를 비운 뒤 위 인덱싱을 다시 실행하세요.

## 6. 시맨틱 캐시

동일/유사 질문에 LLM 호출 없이 캐시된 응답을 반환합니다.

- 유사도 임계값: `CACHE_THRESHOLD` (기본 0.95)
- 최대 항목 수: `CACHE_MAX_ENTRIES` (기본 10,000)
- TTL: `CACHE_TTL_HOURS` (기본 24시간)

비활성화: `CACHE_ENABLED=false`

## 7. 테스트 실행

```bash
python -m pytest tests/ -v
```

## 8. 프로젝트 구조

```
ai/
├── app/
│   ├── main.py                   # FastAPI 앱 진입점 (lifespan)
│   ├── core/
│   │   ├── config.py             # 설정 (환경변수 + 모델 티어링 + 캐시)
│   │   ├── dependencies.py       # DI + 서비스 초기화
│   │   ├── exceptions.py         # AIServiceError, LLMTimeoutError 등
│   │   └── prompts.py            # 인텐트별 프롬프트 템플릿 (8개)
│   ├── models/
│   │   └── chat.py               # ChatRequest, ChatResponse, ChatHistoryResponse
│   ├── routers/
│   │   ├── chat.py               # POST /chat/, GET /chat/history/{id}
│   │   └── health.py             # GET /health/
│   ├── services/
│   │   ├── chat_service.py       # 메인 RAG 파이프라인 오케스트레이터
│   │   ├── intent_classifier.py  # 코사인 유사도 기반 인텐트 분류
│   │   ├── retrieval_service.py  # 하이브리드 검색 (BM25 + 벡터 + RRF)
│   │   ├── cache_service.py      # 시맨틱 캐시 (임베딩 유사도)
│   │   ├── backend_client.py     # 뱅크앱 API 클라이언트 (거래/사업자)
│   │   ├── embedding_service.py  # 로컬 HuggingFace 임베딩 (BAAI/bge-m3)
│   │   ├── vectorstore.py        # ChromaDB 벡터 저장소
│   │   ├── mapping_service.py    # MCC 코드 → 카테고리/경비율 매핑
│   │   └── document_processor.py # 문서 처리 및 인덱싱
│   ├── utils/
│   │   ├── text_utils.py         # 텍스트 포맷팅
│   │   ├── legal_parser.py       # 법률 문서 파싱
│   │   ├── korean_tokenizer.py   # 한글 토크나이저 (BM25용)
│   │   └── filter_builder.py     # 메타데이터 필터 빌더
│   └── data/
│       └── intents/
│           ├── tax_intents.json  # 8개 인텐트 정의 + 예시 발화
│           ├── mappings.json     # MCC 코드 매핑 테이블
│           └── expense_rates.json # 업종별 경비율
├── http/
│   └── chat.http                 # REST Client 테스트 파일
├── tests/                        # pytest 단위/통합 테스트
├── .env.example                  # 환경 변수 예시
├── requirements.txt              # 의존성
└── venv_guide.md                 # 가상 환경 가이드
```
