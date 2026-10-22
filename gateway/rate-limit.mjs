/**
 * The request quota of the gateway (ADR-0020).
 *
 * The quota is per client address, in memory, and is re-created on every
 * process start. That is deliberate: a replica that holds no session is a
 * replica that can be scaled horizontally, and the quota only has to blunt an
 * abusive client, not to bill it. An entitlement per device needs the device
 * attestation the client does not carry yet, and is recorded as pending work
 * rather than reimplemented here as a fiction.
 */

const DEFAULT_QUOTA_PER_MINUTE = 20;
const CAPACITY = 30;

/** The configured number of requests a single address may make in a minute. */
function quotaPerMinute() {
  const quota = Number(process.env.AI_GATEWAY_QUOTA_PER_MINUTE);
  return Number.isFinite(quota) && quota > 0 ? quota : DEFAULT_QUOTA_PER_MINUTE;
}

const buckets = new Map();

setInterval(() => {
  const stale = Date.now() - 10 * 60_000;
  for (const [key, bucket] of buckets) {
    if (bucket.at < stale) buckets.delete(key);
  }
}, 60_000).unref();

/** The quota of one client address. */
export function rateLimitedClient(address) {
  const key = address || "unknown";
  let bucket = buckets.get(key);
  if (!bucket) {
    bucket = {tokens: quotaPerMinute(), at: Date.now()};
    buckets.set(key, bucket);
  }
  refill(bucket);

  return {
    hasQuota: () => bucket.tokens >= 1,
    spend: () => {
      bucket.tokens -= 1;
    },
  };
}

/** Refills the bucket, bounded so a quiet client does not bank a burst. */
function refill(bucket) {
  const now = Date.now();
  const elapsed = now - bucket.at;
  bucket.at = now;
  if (elapsed <= 0) return;
  bucket.tokens = Math.min(CAPACITY, bucket.tokens + (elapsed / 60_000) * quotaPerMinute());
}
