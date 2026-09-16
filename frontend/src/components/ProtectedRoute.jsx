import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import API from "../api/axios";
import { onSessionEnded, saveSessionUser } from "../utils/session";

const ProtectedRoute = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(null);

  useEffect(() => {
    let active = true;
    const unsubscribe = onSessionEnded(() => {
      active = false;
      setIsAuthenticated(false);
    });
    const checkAuth = async () => {
      try {
        const res = await API.get("/auth/check-auth");
        if (!active) return;
        if (res.data.success) {
          saveSessionUser(res.data.user);
          setIsAuthenticated(true);
        } else {
          setIsAuthenticated(false);
        }
      } catch (error) {
        console.error("Auth check failed:", error);
        setIsAuthenticated(false);
      }
    };

    checkAuth();
    return () => {
      active = false;
      unsubscribe();
    };
  }, []);

  if (isAuthenticated === null) {
    return (
      <div className="ui-page flex items-center justify-center p-6">
        <div className="ui-card w-full max-w-sm text-center">
          <div className="mx-auto mb-3 h-10 w-10 animate-spin rounded-full border-4 border-[color:var(--ui-border)] border-t-[color:var(--ui-accent)]" />
          <p className="text-sm text-[color:var(--ui-text-muted)]">Checking session...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

export default ProtectedRoute;
