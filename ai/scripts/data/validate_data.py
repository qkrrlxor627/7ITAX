"""
데이터 검증 및 분할 스크립트.
중복 제거 (ROUGE-L > 0.7) + 레이블 분포 확인 + Train/Val/Test 층화 분할.

사용법:
    python -m ai.scripts.data.validate_data
"""

import json
import logging
import sys
from collections import Counter
from pathlib import Path

from rouge_score import rouge_scorer

sys.path.insert(0, str(Path(__file__).parent))
from tax_categories import TAX_CATEGORIES, get_label_id

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)

# ──────────────────────────────────────────────
# 설정
# ──────────────────────────────────────────────
ROUGE_L_THRESHOLD = 0.7
RECENT_WINDOW = 50  # 중복 비교 시 최근 N개와만 비교
TRAIN_RATIO = 0.70
VAL_RATIO = 0.15
TEST_RATIO = 0.15

SCRIPT_DIR = Path(__file__).parent
SEED_DATA_PATH = SCRIPT_DIR / "seed_data.json"
SYNTHETIC_DATA_PATH = SCRIPT_DIR / "synthetic_data.json"
DATA_DIR = SCRIPT_DIR.parent.parent / "data"
TRAIN_PATH = DATA_DIR / "train.json"
VAL_PATH = DATA_DIR / "val.json"
TEST_PATH = DATA_DIR / "test.json"


def load_data() -> dict[str, list[str]]:
    """시드 데이터 + 합성 데이터를 통합 로드한다."""
    merged: dict[str, list[str]] = {cat: [] for cat in TAX_CATEGORIES}

    # 시드 데이터 로드
    if SEED_DATA_PATH.exists():
        with open(SEED_DATA_PATH, "r", encoding="utf-8") as f:
            seed: dict[str, list[str]] = json.load(f)
        for cat, sentences in seed.items():
            if cat in merged:
                merged[cat].extend(sentences)
        logger.info(f"시드 데이터 로드: {sum(len(v) for v in seed.values())}개")
    else:
        logger.warning(f"시드 데이터 없음: {SEED_DATA_PATH}")

    # 합성 데이터 로드
    if SYNTHETIC_DATA_PATH.exists():
        with open(SYNTHETIC_DATA_PATH, "r", encoding="utf-8") as f:
            synthetic: dict[str, list[str]] = json.load(f)
        for cat, sentences in synthetic.items():
            if cat in merged:
                merged[cat].extend(sentences)
        logger.info(f"합성 데이터 로드: {sum(len(v) for v in synthetic.values())}개")
    else:
        logger.warning(f"합성 데이터 없음: {SYNTHETIC_DATA_PATH}")

    return merged


def deduplicate_rouge_l(
    sentences: list[str],
    threshold: float = ROUGE_L_THRESHOLD,
    window: int = RECENT_WINDOW,
) -> list[str]:
    """ROUGE-L 기반으로 같은 카테고리 내 중복 문장을 제거한다."""
    if not sentences:
        return []

    scorer = rouge_scorer.RougeScorer(["rougeL"], use_stemmer=False)
    unique: list[str] = []

    for sentence in sentences:
        sentence = sentence.strip()
        if not sentence:
            continue

        is_duplicate = False
        # 최근 window개와만 비교하여 성능 확보
        compare_targets = unique[-window:]

        for existing in compare_targets:
            score = scorer.score(existing, sentence)
            if score["rougeL"].fmeasure > threshold:
                is_duplicate = True
                break

        if not is_duplicate:
            unique.append(sentence)

    return unique


def print_distribution(data: dict[str, list[str]], title: str) -> None:
    """카테고리별 분포를 출력한다."""
    logger.info(f"\n{'='*50}")
    logger.info(f"{title}")
    logger.info(f"{'='*50}")

    total = 0
    for cat in TAX_CATEGORIES:
        count = len(data.get(cat, []))
        total += count
        bar = "#" * (count // 5)
        logger.info(f"  {cat:12s}: {count:4d}개 {bar}")
    logger.info(f"  {'총계':12s}: {total:4d}개")


def stratified_split(
    data: dict[str, list[str]],
) -> tuple[list[dict], list[dict], list[dict]]:
    """카테고리별 층화 분할 (Train/Val/Test)."""
    import random

    random.seed(42)

    train_items: list[dict] = []
    val_items: list[dict] = []
    test_items: list[dict] = []

    for cat in TAX_CATEGORIES:
        sentences = data.get(cat, [])
        label_id = get_label_id(cat)

        # 셔플
        shuffled = list(sentences)
        random.shuffle(shuffled)

        n = len(shuffled)
        train_end = int(n * TRAIN_RATIO)
        val_end = train_end + int(n * VAL_RATIO)

        train_split = shuffled[:train_end]
        val_split = shuffled[train_end:val_end]
        test_split = shuffled[val_end:]

        for s in train_split:
            train_items.append({"text": s, "label": label_id})
        for s in val_split:
            val_items.append({"text": s, "label": label_id})
        for s in test_split:
            test_items.append({"text": s, "label": label_id})

    # 각 split 내에서도 셔플
    random.shuffle(train_items)
    random.shuffle(val_items)
    random.shuffle(test_items)

    return train_items, val_items, test_items


def validate_splits(
    train: list[dict],
    val: list[dict],
    test: list[dict],
) -> None:
    """분할 결과의 레이블 분포를 검증한다."""
    for name, split in [("Train", train), ("Val", val), ("Test", test)]:
        counter = Counter(item["label"] for item in split)
        logger.info(f"\n[{name}] 총 {len(split)}개")
        for label_id in sorted(counter.keys()):
            cat = TAX_CATEGORIES[label_id]
            logger.info(f"  {label_id:2d}. {cat}: {counter[label_id]}개")


def main() -> None:
    """메인 실행 함수."""
    # 1. 데이터 로드
    logger.info("1단계: 데이터 로드")
    data = load_data()
    print_distribution(data, "통합 데이터 분포 (중복 제거 전)")

    # 2. ROUGE-L 기반 중복 제거
    logger.info("\n2단계: ROUGE-L 기반 중복 제거 (임계값: {})".format(ROUGE_L_THRESHOLD))
    deduped: dict[str, list[str]] = {}
    total_removed = 0

    for cat in TAX_CATEGORIES:
        original = data.get(cat, [])
        unique = deduplicate_rouge_l(original)
        removed = len(original) - len(unique)
        total_removed += removed
        deduped[cat] = unique

        if removed > 0:
            logger.info(f"  {cat}: {len(original)} -> {len(unique)} ({removed}개 제거)")

    logger.info(f"  총 {total_removed}개 중복 제거")
    print_distribution(deduped, "중복 제거 후 분포")

    # 3. 층화 분할
    logger.info("\n3단계: 층화 분할 (Train {:.0%} / Val {:.0%} / Test {:.0%})".format(
        TRAIN_RATIO, VAL_RATIO, TEST_RATIO
    ))
    train, val, test = stratified_split(deduped)

    # 4. 분할 결과 검증
    logger.info("\n4단계: 분할 결과 검증")
    validate_splits(train, val, test)

    # 5. 저장
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    for path, split_data, name in [
        (TRAIN_PATH, train, "Train"),
        (VAL_PATH, val, "Val"),
        (TEST_PATH, test, "Test"),
    ]:
        with open(path, "w", encoding="utf-8") as f:
            json.dump(split_data, f, ensure_ascii=False, indent=2)
        logger.info(f"저장 완료: {path} ({len(split_data)}개)")

    logger.info("\n모든 작업 완료!")


if __name__ == "__main__":
    main()
