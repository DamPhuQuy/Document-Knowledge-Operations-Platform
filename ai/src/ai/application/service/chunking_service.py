import re
from typing import Any

import tiktoken

from ai.domain.model.chunk import Chunk
from ai.domain.model.document import Document


class ChunkingService:
    """Service providing document chunking strategies."""

    def __init__(
        self,
        default_chunk_size: int = 500,
        default_chunk_overlap: int = 50,
        encoding_name: str = "cl100k_base",
    ) -> None:
        self.default_chunk_size = default_chunk_size
        self.default_chunk_overlap = default_chunk_overlap
        try:
            self._tokenizer = tiktoken.get_encoding(encoding_name)
        except Exception:
            self._tokenizer = tiktoken.get_encoding("cl100k_base")

    def chunk_fixed(
        self,
        document: Document,
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
    ) -> list[Chunk]:
        """Splits document content into fixed character chunks with overlap."""
        size = chunk_size or self.default_chunk_size
        overlap = chunk_overlap if chunk_overlap is not None else self.default_chunk_overlap
        text = document.content
        chunks: list[Chunk] = []

        if not text:
            return chunks

        step = max(1, size - overlap)
        idx = 0
        for start in range(0, len(text), step):
            end = min(start + size, len(text))
            chunk_text = text[start:end].strip()
            if chunk_text:
                metadata: dict[str, Any] = {
                    **document.metadata,
                    "start_char": start,
                    "end_char": end,
                    "strategy": "fixed_character",
                }
                chunks.append(
                    Chunk(
                        id=f"{document.id}_chunk_{idx}",
                        document_id=document.id,
                        content=chunk_text,
                        chunk_index=idx,
                        metadata=metadata,
                    )
                )
                idx += 1
            if end >= len(text):
                break

        return chunks

    def chunk_recursive(
        self,
        document: Document,
        chunk_size: int | None = None,
        chunk_overlap: int | None = None,
        separators: list[str] | None = None,
    ) -> list[Chunk]:
        """
        Recursively splits text by prioritized separators (paragraphs -> sentences -> words)
        to keep semantic blocks coherent within character limit.
        """
        size = chunk_size or self.default_chunk_size
        overlap = chunk_overlap if chunk_overlap is not None else self.default_chunk_overlap
        seps = separators or ["\n\n", "\n", ". ", " ", ""]

        raw_splits = self._split_text_recursive(document.content, size, overlap, seps)
        chunks: list[Chunk] = []
        for idx, text_segment in enumerate(raw_splits):
            if text_segment.strip():
                metadata: dict[str, Any] = {
                    **document.metadata,
                    "strategy": "recursive_character",
                }
                chunks.append(
                    Chunk(
                        id=f"{document.id}_rec_{idx}",
                        document_id=document.id,
                        content=text_segment.strip(),
                        chunk_index=idx,
                        metadata=metadata,
                    )
                )
        return chunks

    def chunk_by_tokens(
        self,
        document: Document,
        max_tokens: int = 256,
        overlap_tokens: int = 32,
    ) -> list[Chunk]:
        """Splits text accurately by LLM tokens using tiktoken."""
        tokens = self._tokenizer.encode(document.content)
        chunks: list[Chunk] = []

        if not tokens:
            return chunks

        step = max(1, max_tokens - overlap_tokens)
        idx = 0
        for start in range(0, len(tokens), step):
            end = min(start + max_tokens, len(tokens))
            chunk_tokens = tokens[start:end]
            chunk_text = self._tokenizer.decode(chunk_tokens).strip()
            if chunk_text:
                metadata: dict[str, Any] = {
                    **document.metadata,
                    "token_count": len(chunk_tokens),
                    "strategy": "token_based",
                }
                chunks.append(
                    Chunk(
                        id=f"{document.id}_tok_{idx}",
                        document_id=document.id,
                        content=chunk_text,
                        chunk_index=idx,
                        metadata=metadata,
                    )
                )
                idx += 1
            if end >= len(tokens):
                break

        return chunks

    def chunk_markdown(
        self,
        document: Document,
        max_chunk_size: int = 600,
    ) -> list[Chunk]:
        """Splits markdown documents based on heading hierarchy (#, ##, ###)."""
        text = document.content
        heading_pattern = r"(^#{1,6}\s+.+$)"
        sections = re.split(heading_pattern, text, flags=re.MULTILINE)

        chunks: list[Chunk] = []
        current_header = "Intro"
        current_buffer = ""
        idx = 0

        for part in sections:
            part = part.strip()
            if not part:
                continue

            if part.startswith("#"):
                if current_buffer:
                    sub_chunks = self._split_if_oversize(
                        current_buffer, max_chunk_size, current_header, document, idx
                    )
                    chunks.extend(sub_chunks)
                    idx += len(sub_chunks)
                    current_buffer = ""
                current_header = part
            else:
                if current_buffer:
                    current_buffer += "\n\n" + part
                else:
                    current_buffer = part

        if current_buffer:
            sub_chunks = self._split_if_oversize(
                current_buffer, max_chunk_size, current_header, document, idx
            )
            chunks.extend(sub_chunks)

        return chunks

    def _split_if_oversize(
        self,
        text: str,
        max_size: int,
        header: str,
        document: Document,
        start_idx: int,
    ) -> list[Chunk]:
        chunks: list[Chunk] = []
        if len(text) <= max_size:
            metadata: dict[str, Any] = {
                **document.metadata,
                "section_header": header,
                "strategy": "markdown_header",
            }
            return [
                Chunk(
                    id=f"{document.id}_md_{start_idx}",
                    document_id=document.id,
                    content=f"### {header}\n{text}" if not text.startswith("#") else text,
                    chunk_index=start_idx,
                    metadata=metadata,
                )
            ]

        doc_temp = Document(id=document.id, content=text, metadata=document.metadata)
        sub_splits = self.chunk_recursive(doc_temp, chunk_size=max_size, chunk_overlap=50)
        for offset, c in enumerate(sub_splits):
            c.id = f"{document.id}_md_{start_idx + offset}"
            c.chunk_index = start_idx + offset
            c.metadata["section_header"] = header
            c.metadata["strategy"] = "markdown_header_split"
            chunks.append(c)
        return chunks

    def _split_text_recursive(
        self, text: str, chunk_size: int, chunk_overlap: int, separators: list[str]
    ) -> list[str]:
        final_chunks: list[str] = []
        separator = separators[-1]
        new_separators: list[str] = []

        for i, sep in enumerate(separators):
            if sep == "":
                separator = ""
                break
            if re.search(re.escape(sep), text):
                separator = sep
                new_separators = separators[i + 1 :]
                break

        splits = text.split(separator) if separator != "" else list(text)

        good_splits: list[str] = []
        for s in splits:
            if not s:
                continue
            if len(s) < chunk_size:
                good_splits.append(s)
            else:
                if good_splits:
                    merged = self._merge_splits(good_splits, separator, chunk_size, chunk_overlap)
                    final_chunks.extend(merged)
                    good_splits = []
                if not new_separators:
                    final_chunks.append(s)
                else:
                    other_chunks = self._split_text_recursive(
                        s, chunk_size, chunk_overlap, new_separators
                    )
                    final_chunks.extend(other_chunks)

        if good_splits:
            merged = self._merge_splits(good_splits, separator, chunk_size, chunk_overlap)
            final_chunks.extend(merged)

        return final_chunks

    def _merge_splits(
        self, splits: list[str], separator: str, chunk_size: int, chunk_overlap: int
    ) -> list[str]:
        docs: list[str] = []
        current_doc: list[str] = []
        total = 0

        for d in splits:
            len_d = len(d)
            sep_len = len(separator) if current_doc else 0
            if total + len_d + sep_len > chunk_size and current_doc:
                doc_text = separator.join(current_doc)
                docs.append(doc_text)

                while total > chunk_overlap and len(current_doc) > 1:
                    removed = current_doc.pop(0)
                    total -= len(removed) + len(separator)

            current_doc.append(d)
            total += len_d + (len(separator) if len(current_doc) > 1 else 0)

        if current_doc:
            docs.append(separator.join(current_doc))

        return docs
