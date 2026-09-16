import { requestToken, verifySession } from "../services/authSecurity.js";
import User from "../models/user.model.js";

export const protectRoute = async (req, res, next) => {
  try {
    const token = requestToken(req);

    if (!token) {
      return res
        .status(401)
        .json({ success: false, message: "Not authenticated" });
    }

    const decodedMessage = verifySession(token);

    const user = await User.findById(decodedMessage._id).select("+sessionVersion");

    if (!user || (user.sessionVersion ?? 0) !== decodedMessage.sessionVersion) {
      return res
        .status(401)
        .json({ success: false, message: "User not found" });
    }

    req.user = user;

    next();
  } catch (error) {
    // An expired or tampered token throws in jwt.verify — that is an
    // authentication failure (401), not a server error
    // Never log authentication material or provider errors.
    return res.status(401).json({
      success: false,
      message: "Session invalid or expired. Please log in again.",
    });
  }
};

export const adminOnly = (req, res, next) => {
  if (req.user && req.user.isAdmin) {
    next();
  } else {
    res.status(403).json({ success: false, message: "Admin access required" });
  }
};
