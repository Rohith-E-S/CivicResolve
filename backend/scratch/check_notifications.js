
import mongoose from "mongoose";
import Notification from "../models/notification.model.js";
import dotenv from "dotenv";
dotenv.config();

async function checkNotifications() {
  await mongoose.connect(`${process.env.MONGODB_URL.replace(/\/$/, "")}/problemRegPortal`);
  const notifications = await Notification.find({ type: "disputed" }).sort({ createdAt: -1 }).limit(5);
  console.log("Recent Dispute Notifications:", notifications.length);
  notifications.forEach(n => {
    console.log(`- To: ${n.userId}, Title: ${n.title}, Message: ${n.message}, CreatedAt: ${n.createdAt}`);
  });
  await mongoose.disconnect();
}

checkNotifications().catch(console.error);
