import crypto from "node:crypto";
import OTP from "../models/otp.model.js";
import AuthGrant from "../models/authGrant.model.js";
import { sendMail } from "../config/email.js";
import { digest } from "./authSecurity.js";

export class AuthInputError extends Error {
  constructor(message, status = 400) { super(message); this.status = status; }
}

export async function issueOtp(email, isForgotPassword) {
  const now = new Date();
  const otp = crypto.randomInt(100000, 1000000).toString();
  const otpHash = digest(otp);
  let record;
  try {
    // Unique email/purpose index prevents concurrent requests bypassing cooldown.
    record = await OTP.findOneAndUpdate({ email, isForgotPassword, $or: [{ issuedAt: { $lte: new Date(now - 60000) } }, { issuedAt: { $exists: false } }] }, {
      $set: { otpHash, issuedAt: now, expiresAt: new Date(+now + 5 * 60000), attempts: 0 },
    }, { upsert: true, new: true });
  } catch (error) {
    if (error.code === 11000) throw new AuthInputError("Wait 60 seconds before requesting another OTP", 429);
    throw error;
  }
  // Issuing a new OTP invalidates any outstanding proof for this purpose.
  await AuthGrant.deleteMany({ email, purpose: isForgotPassword ? "reset" : "signup" });
  try {
    await sendMail(email, isForgotPassword ? "Password Reset OTP" : "Email Verification OTP", `<p>Your verification code is <strong>${otp}</strong>. It expires in 5 minutes.</p>`);
  } catch {
    // Keep cooldown, but never allow an undelivered OTP to be used.
    await OTP.updateOne({ _id: record._id, otpHash }, { $set: { attempts: 5 } });
    throw new AuthInputError("Email delivery failed. Please try again later.", 503);
  }
}

export async function verifyOtpProof(email, otp, isForgotPassword) {
  const record = await OTP.findOneAndUpdate({ email, isForgotPassword, expiresAt: { $gt: new Date() }, attempts: { $lt: 5 } }, { $inc: { attempts: 1 } }, { new: true }).select("+otpHash");
  const hash = digest(otp);
  if (!record || !record.otpHash || !crypto.timingSafeEqual(Buffer.from(record.otpHash, "hex"), Buffer.from(hash, "hex"))) {
    throw new AuthInputError("OTP invalid, expired, or attempt limit reached");
  }
  const consumed = await OTP.findOneAndDelete({ _id: record._id, otpHash: hash, expiresAt: { $gt: new Date() }, attempts: { $lte: 5 } });
  if (!consumed) throw new AuthInputError("OTP invalid or already used");
  const token = crypto.randomBytes(32).toString("hex");
  await AuthGrant.findOneAndUpdate({ email, purpose: isForgotPassword ? "reset" : "signup" }, { $set: { tokenHash: digest(token), expiresAt: new Date(Date.now() + 10 * 60000) } }, { upsert: true });
  return token;
}
