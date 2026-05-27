import json

import pytest

from app.services.mapping_service import ExpenseRate, MappingService, MccInfo


@pytest.fixture
def data_dir(tmp_path):
    """테스트용 JSON 파일이 담긴 임시 디렉토리."""
    mappings_data = {
        "mcc_to_category": {
            "5812": {
                "category": "음식점",
                "expense_type": "접대비",
                "deductible": "conditional",
                "industry_code": "701101",
            }
        },
        "expense_rate_lookup": {
            "701101": {
                "simple_rate": 90.3,
                "standard_rate": 19.8,
                "industry": "한식 음식점업",
            }
        },
    }
    (tmp_path / "mappings.json").write_text(
        json.dumps(mappings_data), encoding="utf-8"
    )

    rates_data = {
        "rates": [
            {
                "industry_code": "701101",
                "industry_name": "한식 음식점업",
                "simple_expense_rate": 90.3,
                "standard_expense_rate": 19.8,
                "self_employed_rate": None,
                "note": "",
            }
        ],
        "metadata": {"source": "test", "year": 2025, "total_entries": 1},
    }
    (tmp_path / "expense_rates.json").write_text(
        json.dumps(rates_data), encoding="utf-8"
    )

    return str(tmp_path)


class TestLoadMccData:
    def test_load_mcc_data(self, data_dir):
        """정상 로드 후 lookup_mcc 결과를 검증한다."""
        service = MappingService(data_dir=data_dir)
        service.load()

        result = service.lookup_mcc("5812")

        assert result is not None
        assert isinstance(result, MccInfo)
        assert result.mcc == "5812"
        assert result.category == "음식점"
        assert result.expense_type == "접대비"
        assert result.deductible == "conditional"
        assert result.industry_code == "701101"


class TestLoadExpenseRates:
    def test_load_expense_rates(self, data_dir):
        """expense_rates.json 로드 후 lookup_expense_rate 결과를 검증한다."""
        service = MappingService(data_dir=data_dir)
        service.load()

        result = service.lookup_expense_rate("701101")

        assert result is not None
        assert isinstance(result, ExpenseRate)
        assert result.industry_code == "701101"
        assert result.industry_name == "한식 음식점업"
        assert result.simple_rate == 90.3
        assert result.standard_rate == 19.8


class TestLookupMccNotFound:
    def test_lookup_mcc_not_found(self, data_dir):
        """등록되지 않은 MCC 코드는 None을 반환한다."""
        service = MappingService(data_dir=data_dir)
        service.load()

        result = service.lookup_mcc("9999")

        assert result is None


class TestLookupExpenseRateNotFound:
    def test_lookup_expense_rate_not_found(self, data_dir):
        """등록되지 않은 업종 코드는 None을 반환한다."""
        service = MappingService(data_dir=data_dir)
        service.load()

        result = service.lookup_expense_rate("000000")

        assert result is None


class TestClassifyTransactionKnownMcc:
    def test_classify_transaction_known_mcc(self, data_dir):
        """알려진 MCC로 classify_transaction 호출 시 카테고리/경비율을 포함해야 한다."""
        service = MappingService(data_dir=data_dir)
        service.load()

        result = service.classify_transaction("5812")

        assert result["mcc"] == "5812"
        assert result["category"] == "음식점"
        assert result["expense_type"] == "접대비"
        assert result["deductible"] == "conditional"
        assert result["industry_name"] == "한식 음식점업"
        assert result["simple_rate"] == 90.3
        assert result["standard_rate"] == 19.8


class TestClassifyTransactionUnknownMcc:
    def test_classify_transaction_unknown_mcc(self, data_dir):
        """미등록 MCC로 classify_transaction 호출 시 '미분류'를 반환한다."""
        service = MappingService(data_dir=data_dir)
        service.load()

        result = service.classify_transaction("0000")

        assert result["mcc"] == "0000"
        assert result["category"] == "미분류"
        assert result["expense_type"] == "미분류"
        assert result["deductible"] is False
        assert result["industry_name"] is None
        assert result["simple_rate"] is None
        assert result["standard_rate"] is None


class TestLoadMissingFiles:
    def test_load_missing_files(self, tmp_path):
        """파일이 없어도 서비스가 크래시 없이 빈 dict를 유지한다."""
        service = MappingService(data_dir=str(tmp_path))
        # 파일 없음 — 예외 발생 없이 완료되어야 한다
        service.load()

        assert service._mcc_map == {}
        assert service._expense_rates == {}
        assert service.lookup_mcc("5812") is None
        assert service.lookup_expense_rate("701101") is None


class TestClassifyTransactionChainLookup:
    def test_classify_transaction_chain_lookup(self, tmp_path):
        """MCC -> industry_code -> ExpenseRate 체인 룩업이 올바르게 작동한다."""
        # mappings.json에는 expense_rate_lookup 없이 mcc_to_category만 포함
        mappings_data = {
            "mcc_to_category": {
                "4121": {
                    "category": "택시",
                    "expense_type": "여비교통비",
                    "deductible": True,
                    "industry_code": "492101",
                }
            },
            "expense_rate_lookup": {},
        }
        (tmp_path / "mappings.json").write_text(
            json.dumps(mappings_data), encoding="utf-8"
        )

        # expense_rates.json에는 해당 업종 코드 포함
        rates_data = {
            "rates": [
                {
                    "industry_code": "492101",
                    "industry_name": "일반택시 운송업",
                    "simple_expense_rate": 79.4,
                    "standard_expense_rate": 18.1,
                    "self_employed_rate": None,
                    "note": "",
                }
            ],
            "metadata": {"source": "test", "year": 2025, "total_entries": 1},
        }
        (tmp_path / "expense_rates.json").write_text(
            json.dumps(rates_data), encoding="utf-8"
        )

        service = MappingService(data_dir=str(tmp_path))
        service.load()

        result = service.classify_transaction("4121")

        # MCC -> MccInfo -> ExpenseRate 체인이 정상 작동
        assert result["mcc"] == "4121"
        assert result["category"] == "택시"
        assert result["expense_type"] == "여비교통비"
        assert result["deductible"] is True
        assert result["industry_name"] == "일반택시 운송업"
        assert result["simple_rate"] == 79.4
        assert result["standard_rate"] == 18.1

    def test_chain_lookup_mcc_without_expense_rate(self, tmp_path):
        """MccInfo는 있으나 매칭되는 ExpenseRate가 없을 때 rate 필드는 None이다."""
        mappings_data = {
            "mcc_to_category": {
                "5812": {
                    "category": "음식점",
                    "expense_type": "접대비",
                    "deductible": "conditional",
                    "industry_code": "999999",  # 존재하지 않는 업종 코드
                }
            },
            "expense_rate_lookup": {},
        }
        (tmp_path / "mappings.json").write_text(
            json.dumps(mappings_data), encoding="utf-8"
        )
        (tmp_path / "expense_rates.json").write_text(
            json.dumps({"rates": [], "metadata": {}}), encoding="utf-8"
        )

        service = MappingService(data_dir=str(tmp_path))
        service.load()

        result = service.classify_transaction("5812")

        assert result["category"] == "음식점"
        assert result["industry_name"] is None
        assert result["simple_rate"] is None
        assert result["standard_rate"] is None
