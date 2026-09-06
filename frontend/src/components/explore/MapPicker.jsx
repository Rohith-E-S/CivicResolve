import { useEffect, useState } from "react";
import { MapContainer, TileLayer, useMap, useMapEvents } from "react-leaflet";

const RecenterButton = ({ onRecenter }) => (
  <button
    type="button"
    onClick={onRecenter}
    className="absolute bottom-3 right-3 z-[400] rounded-full bg-[color:var(--ui-surface)] p-2 shadow border border-[color:var(--ui-border)]"
    title="Use current location"
  >
    <span className="material-symbols-outlined text-[20px]">my_location</span>
  </button>
);

const MapEvents = ({ onMove }) => {
  const map = useMap();
  useMapEvents({
    moveend: () => {
      const c = map.getCenter();
      onMove(c.lat, c.lng);
    },
  });
  // Deliberately no onMove on mount: firing it with the default map center
  // used to pre-fill the form coordinates without any user action, which
  // defeated the "set a pin" submit guard and auto-geocoded the default
  // location
  return null;
};

const MapPicker = ({ latitude, longitude, onChange }) => {
  const [center, setCenter] = useState(() => {
    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);
    if (!isNaN(lat) && !isNaN(lng)) return [lat, lng];
    return [20.5937, 78.9629];
  });
  const [reverseLoading, setReverseLoading] = useState(false);

  useEffect(() => {
    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);
    if (!isNaN(lat) && !isNaN(lng)) setCenter([lat, lng]);
  }, [latitude, longitude]);

  const handleMove = (lat, lng) => {
    onChange(lat.toFixed(6), lng.toFixed(6));
  };

  const recenterToUser = () => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition((pos) => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      setCenter([lat, lng]);
      onChange(lat.toFixed(6), lng.toFixed(6));
    });
  };

  // Reverse geocode debounce handled by parent, here we just show pin
  return (
    <div className="relative h-[300px] overflow-hidden rounded-xl border border-[color:var(--ui-border)]">
      <MapContainer center={center} zoom={17.5} style={{ height: "100%", width: "100%" }} scrollWheelZoom>
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          subdomains={["a", "b", "c", "d"]}
          attribution='&copy; OSM &copy; CARTO'
          maxZoom={19}
        />
        <MapEvents onMove={handleMove} />
      </MapContainer>
      <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
        <span className="material-symbols-outlined -mt-6 text-[36px] text-red-500 drop-shadow">location_on</span>
      </div>
      <div className="pointer-events-none absolute top-2 left-1/2 -translate-x-1/2 rounded-full bg-black/70 px-3 py-1 text-xs text-white">
        Drag map to adjust pin
      </div>
      <RecenterButton onRecenter={recenterToUser} />
    </div>
  );
};

export default MapPicker;
