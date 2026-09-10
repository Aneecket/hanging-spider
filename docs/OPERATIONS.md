# Operations playbook

Living notes for whoever is running Hanging Spider — future you, a new
teammate, or Claude on a different machine.

## Current release state

- **Version:** 0.3.3 (versionCode 9) — uploaded to Play Console closed
  testing 2026-09-10.
- **Version history:** 0.2.0 → 0.2.1 (R8 crash fix) → 0.2.2 (API 36
  target) → 0.3.0 (anonymous auth) → 0.3.2 (word bank + rates) → 0.3.3
  (force-update gate).
- **Target SDK:** 36 (Android 16). **Min SDK:** 24.
- **Signing:** local `hanging-spider.jks` upload key. Play App Signing
  handles the actual production APK signature.

## Force-update lever (new in v0.3.3)

The app reads `/config/minVersionCode` from Firebase Realtime Database
on every launch. If `BuildConfig.VERSION_CODE < minVersionCode`, a
non-dismissible dialog covers the app and opens Play Store when tapped.

**To trigger a forced release:**

1. Bump `versionCode` (and `versionName` for humans) in
   [app/build.gradle.kts](../app/build.gradle.kts).
2. `./gradlew :app:bundleRelease`.
3. Upload the AAB to Play Console → the relevant track → Create new
   release → Start rollout.
4. Wait 30–60 min for Play propagation so devices actually have the new
   version available.
5. Firebase Console → hanging-spider → Realtime Database → set
   `/config/minVersionCode` to the new versionCode. **Value type must be
   Number, not string.**

The moment you save step 5, every v0.3.3+ device on an older versionCode
sees the "UPDATE REQUIRED" dialog on next app open.

**To NOT force** (normal release): skip step 5 entirely. Leave
`/config/minVersionCode` unchanged and let Play Store auto-update handle
the rollout at its own pace.

**When to actually pull the lever:**

- Broken backend integration (RTDB schema change, removed endpoint)
- Security bug (exposed API key, auth bypass)
- Coin economy exploit (dupe glitch, infinite reward)
- Play policy compliance forcing a change

**Do not force for:**

- New words in the bank
- UI improvements
- New features
- Anything cosmetic

Every forced update annoys users. Save the lever for real emergencies.

### Known limitation — pre-v0.3.3 users can't be forced

The gate code first shipped in v0.3.3. APKs from v0.3.2 or older have no
gate code compiled in, so setting `/config/minVersionCode` reaches
nothing on those devices. They only move forward through Play Store's
normal auto-update (usually within 24–48 hours for opted-in testers).

Do not promise force-update reach for pre-v0.3.3 devices. It doesn't
work and never will retroactively.

### Rules and code

- Gate composable: [ForceUpdateGate.kt](../app/src/main/java/com/hangingspider/game/ui/nav/ForceUpdateGate.kt)
- Wired in: `MainActivity` calls `ForceUpdateGate { AppNav() }`.
- RTDB rules: `/config` is `.read: true` (unauthenticated read fires
  before anonymous sign-in completes) and `.write: false` (clients
  cannot bypass by writing a lower value). See
  [firebase.rules.json](../firebase.rules.json).
- `BuildConfig.VERSION_CODE`: requires `buildConfig = true` in
  `buildFeatures` (AGP 8.9+ default is off).
- Failure mode: any Firebase read failure defaults the required version
  to 0 so `versionCode < 0` is always false → gate never fires. A
  Firebase outage never bricks the app.

## Current gameplay tuning (as of v0.3.3)

Defined in [CoinViewModel.kt](../app/src/main/java/com/hangingspider/game/viewmodel/CoinViewModel.kt):

- **Idle coin accrual:** 1 coin per 10 seconds (`TICK_MS = 10_000L`).
- **Doubler duration:** 10 min after watching one rewarded ad.
- **Daily bonus:** 500 coins per 24 h.
- **Game win:** 200 coins.
- **Post-game interstitial:** every 3rd game
  (`GAMES_PER_INTERSTITIAL = 3`).
- **Idle interstitial:** after 3 min of zero interaction. Root-level
  `pointerInput` in [AppNav.kt](../app/src/main/java/com/hangingspider/game/ui/nav/AppNav.kt)
  resets the timer on any tap; route changes reset it too. Coin accrual
  is paused during the ad and resumes when the user dismisses it.

**Word bank:** 1454 curated entries with a 40-word recent-cooldown so
the same word never repeats within the last 40 draws. See
[WordBank.kt](../app/src/main/java/com/hangingspider/game/game/WordBank.kt).

## Auth (as of v0.3.3)

- **Default entry:** Firebase Anonymous auth — no login screen. First
  launch auto-creates a Firebase UID. Users get a `Weaver_XXXX` handle
  (first 4 chars of their UID).
