import { io } from "socket.io-client";

// Get token from cookies
const getToken = () => {
    const cookies = document.cookie.split(";");
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split("=");
        if (name === "token") {
            return value;
        }
    }
    return null;
};

// Same-origin by default (matches the REST baseURL strategy); the Vite dev
// proxy forwards /socket.io, and deployments can override via VITE_SOCKET_URL
const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || window.location.origin;

// Create socket instance
const socket = io(SOCKET_URL, {
    withCredentials: true,
    auth: {
        token: getToken(),
    },
    autoConnect: false,
});

// Function to connect with fresh token
export const connectSocket = () => {
    const token = getToken();
    if (token) {
        socket.auth = { token };
        socket.connect();
    }
};

// Function to disconnect
export const disconnectSocket = () => {
    socket.disconnect();
};

export default socket;
