
import mongoose from "mongoose";
import Complaint from "../models/complaint.model.js";
import dotenv from "dotenv";
dotenv.config();

async function checkComplaints() {
  await mongoose.connect(`${process.env.MONGODB_URL.replace(/\/$/, "")}/problemRegPortal`);
  const complaints = await Complaint.find({}).sort({ createdAt: -1 }).limit(5);
  console.log("Recent Complaints:", complaints.length);
  complaints.forEach(c => {
    console.log(`- ${c.category} in ${c.city}, Status: ${c.status}, ID: ${c._id}`);
  });
  await mongoose.disconnect();
}

checkComplaints().catch(console.error);
