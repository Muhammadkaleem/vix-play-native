# Splash / Onboarding / Permissions — PRD

**Routes:** `/splash`, `/onboarding`, `/permissions` · **Priority:** P0
**Entry:** App cold start.

## Purpose
Get the user from launch to a working library as fast as possible, requesting only the permissions actually needed, with a short value-focused onboarding on first run.

## Layout
- **Splash:** logo, brand accent background. No spinner unless init > 400ms.
- **Onboarding (first run only):** 3 swipeable slides — (1) plays everything, (2) your gestures your way, (3) beautiful themes. "Skip" always visible.
- **Permissions:** rationale card + single primary CTA per permission.

## Permissions requested
- Media/storage access (see engineering/permissions-storage.md — SAF vs `MANAGE_EXTERNAL_STORAGE` decision).
- Notifications (Android 13+) for playback controls.
- Requested contextually, with plain-language rationale before the system dialog.

## States
- **First run:** Splash → Onboarding → Permissions → Home.
- **Returning:** Splash → Home (skip onboarding).
- **Permission denied:** show a persistent "grant access" empty state in library, deep-link to app settings.
- **Partial media access (Android 14+):** show "manage selection" affordance.

## Interactions
- Swipe/skip onboarding. Tap CTA to trigger each permission.
- Never block the whole app on optional permissions (notifications).

## Edge cases
- User denies storage twice → Android stops showing dialog; route to app settings with instructions.
- Restricted/enterprise device without all-files access → fall back to SAF folder picker.

## Acceptance criteria
- [ ] Cold start to Home < 1.5s on mid-tier device (returning user).
- [ ] Onboarding shows only on first run; re-viewable from Settings.
- [ ] Every permission has a pre-dialog rationale.
- [ ] Denied-permission path is recoverable without reinstall.
