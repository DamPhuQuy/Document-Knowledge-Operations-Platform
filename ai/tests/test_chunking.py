from ai.application.service.chunking_service import ChunkingService
from ai.domain.model.document import Document


def test_fixed_character_chunking() -> None:
    chunker = ChunkingService(default_chunk_size=50, default_chunk_overlap=10)
    doc = Document(id="doc1", content="A" * 120, metadata={"source": "test"})
    chunks = chunker.chunk_fixed(doc, chunk_size=50, chunk_overlap=10)

    assert len(chunks) >= 3
    assert all(c.document_id == "doc1" for c in chunks)
    assert all(len(c.content) <= 50 for c in chunks)
    assert chunks[0].metadata["strategy"] == "fixed_character"


def test_recursive_character_chunking() -> None:
    chunker = ChunkingService()
    text = (
        "Paragraph one is here.\n\n"
        "Paragraph two has multiple sentences. Second sentence here.\n\n"
        "Paragraph three concludes the document."
    )
    doc = Document(id="doc2", content=text)
    chunks = chunker.chunk_recursive(doc, chunk_size=80, chunk_overlap=10)

    assert len(chunks) >= 2
    assert all(c.document_id == "doc2" for c in chunks)
    assert all(c.content for c in chunks)


def test_token_based_chunking() -> None:
    chunker = ChunkingService()
    doc = Document(id="doc3", content="This is a token chunking test with OpenAI tiktoken encoder.")
    chunks = chunker.chunk_by_tokens(doc, max_tokens=5, overlap_tokens=1)

    assert len(chunks) > 1
    assert all("token_count" in c.metadata for c in chunks)


def test_markdown_chunking() -> None:
    chunker = ChunkingService()
    md_text = (
        "# Title\n\nIntroductory text here.\n\n"
        "## Section 1\n\nDetails about section 1.\n\n"
        "## Section 2\n\nDetails about section 2."
    )
    doc = Document(id="doc_md", content=md_text)
    chunks = chunker.chunk_markdown(doc, max_chunk_size=100)

    assert len(chunks) >= 3
    assert any("Section 1" in c.content for c in chunks)
