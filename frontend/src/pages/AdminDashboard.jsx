import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../api/axios";
import AdminOverview from "../components/adminDashboard/AdminOverview";
import AllComplaints from "../components/adminDashboard/AllComplaints";
import AdminChats from "../components/adminDashboard/AdminChats";
import AppHeader from "../components/AppHeader";

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState(
    localStorage.getItem("adminDashboardActiveTab") || "overview"
  );
  const [stats, setStats] = useState({
    newComplaint: [],
    inProgressComplaint: [],
    resolvedComplaint: [],
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: "", text: "" });
  const navigate = useNavigate();

  useEffect(() => {
    localStorage.setItem("adminDashboardActiveTab", activeTab);
  }, [activeTab]);

  const fetchStats = useCallback(async () => {
    setLoading(true);
    try {
      const res = await API.get("/complaint/admin/stats");
      if (res.data.success) setStats(res.data.stats);
    } catch {
      setMessage({ type: "error", text: "Failed to load stats." });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const data = JSON.parse(localStorage.getItem("userData"));
    if (!data?.isAdmin) {
      navigate("/dashboard");
      return;
    }
    fetchStats();
  }, [fetchStats, navigate]);

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
        brandTo="/admin-dashboard"
        brandInitial="A"
        brandLabel="Complaint Register Portal"
        title="Admin dashboard"
        subtitle="Review and resolve complaints"
        actions={
          <button onClick={logout} className="ui-btn ui-btn-secondary">
            Log out
          </button>
        }
      />

      <div className="px-3 py-4 pb-20 md:px-4 md:pb-6">
        <div className="mx-auto grid max-w-[1600px] gap-4 md:grid-cols-[220px_1fr]">
          <aside className="ui-card h-fit">
            <nav className="space-y-2">
              <button
                onClick={() => setActiveTab("overview")}
                className={`ui-btn w-full justify-start ${
                  activeTab === "overview" ? "ui-btn-primary" : "ui-btn-secondary"
                }`}
              >
                Overview
              </button>
              <button
                onClick={() => setActiveTab("complaints")}
                className={`ui-btn w-full justify-start ${
                  activeTab === "complaints" ? "ui-btn-primary" : "ui-btn-secondary"
                }`}
              >
                Complaints
              </button>
              <button
                onClick={() => setActiveTab("chats")}
                className={`ui-btn w-full justify-start ${
                  activeTab === "chats" ? "ui-btn-primary" : "ui-btn-secondary"
                }`}
              >
                Chats
              </button>
            </nav>
          </aside>

          <main className="space-y-4">
            {message.text && (
              <div className={`ui-alert ${message.type === "error" ? "ui-alert-error" : "ui-alert-success"}`}>
                {message.text}
              </div>
            )}
            {activeTab === "overview" && <AdminOverview complaints={stats} loading={loading} />}
            {activeTab === "complaints" && <AllComplaints />}
            {activeTab === "chats" && <AdminChats />}
          </main>
        </div>
      </div>

      <nav className="fixed bottom-0 left-0 z-40 flex w-full justify-around border-t border-[color:var(--ui-border)] bg-[color:var(--ui-surface)] p-3 md:hidden">
        <button onClick={() => setActiveTab("overview")} className="ui-btn ui-btn-ghost">
          Overview
        </button>
        <button onClick={() => setActiveTab("complaints")} className="ui-btn ui-btn-ghost">
          Complaints
        </button>
        <button onClick={() => setActiveTab("chats")} className="ui-btn ui-btn-ghost">
          Chats
        </button>
        <button onClick={logout} className="ui-btn ui-btn-ghost">
          Logout
        </button>
      </nav>
    </div>
  );
};

export default AdminDashboard;
