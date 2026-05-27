FROM python:3.11-slim AS builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

FROM python:3.11-slim
WORKDIR /app
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# 로컬 임베딩 모델을 빌드 시 미리 다운로드해 이미지에 캐시한다.
# (런타임 HuggingFace 네트워크 의존 제거 + 컨테이너 재생성 시 재다운로드 방지)
# 주의: bge-m3는 ~2GB라 이미지 크기가 그만큼 증가한다.
ARG EMBEDDING_MODEL=BAAI/bge-m3
ENV HF_HOME=/app/.hf_cache
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('${EMBEDDING_MODEL}')"

COPY models/ models/
COPY resources/ resources/
COPY app/ app/
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
