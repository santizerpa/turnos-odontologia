# Skill Registry

**Delegator use only.** Any agent that launches sub-agents reads this registry to resolve compact rules, then injects them directly into sub-agent prompts. Sub-agents do NOT read this registry or individual SKILL.md files.

Generated: 2026-11-22 · Project: turnos-odontologia · Scope: project skills in `.agents/skills/`, workflow skills in `~/.config/opencode/skills/` and `~/.agents/skills/`.

## Coding Skills (project scope)

| Trigger | Skill | Path |
|---------|-------|------|
| Spring Boot feature work: controllers, validation, ProblemDetail, JPA, config, caching | `java-springboot` | `.agents/skills/java-springboot/SKILL.md` |
| Spring Boot architecture: REST, layered services, JPA, async, logging | `springboot-patterns` | `.agents/skills/springboot-patterns/SKILL.md` |
| Java unit tests with JUnit 5/6, AssertJ, Mockito, parameterized tests | `java-junit` | `.agents/skills/java-junit/SKILL.md` |
| TDD for Spring Boot: red/green/refactor, MockMvc, DataJpaTest, JaCoCo | `springboot-tdd` | `.agents/skills/springboot-tdd/SKILL.md` |
| PostgreSQL schema design: types, indexes, constraints, normalization | `postgresql-table-design` | `.agents/skills/postgresql-table-design/SKILL.md` |
| Spring Security: authn/authz, BCrypt, CSRF, CORS, secrets, rate limiting | `springboot-security` | `.agents/skills/springboot-security/SKILL.md` |
| Spring Boot test patterns: unit, slice, integration, Testcontainers | `spring-boot-test-patterns` | `.agents/skills/spring-boot-test-patterns/SKILL.md` |

## Compact Rules

Pre-digested rules per skill. Delegators copy matching blocks into sub-agent prompts as `## Project Standards (auto-resolved)`.

### java-springboot
- Detect Spring Boot and Java version, starters, persistence and test setup from `build.gradle` before coding; match existing package layout and naming.
- Never upgrade Spring Boot, Java or dependencies implicitly; a Boot 3 → 4 move is a separate task.
- Ask before adding infrastructure (broker, cache provider, new database, scheduler/async).
- Verify every change with the project wrapper: `./gradlew build` (plain `gradle` only if no wrapper).
- Use DTOs + Bean Validation at the boundary and `ProblemDetail` for error responses.
- Constructor injection only; no field injection.
- Boot 3 uses `spring-boot-starter-web`; Boot 4 uses `spring-boot-starter-webmvc`.
- Prefer `@ConfigurationProperties` over scattered `@Value`.

### springboot-patterns
- Layers: controller → service → repository; no business logic in controllers.
- Never expose entities; map to DTOs for request and response.
- `@Valid` + Bean Validation on DTOs; centralize errors in `@RestControllerAdvice` + `@ExceptionHandler`.
- `@Transactional` on service methods, not on repositories or controllers.
- Constructor injection; avoid field injection.
- Paginate with `Pageable`; never return unbounded lists.
- Log with SLF4J; never log secrets or PII.
- Do not trust `X-Forwarded-For` unless the app sits behind a trusted proxy.

### java-junit
- Detect JUnit version and assertion library from the build file; copy the conventions already in `src/test/java`.
- Never bump the JUnit major version implicitly.
- Assert values, not bare `assertNotNull`; expected value first; use `assertAll` for grouped assertions.
- Never catch `AssertionError`/`Exception` to make a test pass.
- Prefer a hand-written fake over a mock for stateful interactions; avoid `mockStatic`/`mockConstruction`.
- Inject `java.time.Clock`; never call `Instant.now()`/`LocalDate.now()` in code under test.
- Never add `-DskipTests`, `-Dmaven.test.skip`, `-x test` or `failIfNoTests=false` to force a build.
- Coverage is a smell detector, not a goal; no assertion-free tests to raise a number.

### springboot-tdd
- Cycle: write a failing test → minimal code to pass → refactor with tests green.
- Unit tests: JUnit 5 + Mockito, no Spring context, < 50ms.
- Web layer: `@WebMvcTest` + MockMvc.
- Persistence: `@DataJpaTest`.
- Integration: `@SpringBootTest`; Testcontainers only when Docker is available.
- Prefer AssertJ `assertThat`; avoid partial mocks.
- Enforce coverage with JaCoCo; CI runs the project wrapper (`./gradlew test` or `build`).
- Each test independent; no shared mutable state and no order reliance.

