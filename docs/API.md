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

**Auth:** All `/api/profiles/**` and `/api/bookings/**` endpoints require a Clerk session token in `Authorization: Bearer <token>`. `GET /api/admin/analytics`, `PUT /api/hotel`, `PUT /api/hotel/logo`, `PUT /api/chatbot`, and every `/api/promotion-codes/**` endpoint except `GET /api/promotion-codes/preview` additionally require `role = agent` in the database profile matching the verified token's `sub` claim. The Supabase profile is read on each request; token role claims and client-supplied roles do not grant access. `POST /api/stripe/webhooks` is public and authenticated by the Stripe-Signature header. Other endpoints remain open. Set `CLERK_ISSUER` and `CLERK_AUTHORIZED_PARTY` on the server to enable Clerk JWT verification.

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
| 403 | Authenticated account has no profile or its role is not `agent` on an agent-only endpoint |
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

#### `GET /api/rooms/available`

Search Result page: room types that can be booked for a stay.

- Auth: none
- Query params (all required):

| Param | Type | Validation |
| --- | --- | --- |
| `checkIn` | date `YYYY-MM-DD` | Not before today in hotel time (`app.hotel.time-zone`, default `Asia/Bangkok`) |
| `checkOut` | date `YYYY-MM-DD` | After `checkIn` (field error `checkOutValid`); stay at most `app.search.max-nights` (default 30) nights |
| `rooms` | integer | 1–10 |
| `guests` | integer | 1–6 |

Optional:

| Param | Type | Validation |
| --- | --- | --- |
| `roomTypeIds` | UUID, repeatable (`?roomTypeIds=…&roomTypeIds=…`) | At most 20. Omitted or empty searches every room type; ids that don't exist simply match nothing (no `400`) |

- Availability rules:
  - **Bookable units** of a room type: non-deleted `room_units` whose status is not `OUT_OF_ORDER` or `OUT_OF_SERVICE`. Housekeeping statuses (clean, dirty, inspected, …) don't affect future stays.
  - **Booked units**: `booking_rooms` rows of that type whose booking overlaps the stay (`booking.check_in < checkOut AND booking.check_out > checkIn`) and is `CONFIRMED`, `CHECKED_IN`, or `PENDING_PAYMENT` with an unexpired hold. A checkout day can be another stay's check-in day. Bookings are counted per room type because `room_unit_id` stays null until a unit is assigned.
  - Only the requested `roomTypeIds` are considered, when given.
  - A room type is returned when `bookable − booked ≥ rooms` and `capacity × rooms ≥ guests`. Soft-deleted room types never appear.
- Response `200`: `ApiResponse<AvailableRoomResponse[]>`, cheapest `pricePerNight` first. **Empty array (not `404`) when nothing is available.**

`AvailableRoomResponse`: the `RoomSummaryResponse` fields plus `description` (string) and `availableUnits` (integer).

```json
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": "3f2c1b9e-7a4d-4c8e-9b1a-2d5e6f7a8b9c", "name": "Superior Garden View", "mainImageUrl": "https://<project>.supabase.co/storage/v1/object/public/room-images/rooms/3f2c.../a1b2....jpg", "pricePerNight": 3100.00, "promotionPrice": 2500.00, "capacity": 2, "bedType": "DOUBLE", "sizeSqm": 32, "description": "Rooms (36sqm) with full garden views, ...", "availableUnits": 8 }
  ],
  "timestamp": "2026-09-18T07:06:56.345Z"
}
```

- Errors:
  - `400` `Validation failed` with field `details`, e.g. `["checkOutValid: check-out must be after check-in", "rooms: must be greater than or equal to 1"]`. A missing param gives `must not be null`; a non-ISO date gives `checkIn: invalid value`.
  - `400` with `message` `checkIn: must not be in the past (hotel time, Asia/Bangkok)` or `checkOut: stay must be at most 30 nights` (`details` empty).
  - `429` rate limit (same limit and body as `GET /api/rooms`).

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

### Promotion codes

