import express from "express";
import {
  getNotifications,
  getUnreadCount,
  markAllRead,
  markOneRead,
  clearAll,
} from "../controllers/notification.controller.js";
import { protectRoute } from "../middleware/auth.middleware.js";

const router = express.Router();

// All notification routes operate on the authenticated user's own data.
router.get("/",                   protectRoute, getNotifications);
router.get("/unread-count",       protectRoute, getUnreadCount);
router.patch("/read-all",         protectRoute, markAllRead);
router.patch("/:id/read",         protectRoute, markOneRead);
router.delete("/",                protectRoute, clearAll);

export default router;
