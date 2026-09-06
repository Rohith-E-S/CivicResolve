import jwt from "jsonwebtoken";
import User from "../models/user.model.js";

export const protectRoute = async (req, res, next) => {
  try {
    const token = req.cookies.token;

    if (!token) {
      return res
        .status(401)
        .json({ success: false, message: "Not authenticated" });
    }

    const decodedMessage = jwt.verify(token, process.env.JWT_SECRET_KEY);

    const user = await User.findById(decodedMessage._id);

    if (!user) {
      return res
        .status(401)
        .json({ success: false, message: "User not found" });
    }

    req.user = user;

    next();
  } catch (error) {
    // An expired or tampered token throws in jwt.verify — that is an
    // authentication failure (401), not a server error
    console.error("protectRoute Error:", error.message);
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
