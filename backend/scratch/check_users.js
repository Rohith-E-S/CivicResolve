
import mongoose from "mongoose";
import User from "../models/user.model.js";
import dotenv from "dotenv";
dotenv.config();

async function checkUsers() {
  await mongoose.connect(`${process.env.MONGODB_URL.replace(/\/$/, "")}/problemRegPortal`);
  const users = await User.find({});
  console.log("Total Users:", users.length);
  users.forEach(u => {
    console.log(`- ${u.fullName} (${u.email}), ID: ${u._id}, isAdmin: ${u.isAdmin}, District: ${u.homeDistrict}`);
  });
  await mongoose.disconnect();
}

checkUsers().catch(console.error);
