import mongoose from "mongoose";
import dotenv from "dotenv";
import User from "../models/user.model.js";

dotenv.config();

const listAllUsers = async () => {
  try {
    await mongoose.connect(process.env.MONGODB_URL);
    const users = await User.find({});
    console.log(`Found ${users.length} total user(s)`);
    users.forEach(u => console.log(`- ${u.fullName} (${u.email}), isAdmin: ${u.isAdmin}, district: ${u.homeDistrict}`));
    process.exit(0);
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
};

listAllUsers();
