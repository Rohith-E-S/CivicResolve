import express from "express";
import {
   sendOtp,
   verifyOtp,
   createAccount,
   login,
   logout,
   updateProfile,
   checkAuth,
   googleLogin,
   verifyPasswordResetOtp,
   sendPasswordResetOtp,
   resetPassword,
   updateHomeDistrict,
   updateUserLocation,
} from "../controllers/user.controller.js";

import { protectRoute } from "../middleware/auth.middleware.js";
import { upload } from "../middleware/uploads.js";

import { authRateLimit } from "../middleware/authRateLimit.js";

const authRouter = express.Router();
// Validate rate keys as strings; each purpose shares IP and account counters.
const sendLimit = authRateLimit("otp-send", { ipLimit: 20, accountLimit: 5 });
const verifyLimit = authRateLimit("otp-verify", { ipLimit: 50, accountLimit: 15 });
const loginLimit = authRateLimit("login");
const signupLimit = authRateLimit("signup");
const resetLimit = authRateLimit("reset");

// OTP BASED SIGNUP
authRouter.post("/send-otp", sendLimit, sendOtp);
authRouter.post("/verify-otp", verifyLimit, verifyOtp);
authRouter.post("/create-account", signupLimit, createAccount);

// LOGIN / LOGOUT
authRouter.post("/login", loginLimit, login);
authRouter.post("/logout", protectRoute, logout);

// CHECK AUTH
authRouter.get("/check-auth", protectRoute, checkAuth);

// UPDATE PROFILE
authRouter.post(
   "/update",
   protectRoute,
   upload.single("profilePic"),
   updateProfile,
);

// GOOGLE OAUTH
authRouter.post("/google-login", loginLimit, googleLogin);

authRouter.post("/sendPasswordResetOtp", sendLimit, sendPasswordResetOtp);

authRouter.post("/verifyPasswordResetOtp", verifyLimit, verifyPasswordResetOtp);

authRouter.post("/reset-password", resetLimit, resetPassword);

authRouter.post("/update-home-district", protectRoute, updateHomeDistrict);

// UPDATE USER GPS LOCATION
authRouter.post("/update-location", protectRoute, updateUserLocation);

export default authRouter;
