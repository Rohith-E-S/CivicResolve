// Single authorization rule for complaint chat access, shared by the socket
// handlers in server.js. Pure function over the authenticated user and the
// complaint document so it can be unit-tested without a database.
const toIdString = (value) => (value == null ? null : value.toString());

export const canAccessComplaintChat = (user, complaint) => {
  if (!user || !complaint) return false;
  // Soft-deleted complaints leave no chat surface
  if (complaint.isDeleted === true) return false;
  if (user.isAdmin) return true;
  return toIdString(complaint.user) === toIdString(user._id);
};

// The Message schema has no length cap; keep one message within notification
// preview sizes and prevent oversized socket payloads from reaching the DB.
export const MAX_CHAT_MESSAGE_LENGTH = 2000;

export const normalizeChatMessage = (rawMessage) => {
  if (typeof rawMessage !== "string") {
    return { error: "Message text is required" };
  }
  const message = rawMessage.trim();
  if (!message) {
    return { error: "Message cannot be empty" };
  }
  if (message.length > MAX_CHAT_MESSAGE_LENGTH) {
    return { error: `Message exceeds ${MAX_CHAT_MESSAGE_LENGTH} characters` };
  }
  return { message };
};
