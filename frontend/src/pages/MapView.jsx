import { useEffect, useState } from "react";
import API from "../api/axios";
import AppHeader from "../components/AppHeader";
import Sidebar from "../components/dashboard/Sidebar";
import { useNavigate } from "react-router-dom";
import ExploreMap from "../components/explore/ExploreMap";

const MapView = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem("sidebarCollapsed") === "true");
  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    localStorage.setItem("sidebarCollapsed", String(next));
  };
  const [userDistrict, setUserDistrict] = useState("");
  const [scope, setScope] = useState("MY_DISTRICT");
  const [complaints, setComplaints] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const data = localStorage.getItem("userData");
    if (data) {
      try {
        const u = JSON.parse(data);
        setUser(u);
        setUserDistrict(u.homeDistrict || u.address || "");
      } catch {}
    }
  }, []);

  const fetchFeed = async () => {
    setLoading(true);
    try {
      const districtParam = scope === "MY_DISTRICT" ? userDistrict : "all";
      const res = await API.get(`/complaint/feed?district=${encodeURIComponent(districtParam)}&page=1&limit=50`);
      if (res.data.success) setComplaints(res.data.complaints);
    } catch {}
    setLoading(false);
  };

  const fetchStats = async () => {
    try {
      const districtParam = scope === "MY_DISTRICT" ? userDistrict : "all";
      const res = await API.get(`/complaint/public-stats?district=${encodeURIComponent(districtParam)}`);
      if (res.data.success) setStats(res.data.stats);
    } catch {}
  };

  useEffect(() => {
    fetchFeed();
    fetchStats();
  }, [scope, userDistrict]);

  const logout = () => {
    localStorage.removeItem("userData");
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <div className="ui-page ui-page-dashboard bg-[#18181B]">
      <AppHeader
        brandTo="/dashboard"
        brandInitial="⬢"
        brandLabel="CivicResolve"
        title="Map view"
        subtitle={scope === "MY_DISTRICT" ? `Surveillance • ${userDistrict || "Set district"}` : "Surveillance • Nationwide"}
        actions={
          <div className="flex items-center gap-2">
            <div className="app-header__user hidden sm:flex">
              <span className="app-header__user-name text-white">{user?.fullName || "Citizen"}</span>
              <span className="app-header__user-meta text-zinc-400">Map • DeFlock style</span>
            </div>
            <button onClick={() => navigate("/dashboard")} className="ui-btn ui-btn-secondary bg-white text-black border-white">
              Dashboard
            </button>
          </div>
        }
      />
      <Sidebar activeTab="map" setActiveTab={() => navigate("/dashboard")} user={user} logout={logout} collapsed={collapsed} onToggle={toggleCollapsed} />

      <main className={`flex flex-col h-[calc(100vh-4.2rem)] pb-12 md:pb-0 will-change-[margin] transition-[margin] duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] ${collapsed ? "md:ml-14" : "md:ml-56"}`}>
        <div className="shrink-0 flex flex-wrap items-center gap-2 border-b border-[#27272A] bg-[#18181B] px-3 py-2">
          <div className="flex rounded-lg border border-[#3F3F46] bg-[#27272A] p-1">
            <button onClick={() => setScope("MY_DISTRICT")} className={`rounded px-3 py-1.5 text-xs font-semibold ${scope === "MY_DISTRICT" ? "bg-white text-black" : "text-zinc-400"}`}>
              <span className="material-symbols-outlined text-[14px] align-middle mr-1">location_on</span> My District
            </button>
            <button onClick={() => setScope("GLOBAL_FEED")} className={`rounded px-3 py-1.5 text-xs font-semibold ${scope === "GLOBAL_FEED" ? "bg-white text-black" : "text-zinc-400"}`}>
              <span className="material-symbols-outlined text-[14px] align-middle mr-1">language</span> Global
            </button>
          </div>
          {stats && (
            <span className="hidden sm:flex items-center gap-2 text-xs font-mono text-zinc-500">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" /> {stats.totalActive} active • {stats.totalResolved} resolved • <span className="truncate max-w-[120px]">{stats.scope}</span>
            </span>
          )}
          <span className="ml-auto flex items-center gap-2">
            <span className="hidden sm:inline text-xs font-mono text-zinc-600">{complaints.length} in view</span>
            <button onClick={fetchFeed} className="ui-btn !min-h-0 !py-1.5 !text-xs bg-[#27272A] border-[#3F3F46] text-white hover:bg-[#3F3F46]">Refresh</button>
          </span>
        </div>

        <div className="flex-1 min-h-0">
          {loading ? (
            <div className="flex h-full items-center justify-center bg-[#18181B] text-sm text-zinc-500">Loading surveillance dots…</div>
          ) : (
            <ExploreMap complaints={complaints} scope={scope} fullHeight />
          )}
        </div>
      </main>

      <nav className="fixed bottom-0 left-0 z-40 flex w-full justify-around border-t border-[#3F3F46] bg-[#18181B] p-3 md:hidden">
        <button onClick={() => navigate("/dashboard")} className="ui-btn bg-[#27272A] text-zinc-400 border-[#3F3F46]">
          Home
        </button>
        <button onClick={() => navigate("/explore")} className="ui-btn bg-[#27272A] text-zinc-400 border-[#3F3F46]">
          Explore
        </button>
        <button onClick={() => navigate("/map")} className="ui-btn bg-white text-black border-white">
          Map
        </button>
      </nav>
    </div>
  );
};

export default MapView;
