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
} from "../controllers/user.controller.js";

import { protectRoute } from "../middleware/auth.middleware.js";
import { upload } from "../middleware/uploads.js";

const authRouter = express.Router();

// OTP BASED SIGNUP
authRouter.post("/send-otp", sendOtp);
authRouter.post("/verify-otp", verifyOtp);
authRouter.post("/create-account", createAccount);

// LOGIN / LOGOUT
authRouter.post("/login", login);
authRouter.post("/logout", logout);

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
authRouter.post("/google-login", googleLogin);

authRouter.post("/sendPasswordResetOtp", sendPasswordResetOtp);

authRouter.post("/verifyPasswordResetOtp", verifyPasswordResetOtp);

authRouter.post("/reset-password", resetPassword);

export default authRouter;
