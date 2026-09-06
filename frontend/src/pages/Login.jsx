import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../api/axios";
import { GoogleLogin } from "@react-oauth/google";
import { jwtDecode } from "jwt-decode";
import { useTheme } from "../context/ThemeContext";

const Login = () => {
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { theme } = useTheme();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await API.post("/auth/login", formData);
      if (res.data.success) {
        localStorage.setItem("token", res.data.token);
        localStorage.setItem("userData", JSON.stringify(res.data.user));
        navigate(res.data.user.isAdmin ? "/admin-dashboard" : "/dashboard");
      }
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong");
      console.error("Login error:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (response) => {
    try {
      const decoded = jwtDecode(response.credential);
      const res = await API.post("/auth/google-login", {
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

  return (
    <div className="ui-page">
      <main className="ui-container py-10 sm:py-16">
        <div className="mx-auto max-w-xl ui-card relative overflow-hidden">
          <div className="absolute left-0 top-0 h-full w-1 bg-[color:var(--ui-accent)]" />
          <p className="ui-mono text-[color:var(--ui-accent)]">Citizen access • Step 1 of 1</p>
          <h1 className="ui-display text-3xl mt-2">Sign in</h1>
          <p className="text-sm text-[color:var(--ui-text-muted)] mt-2">Access your ledger, chats, and verification duties.</p>

          {error && <div className="ui-alert ui-alert-error mt-5">{error}</div>}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <div>
              <label className="ui-label" htmlFor="email">
                Email
              </label>
              <input
                className="ui-input"
                id="email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="name@example.com"
                required
              />
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between gap-2">
                <label className="ui-label mb-0" htmlFor="password">
                  Password
                </label>
                <Link to="/forgot-password" className="text-sm text-[color:var(--ui-accent)] hover:underline">
                  Forgot password?
                </Link>
              </div>
              <input
                className="ui-input"
                id="password"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Enter your password"
                required
              />
            </div>

            <button type="submit" disabled={loading} className="ui-btn ui-btn-primary w-full">
              {loading ? "Signing in..." : "Sign in"}
            </button>
          </form>

          <div className="my-6 border-t border-[color:var(--ui-border)]" />

          <div className="flex justify-center">
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => setError("Google login failed")}
              theme={theme === "dark" ? "filled_black" : "outline"}
              shape="rectangular"
              text="signin_with"
              size="large"
              width="360"
            />
          </div>

          <p className="mt-6 text-center text-sm text-[color:var(--ui-text-muted)]">
            New here?{" "}
            <Link to="/signup" className="font-semibold text-[color:var(--ui-accent)] hover:underline">
              Create an account
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
};

export default Login;
