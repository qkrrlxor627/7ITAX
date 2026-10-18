"""결제 → 장부 자동 생성 스모크 테스트.

사용: python3 scripts/smoke_payment_ledger.py [BASE_URL]   (기본 http://localhost:8080)

흐름:
  1. 사업자 카드를 가진 시드 유저(이싸피, 01022222222)로 테스트 로그인
  2. 내 카드 중 사업자(BUSINESS) 카드·가맹점 조회
     (장부 자동 생성은 결제 purpose가 아니라 카드 유형이 BUSINESS일 때만 동작한다 — PaymentService.publishPaymentCapturedEvent)
  3. 사업용 결제 승인 → 확정
  4. 장부 목록에 해당 paymentId 항목이 생길 때까지 대기(장부 생성은 @Async AFTER_COMMIT 리스너)

전제:
  - BE 기동 시 테스트 로그인 활성화: TEST_LOGIN_ENABLED=true
    (docker compose 사용 시 .env에 설정, bootRun 사용 시 환경변수로 전달)
  - 시드 데이터 활성화(APP_SEED_ENABLED=true, 기본값) + 빈 DB에서 최초 기동.
    CardDataInitializer는 유저가 없을 때만 김싸피(개인 카드)·이싸피(사업자 카드)를 만들고 카드를 발급한다

표준 라이브러리만 사용한다. 실패하면 단계명과 응답 본문을 출력하고 exit 1.
"""
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

SEED_USER = "01022222222"
LEDGER_WAIT_SECONDS = 30


class SmokeFailure(Exception):
    pass


def call(base, method, path, token=None, body=None, step=""):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method)
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            payload = json.loads(res.read().decode("utf-8") or "null")
    except urllib.error.HTTPError as e:
        raise SmokeFailure(f"[{step}] {method} {path} → HTTP {e.code}\n{e.read().decode('utf-8', 'replace')}")
    except urllib.error.URLError as e:
        raise SmokeFailure(f"[{step}] {method} {path} → 연결 실패: {e.reason}")
    if not isinstance(payload, dict) or "data" not in payload:
        raise SmokeFailure(f"[{step}] {method} {path} → 예상과 다른 응답 형식\n{payload}")
    return payload["data"]


def run(base):
    login = call(base, "POST", "/api/auth/test-login?email=" + urllib.parse.quote(SEED_USER), step="로그인")
    token = login.get("accessToken")
    if not token:
        raise SmokeFailure(f"[로그인] accessToken 없음 (추가 인증 요구?)\n{login}")

    cards = call(base, "GET", "/api/cards", token, step="카드 조회")
    card = next((c for c in cards if c.get("cardType") == "BUSINESS"), None)
    if card is None:
        raise SmokeFailure(
            f"[카드 조회] 시드 유저에게 사업자 카드가 없다(카드 {len(cards)}장). "
            "빈 DB에서 APP_SEED_ENABLED=true로 최초 기동했는지 확인한다")

    merchants = call(base, "GET", "/api/cards/merchants", token, step="가맹점 조회")
    if not merchants:
        raise SmokeFailure("[가맹점 조회] 가맹점 목록이 비어 있다")
    merchant = merchants[0]

    amount = 10000 + int(time.time()) % 1000  # 실행마다 다른 금액 → 장부 항목 식별 보조
    authorized = call(base, "POST", "/api/payments/authorize", token, {
        "cardId": card["id"],
        "amount": amount,
        "merchantId": merchant["merchantId"],
        "merchantName": merchant["merchantName"],
        "paymentMethod": "OFFLINE",
        "purpose": "BUSINESS",
    }, step="결제 승인")
    payment_id = authorized["paymentId"]

    captured = call(base, "POST", f"/api/payments/{payment_id}/capture", token, step="결제 확정")
    if captured.get("status") != "CAPTURED":
        raise SmokeFailure(f"[결제 확정] status가 CAPTURED가 아님\n{captured}")

    deadline = time.time() + LEDGER_WAIT_SECONDS
    while time.time() < deadline:
        page = call(base, "GET", "/api/book-entries?size=50", token, step="장부 조회")
        entry = next((e for e in page.get("content", []) if e.get("paymentId") == payment_id), None)
        if entry:
            print(f"OK: paymentId={payment_id} amount={amount} → bookEntryId={entry['id']} "
                  f"category={entry.get('categoryName')}")
            return
        time.sleep(1)
    raise SmokeFailure(f"[장부 생성] {LEDGER_WAIT_SECONDS}초 안에 paymentId={payment_id} 장부가 생기지 않았다")


def main():
    for stream in (sys.stdout, sys.stderr):  # Windows 콘솔(cp949)에서도 한글 메시지가 깨지지 않게
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")
    base =(sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
    try:
        run(base)
    except SmokeFailure as e:
        print(f"FAIL: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
