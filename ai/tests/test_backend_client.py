from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

from app.core.config import Settings
from app.core.exceptions import BackendClientError
from app.services.backend_client import BackendClient, BusinessInfo


def _make_settings() -> Settings:
    return Settings(
        gms_api_key="test-key",
        gms_base_url="http://fake-llm",
        llm_model="test-model",
    )


@pytest.fixture
def backend_client():
    with patch("app.services.backend_client.httpx.AsyncClient") as mock_cls:
        mock_http = AsyncMock()
        mock_cls.return_value = mock_http
        client = BackendClient(settings=_make_settings())
        yield client


# ---------------------------------------------------------------------------
# get_transactions 오류 경로
# ---------------------------------------------------------------------------

class TestGetTransactions:
    async def test_returns_empty_on_404(self, backend_client: BackendClient):
        """404 응답 시 빈 리스트를 반환한다."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "404", request=MagicMock(), response=MagicMock(status_code=404)
        )
        backend_client.client.get = AsyncMock(return_value=mock_response)

        result = await backend_client.get_transactions(user_id="user-1")

        assert result == []

    async def test_raises_on_500(self, backend_client: BackendClient):
        """500 응답 시 BackendClientError를 발생시킨다."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "500", request=MagicMock(), response=MagicMock(status_code=500)
        )
        backend_client.client.get = AsyncMock(return_value=mock_response)

        with pytest.raises(BackendClientError):
            await backend_client.get_transactions(user_id="user-1")

    async def test_raises_on_timeout(self, backend_client: BackendClient):
        """타임아웃 시 BackendClientError를 발생시킨다."""
        backend_client.client.get = AsyncMock(
            side_effect=httpx.TimeoutException("timeout")
        )

        with pytest.raises(BackendClientError):
            await backend_client.get_transactions(user_id="user-1")


# ---------------------------------------------------------------------------
# get_business_info 실패/성공 경로
# ---------------------------------------------------------------------------

class TestGetBusinessInfo:
    async def test_returns_none_on_404(self, backend_client: BackendClient):
        """404 응답 시 None을 반환한다."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "404", request=MagicMock(), response=MagicMock(status_code=404)
        )
        backend_client.client.get = AsyncMock(return_value=mock_response)

        result = await backend_client.get_business_info(user_id="user-1")

        assert result is None

    async def test_raises_on_timeout(self, backend_client: BackendClient):
        """타임아웃 시 BackendClientError를 발생시킨다."""
        backend_client.client.get = AsyncMock(
            side_effect=httpx.TimeoutException("timeout")
        )

        with pytest.raises(BackendClientError):
            await backend_client.get_business_info(user_id="user-1")

    async def test_returns_business_info_on_success(self, backend_client: BackendClient):
        """정상 응답 시 BusinessInfo를 반환한다."""
        mock_response = MagicMock()
        mock_response.raise_for_status = MagicMock()
        mock_response.json.return_value = {
            "business_type": "개인사업자",
            "industry_code": "012345",
            "tax_type": "일반과세",
            "establishment_date": "2020-01-01",
        }
        backend_client.client.get = AsyncMock(return_value=mock_response)

        result = await backend_client.get_business_info(user_id="user-1")

        assert isinstance(result, BusinessInfo)
        assert result.business_type == "개인사업자"


# ---------------------------------------------------------------------------
# close 종료
# ---------------------------------------------------------------------------

class TestClose:
    async def test_close_calls_client_aclose(self):
        """close() 호출 시 내부 httpx 클라이언트의 aclose()가 호출된다."""
        with patch("app.services.backend_client.httpx.AsyncClient") as mock_cls:
            mock_http = AsyncMock()
            mock_cls.return_value = mock_http
            client = BackendClient(settings=_make_settings())
            await client.close()

        mock_http.aclose.assert_called_once()

    async def test_close_idempotent(self):
        """close() 중복 호출 시 예외가 발생하지 않는다."""
        with patch("app.services.backend_client.httpx.AsyncClient") as mock_cls:
            mock_http = AsyncMock()
            mock_cls.return_value = mock_http
            client = BackendClient(settings=_make_settings())
            await client.close()
            await client.close()  # 두 번째 호출도 예외 없이 통과

        assert mock_http.aclose.call_count == 2
