#!/usr/bin/env bash
# k6 부하 테스트 실행 (k6 설치 불필요 — Docker 이미지 사용). 시나리오·기준: docs/PERF_TEST.md
#
#   ./scripts/perf/run.sh smoke                                   # 로컬 compose 스택(backend 컨테이너)
#   ./scripts/perf/run.sh load  https://perf.example.com          # 배포(성능측정) 환경
#   CONTENTION=1 ./scripts/perf/run.sh load                        # 한 계좌 동시 결제 경합
#   USERS=100 VUS=100 ./scripts/perf/run.sh load                   # 사용자 수·동시 사용자 조정
#
# 결과: scripts/perf/results/<프로필>-<시각>.json (k6 요약), 콘솔 요약. 합격 기준 위반 시 exit 99
set -euo pipefail

PROFILE="${1:-smoke}"
TARGET="${2:-}"
K6_IMAGE="grafana/k6:0.54.0"
ROOT="$(cd "$(dirname "$0")" && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"
SUFFIX=""; [ "${CONTENTION:-0}" = "1" ] && SUFFIX="-contention"
mkdir -p "$ROOT/results"

if [ -z "$TARGET" ]; then
  # 로컬: compose 네트워크 안에서 backend:8080을 직접 친다(호스트 포트 포워딩 오버헤드 제외)
  NET_ARGS=(--network 7itax_default)
  BASE_URL="http://backend:8080"
else
  NET_ARGS=()
  BASE_URL="$TARGET"
fi

# Git Bash(Windows)가 /scripts 같은 컨테이너 경로를 C:/Program Files/Git/... 로 바꾸지 않게 한다
export MSYS_NO_PATHCONV=1
MOUNT_SRC="$ROOT"
if command -v cygpath >/dev/null 2>&1; then MOUNT_SRC="$(cygpath -m "$ROOT")"; fi

echo "▶ k6 PROFILE=$PROFILE BASE_URL=$BASE_URL USERS=${USERS:-50} VUS=${VUS:-기본} CONTENTION=${CONTENTION:-0}"
docker run --rm -i "${NET_ARGS[@]}" \
  -v "$MOUNT_SRC:/perf" \
  -e PROFILE="$PROFILE" -e BASE_URL="$BASE_URL" \
  -e USERS="${USERS:-50}" -e VUS="${VUS:-}" -e DURATION="${DURATION:-}" \
  -e CONTENTION="${CONTENTION:-0}" -e PHONE_PREFIX="${PHONE_PREFIX:-0107700}" \
  "$K6_IMAGE" run --summary-export "/perf/results/${PROFILE}${SUFFIX}-${STAMP}.json" /perf/tax7i.js
