import User from "../models/user.model.js";
import AuthGrant from "../models/authGrant.model.js";
import bcrypt from "bcrypt";
import cloudinary from "../config/cloudinary.js";
import fs from "fs";
import { issueOtp, verifyOtpProof, AuthInputError } from "../services/authOtp.js";
import { normalizeEmail, validPassword, validProof, digest, signingSecret, cookieOptions, setSessionCookie, disconnectSessions } from "../services/authSecurity.js";

const failAuth = (res, error) => res.status(error.status || 500).json({ success: false, message: error.status ? error.message : "Authentication request failed" });
const inputError = (message) => { throw new AuthInputError(message); };

const sendOtpFor = (isForgotPassword) => async (req, res) => {
  try {
    const email = normalizeEmail(req.body?.email);
    if (!email) inputError("Valid email required");
    const existing = await User.findOne({ email });
    if (isForgotPassword ? !existing : existing) inputError(isForgotPassword ? "Account does not exist" : "Account already exists");
    await issueOtp(email, isForgotPassword);
    return res.json({ success: true, message: "OTP sent successfully" });
  } catch (error) { return failAuth(res, error); }
};

const verifyOtpFor = (isForgotPassword) => async (req, res) => {
  try {
    const email = normalizeEmail(req.body?.email);
    const otp = req.body?.otp;
    if (!email || typeof otp !== "string" || !/^\d{6}$/.test(otp)) inputError("Valid email and six digit OTP required");
    const proof = await verifyOtpProof(email, otp, isForgotPassword);
    return res.json({ success: true, message: "OTP verified", [isForgotPassword ? "resetToken" : "signupToken"]: proof });
  } catch (error) { return failAuth(res, error); }
};

export const sendOtp = sendOtpFor(false);
export const verifyOtp = verifyOtpFor(false);
export const sendPasswordResetOtp = sendOtpFor(true);
export const verifyPasswordResetOtp = verifyOtpFor(true);

export const createAccount = async (req, res) => {
  try {
    const { fullName, password, address, signupToken } = req.body || {};
    const email = normalizeEmail(req.body?.email);
    if (!email || !validProof(signupToken)) inputError("Email and signupToken required");
    if (!validPassword(password)) inputError("Password must be at least 8 characters and at most 72 UTF-8 bytes");
    if (typeof fullName !== "string" || !fullName.trim() || fullName.length > 200 || (address !== undefined && (typeof address !== "string" || address.length > 1000))) inputError("Invalid profile details");
    signingSecret();
    if (await User.findOne({ email })) inputError("Account already exists");
    const hashedPassword = await bcrypt.hash(password, 10);
    // Only the caller who received this opaque proof can complete signup.
    const grant = await AuthGrant.findOneAndDelete({ email, purpose: "signup", tokenHash: digest(signupToken), expiresAt: { $gt: new Date() } });
    if (!grant) inputError("Signup verification invalid or expired");
    const user = await User.create({ fullName: fullName.trim(), email, password: hashedPassword, address, isVerified: true, isAdmin: false });
    const token = user.getJWT();
    setSessionCookie(res, token);
    return res.json({ success: true, token, user, message: "Account created successfully" });
  } catch (error) { return failAuth(res, error); }
};

export const login = async (req, res) => {
  try {
    const email = normalizeEmail(req.body?.email);
    const password = req.body?.password;
    if (!email || !validPassword(password, 1)) inputError("Invalid email or password format");
    const user = await User.findOne({ email }).select("+password +sessionVersion");
    if (!user || !await user.checkPassword(password)) throw new AuthInputError("Invalid credentials", 401);
    const token = user.getJWT();
    setSessionCookie(res, token);
    return res.status(201).json({ success: true, user, token, message: "Logged in successfully" });
  } catch (error) { return failAuth(res, error); }
};

export const logout = async (req, res) => {
  try {
    await User.updateOne({ _id: req.user._id }, { $inc: { sessionVersion: 1 } });
    disconnectSessions(req, req.user._id);
    res.clearCookie("token", cookieOptions());
    return res.json({ success: true, message: "Logged out successfully" });
  } catch (error) { return failAuth(res, error); }
};

export const resetPassword = async (req, res) => {
  try {
    const { password, token } = req.body || {};
    if (!validProof(token)) inputError("Token invalid or expired");
    if (!validPassword(password)) inputError("Password must be at least 8 characters and at most 72 UTF-8 bytes");
    const hashedPassword = await bcrypt.hash(password, 10);
    const grant = await AuthGrant.findOneAndDelete({ purpose: "reset", tokenHash: digest(token), expiresAt: { $gt: new Date() } });
    if (!grant) inputError("Token invalid or expired");
    const user = await User.findOneAndUpdate({ email: grant.email }, { $set: { password: hashedPassword }, $inc: { sessionVersion: 1 }, $unset: { resetPasswordToken: "", resetPasswordExpires: "" } }, { new: true });
    if (!user) inputError("Token invalid or expired");
    disconnectSessions(req, user._id);
    res.clearCookie("token", cookieOptions());
    return res.json({ success: true, message: "Password reset successfully" });
  } catch (error) { return failAuth(res, error); }
};

