import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import API from "../../api/axios";
import { getStatusBadgeClass } from "../../utils/ui";

const AllComplaints = () => {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [filter, setFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 400);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    setPage(1);
  }, [debouncedSearch, filter]);

  const fetchComplaints = useCallback(async () => {
    setLoading(true);
    try {
      const res = await API.get(
        `/complaint/admin/list?page=${page}&limit=10&status=${filter}&search=${debouncedSearch}`
      );
      if (res.data.success) {
        setComplaints(res.data.complaints);
        setTotalPages(res.data.pagination.totalPages);
      }
    } catch (error) {
      console.error("Error fetching complaints:", error);
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, filter, page]);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  return (
    <div className="space-y-4">
      <section className="ui-card">
        <h2 className="ui-title">All complaints</h2>
        <p className="ui-subtitle">Review, search, and open complaint details.</p>
      </section>

      <section className="ui-card-muted grid gap-3 md:grid-cols-2">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="ui-input"
          placeholder="Search by complaint details"
          type="text"
        />
        <select value={filter} onChange={(e) => setFilter(e.target.value)} className="ui-select">
          <option value="all">All status</option>
          <option value="new">New</option>
          <option value="in_progress">In progress</option>
          <option value="resolved">Resolved</option>
        </select>
      </section>

      <section className="ui-card overflow-x-auto p-0">
        <table className="w-full min-w-[760px] border-collapse">
          <thead>
            <tr className="border-b border-[color:var(--ui-border)] bg-[color:var(--ui-surface-muted)]">
              <th className="px-4 py-3 text-left text-sm font-semibold">ID</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Citizen</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Description</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Location</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Date</th>
              <th className="px-4 py-3 text-left text-sm font-semibold">Status</th>
              <th className="px-4 py-3 text-right text-sm font-semibold">Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="7" className="px-4 py-8 text-center text-[color:var(--ui-text-muted)]">
                  Loading complaints...
                </td>
              </tr>
            ) : complaints.length > 0 ? (
              complaints.map((c) => (
                <tr key={c._id} className="border-b border-[color:var(--ui-border)]">
                  <td className="px-4 py-3 text-sm font-medium">#{c._id.slice(-6).toUpperCase()}</td>
                  <td className="px-4 py-3 text-sm">{c.user?.fullName || "Citizen"}</td>
                  <td className="px-4 py-3 text-sm">{c.description || "-"}</td>
                  <td className="px-4 py-3 text-sm">{c.city || "-"}</td>
                  <td className="px-4 py-3 text-sm">{new Date(c.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3">
                    <span className={getStatusBadgeClass(c.status)}>{c.status}</span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link
                      to={`/admin/complaint-overview/${c._id}`}
                      className="text-sm font-semibold text-[color:var(--ui-accent)] hover:underline"
                    >
                      Open
                    </Link>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" className="px-4 py-8 text-center text-[color:var(--ui-text-muted)]">
                  No complaints found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button
            onClick={() => setPage((prev) => Math.max(1, prev - 1))}
            disabled={page === 1}
            className="ui-btn ui-btn-secondary"
          >
            Previous
          </button>
          <span className="text-sm text-[color:var(--ui-text-muted)]">
            Page {page} of {totalPages}
          </span>
          <button
            onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
            disabled={page === totalPages}
            className="ui-btn ui-btn-secondary"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default AllComplaints;
