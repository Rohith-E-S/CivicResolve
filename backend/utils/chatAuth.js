// Single authorization rule for complaint chat access, shared by the socket
// handlers in server.js. Pure function over the authenticated user and the
// complaint document so it can be unit-tested without a database.
// Accept database ObjectIds/populated references, but never missing identities.
export const toChatId = (value) => {
  if (value == null) return null;
  const id = value._id ?? value;
  const text = typeof id === "string" ? id : typeof id?.toHexString === "function" ? id.toHexString() : null;
  return typeof text === "string" && /^[a-f\d]{24}$/i.test(text) ? text.toLowerCase() : null;
};

// Socket payload IDs must be scalar strings, not MongoDB query objects.
export const isChatId = (value) => typeof value === "string" && toChatId(value) !== null;

export const canAccessComplaintChat = (user, complaint) => {
  if (!user || !complaint) return false;
  // Soft-deleted complaints leave no chat surface
  if (complaint.isDeleted === true) return false;
  const userId = toChatId(user._id);
  if (!userId) return false;
  if (user.isAdmin === true) return true;
  const ownerId = toChatId(complaint.user);
  return ownerId !== null && ownerId === userId;
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
