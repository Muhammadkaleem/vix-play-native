# Monetization

## Model
Free tier with calm ads + one-time **Pro** unlock (Google Play Billing v6). Optional cloud/sync subscription (P2).

## Free vs Pro
| Capability | Free | Pro |
|---|---|---|
| Core playback, subtitles, gestures | Yes | Yes |
| Ads | Occasional, non-intrusive | Removed |
| Player skins, ambient backglow | Limited | All |
| Advanced gestures presets export | — | Yes |
| Cloud/sync (P2) | — | Subscription |

## Ad principles
- Never interrupt playback. No ads on the player surface. Banner only in library chrome / between-session interstitial with frequency capping. Respect user attention — a calm free tier is itself a differentiator vs MX.

## Billing
- Query products, purchase, acknowledge, restore. Verify entitlement server-side if a backend exists; otherwise local cache with Play verification.

## Edge cases
- Purchase pending/deferred; refunds/revocation → downgrade gracefully.
- Restore across devices/reinstall.
- Region pricing.

## Acceptance criteria
- [ ] Pro purchase removes ads + unlocks features; restore works on reinstall.
- [ ] No ad ever appears on the player surface.
