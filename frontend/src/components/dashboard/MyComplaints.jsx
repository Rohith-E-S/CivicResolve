import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import API from "../../api/axios";
import ComplaintCard from "./ComplaintCard";

const MyComplaints = ({ setActiveTab }) => {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [filter, setFilter] = useState("all");

  const fetchComplaints = useCallback(async () => {
    setLoading(true);
    try {
      const res = await API.get(`/complaint/my-list?page=${page}&limit=10&status=${filter}`);
      if (res.data.success) {
        setComplaints(res.data.complaints);
        setTotalPages(res.data.pagination.totalPages);
      }
    } catch (error) {
      console.error("Error fetching complaints:", error);
    } finally {
      setLoading(false);
    }
  }, [filter, page]);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const handleFilterChange = (e) => {
    setFilter(e.target.value);
    setPage(1);
  };

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h3 className="ui-mono text-xs">My ledger • {complaints.length} entries</h3>
        <div className="flex gap-2">
          <select value={filter} onChange={handleFilterChange} className="ui-select sm:w-56 !min-h-[2.4rem]">
            <option value="all">All statutes</option>
            <option value="new">New • red dot</option>
            <option value="in progress">In progress • amber</option>
            <option value="resolved">Resolved • green</option>
          </select>
          <Link to="/explore" className="ui-btn ui-btn-secondary hidden sm:inline-flex">
            Map
          </Link>
        </div>
      </div>

      {loading ? (
        <p className="ui-empty">Indexing ledger…</p>
      ) : complaints.length > 0 ? (
        <div className="space-y-2">
          {complaints.map((c) => (
            <Link key={c._id} to={`/complaint-overview/${c._id}`} className="block">
              <ComplaintCard complaint={c} />
            </Link>
          ))}
        </div>
      ) : (
        <div className="ui-card text-center py-8">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full border-2 border-dashed border-[color:var(--ui-border)]">
            <span className="material-symbols-outlined text-[20px] text-[color:var(--ui-text-muted)]">add_location</span>
          </div>
          <p className="mt-3 font-semibold" style={{ fontFamily: "Instrument Serif, serif" }}>No dots yet</p>
          <p className="mt-1 text-sm text-[color:var(--ui-text-muted)]">Your first report will appear as a red dot on the district map. It takes 30 seconds.</p>
          <button onClick={() => setActiveTab("new-complaint")} className="ui-btn ui-btn-accent mt-4">
            Create first report
          </button>
        </div>
      )}

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

export default MyComplaints;