// -------------------------------------------------------------
// UPDATE PROFILE
// -------------------------------------------------------------
export const updateProfile = async (req, res) => {
  try {
    const { address, fullName } = req.body;
    const userID = req.user._id;

    const updateData = { address, fullName };

    if (req.file) {
      try {
        const upload = await cloudinary.uploader.upload(req.file.path);
        updateData.profilePic = upload.secure_url;
      } finally {
        // The temp file must not linger when the Cloudinary upload fails
        try {
          fs.unlinkSync(req.file.path);
        } catch {}
      }
    }

    const updatedUser = await User.findByIdAndUpdate(userID, updateData, {
      new: true,
    });

    return res.status(200).json({
      success: true,
      user: updatedUser,
      message: "Profile updated successfully",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Error updating profile: " + error.message,
    });
  }
};

// -------------------------------------------------------------
// CHECK AUTH
// -------------------------------------------------------------
export const checkAuth = (req, res) => {
  try {
    return res.status(200).json({ success: true, user: req.user.toObject() });
  } catch (error) {
    console.error("checkAuth Error:", error);
    return res.status(500).json({ success: false, message: error.message || "Internal Server Error" });
  }
};

// Google OAUTH

// Verifies a Google ID token via Google's tokeninfo endpoint (validates
// signature and expiry server-side). Returns the token claims or null.
async function verifyGoogleIdToken(credential) {
  try {
    if (!process.env.GOOGLE_CLIENT_ID || typeof credential !== "string" || !credential.trim()) return null;
    const response = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(credential)}`,
      { signal: AbortSignal.timeout(5000) }
    );
    if (!response.ok) return null;
    const info = await response.json();
    if (info.aud !== process.env.GOOGLE_CLIENT_ID ||
        !["accounts.google.com", "https://accounts.google.com"].includes(info.iss) ||
        !Number.isFinite(Number(info.exp)) || Number(info.exp) <= Date.now() / 1000 ||
        typeof info.email !== "string" || !info.email ||
        typeof info.sub !== "string" || !info.sub) {
      return null;
    }
    if (info.email_verified !== "true" && info.email_verified !== true) return null;
    return info;
  } catch {
    return null;
  }
}

export const googleLogin = async (req, res) => {
  try {
    // Identity fields supplied by clients are never authentication evidence.
    const claims = await verifyGoogleIdToken(req.body?.credential);
    if (!claims) {
      return res.status(401).json({ success: false, message: "Invalid Google credential" });
    }

    signingSecret();
    let user = await User.findOne({ email: claims.email }).select("+sessionVersion");
    if (!user) {
      user = await User.create({
        email: claims.email,
        fullName: claims.name || claims.email,
        googleId: claims.sub,
        profilePic: claims.picture || "",
        isGoogleUser: true,
        isVerified: true,
      });
    }

    const token = user.getJWT();
    setSessionCookie(res, token);

    return res.status(200).json({
      success: true,
      token,
      user: user.toObject(),
    });
  } catch (error) {
    // Provider errors must not expose credential or signing configuration.
    return res.status(500).json({
      success: false,
      message: "Google login failed",
    });
  }
};

// -------------------------------------------------------------
// UPDATE HOME DISTRICT
// -------------------------------------------------------------
export const updateHomeDistrict = async (req, res) => {
  try {
    const { district } = req.body;
    if (!district) {
      return res.status(400).json({ success: false, message: "District name is required" });
    }

    const user = await User.findById(req.user._id);
    if (!user) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    user.homeDistrict = district;
    await user.save();

    return res.status(200).json({
      success: true,
      message: "Home district updated successfully",
      user,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Error updating home district: " + error.message,
    });
  }
};

// -------------------------------------------------------------
// UPDATE USER LOCATION (called from Android periodically)
// -------------------------------------------------------------
export const updateUserLocation = async (req, res) => {
  try {
    const { latitude, longitude } = req.body;

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({ success: false, message: "latitude and longitude are required" });
    }

    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);

    if (isNaN(lat) || isNaN(lng)) {
      return res.status(400).json({ success: false, message: "Invalid coordinates" });
    }

    await User.findByIdAndUpdate(req.user._id, {
      lastLocation: { type: "Point", coordinates: [lng, lat] }, // GeoJSON: [lng, lat]
      lastLocationUpdatedAt: new Date(),
    });

    return res.status(200).json({ success: true, message: "Location updated" });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};
