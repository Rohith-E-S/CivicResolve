import { useState, useEffect } from "react";
import API from "../../api/axios";
import MyComplaints from "./MyComplaints";

const Overview = ({ stats, setActiveTab, user }) => {
  const { total, newComplaint, inProgressComplaint, resolvedComplaint } = stats;
  const active = (newComplaint || 0) + (inProgressComplaint || 0);
  const [dismissed, setDismissed] = useState(() => localStorage.getItem("welcomeChecklistDismissed") === "true");
  const [locEnabled, setLocEnabled] = useState(false);
  const [nearbyVerify, setNearbyVerify] = useState(null);
  useEffect(() => {
    if (navigator.permissions) {
      navigator.permissions.query({ name: "geolocation" }).then((r) => setLocEnabled(r.state === "granted")).catch(() => {});
    }
  }, []);
  useEffect(() => {
    if ((total || 0) < 2) return;
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const res = await API.get(`/complaint/nearby?lat=${pos.coords.latitude}&lng=${pos.coords.longitude}&radius=1000`);
          const pending = res.data.complaints?.filter((c) => c.status === "pending_verification");
          if (pending?.length) setNearbyVerify(pending.length);
        } catch {}
      },
      () => {}
    );
  }, [total]);
  const hasDistrict = !!user?.homeDistrict?.trim();
  const hasReport = (total || 0) > 0;
  const steps = [
    { id: 1, label: "Set home district", done: hasDistrict, action: () => setActiveTab("profile"), icon: "location_on", cta: "Set district" },
    { id: 2, label: "Enable location", done: locEnabled, action: () => navigator.geolocation?.getCurrentPosition(() => setLocEnabled(true), () => {}), icon: "my_location", cta: "Enable" },
    { id: 3, label: "Submit first report", done: hasReport, action: () => setActiveTab("new-complaint"), icon: "add_location", cta: "Report" },
  ];
  const doneCount = steps.filter((s) => s.done).length;
  const showChecklist = !dismissed && doneCount < 3 && (total || 0) < 5;
  const progress = Math.round((doneCount / 3) * 100);
  return (
    <div className="space-y-4">
      <section className="hero-blueprint p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="ui-mono text-[color:var(--ui-accent)]">Ledger for {user?.fullName?.split(" ")[0] || "Citizen"} • CIV-{String(total || 0).padStart(4, "0")}</p>
            <h1 className="ui-display text-3xl mt-1">Welcome back, {user?.fullName?.split(" ")[0] || "Citizen"}</h1>
            <p className="mt-2 text-sm text-[color:var(--ui-text-muted)]">
              You have <strong className="text-[color:var(--ui-text)]">{active} active</strong> {active === 1 ? "report" : "reports"} on the district ledger. {active > 0 ? "Follow their dots on the map." : "Your first dot is one report away."}
            </p>
          </div>
          <div className="flex gap-2 shrink-0">
            <button onClick={() => setActiveTab("new-complaint")} className="ui-btn ui-btn-accent">
              <span className="material-symbols-outlined text-[18px]">add_location</span> Report
            </button>
            <button onClick={() => (window.location.href = "/explore")} className="ui-btn ui-btn-secondary">
              <span className="material-symbols-outlined text-[18px]">explore</span> Explore
            </button>
          </div>
        </div>
      </section>

      {showChecklist && (
        <section className="ui-card border-l-4" style={{ borderLeftColor: "var(--ui-accent)" }}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="ui-mono text-[color:var(--ui-accent)]">New here • {doneCount}/3 complete • {progress}%</p>
              <h3 className="font-semibold mt-1" style={{ fontFamily: "Instrument Serif, serif", fontSize: "1.1rem" }}>Your ledger setup</h3>
              <p className="text-xs text-[color:var(--ui-text-muted)] mt-1">Complete these to unlock My District and nearby verification.</p>
            </div>
            <button onClick={() => { setDismissed(true); localStorage.setItem("welcomeChecklistDismissed", "true"); }} className="text-xs text-[color:var(--ui-text-muted)] hover:text-[color:var(--ui-text)] px-2 py-1 rounded border border-transparent hover:border-[color:var(--ui-border)]">Dismiss</button>
          </div>
          <div className="mt-3 h-1.5 w-full rounded-full bg-[color:var(--ui-surface-muted)] overflow-hidden"><div className="h-full bg-[color:var(--ui-accent)] transition-all duration-500" style={{ width: `${progress}%` }} /></div>
          <div className="mt-4 grid gap-2 sm:grid-cols-3">
            {steps.map((s) => (
              <div key={s.id} className={`flex items-center gap-3 rounded-lg border p-3 ${s.done ? "bg-[color:var(--ui-surface-muted)] border-[color:var(--ui-border)] opacity-70" : "bg-[color:var(--ui-surface)] border-[color:var(--ui-accent)]"}`}>
                <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 ${s.done ? "bg-[color:var(--ui-success)] border-[color:var(--ui-success)] text-white" : "border-[color:var(--ui-border)] text-[color:var(--ui-text-muted)]"}`}>
                  <span className="material-symbols-outlined text-[16px]">{s.done ? "check" : s.icon}</span>
                </span>
                <div className="min-w-0 flex-1">
                  <p className={`text-sm font-semibold truncate ${s.done ? "line-through text-[color:var(--ui-text-muted)]" : ""}`}>{s.label}</p>
                  <p className="text-xs text-[color:var(--ui-text-muted)]">{s.done ? "Done" : `Step ${s.id}`}</p>
                </div>
                {!s.done && (
                  <button onClick={s.action} className="ui-btn ui-btn-secondary !min-h-[36px] !px-3 !py-1.5 text-xs shrink-0 focus-visible:outline focus-visible:outline-2">
                    {s.cta}
                  </button>
                )}
              </div>
            ))}
          </div>
        </section>
      )}

      {nearbyVerify && (
        <section className="ui-card flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-l-4 bg-amber-50 dark:bg-amber-950/30" style={{ borderLeftColor: "#F59E0B" }}>
          <div className="flex gap-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-500 text-white">
              <span className="material-symbols-outlined text-[18px]">verified</span>
            </span>
            <div>
              <p className="text-sm font-bold">Nearby verification needed</p>
              <p className="text-xs text-[color:var(--ui-text-muted)]">{nearbyVerify} {nearbyVerify === 1 ? "report" : "reports"} within 1km are pending verification — your confirmation helps.</p>
            </div>
          </div>
          <button onClick={() => (window.location.href = "/explore")} className="ui-btn bg-amber-500 text-white border-amber-500 hover:bg-amber-600 shrink-0">
            Verify nearby
          </button>
        </section>
      )}

      <section className="grid gap-3 grid-cols-2 lg:grid-cols-4">
        {[
          { label: "Total reports", value: total, sub: "in ledger", color: "var(--ui-text)", bg: "var(--ui-surface)" },
          { label: "New", value: newComplaint, sub: "awaiting triage", color: "#e53935", bg: "#FFF1F0" },
          { label: "In progress", value: inProgressComplaint, sub: "being worked", color: "#FF6B2B", bg: "#FFF7ED" },
          { label: "Resolved", value: resolvedComplaint, sub: "verified", color: "#0E9F6E", bg: "#ECFDF5" },
        ].map((s) => (
          <article key={s.label} className="ui-card relative overflow-hidden" style={{ borderLeft: `3px solid ${s.color}` }}>
            <p className="ui-mono text-[10px]">{s.label}</p>
            <p className="mt-1 text-2xl font-black tracking-tight" style={{ color: s.color }}>{s.value || 0}</p>
            <p className="text-xs text-[color:var(--ui-text-muted)]">{s.sub}</p>
            <span className="ui-dot absolute right-3 top-3" style={{ color: s.color }} />
          </article>
        ))}
      </section>

      <MyComplaints setActiveTab={setActiveTab} />
    </div>
  );
};

export default Overview;
