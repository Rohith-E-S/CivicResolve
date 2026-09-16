import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import API from "../api/axios";
import { PASSWORD_POLICY, passwordError } from "../utils/authContracts";
import { clearSession } from "../utils/session";

const ResetPasswordPage = () => {
  const { id } = useParams();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const navigate = useNavigate();

  const handleReset = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    const validationError = passwordError(password);
    if (validationError) {
      setError(validationError);
      return;
    }
    setLoading(true);
    try {
      const res = await API.post("/auth/reset-password", { password, token: id });
      if (res.data.success) {
        clearSession();
        setMessage("Password reset successfully. Redirecting to sign in...");
        setTimeout(() => navigate("/login"), 2000);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Error resetting password. Link may be expired.");
      console.error("Reset password error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ui-page">
      <main className="ui-container py-10 sm:py-16">
        <div className="mx-auto max-w-xl ui-card">
          <h1 className="ui-title">Set a new password</h1>
          <p className="ui-subtitle">{PASSWORD_POLICY}</p>

          {message && <div className="ui-alert ui-alert-success mt-5">{message}</div>}
          {error && <div className="ui-alert ui-alert-error mt-5">{error}</div>}

          <form onSubmit={handleReset} className="mt-6 space-y-4">
            <div>
              <label className="ui-label" htmlFor="new-password">
                New password
              </label>
              <input
                id="new-password"
                minLength={8}
                autoComplete="new-password"
                type="password"
                className="ui-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="ui-label" htmlFor="confirm-password">
                Confirm password
              </label>
              <input
                id="confirm-password"
                type="password"
                className="ui-input"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" disabled={loading} className="ui-btn ui-btn-primary w-full">
              {loading ? "Resetting..." : "Reset password"}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link to="/login" className="text-sm font-medium text-[color:var(--ui-accent)] hover:underline">
              Back to sign in
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
};

export default ResetPasswordPage;
