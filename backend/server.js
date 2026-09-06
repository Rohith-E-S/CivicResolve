import express, { urlencoded } from "express";
import { createServer } from "http";
import { Server } from "socket.io";
import dotenv from "dotenv";
import cookieParser from "cookie-parser";
import cors from "cors";
import authRouter from "./routes/auth.route.js";
import { connectDB } from "./config/db.js";
import complaintRouter from "./routes/complaint.route.js";
import messageRouter from "./routes/message.route.js";
import notificationRouter from "./routes/notification.route.js";
import { socketAuth } from "./middleware/socket.auth.js";
import Message from "./models/message.model.js";
import Complaint from "./models/complaint.model.js";
import { notifyAdminComment, notifyAdminNewMessage } from "./services/notificationService.js";

dotenv.config();

const app = express();
const server = createServer(app);
const PORT = process.env.PORT || 4000;

// Socket.IO setup with CORS
const io = new Server(server, {
  cors: {
    origin: ["http://localhost:5173", "http://localhost:5174", "http://10.36.100.29:4000"],
    credentials: true,
  },
});

// CORS configuration
app.use(cors({
  origin: ["http://localhost:5173", "http://localhost:5174"],
  credentials: true
}));

app.use(express.json());
app.use(cookieParser());
app.use(urlencoded({ extended: true }));

app.use((req, res, next) => {
  console.log(`[Global] ${req.method} ${req.url}`);
  next();
});

app.get("/", (req, res) => {
  res.send("Server running ");
});

app.use("/api/auth", authRouter);
app.use("/api/complaint", complaintRouter);
app.use("/api/messages", messageRouter);
app.use("/api/notifications", notificationRouter);

// Set io to app to access in controllers
app.set("io", io);

// Socket.IO authentication middleware
io.use(socketAuth);

// Socket.IO event handlers
io.on("connection", (socket) => {
  console.log("User connected:", socket.user.fullName, "ID:", socket.id);

  // Join the authenticated user's personal notification room so targeted
  // emits work. The room is always derived from the socket identity —
  // clients cannot subscribe to other users' notification rooms.
  socket.on("join_room", () => {
    const room = socket.user._id.toString();
    socket.join(room);
    console.log(`[Socket] ${socket.user.fullName} joined notification room: ${room}`);
  });

  // Join complaint room — only the complaint owner or an admin may listen in
  socket.on("joinComplaint", async (complaintId) => {
    try {
      const complaint = await Complaint.findOne({
        _id: complaintId,
        isDeleted: { $ne: true },
      }).select("user");

      if (!complaint) {
        socket.emit("error", { message: "Complaint not found" });
        return;
      }

      if (!socket.user.isAdmin && complaint.user.toString() !== socket.user._id.toString()) {
        socket.emit("error", { message: "Not allowed to join this complaint room" });
        return;
      }

      socket.join(complaintId);
      console.log(`${socket.user.fullName} joined room: ${complaintId}`);
    } catch (error) {
      console.log("Error joining complaint room:", error.message);
    }
  });

  // Handle sending messages
  socket.on("sendMessage", async (data) => {
    try {
      const { complaintId, message } = data;

      const complaint = await Complaint.findById(complaintId);
      if (!complaint) {
        socket.emit("error", { message: "Complaint not found" });
        return;
      }

      // Resolve the recipient server-side. Clients used to send toUser
      // themselves, and citizens ended up addressing their own messages to
      // themselves, which broke seen-state and recipient logic permanently.
      let recipientId;
      if (socket.user.isAdmin) {
        // Admin -> User: address the complaint owner
        recipientId = complaint.user;
      } else {
        // User -> Admin: pick a district admin for this complaint's city
        const User = (await import("./models/user.model.js")).default;
        const allAdmins = await User.find({ isAdmin: true }).select("_id homeDistrict");
        const city = (complaint.city || "").toLowerCase();

        const districtAdmins = allAdmins.filter(admin => {
          if (!admin.homeDistrict || admin.homeDistrict.trim() === "") return true; // Global admin
          const d = admin.homeDistrict.toLowerCase().trim();
          const c = city.trim();

          // Exact / substring matches (both directions)
          if (d === "all" || d.includes(c) || c.includes(d)) return true;

          // Extract core district word (e.g. "Jalandhar" from "Jalandhar Division")
          const coreDistrict = d.split(/\s+/)[0];
          if (coreDistrict && c.includes(coreDistrict)) return true;

          return false;
        });

        // Fallback: if no district admin matched, address any admin
        recipientId = (districtAdmins[0] || allAdmins[0])?._id;
      }

      // Last resort (no admins registered): keep the client-supplied value
      const toUser = recipientId || data.toUser;
      if (!toUser) {
        socket.emit("error", { message: "No recipient available for this message" });
        return;
      }

      // Save message to database
      const newMessage = await Message.create({
        complaintId,
        fromUser: socket.user._id,
        toUser,
        message,
      });

      // Populate user info for response
      const populatedMessage = await Message.findById(newMessage._id)
        .populate("fromUser", "fullName email isAdmin")
        .populate("toUser", "fullName email isAdmin");

      // Broadcast to room
      io.to(complaintId).emit("newMessage", populatedMessage);

      // Notify recipient
      if (socket.user.isAdmin) {
        // Admin -> User: notify the complaint owner
        await notifyAdminComment(io, {
          complaintOwnerId: complaint.user.toString(),
          complaintId,
          adminName: socket.user.fullName,
          commentPreview: message,
          category: complaint.category
        });
      } else {
        const User = (await import("./models/user.model.js")).default;
        const allAdmins = await User.find({ isAdmin: true }).select("_id homeDistrict fullName");
        const city = (complaint.city || "").toLowerCase();

        const districtAdmins = allAdmins.filter(admin => {
          if (!admin.homeDistrict || admin.homeDistrict.trim() === "") return true; // Global admin
          const d = admin.homeDistrict.toLowerCase().trim();
          const c = city.trim();

          // Exact / substring matches (both directions)
          if (d === "all" || d.includes(c) || c.includes(d)) return true;

          // Extract core district word (e.g. "Jalandhar" from "Jalandhar Division")
          const coreDistrict = d.split(/\s+/)[0];
          if (coreDistrict && c.includes(coreDistrict)) return true;

          return false;
        });

        // Fallback: if no district admin matched, notify ALL admins so messages are never lost
        const adminsToNotify = districtAdmins.length > 0 ? districtAdmins : allAdmins;

        console.log(`[Chat] User message for complaint in ${complaint.city}. Notifying ${adminsToNotify.length} admins (matched: ${districtAdmins.length}).`);

        for (const admin of adminsToNotify) {
          await notifyAdminNewMessage(io, {
            adminId: admin._id,
            complaintId,
            userName: socket.user.fullName,
            messagePreview: message,
            category: complaint.category
          });
        }
      }
    } catch (error) {
      console.log("Error sending message:", error.message);
      socket.emit("error", { message: "Failed to send message" });
    }
  });

  // Handle marking messages as seen
  socket.on("markSeen", async (data) => {
    try {
      const { complaintId } = data;

      // Mark all messages sent TO current user as seen
      await Message.updateMany(
        {
          complaintId,
          toUser: socket.user._id,
          hasSeen: false
        },
        { hasSeen: true }
      );

      // Broadcast to room that messages were seen
      io.to(complaintId).emit("messagesSeen", {
        complaintId,
        seenBy: socket.user._id
      });
    } catch (error) {
      console.log("Error marking messages as seen:", error.message);
    }
  });

  socket.on("disconnect", () => {
    console.log("User disconnected:", socket.user.fullName);
  });
});

await connectDB();
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));
