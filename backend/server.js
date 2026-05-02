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
import { socketAuth } from "./middleware/socket.auth.js";
import Message from "./models/message.model.js";

dotenv.config();

const app = express();
const server = createServer(app);
const PORT = process.env.PORT || 4000;

// Socket.IO setup with CORS
const io = new Server(server, {
  cors: {
    origin: ["http://localhost:5173", "http://localhost:5174"],
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

app.get("/", (req, res) => {
  res.send("Server running ");
});

app.use("/api/auth", authRouter);
app.use("/api/complaint", complaintRouter);
app.use("/api/messages", messageRouter);

// Set io to app to access in controllers
app.set("io", io);

// Socket.IO authentication middleware
io.use(socketAuth);

// Socket.IO event handlers
io.on("connection", (socket) => {
  console.log("User connected:", socket.user.fullName);

  // Join complaint room
  socket.on("joinComplaint", (complaintId) => {
    socket.join(complaintId);
    console.log(`${socket.user.fullName} joined room: ${complaintId}`);
  });

  // Handle sending messages
  socket.on("sendMessage", async (data) => {
    try {
      const { complaintId, toUser, message } = data;

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
