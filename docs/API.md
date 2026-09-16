# Neatly Hotel API

Human-readable reference for client and server collaborators. The machine-generated contract is the OpenAPI spec from springdoc; this file must agree with it.

## 1. Overview

| | |
| --- | --- |
| Base URL (local, both profiles) | `http://localhost:8080/api` |
| Base URL from the Vite client | `/api` (proxied to `:8080`) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

- **`local` profile** (default): in-memory H2, seeded hotel information, no Supabase credentials. Storage uploads return `503`.
- **`supabase` profile** (`server/run-supabase.ps1`): Supabase Postgres, plus Supabase Storage when `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are set.

**Auth:** All `/api/profiles/**` endpoints require a Clerk session token in `Authorization: Bearer <token>`. `PUT /api/hotel` and `PUT /api/hotel/logo` additionally require `role = agent` in the database profile matching the verified token's `sub` claim. The Supabase profile is read on each request; token role claims and client-supplied roles do not grant access. Other endpoints remain open. Set `CLERK_ISSUER` and `CLERK_AUTHORIZED_PARTY` on the server to enable Clerk JWT verification.

## 2. Conventions

### Success envelope: `ApiResponse<T>`

Every endpoint except `GET /api/health` wraps its payload:

```json
{
  "success": true,
  "message": "OK",
  "data": { },
  "timestamp": "2026-09-14T07:06:56.345Z"
}
```

### Error shape: `ErrorResponse` (from `GlobalExceptionHandler`)

```json
{
  "timestamp": "2026-09-14T07:06:21.222Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/hotel",
  "details": ["name: must not be blank"]
}
```

`details` lists field errors for validation failures and is otherwise empty (or holds one message).

### Status codes

| Code | When |
| --- | --- |
| 200 | OK |
| 201 | Resource created |
| 400 | Validation failed (`@Valid` body), invalid multipart request, invalid file type |
| 404 | Resource not found (`ResourceNotFoundException`) |
| 409 | Resource already exists |
| 413 | Uploaded file too large |
| 401 | Missing or invalid Clerk session token on a protected endpoint |
| 403 | Authenticated account has no profile or its role is not `agent` on a hotel write endpoint |
| 500 | Unexpected error. **Currently also returned for malformed JSON and invalid UUID path params.** |
| 502 | Upstream storage (Supabase) request failed |
| 503 | Storage not configured |

Swagger UI documents authentication errors on hotel write endpoints; other error codes below come from the handler and services. Security-filter `401` and `403` responses do not use the `ErrorResponse` envelope.

### Formats

- IDs: UUID strings, e.g. `"3f2c1b9e-7a4d-4c8e-9b1a-2d5e6f7a8b9c"`.
- Timestamps: ISO-8601 UTC instants, e.g. `"2026-09-14T07:06:56.329Z"`.
- Money: JSON number (`BigDecimal`, 2 decimal places in the DB).

## 3. Endpoints

### Health

#### `GET /api/health`

Liveness check. **Not wrapped** in `ApiResponse`.

- Auth: none
- Response `200`:

```json
{ "status": "ok", "service": "neatly-hotel-api" }
```

### Rooms

`RoomResponse`:

| Field | Type |
| --- | --- |
| `id` | UUID |
| `name` | string |
| `type` | string |
| `pricePerNight` | number |
| `capacity` | integer |
| `active` | boolean |

#### `GET /api/rooms`

List all rooms.

- Auth: none
- Response `200`:

```json
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": "3f2c1b9e-7a4d-4c8e-9b1a-2d5e6f7a8b9c", "name": "Superior Garden View", "type": "Superior", "pricePerNight": 2500.00, "capacity": 2, "active": true }
  ],
  "timestamp": "2026-09-14T07:06:56.345Z"
}
```

#### `GET /api/rooms/{id}`

Get one room.

- Auth: none
- Path params: `id` (UUID, required)
- Response `200`: `ApiResponse<RoomResponse>`
- Errors: `404` room not found; `500` if `id` is not a valid UUID

#### `POST /api/rooms`

Create a room. New rooms are `active: true`.

- Auth: none
- Body (`application/json`, `CreateRoomRequest`):

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `name` | string | yes | not blank |
| `type` | string | yes | not blank |
| `pricePerNight` | number | yes | ≥ 0 |
| `capacity` | integer | yes | ≥ 1 |

- Response `201`: `ApiResponse<RoomResponse>` with `message: "Room created"`
- Errors: `400` validation failed; `500` malformed JSON

### Hotel information

Single record for the hotel, shown on the Home `#about` section and edited in admin / hotel information.

`HotelInfoResponse`:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | UUID | |
| `name` | string | |
| `description` | string | paragraphs separated by a blank line (`\n\n`) |
| `logoUrl` | string \| null | public Supabase Storage URL; `null` until a logo is uploaded |
| `updatedAt` | timestamp | |

#### `GET /api/hotel`

Get hotel information.

- Auth: none (public)
- Response `200`:

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "00000000-0000-0000-0000-000000000001",
    "name": "Neatly Hotel",
    "description": "Set in Bangkok, Thailand. ...\n\nAll units at the hotel ...",
    "logoUrl": "https://YOUR_PROJECT_REF.supabase.co/storage/v1/object/public/hotel-assets/logo/0b1e6c2a-5d7f-4b8e-9a3c-1f2d4e6a8b0c.png",
    "updatedAt": "2026-09-14T07:06:56.329Z"
  },
  "timestamp": "2026-09-14T07:06:56.345Z"
}
```

- Errors: `404` hotel information row missing (seed not run)

#### `PUT /api/hotel`

Update name and description. Values are trimmed before saving.

- Auth: Clerk session token and database profile with role `agent`
- Body (`application/json`, `UpdateHotelInfoRequest`):

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `name` | string | yes | not blank, max 120 chars |
| `description` | string | yes | not blank, max 5000 chars |

```json
{ "name": "Neatly Hotel", "description": "First paragraph.\n\nSecond paragraph." }
```

- Response `200`: `ApiResponse<HotelInfoResponse>` with `message: "Hotel information updated"`
- Errors: `401` missing/invalid Clerk token; `403` missing profile or non-agent role; `400` validation failed; `404` row missing; `500` malformed JSON

#### `PUT /api/hotel/logo`

Upload a new logo and replace the old one. The server stores it as `logo/<uuid>.<ext>` in the public `hotel-assets` bucket (the client filename is ignored), saves the new URL, then deletes the previous object. If the upload fails, nothing changes.

- Auth: Clerk session token and database profile with role `agent`
- Body: `multipart/form-data`

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `file` | binary | yes | `image/png`, `image/jpeg` or `image/webp` (file content must match the type); max 2 MB. SVG is not allowed. |

- Response `200`: `ApiResponse<HotelInfoResponse>` with `message: "Hotel logo updated"` and the new `logoUrl`
- Errors:

| Code | `message` |
| --- | --- |
| 401 | Missing or invalid Clerk token (security-filter response) |
| 403 | Missing profile or non-agent role (security-filter response) |
| 400 | `Invalid multipart request` (missing `file` part or not multipart) |
| 400 | `Logo file is required` (empty file) |
| 400 | `Logo must be a PNG, JPEG or WEBP image` |
| 404 | `Hotel information not found` |
| 413 | `File exceeds maximum upload size` / `Logo must be 2 MB or smaller` |
| 502 | `Failed to upload file to storage` |
| 503 | `Storage is not configured: set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY` |

### User profiles

`ProfileResponse`:

| Field | Type | Notes |
| --- | --- | --- |
| `clerkUserId` | string | Clerk user ID |
| `firstName` | string | |
| `lastName` | string | |
| `phoneNumber` | string | E.164 format |
| `dateOfBirth` | date | ISO-8601 date |
| `country` | string | |
| `profilePicture` | string \| null | Supabase Storage object path |
| `role` | string | New profiles default to `user` |
| `createdAt` | timestamp | |
| `updatedAt` | timestamp | |

#### `GET /api/profiles/me`

Get the signed-in user's profile. The server reads the Clerk user ID from the verified token's `sub` claim.

- Auth: Clerk session token (`Authorization: Bearer <token>`)
- Response `200`: `ApiResponse<ProfileResponse>`
- Errors: `404` profile not found

#### `POST /api/profiles`

Create a profile for the signed-in user. The server reads `clerkUserId` from the verified token's `sub` claim; it is not accepted from the request body. Text values are trimmed before saving and the role is always set to `user`.

- Auth: Clerk session token (`Authorization: Bearer <token>`)
- Body (`application/json`, `CreateProfileRequest`):

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `firstName` | string | yes | not blank; max 100 chars |
| `lastName` | string | yes | not blank; max 100 chars |
| `phoneNumber` | string | yes | E.164 format (`+` and 8–15 digits) |
| `dateOfBirth` | date | yes | must be in the past and the user must be at least 18 years old |
| `country` | string | yes | not blank; max 100 chars |
| `profilePicture` | string | no | max 500 chars |

- Response `201`: `ApiResponse<ProfileResponse>` with `message: "Profile created"`
- Errors: `400` validation failed; `409` profile already exists

#### `POST /api/profiles/picture`

Upload or replace the signed-in user's profile picture. The server derives the Clerk user ID from the verified JWT, uploads with the Supabase service-role key, and returns the object path. PostgreSQL stores this path rather than image bytes or a full public URL.

- Auth: Clerk session token (`Authorization: Bearer <token>`)
- Body: `multipart/form-data`

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `file` | binary | yes | PNG, JPEG or WEBP; file content must match the declared type; max 5 MB |

- Response `200`: `ApiResponse<string>` with `message: "Profile picture uploaded"`

```json
{
  "success": true,
  "message": "Profile picture uploaded",
  "data": "users/user_abc123/avatar.png",
  "timestamp": "2026-09-14T08:00:00Z"
}
```

- Errors: `400` invalid or missing image; `401` missing/invalid Clerk token; `413` image too large; `502` Supabase Storage failure; `503` Storage not configured

## 4. Changelog

Newest first. Mark breaking changes with **BREAKING**.

### 2026-09-16

- **BREAKING:** `PUT /api/hotel` and `PUT /api/hotel/logo` now require a verified Clerk session token and an `agent` database profile. Guests receive `401`; missing profiles and non-agent accounts receive `403`. Public hotel reads remain available.
- Admin client now sends the Clerk session token when saving hotel information and uploading a logo.

### 2026-09-14

- Added authenticated `GET /api/profiles/me` and `POST /api/profiles`; profile ownership now comes from the Clerk JWT `sub` claim.
- Added authenticated `POST /api/profiles/picture`; profile images are uploaded server-side with the Supabase service-role key and PostgreSQL stores only the object path.
- Profile creation now requires users to be at least 18 years old.
- Added `GET /api/hotel`, `PUT /api/hotel` and `PUT /api/hotel/logo` (hotel information and logo upload to Supabase Storage).
- Documented existing `GET /api/health` and `/api/rooms` endpoints.
