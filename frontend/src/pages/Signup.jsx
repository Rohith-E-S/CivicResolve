import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../api/axios";
import { GoogleLogin } from "@react-oauth/google";
import { jwtDecode } from "jwt-decode";
import { useTheme } from "../context/ThemeContext";

const Signup = () => {
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    address: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { theme } = useTheme();

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleGoogleSuccess = async (response) => {
    try {
      const decoded = jwtDecode(response.credential);
      const res = await API.post("/auth/google-login", {
        credential: response.credential,
        email: decoded.email,
        fullName: decoded.name,
        profilePic: decoded.picture,
        googleId: decoded.sub,
      });

      localStorage.setItem("token", res.data.token);
      localStorage.setItem("userData", JSON.stringify(res.data.user));
      navigate(res.data.user.isAdmin ? "/admin-dashboard" : "/dashboard");
    } catch (err) {
      console.error(err);
      setError("Google login failed");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await API.post("/auth/send-otp", { email: formData.email });
      if (res.data.success) {
        // sessionStorage only: survives a refresh during the OTP step but is
        // wiped when the tab closes, so the plaintext password never lingers
        // in persistent storage
        sessionStorage.setItem("pendingSignup", JSON.stringify(formData));
        navigate("/otp-verify", { state: { email: formData.email } });
      }
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ui-page">
      <main className="ui-container py-10 sm:py-16">
        <div className="mx-auto max-w-xl ui-card relative overflow-hidden">
          <div className="absolute left-0 top-0 h-full w-1 bg-[color:var(--ui-accent-strong)]" />
          <p className="ui-mono text-[color:var(--ui-accent-strong)]">New ledger entry • Step 1 of 2</p>
          <h1 className="ui-display text-3xl mt-2">Create account</h1>
          <p className="text-sm text-[color:var(--ui-text-muted)] mt-2">Takes 45 seconds. You’ll verify your email next.</p>

          {error && <div className="ui-alert ui-alert-error mt-5">{error}</div>}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label className="ui-label" htmlFor="fullName">
                Full name
              </label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                className="ui-input"
                value={formData.fullName}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <label className="ui-label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                className="ui-input"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <label className="ui-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                className="ui-input"
                value={formData.password}
                onChange={handleChange}
                required
              />
            </div>

            <div>
              <label className="ui-label" htmlFor="address">
                Address
              </label>
              <textarea
                id="address"
                name="address"
                className="ui-textarea"
                value={formData.address}
                onChange={handleChange}
                required
              />
            </div>

            <button type="submit" disabled={loading} className="ui-btn ui-btn-primary w-full">
              {loading ? "Sending OTP..." : "Continue with email"}
            </button>
          </form>

          <div className="my-6 border-t border-[color:var(--ui-border)]" />

          <div className="flex justify-center">
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => setError("Google login failed")}
              theme={theme === "dark" ? "filled_black" : "outline"}
              shape="rectangular"
              text="signup_with"
              size="large"
              width="360"
            />
          </div>

          <p className="mt-6 text-center text-sm text-[color:var(--ui-text-muted)]">
            Already have an account?{" "}
            <Link to="/login" className="font-semibold text-[color:var(--ui-accent)] hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
};

export default Signup;
