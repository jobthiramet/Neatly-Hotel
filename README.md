# Neatly-Hotel

Hotel booking monorepo: **Spring Boot** API + **Vue 3** client + **Supabase** (PostgreSQL / Auth).

## Structure

```
Neatly-Hotel/
??? server/     # Java Spring Boot (port 8080)
??? client/     # Vue 3 + Vite + TypeScript (port 5173)
??? README.md
```

## Server package layout

```
com.neatly.hotel/
??? HotelApplication.java
??? config/       # Cors, Security, OpenAPI/Swagger
??? controller/   # REST endpoints
??? service/      # Business logic (interface + impl)
??? repository/   # JpaRepository
??? model/        # JPA entities
??? dto/          # Request/response objects
??? exception/    # Global handlers + custom errors
```

## Prerequisites

- JDK 21+ (JDK 25 works; project targets Java 21)
- Node.js 22+
- Supabase project (https://supabase.com)
- Maven is **not** required globally ? use `mvnw` in `server/`

## Setup

### 1. Client

```bash
cd client
copy .env.example .env
npm install
npm run dev
```

### 2. Server (local ? H2)

```bash
cd server
.\mvnw.cmd spring-boot:run
```

- Health: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- Rooms API: http://localhost:8080/api/rooms
- API reference: [docs/API.md](docs/API.md)

### 3. Server with Supabase PostgreSQL

1. Create `rooms` table: run `server/src/main/resources/db/schema.sql` in Supabase **SQL Editor**
2. Copy `server/.env.example` ? `server/.env` and fill Database credentials (Session pooler, port **5432**)

```powershell
cd server
.\run-supabase.ps1
```

## Design system

The client UI uses the Neatly design system (tokens + shadcn-vue components) from Figma.
Read **[client/DESIGN_SYSTEM.md](client/DESIGN_SYSTEM.md)** before building UI. It covers tokens, components and lint rules.
With `npm run dev` running, the live showcase is at http://localhost:5173/design-system.

## Git branches

- `main` ? production-ready
- `dev` ? integration / testing
- `feat/...` ? feature work ? merge into `dev` first, then `dev` ? `main`

## Contributing

- Branch off `dev` and open PRs back into `dev`. Never target `main`.
- Use [Conventional Commits](https://www.conventionalcommits.org) for commit messages and PR titles.
- **AI coding agents** (Claude Code, Cursor, Copilot, Codex, …) must follow **[AGENTS.md](AGENTS.md)**. It holds the workflow, component-reuse rules, code conventions and git conventions for humans too.
