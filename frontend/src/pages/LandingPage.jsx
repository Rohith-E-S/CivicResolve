import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../api/axios";
import AppHeader from "../components/AppHeader";

const LandingPage = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(() => !!localStorage.getItem("token"));
  const [stats, setStats] = useState({ totalActive: 0, totalResolved: 0, scope: "Nationwide" });
  const navigate = useNavigate();

  useEffect(() => {
    API.get("/complaint/public-stats?district=all")
      .then((r) => r.data?.stats && setStats(r.data.stats))
      .catch(() => {});
  }, []);

  const handleLogout = async () => {
    try {
      await API.post("/auth/logout");
      localStorage.removeItem("token");
      localStorage.removeItem("userData");
      setIsLoggedIn(false);
      navigate("/");
    } catch (err) {
      console.error("Logout failed", err);
    }
  };

  return (
    <div className="ui-page">
      <AppHeader
        brandTo="/"
        brandInitial="⬢"
        brandLabel="CivicResolve"
        title="Municipal ledger"
        subtitle="Report • Verify • Resolve"
        actions={
          isLoggedIn ? (
            <>
              <Link to="/explore" className="ui-btn ui-btn-secondary">
                <span className="material-symbols-outlined text-[16px]">explore</span> Explore map
              </Link>
              <Link to="/dashboard" className="ui-btn ui-btn-primary">
                Dashboard
              </Link>
              <button onClick={handleLogout} className="ui-btn ui-btn-ghost">
                Log out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="ui-btn ui-btn-secondary">
                Sign in
              </Link>
              <Link to="/signup" className="ui-btn ui-btn-primary">
                Create account
              </Link>
            </>
          )
        }
      />

      <main className="ui-container py-8 sm:py-10">
        {/* Thesis Hero – Blueprint + Ledger */}
        <section className="hero-blueprint p-6 sm:p-8 mb-6">
          <div className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr] items-center">
            <div>
              <p className="ui-mono mb-2 text-[color:var(--ui-accent)]">Civic ledger • Every dot is a neighborhood report</p>
              <h1 className="ui-display text-[clamp(2.2rem,4vw+1rem,3.6rem)]">
                Your street, <br />
                <span className="italic font-normal text-[color:var(--ui-accent)]">on the record.</span>
              </h1>
              <p className="ui-subtitle max-w-xl mt-3">
                File a report, watch it appear on the communal map, and follow it from <strong>new → verification → resolved</strong>. Not a ticket — a visible entry in the neighborhood ledger.
              </p>
              <div className="mt-6 flex flex-wrap gap-3">
                {isLoggedIn ? (
                  <>
                    <Link to="/dashboard" className="ui-btn ui-btn-primary">
                      Open dashboard
                    </Link>
                    <Link to="/explore" className="ui-btn ui-btn-secondary">
                      <span className="material-symbols-outlined text-[18px]">map</span> Explore live map
                    </Link>
                  </>
                ) : (
                  <>
                    <Link to="/signup" className="ui-btn ui-btn-accent">
                      Submit a complaint
                    </Link>
                    <Link to="/explore" className="ui-btn ui-btn-secondary">
                      View public map
                    </Link>
                    <Link to="/login" className="ui-btn ui-btn-ghost">
                      Track existing
                    </Link>
                  </>
                )}
              </div>
              <div className="mt-6 flex flex-wrap gap-4 text-xs">
                <span className="ui-stencil">CIV-LEDGER 2024 • Surveyor grid 32×32</span>
                <span className="hidden sm:inline text-[color:var(--ui-text-muted)]">Est. for residents & ward admins • No tracking • Public by default</span>
              </div>
            </div>

            <div className="relative">
              <div className="rounded-xl overflow-hidden border border-[color:var(--ui-border)] bg-[color:var(--ui-surface-muted)] h-[280px] relative">
                <div className="absolute inset-0 opacity-20" style={{ backgroundImage: `radial-gradient(circle at 30% 40%, #1A56DB 2px, transparent 2.5px), radial-gradient(circle at 65% 55%, #FF6B2B 2px, transparent 2.5px), radial-gradient(circle at 50% 30%, #0E9F6E 2px, transparent 2.5px), radial-gradient(circle at 75% 70%, #1A56DB 1.5px, transparent 2px)`, backgroundSize: "100% 100%" }} />
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="bg-[color:var(--ui-surface)] border border-[color:var(--ui-border)] rounded-lg p-3 shadow">
                    <div className="flex items-center gap-2">
                      <span className="ui-dot pulse" style={{ color: "#1A56DB" }} />
                      <span className="text-xs font-bold">Live ledger</span>
                      <span className="ui-stencil !py-0 !text-[10px]">{stats.totalActive + stats.totalResolved} dots</span>
                    </div>
                    <div className="mt-2 grid grid-cols-2 gap-2">
                      <div className="rounded bg-amber-50 border border-amber-200 px-3 py-2 text-center">
                        <p className="text-lg font-bold text-amber-700">{stats.totalActive}</p>
                        <p className="text-[10px] font-bold tracking-widest uppercase text-amber-700">Active</p>
                      </div>
                      <div className="rounded bg-emerald-50 border border-emerald-200 px-3 py-2 text-center">
                        <p className="text-lg font-bold text-emerald-700">{stats.totalResolved}</p>
                        <p className="text-[10px] font-bold tracking-widest uppercase text-emerald-700">Resolved</p>
                      </div>
                    </div>
                    <p className="mt-2 text-[10px] text-center text-[color:var(--ui-text-muted)] font-mono">{stats.scope}</p>
                  </div>
                </div>
                <div className="absolute bottom-2 left-2 flex gap-1">
                  <span className="h-2 w-2 rounded-full bg-red-500 border border-white" />
                  <span className="h-2 w-2 rounded-full bg-[#FFB74D] border border-white" />
                  <span className="h-2 w-2 rounded-full bg-[#0E9F6E] border border-white" />
                  <span className="text-[10px] ml-1 font-mono text-[color:var(--ui-text-muted)]">new • active • resolved</span>
                </div>
              </div>
              <p className="mt-2 text-center text-xs text-[color:var(--ui-text-muted)] font-mono">Carto Voyager • OSM • 32m grid • No API key</p>
            </div>
          </div>
        </section>

        {/* How it works – 3 civic verbs with numbers that encode sequence */}
        <section className="grid gap-4 md:grid-cols-3 mb-6">
          {[
            { n: "01", title: "Report", desc: "Drop a pin, describe the issue, add a photo. It becomes a dot on the district map within seconds.", icon: "location_on", accent: "#1A56DB" },
            { n: "02", title: "Verify", desc: "Neighbors within 500m confirm fixes. Three verifications move a pending report to resolved.", icon: "verified", accent: "#FF6B2B" },
            { n: "03", title: "Resolve", desc: "Admins update status, you rate the result, and the dot turns green — visible to everyone.", icon: "check_circle", accent: "#0E9F6E" },
          ].map((s) => (
            <article key={s.n} className="ui-card relative overflow-hidden">
              <span className="absolute right-3 top-3 font-mono text-5xl font-black opacity-5" style={{ color: s.accent }}>{s.n}</span>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-[20px]" style={{ color: s.accent }}>{s.icon}</span>
                <span className="ui-mono text-[11px]" style={{ color: s.accent }}>Step {s.n}</span>
              </div>
              <h2 className="mt-2 font-semibold" style={{ fontFamily: "Instrument Serif, serif", fontSize: "1.25rem" }}>{s.title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-[color:var(--ui-text-muted)]">{s.desc}</p>
            </article>
          ))}
        </section>

        <section className="ui-card flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <p className="ui-mono text-xs">Public by default • No login to browse</p>
            <p className="text-sm text-[color:var(--ui-text-muted)] mt-1">Even without an account, the map and recent reports are visible. Sign in to contribute.</p>
          </div>
          <Link to="/explore" className="ui-btn ui-btn-primary">
            Open explore → 
          </Link>
        </section>
      </main>
    </div>
  );
};

export default LandingPage;
