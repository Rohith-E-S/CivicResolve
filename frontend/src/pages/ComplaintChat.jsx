import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import API from "../api/axios";
import socket, { connectSocket, disconnectSocket } from "../utils/socket";
import ChatBox from "../components/ChatBox";
import AppHeader from "../components/AppHeader";

const ComplaintChat = () => {
  const { complaintId } = useParams();
  const [messages, setMessages] = useState([]);
  const [complaint, setComplaint] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const complaintRes = await API.get(`/complaint/get-complaint-data/${complaintId}`);
        setComplaint(complaintRes.data.complaint);

        const userRes = await API.get("/auth/check-auth");
        setCurrentUser(userRes.data.user);

        const messagesRes = await API.get(`/messages/${complaintId}`);
        setMessages(messagesRes.data.messages || []);
      } catch (err) {
        console.error("Error fetching data:", err);
        setError("Failed to load chat data");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [complaintId]);

  useEffect(() => {
    if (!currentUser || !complaintId) return undefined;

    connectSocket();
    socket.emit("joinComplaint", complaintId);
    socket.emit("markSeen", { complaintId });

    socket.on("newMessage", (message) => {
      setMessages((prev) => [...prev, message]);
      socket.emit("markSeen", { complaintId });
    });

    socket.on("messagesSeen", ({ seenBy }) => {
      setMessages((prev) =>
        prev.map((msg) =>
          msg.fromUser._id === currentUser._id && msg.toUser?._id === seenBy
            ? { ...msg, hasSeen: true }
            : msg
        )
      );
    });

    return () => {
      socket.off("newMessage");
      socket.off("messagesSeen");
      disconnectSocket();
    };
  }, [currentUser, complaintId]);

  const handleSendMessage = (message) => {
    if (!complaint || !currentUser) return;
    // Recipient is resolved server-side (owner for admins, a district admin
    // for citizens) — never send identity data from the client
    socket.emit("sendMessage", { complaintId, message });
  };

  if (loading) return <div className="ui-page ui-empty">Loading chat...</div>;

  if (error) {
    return (
      <div className="ui-page ui-empty">
        <p>{error}</p>
        <Link to="/dashboard" className="text-[color:var(--ui-accent)] hover:underline">
          Back to dashboard
        </Link>
      </div>
    );
  }

  return (
    <div className="ui-page">
      <AppHeader
        brandTo={currentUser?.isAdmin ? "/admin-dashboard" : "/dashboard"}
        brandInitial="C"
        brandLabel="Complaint Register Portal"
        title="Complaint chat"
        subtitle={`ID #${complaintId.slice(-8).toUpperCase()}`}
        actions={
          <>
            <Link
              to={currentUser?.isAdmin ? "/admin-dashboard" : "/dashboard"}
              className="ui-btn ui-btn-secondary"
            >
              Dashboard
            </Link>
            <Link
              to={
                currentUser?.isAdmin
                  ? `/admin/complaint-overview/${complaintId}`
                  : `/complaint-overview/${complaintId}`
              }
              className="ui-btn ui-btn-primary"
            >
              Details
            </Link>
          </>
        }
      />

      <main className="ui-container py-6">
        <div className="h-[calc(100vh-10rem)] min-h-[480px]">
          <ChatBox
            messages={messages}
            onSendMessage={handleSendMessage}
            currentUserId={currentUser?._id}
            chattingWith={currentUser?.isAdmin ? complaint?.user?.fullName : "Admin"}
          />
        </div>
      </main>
    </div>
  );
};

export default ComplaintChat;
