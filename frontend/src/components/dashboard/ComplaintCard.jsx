import { getStatusBadgeClass } from "../../utils/ui";

const statusDotColor = (status = "") => {
  const s = status.toLowerCase();
  if (["new", "under_review"].includes(s)) return "#e53935";
  if (["in_progress", "in progress", "re_opened"].includes(s)) return "#FFB74D";
  if (["pending_verification"].includes(s)) return "#E67E22";
  if (["disputed"].includes(s)) return "#9C27B0";
  if (["resolved", "confirmed_resolved"].includes(s)) return "#0E9F6E";
  return "#8B95A1";
};

const ComplaintCard = ({ complaint }) => {
  const dot = statusDotColor(complaint.status);
  const stencil = `CIV-${complaint._id.slice(-6).toUpperCase()}`;
  return (
    <article className="ui-card group relative overflow-hidden hover:border-[color:var(--ui-accent)] transition-colors">
      <div className="absolute left-0 top-0 h-full w-[3px]" style={{ background: dot }} />
      <div className="flex gap-3">
        <span className="ui-dot mt-1 shrink-0 pulse" style={{ color: dot }} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="ui-stencil">{stencil}</span>
            <span className={getStatusBadgeClass(complaint.status)}>{complaint.status}</span>
            {complaint.category && <span className="text-xs text-[color:var(--ui-text-muted)]">• {complaint.category.replace(/_/g, " ")}</span>}
            {complaint.supportCount > 0 && <span className="text-xs font-semibold">▲ {complaint.supportCount} supports</span>}
          </div>
          <h3 className="mt-2 truncate pr-2 text-[15px] font-semibold leading-tight group-hover:text-[color:var(--ui-accent)]">
            {complaint.description || "Reported issue"}
          </h3>
          <p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[color:var(--ui-text-muted)]">
            <span className="material-symbols-outlined text-[14px]">calendar_today</span> {new Date(complaint.createdAt).toLocaleDateString()}
            <span>•</span>
            <span className="material-symbols-outlined text-[14px]">location_on</span> {complaint.landmark ? `${complaint.landmark}, ` : ""}{complaint.city || "Unknown"}
          </p>
        </div>
        <div className="hidden sm:flex flex-col items-end gap-1 shrink-0">
          {complaint.status === "resolved" && complaint.rating > 0 && (
            <span className="text-xs font-bold text-[color:var(--ui-success)]">★ {complaint.rating}/5</span>
          )}
          <span className="text-[10px] font-mono text-[color:var(--ui-text-muted)]">{new Date(complaint.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
        </div>
      </div>
    </article>
  );
};

export default ComplaintCard;
