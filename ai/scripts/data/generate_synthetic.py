"""
GPT-4o-mini를 사용하여 시드 데이터를 카테고리당 150개로 확장하는 합성 데이터 생성 스크립트.

사용법:
    python -m ai.scripts.data.generate_synthetic
"""

import asyncio
import json
import logging
import os
import sys
from pathlib import Path

from dotenv import load_dotenv
from openai import AsyncOpenAI

# ai/.env 로드
load_dotenv(Path(__file__).parent.parent.parent / ".env")

sys.path.insert(0, str(Path(__file__).parent))
from tax_categories import CATEGORY_DESCRIPTIONS, TAX_CATEGORIES

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)

# ──────────────────────────────────────────────
# 설정
# ──────────────────────────────────────────────
TARGET_PER_CATEGORY = 150
MODEL = "gpt-4o-mini"
MAX_RETRIES = 3
BATCH_SIZE_PER_REQUEST = 30  # 한 번에 생성 요청할 문장 수
INTERMEDIATE_SAVE_INTERVAL = 5  # 카테고리 N개마다 중간 저장

SCRIPT_DIR = Path(__file__).parent
SEED_DATA_PATH = SCRIPT_DIR / "seed_data.json"
OUTPUT_PATH = SCRIPT_DIR / "synthetic_data.json"
INTERMEDIATE_PATH = SCRIPT_DIR / "synthetic_data_partial.json"

client = AsyncOpenAI(
    api_key=os.getenv("GMS_API_KEY"),
    base_url=os.getenv("GMS_BASE_URL"),
)


def build_prompt(category: str, examples: list[str], count: int) -> str:
    """합성 데이터 생성용 프롬프트를 구성한다."""
    description = CATEGORY_DESCRIPTIONS.get(category, "")
    example_text = "\n".join(f"- {ex}" for ex in examples)

    return f"""당신은 세금 분류 학습용 데이터를 생성하는 전문가입니다.

## 세목 정보
- 세목명: {category}
- 설명: {description}
- 대상: 1인 IT 개발자 (프리랜서/소규모 사업자)

## 기존 예시
{example_text}

## 작업
위 예시와 **같은 세목**에 해당하지만 **표현이 다른** 새로운 문장을 {count}개 생성하세요.

## 규칙
1. 실제 사용자가 채팅으로 입력할 법한 구어체 문장
2. 문장 길이는 10~50자 사이
3. 기존 예시와 동일하거나 너무 유사한 문장은 제외
4. 금액, 상호명, 날짜 등을 다양하게 변형
5. 줄임말, 비격식체 등도 포함

## 출력 형식
JSON 배열로만 응답하세요. 다른 텍스트 없이 JSON만 출력합니다.
["문장1", "문장2", ...]"""


async def generate_batch(
    category: str,
    examples: list[str],
    count: int,
) -> list[str]:
    """하나의 배치에 대해 합성 문장을 생성한다. 실패 시 최대 3회 재시도."""
    prompt = build_prompt(category, examples, count)

    for attempt in range(1, MAX_RETRIES + 1):
        try:
            response = await client.chat.completions.create(
                model=MODEL,
                messages=[
                    {
                        "role": "system",
                        "content": "JSON 배열 형식으로만 응답하세요.",
                    },
                    {"role": "user", "content": prompt},
                ],
                temperature=0.9,
                max_tokens=4096,
            )

            content = response.choices[0].message.content.strip()

            # JSON 코드 블록 처리
            if content.startswith("```"):
                content = content.split("\n", 1)[1]
                content = content.rsplit("```", 1)[0].strip()

            sentences: list[str] = json.loads(content)

            if not isinstance(sentences, list):
                raise ValueError("응답이 리스트가 아닙니다.")

            # 문자열만 필터링
            sentences = [s for s in sentences if isinstance(s, str) and s.strip()]
            logger.info(
                f"  [{category}] 배치 생성 완료: {len(sentences)}개 (시도 {attempt})"
            )
            return sentences

        except (json.JSONDecodeError, ValueError) as e:
            logger.warning(
                f"  [{category}] JSON 파싱 실패 (시도 {attempt}/{MAX_RETRIES}): {e}"
            )
            if attempt == MAX_RETRIES:
                logger.error(f"  [{category}] 최대 재시도 초과, 빈 결과 반환")
                return []
        except Exception as e:
            logger.error(
                f"  [{category}] API 호출 오류 (시도 {attempt}/{MAX_RETRIES}): {e}"
            )
            if attempt == MAX_RETRIES:
                return []
            await asyncio.sleep(2 ** attempt)

    return []


