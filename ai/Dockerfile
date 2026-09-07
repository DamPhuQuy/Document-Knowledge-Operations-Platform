# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Base Stage: Python 3.11 with official uv binary
# ------------------------------------------------------------------------------
FROM ghcr.io/astral-sh/uv:python3.11-bookworm-slim AS base

WORKDIR /app

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PYTHONUNBUFFERED=1

# Copy dependency manifests first for optimal layer caching
COPY pyproject.toml uv.lock ./

# Install project dependencies without project source code
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev --no-install-project

# ------------------------------------------------------------------------------
# Target: Development (Live reload & Compose Watch)
# ------------------------------------------------------------------------------
FROM base AS dev

COPY . .

RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

EXPOSE 8000

ENV PATH="/app/.venv/bin:$PATH" \
    PYTHONPATH="/app/src"

CMD ["uvicorn", "ai.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

# ------------------------------------------------------------------------------
# Target: Production (Hardened non-root user)
# ------------------------------------------------------------------------------
FROM python:3.11-slim AS prod

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

RUN addgroup --system app && adduser --system --group app

COPY --from=dev --chown=app:app /app/.venv /app/.venv
COPY --from=dev --chown=app:app /app/src /app/src

USER app

EXPOSE 8000

ENV PATH="/app/.venv/bin:$PATH" \
    PYTHONPATH="/app/src" \
    PYTHONUNBUFFERED=1

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8000/docs || exit 1

CMD ["uvicorn", "ai.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
