from pathlib import Path
from typing import Iterable

from langchain_community.document_loaders import PyPDFLoader, TextLoader
from langchain_core.documents import Document


SUPPORTED_EXTENSIONS = {".txt", ".md", ".pdf"}


def load_documents(input_path: str) -> list[Document]:
    """加载文件或目录下的文档，并过滤为支持格式。"""
    path = Path(input_path)
    files: Iterable[Path]

    if path.is_file():
        files = [path]
    elif path.is_dir():
        files = (p for p in path.rglob("*") if p.is_file())
    else:
        raise ValueError(f"Path not found: {input_path}")

    docs: list[Document] = []
    for file in files:
        if file.suffix.lower() not in SUPPORTED_EXTENSIONS:
            continue
        docs.extend(_load_single_file(file))

    if not docs:
        raise ValueError("No supported documents found. Use txt, md, or pdf files.")

    return docs


def _load_single_file(file: Path) -> list[Document]:
    """按文件类型选择 Loader。"""
    suffix = file.suffix.lower()
    if suffix == ".pdf":
        loader = PyPDFLoader(str(file))
        return loader.load()

    loader = TextLoader(str(file), encoding="utf-8")
    return loader.load()
