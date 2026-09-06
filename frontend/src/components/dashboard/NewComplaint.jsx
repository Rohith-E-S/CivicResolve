import { useRef, useState, useEffect } from "react";
import API from "../../api/axios";
import MapPicker from "../explore/MapPicker";

const NewComplaint = ({
  formData,
  setFormData,
  submitLoading,
  setSubmitLoading,
  message,
  setMessage,
  fetchComplaints,
  setActiveTab,
}) => {
  const fileInputRef = useRef(null);
  const [showNearby, setShowNearby] = useState(null);
  const [pendingData, setPendingData] = useState(null);

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleFile = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFormData((prev) => ({ ...prev, imageUrl: e.target.files[0] }));
    }
  };

  const reverseGeocode = async (lat, lng) => {
    try {
      const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`);
      const data = await res.json();
      const city = data?.address?.city || data?.address?.town || data?.address?.village || data?.address?.municipality || "";
      const state = data?.address?.state || "";
      const landmark = data?.address?.road || data?.address?.suburb || "";
      setFormData((prev) => ({ ...prev, city, state, landmark: prev.landmark || landmark }));
    } catch {}
  };

  useEffect(() => {
    if (!formData.latitude || !formData.longitude) return;
    const t = setTimeout(() => reverseGeocode(formData.latitude, formData.longitude), 800);
    return () => clearTimeout(t);
  }, [formData.latitude, formData.longitude]);

  const getLocation = () => {
    if (!navigator.geolocation) {
      setMessage({ type: "error", text: "Geolocation not supported" });
      return;
    }
    setMessage({ type: "success", text: "Fetching location..." });
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude.toFixed(6);
        const lng = pos.coords.longitude.toFixed(6);
        setFormData((prev) => ({ ...prev, latitude: lat, longitude: lng }));
        await reverseGeocode(lat, lng);
        setMessage({ type: "success", text: "Location fetched. Drag map to adjust pin." });
      },
      () => setMessage({ type: "error", text: "Unable to fetch location" })
    );
  };

  const handlePickerChange = (lat, lng) => {
    setFormData((prev) => {
      if (prev.latitude === lat && prev.longitude === lng) return prev;
      return { ...prev, latitude: lat, longitude: lng };
    });
  };

  const doSubmit = async (data) => {
    setSubmitLoading(true);
    setMessage({ type: "", text: "" });
    try {
      const res = await API.post("/complaint/create-complaint", data, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      if (res.data.success) {
        setMessage({ type: "success", text: "Complaint submitted successfully." });
        setFormData({ description: "", city: "", state: "", landmark: "", latitude: "", longitude: "", imageUrl: null });
        if (fileInputRef.current) fileInputRef.current.value = "";
        fetchComplaints();
        setTimeout(() => setActiveTab("overview"), 1000);
      }
    } catch (error) {
      const msg = error.response?.data?.message || "Submission failed. Please try again.";
      setMessage({ type: "error", text: msg });
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.latitude || !formData.longitude) {
      setMessage({ type: "error", text: "Please fetch your location or drag map to set pin." });
      return;
    }
    const data = new FormData();
    Object.entries(formData).forEach(([key, val]) => {
      if (val) data.append(key, val);
    });
    // Nearby check (Android: 500m radius) - show dialog if duplicates nearby
    try {
      const res = await API.get(`/complaint/nearby?lat=${formData.latitude}&lng=${formData.longitude}&radius=500`);
      if (res.data?.complaints?.length > 0) {
        setPendingData(data);
        setShowNearby(res.data.complaints);
        return;
      }
    } catch {}
    await doSubmit(data);
  };

  return (
    <div className="ui-card space-y-4">
      <div>
        <h1 className="ui-title">New complaint</h1>
        <p className="ui-subtitle">Provide accurate details for faster resolution.</p>
      </div>

      {message.text && (
        <div className={`ui-alert ${message.type === "success" ? "ui-alert-success" : "ui-alert-error"}`}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="ui-label" htmlFor="description">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            className="ui-textarea"
            placeholder="Describe the issue clearly."
            required
          />
        </div>

        <div className="space-y-3">
          <div className="ui-card-muted">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-[color:var(--ui-text-muted)]">Drag map to adjust pin, or use current location.</p>
              <button type="button" onClick={getLocation} className="ui-btn ui-btn-secondary">
                <span className="material-symbols-outlined text-[18px]">my_location</span> Use current location
              </button>
            </div>
            {formData.latitude && formData.longitude && (
              <div className="mt-3 space-y-1">
                <p className="text-sm text-[color:var(--ui-text-muted)]">
                  Coordinates: {formData.latitude}, {formData.longitude}
                </p>
                {(formData.city || formData.state || formData.landmark) && (
                  <p className="text-sm text-[color:var(--ui-text-muted)]">
                    Address: {[formData.landmark, formData.city, formData.state].filter(Boolean).join(", ")}
                  </p>
                )}
              </div>
            )}
          </div>
          <MapPicker latitude={formData.latitude} longitude={formData.longitude} onChange={handlePickerChange} />
          <div className="grid gap-3 sm:grid-cols-3">
            <input name="city" value={formData.city} onChange={handleChange} placeholder="City" className="ui-input" />
            <input name="state" value={formData.state} onChange={handleChange} placeholder="State" className="ui-input" />
            <input name="landmark" value={formData.landmark} onChange={handleChange} placeholder="Landmark" className="ui-input" />
          </div>
        </div>

        <div>
          <label className="ui-label" htmlFor="image-upload">
            Evidence image
          </label>
          <input
            id="image-upload"
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleFile}
            className="ui-input file:mr-3 file:rounded file:border-0 file:bg-[color:var(--ui-surface-muted)] file:px-3 file:py-2 file:text-sm"
          />
          {formData.imageUrl && (
            <p className="mt-2 text-sm text-[color:var(--ui-text-muted)]">Selected: {formData.imageUrl.name}</p>
          )}
        </div>

        <div className="flex flex-wrap gap-3">
          <button type="submit" disabled={submitLoading} className="ui-btn ui-btn-primary">
            {submitLoading ? "Submitting..." : "Submit complaint"}
          </button>
          <button type="button" onClick={() => setActiveTab("overview")} className="ui-btn ui-btn-secondary">
            Cancel
          </button>
        </div>
      </form>

      {showNearby && (
        <div className="fixed inset-0 z-[60] flex items-end justify-center bg-black/40 p-3 md:items-center">
          <div className="w-full max-w-lg rounded-xl bg-[color:var(--ui-surface)] p-4 shadow-xl">
            <h3 className="text-sm font-bold">Nearby issues found ({showNearby.length} within 500m)</h3>
            <p className="mt-1 text-xs text-[color:var(--ui-text-muted)]">These issues are very close to your pin. Check if yours is duplicate before submitting.</p>
            <div className="mt-3 max-h-[200px] space-y-2 overflow-auto">
              {showNearby.map((r) => (
                <div key={r._id || r.category + r.distanceMeters} className="rounded-lg border border-[color:var(--ui-border)] p-2">
                  <p className="text-xs font-semibold capitalize">{r.category?.replace(/_/g, " ")} • {r.status}</p>
                  <p className="text-xs text-[color:var(--ui-text-muted)]">{r.description?.slice(0, 80)}</p>
                  <p className="text-xs font-medium">{r.distanceMeters}m away</p>
                </div>
              ))}
            </div>
            <div className="mt-4 flex gap-2">
              <button onClick={() => { setShowNearby(null); setPendingData(null); }} className="ui-btn ui-btn-secondary flex-1">
                Cancel
              </button>
              <button
                onClick={async () => {
                  const d = pendingData;
                  setShowNearby(null);
                  setPendingData(null);
                  await doSubmit(d);
                }}
                className="ui-btn ui-btn-primary flex-1"
              >
                Submit anyway
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default NewComplaint;
