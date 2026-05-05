import express from "express";
import { getUserAnalytics, getAdminAnalytics } from "../controllers/analytics.controller.js";
import { protectRoute, adminOnly } from "../middleware/auth.middleware.js";

const analyticsRouter = express.Router();

analyticsRouter.get("/user", protectRoute, getUserAnalytics);
analyticsRouter.get("/admin", protectRoute, adminOnly, getAdminAnalytics);

export default analyticsRouter;
