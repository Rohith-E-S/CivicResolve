import { io } from "socket.io-client";
import { onSessionEnded } from "./session.js";

// Use the same origin as REST (Vite proxies /socket.io in development).
// An override must be same-site and permitted by the server's credentialed CORS.
const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || window.location.origin;

const socket = io(SOCKET_URL, {
  withCredentials: true,
  autoConnect: false,
});

// HttpOnly cookies are sent by the browser during the handshake.
export const connectSocket = () => socket.connect();
export const disconnectSocket = () => {
  socket.disconnect();
  // Never replay queued chat messages under a later login.
  socket.sendBuffer = [];
};

const unsubscribe = onSessionEnded(disconnectSocket);
if (import.meta.hot) import.meta.hot.dispose(unsubscribe);

export default socket;
