import User from "../models/user.model.js";
import OTP from "../models/otp.model.js";
import bcrypt from "bcrypt";
import cloudinary from "../config/cloudinary.js";
import fs from "fs";
import { sendMail } from "../config/email.js";
import jwt from "jsonwebtoken";
import crypto from "crypto";

// -------------------------------------------------------------
// 1. SEND OTP (STEP 1)
// -------------------------------------------------------------
export const sendOtp = async (req, res) => {
  try {
    const { email } = req.body;

    if (!email)
      return res
        .status(400)
        .json({ success: false, message: "Email required" });

    const existing = await User.findOne({ email });
    if (existing)
      return res
        .status(400)
        .json({ success: false, message: "Account already exists" });

    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    await OTP.create({
      email,
      otp,
      expiresAt: Date.now() + 5 * 60 * 1000,
    });

    await sendMail(
      email,
      "Your Verification OTP 🔐",
      `
  <div style="font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; color: #333;">
    <h2 style="color: #2b6cb0;">Email Verification</h2>

    <p>Hi,</p>

    <p>Your One-Time Password (OTP) for email verification is:</p>

    <div style="
      font-size: 24px;
      font-weight: bold;
      background: #f3f4f6;
      padding: 10px 20px;
      border-left: 4px solid #2b6cb0;
      display: inline-block;
      margin: 10px 0;
      border-radius: 5px;
    ">
      ${otp}
    </div>

    <p>This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>

    <p style="margin-top: 20px;">Regards,<br><strong>Complaint Management Team</strong></p>
  </div>
  `
    );

    return res.json({ success: true, message: "OTP sent successfully" });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// -------------------------------------------------------------
// 2. VERIFY OTP (STEP 2)
// -------------------------------------------------------------
export const verifyOtp = async (req, res) => {
  try {
    const { email, otp } = req.body;

    const record = await OTP.findOne({ email });

    if (!record)
      return res.status(400).json({ success: false, message: "OTP expired" });

    if (record.expiresAt < Date.now())
      return res.status(400).json({ success: false, message: "OTP expired" });

    if (record.otp !== otp)
      return res.status(400).json({ success: false, message: "Wrong OTP" });

    return res.json({ success: true, message: "OTP verified" });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// -------------------------------------------------------------
// 3. CREATE ACCOUNT AFTER OTP VERIFIED (STEP 3)
// -------------------------------------------------------------
export const createAccount = async (req, res) => {
  try {
    const { fullName, email, password, address } = req.body;

    const exists = await User.findOne({ email });
    if (exists)
      return res
        .status(400)
        .json({ success: false, message: "Account already exists" });

    const hashedPassword = await bcrypt.hash(password, 10);

    const newUser = await User.create({
      fullName,
      email,
      password: hashedPassword,
      address,
      isVerified: true,
      isAdmin: false,
    });

    const token = await newUser.getJWT();

    res.cookie("token", token, {
      expires: new Date(Date.now() + 24 * 60 * 60 * 1000),
    });

    return res.json({
      success: true,
      token,
      user: newUser,
      message: "Account created successfully",
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: error.message });
  }
};

// -------------------------------------------------------------
// LOGIN
// -------------------------------------------------------------
export const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    const userData = await User.findOne({ email });

    if (!userData)
      return res.status(404).json({ success: false, message: "No User Found" });

    const isPasswordValid = await userData.checkPassword(password);

    if (!isPasswordValid)
      return res
        .status(404)
        .json({ success: false, message: "Invalid credentials" });

    const token = await userData.getJWT();

    res.cookie("token", token, {
      expires: new Date(Date.now() + 24 * 60 * 60 * 1000),
    });

    res.status(201).json({
      success: true,
      user: userData,
      token,
      message: "Logged in successfully",
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: `Error in login API: ${error.message}`,
    });
  }
};

// -------------------------------------------------------------
// LOGOUT
// -------------------------------------------------------------
export const logout = async (req, res) => {
  try {
    res.cookie("token", null, { expires: new Date(Date.now()) });
    return res.json({ success: true, message: "Logged out successfully" });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error in logout API: ${error.message}`,
    });
  }
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
      const upload = await cloudinary.uploader.upload(req.file.path);
      updateData.profilePic = upload.secure_url;
      fs.unlinkSync(req.file.path);
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

export const googleLogin = async (req, res) => {
  try {
    const { email, fullName, profilePic, googleId } = req.body;

    if (!email || !googleId) {
      return res.status(400).json({
        success: false,
        message: "Invalid Google login data",
      });
    }

    // Check if user exists
    let user = await User.findOne({ email });

    // Create new user if doesn't exist
    if (!user) {
      user = await User.create({
        email,
        fullName,
        googleId,
        profilePic,
        isGoogleUser: true,
        isVerified: true, // Google verifies email already
      });
    }

    // Create a JWT token
    const token = jwt.sign({ _id: user._id.toString() }, process.env.JWT_SECRET_KEY, {
      expiresIn: "7d",
    });

    res.cookie("token", token, {
      expires: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    });

    return res.status(200).json({
      success: true,
      token,
      user: user.toObject(),
    });
  } catch (error) {
    console.error("Google Login Error:", error);
    return res.status(500).json({
      success: false,
      message: "Google login failed",
    });
  }
};

// SendpasswordResetOtp
export const sendPasswordResetOtp = async (req, res) => {
  try {
    const { email } = req.body;

    if (!email)
      return res
        .status(400)
        .json({ success: false, message: "Email required" });

    const existing = await User.findOne({ email });
    if (!existing)
      return res
        .status(400)
        .json({ success: false, message: "Account does not exists" });

    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    await OTP.create({
      email,
      otp,
      expiresAt: Date.now() + 5 * 60 * 1000,
      isForgotPassword: true,
    });

    await sendMail(
      email,
      "Password Reset OTP 🔐",
      `
  <div style="font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; color: #333;">
    <h2 style="color: #d9534f;">Reset Your Password</h2>

    <p>Hi,</p>

    <p>You requested to reset your password. Use the OTP below to proceed:</p>

    <div style="
      font-size: 24px;
      font-weight: bold;
      background: #f3f4f6;
      padding: 10px 20px;
      border-left: 4px solid #d9534f;
      display: inline-block;
      margin: 10px 0;
      border-radius: 5px;
    ">
      ${otp}
    </div>

    <p>This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>

    <p>If you did not request a password reset, please ignore this email.</p>

    <p style="margin-top: 20px;">Regards,<br><strong>Complaint Management Team</strong></p>
  </div>
  `
    );

    return res.json({ success: true, message: "OTP sent successfully" });
  } catch (error) {
    console.log(error.message);
    return res.status(404).json({ success: false, message: error.messsage });
  }
};

// verifyPasswordResetOtp
export const verifyPasswordResetOtp = async (req, res) => {
  try {
    const { email, otp } = req.body;

    const record = await OTP.findOne({ email, isForgotPassword: true });

    if (!record)
      return res.status(400).json({ success: false, message: "OTP expired" });

    if (record.expiresAt < Date.now())
      return res.status(400).json({ success: false, message: "OTP expired" });

    if (record.otp !== otp)
      return res.status(400).json({ success: false, message: "Wrong OTP" });

    const user = await User.findOne({ email });

    const resetToken = crypto.randomBytes(32).toString("hex");

    user.resetPasswordToken = resetToken;
    user.resetPasswordExpires = Date.now() + 10 * 60 * 1000;
    await user.save();

    await OTP.deleteOne({ email, isForgotPassword: true });

    return res.status(200).json({
      success: true,
      message: "OTP verified",
      resetToken,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(500).json({ success: false, message: error.message });
  }
};

// reset password
export const resetPassword = async (req, res) => {
  const { password, token } = req.body;

  const user = await User.findOne({
    resetPasswordToken: token,
    resetPasswordExpires: { $gt: Date.now() },
  });

  if (!user)
    return res
      .status(400)
      .json({ success: false, message: "Token invalid or expired" });

  const hashedPassword = await bcrypt.hash(password, 10);

  user.password = hashedPassword;
  user.resetPasswordToken = undefined;
  user.resetPasswordExpires = undefined;

  await user.save();

  return res.json({ success: true, message: "Password reset successfully" });
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
