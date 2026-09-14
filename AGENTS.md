# AGENTS.md

Rules for AI coding agents (Claude Code, Cursor, Copilot, Codex, …) working in this repo. Follow them exactly.

## 1. Project overview

- Hotel booking monorepo: **Vue 3** client, **Spring Boot** API, **Supabase** (PostgreSQL / Auth).
- `client/`: Vue 3 + Vite + TypeScript + Tailwind CSS v4 + shadcn-vue (Reka UI), Pinia, Vue Router. Dev server on `:5173` proxies `/api` to `:8080`.
- `server/`: Spring Boot (Java 21, Maven wrapper), Spring Security, springdoc OpenAPI/Swagger, JPA. `local` profile = in-memory H2, `supabase` profile = Supabase Postgres.
- UI design system: **[client/DESIGN_SYSTEM.md](client/DESIGN_SYSTEM.md)**. Read it before any UI work.
- API reference: **[docs/API.md](docs/API.md)**. Keep it in sync with Swagger (see §12).

## 2. Commands

Client (run in `client/`):

| Task | Command |
| --- | --- |
| Install | `npm install` (copy `.env.example` to `.env` first) |
| Dev server | `npm run dev` (showcase at `/design-system`) |
| Type-check | `npm run type-check` |
| Build (type-check + vite build) | `npm run build` |
| Lint (oxlint → eslint → stylelint) | `npm run lint` |
| Test | none yet. No test runner is configured. |

`npm run lint` runs oxlint and eslint with `--fix`, so it can modify files. Review the diff afterwards and revert fixes to files outside your task.

Server (run in `server/`; on macOS/Linux use `./mvnw` instead of `.\mvnw.cmd`):

| Task | Command |
| --- | --- |
| Run (H2, no credentials) | `.\mvnw.cmd spring-boot:run` |
| Run against Supabase | `.\run-supabase.ps1` (needs `server/.env` from `.env.example`) |
| Test | `.\mvnw.cmd test` |
| Build | `.\mvnw.cmd package` |
| Lint | none configured |

Swagger UI: `http://localhost:8080/swagger-ui.html`. Health: `http://localhost:8080/api/health`.

## 3. Workflow

- For anything beyond a trivial change: read the relevant code, propose a short plan, and wait for approval.
- Stay within the requested scope. No drive-by refactors, renames or formatting changes to unrelated files. Mention those as suggestions instead.
- Before saying a task is done, run checks for every part you touched and report results honestly (including failures):
  - client: `npm run lint` and `npm run build`
  - server: `.\mvnw.cmd test`
  - server API changes: `docs/API.md` updated and consistent with Swagger UI.

## 4. Component reuse (client)

- Before creating UI, search `client/src/components/` and [DESIGN_SYSTEM.md §3](client/DESIGN_SYSTEM.md) for an existing component. Existing ones include:
  - `ui/`: `button`, `input`, `label`, `form-field`, `select`, `checkbox`, `radio-group`, `date-picker`, `calendar`, `popover`, `dialog`, `badge`, `stepper`, `menu-link`, `payment-option`
  - `icons/` (Figma icons, `Icon*.vue`), `NeatlyLogo.vue`
- Prefer, in order: **use an existing component → extend it with a prop or variant → compose existing components.**
- **Ask the user before creating a new component.** Explain why existing ones don't fit and propose its name, location and API (props, emits, slots). Once approved, follow [DESIGN_SYSTEM.md §5 "Adding a component"](client/DESIGN_SYSTEM.md).
- Use design tokens only. No hard-coded colors, no arbitrary Tailwind values (`bg-[#fff]`, `p-[13px]`). Follow [DESIGN_SYSTEM.md §4 "Rules"](client/DESIGN_SYSTEM.md).

## 5. Engineering principles

- Prefer readable code over clever code.
- Prioritize maintainability.
- Follow DRY, KISS, SRP, High Cohesion, and Loose Coupling.
- Keep components focused on a single responsibility.
- Do not over-engineer.
- Do not introduce unnecessary abstractions.

**When multiple solutions exist:**

1. Choose the simplest.
2. Choose the one that best matches the existing codebase.
3. Avoid introducing new patterns unless necessary.

Extract shared code when the same logic appears a third time (the rule of three). Until then, a little duplication is better than the wrong abstraction.

## 6. Code conventions

Client (`client/src/`):

- SFCs are PascalCase and use `<script setup lang="ts">`. Props via `defineProps<{ … }>()`.
- Pages: `views/<Name>View.vue`, registered in `router/index.ts`.
- UI primitives: `components/ui/<kebab-name>/<PascalName>.vue` plus an `index.ts` barrel. Import from the folder: `import { Button } from '@/components/ui/button'`.
- Import with the `@/` alias, not long relative paths.
- Merge classes with `cn()` from `@/lib/utils`.
- Composables: `useX.ts` in `src/composables/` (folder not created yet; alias is set in `components.json`).
- Stores: Pinia setup stores in `stores/`, named `useXStore`.
- HTTP: go through the axios instance in `api/client.ts`. Supabase client: `lib/supabase.ts`.

Server (`server/src/main/java/com/neatly/hotel/`):

