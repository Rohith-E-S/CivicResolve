
import mongoose from "mongoose";
import User from "../models/user.model.js";
import dotenv from "dotenv";
dotenv.config();

async function checkAdmins() {
  await mongoose.connect(process.env.MONGODB_URL);
  const admins = await User.find({ isAdmin: true });
  console.log("Admins Found:", admins.length);
  admins.forEach(a => {
    console.log(`- ${a.fullName} (${a.email}), ID: ${a._id}, District: ${a.homeDistrict}`);
  });
  await mongoose.disconnect();
}

checkAdmins().catch(console.error);
