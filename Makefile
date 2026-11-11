# =============================================================================
#  Dhun music app – one entry point for every task.
#
#  Every target works the same:
#    * on your machine  → tool targets run inside the `dev` container (no Java/Node needed)
#    * inside `make dkr-sh` or in CI (IN_CONTAINER=1 / CI=true) → run directly
#    * LOCAL=1 make <target> → run directly with the tools installed on your machine
#
#  `make` or `make help` lists everything.
# =============================================================================
SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE      := docker compose
DEV          := $(COMPOSE) --profile tools run --rm dev
DEV_PORTS    := $(COMPOSE) --profile tools run --rm --service-ports dev
MVN          := ./mvnw -B
DB_URL_LOCAL := jdbc:mysql://localhost:3306/musicapp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

ifneq ($(filter 1 true,$(IN_CONTAINER) $(CI) $(LOCAL)),)
  INSIDE := 1
endif

# $(call run,<shell command>) – run here when INSIDE, otherwise inside the dev container.
run       = $(if $(INSIDE),$(1),$(DEV) bash -c '$(1)')
run_ports = $(if $(INSIDE),$(1),$(DEV_PORTS) bash -c '$(1)')

# service name for dkr-logs / dkr-restart, e.g. `make dkr-logs s=backend`
s ?=

