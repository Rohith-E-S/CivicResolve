import mongoose from "mongoose";

const notificationSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    type: {
      type: String,
      enum: [
        "status_changed",
        "upvoted",
        "admin_comment",
        "new_in_district",
        "verification_needed",
        "verified",
        "disputed",
        "dispute_resolved",
      ],
      required: true,
    },
    title:   { type: String, required: true },
    message: { type: String, required: true },
    complaintId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Complaint",
      default: null,
    },
    isRead:   { type: Boolean, default: false },
    metadata: { type: mongoose.Schema.Types.Mixed, default: {} },
  },
  { timestamps: true }
);

// Fast unread-count queries
notificationSchema.index({ userId: 1, isRead: 1 });

export default mongoose.model("Notification", notificationSchema);
