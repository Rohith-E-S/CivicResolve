import mongoose from "mongoose";
import dotenv from "dotenv";
import bcrypt from "bcrypt";
import User from "../models/user.model.js";
import Complaint from "../models/complaint.model.js";

dotenv.config();

let mongoUrl = process.env.MONGODB_URL.trim().replace(/\/$/, "");
if (mongoUrl.includes("?")) {
  const q = mongoUrl.indexOf("?");
  mongoUrl = mongoUrl.slice(0, q).replace(/\/$/, "") + "/problemRegPortal?" + mongoUrl.slice(q + 1);
} else mongoUrl += "/problemRegPortal";

await mongoose.connect(mongoUrl);
console.log("Connected", mongoose.connection.name);

// Update existing primary user homeDistrict for My District
const existing = await User.findOne({ email: "vidyaarohith@protonmail.com" });
if (existing) {
  existing.homeDistrict = "Gobi";
  existing.address = "Gobi, Erode, Tamil Nadu";
  existing.lastLocation = { type: "Point", coordinates: [77.4244, 11.4513] };
  existing.lastLocationUpdatedAt = new Date();
  await existing.save();
  console.log("Updated primary user to Gobi");
}

const hash = await bcrypt.hash("Test123!", 10);

const syntheticUsers = [
  { fullName: "Priya Menon", email: "priya.menon@test.com", homeDistrict: "Erode", address: "Erode, Tamil Nadu", isAdmin: false, city: "erode" },
  { fullName: "Arjun Kumar", email: "arjun.kumar@test.com", homeDistrict: "Gobi", address: "Gobi, Erode", isAdmin: false, city: "gobi" },
  { fullName: "Admin Gobi", email: "admin.gobi@civic.test", homeDistrict: "Gobi", address: "Gobi Division", isAdmin: true, city: "gobi" },
  { fullName: "Kavya Reddy", email: "kavya.reddy@test.com", homeDistrict: "Coimbatore", address: "Coimbatore, Tamil Nadu", isAdmin: false, city: "coimbatore" },
];

for (const u of syntheticUsers) {
  let user = await User.findOne({ email: u.email });
  if (!user) {
    user = await User.create({
      fullName: u.fullName,
      email: u.email,
      password: hash,
      address: u.address,
      homeDistrict: u.homeDistrict,
      isAdmin: u.isAdmin,
      isVerified: true,
      lastLocation: {
        type: "Point",
        coordinates: u.city === "gobi" ? [77.4244 + (Math.random() - 0.5) * 0.04, 11.4513 + (Math.random() - 0.5) * 0.04] : u.city === "erode" ? [77.7172 + (Math.random() - 0.5) * 0.04, 11.341 + (Math.random() - 0.5) * 0.04] : [76.9558 + (Math.random() - 0.5) * 0.04, 11.0168 + (Math.random() - 0.5) * 0.04],
      },
      lastLocationUpdatedAt: new Date(),
    });
    console.log("Created", u.email);
  } else {
    console.log("Exists", u.email);
  }
}

const allUsers = await User.find({});
const userIds = allUsers.map((u) => u._id);

const cities = [
  { city: "gobi", state: "tamil nadu", landmark: "Gandhi Nagar", lat: 11.4513, lng: 77.4244 },
  { city: "erode", state: "tamil nadu", landmark: "Perundurai Road", lat: 11.341, lng: 77.7172 },
  { city: "coimbatore", state: "tamil nadu", landmark: "Gandhipuram", lat: 11.0168, lng: 76.9558 },
  { city: "chennai", state: "tamil nadu", landmark: "T Nagar", lat: 13.0827, lng: 80.2707 },
  { city: "salem", state: "tamil nadu", landmark: "Five Roads", lat: 11.6643, lng: 78.146 },
  { city: "madurai", state: "tamil nadu", landmark: "Mattuthavani", lat: 9.9252, lng: 78.1198 },
];

