"""
세목 분류 모델 파인튜닝 스크립트.

klue/roberta-base를 20개 세목 레이블로 파인튜닝한다.
RTX 4070 8GB 기준 최적화 (fp16, batch=32).

사용법:
    cd ai/
    python -m scripts.train_tax_classifier
"""

import json
from pathlib import Path

import numpy as np
import torch
from sklearn.metrics import accuracy_score, f1_score
from torch.utils.data import Dataset
from transformers import (
    AutoModelForSequenceClassification,
    AutoTokenizer,
    EarlyStoppingCallback,
    Trainer,
    TrainingArguments,
)

BASE_DIR = Path(__file__).parent.parent  # ai/
DATA_DIR = BASE_DIR / "data"
MODEL_OUTPUT_DIR = BASE_DIR / "models" / "tax_classifier"

MODEL_NAME = "klue/roberta-base"
NUM_LABELS = 20
MAX_LENGTH = 128


class TaxDataset(Dataset):
    """세목 분류용 데이터셋. JSON 포맷: [{"text": str, "label": int}]"""

    def __init__(self, data_path: Path, tokenizer) -> None:
        with open(data_path, encoding="utf-8") as f:
            raw_data = json.load(f)

        self.texts: list[str] = [item["text"] for item in raw_data]
        self.labels: list[int] = [item["label"] for item in raw_data]
        self.encodings = tokenizer(
            self.texts,
            truncation=True,
            padding="max_length",
            max_length=MAX_LENGTH,
            return_tensors="pt",
        )

    def __len__(self) -> int:
        return len(self.labels)

    def __getitem__(self, idx: int) -> dict:
        item = {k: v[idx] for k, v in self.encodings.items()}
        item["labels"] = torch.tensor(self.labels[idx], dtype=torch.long)
        return item


def compute_metrics(eval_pred) -> dict[str, float]:
    """Macro-F1 + Accuracy 계산."""
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=-1)
    return {
        "f1": f1_score(labels, preds, average="macro", zero_division=0),
        "accuracy": accuracy_score(labels, preds),
    }


def main() -> None:
    MODEL_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print(f"[INFO] 모델: {MODEL_NAME}")
    device_info = (
        f"{torch.cuda.get_device_name(0)}" if torch.cuda.is_available() else "CPU"
    )
    print(f"[INFO] GPU: {torch.cuda.is_available()} ({device_info})")

    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)

    print("[INFO] 데이터셋 로드...")
    train_dataset = TaxDataset(DATA_DIR / "train.json", tokenizer)
    val_dataset = TaxDataset(DATA_DIR / "val.json", tokenizer)
    print(f"  Train: {len(train_dataset)}, Val: {len(val_dataset)}")

    model = AutoModelForSequenceClassification.from_pretrained(
        MODEL_NAME, num_labels=NUM_LABELS
    )

    training_args = TrainingArguments(
        output_dir=str(MODEL_OUTPUT_DIR),
        num_train_epochs=5,
        per_device_train_batch_size=32,
        per_device_eval_batch_size=64,
        warmup_ratio=0.1,
        learning_rate=3e-5,
        weight_decay=0.01,
        eval_strategy="epoch",
        save_strategy="epoch",
        metric_for_best_model="f1",
        greater_is_better=True,
        load_best_model_at_end=True,
        fp16=torch.cuda.is_available(),
        logging_steps=50,
        report_to="none",
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        compute_metrics=compute_metrics,
        callbacks=[EarlyStoppingCallback(early_stopping_patience=2)],
    )

    print("\n[INFO] 학습 시작...")
    trainer.train()

    print("\n[INFO] 최종 검증 평가...")
    metrics = trainer.evaluate()
    print(f"  Val Macro-F1: {metrics['eval_f1']:.4f}")
    print(f"  Val Accuracy: {metrics['eval_accuracy']:.4f}")

    if metrics["eval_f1"] >= 0.85:
        print("  [PASS] Macro-F1 >= 0.85 달성!")
    else:
        print(f"  [FAIL] Macro-F1 목표 미달. 현재: {metrics['eval_f1']:.4f}")

    trainer.save_model(str(MODEL_OUTPUT_DIR))
    tokenizer.save_pretrained(str(MODEL_OUTPUT_DIR))
    print(f"\n[INFO] 모델 저장: {MODEL_OUTPUT_DIR}")


if __name__ == "__main__":
    main()
