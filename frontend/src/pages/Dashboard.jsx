import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../api/axios";
import Sidebar from "../components/dashboard/Sidebar";
import Overview from "../components/dashboard/Overview";
import NewComplaint from "../components/dashboard/NewComplaint";
import Profile from "../components/dashboard/Profile";
import UserChats from "../components/dashboard/UserChats";
import AppHeader from "../components/AppHeader";
import OnboardingDistrict from "../components/OnboardingDistrict";

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState(localStorage.getItem("dashboardActiveTab") || "overview");
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem("sidebarCollapsed") === "true");
  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    localStorage.setItem("sidebarCollapsed", String(next));
  };
  const [stats, setStats] = useState({
    total: 0,
    newComplaint: 0,
    inProgressComplaint: 0,
    resolvedComplaint: 0,
  });
  const [loading, setLoading] = useState(false);
  const [user, setUser] = useState(null);
  const [message, setMessage] = useState({ type: "", text: "" });
  const [submitLoading, setSubmitLoading] = useState(false);
  const [formData, setFormData] = useState({
    description: "",
    city: "",
    state: "",
    landmark: "",
    latitude: "",
    longitude: "",
    imageUrl: null,
  });
  const [profileData, setProfileData] = useState({
    fullName: "",
    address: "",
    profilePic: null,
    previewUrl: null,
  });
  const [showOnboarding, setShowOnboarding] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    localStorage.setItem("dashboardActiveTab", activeTab);
  }, [activeTab]);

  // External navigators (Command palette, Explore bottom nav) request a tab
  // switch via this event — localStorage alone can't re-trigger a tab change
  // when /dashboard is already open
  useEffect(() => {
    const handler = (e) => {
      if (e.detail) setActiveTab(e.detail);
    };
    window.addEventListener("dashboard:tab", handler);
    return () => window.removeEventListener("dashboard:tab", handler);
  }, []);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await API.get("/complaint/my-stats");
      if (res.data.success) setStats(res.data.stats);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const data = localStorage.getItem("userData");
    if (data) {
      const parsed = JSON.parse(data);
      setUser(parsed);
      setProfileData({
        fullName: parsed.fullName,
        address: parsed.address,
        previewUrl: parsed.profilePic || null,
      });
      if (!parsed.homeDistrict || parsed.homeDistrict.trim() === "") {
        setShowOnboarding(true);
      }
    }
    fetchStats();
  }, []);

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
        title="Citizen ledger"
        subtitle="Case tracking and submissions"
        actions={
          <div className="app-header__user">
            <span className="app-header__user-name">{user?.fullName || "Citizen"}</span>
            <span className="app-header__user-meta">CIV-{String(stats.total || 0).padStart(4, "0")} • Ledger</span>
          </div>
        }
      />

      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} user={user} logout={logout} collapsed={collapsed} onToggle={toggleCollapsed} />

      {showOnboarding && user && (
        <OnboardingDistrict
          user={user}
          onComplete={(updated) => {
            setUser(updated);
            setShowOnboarding(false);
          }}
        />
      )}

      <button
        onClick={() => setActiveTab("new-complaint")}
        className="fixed bottom-20 right-4 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-[color:var(--ui-accent-strong)] text-white shadow-lg hover:brightness-110 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[color:var(--ui-focus)] md:bottom-6 md:right-6 md:h-12 md:w-12"
        aria-label="Report new issue"
        title="Report new issue (N)"
      >
        <span className="material-symbols-outlined text-[24px]">add_location</span>
      </button>

      <main className={`px-3 py-4 pb-20 md:px-4 md:pb-6 will-change-[margin] transition-[margin] duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] ${collapsed ? "md:ml-14" : "md:ml-56"}`}>
        <div className="mx-auto max-w-[1500px] space-y-4">
          {activeTab === "overview" && (
            <Overview stats={stats} loading={loading} setActiveTab={setActiveTab} user={user} />
          )}
          {activeTab === "new-complaint" && (
            <NewComplaint
              formData={formData}
              setFormData={setFormData}
              submitLoading={submitLoading}
              setSubmitLoading={setSubmitLoading}
              message={message}
              setMessage={setMessage}
              fetchComplaints={fetchStats}
              setActiveTab={setActiveTab}
            />
          )}
          {activeTab === "chats" && <UserChats setActiveTab={setActiveTab} />}
          {activeTab === "profile" && (
            <Profile
              profileData={profileData}
              setProfileData={setProfileData}
              message={message}
              setMessage={setMessage}
              submitLoading={submitLoading}
              setSubmitLoading={setSubmitLoading}
              user={user}
              setUser={setUser}
            />
          )}
        </div>
      </main>

      <nav className="fixed bottom-0 left-0 z-40 flex w-full justify-around border-t border-[color:var(--ui-border)] bg-[color:var(--ui-surface)] p-3 md:hidden">
        <button onClick={() => setActiveTab("overview")} className="ui-btn ui-btn-ghost">
          Home
        </button>
        <button onClick={() => setActiveTab("new-complaint")} className="ui-btn ui-btn-ghost">
          New
        </button>
        <button onClick={() => navigate("/explore")} className="ui-btn ui-btn-ghost">
          Explore
        </button>
        <button onClick={() => setActiveTab("chats")} className="ui-btn ui-btn-ghost">
          Chats
        </button>
        <button onClick={() => setActiveTab("profile")} className="ui-btn ui-btn-ghost">
          Profile
        </button>
      </nav>
    </div>
  );
};

export default Dashboard;
