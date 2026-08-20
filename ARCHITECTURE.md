# 🏛️ Architecture Deep-Dive

This document goes deeper than the README — it covers *why* the system is built this way, not just *what* it does. Written in lightweight ADR (Architecture Decision Record) format.

---

## Table of Contents
1. [Payment State Machine](#1-payment-state-machine)
2. [Idempotency Strategy](#2-idempotency-strategy)
3. [Webhook Delivery & Retry Design](#3-webhook-delivery--retry-design)
4. [Card Vault & Tokenization](#4-card-vault--tokenization)
5. [Settlement Engine](#5-settlement-engine)
6. [Multi-Tenant Security](#6-multi-tenant-security)
7. [Data Model](#7-data-model)

---

## 1. Payment State Machine

**Decision:** Payments transition through an explicit, validated finite state machine rather than a freely-writable `status` string.

**States:** `CREATED → AUTHORIZED → CAPTURED → SETTLED`, with `FAILED`, `CANCELLED`, `PARTIAL_REFUND`, `REFUNDED` as branch states.

**Why:** In a system with concurrent writers — a webhook from the acquirer, a poll from the merchant SDK, a manual dashboard action — nothing stops two writers from racing to update the same row. Without validation, a delayed "authorized" webhook arriving after a "captured" webhook could revert a payment backward, corrupting downstream settlement math.

**Implementation:** Transitions are checked against an explicit allow-list (`Map<Status, Set<Status>>`) before any write commits. Every transition — valid or attempted-invalid — is recorded in `PAYMENT_TRANSITION_LOG` with `from_status`, `to_status`, `actor`, `reason`, and `occurred_at`. This gives a full audit trail for support/dispute investigation, which real payment systems require.

**Trade-off:** More upfront modeling than a simple enum column, and every new payment method integration has to fit into the existing FSM rather than inventing new ad-hoc states.

---

## 2. Idempotency Strategy

**Decision:** All money-moving `POST` endpoints (order creation, payment creation) require an `X-Idempotency-Key` header.

**Why:** Network retries are the default assumption in payment systems, not an edge case — a client can time out on a response after the server already processed the charge, and will retry. Without idempotency, that retry becomes a duplicate charge.

**Implementation:** The idempotency key is stored alongside a hash of the request body. If the same key arrives with the same body, the original response is replayed. If the same key arrives with a **different** body, the request is rejected with `409 Conflict` rather than silently processed — this catches client bugs (e.g. accidental key reuse) instead of masking them.

---

## 3. Webhook Delivery & Retry Design

**Decision:** Webhook delivery is built as its own pipeline with signing, scheduled retry via Redis, and dead-lettering — not a synchronous `fetch()` call at the point of state change.

**Why:** Merchant endpoints are unreliable by nature (deploys, downtime, slow responses). A payment gateway that gives up after one failed webhook attempt is not a payment gateway merchants can build a business on.

**Flow:**
```
State change → Kafka event → consumer loads merchant config → HMAC-SHA256 sign payload
→ enqueue in Redis Sorted Set (score = next-attempt unix timestamp)
→ delivery service polls due deliveries every 5s → HTTP POST
→ 2XX: mark DELIVERED
→ non-2XX / timeout: attempt < 7 → reschedule with next backoff interval
                      attempt ≥ 7 → move to DLQ_EVENT
```

**Backoff schedule:** `1m, 5m, 30m, 2h, 8h, 24h` — front-loaded for transient blips (deploy in progress), long-tailed for extended outages, capped at 7 attempts to avoid indefinite retry storms.

**Why Redis Sorted Set over a cron job:** A cron sweep (e.g. "run every minute, check what's due") gives you coarse granularity and forces every retry interval to be a multiple of the sweep interval. A Sorted Set with `ZRANGEBYSCORE` gives O(log N) insertion and precise "what's due right now" queries — the delivery service just asks Redis for everything scored ≤ now, in order.

**DLQ + Replay:** After 7 failures, the event moves to `DLQ_EVENT` and stops consuming delivery-service resources. A `POST /v1/webhooks/:id/replay` endpoint lets the event be manually re-enqueued once the merchant confirms their endpoint is fixed — this mirrors how Kafka/SQS DLQ patterns work in production event systems.

---

## 4. Card Vault & Tokenization

**Decision:** Card capture happens via a direct browser → `vault_service` call, bypassing the merchant backend entirely. The merchant server only ever sees a token.

**Why:** This is the core idea behind PCI-DSS scope reduction in real payment gateways — if raw PAN never touches the merchant's servers, the merchant's compliance burden shrinks dramatically. Routing card data through the merchant backend "just to relay it" still puts PAN in that server's memory, logs, and network path.

**Implementation:**
- `vault_service` receives `(PAN, CVV, expiry)` directly from the browser.
- PAN is encrypted with AES-256; the encryption key itself (DEK) is encrypted and stored separately from the ciphertext (envelope encryption pattern).
- A random opaque token is generated and mapped to the encrypted PAN in `VAULT_CARD` / `CARD_TOKEN`.
- **No `GET /pan` endpoint exists anywhere in the system** — this is a deliberate design constraint, not an oversight. The only operation the vault exposes on saved cards is "charge with token."
- A `@MaskedCard` annotation + Logback filter strips any PAN-shaped digit sequences from log output, as a defense-in-depth measure against accidental logging.

---

## 5. Settlement Engine

**Decision:** Money is settled to merchants in a nightly batch job, not updated in real time as payments are captured.

**Why:** This mirrors real settlement rails (NEFT/IMPS batch windows, card network settlement cycles) — captured ≠ settled. Modeling this distinction, rather than crediting a merchant balance instantly on capture, is what makes the fee/GST/net-amount math auditable and matches how actual payment gateways report "T+1 settlement."

**Fee model:** 2% platform fee on captured amount, 18% GST on the fee (not on the full transaction — a common real-world gotcha).

**Implementation:** The nightly job aggregates all `CAPTURED` payments not yet included in a settlement, creates one `SETTLEMENT` record per merchant with `gross_amount`, `fee_amount`, `gst_amount`, `net_amount`, and links each contributing payment via `SETTLEMENT_PAYMENT` (a join table, so a single payment's contribution to a batch is independently traceable). A mock bank transfer step simulates real transfer failure so the settlement retry path is actually tested, not assumed.

**Reconciliation invariant enforced in tests:** `sum(SETTLEMENT_PAYMENT.net_amount) == SETTLEMENT.net_amount_paise`, to the paisa, for every batch.

---

## 6. Multi-Tenant Security

**Decision:** Two separate auth mechanisms for two separate trust boundaries — API key + HMAC for server-to-server, JWT for the dashboard.

**Why:** These have different threat models. Server-to-server calls (merchant backend → PayFlow API) need long-lived, rotatable credentials with a show-once secret. Dashboard sessions (a human logging in) need short-lived, revocable tokens. Conflating them tends to produce either overly long-lived dashboard sessions or awkward key-rotation UX for server integrations.

**API key design:** `key_id` + `key_secret_hash` (secret shown once at creation, never retrievable again — only the hash is stored). Includes `rotated_at` and `grace_period_expires_at` so a merchant can rotate keys without a hard cutover that breaks in-flight integrations.

**Rate limiting:** Implemented per-merchant, comparing token bucket, sliding window, and fixed window strategies under burst load — deliberately built more than one strategy to reason about their trade-offs under real traffic patterns rather than picking one from a tutorial.

---

## 7. Data Model

Full ER diagram: [`/docs/diagrams/er-diagram.png`](diagrams/er-diagram.png)

Key relationships:
- `MERCHANT` 1—* `ORDER_RECORD` 1—* `PAYMENT` 1—* `REFUND`
- `PAYMENT` 1—* `PAYMENT_TRANSITION_LOG` (full audit trail per payment)
- `MERCHANT` 1—* `VAULT_CARD` —tokenize→ `CARD_TOKEN`
- `MERCHANT` 1—* `WEBHOOK_EVENT` —dead-lettered→ `DLQ_EVENT`
- `MERCHANT` 1—* `SETTLEMENT` —includes→ `SETTLEMENT_PAYMENT` (*—* join to `PAYMENT`)

Money fields are stored as `long` in the smallest currency unit (paise), never as floating point — avoids the classic float-rounding-error-in-financial-systems bug class entirely.
