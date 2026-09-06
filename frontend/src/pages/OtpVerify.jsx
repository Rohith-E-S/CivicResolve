import { useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import API from "../api/axios";

const OtpVerify = () => {
  const [otpValues, setOtpValues] = useState(["", "", "", "", "", ""]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const inputRefs = useRef([]);
  const { state } = useLocation();
  const navigate = useNavigate();
  const email = state?.email;

  const handleChange = (index, e) => {
    const value = e.target.value;
    if (Number.isNaN(Number(value))) return;

    const next = [...otpValues];
    next[index] = value.substring(value.length - 1);
    setOtpValues(next);

    if (value && index < 5) inputRefs.current[index + 1]?.focus();
  };

  const handleKeyDown = (index, e) => {
    if (e.key === "Backspace" && !otpValues[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setError("");
    const otp = otpValues.join("");
    if (otp.length < 6) {
      setError("Please enter the complete 6-digit OTP.");
      return;
    }

    setLoading(true);
    try {
      const res = await API.post("/auth/verify-otp", { email, otp });
      if (res.data.success) {
        const pending = JSON.parse(localStorage.getItem("pendingSignup"));
        if (!pending) {
          setError("Signup session expired. Please register again.");
          navigate("/signup");
          return;
        }

        const account = await API.post("/auth/create-account", pending);
        localStorage.setItem("token", account.data.token);
        localStorage.setItem("userData", JSON.stringify(account.data.user));
        localStorage.removeItem("pendingSignup");
        navigate("/dashboard");
      }
    } catch (err) {
      setError(err.response?.data?.message || "Invalid OTP");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ui-page">
      <main className="ui-container py-10 sm:py-16">
        <div className="mx-auto max-w-xl ui-card relative overflow-hidden">
          <div className="absolute left-0 top-0 h-full w-1 bg-[color:var(--ui-success)]" />
          <p className="ui-mono text-[color:var(--ui-success)]">Step 2 of 2 • Check your inbox</p>
          <h1 className="ui-display text-3xl mt-2">Verify your email</h1>
          <p className="text-sm text-[color:var(--ui-text-muted)] mt-2">Enter the 6-digit code sent to {email || "your inbox"}. Valid for 5 minutes.</p>

          {error && <div className="ui-alert ui-alert-error mt-5">{error}</div>}

          <form onSubmit={handleVerify} className="mt-6 space-y-6">
            <div className="grid grid-cols-6 gap-2 sm:gap-3">
              {otpValues.map((val, index) => (
                <input
                  key={index}
                  ref={(el) => {
                    inputRefs.current[index] = el;
                  }}
                  value={val}
                  onChange={(e) => handleChange(index, e)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  className="ui-input !px-0 text-center text-xl font-semibold"
                  maxLength={1}
                  type="text"
                  inputMode="numeric"
                  required
                />
              ))}
            </div>

            <button type="submit" disabled={loading} className="ui-btn ui-btn-primary w-full">
              {loading ? "Verifying..." : "Verify account"}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link to="/signup" className="text-sm font-medium text-[color:var(--ui-accent)] hover:underline">
              Use a different email
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
};

export default OtpVerify;
