.PHONY: help dev dev-watch down db backend backend-watch frontend frontend-watch \
        ai ai-watch ai-local logs logs-backend logs-frontend logs-ai logs-db ps \
        sh-backend sh-frontend sh-ai staging staging-down prod prod-down clean

ENV_STAGING ?= .env.staging
ENV_PROD    ?= .env.prod

DC         = docker compose
DC_STAGING = docker compose -f docker-compose.staging.yaml --env-file $(ENV_STAGING)
DC_PROD    = docker compose -f docker-compose.prod.yaml --env-file $(ENV_PROD)

.env:
	@if [ ! -f .env ]; then cp .env.example .env; fi

dev: .env
	$(DC) up -d --build

dev-watch: .env
	$(DC) up -d --build
	$(DC) watch

down:
	$(DC) down

db: .env
	$(DC) up -d postgres

backend: .env
	$(DC) up -d --build backend

backend-watch: .env
	$(DC) up -d --build backend
	$(DC) watch backend

frontend: .env
	$(DC) up -d --build --no-deps frontend

frontend-watch: .env
	$(DC) up -d --build --no-deps frontend
	$(DC) watch --no-up frontend

ai: .env
	$(DC) up -d --build ai

ai-watch: .env
	$(DC) up -d --build ai
	$(DC) watch ai

ai-local:
	cd ai && uv run python src/ai/main.py

logs:
	$(DC) logs -f

logs-backend:
	$(DC) logs -f backend

logs-frontend:
	$(DC) logs -f frontend

logs-ai:
	$(DC) logs -f ai

logs-db:
	$(DC) logs -f postgres

ps:
	$(DC) ps

sh-backend:
	$(DC) exec backend sh

sh-frontend:
	$(DC) exec frontend sh

sh-ai:
	$(DC) exec ai sh

staging:
	@if [ ! -f $(ENV_STAGING) ]; then cp .env.staging.example $(ENV_STAGING); fi
	$(DC_STAGING) up -d --build

staging-down:
	$(DC_STAGING) down

prod:
	@if [ ! -f $(ENV_PROD) ]; then cp .env.prod.example $(ENV_PROD); fi
	$(DC_PROD) up -d --build

prod-down:
	$(DC_PROD) down

clean:
	$(DC) down -v --remove-orphans

help:
	@printf "\nUsage: make <target>\n\n"
	@printf "  dev             Start all services (db, backend, ai, frontend)\n"
	@printf "  dev-watch       Start all services with compose watch\n"
	@printf "  down            Stop all services\n\n"
	@printf "  db              Start postgres (pgvector)\n"
	@printf "  backend         Start backend (+ postgres)\n"
	@printf "  backend-watch   Start backend with compose watch\n"
	@printf "  frontend        Start frontend standalone\n"
	@printf "  frontend-watch  Start frontend with compose watch\n"
	@printf "  ai              Start ai service (+ postgres)\n"
	@printf "  ai-watch        Start ai service with compose watch\n"
	@printf "  ai-local        Run ai service on host with uv\n\n"
	@printf "  logs            Follow logs for all services\n"
	@printf "  logs-backend    Follow backend logs\n"
	@printf "  logs-frontend   Follow frontend logs\n"
	@printf "  logs-ai         Follow ai logs\n"
	@printf "  logs-db         Follow postgres logs\n"
	@printf "  ps              List running containers\n"
	@printf "  sh-backend      Open shell in backend container\n"
	@printf "  sh-frontend     Open shell in frontend container\n"
	@printf "  sh-ai           Open shell in ai container\n\n"
	@printf "  staging         Start staging stack\n"
	@printf "  staging-down    Stop staging stack\n"
	@printf "  prod            Start production stack\n"
	@printf "  prod-down       Stop production stack\n"
	@printf "  clean           Remove all containers and volumes\n\n"