Admin Promo code page. Codes are **soft deleted**: `DELETE` sets `deletedAt` and hides the row from the list. Past bookings keep the foreign key. A code applies at checkout only when it is active, not deleted, the pre-discount total (room subtotal + extras) is at least `minPurchaseAmount`, and the booked room type is in `roomTypes` (an empty list means every room type). `FIXED` subtracts `amountOff` baht. `PERCENT` subtracts `percentOff` percent of that same total, rounded half up to 2 decimals. The grand total is never negative.

`PromotionCodeRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `code` | string | yes | 1–40 characters, letters, numbers, and hyphens; stored uppercase; unique among non-deleted codes |
| `discountType` | enum | yes | `FIXED` or `PERCENT` |
| `amountOff` | number \| null | when `FIXED` | > 0, ≤ 2 decimals. Omit or send null for `PERCENT` |
| `percentOff` | number \| null | when `PERCENT` | > 0 and ≤ 100, ≤ 2 decimals. Omit or send null for `FIXED` |
| `minPurchaseAmount` | number | yes | ≥ 0, ≤ 2 decimals. Compared with room subtotal + extras |
| `roomTypeIds` | UUID[] | no | max 50; each id must be a non-deleted room type. Empty or omitted means every room type |

`PromotionCodeResponse`: `id`, `code`, `discountType`, `amountOff` (null for percent), `percentOff` (null for fixed), `minPurchaseAmount`, `roomTypes` (`{ id, name }[]`; empty means every room type). Deleted room types are omitted from `roomTypes`.

#### `GET /api/promotion-codes/preview`

Public checkout check for a single code. Used by the booking payment field. Does not list other codes.

- Auth: none
- Query: `code` (string), `roomTypeId` (UUID), `purchase` (number, pre-discount room + extras, ≥ 0)
- Response `200`: `ApiResponse<PromotionCodePreviewResponse>`
  - `status`: `APPLIED`, `NOT_FOUND` (unknown, inactive, or deleted), `ROOM_NOT_ELIGIBLE`, or `BELOW_MINIMUM`
  - `discountAmount`: positive number when `APPLIED`, otherwise `null`
  - `minPurchaseAmount`: set when `BELOW_MINIMUM`, otherwise `null`
- Errors: `400` when `purchase` is missing or negative; `404` when `roomTypeId` is not a live room type

#### `GET /api/promotion-codes`

List non-deleted codes, sorted by `code`.

- Auth: Clerk session token and `role = agent`
- Response `200`: `ApiResponse<PromotionCodeResponse[]>`
- Errors: `401`; `403`

#### `GET /api/promotion-codes/{id}`

- Auth: Clerk session token and `role = agent`
- Response `200`: `ApiResponse<PromotionCodeResponse>`
- Errors: `401`; `403`; `404`

#### `POST /api/promotion-codes`

- Auth: Clerk session token and `role = agent`
- Body: `PromotionCodeRequest`
- Response `201`: `ApiResponse<PromotionCodeResponse>` with `message: "Promo code created"`
- Errors: `400` validation, missing amount for the selected type, or unknown room type; `401`; `403`; `409` duplicate code

#### `PUT /api/promotion-codes/{id}`

- Auth: Clerk session token and `role = agent`
- Body: `PromotionCodeRequest`
- Response `200`: `ApiResponse<PromotionCodeResponse>` with `message: "Promo code updated"`
- Errors: `400`; `401`; `403`; `404`; `409`

#### `DELETE /api/promotion-codes/{id}`

Soft-delete a code.

- Auth: Clerk session token and `role = agent`
- Response `204`: empty body
- Errors: `401`; `403`; `404`

### Chatbot script

One row for the guest assistant. Booking and Cancel Booking login buttons stay in the client. Room cards still resolve names, photos and prices from the client room catalog; `roomIds` are those slugs, not `room_types.id`.

`ChatbotScriptResponse`:

| Field | Type | Notes |
| --- | --- | --- |
| `greeting` | string | first bot message |
| `autoReply` | string | used when typed text does not match a topic label |
| `topics` | array | suggestion topics, in display order |

Each topic is one of:

| `format` | Required fields |
| --- | --- |
| `message` | `id`, `label`, `text` |
| `room-type` | `id`, `label`, `title`, `actionLabel`, `roomIds` (may be empty) |
| `option-with-details` | `id`, `label`, `title`, `options[]` of `{ label, detail }` |

`enabled` is always `true` in the response.

#### `GET /api/chatbot`

- Auth: none (public)
- Response `200`: `ApiResponse<ChatbotScriptResponse>`
- Errors: `404` when the script row has not been seeded; `500` when stored topics are not valid JSON

#### `PUT /api/chatbot`

Replaces greeting, auto-reply and the whole topic list.

- Auth: Clerk session token and `role = agent`
- Body: `UpdateChatbotScriptRequest` with `greeting`, `autoReply` and a non-empty `topics` array. Topic ids must be unique. Each topic must include the fields for its `format`.
- Response `200`: `ApiResponse<ChatbotScriptResponse>` with `message: "Chatbot script updated"`
- Errors: `400` validation; `401`; `403`; `404` when the script row is missing

Run `014_chatbot_script.sql` then `014_chatbot_script_seed.sql` on Supabase before using the supabase profile. The local profile loads the seed automatically.

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

### Admin analytics

#### `GET /api/admin/analytics`

Returns the real booking, revenue, guest, payment and current room-availability data used by the admin dashboard.

- Auth: Clerk session token and database profile with role `agent`
- Query parameters:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `from` | date | yes | inclusive; must be on or before `to` |
| `to` | date | yes | inclusive; must not be after today in `Asia/Bangkok` |

The range may contain at most 366 days. A range of 31 days or fewer is grouped by day; longer ranges are grouped by month. Summary changes compare against the immediately preceding range with the same number of days. `changePercent` is `null` when the previous value is zero.

`totalBookings`, `totalSales`, trends, guest mix and payment methods include `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT` and `COMPLETED` bookings by `createdAt`. Room availability is a current snapshot: `occupied` is `CHECKED_IN`; `booked` is overlapping `CONFIRMED` plus unexpired `PENDING_PAYMENT`; `available` is current bookable units minus both.

- Response `200`: `ApiResponse<AnalyticsResponse>`

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "period": {
      "from": "2026-09-01",
      "to": "2026-09-24",
      "comparisonFrom": "2026-08-08",
      "comparisonTo": "2026-08-31",
      "granularity": "DAY"
    },
    "summary": {
      "totalBookings": { "value": 12, "previousValue": 10, "changePercent": 20.0 },
      "totalSales": { "value": 42000.00, "previousValue": 35000.00, "changePercent": 20.0 },
      "bookingUsers": { "value": 9, "previousValue": 8, "changePercent": 12.5 }
    },
    "roomAvailability": { "occupied": 2, "booked": 3, "available": 5, "totalBookable": 10 },
    "bookingTrend": [{ "label": "1 Sep", "value": 2 }],
    "revenueTrend": [{ "label": "1 Sep", "value": 7500.00 }],
    "guestMix": [
      { "key": "NEW", "count": 6, "percentage": 66.7 },
      { "key": "RETURNING", "count": 3, "percentage": 33.3 }
    ],
    "paymentMethods": [
      { "key": "STRIPE", "count": 7, "percentage": 58.3 },
      { "key": "CASH", "count": 5, "percentage": 41.7 }
    ]
  }
}
```

