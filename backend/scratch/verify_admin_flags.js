import mongoose from "mongoose";
import dotenv from "dotenv";
import User from "../models/user.model.js";

dotenv.config();

const checkAdminFlags = async () => {
  try {
    const mongoUrl = process.env.MONGODB_URL.replace(/\/$/, '');
    await mongoose.connect(`${mongoUrl}/problemRegPortal`);
    
    const admins = await User.find({ isAdmin: true });
    console.log(`Found ${admins.length} admins in DB`);
    admins.forEach(a => console.log(`- ${a.fullName} (${a.email}), isAdmin: ${a.isAdmin}, homeDistrict: ${a.homeDistrict}`));
    process.exit(0);
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
};

checkAdminFlags();
