import mongoose from "mongoose";
import dotenv from "dotenv";
import Complaint from "../models/complaint.model.js";

dotenv.config({ path: "backend/.env" });

async function backfill() {
  try {
    const mongoUrl = process.env.MONGODB_URL;
    if (!mongoUrl) {
      throw new Error("MONGODB_URL is not defined in .env");
    }
    await mongoose.connect(mongoUrl.endsWith('/') ? `${mongoUrl}problemRegPortal` : `${mongoUrl}/problemRegPortal`);
    console.log("Connected to MongoDB");

    const complaints = await Complaint.find({});
    console.log(`Found ${complaints.length} complaints to process`);

    let updatedCount = 0;

    for (const complaint of complaints) {
      let changed = false;

      // 1. Normalize status (space -> underscore)
      if (complaint.status === "in_progress") {
        complaint.status = "in_progress";
        changed = true;
      }
      if (complaint.status === "under_review") {
        complaint.status = "under_review";
        changed = true;
      }

      // 2. Initialize timestamps if missing
      if (!complaint.timestamps) {
        complaint.timestamps = {
          reported: complaint.createdAt,
        };
        changed = true;
      } else if (!complaint.timestamps.reported) {
        complaint.timestamps.reported = complaint.createdAt;
        changed = true;
      }

      // 3. Backfill current status timestamp if missing
      if (complaint.status === "in_progress" && !complaint.timestamps.inProgress) {
        complaint.timestamps.inProgress = complaint.updatedAt || complaint.createdAt;
        changed = true;
      } else if (complaint.status === "resolved" && !complaint.timestamps.resolved) {
        complaint.timestamps.resolved = complaint.updatedAt || complaint.createdAt;
        changed = true;
      } else if (complaint.status === "under_review" && !complaint.timestamps.underReview) {
        complaint.timestamps.underReview = complaint.updatedAt || complaint.createdAt;
        changed = true;
      }

      // 4. Ensure location GeoJSON is present (optional but good)
      if (!complaint.location || !complaint.location.coordinates || complaint.location.coordinates.length === 0) {
        const lat = parseFloat(complaint.latitude);
        const lng = parseFloat(complaint.longitude);
        if (!isNaN(lat) && !isNaN(lng)) {
          complaint.location = {
            type: "Point",
            coordinates: [lng, lat],
          };
          changed = true;
        }
      }

      if (changed) {
        await complaint.save();
        updatedCount++;
      }
    }

    console.log(`Successfully updated ${updatedCount} complaints.`);
    process.exit(0);
  } catch (error) {
    console.error("Migration failed:", error);
    process.exit(1);
  }
}

backfill();