- Errors: `400` missing/malformed dates, future `to`, reversed range or range over 366 days; `401` missing/invalid Clerk token; `403` missing profile or non-agent role

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

Guest checkout for a signed-in Clerk user. Prices, extras, and promo discounts are calculated on the server from `room_types` and `promotion_codes`. Inventory is the room type's bookable `room_units` (not deleted, status not `OUT_OF_ORDER`/`OUT_OF_SERVICE`, same rule as `GET /api/rooms/available`) minus overlapping `booking_rooms` in `PENDING_PAYMENT`, `CONFIRMED`, or `CHECKED_IN` (expired Stripe drafts do not occupy a unit). Checkout inserts one `booking_rooms` row per requested room with `room_unit_id` null; a physical unit is assigned later. Money is THB `numeric(12,2)`; Stripe amounts are that value × 100.

Card payments use Stripe Checkout Sessions with `ui_mode: elements` (Payment Element). The session accepts cards only; PromptPay and other non-card methods are excluded. Do not send PAN/CVC to this API.

`CreateBookingRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `roomTypeId` | UUID | yes | existing non-deleted room type |
| `checkIn` | date | yes | ISO-8601 date |
| `checkOut` | date | yes | must be after `checkIn` (`stayValid`) |
| `guests` | integer | yes | 1–6, and ≤ room `capacity` × `roomsCount` |
| `roomsCount` | integer | no | 1–10; default `1`. That many units of the room type must be free for the whole stay (`409` otherwise); the room subtotal is nightly price × nights × `roomsCount` |
| `firstName` | string | yes | not blank; max 100 |
| `lastName` | string | yes | not blank; max 100 |
| `email` | string | yes | email; max 254 |
| `phoneNumber` | string | yes | not blank; max 32 |
| `country` | string | yes | not blank; max 100 |
| `dateOfBirth` | date | yes | in the past; guest must be at least 18 |
| `standardRequestCodes` | string[] | no | known preference codes (e.g. `early-check-in`) |
| `specialRequestCodes` | string[] | no | known paid extras (e.g. `baby-cot`). Each extra is its catalog price × nights (same night count as the room). |
| `additionalRequest` | string | no | max 2000 |
| `promotionCode` | string | no | max 40; ignored when unknown, inactive, deleted, below the code's minimum purchase, or not valid for the booked room type |
| `paymentMethod` | enum | yes | `STRIPE` or `CASH` |

`BookingResponse` (selected fields): `id`, `bookingNumber`, `roomTypeId`, `roomName`, `roomImageUrl`, `checkIn`, `checkOut`, `checkInTimeText`, `checkOutTimeText`, `guests`, `nights`, `roomsCount`, `status` (`PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `COMPLETED`, `CANCELLED`, `EXPIRED`), `paymentMethod`, guest fields, `standardRequests` (`{ code, label }[]`), `additionalRequest`, `promotionCode`, `currency` (`THB`), `items` (`kind` `ROOM` \| `ADDON` \| `DISCOUNT`), `roomSubtotal`, `extrasTotal`, `discountTotal`, `grandTotal`, `paymentMethodText`, `clientSecret` (Stripe Checkout client secret while a card payment is still pending; otherwise `null`), `holdExpiresAt`, `cancelledAt`, `createdAt`, `updatedAt`.

