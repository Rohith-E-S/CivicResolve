import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import API from "../api/axios";
import { getStatusBadgeClass } from "../utils/ui";
import MapLeaflet from "../components/dashboard/MapLeaflet";
import AppHeader from "../components/AppHeader";

const AdminComplaintOverviewPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [message, setMessage] = useState({ type: "", text: "" });

  const fetchComplaint = useCallback(async () => {
    try {
      const res = await API.get(`/complaint/get-complaint-data/${id}`);
      setComplaint(res.data.complaint);
      if (!res.data.isAdmin) navigate("/dashboard");
    } catch (error) {
      console.error("Error fetching complaint:", error);
    } finally {
      setLoading(false);
    }
  }, [id, navigate]);

  useEffect(() => {
    fetchComplaint();
  }, [fetchComplaint]);

  const updateComplaint = async (status, file) => {
    const formData = new FormData();
    if (status) formData.append("status", status);
    if (file) formData.append("imageUrl", file);
    setUpdating(true);
    try {
      const res = await API.post(`/complaint/update-complaint-status-upload-image/${id}`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      if (res.data.success) {
        setMessage({ type: "success", text: "Complaint updated successfully." });
        fetchComplaint();
      }
    } catch (error) {
      console.error("Error updating:", error);
      setMessage({ type: "error", text: "Update failed. Please try again." });
    } finally {
      setUpdating(false);
      setTimeout(() => setMessage({ type: "", text: "" }), 2500);
    }
  };

  if (loading) return <div className="ui-page ui-empty">Loading complaint...</div>;
  if (!complaint) return <div className="ui-page ui-empty">Complaint not found.</div>;

  return (
    <div className="ui-page">
      <AppHeader
        brandTo="/admin-dashboard"
        brandInitial="A"
        brandLabel="Complaint Register Portal"
        title="Manage complaint"
        subtitle={`ID #${complaint._id.slice(-8).toUpperCase()}`}
        actions={
          <>
            <Link to="/admin-dashboard" className="ui-btn ui-btn-secondary">
              Dashboard
            </Link>
            <Link to={`/complaint/${id}/chat`} className="ui-btn ui-btn-primary">
              Chat
            </Link>
          </>
        }
      />

      <main className="ui-container py-6 space-y-6">
        <section className="ui-card">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <span className={getStatusBadgeClass(complaint.status)}>{complaint.status}</span>
              <h1 className="ui-title mt-3">{complaint.description || "Reported issue"}</h1>
              <p className="ui-subtitle">
                #{complaint._id.slice(-8).toUpperCase()} • {complaint.city}, {complaint.state}
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              <select
                className="ui-select"
                value={complaint.status}
                onChange={(e) => {
                  const nextStatus = e.target.value;
                  if (nextStatus === "resolved" && !complaint.afterImageUrl) {
                    setMessage({ type: "error", text: "Upload an after image before resolving." });
                    return;
                  }
                  updateComplaint(nextStatus, null);
                }}
                disabled={updating}
              >
                <option value="new">New</option>
                <option value="under_review">Under review</option>
                <option value="in_progress">In progress</option>
                <option value="resolved">Resolved</option>
                <option value="re_opened">Re-opened</option>
              </select>

              <label className="ui-btn ui-btn-primary cursor-pointer">
                Upload after image
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => updateComplaint(null, e.target.files[0])}
                  disabled={updating}
                />
              </label>

              <Link to={`/complaint/${id}/chat`} className="ui-btn ui-btn-secondary">
                Open chat
              </Link>
            </div>
          </div>

          {message.text && (
            <div className={`mt-4 ui-alert ${message.type === "success" ? "ui-alert-success" : "ui-alert-error"}`}>
              {message.text}
            </div>
          )}
        </section>

        <section className="grid gap-6 lg:grid-cols-2">
          <article className="ui-card">
            <h2 className="text-lg font-semibold">Reporter details</h2>
            <div className="mt-3 space-y-2 text-sm">
              <p>
                <strong>Name:</strong> {complaint.user?.fullName || "Citizen"}
              </p>
              <p>
                <strong>Email:</strong> {complaint.user?.email || "-"}
              </p>
              <p>
                <strong>Landmark:</strong> {complaint.landmark || "-"}
              </p>
              <p>
                <strong>Rating:</strong> {complaint.rating > 0 ? `${complaint.rating}/5` : "Not rated"}
              </p>
            </div>
            <div className="mt-4">
              <MapLeaflet lat={complaint.latitude} lng={complaint.longitude} />
            </div>
          </article>

          <article className="ui-card">
            <h2 className="text-lg font-semibold">Evidence images</h2>
            <div className="mt-4 space-y-4">
              <div>
                <p className="mb-2 text-sm font-medium text-[color:var(--ui-text-muted)]">Before image</p>
                <div className="overflow-hidden rounded-lg border border-[color:var(--ui-border)] bg-[color:var(--ui-surface-muted)]">
                  {complaint.beforeImageUrl ? (
                    <img src={complaint.beforeImageUrl} alt="Before" className="h-56 w-full object-cover" />
                  ) : (
                    <p className="ui-empty">No image</p>
                  )}
                </div>
              </div>
              <div>
                <p className="mb-2 text-sm font-medium text-[color:var(--ui-text-muted)]">After image</p>
                <div className="overflow-hidden rounded-lg border border-[color:var(--ui-border)] bg-[color:var(--ui-surface-muted)]">
                  {complaint.afterImageUrl ? (
                    <img src={complaint.afterImageUrl} alt="After" className="h-56 w-full object-cover" />
                  ) : (
                    <p className="ui-empty">No image uploaded yet</p>
                  )}
                </div>
              </div>
            </div>
          </article>
        </section>
      </main>
    </div>
  );
};

export default AdminComplaintOverviewPage;
