"""에뮬레이터 1대로 QR 결제를 확인할 때 '가맹점' 역할을 대신한다.

앱의 [QR 결제] 탭은 결제 QR(토큰)을 띄우고 상태를 폴링한다. 실제로는 가맹점이 QR을 스캔해
POST /api/payments/qr/token/{token}/confirm 을 부르는데, 이 스크립트가 Redis에서 가장 최근 토큰을 찾아 대신 확정한다.

전제: docker compose 스택(redis·backend) 기동, TEST_LOGIN_ENABLED=true(가맹점 계정 간편 로그인용)
사용: python scripts/confirm_latest_qr.py [BE 주소, 기본 http://localhost:8080]
"""
import json
import subprocess
import sys
import urllib.error
import urllib.request

REDIS_CONTAINER = "tax7i-redis"
REDIS_PASSWORD = "ssafy"
MERCHANT_PHONE = "01099999999"


def redis(*args):
    cmd = ["docker", "exec", REDIS_CONTAINER, "redis-cli", "-a", REDIS_PASSWORD, "--no-auth-warning", *args]
    return subprocess.run(cmd, capture_output=True, text=True, check=True).stdout.strip()


def post(base, path, body=None, token=None):
    req = urllib.request.Request(base + path, data=json.dumps(body or {}).encode("utf-8"), method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=15) as res:
        return json.loads(res.read().decode("utf-8"))["data"]


def main():
    base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
    keys = redis("--scan", "--pattern", "qr-pay:*").split()
    if not keys:
        print("확정할 QR 토큰이 없다. 앱에서 [QR 결제] 탭을 먼저 연다(토큰 유효 5분)")
        return 1
    # TTL이 가장 많이 남은 = 가장 최근에 만든 토큰
    latest = max(keys, key=lambda k: int(redis("TTL", k)))
    token = latest.split(":", 1)[1]

    merchant = post(base, "/api/auth/simple-login", {"phoneNumber": MERCHANT_PHONE, "name": "가맹점"})["accessToken"]
    try:
        result = post(base, f"/api/payments/qr/token/{token}/confirm", token=merchant)
    except urllib.error.HTTPError as e:
        print(f"확정 실패 HTTP {e.code}: {e.read().decode('utf-8', 'replace')}")
        return 1
    print(f"OK: token={token} paymentId={result.get('paymentId')} amount={result.get('amount')} status={result.get('status')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