Seeded promo: `NEATLYNEW400` (fixed THB 400 off, no minimum, every room type) from `011_bookings_checkout.sql` / `local_promotion_seed.sql`. A percent code discounts the pre-discount total (room + extras). A fixed amount is subtracted the same way and the grand total is floored at zero. See Promotion codes.

#### `POST /api/bookings`

Create a booking for the signed-in user (`sub` claim).

- Auth: Clerk session token
- Body: `CreateBookingRequest`
- `CASH`: status `CONFIRMED` immediately, payment `UNPAID` (pay at hotel). No `clientSecret`.
- `STRIPE`: status `PENDING_PAYMENT`, 5-minute hold (`holdExpiresAt`), a Checkout Session, and `clientSecret` for `stripe.initCheckoutElementsSdk`. Any other open Stripe draft for this user is expired first.
- Response `201`: `ApiResponse<BookingResponse>` with `message: "Booking created"`
- Errors: `400` validation / unknown extra or preference / guest count exceeds capacity × rooms; `401`; `404` room not found; `409` no remaining units for the dates; `502` Stripe API failure; `503` `STRIPE_SECRET_KEY` missing (cash still works)

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

#### `PATCH /api/bookings/{id}/promotion`

Reprice an open card draft (`PENDING_PAYMENT`, `STRIPE`) and update that Checkout Session's amount. The client secret does not change, so the card form stays filled while the guest edits the code. A blank `promotionCode` removes the discount. Unknown or ineligible codes are ignored, same as checkout.

`UpdatePromotionCodeRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `promotionCode` | string | no | max 40; blank clears the code |

- Auth: Clerk session token
- Response `200`: `ApiResponse<BookingResponse>` with `message: "Promotion code updated"` and `clientSecret: null`
- Errors: `400` booking is not an open card draft, or it has no Checkout Session; `401`; `404`; `502` / `503` Stripe

#### `POST /api/bookings/{id}/cancel`

Cancel a `CONFIRMED` booking owned by the signed-in user, until check-in at 14:00 `Asia/Bangkok`. `cancelledAt` is set to now. Inventory is released by the existing `booking_rooms` trigger.

Refunds are decided on the server (the client does not send a refund flag):

- **Stripe** with a `SUCCEEDED` charge and check-in more than 24 hours away (14:00 `Asia/Bangkok`): create a Stripe refund of `grandTotal` back to that card, then insert a `payments` row (`kind` `REFUND`, `stripe_refund_id`)
- **Stripe** within 24 hours of check-in: cancel only; no Stripe refund
- **Cash**: cancel only; no money is moved. Cash stays unpaid until the guest pays at the hotel
- After check-in (14:00 `Asia/Bangkok` has passed) the booking cannot be cancelled
- Stripe refund failure returns `502` and the booking stays `CONFIRMED`
- After a successful cancel, the API sends a confirmation email to `guestEmail` through Brevo. The message names the refund amount only when a Stripe refund was created. A card cancellation inside the 24-hour window says the stay is not eligible for a refund. A cash cancellation says no payment was taken. A missing `BREVO_API_KEY` or `MAIL_FROM`, or a mail-provider failure, is logged and does not change the `200` response

- Auth: Clerk session token
- Body: none
- Response `200`: `ApiResponse<BookingResponse>` with `message: "Booking cancelled"`
- Errors: `400` status is not `CONFIRMED`, or check-in time has passed; `401`; `404`; `502` Stripe refund failed; `503` `STRIPE_SECRET_KEY` missing when a card refund is required

#### `PATCH /api/bookings/{id}/dates`

Change check-in and check-out of a `CONFIRMED` booking within 24 hours of `createdAt`, while check-in is still more than 24 hours away (14:00 `Asia/Bangkok`). Price is not recalculated. The new stay must keep exactly the original number of nights, so only the dates move. Availability reuses the same occupancy check as checkout, excluding this booking.

`ChangeBookingDatesRequest`:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `checkIn` | date | yes | ISO-8601 date |
| `checkOut` | date | yes | must be after `checkIn` (`stayValid`) |

- Auth: Clerk session token
- Body: `ChangeBookingDatesRequest`
- Response `200`: `ApiResponse<BookingResponse>` with `message: "Booking dates updated"`
- Errors: `400` outside the 24-hour booking window, check-in within 24 hours or already started, not `CONFIRMED`, or a different number of nights than the original; `401`; `404`; `409` no remaining units for the new dates

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

### 2026-09-24

- Added public `GET /api/chatbot` and agent-only `PUT /api/chatbot` for the guest assistant script (greeting, auto-reply and suggestion topics). Run `014_chatbot_script.sql` then `014_chatbot_script_seed.sql` on Supabase before using the supabase profile.

- Added agent-only `GET /api/admin/analytics?from&to` for the admin dashboard, including booking and revenue trends, summary comparisons, guest/payment breakdowns and current room availability.
- Public `GET /api/promotion-codes/preview?code&roomTypeId&purchase` tells checkout whether a code applies. `NOT_FOUND` covers unknown, inactive, and deleted codes. `ROOM_NOT_ELIGIBLE` means the code does not include that room type. `BELOW_MINIMUM` includes `minPurchaseAmount`.
- `PATCH /api/bookings/{id}/promotion` reprices an open card draft and updates the existing Checkout Session amount. The client secret stays the same.
- Added agent-only `GET/POST/PUT/DELETE /api/promotion-codes` for the admin Promo code page. Codes can be a fixed THB amount or a percent of the pre-discount total (room + extras), with a minimum purchase and an optional room-type limit (empty `roomTypeIds` means every room type). `DELETE` sets `deletedAt`. Checkout ignores a code that is deleted, below its minimum, or not valid for the booked room type. Run `012_promotion_code_rules.sql` on Supabase before using the supabase profile.
- Paid extras on `POST /api/bookings` (`specialRequestCodes`) are charged per night: line amount is the catalog price × nights. A 7-night baby cot is THB 2,800. One-night totals are unchanged.
- Stripe Checkout Sessions accept cards only. PromptPay and other non-card methods are excluded from the Payment Element.

### 2026-09-23

- `POST /api/bookings/{id}/cancel` sends a best-effort cancellation email to `guestEmail` through Brevo after the booking is saved. The refund amount is included only when a Stripe refund was created. Cash cancellations say no payment was taken. Mail is skipped when `BREVO_API_KEY` or `MAIL_FROM` is empty, and a Brevo failure does not roll back the cancellation.
- Card refunds stay on the original Stripe charge. Cash bookings are cancelled without a refund.

### 2026-09-21

- `POST /api/bookings/{id}/cancel` returns `400` once check-in at 14:00 `Asia/Bangkok` has passed (booking stays `CONFIRMED`).
- `PATCH /api/bookings/{id}/dates` is allowed only within 24 hours of `createdAt` **and** while check-in is still more than 24 hours away.
- `GET /api/rooms/available` takes an optional repeatable `roomTypeIds` param (max 20) to search only those room types. Omitted or empty is unchanged; unknown ids match nothing.

### 2026-09-18

- Added authenticated `POST /api/bookings/{id}/cancel` (full Stripe refund when check-in is more than 24 hours away; cash/unpaid bookings are cancelled only).
- Added authenticated `PATCH /api/bookings/{id}/dates` (within 24 hours of booking; new stay must keep the original number of nights; price unchanged).
- Added public `GET /api/rooms/available?checkIn&checkOut&rooms&guests` for the Search Result page. Counts bookable `room_units` minus overlapping active bookings per room type; rate limited like `GET /api/rooms`. Checkout (`POST /api/bookings`) now uses the same inventory: bookable `room_units` instead of `room_types.total_units` (behaviour change: a type's sellable count now follows its units).
- `POST /api/bookings`: `guests` may now be up to `capacity × roomsCount` (was `capacity`), so a 2-room booking for 4 guests of a 2-person room is accepted.
- `400 Validation failed` details for a query param of the wrong type (bound to a request object) now read `<field>: invalid value` instead of the Java conversion message (all endpoints).

### 2026-09-17

- Added `GET/POST/PUT/DELETE /api/room-units` and `GET /api/room-units/statuses` for Admin Room Management (physical rooms on `room_units` / `room_statuses`).
- Fixed leftover merge conflict markers in this file (hotel PUT errors + 2026-09-16 changelog).
- Room amenities are now stored in a shared `amenities` table. Request and response shapes are unchanged; `amenities` in `RoomRequest` is trimmed and case-insensitive duplicates are dropped (first spelling kept), and an existing amenity name is reused with its stored spelling.
- Added authenticated guest checkout: `POST /api/bookings`, `GET /api/bookings`, `GET /api/bookings/{id}`, `POST /api/bookings/{id}/payment-session`.
- Added public `POST /api/stripe/webhooks` (Checkout Session completed/expired). Card checkout uses Stripe Checkout `ui_mode: elements`; cash confirms immediately as unpaid pay-at-hotel.
- Bookings now share `dev`'s `booking_rooms` inventory model: checkout writes one unassigned room row per requested unit; Stripe/cash columns live in `011_bookings_checkout.sql`.

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
