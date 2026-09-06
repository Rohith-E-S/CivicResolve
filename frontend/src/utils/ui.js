export const getStatusBadgeClass = (status = "") => {
  const s = status.toLowerCase();
  if (s === "resolved" || s === "confirmed_resolved") return "ui-badge ui-badge-resolved";
  if (["in_progress", "in progress", "re_opened", "pending_verification", "disputed"].includes(s)) return "ui-badge ui-badge-progress";
  return "ui-badge ui-badge-new";
};
