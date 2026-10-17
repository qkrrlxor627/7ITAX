"""BE에서 다른 코드가 참조하지 않는 클래스와 public 메서드 후보를 출력한다.

사용: python3 scripts/find_unused_java.py
결과는 후보 목록이다. 프레임워크가 호출하는 메서드(리스너, 스케줄러, AOP, JPA 컨버터,
템플릿 getter)는 정상이므로 사람이 걸러서 판단한다.
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "BE", "src")
MAIN = os.path.join(ROOT, "main", "java")
STEREOTYPE = re.compile(
    r"@(Component|Service|Repository|RestController|Controller|Configuration|Entity|"
    r"Embeddable|ControllerAdvice|RestControllerAdvice|SpringBootApplication|Aspect|MappedSuperclass)\b"
)
PUBLIC_METHOD = re.compile(r"^\s+public\s+(?!class|record|interface|enum)[\w<>\[\], ?]+\s+(\w+)\s*\(", re.M)
SKIP_METHODS = {"equals", "hashCode", "toString", "main", "run", "of", "from"}


def load_sources():
    sources = {}
    for directory, _, names in os.walk(ROOT):
        for name in names:
            if name.endswith(".java"):
                path = os.path.join(directory, name)
                with open(path, encoding="utf-8") as f:
                    sources[path] = f.read()
    return sources


def unused_classes(sources):
    for path, text in sources.items():
        if not path.startswith(MAIN) or STEREOTYPE.search(text):
            continue
        name = os.path.basename(path)[:-5]
        pattern = re.compile(rf"\b{re.escape(name)}\b")
        if not any(pattern.search(t) for p, t in sources.items() if p != path):
            yield os.path.relpath(path, MAIN)


def unused_methods(sources):
    main_sources = {p: t for p, t in sources.items() if p.startswith(MAIN)}
    for path, text in main_sources.items():
        if not re.search(r"@(Service|Component)\b", text):
            continue
        for method in sorted(set(PUBLIC_METHOD.findall(text)) - SKIP_METHODS):
            call = re.compile(rf"\.{method}\s*\(|::{method}\b")
            called_elsewhere = any(call.search(t) for p, t in main_sources.items() if p != path)
            called_inside = len(re.findall(rf"\b{method}\s*\(", text)) > 1
            if not called_elsewhere and not called_inside:
                yield os.path.relpath(path, MAIN), method


def main():
    sources = load_sources()
    classes = list(unused_classes(sources))
    methods = list(unused_methods(sources))
    for c in classes:
        print(f"[class]  {c}")
    for path, m in methods:
        print(f"[method] {path}#{m}")
    if not classes and not methods:
        print("후보 없음")
    return 0


if __name__ == "__main__":
    sys.exit(main())
