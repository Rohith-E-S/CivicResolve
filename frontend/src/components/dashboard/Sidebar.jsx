import { useNavigate } from "react-router-dom";
const Sidebar = ({ activeTab, setActiveTab, logout, collapsed = false, onToggle }) => {
  const navigate = useNavigate();
  const items = [
    { key: "overview", label: "Overview", icon: "dashboard" },
    { key: "new-complaint", label: "New complaint", icon: "add_location" },
    { key: "chats", label: "Chats", icon: "chat" },
    { key: "profile", label: "Profile", icon: "person" },
  ];

  return (
    <aside
      className={`fixed left-0 top-0 hidden h-full border-r border-[color:var(--ui-border)] bg-[color:var(--ui-surface)] pt-16 md:flex md:flex-col overflow-hidden will-change-[width] transition-[width] duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] ${collapsed ? "w-14" : "w-56"}`}
    >
      <div className={`flex items-center gap-2 py-2 ${collapsed ? "justify-center px-1.5" : "justify-between px-4"}`}>
        <p className={`text-sm font-semibold truncate whitespace-nowrap transition-all duration-300 ${collapsed ? "max-w-0 opacity-0" : "max-w-[140px] opacity-100"}`}>Citizen dashboard</p>
        <button
          onClick={onToggle}
          className="hidden md:inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-[color:var(--ui-border)] bg-[color:var(--ui-surface-muted)] text-[color:var(--ui-text-muted)] hover:text-[color:var(--ui-text)] hover:bg-[color:var(--ui-surface)] transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-[color:var(--ui-focus)]"
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <span className={`material-symbols-outlined text-[18px] transition-transform duration-300 ${collapsed ? "" : "rotate-180"}`}>chevron_right</span>
        </button>
      </div>

      <nav className={`flex-1 space-y-1 overflow-hidden transition-[padding] duration-300 ${collapsed ? "px-1.5" : "px-3"}`}>
        {items.map((item) => (
          <button
            key={item.key}
            onClick={() => setActiveTab(item.key)}
            title={collapsed ? item.label : undefined}
            className={`ui-btn w-full overflow-hidden whitespace-nowrap ${collapsed ? "justify-center !px-0" : "justify-start"} ${activeTab === item.key ? "ui-btn-primary" : "ui-btn-secondary"}`}
          >
            <span className="material-symbols-outlined text-[18px] shrink-0">{item.icon}</span>
            <span className={`truncate transition-all duration-300 ${collapsed ? "max-w-0 opacity-0 ml-0" : "max-w-[120px] opacity-100 ml-2"}`}>{item.label}</span>
          </button>
        ))}
        <button
          onClick={() => navigate("/explore")}
          title={collapsed ? "Explore" : undefined}
          className={`ui-btn w-full overflow-hidden whitespace-nowrap ${collapsed ? "justify-center !px-0" : "justify-start"} ${activeTab === "explore" ? "ui-btn-primary" : "ui-btn-secondary"}`}
        >
          <span className="material-symbols-outlined text-[18px] shrink-0">explore</span>
          <span className={`truncate transition-all duration-300 ${collapsed ? "max-w-0 opacity-0 ml-0" : "max-w-[120px] opacity-100 ml-2"}`}>Explore</span>
        </button>
        <button
          onClick={() => navigate("/map")}
          title={collapsed ? "Map view" : undefined}
          className={`ui-btn w-full overflow-hidden whitespace-nowrap ${collapsed ? "justify-center !px-0" : "justify-start"} ${activeTab === "map" ? "ui-btn-primary" : "ui-btn-secondary"}`}
        >
          <span className="material-symbols-outlined text-[18px] shrink-0">map</span>
          <span className={`truncate transition-all duration-300 ${collapsed ? "max-w-0 opacity-0 ml-0" : "max-w-[120px] opacity-100 ml-2"}`}>Map view</span>
        </button>
      </nav>

      <div className={`py-2 overflow-hidden transition-[padding] duration-300 ${collapsed ? "px-1.5" : "p-3"}`}>
        <button
          onClick={logout}
          title={collapsed ? "Log out" : undefined}
          className={`ui-btn ui-btn-secondary w-full overflow-hidden whitespace-nowrap ${collapsed ? "justify-center !px-0" : "justify-start"}`}
        >
          <span className="material-symbols-outlined text-[18px] shrink-0">logout</span>
          <span className={`truncate transition-all duration-300 ${collapsed ? "max-w-0 opacity-0 ml-0" : "max-w-[120px] opacity-100 ml-2"}`}>Log out</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
