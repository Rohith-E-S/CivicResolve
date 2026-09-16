import crypto from "node:crypto";
import jwt from "jsonwebtoken";

export const digest = (value) => crypto.createHash("sha256").update(value).digest("hex");
export const normalizeEmail = (value) => typeof value === "string" && value.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ? value.trim().toLowerCase() : null;
export const validProof = (value) => typeof value === "string" && /^[a-f0-9]{64}$/.test(value);
export const validPassword = (value, minimum = 8) => typeof value === "string" && [...value].length >= minimum && Buffer.byteLength(value, "utf8") <= 72;

// Validate at use time: dotenv is loaded after ESM imports in server.js.
export function signingSecret() {
  const secret = process.env.JWT_SECRET_KEY;
  if (typeof secret !== "string" || Buffer.byteLength(secret) < 32 || new Set(secret).size < 12 || /secret|password|changeme|example|replace|test-only/i.test(secret)) {
    throw new Error("Authentication signing configuration unavailable");
  }
  return secret;
}

export function signSession(user) {
  return jwt.sign({ _id: user._id.toString(), sessionVersion: user.sessionVersion ?? 0 }, signingSecret(), { algorithm: "HS256", expiresIn: "1d" });
}

export function verifySession(token) {
  const claims = jwt.verify(token, signingSecret(), { algorithms: ["HS256"] });
  if (!claims || typeof claims._id !== "string" || !/^[a-f\d]{24}$/i.test(claims._id) || !Number.isSafeInteger(claims.sessionVersion) || claims.sessionVersion < 0 || !Number.isFinite(claims.exp)) {
    throw new Error("Invalid session");
  }
  return claims;
}

export const cookieOptions = () => ({ httpOnly: true, sameSite: "lax", secure: process.env.NODE_ENV === "production", path: "/" });
export function setSessionCookie(res, token) {
  res.cookie("token", token, { ...cookieOptions(), maxAge: 24 * 60 * 60 * 1000 });
}

export function requestToken(req) {
  const authorization = req.headers?.authorization;
  return req.cookies?.token || (typeof authorization === "string" && authorization.startsWith("Bearer ") ? authorization.slice(7) : null);
}

export const sessionRoom = (id) => `auth-session:${id}`;
export function disconnectSessions(req, id) {
  req.app?.get("io")?.in(sessionRoom(id)).disconnectSockets(true);
}
