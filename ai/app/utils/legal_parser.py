# ai/app/utils/legal_parser.py

import re
from dataclasses import dataclass, field


@dataclass
class LegalChunk:
    """법률 구조 보존 청크."""
    content: str
    metadata: dict = field(default_factory=dict)


# 법률 구조 식별 정규식 패턴
LEGAL_PATTERNS = {
    "part":         re.compile(r"제(\d+)편\s+(.+)"),
    "chapter":      re.compile(r"제(\d+)장\s+(.+)"),
    "section":      re.compile(r"제(\d+)절\s+(.+)"),
    "article":      re.compile(r"제(\d+)조\s*\((.+?)\)"),
    "article_alt":  re.compile(r"제(\d+)조의(\d+)\s*\((.+?)\)"),
    "paragraph":    re.compile(r"([①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮])"),
    "subparagraph": re.compile(r"(\d+)\.\s"),
}

CIRCLED_NUMBER_MAP = {
    "①": 1, "②": 2, "③": 3, "④": 4, "⑤": 5,
    "⑥": 6, "⑦": 7, "⑧": 8, "⑨": 9, "⑩": 10,
    "⑪": 11, "⑫": 12, "⑬": 13, "⑭": 14, "⑮": 15,
}

TOPIC_KEYWORDS: dict[str, list[str]] = {
    "세율": ["세율", "과세표준", "세율표", "세액", "과세", "누진"],
    "경비": ["경비", "필요경비", "비용", "손금", "경비율"],
    "공제": ["공제", "감면", "세액공제", "소득공제", "특별공제"],
    "신고": ["신고", "납부", "절차", "기한", "신고서", "확정신고"],
    "접대비": ["접대비", "접대"],
    "감가상각": ["감가상각", "상각", "내용연수"],
    "계산": ["계산", "산출", "산정", "결정"],
}


class LegalParser:
    """한국 세법 PDF에서 편-장-절-조-항-호 구조를 파싱하여 구조 보존 청크를 생성한다."""

    MAX_CHUNK_SIZE = 1500

    def _extract_topics(self, article_title: str, content: str) -> str:
        """조 제목과 내용에서 토픽 키워드를 추출하여 쉼표 구분 문자열로 반환한다."""
        text = f"{article_title} {content[:500]}"
        matched = []
        for topic, keywords in TOPIC_KEYWORDS.items():
            if any(kw in text for kw in keywords):
                matched.append(topic)
        return ",".join(matched) if matched else ""

    def parse(self, text: str, law_name: str, law_type: str, tax_type: str) -> list[LegalChunk]:
        """전체 법률 텍스트를 구조 보존 청크로 분할한다."""
        articles = self._split_into_articles(text)
        chunks: list[LegalChunk] = []

        for art in articles:
            content = art["content"]
            article_sub = art.get("article_sub")
            article_id = (
                f"{art['article']}_sub{article_sub}"
                if article_sub is not None
                else str(art["article"])
            )
            base_metadata = {
                "law_name": law_name,
                "law_type": law_type,
                "tax_type": tax_type,
                "article": art["article"],
                "article_sub": article_sub,
                "article_title": art["article_title"],
                "part": art.get("part"),
                "part_title": art.get("part_title", ""),
                "chapter": art.get("chapter"),
                "chapter_title": art.get("chapter_title", ""),
                "section": art.get("section"),
                "section_title": art.get("section_title", ""),
                "paragraph": None,
                "topics": self._extract_topics(art["article_title"], content),
            }
            if len(content) > self.MAX_CHUNK_SIZE:
                paragraphs = self._split_article_by_paragraph(content)
                if paragraphs:
                    for para_text, para_num in paragraphs:
                        meta = dict(base_metadata)
                        meta["paragraph"] = para_num
                        meta["chunk_id"] = f"{law_name}_{article_id}_{para_num}"
                        chunks.append(LegalChunk(content=para_text, metadata=meta))
                else:
                    base_metadata["chunk_id"] = f"{law_name}_{article_id}"
                    chunks.append(LegalChunk(content=content, metadata=base_metadata))
            else:
                base_metadata["chunk_id"] = f"{law_name}_{article_id}"
                chunks.append(LegalChunk(content=content, metadata=base_metadata))

        return chunks

    def _split_into_articles(self, text: str) -> list[dict]:
        """텍스트를 조 단위로 분할한다."""
        lines = text.split("\n")

        current_part: int | None = None
        current_part_title: str = ""
        current_chapter: int | None = None
        current_chapter_title: str = ""
        current_section: int | None = None
        current_section_title: str = ""
        current_article: int | None = None
        current_article_sub: int | None = None
        current_article_title: str = ""
        current_lines: list[str] = []
        articles: list[dict] = []

        def flush():
            """현재 버퍼의 내용을 조 정보와 함께 저장한다."""
            if current_article is not None and current_lines:
                articles.append({
                    "article": current_article,
                    "article_sub": current_article_sub,
                    "article_title": current_article_title,
                    "part": current_part,
                    "part_title": current_part_title,
                    "chapter": current_chapter,
                    "chapter_title": current_chapter_title,
                    "section": current_section,
                    "section_title": current_section_title,
                    "content": "\n".join(current_lines).strip(),
                })

        for line in lines:
            stripped = line.strip()
            if not stripped:
                if current_article is not None:
                    current_lines.append(line)
                continue

            # 편
            m = LEGAL_PATTERNS["part"].match(stripped)
            if m:
                current_part = int(m.group(1))
                current_part_title = m.group(2).strip()
                current_chapter = None
                current_chapter_title = ""
                current_section = None
                current_section_title = ""
                continue

            # 장
            m = LEGAL_PATTERNS["chapter"].match(stripped)
            if m:
                current_chapter = int(m.group(1))
                current_chapter_title = m.group(2).strip()
                current_section = None
                current_section_title = ""
                continue

            # 절
            m = LEGAL_PATTERNS["section"].match(stripped)
            if m:
                current_section = int(m.group(1))
                current_section_title = m.group(2).strip()
                continue

            # 조의N (article_alt 먼저)
            m = LEGAL_PATTERNS["article_alt"].match(stripped)
            if m:
                flush()
                current_article = int(m.group(1))
                current_article_sub = int(m.group(2))
                current_article_title = m.group(3)
                current_lines = [stripped]
                continue

            # 조
            m = LEGAL_PATTERNS["article"].match(stripped)
            if m:
                flush()
                current_article = int(m.group(1))
                current_article_sub = None
                current_article_title = m.group(2)
                current_lines = [stripped]
                continue

            if current_article is not None:
                current_lines.append(line)

        flush()
        return articles

    def _split_article_by_paragraph(self, article_text: str) -> list[tuple[str, int]]:
        """조 텍스트를 항 단위로 분할한다."""
        pattern = LEGAL_PATTERNS["paragraph"]
        parts = pattern.split(article_text)

        if len(parts) <= 1:
            return []

        result: list[tuple[str, int]] = []
        # parts[0]은 항 이전 텍스트, 이후 [원문자, 텍스트, 원문자, 텍스트, ...] 패턴
        i = 1
        while i < len(parts):
            circled = parts[i]
            para_num = CIRCLED_NUMBER_MAP.get(circled, i)
            para_text = circled + (parts[i + 1] if i + 1 < len(parts) else "")
            result.append((para_text.strip(), para_num))
            i += 2

        return result
