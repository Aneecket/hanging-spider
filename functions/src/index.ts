/**
 * Cloud Functions for Hanging Spider.
 *
 * Public entrypoints:
 *   - coinbaseWebhook (HTTPS)   — Coinbase Commerce charge:confirmed → credits coins
 *
 * Admin callables (require caller's uid to appear in /admins):
 *   - adminAwardCoins  — grant coins to a user (used for leaderboard prizes, refunds, gifts)
 *   - adminSendMessage — write /messages entry + optional FCM push (topic or single token)
 *   - adminLogPrize    — record an off-app prize payout (Amazon voucher code, USDT tx hash, ...)
 *
 * All balance and prize writes go through here so the client can never spoof a purchase.
 */

import * as crypto from "crypto";
import {onRequest, onCall, HttpsError} from "firebase-functions/v2/https";
import {logger} from "firebase-functions";
import {defineSecret} from "firebase-functions/params";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.database();

// Secret used for HMAC verification of Coinbase Commerce webhooks.
// Set once with:  firebase functions:secrets:set COINBASE_WEBHOOK_SECRET
const COINBASE_WEBHOOK_SECRET = defineSecret("COINBASE_WEBHOOK_SECRET");

// USDT → coins pricing. Client reads /coinPacks so the UI stays in sync.
const COIN_PACKS: Record<string, {coins: number; usd: number}> = {
  starter: {coins: 500, usd: 1},
  bronze: {coins: 3_000, usd: 5},
  silver: {coins: 7_000, usd: 10},
  gold: {coins: 20_000, usd: 25},
  royal: {coins: 60_000, usd: 60},
};

// ---------- helpers ----------

async function requireAdmin(uid: string | undefined) {
  if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
  const snap = await db.ref(`admins/${uid}`).get();
  if (!snap.exists() || snap.val() !== true) {
    throw new HttpsError("permission-denied", "Admins only.");
  }
}

async function creditCoins(uid: string, coins: number, source: string, ref?: string) {
  const userRef = db.ref(`users/${uid}`);
  const result = await userRef.child("coins").transaction((current) => (current ?? 0) + coins);
  const newCoins = (result.snapshot.val() as number) ?? coins;

  // Mirror the leaderboard row (server-authoritative)
  const nameSnap = await userRef.child("displayName").get();
  const name = (nameSnap.val() as string | undefined)?.trim() || "Adventurer";
  await db.ref(`leaderboard/${uid}`).set({uid, name, coins: newCoins});

  await db.ref(`purchases/${uid}`).push({
    coins,
    source,
    externalRef: ref ?? null,
    at: admin.database.ServerValue.TIMESTAMP,
  });
  return newCoins;
}

// ---------- Coinbase Commerce webhook ----------

export const coinbaseWebhook = onRequest(
  {secrets: [COINBASE_WEBHOOK_SECRET], region: "asia-southeast1"},
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("POST only");
      return;
    }
    const sig = req.headers["x-cc-webhook-signature"];
    const raw: Buffer = (req as any).rawBody ?? Buffer.from(JSON.stringify(req.body));
    const expected = crypto
      .createHmac("sha256", COINBASE_WEBHOOK_SECRET.value())
      .update(raw)
      .digest("hex");
    if (typeof sig !== "string" || sig !== expected) {
      logger.warn("Bad Coinbase signature", {sig});
      res.status(401).send("bad signature");
      return;
    }

    const event = req.body?.event;
    const type = event?.type as string | undefined;
    if (type !== "charge:confirmed" && type !== "charge:resolved") {
      res.status(200).send("ignored");
      return;
    }

    const meta = event?.data?.metadata ?? {};
    const uid = meta.uid as string | undefined;
    const packId = meta.pack as string | undefined;
    const chargeId = event?.data?.id as string | undefined;
    if (!uid || !packId || !COIN_PACKS[packId]) {
      logger.error("Missing metadata", {uid, packId, chargeId});
      res.status(400).send("missing metadata");
      return;
    }

    // Idempotency: only credit each charge once.
    const claim = await db.ref(`purchases_claimed/${chargeId}`).transaction((v) => {
      if (v) return; // already claimed
      return {uid, packId, at: admin.database.ServerValue.TIMESTAMP};
    });
    if (!claim.committed || claim.snapshot.val()?.uid !== uid) {
      res.status(200).send("already credited");
      return;
    }

    const newCoins = await creditCoins(uid, COIN_PACKS[packId].coins, "coinbase", chargeId);
    logger.info("Credited coins", {uid, packId, coins: COIN_PACKS[packId].coins, newCoins});
    res.status(200).send("ok");
  }
);

// ---------- admin callables ----------

export const adminAwardCoins = onCall({region: "asia-southeast1"}, async (req) => {
  await requireAdmin(req.auth?.uid);
  const targetUid = req.data?.uid as string;
  const amount = Number(req.data?.amount);
  const reason = String(req.data?.reason ?? "admin award");
  if (!targetUid || !Number.isFinite(amount) || amount <= 0) {
    throw new HttpsError("invalid-argument", "uid and positive amount required");
  }
  const newCoins = await creditCoins(targetUid, amount, `admin:${reason}`);
  return {ok: true, newCoins};
});

export const adminSendMessage = onCall({region: "asia-southeast1"}, async (req) => {
  await requireAdmin(req.auth?.uid);
  const title = String(req.data?.title ?? "").slice(0, 120);
  const body = String(req.data?.body ?? "").slice(0, 2000);
  const push = Boolean(req.data?.push);
  const targetToken = req.data?.token as string | undefined;
  if (!title.trim()) throw new HttpsError("invalid-argument", "title required");
  const senderSnap = await db.ref(`admins/${req.auth!.uid}/name`).get();
  const sender = (senderSnap.val() as string | undefined) ?? "Admin";

  const ref = db.ref("messages").push();
  await ref.set({
    title, body, sender,
    sentAt: admin.database.ServerValue.TIMESTAMP,
  });

  if (push) {
    const message = {notification: {title, body}, data: {msgId: ref.key ?? ""}};
    if (targetToken) {
      await admin.messaging().send({...message, token: targetToken});
    } else {
      await admin.messaging().send({...message, topic: "all"});
    }
  }
  return {ok: true, id: ref.key};
});

export const adminLogPrize = onCall({region: "asia-southeast1"}, async (req) => {
  await requireAdmin(req.auth?.uid);
  const targetUid = req.data?.uid as string;
  const type = String(req.data?.type ?? "amazon"); // amazon | usdt | btc | custom
  const value = String(req.data?.value ?? "");    // voucher code, tx hash, note...
  const note = String(req.data?.note ?? "");
  if (!targetUid || !value) throw new HttpsError("invalid-argument", "uid and value required");
  const ref = db.ref(`prizes/${targetUid}`).push();
  await ref.set({
    type, value, note,
    grantedBy: req.auth!.uid,
    at: admin.database.ServerValue.TIMESTAMP,
  });
  return {ok: true, id: ref.key};
});
