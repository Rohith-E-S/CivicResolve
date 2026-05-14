import mongoose from "mongoose";
import dotenv from "dotenv";
import Notification from "../models/notification.model.js";

dotenv.config();

const checkNotifications = async () => {
  try {
    const mongoUrl = process.env.MONGODB_URL.replace(/\/$/, '');
    await mongoose.connect(`${mongoUrl}/problemRegPortal`);
    
    const notifications = await Notification.find({ type: "disputed" }).sort({ createdAt: -1 }).limit(5);
    console.log(`Found ${notifications.length} recent dispute notifications`);
    notifications.forEach(n => {
      console.log(`- To: ${n.userId}, Title: ${n.title}, Created: ${n.createdAt}`);
    });
    process.exit(0);
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
};

checkNotifications();