- **Optional sync:** "Sync with Google" row in Settings. Links the
  anonymous UID to a Google credential on first device; on collision
  (same Google account already linked elsewhere), signs in as the
  existing linked UID and merges any pre-sync anonymous coins into the
  returned balance.
- **Firebase Console prerequisite:** Anonymous provider must be Enabled
  at Authentication → Sign-in method. Google provider stays Enabled for
  the optional Sync flow.
- **OAuth consent screen:** already "In production" in Google Cloud
  Console for the `hanging-spider` project. No test-user allowlist.
- Historical note: real-device Google Sign-In via
  `GetSignInWithGoogleOption` kept returning "account reauth failed" for
  weeks despite every SHA-1 (debug, upload, Play App Signing) being
  registered and OAuth being published. Root cause never fully isolated.
  Anonymous auth was the ship-workaround.

## Hosting / infra

- **Firebase Hosting** (Spark plan, free) serves everything in `admin/`
  at `https://hanging-spider.web.app`:
  - `/` — admin console UI (gated server-side by `/admins/{uid}`)
  - `/privacy.html` — Privacy Policy
  - `/terms.html` — Terms of Service
  - `/app-ads.txt` — AdMob verification for publisher
    `pub-5452237321152820`
- **GitHub Pages** at `aneecket.github.io/hanging-spider/` serves the
  same legal pages as a backup (source: `docs/`).
- **Firebase RTDB** at
  `hanging-spider-default-rtdb.asia-southeast1.firebasedatabase.app`.
- **Cloud Functions** scaffolded in `functions/` but dormant — enabling
  them requires upgrading to Blaze billing. Only needed when
  Coinbase-Commerce coin purchases go live.
- **App Check:** not yet enabled. Recommended before production for
  anti-piracy.

## Play Console setup

- **Website URL** field on the Play Store listing: **must** be exactly
  `https://hanging-spider.web.app` (root, no path). AdMob only crawls
  `app-ads.txt` from that root — anything else and verification fails.
- **Privacy Policy URL:** `https://hanging-spider.web.app/privacy.html`.
- **Advertising ID:** declared Yes. AdMob's
  `play-services-ads` library auto-injects the `AD_ID` permission.
- **Contact email:** `sandeepmt407@gmail.com`.
- **Feedback URL for testers:**
  `sandeepmt407+hangingspider@gmail.com` (Gmail alias for filtering).

### Path to production

Play requires (individual developer accounts):
1. **12 opted-in testers** on the Closed Testing track.
2. **14 continuous days** of testing with 12+ testers before the "Apply
   for access to production" button unlocks.
3. A short application form after that; Google review is usually 1–7
   days.

Google Group tester counter lags behind reality by hours to days.
Email lists propagate to Play Console in minutes. Use both if the
counter is stuck.

## Server-authoritative rules

| Path | Writer | Notes |
|---|---|---|
| `/users/{uid}/coins` (idle+daily+game) | client | client-authoritative today; move behind Functions before scale |
| `/users/{uid}/coins` (purchases) | Cloud Function only | **only** money → coins path |
| `/leaderboard/{uid}` | client (game) + Function (purchases) | denormalised for cheap listing |
| `/messages/{uid}` | admins only | client-writes blocked by rules |
| `/prizes/{uid}/{prizeId}` | admins only | off-app payout audit log |
| `/admins/{uid}` | none from client (`.write: false`) | Firebase Console only |
| `/config/minVersionCode` | none from client (`.write: false`) | Firebase Console only |

## Local dev

Requires JDK 17 at `/opt/homebrew/opt/openjdk@17` and Android SDK 34+
installed.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:installDebug
```

Firebase CLI (for `firebase deploy`):

```bash
npm install -g firebase-tools
firebase login
firebase use hanging-spider
```

Cloud Functions emulator when Functions become active:

```bash
cd functions && npm run serve
```

## Coin-pack sync gotcha

Coin packs are duplicated between server and client — keep them in sync
manually:

- `functions/src/index.ts` → `COIN_PACKS` (authoritative)
- `app/src/main/java/com/hangingspider/game/ui/screens/BuyCoinsScreen.kt`
  → `PACKS` (UI)

## Known landmine — R8 + Firebase KTX

`snap.getValue<UserProfile>()` uses `GenericTypeIndicator<T>`. R8
obfuscates the anonymous subclass and crashes on first read after
sign-in with:

```
Not a direct subclass of GenericTypeIndicator: class Y1.i
```

Fixed by ProGuard keeps for `GenericTypeIndicator` base + every
subclass. See [proguard-rules.pro](../app/proguard-rules.pro). Do not
remove these rules.
