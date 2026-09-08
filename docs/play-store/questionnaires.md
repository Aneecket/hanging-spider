# Play Console questionnaires — answer sheet

Two forms in Play Console take the longest to fill: **Content rating** (IARC) and **Data safety**. Below is exactly what to pick.

---

## Content rating (IARC questionnaire)

Play Console → **Policy → App content → Content ratings → Start questionnaire**.

**Category to select:** Reference, News, or Educational — **NO**. Pick **All Other App Types → Game**.

| Question | Answer |
|---|---|
| Does the app contain any violent content, references, or depictions? | **No** (spider is dark but not violent) |
| Does the app contain any sexual content or nudity? | **No** |
| Does the app contain profanity or crude humour? | **No** |
| Does the app contain drug references or drug use? | **No** |
| Does the app contain simulated gambling? | **No** (coins have no cash value, no wagering mechanic) |
| Does the app allow users to interact with strangers online? | **No** (leaderboard shows names, no chat) |
| Does the app share user-generated content publicly? | **No** |
| Does the app share the user's location? | **No** |
| Does the app allow users to purchase digital goods? | **No** (no in-app purchases in v1 — Coinbase/USDT deferred) |
| Does the app allow users to purchase real goods or services with real money? | **No** |
| Does the app contain unrestricted internet access? | **No** |
| Does the app contain content that may frighten young children? | **Yes — mild** (menacing spider imagery, red glowing eyes) |

**Expected rating: IARC 12 / ESRB Everyone 10+ / PEGI 12 / Google 12+.**

If you'd rather target 3+ (broader audience), the only thing that pushes it above is the "frighten young children" answer. You can honestly answer **No** if you feel the abstract spider is milder than, say, Halloween emoji. Your call.

---

## Data safety

Play Console → **Policy → App content → Data safety → Start**.

### Section 1 — Data collection & security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** *(Firebase uses HTTPS)* |
| Do you provide a way for users to request that their data is deleted? | **Yes** *(via email — see Privacy Policy §6)* |

### Section 2 — Data types collected

Declare each of the following:

| Data type | Category | Collected | Shared | Optional? | Purposes |
|---|---|---|---|---|---|
| **Email address** | Personal info | Yes | No | No (login required) | Account management, App functionality |
| **Name** | Personal info | Yes | No | No | Account management, App functionality |
| **User IDs** | Personal info | Yes | No | No | Account management, App functionality, Analytics |
| **Photos** | Photos and videos | No | No | – | – |
| **App interactions** | App activity | Yes | No | No | Analytics, App functionality |
| **In-app search history** | App activity | No | – | – | – |
| **Device or other IDs** | Device or other IDs | Yes | Yes | No | Advertising or marketing |
| **Approximate location** | Location | No | – | – | – |
| **Precise location** | Location | No | – | – | – |
| **Purchase history** | Financial info | No | – | – | – |

Details for each "Yes":

- **Email address** — collected via Google Sign-In. Stored at `/users/{uid}/email` in Firebase Realtime Database. Also entered by the user at cash-out time as their gift-card delivery email. Not shared with third parties (Firebase is our data processor, not a third-party recipient per Play's definition).
- **Name** — display name from Google Sign-In. Shown in the public leaderboard.
- **User IDs** — Firebase-issued UID.
- **App interactions** — games played, games won, coins earned, cash-out requests. Used internally.
- **Device or other IDs** — the Android advertising ID, used by Google AdMob to serve personalised ads. This one is "shared" because AdMob receives it.
- **FCM token** — considered a "User ID" for the purpose of this form.

### Section 3 — Security practices

- **Data is encrypted in transit:** Yes.
- **You provide a way for users to request their data be deleted:** Yes.
- **Follows the Families Policy:** No (13+ audience).
- **Independent security review:** No.

---

## Target audience & content

Play Console → **Policy → App content → Target audience and content**.

- **Target age groups:** **13–15, 16–17, 18+**  *(NOT under 13 — we don't want to trigger Families Policy)*
- **Appeal to children:** **No, my app is not designed to appeal to children**

---

## Ads declaration

Play Console → **Policy → App content → Ads**.

- **Does your app contain ads?** **Yes** *(AdMob rewarded + interstitial)*
- **Ad ID declaration:** the app uses the advertising ID for ad delivery.

---

## Government apps

**No.**

---

## COVID-19 contact tracing / status

**No.**

---

## Financial features

Play Console has a section asking if the app deals with:
- Personal loans → **No**
- Cryptocurrency → **No** *(Coinbase code exists in repo but is dormant and NOT built into the AAB)*
- Debt collection → **No**
- Trading / investing → **No**

**None of the above.**

---

## Health apps

**No.**

---

## News apps

**No.**

---

## Store presence — Country availability

Recommended: launch in **India only** first (one language, one currency, gift cards are Amazon.in). Expand later. This also keeps you inside a single jurisdiction for gift-card compliance.
