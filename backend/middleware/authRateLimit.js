import AuthRateLimit from "../models/authRateLimit.model.js";
import { digest, normalizeEmail } from "../services/authSecurity.js";

// Database-backed fixed windows share limits across workers and restarts.
export function authRateLimit(scope, { ipLimit = 30, accountLimit = 10, windowMs = 15 * 60 * 1000 } = {}) {
  return async (req, res, next) => {
    try {
      const now = Date.now();
      const window = Math.floor(now / windowMs);
      const expiresAt = new Date((window + 1) * windowMs);
      const keys = [[`ip:${req.ip || req.socket?.remoteAddress || "unknown"}`, ipLimit]];
      const email = normalizeEmail(req.body?.email);
      if (email) keys.push([`account:${email}`, accountLimit]);
      // Reset proofs and Google credentials are not account identifiers before
      // validation, but bounding each opaque proof adds a second abuse bucket.
      const proof = req.body?.token || req.body?.credential;
      if (!email && typeof proof === "string") keys.push([`proof:${digest(proof)}`, accountLimit]);
      for (const [key, limit] of keys) {
        const _id = digest(`${scope}:${key}:${window}`);
        let record;
        try {
          record = await AuthRateLimit.findOneAndUpdate({ _id }, { $inc: { count: 1 }, $setOnInsert: { expiresAt } }, { upsert: true, new: true });
        } catch (error) {
          if (error.code !== 11000) throw error;
          record = await AuthRateLimit.findOneAndUpdate({ _id }, { $inc: { count: 1 } }, { new: true });
        }
        if (!record || record.count > limit) {
          res.set("Retry-After", String(Math.ceil((expiresAt.getTime() - now) / 1000)));
          return res.status(429).json({ success: false, message: "Too many attempts. Try again later." });
        }
      }
      next();
    } catch {
      return res.status(503).json({ success: false, message: "Authentication temporarily unavailable" });
    }
  };
}
