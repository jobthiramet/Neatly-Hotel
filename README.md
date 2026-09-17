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

1. Create `room_types` table: run `server/src/main/resources/db/schema.sql` in Supabase **SQL Editor**
2. Copy `server/.env.example` ? `server/.env` and fill Database credentials (Session pooler, port **5432**)

```powershell
cd server
.\run-supabase.ps1
```

### 4. Seeding rooms

The six room types from Figma live in `server/seed/rooms.json`, with images in `server/seed/rooms/`.

- **supabase:** run `db/005_rooms.sql`, `db/006_storage_room_images.sql`, `db/007_rename_rooms_to_room_types.sql` and `db/008_room_cleanup_and_amenities.sql` once, in order (all are safe to re-run). Start the server with `.\run-supabase.ps1`, then in another terminal:

  ```powershell
  cd server
  .\seed-rooms.ps1                                  # or -ApiBase http://localhost:8081/api
  ```

  It creates each room through `POST /api/rooms`, so rows, `room_type_images` and bucket objects match real uploads. Rooms whose name already exists are skipped, so it's safe to re-run. macOS/Linux: `pwsh ./seed-rooms.ps1`.
  Then run `db/009_room_statuses_and_units.sql` (housekeeping statuses and the 40 room units, matched by room type name), `db/010_bookings.sql` (bookings, double-booking guard, occupancy view), `db/011_bookings_checkout.sql` (checkout columns, payments, promo codes), and `db/012_drop_legacy_booking_columns.sql` (drops leftover `bookings.room_type_id` / `rooms_count` / `grand_total` from the old checkout table).
- **local (H2):** nothing to run. The same six rooms (without images) are seeded on startup from `db/local_rooms_seed.sql`. Running the script there just skips them.

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
