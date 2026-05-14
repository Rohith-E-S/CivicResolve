import mongoose from "mongoose";
import dotenv from "dotenv";
import User from "../models/user.model.js";

dotenv.config();

const getAdminIds = async () => {
  try {
    const mongoUrl = process.env.MONGODB_URL.replace(/\/$/, '');
    await mongoose.connect(`${mongoUrl}/problemRegPortal`);
    
    const admins = await User.find({ isAdmin: true });
    admins.forEach(a => console.log(`${a.fullName}: ${a._id}`));
    process.exit(0);
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
};

getAdminIds();
