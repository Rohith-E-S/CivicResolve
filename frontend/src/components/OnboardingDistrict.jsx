import { useState } from "react";
import API from "../api/axios";

const DISTRICTS = ["Gobi", "Erode", "Coimbatore", "Chennai", "Salem", "Madurai", "Tiruchirappalli", "Thanjavur", "Dindigul", "Tiruppur"];

const OnboardingDistrict = ({ user, onComplete }) => {
  const [district, setDistrict] = useState(user?.homeDistrict || "");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [locLoading, setLocLoading] = useState(false);

  const detectDistrict = () => {
    if (!navigator.geolocation) {
      setMessage("Geolocation not supported");
      return;
    }
    setLocLoading(true);
    setMessage("Detecting district…");
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${pos.coords.latitude}&lon=${pos.coords.longitude}`);
          const data = await res.json();
          const city = data?.address?.city || data?.address?.town || data?.address?.village || "";
          if (city) {
            setDistrict(city);
            setMessage(`Detected ${city} — confirm below`);
          } else setMessage("Detected location, please select district manually");
        } catch {
          setMessage("Could not reverse geocode — select manually");
        } finally {
          setLocLoading(false);
        }
      },
      () => {
        setMessage("Location denied — select district manually");
        setLocLoading(false);
      }
    );
  };

  const handleSave = async () => {
    if (!district.trim()) {
      setMessage("Select a district to continue");
      return;
    }
    setLoading(true);
    try {
      const res = await API.post("/auth/update-home-district", { district: district.trim() });
      const updated = res.data.user || { ...user, homeDistrict: district.trim() };
      localStorage.setItem("userData", JSON.stringify(updated));
      onComplete(updated);
    } catch (e) {
      setMessage(e.response?.data?.message || "Failed to save district");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-[#18181B]/80 p-4 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-xl border border-[#3F3F46] bg-[#27272A] p-6 shadow-2xl">
        <p className="ui-mono text-[#60A5FA]">Step 1 • Set your ledger district</p>
        <h2 className="ui-display text-2xl mt-2 text-white">Where do you live?</h2>
        <p className="text-sm text-zinc-400 mt-2">We use this to show <strong className="text-white">My District</strong> reports and to keep your `Map view` centered. You can change it later in Profile.</p>

        <div className="mt-5 space-y-3">
          <label className="ui-label text-zinc-300">Home district</label>
          <select value={district} onChange={(e) => setDistrict(e.target.value)} className="ui-input bg-[#18181B] border-[#3F3F46] text-white">
            <option value="">Select district…</option>
            {DISTRICTS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
          <button type="button" onClick={detectDistrict} disabled={locLoading} className="ui-btn w-full justify-center bg-[#18181B] border-[#3F3F46] text-white hover:bg-[#3F3F46]">
            <span className="material-symbols-outlined text-[16px]">my_location</span>
            {locLoading ? "Detecting…" : "Use current location"}
          </button>
        </div>

        {message && <p className="mt-3 text-xs font-mono text-zinc-400 bg-[#18181B] border border-[#3F3F46] rounded px-2 py-1">{message}</p>}

        <div className="mt-6 flex gap-2">
          <button onClick={handleSave} disabled={loading} className="ui-btn bg-white !text-zinc-900 flex-1 justify-center font-bold hover:bg-zinc-100 border border-zinc-200">
            {loading ? "Saving…" : "Save & continue"}
          </button>
        </div>
        <p className="mt-2 text-center text-xs text-zinc-500">Takes 5 seconds • You can skip by selecting manually</p>
      </div>
    </div>
  );
};

export default OnboardingDistrict;
