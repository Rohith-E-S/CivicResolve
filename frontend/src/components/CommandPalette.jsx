import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

const goToDashboardTab = (tab) => (nav) => {
  window.dispatchEvent(new CustomEvent("dashboard:tab", { detail: tab }));
  nav("/dashboard");
};

const COMMANDS = [
  { id: "overview", label: "Go to Overview", icon: "dashboard", action: (nav) => nav("/dashboard"), keys: "G O" },
  { id: "new", label: "New complaint", icon: "add_location", action: goToDashboardTab("new-complaint"), keys: "N" },
  { id: "explore", label: "Explore list", icon: "explore", action: (nav) => nav("/explore"), keys: "E" },
  { id: "map", label: "Map view", icon: "map", action: (nav) => nav("/map"), keys: "M" },
  { id: "chats", label: "Chats", icon: "chat", action: goToDashboardTab("chats"), keys: "C" },
  { id: "profile", label: "Profile", icon: "person", action: goToDashboardTab("profile"), keys: "P" },
];

const CommandPalette = () => {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const handler = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setOpen((v) => !v);
      }
      if (e.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  useEffect(() => {
    if (open) setQuery("");
  }, [open]);

  if (!open) return null;

  const q = query.toLowerCase();
  const filtered = COMMANDS.filter((c) => !q || c.label.toLowerCase().includes(q) || c.id.includes(q));
  const isCIV = /^civ[-_]?[a-f0-9]{4,8}$/i.test(q.trim());

  return (
    <div className="fixed inset-0 z-[80] flex items-start justify-center bg-[#18181B]/60 p-4 pt-[20vh] backdrop-blur-sm" onClick={() => setOpen(false)}>
      <div className="w-full max-w-lg rounded-xl border border-[#3F3F46] bg-[#27272A] shadow-2xl overflow-hidden" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-2 border-b border-[#3F3F46] px-3 py-2">
          <span className="material-symbols-outlined text-zinc-400 text-[18px]">search</span>
          <input
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search commands, CIV-XXXX, city…  (⌘K to close)"
            className="flex-1 bg-transparent text-sm text-white placeholder:text-zinc-500 focus:outline-none"
          />
          <span className="hidden sm:inline rounded border border-[#3F3F46] bg-[#18181B] px-1.5 py-0.5 text-[10px] font-mono text-zinc-500">ESC</span>
        </div>
        <div className="max-h-[50vh] overflow-auto p-2">
          {isCIV && (
            <button
              onClick={() => {
                const id = q.replace(/^civ[-_]?/i, "");
                // try to navigate to overview with search? For now go to explore
                navigate(`/explore`);
                setOpen(false);
              }}
              className="flex w-full items-center gap-3 rounded-lg bg-[#18181B] border border-[#3F3F46] px-3 py-2 text-left hover:bg-[#3F3F46]"
            >
              <span className="material-symbols-outlined text-[18px] text-zinc-400">tag</span>
              <span className="text-sm font-medium">Search {q.toUpperCase()} in Explore</span>
              <span className="ml-auto text-xs text-zinc-500">↵</span>
            </button>
          )}
          <p className="px-2 py-1 text-[10px] font-mono uppercase tracking-widest text-zinc-500">Quick actions</p>
          {filtered.map((c) => (
            <button
              key={c.id}
              onClick={() => {
                c.action(navigate);
                setOpen(false);
              }}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left hover:bg-[#18181B] border border-transparent hover:border-[#3F3F46]"
            >
              <span className="flex h-7 w-7 items-center justify-center rounded bg-[#18181B] border border-[#3F3F46]"><span className="material-symbols-outlined text-[16px] text-zinc-300">{c.icon}</span></span>
              <span className="text-sm font-medium">{c.label}</span>
              <span className="ml-auto text-xs font-mono text-zinc-500">{c.keys}</span>
            </button>
          ))}
          {filtered.length === 0 && !isCIV && <p className="px-3 py-4 text-center text-sm text-zinc-500">No commands match “{query}”</p>}
        </div>
        <div className="border-t border-[#3F3F46] bg-[#18181B] px-3 py-1.5 flex justify-between text-[10px] font-mono text-zinc-500">
          <span>↑↓ Navigate • ↵ Select • ⌘K Close</span>
          <span>Veteran • {COMMANDS.length} commands</span>
        </div>
      </div>
    </div>
  );
};

export default CommandPalette;
