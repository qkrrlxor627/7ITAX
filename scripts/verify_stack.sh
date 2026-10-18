#!/usr/bin/env bash
# 컨테이너 빌드 → 기동 → healthy 대기 → 결제·장부 스모크.
# 성공하면 조용히 한 줄, 실패하면 원인과 로그를 자세히 출력한다.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "FAIL: .env 없음 — cp .env.example .env 후 AES_ENCRYPTION_KEY, JWT_SECRET을 채울 것" >&2
  exit 1
fi

docker compose up -d --build postgres redis backend >/dev/null

status="starting"
for _ in $(seq 1 60); do
  status=$(docker inspect -f '{{.State.Health.Status}}' tax7i-backend 2>/dev/null || echo "missing")
  [[ "$status" == "healthy" ]] && break
  [[ "$status" == "missing" || "$status" == "exited" ]] && break
  sleep 5
done

if [[ "$status" != "healthy" ]]; then
  echo "FAIL: backend 상태=$status (healthy가 아님)" >&2
  docker compose logs --tail=200 backend >&2
  exit 1
fi

if ! out=$(python3 scripts/smoke_payment_ledger.py http://localhost:8080 2>&1); then
  echo "FAIL: 스모크 테스트 실패" >&2
  echo "$out" >&2
  docker compose logs --tail=100 backend >&2
  exit 1
fi

echo "OK: backend healthy + 결제→장부 스모크 통과"
