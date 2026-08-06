# Pro & Play Billing

## Capability
Turn the existing ad seam into revenue: a one-time **VixPlay Pro** purchase that removes ads and unlocks Pro perks, verified locally through Google Play Billing. Complements `monetization.md` (the ad model); this doc covers the entitlement, the paywall, and the purchase lifecycle (`product/05-growth-foundation.md`, Workstream B).

## Model
- Product: single **non-consumable** in-app product `vixplay_pro` (lifetime unlock). Subscription tiers are out of scope until there is a recurring perk to justify them.
- Billing: Google Play Billing v6 (`BillingClient`), local verification only — no backend (the architecture decision; revisit at a subscription tier).
- `queryPurchases()` is the trust anchor: entitlement is recomputed from Play on every app start and on billing reconnection, never trusted from cache alone.

## Entitlement seam
- `EntitlementRepository` exposes `isPro: StateFlow<Boolean>`, cached in EncryptedSharedPreferences / DataStore for offline reads but always reconciled against `queryPurchases`.
- `AdsController.adsEnabled` is driven by `!isPro` — the single existing seam. Going Pro flips every ad placement off reactively (banner + per-list native/banner) with no UI hunt.
- Acknowledge purchases within the 3-day window (`acknowledgePurchase`) or Play auto-refunds.

## Paywall & upsell surfaces
- `ProScreen` — perk list, price (from `ProductDetails`, never hardcoded), Buy + **Restore purchases**, and a plain "you currently see ads" honesty line.
- Upsell entry points: Settings → Upgrade, an ad-adjacent "Remove ads" affordance, and Pro-gated perk taps (deep perks per roadmap: player skins, ambient backglow, subtitle online search, etc.). No interstitials, no dark patterns.
- Fire `paywall_view {source}` and `pro_purchase {price_micros, currency}` (`analytics.md`).

## Edge cases
- Billing unavailable (no Play Services / sideload) — `ProScreen` shows an explanatory state; the app stays fully functional in free (ad) mode. Never a dead Buy button.
- Purchase pending (`PENDING` state) — keep ads on until it resolves to `PURCHASED`; don't grant early.
- Refund / revoke — `queryPurchases` drops the entitlement on next reconcile and ads return.
- Restore on a new device — Buy button becomes "Restore" once Play reports the owned product.
- Play policy lint — no misleading "free" claims; the ad disclosure and price are surfaced before purchase.

## Acceptance criteria
- [ ] `vixplay_pro` purchased in a test track flips `isPro` true and removes every ad placement without restart.
- [ ] Entitlement survives app restart and offline, and is revoked after a refund on next reconcile.
- [ ] Restore purchases re-grants Pro on a fresh install.
- [ ] Price and perks come from `ProductDetails`; nothing hardcoded.
- [ ] Purchases are acknowledged within the window; pending purchases don't grant early.
