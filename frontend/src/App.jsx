import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Signup from "./pages/Signup";
import { ThemeProvider } from "./context/ThemeContext";
import Login from "./pages/Login";
import LandingPage from "./pages/LandingPage";
import Dashboard from "./pages/Dashboard";
import AdminDashboard from "./pages/AdminDashboard";
import ProtectedRoute from "./components/ProtectedRoute";
import "./App.css";
import ComplaintOverviewPage from "./pages/ComplaintOverviewPage";
import AdminComplaintOverviewPage from "./pages/AdminComplaintOverviewPage";
import OtpVerify from "./pages/OtpVerify";
import ComplaintChat from "./pages/ComplaintChat";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPasswordPage from "./pages/ResetPasswordPage";
import ThemeToggle from "./components/ThemeToggle";
import Explore from "./pages/Explore";
import MapView from "./pages/MapView";
import CommandPalette from "./components/CommandPalette";

function App() {
  return (
    <Router>
      <ThemeProvider>
        <CommandPalette />
        <ThemeToggle className="fixed bottom-3 right-3 z-[80]" />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin-dashboard"
            element={
              <ProtectedRoute>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/complaint-overview/:id"
            element={
              <ProtectedRoute>
                <ComplaintOverviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/complaint-overview/:id"
            element={
              <ProtectedRoute>
                <AdminComplaintOverviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/complaint/:complaintId/chat"
            element={
              <ProtectedRoute>
                <ComplaintChat />
              </ProtectedRoute>
            }
          />
          <Route path="/otp-verify" element={<OtpVerify />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password/:id" element={<ResetPasswordPage />} />
          <Route
            path="/explore"
            element={
              <ProtectedRoute>
                <Explore />
              </ProtectedRoute>
            }
          />
          <Route
            path="/map"
            element={
              <ProtectedRoute>
                <MapView />
              </ProtectedRoute>
            }
          />
        </Routes>
      </ThemeProvider>
    </Router>
  );
}

export default App;
