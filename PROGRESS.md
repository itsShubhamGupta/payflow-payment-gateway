# 📅 Build Log — PayFlow

A week-by-week record of how this system was designed and built. Kept public and updated as I go, rather than reconstructed after the fact.

> Legend: ✅ Done · 🔧 In progress · 🧪 Testing/hardening · 💡 Design decision

---

## Week 1 — Foundations & Merchant Onboarding
**Goal:** Get a merchant able to sign up, log in, and get API keys.

- ✅ Project scaffolding: Spring Boot + PostgreSQL + Docker Compose
- ✅ `MERCHANT` entity with KYC fields (business name, GST ID, PAN, settlement bank details)
- ✅ Signup + login (bcrypt password hashing)
- ✅ `API_KEY` generation — key shown once on creation, hash stored, never retrievable again
- ✅ `APP_USER` for dashboard-side team members per merchant
- 💡 Decision: separate `API_KEY` (server-to-server) from `APP_USER`/JWT (dashboard) auth from day one — mixing them gets painful fast once rate limiting comes in

**Shipped:** `POST /merchants`, `POST /auth/login`, `POST /api-keys`

---

## Week 2 — Orders
**Goal:** An order is a reservation of intent to pay — before any money moves.

- ✅ `ORDER_RECORD` entity: amount, currency, status, idempotency key
- ✅ `POST /v1/orders` with `X-Idempotency-Key` header enforcement
- ✅ 15-minute auto-expiry via scheduled job
- ✅ Pagination + filtering on order list endpoint
- 🧪 Wrote first integration tests using Testcontainers (real Postgres, not H2)
- 💡 Decision: idempotency keys stored with a hash of the request body — same key + different payload = `409`, not a silent overwrite

**Shipped:** `POST /v1/orders`, `GET /v1/orders`, `GET /v1/orders/:id`

---

## Week 3 — Payment Lifecycle & State Machine
**Goal:** The core of the system — payments must move through valid states only.

- ✅ Designed the 8-state payment FSM: `CREATED → AUTHORIZED → CAPTURED → SETTLED`, with `FAILED`, `CANCELLED`, `PARTIAL_REFUND`, `REFUNDED` branches
- ✅ `PAYMENT_TRANSITION_LOG` — every transition recorded with actor, reason, timestamp (audit trail, not just a status column)
- ✅ Mock acquirer with chaos modes: `SUCCESS`, `FAILURE`, `TIMEOUT`, `SLOW`
- ✅ Support for Card, UPI, Net Banking, Wallet method payloads
- 🧪 Tested invalid transitions are rejected (e.g. `SETTLED → AUTHORIZED` throws, doesn't silently update)
- 💡 Decision: transitions validated against an explicit allow-list map, not scattered `if` checks — makes the FSM auditable in one place

**Shipped:** `POST /v1/payments`, `GET /v1/payments/:id`, transition validation layer

---

## Week 4 — Refunds
**Goal:** Full and partial refunds without corrupting settlement math later.

- ✅ `REFUND` entity with its own sub-state machine (`PENDING → PROCESSING → PROCESSED/FAILED`)
- ✅ Partial refund support with running `refunded_amount_paise` tracked on `PAYMENT`
- ✅ Async refund processing via scheduled job (mirrors real bank ACH-style delay)
- 🧪 Edge case tests: refund > captured amount rejected, double-refund rejected, refund on non-captured payment rejected

**Shipped:** `POST /v1/payments/:id/refund`, `GET /v1/refunds/:id`

---

## Week 5 — Webhook Delivery Engine
**Goal:** This is where most "clone" projects stop at a `fetch()` call. I wanted real delivery guarantees.

- ✅ `MERCHANT_WEBHOOK_CONFIG` — per-merchant target URL + event type filter
- ✅ HMAC-SHA256 payload signing (`X-Payflow-Signature` header)
- ✅ Kafka producer/consumer: payment state changes publish events, webhook consumer picks them up
- ✅ Redis Sorted Set for scheduled delivery — score = next attempt timestamp
- ✅ Exponential backoff schedule: `1m → 5m → 30m → 2h → 8h → 24h`
- ✅ Dead Letter Queue after 7 failed attempts (`DLQ_EVENT`)
- ✅ Replay API to manually redeliver dead-lettered events
- 🧪 Simulated a flaky merchant endpoint (random 500s) to verify backoff timing and eventual DLQ move
- 💡 Decision: Redis Sorted Set over a cron sweep — gives precise "due now" queries via `ZRANGEBYSCORE` instead of coarse minute-granularity polling

**Shipped:** Webhook delivery service, `POST /v1/webhooks/:id/replay`

---

## Week 6 — Card Vault & Tokenization
**Goal:** Never let raw card numbers touch the merchant server or application logs.

- ✅ `vault_service` — separate module, browser calls it directly (not proxied through merchant backend)
- ✅ AES-256 encryption of PAN, encrypted DEK stored alongside
- ✅ Token generation + `CARD_TOKEN` mapping (`VAULT_CARD` never exposes PAN via any API)
- ✅ Charge-with-token flow for saved/one-click checkout
- ✅ `@MaskedCard` annotation + Logback filter to strip PAN-like patterns from logs
- 🧪 Verified no PAN appears in any log output under load, including error stack traces

**Shipped:** `POST /v1/vault/cards`, `POST /v1/payments` (token-based charge)

---

## Week 7 — Settlement Engine
**Goal:** Money doesn't move in real time — it settles in batches, like real payment rails.

- ✅ Nightly batch job (11pm scheduled, configurable)
- ✅ Fee calculation: 2% of captures + 18% GST on fee
- ✅ Per-merchant net settlement with `SETTLEMENT` + `SETTLEMENT_PAYMENT` join table
- ✅ Full audit trail: gross amount, fee, GST, net amount, per-payment breakdown
- ✅ Mock bank transfer step with simulated failure/chaos for testing settlement retry logic
- 🧪 Reconciliation test: sum of `SETTLEMENT_PAYMENT` net amounts must equal `SETTLEMENT.net_amount_paise` exactly, to the paisa

**Shipped:** Settlement scheduler, `GET /v1/settlements`, `GET /v1/settlements/:id`

---

## Week 8 — Multi-Tenant Security & Rate Limiting 🔧
**Goal:** Lock down the platform for real multi-merchant usage.

- ✅ Per-merchant rate limiting (token bucket implementation)
- 🔧 Sliding window + fixed window strategies (comparing behavior under burst load)
- 🔧 JWT refresh token flow for dashboard sessions
- 🧪 Load testing rate limiter under concurrent requests from same merchant

---

## Week 9 — Analytics Dashboard 🔧
**Goal:** Give merchants visibility without querying the DB directly.

- 🔧 Real-time revenue dashboard (today's captures, success rate)
- 🔧 Per-merchant analytics filter
- ⬜ 30-day historical reports
- ⬜ Export to CSV

---

## Backlog / Next Up
- ⬜ Load testing report (k6 or Gatling) — publish latency percentiles under load
- ⬜ Chaos testing the settlement job for partial-batch failure recovery
- ⬜ OpenAPI spec generation + published Swagger UI
- ⬜ CI pipeline (GitHub Actions): test + build on every PR
- ⬜ Terraform for a cloud deployment (currently local Docker Compose only)

---

<sub>Updated weekly. Commit history is the source of truth — this log is the human-readable summary.</sub>
