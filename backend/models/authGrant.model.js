import mongoose from "mongoose";

const authGrantSchema = new mongoose.Schema({
  email: { type: String, required: true },
  purpose: { type: String, enum: ["signup", "reset"], required: true },
  tokenHash: { type: String, required: true, select: false },
  expiresAt: { type: Date, required: true },
});
authGrantSchema.index({ email: 1, purpose: 1 }, { unique: true });
authGrantSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });
export default mongoose.model("AuthGrant", authGrantSchema);
