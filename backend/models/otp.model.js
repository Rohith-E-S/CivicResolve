import mongoose from "mongoose";

const otpSchema = new mongoose.Schema({
  email: { type: String, required: true },
  otpHash: { type: String, required: true, select: false },
  attempts: { type: Number, default: 0 },
  issuedAt: { type: Date, required: true },
  expiresAt: {
    type: Date,
    required: true,
    default: () => new Date(Date.now() + 2 * 60 * 1000),
  },
  isForgotPassword: { type: Boolean, default: false },
}, { timestamps: true });

otpSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });
otpSchema.index({ email: 1, isForgotPassword: 1 }, { unique: true });

export default mongoose.model("OTP", otpSchema);
