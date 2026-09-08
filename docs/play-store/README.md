# Play Store submission pack

Everything you need to submit **Hanging Spider: Word Hunt** to Google Play. Do these in order.

## What's in this folder

| File | For |
|---|---|
| [`listing.md`](listing.md) | Title, short & long description, category, tags, contact fields |
| [`questionnaires.md`](questionnaires.md) | Content rating + Data safety + Target audience answers |
| [`screenshots/`](screenshots/) | 5 phone screenshots ready to upload |
| `README.md` | This file |
| [`../../admin/privacy.html`](../../admin/privacy.html) | Privacy Policy — deploy via Firebase Hosting |
| [`../../admin/terms.html`](../../admin/terms.html) | Terms of Service — deploy via Firebase Hosting |
| [`../../app/build/outputs/bundle/release/app-release.aab`](../../app/build/outputs/bundle/release/app-release.aab) | Signed AAB you upload |

## Step 1 — Deploy privacy + terms to Firebase Hosting

Play requires a public HTTPS URL for the Privacy Policy. `admin/` already contains it.

```bash
# One-time
npm install -g firebase-tools
firebase login

# Deploy
firebase deploy --only hosting
```

After deploy your URLs will be:
- `https://hanging-spider.web.app/privacy.html`
- `https://hanging-spider.web.app/terms.html`
- `https://hanging-spider.web.app/` — the admin console (behind Google Sign-In + `/admins/{uid}` gate)

## Step 2 — Create a Play Console account

- Go to **https://play.google.com/console/signup**
- **$25 one-time** developer registration fee (US dollars, non-refundable)
- Verify identity (photo ID, address)
- Choose **Individual** unless you have a registered business

Takes 1–48 hours to activate.

## Step 3 — Create the app in Play Console

- Console → **Create app**
- App name: `Hanging Spider: Word Hunt`
- Default language: `English (India) – en-IN`
- App or game: **Game**
- Free or paid: **Free**
- Accept the two declarations (Developer Program Policies, US export laws)
- **Create app**

## Step 4 — Store listing (uses [`listing.md`](listing.md))

**Main store listing** → paste each field from `listing.md`:
- App name
- Short description
- Full description
- App icon → use `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` rendered at 512×512
  - Easiest way: **Android Studio → Image Asset Studio → Launcher Icons → Legacy → 512×512 PNG** (only if you don't already have one). If the current adaptive icon works, export it via any PNG converter at 512×512.
- Feature graphic → **1024×500** — not generated yet. Use any image tool, or say `feature graphic` and I'll create one.
- Phone screenshots → upload all 5 files from [`screenshots/`](screenshots/) in order

## Step 5 — App content (uses [`questionnaires.md`](questionnaires.md))

**Policy → App content** — answer every section using the sheet:
- Privacy policy → paste your deployed `https://hanging-spider.web.app/privacy.html` URL
- Access to app → **All functionality is available without special access**
- Ads → **Yes, contains ads**
- Content rating → walk through the IARC questionnaire with the answers in `questionnaires.md`
- Target audience → **13–15, 16–17, 18+** (NOT under 13)
- Data safety → walk through with the table in `questionnaires.md`
- Government apps → **No**
- Financial features → **None of the above**
- Health → **No**
- News → **No**
- COVID-19 → **No**

## Step 6 — Upload the AAB

**Release → Testing → Internal testing → Create new release**:
- Upload `app/build/outputs/bundle/release/app-release.aab`
- Release name: `0.1.0-internal-1` (or whatever your `versionName` is)
- Release notes: paste from `listing.md` → "What's new"
- **Save → Review release → Start rollout to internal testing**

Add yourself and a friend or two as testers via **Testers → Email list**. Once you accept the invite email, you can install from a Play Store link — this is the real production install path minus the wider audience.

## Step 7 — Bump to production (later)

Once internal testing looks good:
- **Release → Testing → Closed testing → Create new track** — invite 20 external testers, get 14 days of usage
- **Release → Production → Create new release** — the real launch

## Known gaps (fix before or after launch, your call)

- **In-app account deletion** — Play requires this since April 2024. Right now, users email us to delete. Google may push back on data safety review. To make bulletproof: add a "Delete my account" button on Home → confirmation dialog → deletes `/users/{uid}`, `/leaderboard/{uid}`, `/cashouts/{uid}`, `/messages/{uid}` and signs out. ~30 min of work — say `delete-account` and I'll add it.
- **Feature graphic (1024×500)** — not in this pack yet. Say `feature graphic` for a crimson hero-spider banner.
- **512×512 icon PNG** — the adaptive icon is bundled; the store listing wants a static PNG. Any converter works, or I can generate one from the vector.
- **Google Sign-In SHA-1 for the release keystore** — you added the DEBUG keystore SHA-1 to Firebase earlier. When Play generates a new upload key or uses App Signing, add THAT SHA-1 to Firebase too or Google Sign-In will fail for real installs. This is a common day-one bug.

## Timing

Realistic:
- Internal testing → up in **2 hours** after Play Console activates
- Closed testing → **1–3 days** review
- Production → **1–7 days** review (first release is slower, they scrutinise)

## Contact issues

If a review is rejected, the reason usually falls into:
- Data safety mismatches (declared X, code actually does Y)
- Privacy policy missing a specific data type
- Content rating too low for the actual imagery

Reply to the rejection email with the specific fix; usually turns around in 24–48 hours.
