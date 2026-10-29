KEYCLOAK_URL := http://localhost:8180
REALM        := customer-realm
TOKEN_URL    := $(KEYCLOAK_URL)/realms/$(REALM)/protocol/openid-connect/token
KC_USER      ?= ada.admin

.DEFAULT_GOAL := help
.PHONY: help up down reset logs wait run run-plain test package clean token docker-run docker-test

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

up: ## Start Keycloak and wait until the realm is ready
	docker compose up -d
	@$(MAKE) --no-print-directory wait

down: ## Stop Keycloak and the app container (removes Keycloak's data)
	docker compose --profile app down

reset: down up ## Recreate Keycloak so it re-imports keycloak/customer-realm.json

logs: ## Follow Keycloak logs
	docker compose logs -f keycloak

wait:
	@printf "Waiting for Keycloak"
	@for i in $$(seq 1 60); do \
		curl -sf -o /dev/null $(KEYCLOAK_URL)/realms/$(REALM)/.well-known/openid-configuration \
			&& echo " ready on $(KEYCLOAK_URL)" && exit 0; \
		printf "."; sleep 2; \
	done; echo " timed out"; exit 1

run: up ## Start Keycloak, then the app with the demo profile (seed data + demo page)
	mvn spring-boot:run -Dspring-boot.run.profiles=demo

run-plain: up ## Start Keycloak, then the app without the demo profile (empty database, no page)
	mvn spring-boot:run

docker-run: ## No JDK/Maven needed: build and run Keycloak + app in Docker (demo profile)
	docker compose --profile app up --build

docker-test: ## No JDK/Maven needed: run the tests in a Docker container
	docker build --target build -t customer-api-build .
	docker run --rm customer-api-build mvn test

test: ## Run the tests (no Keycloak needed)
	mvn test

package: ## Build the jar into target/
	mvn package

clean: ## Remove build output
	mvn clean

token: ## Print an access token, e.g. make token KC_USER=sally.sales
	@curl -s -d "grant_type=password&client_id=customer-api-client&username=$(KC_USER)&password=password" $(TOKEN_URL) \
		| python3 -c "import sys, json; r = json.load(sys.stdin); print(r.get('access_token') or r)"
