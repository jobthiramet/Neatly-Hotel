# Neatly Hotel API

Human-readable reference for client and server collaborators. The machine-generated contract is the OpenAPI spec from springdoc; this file must agree with it.

## 1. Overview

| | |
| --- | --- |
| Base URL (local, both profiles) | `http://localhost:8080/api` |
| Base URL from the Vite client | `/api` (proxied to `:8080`) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

- **`local` profile** (default): in-memory H2, seeded hotel information and the six Figma room types (no images), no Supabase credentials. Storage uploads return `503`.
- **`supabase` profile** (`server/run-supabase.ps1`): Supabase Postgres, plus Supabase Storage when `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are set.

**Auth:** All `/api/profiles/**` and `/api/bookings/**` endpoints require a Clerk session token in `Authorization: Bearer <token>`. `PUT /api/hotel` and `PUT /api/hotel/logo` additionally require `role = agent` in the database profile matching the verified token's `sub` claim. The Supabase profile is read on each request; token role claims and client-supplied roles do not grant access. `POST /api/stripe/webhooks` is public and authenticated by the Stripe-Signature header. Other endpoints remain open. Set `CLERK_ISSUER` and `CLERK_AUTHORIZED_PARTY` on the server to enable Clerk JWT verification.

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
| 400 | Validation failed (`@Valid` body), invalid multipart request, invalid file type, malformed JSON, invalid UUID or number params |
| 404 | Resource not found (`ResourceNotFoundException`) |
| 409 | Resource already exists |
| 413 | Uploaded file too large |
| 429 | Rate limit exceeded (see `Retry-After` header) |
| 401 | Missing or invalid Clerk session token on a protected endpoint |
| 403 | Authenticated account has no profile or its role is not `agent` on a hotel write endpoint |
| 500 | Unexpected error. **Currently also returned for malformed JSON and invalid UUID path params.** |
| 502 | Upstream storage (Supabase) request failed |
| 503 | Storage not configured, or Stripe secret / webhook signing secret missing |

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

Admin Room & Property. Rooms are **soft deleted**: `DELETE` sets `deletedAt`, and deleted rooms are excluded from every endpoint below (`404` by id). Their rows and images are kept for booking history, and a deleted room's `name` can be reused.

Write endpoints are open until admin auth is wired (`TODO(auth)`).

`RoomRequest` (create `room` part and update body):

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `name` | string | yes | Room type. Not blank, โค 120, unique among non-deleted rooms (case-insensitive, `409`) |
| `bedType` | enum | yes | `SINGLE`, `DOUBLE`, `KING` (double bed, king size), `TWIN` |
| `sizeSqm` | integer | yes | 1–10000 |
| `capacity` | integer | yes | Guests, 2–6 |
| `pricePerNight` | number | yes | > 0, โค 2 decimals |
| `promotionPrice` | number \| null | no | > 0, โค 2 decimals, lower than `pricePerNight` (field error `promotionPriceValid`) |
| `description` | string | yes | Not blank, โค 5000 |
| `amenities` | string[] | yes | 1–50 items, each not blank and โค 120. Order is display order. Trimmed; case-insensitive duplicates are dropped |

`RoomResponse`:

| Field | Type |
| --- | --- |
| `id` | UUID |
| `name` | string |
| `bedType` | enum |
| `sizeSqm` | integer |
| `capacity` | integer |
| `pricePerNight` | number |
| `promotionPrice` | number \| null |
| `description` | string |
| `amenities` | string[] |
| `mainImage` | `RoomImage` \| null |
| `gallery` | `RoomImage[]` (display order) |
| `createdAt`, `updatedAt` | timestamp |

`RoomImage`: `{ "id": UUID, "url": string }` (public Supabase Storage URL).

`RoomSummaryResponse` (list row): `id`, `name`, `mainImageUrl` (string \| null), `pricePerNight`, `promotionPrice`, `capacity`, `bedType`, `sizeSqm`.

`PageResponse<T>`: `content` (T[]), `page` (zero-based), `size`, `totalElements`, `totalPages`.

**Image rules:** PNG, JPEG or WEBP only (checked by content type and magic bytes; no SVG), max 5 MB each, one main image plus 4–12 gallery images per room. Objects are stored in the public `room-images` bucket at `rooms/<roomId>/<uuid>.<ext>`; the client filename is never used. Removed or replaced images are deleted from storage after the database update (failures are logged, not returned).

#### `GET /api/rooms`

List non-deleted rooms, newest first.

- Auth: none
- Query params:

| Param | Default | Notes |
| --- | --- | --- |
| `search` | `""` | Case-insensitive substring of room type (`name`) or bed type |
| `page` | `0` | Zero-based; negative is treated as `0` |
| `size` | `10` | Clamped to 1–50 |

- Response `200`:

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "content": [
      { "id": "3f2c1b9e-7a4d-4c8e-9b1a-2d5e6f7a8b9c", "name": "Superior Garden View", "mainImageUrl": "https://<project>.supabase.co/storage/v1/object/public/room-images/rooms/3f2c.../a1b2....jpg", "pricePerNight": 3000.00, "promotionPrice": 2500.00, "capacity": 2, "bedType": "DOUBLE", "sizeSqm": 32 }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 12,
    "totalPages": 2
  },
  "timestamp": "2026-09-15T07:06:56.345Z"
}
```

- A `page` past the last page returns `200` with empty `content` (metadata still describes the full result).
- Rate limit: **60 requests per minute per client IP** (fixed window; `app.rate-limit.*` properties). In-memory per server instance; the client IP is the socket address (`X-Forwarded-For` is not trusted).
- Errors: `400` non-numeric `page`/`size`; `429` rate limit exceeded, with a `Retry-After` header (seconds) and the standard `ErrorResponse` body:

```json
{
  "timestamp": "2026-09-16T07:06:21.222Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many requests, please try again later",
  "path": "/api/rooms",
  "details": []
}
```

#### `GET /api/rooms/{id}`

- Auth: none
- Response `200`: `ApiResponse<RoomResponse>`
- Errors: `400` invalid UUID; `404` room not found or deleted

#### `POST /api/rooms`

Create a room with its images in one request. If any upload or the database write fails, nothing is saved and already-uploaded objects are deleted.

- Auth: none (TODO admin)
- Body (`multipart/form-data`):

| Part | Type | Required |
| --- | --- | --- |
| `room` | `application/json` `RoomRequest` | yes |
| `mainImage` | file | yes |
| `gallery` | file, repeated 4–12 times | yes |

- Response `201`: `ApiResponse<RoomResponse>` with `message: "Room created"`
- Errors: `400` validation failed / invalid image / wrong gallery count / malformed JSON; `409` room type already exists; `413` image too large; `502` storage failure; `503` storage not configured (always on the `local` profile)

#### `PUT /api/rooms/{id}`

Update room fields (images have their own endpoints).

- Auth: none (TODO admin)
- Body (`application/json`): `RoomRequest`
- Response `200`: `ApiResponse<RoomResponse>` with `message: "Room updated"`
- Errors: `400` validation failed; `404`; `409` room type already exists

#### `DELETE /api/rooms/{id}`

Soft delete. Images stay in storage.

- Auth: none (TODO admin)
- Response `200`: `ApiResponse<null>` with `message: "Room deleted"`
- Errors: `404` room not found or already deleted

#### `POST /api/rooms/{id}/images`

Upload one image.

- Auth: none (TODO admin)
- Query params: `main` (boolean, default `false`). `true` replaces the main image and deletes the old object; `false` appends to the gallery.
- Body (`multipart/form-data`): `file`
- Response `200`: `ApiResponse<RoomResponse>` with `message: "Room image uploaded"`
- Errors: `400` invalid image or gallery already has 12; `404`; `413`; `502`; `503`

#### `DELETE /api/rooms/{id}/images/{imageId}`

Remove a gallery image and its storage object.

- Auth: none (TODO admin)
- Response `200`: `ApiResponse<RoomResponse>` with `message: "Room image removed"`
- Errors: `400` image is the main image (upload a replacement instead) or fewer than 4 gallery images would remain; `404` room or image not found

#### `PUT /api/rooms/{id}/images/order`

Reorder the gallery.

- Auth: none (TODO admin)
- Body (`application/json`): `{ "imageIds": [UUID, ...] }`, every gallery image id exactly once, in the new order
- Response `200`: `ApiResponse<RoomResponse>` with `message: "Room images reordered"`
- Errors: `400` ids don't match the gallery; `404`

### Room units

Admin Room Management (physical rooms). Separate from `/api/rooms` (room types / Room & Property). Units are **soft deleted**: `DELETE` sets `deletedAt`. Occupancy for badges is reserved (`occupied` is currently always `false` until booking wiring); `displayStatus` combines Vacant/Occupied with the housekeeping label, except `ASSIGN_*` and `OUT_OF_*` which show the label alone.

Write endpoints are open until admin auth is wired (`TODO(auth)`).

`RoomUnitRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `roomNumber` | string | yes | exactly 4 digits (`^[0-9]{4}$`), unique |
| `roomTypeId` | UUID | yes | must reference a non-deleted room type |
| `statusCode` | string | yes | must match a `room_statuses.code` (case-insensitive) |

`RoomUnitResponse`:

| Field | Type |
| --- | --- |
| `id` | UUID |
| `roomNumber` | string |
| `floor` | integer (derived from room number: 0001–0010 → 1, …) |
| `roomTypeId` | UUID |
| `roomTypeName` | string |
| `bedType` | enum (`SINGLE`, `DOUBLE`, `KING`, `TWIN`) |
| `statusCode` | string |
| `statusLabel` | string |
| `occupied` | boolean |
| `displayStatus` | string (badge label) |

#### `GET /api/room-units`

List non-deleted units, sorted by `roomNumber`.

- Auth: none (TODO admin)
- Response `200`: `ApiResponse<RoomUnitResponse[]>`

#### `GET /api/room-units/statuses`

List housekeeping statuses ordered by `sortOrder`.

- Auth: none
- Response `200`: `ApiResponse<{ id, code, label, sortOrder }[]>`

#### `GET /api/room-units/{id}`

- Auth: none (TODO admin)
- Response `200`: `ApiResponse<RoomUnitResponse>`
- Errors: `404`

#### `POST /api/room-units`

- Auth: none (TODO admin)
- Body: `RoomUnitRequest`
- Response `201`: `ApiResponse<RoomUnitResponse>` with `message: "Room unit created"`
- Errors: `400` validation / invalid status; `404` room type; `409` duplicate room number

#### `PUT /api/room-units/{id}`

- Auth: none (TODO admin)
- Body: `RoomUnitRequest`
- Response `200`: `ApiResponse<RoomUnitResponse>` with `message: "Room unit updated"`
- Errors: `400`; `404`; `409`

#### `DELETE /api/room-units/{id}`

Soft-delete a unit.

- Auth: none (TODO admin)
- Response `204`: empty body
- Errors: `404`

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
- Errors: `401` missing/invalid Clerk token; `403` missing profile or non-agent role; `400` validation failed or malformed JSON; `404` row missing

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

### Bookings

Guest checkout for a signed-in Clerk user. Prices, extras, and promo discounts are calculated on the server from `room_types` and `promotion_codes`. Inventory is `room_types.total_units` minus overlapping stays in `PENDING_PAYMENT`, `CONFIRMED`, or `CHECKED_IN` (expired Stripe drafts do not occupy a unit). Money is THB `numeric(12,2)`; Stripe amounts are that value × 100.

Card payments use Stripe Checkout Sessions with `ui_mode: elements` (Payment Element). Do not send PAN/CVC to this API.

`CreateBookingRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `roomTypeId` | UUID | yes | existing non-deleted room type |
| `checkIn` | date | yes | ISO-8601 date |
| `checkOut` | date | yes | must be after `checkIn` (`stayValid`) |
| `guests` | integer | yes | 1–6, and ≤ room `capacity` |
| `roomsCount` | integer | no | 1–10; default `1` |
| `firstName` | string | yes | not blank; max 100 |
| `lastName` | string | yes | not blank; max 100 |
| `email` | string | yes | email; max 254 |
| `phoneNumber` | string | yes | not blank; max 32 |
| `country` | string | yes | not blank; max 100 |
| `dateOfBirth` | date | yes | in the past; guest must be at least 18 |
| `standardRequestCodes` | string[] | no | known preference codes (e.g. `early-check-in`) |
| `specialRequestCodes` | string[] | no | known paid extras (e.g. `baby-cot`) |
| `additionalRequest` | string | no | max 2000 |
| `promotionCode` | string | no | max 40; unknown or inactive codes are ignored |
| `paymentMethod` | enum | yes | `STRIPE` or `CASH` |

`BookingResponse` (selected fields): `id`, `roomTypeId`, `roomName`, `roomImageUrl`, `checkIn`, `checkOut`, `checkInTimeText`, `checkOutTimeText`, `guests`, `nights`, `roomsCount`, `status` (`PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`, `COMPLETED`, `CANCELLED`, `EXPIRED`), `paymentMethod`, guest fields, `standardRequests` (`{ code, label }[]`), `additionalRequest`, `promotionCode`, `currency` (`THB`), `items` (`kind` `ROOM` \| `ADDON` \| `DISCOUNT`), `roomSubtotal`, `extrasTotal`, `discountTotal`, `grandTotal`, `paymentMethodText`, `clientSecret` (Stripe Checkout client secret while a card payment is still pending; otherwise `null`), `holdExpiresAt`, `cancelledAt`, `createdAt`, `updatedAt`.

Seeded promo: `NEATLYNEW400` (THB 400 off) from `008_bookings_and_payments.sql` / `local_promotion_seed.sql`.

#### `POST /api/bookings`

Create a booking for the signed-in user (`sub` claim).

- Auth: Clerk session token
- Body: `CreateBookingRequest`
- `CASH`: status `CONFIRMED` immediately, payment `UNPAID` (pay at hotel). No `clientSecret`.
- `STRIPE`: status `PENDING_PAYMENT`, 5-minute hold (`holdExpiresAt`), a Checkout Session, and `clientSecret` for `stripe.initCheckoutElementsSdk`. Any other open Stripe draft for this user is expired first.
- Response `201`: `ApiResponse<BookingResponse>` with `message: "Booking created"`
- Errors: `400` validation / unknown extra or preference / guest count exceeds capacity; `401`; `404` room not found; `409` no remaining units for the dates; `502` Stripe API failure; `503` `STRIPE_SECRET_KEY` missing (cash still works)

#### `GET /api/bookings`

List the signed-in user's bookings, newest first. Includes `CONFIRMED`, `CHECKED_IN`, `COMPLETED`, and `CANCELLED` (not `PENDING_PAYMENT` or `EXPIRED`).

- Auth: Clerk session token
- Query: `page` (default `0`), `size` (default `10`, clamped 1–50)
- Response `200`: `ApiResponse<PageResponse<BookingResponse>>`

#### `GET /api/bookings/{id}`

Get one booking owned by the signed-in user. If status is `PENDING_PAYMENT`, the server re-reads the Stripe session and may confirm it (`paid` → `CONFIRMED`) before responding.

- Auth: Clerk session token
- Response `200`: `ApiResponse<BookingResponse>` (`clientSecret` is set only while still pending)
- Errors: `401`; `404` booking not found or not owned by this user

#### `POST /api/bookings/{id}/payment-session`

Create a new Stripe Checkout Session for an unpaid card booking (`PENDING_PAYMENT` or `EXPIRED`). Already `CONFIRMED` bookings are returned unchanged.

- Auth: Clerk session token
- Response `200`: `ApiResponse<BookingResponse>` with a fresh `clientSecret`
- Errors: `400` not a card booking, or status cannot be paid; `401`; `404`; `409` no remaining units; `502` / `503` Stripe

#### `POST /api/stripe/webhooks`

Stripe event receiver. Not wrapped in `ApiResponse`. Hidden from Swagger.

- Auth: none (verify `Stripe-Signature` with `STRIPE_WEBHOOK_SECRET`)
- Body: raw Stripe event JSON
- Handles `checkout.session.completed` (confirm booking, mark charge `SUCCEEDED`) and `checkout.session.expired` (booking `EXPIRED`)
- Duplicate `event.id` values are ignored (`stripe_events`)
- Response `200`: `{}`
- Errors: `400` missing/invalid signature; `503` webhook secret not configured

Local: `stripe listen --forward-to localhost:8080/api/stripe/webhooks`

## 4. Changelog

Newest first. Mark breaking changes with **BREAKING**.

### 2026-09-17

- Added `GET/POST/PUT/DELETE /api/room-units` and `GET /api/room-units/statuses` for Admin Room Management (physical rooms on `room_units` / `room_statuses`).
- Fixed leftover merge conflict markers in this file (hotel PUT errors + 2026-09-16 changelog).
- Room amenities are now stored in a shared `amenities` table. Request and response shapes are unchanged; `amenities` in `RoomRequest` is trimmed and case-insensitive duplicates are dropped (first spelling kept), and an existing amenity name is reused with its stored spelling.
- Added authenticated guest checkout: `POST /api/bookings`, `GET /api/bookings`, `GET /api/bookings/{id}`, `POST /api/bookings/{id}/payment-session`.
- Added public `POST /api/stripe/webhooks` (Checkout Session completed/expired). Card checkout uses Stripe Checkout `ui_mode: elements`; cash confirms immediately as unpaid pay-at-hotel.

### 2026-09-16

- **BREAKING:** `PUT /api/hotel` and `PUT /api/hotel/logo` now require a verified Clerk session token and an `agent` database profile. Guests receive `401`; missing profiles and non-agent accounts receive `403`. Public hotel reads remain available.
- Admin client now sends the Clerk session token when saving hotel information and uploading a logo.
- `GET /api/rooms` is rate limited to 60 requests per minute per client IP and returns `429` with `Retry-After` when exceeded.
- Documented the default page size (10) and that an out-of-range `page` returns an empty page.

### 2026-09-15

- **BREAKING:** `GET /api/rooms` now returns `PageResponse<RoomSummaryResponse>` (with `search`, `page`, `size`) instead of a `RoomResponse[]`, and excludes soft-deleted rooms.
- **BREAKING:** `POST /api/rooms` is now `multipart/form-data` (`room` JSON + `mainImage` + 4–12 `gallery` files). `CreateRoomRequest` is replaced by `RoomRequest`.
- **BREAKING:** `RoomResponse` drops `type` and `active`; adds `bedType`, `sizeSqm`, `promotionPrice`, `description`, `amenities`, `mainImage`, `gallery`, `createdAt`, `updatedAt`. `pricePerNight` must now be > 0.
- Added `PUT /api/rooms/{id}`, soft `DELETE /api/rooms/{id}`, `POST /api/rooms/{id}/images`, `DELETE /api/rooms/{id}/images/{imageId}` and `PUT /api/rooms/{id}/images/order`.
- Malformed JSON bodies and invalid UUID/number params now return `400` instead of `500` (all endpoints).

### 2026-09-14

- Added authenticated `GET /api/profiles/me` and `POST /api/profiles`; profile ownership now comes from the Clerk JWT `sub` claim.
- Added authenticated `POST /api/profiles/picture`; profile images are uploaded server-side with the Supabase service-role key and PostgreSQL stores only the object path.
- Profile creation now requires users to be at least 18 years old.
- Added `GET /api/hotel`, `PUT /api/hotel` and `PUT /api/hotel/logo` (hotel information and logo upload to Supabase Storage).
- Documented existing `GET /api/health` and `/api/rooms` endpoints.
