import User from "../models/user.model.js";
import { verifySession, sessionRoom } from "../services/authSecurity.js";

function handshakeToken(socket) {
    const cookies = socket.handshake.headers?.cookie;
    const cookie = typeof cookies === "string" && cookies.split(";").map((part) => part.trim()).find((part) => part.startsWith("token="));
    return cookie ? decodeURIComponent(cookie.slice(6)) : socket.handshake.auth?.token;
}

export const socketAuth = async (socket, next) => {
    try {
        const token = handshakeToken(socket);
        const authenticate = async () => {
            const decoded = verifySession(token);
            const user = await User.findById(decoded._id).select("+sessionVersion");
            if (!user || (user.sessionVersion ?? 0) !== decoded.sessionVersion) throw new Error("Session revoked");
            socket.user = user;
            return decoded;
        };
        const decoded = await authenticate();
        // Auth-owned room cannot be chosen by a client. Includes sockets that
        // never emit join_room; revocation uses the adapter across all workers.
        await socket.join(sessionRoom(decoded._id));
        // Close the authentication/room-registration revocation race.
        await authenticate();
        socket.use(async (_packet, packetNext) => {
            try { await authenticate(); packetNext(); }
            catch { socket.disconnect(true); }
        });
        // Idle sockets must also stop receiving events when their JWT expires.
        const timer = setTimeout(() => socket.disconnect(true), Math.max(1, decoded.exp * 1000 - Date.now()));
        timer.unref?.();
        socket.once("disconnect", () => clearTimeout(timer));
        next();
    } catch {
        next(new Error("Authentication error: Invalid or expired session"));
    }
};
