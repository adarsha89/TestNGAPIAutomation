# Reqres API Automation Framework

A layered TestNG + RestAssured API test automation framework covering **REST**, **GraphQL**, **WebSocket**,
and **Webhook** protocols behind one protocol-agnostic client abstraction. Tests describe behaviour; the
framework owns configuration, request building, masked logging, and reporting.

An architecture reference (basic + detailed diagrams, component reference, request-lifecycle walkthrough)
is available for onboarding — ask in Claude Code to regenerate/open it, or see `docs/` for prior
requirements/plans/reviews produced by the agent pipeline described below.

## Tech stack

| Concern | Library |
|---|---|
| Language / build | Java 17, Maven |
| Test execution | TestNG 7.9 |
| HTTP / REST / GraphQL | RestAssured 5.4 (+ json-schema-validator) |
| WebSocket | Java-WebSocket 1.5 |
| Webhook stub | WireMock (JRE8) 2.35 |
| Reporting | Allure 2.27 (allure-testng, allure-rest-assured) |
| JSON | Jackson Databind |
| Logging | SLF4J + Logback |

## Project structure

```
src/main/java/com/reqres/automation/
  config/       ConfigLoader, EnvConfig            — layered env resolution
  clients/      ApiClient, ClientFactory,           — factory/strategy dispatch
                RequestSpecFactory, RestAssuredConfigFactory
    rest/       RestClientBase, UserRestClient
    graphql/    GraphQLClient, GraphQLStubServer     — test classes only stub/configure
                GraphQL responses through GraphQLStubServer, never WireMock directly
    websocket/  WebSocketTestClient
    webhook/    WebhookReceiver                     — embeds WireMock; exposes
                stubIncomingCallResponse(...) so test classes never drive WireMock's
                stubbing DSL directly
  models/       rest/graphql/websocket request & response POJOs
  testdata/     UserDataBuilder, GraphQLPayloadBuilder
  assertions/   ResponseAssertions, SchemaAssertions, GraphQLAssertions,
                WebSocketAssertions, WebhookAssertions               — every protocol's
                test code (REST, GraphQL, WebSocket, webhook) calls into these rather
                than writing inline response parsing, field navigation, or direct
                TestNG Assert/WireMock-verify calls
  util/         Constants, LogMasker

src/test/java/com/reqres/automation/
  base/         Base*Test — thread-local client per test class (parallel-safe)
  rest/ graphql/ websocket/ webhook/  — test classes

src/test/resources/
  config/       common.properties + qa/staging/prod.properties
  schemas/      JSON schemas used by SchemaAssertions

infra/          docker-compose.yml — local SonarQube, Grafana, Elasticsearch, Jenkins
.github/workflows/api-tests.yml    — CI: PR → smoke, schedule/dispatch → regression
```

### Test-writing convention

Every test class, across all four protocol areas (REST, GraphQL, WebSocket, webhook), only
orchestrates a call into `clients/` and a check into `assertions/`, plus TestNG
annotations/data providers. A test class never constructs a raw request/connection, never
drives a mock's stubbing/verification DSL, never parses a raw response, and never asserts
directly — if a helper doesn't yet exist for something a test needs, the gap gets filled in
`clients/`/`assertions/`, not inlined into the test.

## Prerequisites

- JDK 17
- Maven (or use the bundled `./mvnw` / `./mvnw.cmd` wrapper)

## Running tests

```bash
./mvnw test                    # runs the full suite (testng.xml)
./mvnw test -Dgroups=smoke     # smoke-tagged tests only
./mvnw test -Dgroups=regression
```

Tests run in parallel — `testng.xml` uses `parallel="classes" thread-count="4"`.

### Environments

```bash
./mvnw test -Denv=qa        # default
./mvnw test -Denv=staging
./mvnw test -Denv=prod
```

Config resolves in layers (later overrides earlier): `common.properties` →
`<env>.properties` → gitignored `<env>.local.properties` → identically-named environment variable
(e.g. `rest.base.url` → `REST_BASE_URL`, `api.key` → `API_KEY`). Never commit a real API key — supply it
via `API_KEY` or a local `.local.properties` override.

## Allure reporting

Raw results are written to `target/allure-results` (see `allure.properties`). Requires the
[Allure Commandline](https://allurereport.org/docs/install/) (`brew install allure` on macOS).

```bash
# Serve an interactive report straight from raw results (opens in browser, no report dir left behind)
allure serve target/allure-results

# Generate a report and open it separately
./mvnw allure:report
allure open target/site/allure-maven-plugin

# Generate a single-file HTML report (easy to share/attach — one .html, no server needed)
allure generate --single-file target/allure-results
```

## CI

`.github/workflows/api-tests.yml`:
- **Pull requests** → `smoke` group
- **Nightly schedule (02:00 UTC) / manual `workflow_dispatch`** → `regression` group

Each run uploads Surefire results and the generated Allure report as workflow artifacts.

## Local support infrastructure (optional)

`infra/docker-compose.yml` runs SonarQube (+ Postgres), Grafana, Elasticsearch, and Jenkins for local
code-quality scans and dashboards. Not required to run tests.

```bash
docker compose -f infra/docker-compose.yml up -d
```

## Automation code changes

Automation code changes (new/changed test coverage) in this repo go through the multi-agent pipeline
defined in `.claude/agents/` and documented in `CLAUDE.md`:
`task-analysis-agent` → `planning-agent` → `coding-agent` → `code-review-agent` → `code-quality-agent`.