const categories = ["road_damage", "garbage_issue", "water_leakage", "electricity_issue", "tree_fallen", "accident", "fire", "drainage_problem", "noise_issue", "other"];
const statuses = ["new", "under_review", "in_progress", "pending_verification", "disputed", "resolved", "re_opened"];
const descriptions = [
  "Pothole near main road causing traffic jam during school hours",
  "Garbage pile not collected for 3 days near market area",
  "Water leakage from main pipeline flooding footpath",
  "Streetlight not working for a week, area dark at night",
  "Fallen tree blocking pedestrian pathway after heavy rain",
  "Open drainage overflowing near residential area",
  "Loud construction noise beyond permitted hours",
  "Damaged road divider creating accident risk",
  "Fire hazard due to exposed electrical wires",
  "Stagnant water causing mosquito breeding near school",
];

await Complaint.deleteMany({ description: { $regex: /^synthetic/i } });
console.log("Cleared old synthetic");

const now = Date.now();
for (let i = 0; i < 32; i++) {
  const cityInfo = cities[i % cities.length];
  // Bias 60% to gobi/erode for My District
  const biasedCity = i < 18 ? cities[i % 2] : cityInfo;
  const category = categories[Math.floor(Math.random() * categories.length)];
  const status = statuses[Math.floor(Math.random() * statuses.length)];
  const userId = userIds[Math.floor(Math.random() * userIds.length)];
  const offsetLat = (Math.random() - 0.5) * 0.08;
  const offsetLng = (Math.random() - 0.5) * 0.08;
  const lat = (biasedCity.lat + offsetLat).toFixed(6);
  const lng = (biasedCity.lng + offsetLng).toFixed(6);
  const desc = `Synthetic ${i + 1}: ${descriptions[i % descriptions.length]} (${biasedCity.city})`;
  const createdAt = new Date(now - Math.floor(Math.random() * 14 * 24 * 60 * 60 * 1000));

  const c = await Complaint.create({
    user: userId,
    description: desc.toLowerCase(),
    latitude: lat,
    longitude: lng,
    city: biasedCity.city,
    state: biasedCity.state,
    landmark: biasedCity.landmark.toLowerCase() + ` ${i + 1}`,
    beforeImageUrl: `https://picsum.photos/seed/civic${i}/400/300`.toLowerCase(),
    afterImageUrl: ["resolved", "pending_verification"].includes(status) ? `https://picsum.photos/seed/after${i}/400/300`.toLowerCase() : undefined,
    category,
    status,
    location: { type: "Point", coordinates: [parseFloat(lng), parseFloat(lat)] },
    timestamps: { reported: createdAt },
    rating: status === "resolved" && Math.random() > 0.5 ? Math.floor(Math.random() * 5) + 1 : 0,
    supportCount: Math.floor(Math.random() * 8),
    supporters: [],
    createdAt,
    updatedAt: createdAt,
  });
  // Set timestamps for status progression
  if (status !== "new") c.timestamps.underReview = new Date(createdAt.getTime() + 3600000);
  if (["in_progress", "pending_verification", "resolved", "re_opened", "disputed"].includes(status)) c.timestamps.inProgress = new Date(createdAt.getTime() + 7200000);
  if (["pending_verification", "resolved"].includes(status)) c.timestamps.pendingVerification = new Date(createdAt.getTime() + 10800000);
  if (status === "resolved") c.timestamps.resolved = new Date(createdAt.getTime() + 14400000);
  await c.save();
}

console.log("Seeded 32 complaints");
console.log("Counts by city:", await Complaint.aggregate([{ $group: { _id: "$city", count: { $sum: 1 } } }]));
console.log("Counts by status:", await Complaint.aggregate([{ $group: { _id: "$status", count: { $sum: 1 } } }]));
console.log("Counts by category:", await Complaint.aggregate([{ $group: { _id: "$category", count: { $sum: 1 } } }]));

await mongoose.disconnect();
process.exit(0);
