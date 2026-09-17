// Authentication lives in the HttpOnly cookie, never in browser token storage.
const LOGOUT_KEY = "civicresolve:logout";
const SESSION_ENDED = "civicresolve:session-ended";

export const removeLegacyToken = () => localStorage.removeItem("token");

export const saveSessionUser = (user) => {
  removeLegacyToken();
  localStorage.setItem("userData", JSON.stringify(user));
};

export const clearSession = () => {
  removeLegacyToken();
  localStorage.removeItem("userData");
  sessionStorage.removeItem("pendingSignup");
  // No secrets: a fresh marker also notifies tabs that have no cached user.
  localStorage.setItem(LOGOUT_KEY, crypto.randomUUID());
  window.dispatchEvent(new Event(SESSION_ENDED));
};

export const onSessionEnded = (callback) => {
  const local = () => callback(false);
  const remote = (event) => {
    if (event.key === LOGOUT_KEY && event.newValue) {
      removeLegacyToken();
      sessionStorage.removeItem("pendingSignup");
      callback(true);
    }
  };
  window.addEventListener(SESSION_ENDED, local);
  window.addEventListener("storage", remote);
  return () => {
    window.removeEventListener(SESSION_ENDED, local);
    window.removeEventListener("storage", remote);
  };
};
