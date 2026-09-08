# Hanging Spider

Hangman-with-spider Android game. Kotlin + Jetpack Compose. Firebase Auth + Realtime DB backend. AdMob monetization. USDT coin purchases via Coinbase Commerce webhook. Admin console for messaging and prize fulfillment.

## Status

- **Phase 1 — done.** Google Sign-In, RTDB profile sync, coin economy, hangman gameplay.
- **Phase 2 — done.** AdMob rewarded ad (2× doubler) and interstitial (post-game). Leaderboard with seeded `AI BOT` rows. Admin messages inbox. FCM push service. Bottom navigation. Full theme system (Cinzel + Manrope, three per-screen gradients).
- **Phase 3 — this commit.** Server-authoritative Cloud Functions (Coinbase webhook + admin callables). Admin web console at `admin/`. Buy-Coins tab in-app. Prize log / purchase log.

## Repo layout

```
app/                Android app (Kotlin + Compose)
functions/          Cloud Functions (TypeScript) — webhook + admin callables
admin/              Admin web console (single-file HTML) — Firebase Hosting target
firebase.rules.json Realtime Database security rules
firebase.json       Firebase project config
```

## Legal / policy — read before pushing to Play

- **Google Play forbids in-app payouts in crypto.** Prizes (Amazon vouchers, USDT/BTC transfers) MUST be sent from the **admin web console**, NOT from inside the APK.
- **AdMob** forbids paying users just to keep the screen on. The idle-accrual ticker is fine as long as it's not framed as a reward-for-viewing.
- **Fake leaderboard entries** are marked `AI BOT` in the UI for consumer-protection compliance.

## One-time setup

1. **Firebase project** — you already have `hanging-spider` in `asia-southeast1`. Realtime Database, Auth (Google enabled), FCM available.
2. **Blaze billing** — required for Cloud Functions. Enable in the Firebase console.
3. **Coinbase Commerce account** — sign up, generate a webhook shared secret. Save it as a secret:
   ```bash
   cd functions
   firebase functions:secrets:set COINBASE_WEBHOOK_SECRET
   ```
4. **AdMob** — swap the test app ID (`ca-app-pub-3940256099942544~3347511713`) and test unit IDs (`AdManager.kt`, `AndroidManifest.xml`) for your production ones once you have an AdMob account.
5. **First admin** — pick a Google account, sign in once to the Android app, look up its UID in Firebase Auth, and in the RTDB console create `/admins/<uid> = true`. That account can then log into the admin web console.

## Deploy

```bash
# Rules
firebase deploy --only database

# Cloud Functions
cd functions && npm install && npm run build && cd ..
firebase deploy --only functions

# Admin console (Firebase Hosting)
firebase deploy --only hosting

# Android
./gradlew :app:assembleRelease
```

## Purchase flow

1. User taps a coin pack in the Buy-Coins tab.
2. App opens a Coinbase Commerce hosted checkout URL for that pack with `metadata: { uid, pack }`.
3. User pays in USDT (or any accepted asset).
4. Coinbase Commerce fires `charge:confirmed` → `coinbaseWebhook` Cloud Function.
5. Webhook verifies HMAC signature, idempotently credits coins to `/users/{uid}/coins` and mirrors to `/leaderboard/{uid}`.
6. Client sees the coin balance update instantly via the RTDB listener.

The client **never** writes purchase-derived coins directly. This is the only supported path for money-backed coins.

## Prize flow (Amazon voucher / USDT / BTC)

1. Admin opens the web console (`admin/index.html` → `/`), signs in with Google.
2. Console pulls live leaderboard from `/leaderboard`.
3. Admin picks a winner, buys the Amazon voucher / sends USDT off-app.
4. Admin logs the payout via **Log prize payout** — writes to `/prizes/{uid}` (voucher code or tx hash).
5. Admin optionally sends the winner a message + push via **Send whisper**.

`/prizes/{uid}` is your audit trail. Users can read their own `/prizes/{uid}` entries; admins can read everything.

## Server-authoritative data

| Field | Written by | Notes |
|---|---|---|
| `/users/{uid}/coins` (idle+daily+game) | client | Currently client-authoritative for game/idle rewards. Move behind Cloud Functions for anti-cheat before scale. |
| `/users/{uid}/coins` (purchases) | Cloud Function | Only path from real money → coins. |
| `/leaderboard/{uid}` | client (game) + Cloud Function (purchases) | Denormalised. |
| `/messages/{msgId}` | admins only (rules) | Admin callable writes; RTDB rules block direct client writes. |
| `/prizes/{uid}/{prizeId}` | admins only | Off-app payout audit log. |

## Local dev

Requires JDK 17 (`/opt/homebrew/opt/openjdk@17`), Android SDK 34.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:installDebug
```

Firebase Emulator Suite for functions:

```bash
cd functions && npm run serve
```

## Ad units and pricing

Change the coin packs in **two places** (kept manually in sync, or read `/coinPacks` in-app later):

- `functions/src/index.ts` — `COIN_PACKS` map (authoritative).
- `app/src/main/java/com/hangingspider/game/ui/screens/BuyCoinsScreen.kt` — `PACKS` list (UI).

## Remaining work

- Move client-authoritative coin writes (idle, daily, game reward) behind Cloud Functions to prevent RTDB tampering.
- Real Coinbase Commerce charge creation (server-side, returns hosted URL) instead of the placeholder link in `BuyCoinsScreen.launchCheckout`.
- Bot list decay: retire bots as real users overtake them (currently they linger forever).
- Notification permission prompt on API 33+.
