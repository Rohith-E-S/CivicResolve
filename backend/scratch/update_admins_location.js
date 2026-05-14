import mongoose from "mongoose";
import dotenv from "dotenv";
import User from "../models/user.model.js";

dotenv.config();

const updateAdmins = async () => {
  try {
    const mongoUrl = process.env.MONGODB_URL.replace(/\/$/, '');
    await mongoose.connect(`${mongoUrl}/problemRegPortal`);
    console.log("Connected to MongoDB");

    const result = await User.updateMany(
      { isAdmin: true },
      { $set: { homeDistrict: "Jalandhar Division" } }
    );

    console.log(`Successfully updated ${result.modifiedCount} admin users to Jalandhar Division.`);
    process.exit(0);
  } catch (err) {
    console.error("Error updating admins:", err.message);
    process.exit(1);
  }
};

updateAdmins();
