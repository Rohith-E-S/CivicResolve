import express from "express";
import {
  createComplaint,
  deleteMyComplaint,
  filterComplaintOnStateCity,
  getAllComplaints,
  getNearbyComplaints,
  getComplaint,
  getMyComplaint,
  updateAfterImageUrl,
  updateComplaint,
  updateMyComplaint,
  updateComplaintStatus,
  rateComplaint,
  supportComplaint,
  getComplaintStats,
  getPaginatedComplaints,
  getMyComplaintStats,
  getMyPaginatedComplaints,
  getComplaintsWithMessages,
  getMyComplaintsWithMessages,
  getPublicStats,
  getPublicFeed,
} from "../controllers/complaint.controller.js";
import { protectRoute } from "../middleware/auth.middleware.js";
import { upload } from "../middleware/uploads.js";

const complaintRouter = express.Router();

complaintRouter.post("/create-complaint",protectRoute,upload.single("imageUrl"),createComplaint,);

complaintRouter.get("/get-complaints", protectRoute, getMyComplaint);

complaintRouter.get("/get-all-complaints", protectRoute, getAllComplaints);

complaintRouter.get("/public-stats", protectRoute, getPublicStats);
complaintRouter.get("/feed", protectRoute, getPublicFeed);

complaintRouter.post("/update-complaint-status/:id",protectRoute,updateComplaintStatus,);

complaintRouter.get("/fetch-complaints-city-state",protectRoute,filterComplaintOnStateCity,);

complaintRouter.post("/upload-after-image/:id",protectRoute,upload.single("imageUrl"),updateAfterImageUrl,);

complaintRouter.post("/update-complaint-status-upload-image/:id",protectRoute,upload.single("imageUrl"),updateComplaint,);

complaintRouter.get("/get-complaint-data/:id", protectRoute, getComplaint);

complaintRouter.post("/rate/:id", protectRoute, rateComplaint);

complaintRouter.get("/admin/stats", protectRoute, getComplaintStats);
complaintRouter.get("/admin/active-chats",protectRoute,
  getComplaintsWithMessages,
);
complaintRouter.get("/admin/list", protectRoute, getPaginatedComplaints);

complaintRouter.get("/my-stats", protectRoute, getMyComplaintStats);
complaintRouter.get("/my-active-chats",protectRoute,
  getMyComplaintsWithMessages,
);
complaintRouter.get("/my-list", protectRoute, getMyPaginatedComplaints);

complaintRouter.get("/nearby", protectRoute, getNearbyComplaints);
complaintRouter.patch("/:id", protectRoute, updateMyComplaint);
complaintRouter.delete("/:id", protectRoute, deleteMyComplaint);
complaintRouter.post("/support/:id", protectRoute, supportComplaint);

export default complaintRouter;
