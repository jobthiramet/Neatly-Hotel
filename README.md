# Neatly-Hotel

Hotel booking monorepo: **Spring Boot** API + **Vue 3** frontend + **Supabase** (PostgreSQL / Auth).

## Structure

```
Neatly-Hotel/
??? backend/     # Java Spring Boot (port 8080)
??? frontend/    # Vue 3 + Vite + TypeScript (port 5173)
??? README.md
```

## Backend package layout

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
- Maven is **not** required globally ? use `mvnw` in `backend/`

## Setup

### 1. Frontend

```bash
cd frontend
copy .env.example .env
npm install
npm run dev
```

### 2. Backend (local ? H2)

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

- Health: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- Rooms API: http://localhost:8080/api/rooms

### 3. Backend with Supabase PostgreSQL

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="supabase"
$env:SUPABASE_DB_URL="jdbc:postgresql://..."
$env:SUPABASE_DB_USER="postgres.YOUR_REF"
$env:SUPABASE_DB_PASSWORD="..."
.\mvnw.cmd spring-boot:run
```

## Git branches

- `main` ? production-ready
- `dev` ? integration / testing
- `feat/...` ? feature work ? merge into `dev` first, then `dev` ? `main`
