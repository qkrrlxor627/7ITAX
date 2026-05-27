"""ai/app/ 하위 Python 파일에서 docstring 누락 현황을 스캔한다."""

import ast
import json
import os

TARGET_DIR = os.path.join(os.path.dirname(__file__), "..", "app")


def scan_file(fpath: str) -> list[dict]:
    """파일에서 docstring이 없는 함수/클래스를 찾는다."""
    with open(fpath, encoding="utf-8") as f:
        source = f.read()
    tree = ast.parse(source)

    missing = []
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            if ast.get_docstring(node) is not None:
                continue
            name = node.name
            # skip magic methods except __init__
            if name.startswith("__") and name.endswith("__") and name != "__init__":
                continue
            args = [a.arg for a in node.args.args if a.arg != "self"]
            ret = ast.unparse(node.returns) if node.returns else None
            missing.append({
                "type": "async_function" if isinstance(node, ast.AsyncFunctionDef) else "function",
                "name": name,
                "line": node.lineno,
                "args": args,
                "returns": ret,
            })
        elif isinstance(node, ast.ClassDef):
            if ast.get_docstring(node) is not None:
                continue
            missing.append({
                "type": "class",
                "name": node.name,
                "line": node.lineno,
            })

    return missing


def main():
    results = {}
    for root, _dirs, files in os.walk(TARGET_DIR):
        for fname in sorted(files):
            if not fname.endswith(".py") or fname == "__init__.py":
                continue
            fpath = os.path.join(root, fname)
            rel = os.path.relpath(fpath, os.path.join(TARGET_DIR, "..")).replace("\\", "/")
            try:
                missing = scan_file(fpath)
            except Exception as e:
                results[rel] = {"error": str(e)}
                continue
            if missing:
                results[rel] = {"missing": missing, "count": len(missing)}

    print(json.dumps(results, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
