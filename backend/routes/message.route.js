import express from "express";
import { protectRoute } from "../middleware/auth.middleware.js";
import Message from "../models/message.model.js";
import Complaint from "../models/complaint.model.js";

const messageRouter = express.Router();

// GET /api/messages/:complaintId - Fetch chat history
messageRouter.get("/:complaintId", protectRoute, async (req, res) => {
    try {
        const { complaintId } = req.params;

        // Only the complaint owner or an admin may read the conversation
        const complaint = await Complaint.findOne({
            _id: complaintId,
            isDeleted: { $ne: true },
        }).select("user");

        if (!complaint) {
            return res.status(404).json({
                success: false,
                message: "Complaint not found",
            });
        }

        if (!req.user.isAdmin && complaint.user.toString() !== req.user._id.toString()) {
            return res.status(403).json({
                success: false,
                message: "You are not allowed to view this conversation",
            });
        }

        const messages = await Message.find({ complaintId })
            .populate("fromUser", "fullName email isAdmin")
            .populate("toUser", "fullName email isAdmin")
            .sort({ createdAt: 1 });

        return res.status(200).json({ success: true, messages });
    } catch (error) {
        console.log("Error fetching messages:", error.message);
        return res.status(500).json({
            success: false,
            message: `Error fetching messages: ${error.message}`,
        });
    }
});

export default messageRouter;
