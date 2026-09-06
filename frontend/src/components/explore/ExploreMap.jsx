import { useEffect, useState, useMemo, useRef } from "react";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import { Link } from "react-router-dom";

// DeFlock-style palette: dark base + high-contrast civic dots
const getStatusColor = (status = "") => {
  const s = status.toLowerCase();
  if (["new", "under_review"].includes(s)) return "#FF453A"; // DeFlock red
  if (["in_progress", "in progress", "re_opened"].includes(s)) return "#FF9F0A";
  if (["pending_verification"].includes(s)) return "#FFCC00";
  if (["disputed"].includes(s)) return "#AF52DE";
  if (["resolved", "confirmed_resolved"].includes(s)) return "#30D158";
  return "#8E8E93";
};

const createStatusIcon = (status) => {
  const color = getStatusColor(status);
  const pulse = ["new", "under_review", "disputed"].includes(status.toLowerCase());
  return L.divIcon({
    className: "deflock-dot",
    html: `<div style="background:${color};width:14px;height:14px;border-radius:50%;border:2px solid #18181B;box-shadow:0 0 0 2px ${color} , 0 0 12px ${color}80;${pulse ? "animation:deflockPulse 1.6s infinite;" : ""}"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
    popupAnchor: [0, -10],
  });
};

const Recenter = ({ center, zoom }) => {
  const map = useMap();
  const prev = useRef(null);
  useEffect(() => {
    if (!center) return;
    const key = `${center[0].toFixed(4)},${center[1].toFixed(4)},${zoom}`;
    if (prev.current === key) return;
    prev.current = key;
    map.setView(center, zoom, { animate: true, duration: 0.8 });
  }, [center, zoom, map]);
  return null;
};

const ExploreMap = ({ complaints = [], scope = "MY_DISTRICT", onSelect, fullHeight = false }) => {
  const [selected, setSelected] = useState(null);
  const [showFilters, setShowFilters] = useState(false);
  const [activeLayers, setActiveLayers] = useState(new Set(["New", "Active", "Resolved", "Disputed"]));
  const [activeFilters, setActiveFilters] = useState(new Set(["ROAD", "GARBAGE", "WATER", "ELECTRIC", "OTHER"]));

  const layerForStatus = (status = "") => {
    const s = status.toLowerCase();
    if (["new", "under_review"].includes(s)) return "New";
    if (["in_progress", "in progress", "re_opened", "pending_verification"].includes(s)) return "Active";
    if (["resolved", "confirmed_resolved"].includes(s)) return "Resolved";
    if (s === "disputed") return "Disputed";
    return null;
  };
  const categoryFor = (cat = "") => {
    const c = cat.toLowerCase();
    if (c === "road_damage") return "ROAD";
    if (c === "garbage_issue") return "GARBAGE";
    if (c === "water_leakage") return "WATER";
    if (c === "electricity_issue") return "ELECTRIC";
    return "OTHER";
  };
  const toggleLayer = (label) =>
    setActiveLayers((prev) => {
      const next = new Set(prev);
      if (next.has(label)) next.delete(label);
      else next.add(label);
      return next;
    });
  const toggleFilter = (label) =>
    setActiveFilters((prev) => {
      const next = new Set(prev);
      if (next.has(label)) next.delete(label);
      else next.add(label);
      return next;
    });

  const validComplaints = complaints.filter((c) => {
    const lat = parseFloat(c.latitude);
    const lng = parseFloat(c.longitude);
    return !isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0;
  });

  const filteredComplaints = validComplaints.filter((c) => {
    const layer = layerForStatus(c.status);
    if (layer && !activeLayers.has(layer)) return false;
    const cat = categoryFor(c.category);
    if (!activeFilters.has(cat)) return false;
    return true;
  });

  if (validComplaints.length === 0) {
    return (
      <div className={`flex ${fullHeight ? "h-full" : "h-[560px]"} flex-col items-center justify-center ${fullHeight ? "border-0 rounded-none" : "rounded-xl border border-[#3F3F46]"} bg-[#18181B] text-sm text-zinc-400`}>
        <span className="material-symbols-outlined text-[24px] mb-2">satellite_alt</span>
        No surveillance dots in view. Adjust filters or switch to Global.
      </div>
    );
  }

  // Center on ALL valid dots for current scope — filtering must NOT move the map (DeFlock-style: viewport stays where user left it)
  const avgLat = validComplaints.reduce((a, c) => a + parseFloat(c.latitude), 0) / validComplaints.length;
  const avgLng = validComplaints.reduce((a, c) => a + parseFloat(c.longitude), 0) / validComplaints.length;
  const targetZoom = scope === "MY_DISTRICT" ? 12 : scope === "GLOBAL_FEED" ? 7 : 12;
  const center = useMemo(() => [avgLat, avgLng], [avgLat, avgLng, targetZoom]);

  return (
    <div className={`relative overflow-hidden ${fullHeight ? "flex flex-col h-full border-0 rounded-none" : "rounded-xl border border-[#3F3F46]"} bg-[#18181B]`} style={{ fontFamily: "Inter, sans-serif" }}>
      <style>{`@keyframes deflockPulse{0%{box-shadow:0 0 0 2px var(--c), 0 0 0 0 rgba(255,69,58,0.5);}70%{box-shadow:0 0 0 2px var(--c), 0 0 0 10px transparent;}100%{box-shadow:0 0 0 2px var(--c), 0 0 0 0 transparent;}}`}</style>

      {/* DeFlock top bar */}
      <div className={`flex h-11 shrink-0 items-center justify-between border-b border-[#27272A] bg-[#18181B]/90 px-3 backdrop-blur ${fullHeight ? "" : "absolute top-0 left-0 right-0 z-[401]"}`}>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded bg-[#1A56DB] text-[11px] font-black text-white">⬢</span>
            <span className="hidden sm:inline text-sm font-bold tracking-tight text-white">CivicResolve</span>
            <span className="hidden lg:inline text-[10px] font-mono tracking-widest text-zinc-500">MAPS</span>
          </div>
          <nav className="hidden md:flex items-center gap-1 ml-4">
            <span className="px-2.5 py-1 rounded text-xs font-medium bg-white text-black">Map</span>
          </nav>
        </div>
        <div className="flex items-center gap-2">
          <span className="hidden sm:inline-flex items-center gap-1 rounded bg-[#27272A] border border-[#3F3F46] px-2.5 py-1 text-xs font-mono text-zinc-300">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" /> {filteredComplaints.length} in view
          </span>
          <button onClick={() => setShowFilters(!showFilters)} className="md:hidden rounded border border-[#3F3F46] bg-[#27272A] px-2 py-1 text-xs text-white">
            Filters
          </button>
          <button className="hidden sm:inline-flex items-center gap-1 rounded bg-white px-3 py-1 text-xs font-semibold text-black">
            <span className="material-symbols-outlined text-[14px]">share</span> Share
          </button>
        </div>
      </div>

      <div className={`flex ${fullHeight ? "flex-1 min-h-0" : ""}`}>
        {/* DeFlock left drawer - Layers/Filters */}
        <div className={`${showFilters ? "flex" : "hidden"} md:flex absolute md:static left-0 top-11 md:top-0 z-[400] w-[220px] shrink-0 flex-col border-r border-[#27272A] bg-[#18181B] p-3 ${fullHeight ? "h-full md:h-full" : "md:h-[560px] h-[calc(560px-44px)]"} overflow-auto`}>
          <div className="space-y-4">
            <div>
              <div className="flex items-center justify-between">
                <p className="text-[10px] font-mono tracking-widest text-zinc-500">LAYERS</p>
                <button
                  onClick={() => setActiveLayers(new Set(["New", "Active", "Resolved", "Disputed"]))}
                  className="text-[9px] font-mono uppercase tracking-widest text-zinc-500 hover:text-white"
                >
                  All
                </button>
              </div>
              <div className="mt-2 space-y-1">
                {[
                  { label: "New", dot: "#FF453A", count: validComplaints.filter((c) => ["new", "under_review"].includes(c.status)).length },
                  { label: "Active", dot: "#FF9F0A", count: validComplaints.filter((c) => ["in_progress", "re_opened", "pending_verification"].includes(c.status)).length },
                  { label: "Resolved", dot: "#30D158", count: validComplaints.filter((c) => ["resolved", "confirmed_resolved"].includes(c.status)).length },
                  { label: "Disputed", dot: "#AF52DE", count: validComplaints.filter((c) => c.status === "disputed").length },
                ].map((l) => {
                  const active = activeLayers.has(l.label);
                  return (
                    <button
                      key={l.label}
                      onClick={() => toggleLayer(l.label)}
                      className={`flex w-full items-center justify-between rounded border px-2 py-1.5 text-left transition ${active ? "bg-[#27272A] border-[#3F3F46] hover:border-zinc-600" : "bg-[#18181B] border-[#27272A] opacity-60 hover:opacity-100"}`}
                    >
                      <span className="flex items-center gap-2 text-xs">
                        <span className="flex h-3.5 w-3.5 items-center justify-center rounded-sm border" style={{ background: active ? l.dot : "transparent", borderColor: active ? l.dot : "#3F3F46" }}>
                          {active && <span className="text-[10px] leading-none text-black">✓</span>}
                        </span>
                        <span className={active ? "text-zinc-200" : "text-zinc-500"}>{l.label}</span>
                      </span>
                      <span className={`text-xs font-mono ${active ? "text-zinc-300" : "text-zinc-600"}`}>{l.count}</span>
                    </button>
                  );
                })}
              </div>
            </div>
            <div>
              <div className="flex items-center justify-between">
                <p className="text-[10px] font-mono tracking-widest text-zinc-500">FILTERS</p>
                <button
                  onClick={() => setActiveFilters(new Set(["ROAD", "GARBAGE", "WATER", "ELECTRIC", "OTHER"]))}
                  className="text-[9px] font-mono uppercase tracking-widest text-zinc-500 hover:text-white"
                >
                  All
                </button>
              </div>
              <div className="mt-2 flex flex-wrap gap-1">
                {[
                  { key: "ROAD", label: "road" },
                  { key: "GARBAGE", label: "garbage" },
                  { key: "WATER", label: "water" },
                  { key: "ELECTRIC", label: "electric" },
                  { key: "OTHER", label: "other" },
                ].map((f) => {
                  const active = activeFilters.has(f.key);
                  return (
                    <button
                      key={f.key}
                      onClick={() => toggleFilter(f.key)}
                      className={`rounded-full border px-2.5 py-1 text-[10px] font-mono uppercase tracking-widest transition ${active ? "bg-white text-black border-white" : "bg-[#27272A] text-zinc-500 border-[#3F3F46] hover:border-zinc-600 hover:text-zinc-300"}`}
                    >
                      {f.label}
                    </button>
                  );
                })}
              </div>
            </div>
            <div>
              <p className="text-[10px] font-mono tracking-widest text-zinc-500">HEATMAP</p>
              <div className="mt-2 rounded bg-[#27272A] p-2">
                <div className="h-1.5 rounded-full bg-gradient-to-r from-[#18181B] via-[#FF453A] to-[#FFCC00]" />
                <div className="mt-1 flex justify-between text-[9px] font-mono text-zinc-500"><span>Low</span><span>Density</span><span>High</span></div>
              </div>
            </div>
            <p className="text-[9px] font-mono leading-relaxed text-zinc-600 border-t border-[#27272A] pt-3">
              Crowdsourced civic surveillance map. Data from CivicResolve & OSM contributors. Every dot is a resident report, not a camera.
            </p>
          </div>
        </div>

        <div className="relative flex-1 min-h-0">
          <MapContainer center={center} zoom={targetZoom} scrollWheelZoom style={{ height: fullHeight ? "100%" : "560px", width: "100%", background: "#18181B" }} zoomControl={false}>
            <TileLayer
              url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
              subdomains={["a", "b", "c", "d"]}
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; CARTO'
              maxZoom={19}
            />
            <Recenter center={center} zoom={targetZoom} />
            {filteredComplaints.map((c) => (
              <Marker
                key={c._id}
                position={[parseFloat(c.latitude), parseFloat(c.longitude)]}
                icon={createStatusIcon(c.status)}
                eventHandlers={{
                  click: () => {
                    setSelected(c);
                    if (onSelect) onSelect(c);
                  },
                }}
              />
            ))}
          </MapContainer>

          {filteredComplaints.length === 0 && validComplaints.length > 0 && (
            <div className="absolute top-1/2 left-1/2 z-[400] -translate-x-1/2 -translate-y-1/2 rounded-lg border border-[#3F3F46] bg-[#18181B]/90 px-4 py-2 text-xs font-mono text-zinc-400 backdrop-blur">
              No dots match filters • {activeLayers.size} layers • {activeFilters.size} categories
            </div>
          )}
          {/* Bottom DeFlock attribution bar */}
          <div className="absolute bottom-0 left-0 right-0 z-[400] flex items-center justify-between border-t border-[#27272A] bg-[#18181B]/90 px-2 py-1 text-[9px] font-mono text-zinc-500 backdrop-blur">
            <span>Maps by <a href="https://openroadlabs.org" className="underline">OpenRoad Labs</a> • © OSM</span>
            <span className="hidden sm:inline">DeFlock-style • WebGL required • Civic ledger</span>
          </div>

          {/* Detail drawer like DeFlock */}
          {selected && (
            <div className="absolute bottom-8 left-2 right-2 md:left-1/2 md:right-auto md:w-[380px] md:-translate-x-1/2 z-[400] rounded-lg border border-[#3F3F46] bg-[#27272A] p-3 shadow-2xl">
              <div className="flex gap-3">
                {selected.beforeImageUrl && <img src={selected.beforeImageUrl} alt="" className="h-16 w-16 rounded object-cover border border-[#3F3F46]" />}
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-mono uppercase tracking-widest" style={{ color: getStatusColor(selected.status) }}>
                    {selected.category?.replace(/_/g, " ")} • CIV-{selected._id.slice(-6).toUpperCase()}
                  </p>
                  <p className="line-clamp-2 text-xs text-zinc-300 mt-1">{selected.description}</p>
                  <p className="mt-1 flex items-center gap-1 text-[10px] font-mono text-zinc-500">
                    <span className="h-1.5 w-1.5 rounded-full" style={{ background: getStatusColor(selected.status) }} />{selected.status} • {selected.city} • {new Date(selected.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <button onClick={() => setSelected(null)} className="h-6 w-6 shrink-0 rounded bg-[#27272A] text-zinc-400">×</button>
              </div>
              <Link to={`/complaint-overview/${selected._id}`} style={{ color: "#18181B" }} className="mt-3 flex items-center justify-center gap-1.5 rounded !bg-white !text-zinc-900 py-2 text-center text-xs font-bold tracking-wide !text-zinc-900 hover:!bg-zinc-100 border border-zinc-200 shadow-sm">
                <span style={{ color: "#18181B" }}>Open ledger entry</span>
                <span style={{ color: "#18181B" }} className="material-symbols-outlined text-[14px]">arrow_forward</span>
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ExploreMap;