async def generate_for_category(
    category: str,
    seed_sentences: list[str],
) -> list[str]:
    """한 카테고리에 대해 목표 수량까지 합성 데이터를 생성한다."""
    current_count = len(seed_sentences)
    needed = TARGET_PER_CATEGORY - current_count

    if needed <= 0:
        logger.info(f"[{category}] 이미 {current_count}개 보유, 스킵")
        return seed_sentences[:TARGET_PER_CATEGORY]

    logger.info(
        f"[{category}] 시드 {current_count}개 -> 목표 {TARGET_PER_CATEGORY}개 "
        f"(추가 필요: {needed}개)"
    )

    all_generated: list[str] = []
    remaining = needed

    while remaining > 0:
        batch_count = min(BATCH_SIZE_PER_REQUEST, remaining)
        # 시드 + 이미 생성된 최근 문장을 예시로 제공
        recent_examples = seed_sentences + all_generated[-20:]
        batch = await generate_batch(category, recent_examples, batch_count)
        all_generated.extend(batch)
        remaining -= len(batch)

        if not batch:
            logger.warning(f"[{category}] 빈 배치 반환, 남은 수량: {remaining}")
            break

    result = seed_sentences + all_generated
    logger.info(f"[{category}] 최종 {len(result)}개 확보")
    return result


def save_intermediate(data: dict[str, list[str]], path: Path) -> None:
    """중간 결과를 저장한다."""
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    logger.info(f"중간 저장 완료: {path}")


async def main() -> None:
    """메인 실행 함수."""
    # 시드 데이터 로드
    if not SEED_DATA_PATH.exists():
        logger.error(f"시드 데이터 없음: {SEED_DATA_PATH}")
        sys.exit(1)

    with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
        seed_data: dict[str, list[str]] = json.load(f)

    # 카테고리 검증
    missing = [c for c in TAX_CATEGORIES if c not in seed_data]
    if missing:
        logger.error(f"시드 데이터에 없는 카테고리: {missing}")
        sys.exit(1)

    logger.info(f"시드 데이터 로드 완료: {len(seed_data)}개 카테고리")

    # GMS API 설정 확인
    if not os.getenv("GMS_API_KEY"):
        logger.error("GMS_API_KEY 환경변수가 설정되지 않았습니다.")
        sys.exit(1)
    if not os.getenv("GMS_BASE_URL"):
        logger.error("GMS_BASE_URL 환경변수가 설정되지 않았습니다.")
        sys.exit(1)

    synthetic_data: dict[str, list[str]] = {}

    for idx, category in enumerate(TAX_CATEGORIES, 1):
        logger.info(f"\n{'='*50}")
        logger.info(f"[{idx}/{len(TAX_CATEGORIES)}] {category} 처리 중...")
        logger.info(f"{'='*50}")

        seed_sentences = seed_data.get(category, [])
        result = await generate_for_category(category, seed_sentences)
        synthetic_data[category] = result

        # 중간 저장
        if idx % INTERMEDIATE_SAVE_INTERVAL == 0:
            save_intermediate(synthetic_data, INTERMEDIATE_PATH)

    # 최종 저장
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(synthetic_data, f, ensure_ascii=False, indent=2)
    logger.info(f"\n최종 저장 완료: {OUTPUT_PATH}")

    # 통계 출력
    total = sum(len(v) for v in synthetic_data.values())
    logger.info(f"\n{'='*50}")
    logger.info("생성 결과 요약")
    logger.info(f"{'='*50}")
    for cat in TAX_CATEGORIES:
        count = len(synthetic_data.get(cat, []))
        logger.info(f"  {cat}: {count}개")
    logger.info(f"  총계: {total}개")


if __name__ == "__main__":
    asyncio.run(main())