- Layering: `controller/` → `service/` → `repository/`. Controllers hold no business logic and never call repositories directly.
- Services: interface `XService` plus `XServiceImpl` annotated `@Service` and `@Transactional` (`readOnly = true` on reads).
- Repositories: `XRepository extends JpaRepository<X, UUID>`. Entities in `model/` extend `BaseEntity`.
- DTOs in `dto/` are Java records: `CreateXRequest` with jakarta validation (`@NotBlank`, `@NotNull`, …), `XResponse` with a static `from(entity)`. Never return entities from controllers.
- Controllers use `@Valid @RequestBody`, return `ApiResponse<T>`, and carry `@Tag` / `@Operation` annotations.
- Errors: throw `ResourceNotFoundException` / `ApiException`; `GlobalExceptionHandler` maps them.
- Use constructor injection (no field `@Autowired`).

## 7. Ask before you

- add, remove or upgrade a dependency (including adding a test runner)
- create a new component, page route or API endpoint
- introduce a new pattern, library or architectural layer not already in the codebase
- change the database schema or write a migration (never destructive changes without explicit approval)
- change auth or security config (`SecurityConfig`), CORS (`CorsConfig`), or environment variables
- change a shared API contract (request/response shapes) used by the other side of the stack
- delete files

## 8. Never

- push to, commit on, or open PRs into `main`. `main` only receives merges from `dev`, done by a human maintainer.
- merge your own PR into `dev` unless the user explicitly says to
- commit secrets, `.env` files or API keys (use `.env.example`)
- force-push shared branches (`main`, `dev`)
- skip hooks (`--no-verify`)
- disable lint rules to make errors go away

## 9. Git: branches

- `dev` is the integration branch. All work starts from it and returns to it.
- Before starting: `git checkout dev && git pull origin dev`, then create `<type>/<short-kebab-description>` from `dev` (e.g. `feat/room-booking-form`, `fix/login-redirect`).
- Before creating a branch or committing, check the current branch (`git branch --show-current`). If it is `main`, stop and switch to `dev` first.
- Flow: `feature branch → PR → dev → (maintainer) → main`

## 10. Git: Conventional Commits

Format: `<type>(<scope>): <subject>`

| Type | Meaning |
| --- | --- |
| `feat` | a new feature |
| `fix` | a bug fix |
| `docs` | documentation only |
| `style` | formatting, no code meaning change |
| `refactor` | code change that neither fixes a bug nor adds a feature |
| `perf` | performance improvement |
| `test` | add or fix tests |
| `build` | build system or dependencies (npm, Maven) |
| `ci` | CI configuration |
| `chore` | maintenance that doesn't touch src or tests |
| `revert` | revert a previous commit |

- Scopes: `client`, `server`, `db`, `design-system`, `docs`, `ci`. Omit the scope if none fits.
- Subject: imperative mood, lowercase, no trailing period, ≤ 72 characters.
- Body: explain *why*, wrapped at 72 characters. Grouped commits list the included changes as bullets.
- Footer: `BREAKING CHANGE: ...` (or `!` after type/scope), plus issue refs like `Closes #12`.
- **Group related work into as few commits as practical.**
  - One commit per feature or per task/prompt request, not one per file, section or small step.
  - Fold follow-ups to unpushed work (review feedback, visual, lint or copy fixes) into the existing commit with `git commit --amend`, or squash them in before pushing.
  - Never commit work-in-progress, "fix typo" or "fix lint" commits on their own.
- **Split into separate commits only when:** changes are unrelated features that could be reverted independently; a change touches a different part of the stack with its own scope (`client` vs `server` vs `db` migration); or the user explicitly asks.
- **History safety:** only amend or squash commits that have **not been pushed**. Check `git log @{u}..HEAD` (or confirm there's no upstream) first. Never rewrite pushed commits on shared branches (`dev`, `main`).
- **Grouped commit messages:** the subject summarises the whole change; the body lists the included changes as bullet points.
- **Attribution:** no `Co-Authored-By` trailers and no mention of Claude or AI in commit messages or PR descriptions.

Examples:

```text
feat(client): build home page sections from figma

- Add hero with booking search form
- Add gallery carousel with infinite loop
- Link footer social icons to platform homepages
- Add placeholder testimonials
```

```text
fix(server): return 404 when room id does not exist
feat(db): add bookings table to schema.sql
docs(design-system): document stepper states
feat(server)!: rename pricePerNight to nightlyRate in room responses

BREAKING CHANGE: RoomResponse.pricePerNight is now nightlyRate.
Clients must update their field access.
Closes #12
```

## 11. Pull requests

- Always target `dev` as the base branch, never `main`.
- Title in Conventional Commit format.
- Description covers: what, why, how it was tested, screenshots for UI changes, notes on any DB migration.
- PRs that change the API must include the `docs/API.md` diff.
- Keep PRs small. Rebase on the latest `dev` before opening.

## 12. API & data

- The OpenAPI spec (generated by springdoc from controller annotations and DTOs) is the contract. When you change an endpoint, update its `@Operation` / `@Tag` annotations, DTOs and validation in the same change, and check Swagger UI.
- Any change to an endpoint (add, remove, rename, request/response shape, validation, status codes, auth) must update [`docs/API.md`](docs/API.md) in the same commit, including a Changelog entry. Mark breaking changes clearly.
- If a request/response shape changes, update the client callers in the same PR or flag it clearly.
- Supabase schema changes go through SQL files committed under `server/src/main/resources/db/` (update `schema.sql` or add a new numbered file) **before** they are run in Supabase. Never make ad-hoc schema edits in the dashboard.
- The `supabase` profile uses `ddl-auto=validate`: entities in `model/` must match the SQL schema.

## 13. When unsure

- Ask a concise question rather than guessing.
- Prefer reading existing code over assuming patterns.
