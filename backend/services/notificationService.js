import Notification from "../models/notification.model.js";

/**
 * Central helper — creates a notification in DB and emits via Socket.IO.
 * Pass `io` from req.app.get("io").
 */
export async function createNotification(io, { userId, type, title, message, complaintId = null, metadata = {} }) {
  try {
    const notification = await Notification.create({ userId, type, title, message, complaintId, metadata });

    if (io) {
      const room = userId.toString();
      const clients = io.sockets.adapter.rooms.get(room);
      console.log(`[Socket] Sending ${type} to room ${room}. Listeners: ${clients ? clients.size : 0}`);

      io.to(room).emit("new_notification", {
        _id: notification._id,
        type: notification.type,
        title: notification.title,
        message: notification.message,
        complaintId: notification.complaintId,
        isRead: notification.isRead,
        createdAt: notification.createdAt,
      });
    }
    return notification;
  } catch (err) {
    console.error("[NotificationService] Failed:", err.message);
  }
}

/** Call when admin changes complaint status */
export async function notifyStatusChanged(io, { complaintOwnerId, complaintId, oldStatus, newStatus, category }) {
  return createNotification(io, {
    userId: complaintOwnerId,
    type: "status_changed",
    title: "Issue Status Updated",
    message: `Your ${category} report moved from "${oldStatus}" to "${newStatus}".`,
    complaintId,
    metadata: { oldStatus, newStatus },
  });
}

/** Call when a user upvotes a complaint */
export async function notifyUpvoted(io, { complaintOwnerId, complaintId, upvoterId, upvoterName, category }) {
  // Check if a notification already exists for this upvoter on this complaint
  const existing = await Notification.findOne({
    userId: complaintOwnerId,
    type: "upvoted",
    complaintId: complaintId,
    "metadata.upvoterId": upvoterId.toString()
  });

  if (existing) {
    // Already notified once, don't spam.
    return existing;
  }

  return createNotification(io, {
    userId: complaintOwnerId,
    type: "upvoted",
    title: "Someone Upvoted Your Report",
    message: `${upvoterName} upvoted your "${category}" report. Keep it up!`,
    complaintId,
    metadata: { upvoterName, upvoterId: upvoterId.toString() },
  });
}

/** Call when admin adds a chat message */
export async function notifyAdminComment(io, { complaintOwnerId, complaintId, adminName, commentPreview, category }) {
  return createNotification(io, {
    userId: complaintOwnerId,
    type: "admin_comment",
    title: "Message from Admin",
    message: `${adminName} commented on your ${category} report: "${commentPreview}"`,
    complaintId,
    metadata: { adminName, commentPreview },
  });
}

/** Notify admin that a resident sent a message */
export async function notifyAdminNewMessage(io, { adminId, complaintId, userName, messagePreview, category }) {
  return createNotification(io, {
    userId: adminId,
    type: "new_message",
    title: "New Message from Resident",
    message: `${userName} sent a message regarding ${category}: "${messagePreview}"`,
    complaintId,
  });
}

/** Call when a new complaint is filed — notify district neighbours */
export async function notifyNewInDistrict(io, { districtUserIds, complaintId, category, district }) {
  const promises = districtUserIds.map((userId) =>
    createNotification(io, {
      userId,
      type: "new_in_district",
      title: "New Issue in Your District",
      message: `A new "${category}" issue was reported in ${district}.`,
      complaintId,
      metadata: { district },
    })
  );
  return Promise.all(promises);
}

/** Notify nearby users that an issue needs verification */
export async function notifyNearbyForVerification(io, { nearbyUserIds, complaintId, category, city }) {
  const promises = nearbyUserIds.map((userId) =>
    createNotification(io, {
      userId,
      type: "verification_needed",
      title: "Help Verify Resolution",
      message: `The "${category}" issue in ${city} was marked resolved. Can you verify it?`,
      complaintId,
    })
  );
  return Promise.all(promises);
}

/** Notify owner that issue is verified resolved */
export async function notifyOwnerVerified(io, { complaintOwnerId, complaintId, category, count }) {
  return createNotification(io, {
    userId: complaintOwnerId,
    type: "verified",
    title: "Issue Verified Resolved",
    message: `Your "${category}" report has been verified resolved by ${count} citizens!`,
    complaintId,
  });
}

/** Notify admin that a resolution is disputed */
export async function notifyAdminDisputed(io, { adminIds, complaintId, category, aiConfidence }) {
  const promises = adminIds.map((userId) =>
    createNotification(io, {
      userId,
      type: "disputed",
      title: "Resolution Disputed",
      message: `A resolution for "${category}" was disputed. AI confirms issue may still exist (${aiConfidence}%).`,
      complaintId,
    })
  );
  return Promise.all(promises);
}

/** Notify disputer when admin decides */
export async function notifyDisputeResolved(io, { userId, complaintId, category, action }) {
  const actionText = action === "reopen" ? "re-opened" : "confirmed resolved";
  return createNotification(io, {
    userId,
    type: "dispute_resolved",
    title: "Dispute Reviewed",
    message: `Admin reviewed your dispute for "${category}" and ${actionText} the issue.`,
    complaintId,
  });
}

/** 
 * Notify all parties when a user verifies a report 
 */
export async function notifyVerificationDone(io, { verifierId, verifierName, reporterId, adminIds, complaintId, category, city }) {
  console.log(`[Notification] Dispatching verification alerts for complaint ${complaintId}`);
  console.log(`[Notification] Verifier: ${verifierId}, Reporter: ${reporterId}, Admins count: ${adminIds.length}`);

  // 1. Notify Verifier (Thank you)
  await createNotification(io, {
    userId: verifierId,
    type: "verified_by_you",
    title: "Verification Complete",
    message: "You have successfully verified this report. Thank you for your contribution!",
    complaintId,
  });

  // 2. Notify Reporter (Only if they are not the verifier)
  if (reporterId.toString() !== verifierId.toString()) {
    console.log(`[Notification] Notifying reporter ${reporterId}`);
    await createNotification(io, {
      userId: reporterId,
      type: "verified_by_peer",
      title: "Your Report was Verified",
      message: `${verifierName} has verified that your "${category}" issue in ${city} is resolved.`,
      complaintId,
      metadata: { verifierName }
    });
  } else {
    console.log("[Notification] Verifier is reporter, skipping peer notification");
  }

  // 3. Notify Admins
  const adminPromises = adminIds.map(adminId => {
    console.log(`[Notification] Notifying admin ${adminId}`);
    return createNotification(io, {
      userId: adminId,
      type: "admin_verification_alert",
      title: "Citizen Verified Report",
      message: `${verifierName} verified the "${category}" report in ${city}.`,
      complaintId,
      metadata: { verifierName, city }
    });
  });

  return Promise.all(adminPromises);
}

/** Notify admins when a new report is filed in their district */
export async function notifyAdminNewReport(io, { adminIds, complaintId, category, city }) {
  const title = "New Report in Your Area";
  const message = `A new ${category} report has been filed in ${city}.`;

  console.log(`[Notification] Notifying ${adminIds.length} admins about new report in ${city}:`, adminIds);
  for (const adminId of adminIds) {
    await createNotification(io, {
      userId: adminId,
      type: "admin_new_report",
      title,
      message,
      complaintId,
    });
  }
}