##@ Help
.PHONY: help
help: ## Show this help
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage: make \033[36m<target>\033[0m\n"} \
	    /^[a-zA-Z0-9_-]+:.*?##/ { printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2 } \
	    /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) }' $(MAKEFILE_LIST)
	@echo ""
	@echo "  Context: $(if $(INSIDE),running commands directly,host - tool targets run in the dev container)"

##@ Docker
.PHONY: dkr-build dkr-build-nc dkr-start dkr-up dkr-stop dkr-down dkr-reset dkr-restart dkr-ps dkr-logs dkr-sh dkr-mysql dkr-prune
dkr-build: ## docker compose build (app images + dev toolbox)
	$(COMPOSE) --profile tools build

dkr-build-nc: ## docker compose build --no-cache
	$(COMPOSE) --profile tools build --no-cache --pull

dkr-start: config ## Build and start the whole stack (app on http://localhost:3000)
	$(COMPOSE) up -d --build --wait
	@echo "App: http://localhost:3000   API: http://localhost:8080   Kafka UI: http://localhost:8090"

dkr-up: ## docker compose up -d (no rebuild)
	$(COMPOSE) up -d --wait

dkr-stop: ## Stop containers, keep them
	$(COMPOSE) stop

dkr-down: ## docker compose down (data volumes are kept)
	$(COMPOSE) --profile tools down --remove-orphans

dkr-reset: ## docker compose down -v (DELETES MySQL/Kafka data and caches)
	$(COMPOSE) --profile tools down -v --remove-orphans

dkr-restart: ## docker compose restart (all, or one with s=backend)
	$(COMPOSE) restart $(s)

dkr-ps: ## Show container status
	$(COMPOSE) ps

dkr-logs: ## Follow logs (all, or one with s=backend)
	$(COMPOSE) logs -f --tail=200 $(s)

dkr-sh: ## Open a shell in the dev container - run any `make` target from there
	$(DEV_PORTS) bash

dkr-mysql: ## MySQL client on the app database
	$(COMPOSE) exec mysql sh -c 'mysql -umusicapp -p"$$MYSQL_PASSWORD" musicapp'

dkr-prune: ## Remove dangling images and Docker build cache (affects all projects on this machine)
	docker image prune -f
	docker builder prune -f

##@ Project
.PHONY: config install migrate migrate-info run-backend run-frontend
config: ## Create .env from .env.example (if missing) and validate docker-compose
	@test -f .env || (cp .env.example .env && echo "Created .env from .env.example")
	@$(COMPOSE) config -q && echo "docker-compose config OK"

install: ## Download backend dependencies and install frontend packages
	$(call run,cd backend && $(MVN) -q dependency:go-offline && cd ../frontend && npm ci --no-audit --no-fund)

migrate: ## Apply Flyway DB migrations (MySQL must be running: make dkr-up)
	$(call run,cd backend && $(MVN) -q flyway:migrate -Dflyway.url="$${DB_URL:-$(DB_URL_LOCAL)}" -Dflyway.user="$${DB_USER:-musicapp}" -Dflyway.password="$${DB_PASSWORD:-musicapp}")

migrate-info: ## Show applied and pending migrations
	$(call run,cd backend && $(MVN) -q flyway:info -Dflyway.url="$${DB_URL:-$(DB_URL_LOCAL)}" -Dflyway.user="$${DB_USER:-musicapp}" -Dflyway.password="$${DB_PASSWORD:-musicapp}")

run-backend: ## Run Spring Boot from source (port 8081 in the dev container; needs make dkr-up)
	$(call run_ports,cd backend && $(MVN) spring-boot:run)

run-frontend: ## Run the Vite dev server with hot reload on http://localhost:5173
	$(call run_ports,cd frontend && npm run dev -- --host 0.0.0.0 --port 5173)

##@ Tests
.PHONY: test test-unit test-acceptance test-backend test-js test-js-watch test-js-coverage test-e2e tdd tdd-clean
test: test-backend test-js test-e2e ## Run every test suite

test-unit: ## Backend unit tests (JUnit, Mockito, MockMvc)
	$(call run,cd backend && $(MVN) test)

test-acceptance: ## Backend acceptance tests (Cucumber + Testcontainers MySQL/Kafka + WireMock)
	$(call run,cd backend && $(MVN) verify -DskipUTs=true)

test-backend: ## Backend unit + acceptance tests
	$(call run,cd backend && $(MVN) verify)

test-js: ## Frontend unit tests (Vitest + Testing Library)
	$(call run,cd frontend && npm run test)

test-js-watch: ## Frontend unit tests in watch mode
	$(call run,cd frontend && npm run test:watch)

test-js-coverage: ## Frontend unit tests with coverage report (frontend/coverage)
	$(call run,cd frontend && npm run test:coverage)

test-e2e: ## Frontend acceptance tests in Chromium, desktop + mobile (Playwright)
	$(call run,cd frontend && npm run test:e2e)

tdd: test-unit test-js ## Quick feedback loop: backend + frontend unit tests

tdd-clean: clean tdd ## Clean build output, then run the unit tests from scratch

##@ Frontend quality & build
.PHONY: lint lint-fix typecheck build-js build-backend build
lint: ## ESLint (fails on any warning)
	$(call run,cd frontend && npm run lint)

lint-fix: ## ESLint with auto-fix
	$(call run,cd frontend && npm run lint:fix)

typecheck: ## TypeScript type check
	$(call run,cd frontend && npm run typecheck)

build-js: ## Production build of the React app (frontend/dist)
	$(call run,cd frontend && npm run build)

build-backend: ## Build the Spring Boot jar (backend/target)
	$(call run,cd backend && $(MVN) -q package -DskipTests)

build: build-backend build-js ## Build backend and frontend

##@ CI
.PHONY: ci
ci: lint typecheck test-js build-js test-backend test-e2e ## Everything the GitHub PR checks run

##@ Clean & caches
.PHONY: clean cache-clean cache-clean-all
clean: ## Remove build output (backend/target, frontend/dist, coverage, test reports)
	rm -rf backend/target frontend/dist frontend/dev-dist frontend/coverage frontend/test-results frontend/playwright-report

cache-clean: clean ## Also clear tool caches (Vite, Vitest, ESLint, TypeScript build info)
	rm -rf frontend/node_modules/.vite frontend/node_modules/.vitest frontend/node_modules/.cache frontend/.eslintcache
	rm -f frontend/tsconfig.tsbuildinfo

cache-clean-all: cache-clean ## Also remove node_modules and the Maven / node_modules Docker volumes
	rm -rf frontend/node_modules
	-docker volume rm dhun-music_maven-repo dhun-music_frontend-node-modules