### postgresql-table-design
- Primary key: `BIGINT GENERATED ALWAYS AS IDENTITY`; use `UUID` only for global uniqueness or opacity.
- Normalize to 3NF first; denormalize only for measured, high-ROI reads.
- Add `NOT NULL` where semantically required and `DEFAULT` for common values.
- Index foreign key columns manually; index the filter, sort and join paths actually queried.
- `TIMESTAMPTZ` for event time, `NUMERIC` for money, `TEXT` for strings, `BIGINT` for integers.
- Use unquoted `snake_case` identifiers.
- `UNIQUE` allows multiple NULLs unless `NULLS NOT DISTINCT` (PG15+).
- Avoid `serial`; use identity columns.

### springboot-security
- Hash passwords with BCrypt or Argon2; never store plaintext.
- Prefer stateless tokens with revocation, or `httpOnly` + `Secure` + `SameSite=Strict` session cookies.
- Bind native query parameters (`:param`); never concatenate SQL strings.
- Validate all input with Bean Validation; reject unknown fields.
- Restrict CORS origins; never `*` in production.
- Never log secrets, tokens, passwords or PII.
- Add security headers and rate-limit authentication endpoints.
- Scan dependencies for known CVEs before release.

### spring-boot-test-patterns
- Unit: `@ExtendWith(MockitoExtension.class)`, < 50ms, no Spring context.
- Repository: `@DataJpaTest`; controller: `@WebMvcTest`.
- Integration: `@SpringBootTest`; on Boot 3.5+ prefer `@ServiceConnection` over `@DynamicPropertySource`.
- Never use `@DirtiesContext` unless absolutely necessary.
- Do not mix `@MockBean` configurations; each combination creates a new context.
- Tests independent; no execution-order reliance; no static mutable state.
- Do not use `@SpringBootTest` for unit tests; use plain Mockito.

## Workflow Skills (orchestrator-only — no compact rules)

| Trigger | Skill | Path |
|---------|-------|------|
| Start or resume project foundation; preview plan and delegate phases | `active-orchestrator` | `~/.config/opencode/skills/active-orchestrator/SKILL.md` |
| Generate or update `AGENTS.md` / `CLAUDE.md` from KB and roadmap | `agent-instruction` | `~/.config/opencode/skills/agent-instruction/SKILL.md` |
| Define or audit the project visual identity / design system | `design-system` | `~/.config/opencode/skills/design-system/SKILL.md` |
| UI polish and component design philosophy (v1, compatibility) | `design-taste-frontend-v1` | `~/.config/opencode/skills/design-taste-frontend-v1/SKILL.md` |
| Discovery research before building the knowledge base | `discovery-research` | `~/.config/opencode/skills/discovery-research/SKILL.md` |
| UI polish, animation and invisible detail philosophy | `emil-design-eng` | `~/.config/opencode/skills/emil-design-eng/SKILL.md` |
| Find and install agent skills from the ecosystem | `find-skills` | `~/.config/opencode/skills/find-skill/SKILL.md` |
| Build the 10-file project knowledge base | `kb-creator` | `~/.config/opencode/skills/kb-creator/SKILL.md` |
| Generate `CHANGES.md` roadmap with dependencies and gates | `roadmap-generator` | `~/.config/opencode/skills/roadmap-generator/SKILL.md` |
| Create, edit, evaluate or benchmark skills | `skill-creator` | `~/.config/opencode/skills/skill-creator/SKILL.md` |
| Research a competitor from a URL into fixed-schema notes | `web-scraper` | `~/.config/opencode/skills/web-scraper/SKILL.md` |
| Explore ideas before an OpenSpec change | `openspec-explore` | `~/.agents/skills/openspec-explore/SKILL.md` |
| Propose a change with proposal, design, specs and tasks | `openspec-propose` | `~/.agents/skills/openspec-propose/SKILL.md` |
| Implement tasks from an OpenSpec change | `openspec-apply-change` | `~/.agents/skills/openspec-apply-change/SKILL.md` |
| Sync delta specs to main specs | `openspec-sync-specs` | `~/.agents/skills/openspec-sync-specs/SKILL.md` |
| Revise an OpenSpec change's planning artifacts | `openspec-update-change` | `~/.agents/skills/openspec-update-change/SKILL.md` |
| Archive a completed OpenSpec change | `openspec-archive-change` | `~/.agents/skills/openspec-archive-change/SKILL.md` |

## Project Conventions

| File | Path | Notes |
|------|------|-------|
| OpenSpec config | `openspec/config.yaml` | Artifacts in Spanish; domain turnos odontológicos; telemetry off |
| Roadmap | `CHANGES.md` | C-01…C-14, 7 phases, gates, 5-person plan |
| KB index | `knowledge-base/README.md` | Index of the 10 canonical KB files |
| Foundation state | `.active-orchestrator-state.json` | Phase status, gate decisions, visual-design deferred |
| Agent instructions | `AGENTS.md` | Pending — generated in the agents phase |

No `AGENTS.md` or `.gitignore` exists yet. When `AGENTS.md` is generated, add it here. If a `.gitignore` is created, add `.atl/`.
