import { useEffect, useState, useMemo } from "react";
import API from "../api/axios";
import AppHeader from "../components/AppHeader";
import Sidebar from "../components/dashboard/Sidebar";
import { Link, useNavigate } from "react-router-dom";

const getStatusBadgeClass = (status = "") => {
  const s = status.toLowerCase();
  if (s === "resolved" || s === "confirmed_resolved") return "ui-badge ui-badge-resolved";
  if (["in_progress", "in progress", "re_opened", "pending_verification", "disputed"].includes(s)) return "ui-badge ui-badge-progress";
  return "ui-badge ui-badge-new";
};

const haversineDistance = (lat1, lng1, lat2, lng2) => {
  const toRad = (d) => (d * Math.PI) / 180;
  const R = 6371000;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
};

const Explore = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem("sidebarCollapsed") === "true");
  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    localStorage.setItem("sidebarCollapsed", String(next));
  };
  const [userDistrict, setUserDistrict] = useState("");
  const [scope, setScope] = useState(() => localStorage.getItem("exploreScope") || "MY_DISTRICT");
  const [filter, setFilter] = useState(() => localStorage.getItem("exploreFilter") || "all");
  const [search, setSearch] = useState(() => localStorage.getItem("exploreSearch") || "");
  const [sort, setSort] = useState(() => localStorage.getItem("exploreSort") || "newest");
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [stats, setStats] = useState(null);
  const [userLoc, setUserLoc] = useState(null);
  const [showBeyond, setShowBeyond] = useState(false);

  useEffect(() => {
    const data = localStorage.getItem("userData");
    if (data) {
      try {
        const u = JSON.parse(data);
        setUser(u);
        setUserDistrict(u.homeDistrict || u.address || "");
      } catch {}
    }
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => setUserLoc({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => {}
      );
    }
  }, []);

  useEffect(() => {
    if (!userLoc) return;
    API.post("/auth/update-location", { latitude: userLoc.lat, longitude: userLoc.lng }).catch(() => {});
  }, [userLoc]);

  useEffect(() => localStorage.setItem("exploreScope", scope), [scope]);
  useEffect(() => localStorage.setItem("exploreFilter", filter), [filter]);
  useEffect(() => localStorage.setItem("exploreSort", sort), [sort]);
  useEffect(() => localStorage.setItem("exploreSearch", search), [search]);

  const fetchFeed = async (p = 1) => {
    setLoading(true);
    try {
      const districtParam = scope === "MY_DISTRICT" ? userDistrict : "all";
      const res = await API.get(`/complaint/feed?district=${encodeURIComponent(districtParam)}&page=${p}&limit=20`);
      if (res.data.success) {
        setComplaints(res.data.complaints);
        setTotalPages(res.data.pagination?.totalPages || 1);
        setPage(p);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      const districtParam = scope === "MY_DISTRICT" ? userDistrict : "all";
      const res = await API.get(`/complaint/public-stats?district=${encodeURIComponent(districtParam)}`);
      if (res.data.success) setStats(res.data.stats);
    } catch {}
  };

  useEffect(() => {
    fetchFeed(1);
    fetchStats();
  }, [scope, userDistrict]);

  const filtered = useMemo(() => {
    let list = [...complaints];
    if (filter !== "all") {
      if (filter === "active") {
        list = list.filter((c) => ["in_progress", "in progress", "re_opened", "pending_verification", "disputed"].includes(c.status));
      } else if (filter === "resolved") {
        list = list.filter((c) => ["resolved", "confirmed_resolved"].includes(c.status));
      } else {
        list = list.filter((c) => c.status === filter);
      }
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter((c) => `${c.description} ${c.category} ${c.city} ${c.landmark}`.toLowerCase().includes(q));
    }
    if (sort === "newest") list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    else if (sort === "oldest") list.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    else if (sort === "nearest" && userLoc) {
      list = list
        .map((c) => ({ ...c, _dist: haversineDistance(userLoc.lat, userLoc.lng, parseFloat(c.latitude), parseFloat(c.longitude)) }))
        .sort((a, b) => a._dist - b._dist);
      if (!showBeyond && scope === "MY_DISTRICT") {
        const within = list.filter((c) => c._dist <= 5000);
        return { visible: within, hiddenCount: list.length - within.length, all: list };
      }
    }
    return { visible: list, hiddenCount: 0, all: list };
  }, [complaints, filter, search, sort, userLoc, showBeyond, scope]);

  const visibleList = Array.isArray(filtered) ? filtered : filtered.visible;
  const hiddenCount = Array.isArray(filtered) ? 0 : filtered.hiddenCount;

  const logout = async () => {
    try {
      await API.post("/auth/logout");
    } catch {
      // clear local session even if the server call fails
    }
    localStorage.removeItem("userData");
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <div className="ui-page ui-page-dashboard">
      <AppHeader
        brandTo="/dashboard"
        brandInitial="⬢"
        brandLabel="CivicResolve"
        title="Explore"
        subtitle={scope === "MY_DISTRICT" ? `My District • ${userDistrict || "Set district in profile"}` : "Global Feed • Nationwide"}
        actions={
          <div className="flex items-center gap-2">
            <div className="app-header__user hidden sm:flex">
              <span className="app-header__user-name">{user?.fullName || "Citizen"}</span>
              <span className="app-header__user-meta">Explore • Ledger</span>
            </div>
            <button onClick={() => navigate("/dashboard")} className="ui-btn ui-btn-secondary">
              Dashboard
            </button>
          </div>
        }
      />

      <Sidebar activeTab="explore" setActiveTab={() => navigate("/dashboard")} user={user} logout={logout} collapsed={collapsed} onToggle={toggleCollapsed} />

      <main className={`px-3 py-4 pb-20 md:px-4 md:pb-6 will-change-[margin] transition-[margin] duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] ${collapsed ? "md:ml-14" : "md:ml-56"}`}>
        <div className="mx-auto max-w-[1500px] space-y-4">
        {stats && (
          <div className="ui-grid-auto mb-4">
            <div className="ui-card">
              <p className="text-xs font-semibold text-[color:var(--ui-text-muted)]">Scope</p>
              <p className="text-sm font-bold">{stats.scope}</p>
            </div>
            <div className="ui-card">
              <p className="text-xs font-semibold text-[color:var(--ui-text-muted)]">Active issues</p>
              <p className="text-lg font-bold">{stats.totalActive}</p>
            </div>
            <div className="ui-card">
              <p className="text-xs font-semibold text-[color:var(--ui-text-muted)]">Resolved</p>
              <p className="text-lg font-bold text-[color:var(--ui-success)]">{stats.totalResolved}</p>
            </div>
          </div>
        )}

        <div className="mb-3 flex flex-wrap gap-2">
          <button
            onClick={() => setScope("MY_DISTRICT")}
            className={`ui-btn ${scope === "MY_DISTRICT" ? "ui-btn-primary" : "ui-btn-secondary"}`}
          >
            <span className="material-symbols-outlined text-[18px]">location_on</span> My District
          </button>
          <button
            onClick={() => setScope("GLOBAL_FEED")}
            className={`ui-btn ${scope === "GLOBAL_FEED" ? "ui-btn-primary" : "ui-btn-secondary"}`}
          >
            <span className="material-symbols-outlined text-[18px]">language</span> Global Feed
          </button>
        </div>

        <div className="ui-card mb-4">
          <div className="grid gap-3 md:grid-cols-[1fr_180px_180px_180px]">
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by description, category, city…"
              className="ui-input"
            />
            <select value={filter} onChange={(e) => setFilter(e.target.value)} className="ui-select">
              <option value="all">All status</option>
              <option value="new">New</option>
              <option value="active">Active</option>
              <option value="resolved">Resolved</option>
            </select>
            <select value={sort} onChange={(e) => setSort(e.target.value)} className="ui-select">
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
              <option value="nearest">Nearest (5km)</option>
            </select>
            <div className="flex gap-2">
              <button onClick={() => fetchFeed(1)} className="ui-btn ui-btn-secondary flex-1">
                Refresh
              </button>
              {sort === "nearest" && !userLoc && (
                <span className="flex items-center text-xs text-amber-600">Enable location for nearest</span>
              )}
            </div>
          </div>
        </div>

        {loading ? (
          <div className="ui-card text-center text-sm text-[color:var(--ui-text-muted)]">Loading ledger…</div>
        ) : (
          <>
            {hiddenCount > 0 && (
              <div className="ui-card flex items-center justify-between gap-3 border-amber-200 bg-amber-50 text-sm">
                <span>{hiddenCount} beyond 5km hidden.</span>
                <button onClick={() => setShowBeyond(true)} className="ui-btn ui-btn-secondary !min-h-0 !py-1 text-xs">
                  Show all
                </button>
              </div>
            )}
            {visibleList.length === 0 ? (
              <div className="ui-card text-center py-10">
                <span className="material-symbols-outlined text-[28px] text-[color:var(--ui-text-muted)]">search_off</span>
                <p className="mt-2 text-sm font-semibold">No entries match</p>
                <p className="text-xs text-[color:var(--ui-text-muted)]">Try Global feed or clear filters.</p>
              </div>
            ) : (
              <div className="grid gap-3">
                {visibleList.map((c) => {
                  const dist =
                    userLoc && c.latitude && c.longitude
                      ? haversineDistance(userLoc.lat, userLoc.lng, parseFloat(c.latitude), parseFloat(c.longitude))
                      : null;
                  const distColor = dist == null ? "" : dist <= 1000 ? "text-emerald-600" : dist <= 5000 ? "text-amber-600" : "text-red-500";
                  return (
                    <Link key={c._id} to={`/complaint-overview/${c._id}`} className="ui-card flex gap-3 hover:border-[color:var(--ui-accent)] hover:shadow-sm transition">
                      {c.beforeImageUrl && <img src={c.beforeImageUrl} alt="" className="h-20 w-20 shrink-0 rounded-lg object-cover border border-[color:var(--ui-border)]" />}
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-[15px] font-semibold leading-tight">{c.description?.slice(0, 88)}</p>
                        <p className="mt-1 text-xs text-[color:var(--ui-text-muted)] font-mono">
                          CIV-{c._id.slice(-6).toUpperCase()} • {c.category?.replace(/_/g, " ")} • {c.city}
                        </p>
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                          <span className={getStatusBadgeClass(c.status)}>{c.status}</span>
                          {dist != null && <span className={`text-xs font-mono font-medium ${distColor}`}>{(dist / 1000).toFixed(1)}km</span>}
                          {c.supportCount > 0 && <span className="text-xs font-semibold">▲ {c.supportCount}</span>}
                          <span className="ml-auto text-xs text-[color:var(--ui-text-muted)]">{new Date(c.createdAt).toLocaleDateString()}</span>
                        </div>
                      </div>
                    </Link>
                  );
                })}
              </div>
            )}
            <div className="mt-6 flex items-center justify-between border-t border-[color:var(--ui-border)] pt-4">
              <button disabled={page <= 1} onClick={() => fetchFeed(page - 1)} className="ui-btn ui-btn-secondary">
                Previous
              </button>
              <span className="ui-mono text-xs">Page {page} of {totalPages}</span>
              <button disabled={page >= totalPages} onClick={() => fetchFeed(page + 1)} className="ui-btn ui-btn-secondary">
                Next
              </button>
            </div>
          </>
        )}
        </div>
      </main>

      <nav className="fixed bottom-0 left-0 z-40 flex w-full justify-around border-t border-[color:var(--ui-border)] bg-[color:var(--ui-surface)] p-3 md:hidden">
        <button onClick={() => navigate("/dashboard")} className="ui-btn ui-btn-ghost">
          Home
        </button>
        <button onClick={() => navigate("/explore")} className="ui-btn ui-btn-primary">
          Explore
        </button>
        <button onClick={() => navigate("/map")} className="ui-btn ui-btn-ghost">
          Map
        </button>
        <button onClick={() => navigate("/dashboard")} className="ui-btn ui-btn-ghost">
          Chats
        </button>
      </nav>
    </div>
  );
};

export default Explore;
