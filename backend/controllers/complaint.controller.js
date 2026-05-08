import cloudinary from "../config/cloudinary.js";
import Complaint from "../models/complaint.model.js";
import Message from "../models/message.model.js";
import User from "../models/user.model.js";
import fs from "fs";
import {
  roadKeywords,
  garbageKeywords,
  waterKeywords,
  electricityKeywords,
  treeKeywords,
  fireKeywords,
  accidentKeywords,
  noiseKeywords,
  hasKeyword,
} from "../config/constants.js";
import { sendMail } from "../config/email.js";
import { notifyStatusChanged, notifyNewInDistrict, notifyUpvoted } from "../services/notificationService.js";

const ACTIVE_COMPLAINT_QUERY = { isDeleted: { $ne: true } };
const toRadians = (degree) => (degree * Math.PI) / 180;
const haversineDistanceKm = (lat1, lon1, lat2, lon2) => {
  const earthRadiusKm = 6371;
  const dLat = toRadians(lat2 - lat1);
  const dLon = toRadians(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRadians(lat1)) *
    Math.cos(toRadians(lat2)) *
    Math.sin(dLon / 2) ** 2;

  return earthRadiusKm * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
};

// AI Analyze Image
export const analyzeImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: "Image is required" });
    }

    let detectedCategory = "other";
    let confidence = 0.5;
    let description = "Our AI is still learning to identify this specific issue.";
    let severity = "moderate";
    let emoji = "📦";

    try {
      const upload = await cloudinary.uploader.upload(req.file.path, {
        categorization: "google_tagging",
        auto_tagging: 0.7,
      });

      const tags = upload.info?.categorization?.google_tagging?.data?.map((t) => t.tag.toLowerCase()) || [];
      confidence = upload.info?.categorization?.google_tagging?.data?.[0]?.confidence || 0.85;

      if (hasKeyword(tags, roadKeywords)) {
        detectedCategory = "road_damage";
        description = "Road surface damage or pothole detected.";
        severity = "urgent";
        emoji = "🕳️";
      } else if (hasKeyword(tags, garbageKeywords)) {
        detectedCategory = "garbage_issue";
        description = "Illegal dumping or waste accumulation detected.";
        severity = "moderate";
        emoji = "🗑️";
      } else if (hasKeyword(tags, waterKeywords)) {
        detectedCategory = "water_leakage";
        description = "Water leakage or flooding detected.";
        severity = "urgent";
        emoji = "💧";
      } else if (hasKeyword(tags, electricityKeywords)) {
        detectedCategory = "electricity_issue";
        description = "Electrical issue or streetlight failure detected.";
        severity = "urgent";
        emoji = "⚡";
      } else if (hasKeyword(tags, treeKeywords)) {
        detectedCategory = "tree_fallen";
        description = "Overgrown vegetation or fallen tree detected.";
        severity = "low";
        emoji = "🧹";
      } else if (hasKeyword(tags, accidentKeywords)) {
        detectedCategory = "accident";
        description = "Vehicle accident or traffic hazard detected.";
        severity = "urgent";
        emoji = "🚦";
      }
      fs.unlinkSync(req.file.path);
    } catch (error) {

      console.log(error);
    }

    res.status(200).json({
      success: true,
      category: detectedCategory,
      confidence: confidence,
      description: description,
      severity: severity,
      emoji: emoji,
    });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
};


// Create a Complaint
export const createComplaint = async (req, res) => {
  try {
    const { description, latitude, longitude, city, state, landmark, status } =
      req.body;

    if (
      !description ||
      !latitude ||
      !longitude ||
      !city ||
      !state ||
      !landmark
    ) {
      return res.status(404).json({
        success: false,
        message: "All details are required",
      });
    }

    const homeDistrict = req.user?.homeDistrict || "";
    if (homeDistrict.trim() !== "") {
      const isWithinBounds =
        city.toLowerCase().includes(homeDistrict.toLowerCase()) ||
        state.toLowerCase().includes(homeDistrict.toLowerCase()) ||
        homeDistrict.toLowerCase().includes(city.toLowerCase()) ||
        homeDistrict.toLowerCase().includes(landmark.toLowerCase());

      if (!isWithinBounds) {
        return res.status(403).json({
          success: false,
          message: `Out of Bounds: You are currently outside your home district (${homeDistrict}). You can only report issues for your local community.`,
        });
      }
    }

    let beforeImageUrl = "";
    let detectedCategory = "other";

    if (req.file) {
      try {
        const upload = await cloudinary.uploader.upload(req.file.path, {
          categorization: "google_tagging",
          auto_tagging: 0.7,
        });

        beforeImageUrl = upload.secure_url;

        const tags =
          upload.info?.categorization?.google_tagging?.data?.map((t) =>
            t.tag.toLowerCase()
          ) || [];

        console.log("Detected Tags:", tags);

        if (hasKeyword(tags, roadKeywords)) detectedCategory = "road_damage";
        else if (hasKeyword(tags, garbageKeywords))
          detectedCategory = "garbage_issue";
        else if (hasKeyword(tags, waterKeywords))
          detectedCategory = "water_leakage";
        else if (hasKeyword(tags, electricityKeywords))
          detectedCategory = "electricity_issue";
        else if (hasKeyword(tags, treeKeywords))
          detectedCategory = "tree_fallen";
        else if (hasKeyword(tags, fireKeywords)) detectedCategory = "fire";
        else if (hasKeyword(tags, accidentKeywords))
          detectedCategory = "accident";
        else if (hasKeyword(tags, noiseKeywords))
          detectedCategory = "noise_issue";

        fs.unlinkSync(req.file.path);
      } catch (error) {
        console.log(error);
        return res.status(500).json({
          success: false,
          message: `Error uploading image to Cloudinary: ${error.message}`,
        });
      }
    }

    // SAVE COMPLAINT
    const newComplaint = await Complaint.create({
      user: req.user._id,
      description,
      latitude,
      longitude,
      location: {
        type: "Point",
        coordinates: [parseFloat(longitude) || 0, parseFloat(latitude) || 0],
      },
      timestamps: {
        reported: new Date(),
      },
      city,
      state,
      landmark,
      beforeImageUrl,
      category: req.body.category || detectedCategory,
      status: "new",
    });


    // Populate user before sending to Android to avoid parsing errors
    const populatedComplaint = await Complaint.findById(newComplaint._id).populate("user");

    res.status(201).json({
      success: true,
      message: "Complaint submitted successfully",
      complaint: populatedComplaint,
    });

    // Notify neighbors in the same district
    try {
      const io = req.app.get("io");
      if (io && city) {
        const neighbors = await User.find({
          homeDistrict: { $regex: new RegExp(city, "i") },
          _id: { $ne: req.user._id }
        }).select("_id");
        
        if (neighbors.length > 0) {
          const neighborIds = neighbors.map(n => n._id);
          await notifyNewInDistrict(io, {
            districtUserIds: neighborIds,
            complaintId: newComplaint._id,
            category: detectedCategory,
            district: city
          });
        }
      }
    } catch (err) {
      console.error("Failed to notify neighbors:", err.message);
    }
  } catch (error) {
    console.log(error.message);
    return res.status(500).json({
      success: false,
      message: `Error in create complaint API: ${error.message}`,
    });
  }
};

// Get LoggedIn User complaints
export const getMyComplaint = async (req, res) => {
  try {
    const complaints = await Complaint.find({
      user: req.user._id,
      ...ACTIVE_COMPLAINT_QUERY,
    }).populate("user");
    if (!complaints) {
      return res
        .status(404)
        .json({ success: false, message: "No Complaints found" });
    }

    res.status(201).json({
      success: true,
      message: "Fetched all the user complaints",
      complaints,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(501).json({
      success: false,
      message: `Error in getUserComplaint API: ${error.message}`,
    });
  }
};

// get All Complaints - ADMIN
export const getAllComplaints = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res
        .status(404)
        .json({ success: false, message: "Only Admin can access this API" });
    }

    const complaints = await Complaint.find(ACTIVE_COMPLAINT_QUERY).populate(
      "user"
    );

    if (!complaints) {
      return res
        .status(404)
        .json({ success: false, message: "No Complaints Found" });
    }

    const newComplaint = complaints.filter(
      (complaint) => complaint.status === "new"
    );

    const underReviewComplaint = complaints.filter(
      (complaint) => complaint.status === "under_review"
    );

    const inProgressComplaint = complaints.filter(
      (complaint) => complaint.status === "in_progress"
    );

    const resolvedComplaint = complaints.filter(
      (complaint) => complaint.status === "resolved"
    );

    res.status(201).json({
      success: true,
      message: "Fetch all the complaints",
      complaints: {
        newComplaint,
        underReviewComplaint,
        inProgressComplaint,
        resolvedComplaint,
      },
    });
  } catch (error) {
    console.log(error.message);
    return res.status(501).json({
      success: false,
      message: `Error in Get All Complaints API: ${error.message}`,
    });
  }
};

// Filter Complaints on base of state/city - ADMIN (first way)
// export const filterComplaintOnStateCity = async (req, res) => {
//   try {
//     if (!req.user.isAdmin) {
//       return res
//         .status(401)
//         .json({ success: false, message: "You are not a admin" });
//     }

//     const state = req.body.state?.toLowerCase();
//     const city = req.body.city?.toLowerCase();

//     if (!state && !city) {
//       return res
//         .status(404)
//         .json({ success: true, message: "Need to provider state or city" });
//     }

//     let complaints;

//     if (state) {
//       complaints = await Complaint.find({ state }).populate("user");

//       if (!complaints || complaints.length === 0) {
//         return res
//           .status(404)
//           .json({ success: false, message: "No complaints within this state" });
//       }
//     } else {
//       complaints = await Complaint.find({ city }).populate("user");

//       if (!complaints || complaints.length === 0) {
//         return res
//           .status(404)
//           .json({ success: false, message: "No complaints within this city" });
//       }
//     }

//     res.status(201).json({
//       success: true,
//       message: "Complaint fetched successfully",
//       complaints,
//     });
//   } catch (error) {
//     console.log(error.message);
//     return res.status(501).json({
//       success: false,
//       message: `Error in filter complaint based on state and city API: ${error.message}`,
//     });
//   }
// };

// Filter Complaints on base of state/city - ADMIN (second way)
export const filterComplaintOnStateCity = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res.status(401).json({
        success: false,
        message: "You are not an admin",
      });
    }

    const state = (req.body.state || "").toLowerCase();
    const city = (req.body.city || "").toLowerCase();

    if (!state && !city) {
      return res.status(400).json({
        success: false,
        message: "You must provide state or city",
      });
    }

    let fetchField;
    let fetchValue;

    if (state) {
      fetchField = "state";
      fetchValue = state;
    } else {
      fetchField = "city";
      fetchValue = city;
    }

    const query = { [fetchField]: fetchValue, ...ACTIVE_COMPLAINT_QUERY };

    const complaints = await Complaint.find(query).populate("user");

    if (!complaints.length) {
      return res.status(404).json({
        success: false,
        message: `No complaints for this ${fetchField}`,
      });
    }

    return res.status(200).json({
      success: true,
      message: "Complaints fetched successfully",
      complaints,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error: ${error.message}`,
    });
  }
};

// Update complaint status - ADMIN
export const updateComplaintStatus = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res
        .status(404)
        .json({ success: false, message: "You are not a Admin" });
    }

    const complaintId = req.params.id;
    const { status } = req.body; // "under_review" | "in_progress" | "resolved"

    console.log("[updateComplaintStatus] email:", req.user?.email, "isAdmin:", req.user?.isAdmin, "status:", JSON.stringify(status), "body:", JSON.stringify(req.body));

    if (!["new", "under_review", "in_progress", "resolved"].includes(status)) {
      return res
        .status(404)
        .json({ success: false, message: "Invalid status value" });
    }

    const complaint = await Complaint.findOne({
      _id: complaintId,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    if (!complaint) {
      return res
        .status(404)
        .json({ success: false, message: "Complaint not found" });
    }

    const oldStatus = complaint.status;

    if (status === "resolved" && !complaint.afterImageUrl) {
      return res.status(400).json({
        success: false,
        message: "Cannot resolve complaint without an after image",
      });
    }

    // Map status string -> timestamps field
    const timestampMap = {
      under_review: "underReview",
      in_progress: "inProgress",
      resolved: "resolved",
    };
    const tsField = timestampMap[status];

    // Update status + record timestamp
    complaint.status = status;
    if (!complaint.timestamps) complaint.timestamps = {};
    if (tsField && !complaint.timestamps[tsField]) {
      complaint.timestamps[tsField] = new Date();
    }
    await complaint.save();

    // Return populated complaint so Android gets user object
    const populatedComplaint = await Complaint.findById(complaintId).populate("user");

    // Socket broadcasting logic
    const io = req.app.get("io");
    if (io) {
      if (status === "resolved") {
        io.emit("globalToast", {
          message: `Success! An issue in ${complaint.city} has been resolved!`,
          type: "success",
        });
      }
      io.to(complaintId).emit("statusUpdated", {
        complaintId,
        status: status.toUpperCase(),
      });
    }

    // Push in-app notification to complaint owner
    await notifyStatusChanged(io, {
      complaintOwnerId: complaint.user,
      complaintId,
      oldStatus,
      newStatus: status,
      category: complaint.category,
    });

    res.status(201).json({
      success: true,
      message: "Complaint status updated",
      complaint: populatedComplaint,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(501).json({
      success: false,
      message: `Error in update complaint status API: ${error.message}`,
    });
  }
};

// Update the afterImageUrl when status is resolved - ADMIN
export const updateAfterImageUrl = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res
        .status(404)
        .json({ success: false, message: "You are not a admin" });
    }

    const complaintId = req.params.id;

    if (!complaintId) {
      return res
        .status(404)
        .json({ success: false, message: "Please provide a complaintId" });
    }

    const complaint = await Complaint.findOne({
      _id: complaintId,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    if (!complaint) {
      return res
        .status(404)
        .json({ success: false, message: "No complaint found" });
    }

    if (!req.file) {
      return res
        .status(404)
        .json({ success: false, message: "Please provide a image" });
    }

    let imageUrl;
    if (req.file) {
      try {
        const upload = await cloudinary.uploader.upload(req.file.path);

        imageUrl = upload.secure_url;

        fs.unlinkSync(req.file.path);
      } catch (error) {
        console.log(error.message);
        return res.status(500).json({
          success: false,
          message: `Error while uploading the image to cloudinary: ${error.message}`,
        });
      }
    }

    complaint.afterImageUrl = imageUrl;
    complaint.status = "resolved";

    await complaint.save();
    const populatedComplaint = await Complaint.findById(complaint._id).populate("user");

    // Socket broadcasting
    const io = req.app.get("io");
    if (io) {
      io.emit("globalToast", {
        message: `Outstanding! A problem in ${complaint.city} was just resolved.`,
        type: "success"
      });
      io.to(complaintId).emit("statusUpdated", {
        complaintId,
        status: "RESOLVED"
      });
    }

    // Push in-app notification to complaint owner (fire-and-forget)
    notifyStatusChanged(io, {
      complaintOwnerId: complaint.user,
      complaintId,
      oldStatus: "in_progress",
      newStatus: "resolved",
      category:  complaint.category,
    }).catch(err => console.error("Notification failed:", err.message));

    res.status(201).json({
      success: true,
      message: "After Image uploaded successfully",
      complaint: populatedComplaint,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(501).json({
      success: false,
      message: `Error in update AfterImageUrl API: ${error.message}`,
    });
  }
};

// Update image and status - ADMIN
export const updateComplaint = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res.status(403).json({
        success: false,
        message: "You are not an admin",
      });
    }

    const complaintId = req.params.id;
    const { status } = req.body;

    const complaint = await Complaint.findOne({
      _id: complaintId,
      ...ACTIVE_COMPLAINT_QUERY,
    }).populate("user");

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: "Complaint not found",
      });
    }

    // Validate status (if provided)
    if (
      status &&
      !["new", "in_progress", "resolved"].includes(status.toLowerCase())
    ) {
      return res.status(400).json({
        success: false,
        message: "Invalid status value",
      });
    }

    if (req.file) {
      try {
        const uploaded = await cloudinary.uploader.upload(req.file.path);
        complaint.afterImageUrl = uploaded.secure_url;

        // Cleanup temp file
        fs.unlinkSync(req.file.path);

        // Auto-resolve if image uploaded
        complaint.status = "resolved";
      } catch (error) {
        return res.status(500).json({
          success: false,
          message: "Cloudinary upload failed: " + error.message,
        });
      }
    }

    if (!req.file && status) {
      complaint.status = status.toLowerCase();
    }

    // Push in-app notification to complaint owner
    const previousStatus = complaint.status; // captured before save
    await complaint.save();

    // Socket broadcasting
    const io = req.app.get("io");
    if (io) {
      if (complaint.status === "resolved") {
        io.emit("globalToast", {
          message: `Great news! Issues in ${complaint.city} are being fixed.`,
          type: "success"
        });
      }
      io.to(complaintId).emit("statusUpdated", {
        complaintId,
        status: complaint.status.toUpperCase()
      });
    }

    notifyStatusChanged(io, {
      complaintOwnerId: complaint.user._id,
      complaintId,
      oldStatus: previousStatus,
      newStatus: complaint.status,
      category:  complaint.category,
    }).catch(err => console.error("Notification failed:", err.message));

    // Send email after resolved
    if (complaint.status === "resolved") {
      await sendMail(
        complaint.user.email,
        "Your Complaint Has Been Resolved ✔️",
        `
    <div style="font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; color: #333;">
      <h2 style="color: #2b6cb0;">Your Complaint Has Been Resolved</h2>

      <p>Hi ${complaint.user.fullName},</p>

      <p>
        We are happy to inform you that your complaint has been successfully resolved.
      </p>

      <div style="background: #f3f4f6; padding: 15px; border-left: 4px solid #2b6cb0; margin: 20px 0;">
        <strong>Complaint:</strong><br />
        ${complaint.description}
      </div>

      <p style="margin-bottom: 10px;">
        Here is the latest image after resolution:
      </p>

      <div style="text-align: center; margin: 20px 0;">
        <img 
          src="${complaint.afterImageUrl}" 
          alt="Resolved Complaint Image" 
          style="max-width: 100%; border-radius: 10px; border: 1px solid #ccc;"
        />
      </div>

      <p>
        If you think further improvement is needed or want to share feedback, feel free to reply to this email.
      </p>

      <p style="margin-top: 30px;">
        Regards,<br />
        <strong>Complaint Management Team</strong>
      </p>
    </div>
    `
      );
    }

    return res.status(200).json({
      success: true,
      message: "Complaint updated successfully",
      complaint,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Error updating complaint: " + error.message,
    });
  }
};

// Get Complaint Details
export const getComplaint = async (req, res) => {
  try {
    const complaintId = req.params.id;

    const complaint = await Complaint.findOne({
      _id: complaintId,
      ...ACTIVE_COMPLAINT_QUERY,
    }).populate("user");

    if (!complaint) {
      return res
        .status(404)
        .json({ success: false, message: "No complaint found" });
    }

    res.status(201).json({
      success: true,
      message: "Complaint data fetched successfully",
      complaint,
      isAdmin: req.user.isAdmin,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(501).json({
      success: false,
      message: `Error in get complaiant data API: ${error.message}`,
    });
  }
};

// Rate a complaint
export const rateComplaint = async (req, res) => {
  try {
    const complaintId = req.params.id;
    const { rating } = req.body;

    if (!rating || rating < 1 || rating > 5) {
      return res.status(400).json({
        success: false,
        message: "Please provide a valid rating between 1 and 5",
      });
    }

    const complaint = await Complaint.findOne({
      _id: complaintId,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: "Complaint not found",
      });
    }

    if (complaint.status !== "resolved") {
      return res.status(400).json({
        success: false,
        message: "You can only rate resolved complaints",
      });
    }

    complaint.rating = rating;
    await complaint.save();
    const populatedComplaint = await Complaint.findById(complaint._id).populate("user");

    res.status(200).json({
      success: true,
      message: "Complaint rated successfully",
      complaint: populatedComplaint,
    });
  } catch (error) {
    console.log(error.message);
    return res.status(500).json({
      success: false,
      message: `Error in rate complaint API: ${error.message}`,
    });
  }
};


// Get Complaint Stats - ADMIN
export const getComplaintStats = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res.status(403).json({
        success: false,
        message: "You are not an admin",
      });
    }

    const complaints = await Complaint.find(ACTIVE_COMPLAINT_QUERY);

    const newComplaint = complaints.filter((c) => c.status === "new");
    const inProgressComplaint = complaints.filter(
      (c) => c.status === "in_progress"
    );
    const resolvedComplaint = complaints.filter((c) => c.status === "resolved");

    res.status(200).json({
      success: true,
      stats: {
        newComplaint,
        inProgressComplaint,
        resolvedComplaint,
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching stats: ${error.message}`,
    });
  }
};

// Get Paginated Complaints - ADMIN
export const getPaginatedComplaints = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res.status(403).json({
        success: false,
        message: "You are not an admin",
      });
    }

    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    const query = { ...ACTIVE_COMPLAINT_QUERY };
    if (status && status !== "all") {
      query.status = status;
    }

    const { search } = req.query;
    if (search) {
      // Escape special characters to prevent invalid regex errors
      const safeSearch = search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const searchRegex = new RegExp(safeSearch, "i");

      const searchConditions = [
        { description: searchRegex },
        { category: searchRegex },
        { city: searchRegex },
        { state: searchRegex },
        { landmark: searchRegex },
      ];

      // If search looks like a valid ObjectId, try to search by _id
      if (search.match(/^[0-9a-fA-F]{24}$/)) {
        searchConditions.push({ _id: search });
      }

      query.$or = searchConditions;
    }

    const total = await Complaint.countDocuments(query);
    const totalPages = Math.ceil(total / limit);
    const skip = (page - 1) * limit;

    const complaints = await Complaint.find(query)
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .populate("user");

    res.status(200).json({
      success: true,
      complaints,
      pagination: {
        total,
        totalPages,
        currentPage: page,
        limit,
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching paginated complaints: ${error.message}`,
    });
  }
};

// Get User Complaint Stats
export const getMyComplaintStats = async (req, res) => {
  try {
    const complaints = await Complaint.find({
      user: req.user._id,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    const newComplaint = complaints.filter((c) => c.status === "new");
    const inProgressComplaint = complaints.filter((c) => c.status === "in_progress");
    const resolvedComplaint = complaints.filter((c) => c.status === "resolved");

    res.status(200).json({
      success: true,
      stats: {
        total: complaints.length,
        newComplaint: newComplaint.length,
        inProgressComplaint: inProgressComplaint.length,
        resolvedComplaint: resolvedComplaint.length,
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching user stats: ${error.message}`,
    });
  }
};

// Get User Paginated Complaints
export const getMyPaginatedComplaints = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    const query = { user: req.user._id, ...ACTIVE_COMPLAINT_QUERY };
    if (status && status !== "all") {
      query.status = status;
    }

    const total = await Complaint.countDocuments(query);
    const totalPages = Math.ceil(total / limit);
    const skip = (page - 1) * limit;

    const complaints = await Complaint.find(query)
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit);

    res.status(200).json({
      success: true,
      complaints,
      pagination: {
        total,
        totalPages,
        currentPage: page,
        limit,
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching user paginated complaints: ${error.message}`,
    });
  }
};

// Get complaints with messages - ADMIN
export const getComplaintsWithMessages = async (req, res) => {
  try {
    if (!req.user.isAdmin) {
      return res.status(403).json({
        success: false,
        message: "You are not an admin",
      });
    }

    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    // 1. Get distinct complaint IDs that have messages
    const complaintIds = await Message.distinct("complaintId");

    const query = { _id: { $in: complaintIds }, ...ACTIVE_COMPLAINT_QUERY };
    if (status && status !== "all") {
      query.status = status;
    }

    const total = await Complaint.countDocuments(query);
    const totalPages = Math.ceil(total / limit);
    const skip = (page - 1) * limit;

    const complaints = await Complaint.find(query)
      .sort({ updatedAt: -1 }) // Sort by recently updated
      .skip(skip)
      .limit(limit)
      .populate("user");

    res.status(200).json({
      success: true,
      complaints,
      pagination: {
        total,
        totalPages,
        currentPage: page,
        limit,
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching active chats: ${error.message}`,
    });
  }
};

// Get active chats for current user
export const getMyComplaintsWithMessages = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const status = req.query.status;

    // 1. Get distinct complaint IDs where user is sender or receiver
    const complaintIds = await Message.distinct("complaintId", {
      $or: [{ fromUser: req.user._id }, { toUser: req.user._id }],
    });

    const query = {
      _id: { $in: complaintIds },
      user: req.user._id, // Ensure it's their own complaint
      ...ACTIVE_COMPLAINT_QUERY,
    };

    if (status && status !== "all") {
      query.status = status;
    }

    const total = await Complaint.countDocuments(query);
    const totalPages = Math.ceil(total / limit);
    const skip = (page - 1) * limit;

    const complaints = await Complaint.find(query)
      .sort({ updatedAt: -1 })
      .skip(skip)
      .limit(limit);

    res.status(200).json({
      success: true,
      complaints,
      pagination: {
        total,
        totalPages,
        currentPage: page,
        limit,
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching user active chats: ${error.message}`,
    });
  }
};

// Update current user's complaint details (only while complaint is new)
export const updateMyComplaint = async (req, res) => {
  try {
    const complaintId = req.params.id;
    const complaint = await Complaint.findOne({
      _id: complaintId,
      user: req.user._id,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: "Complaint not found",
      });
    }

    if (complaint.status !== "new") {
      return res.status(400).json({
        success: false,
        message: "Only new complaints can be edited",
      });
    }

    const allowedFields = [
      "description",
      "latitude",
      "longitude",
      "city",
      "state",
      "landmark",
      "category",
    ];

    let updatedFieldCount = 0;
    allowedFields.forEach((field) => {
      if (typeof req.body[field] !== "string") {
        return;
      }

      const value = req.body[field].trim();
      if (!value) {
        return;
      }

      complaint[field] = value;
      updatedFieldCount += 1;
    });

    if (!updatedFieldCount) {
      return res.status(400).json({
        success: false,
        message: "Provide at least one valid field to update",
      });
    }

    await complaint.save();

    return res.status(200).json({
      success: true,
      message: "Complaint updated successfully",
      complaint,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error updating complaint: ${error.message}`,
    });
  }
};

// Soft delete current user's complaint (only while complaint is new)
export const deleteMyComplaint = async (req, res) => {
  try {
    const complaintId = req.params.id;
    const complaint = await Complaint.findOne({
      _id: complaintId,
      user: req.user._id,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: "Complaint not found",
      });
    }

    if (complaint.status !== "new") {
      return res.status(400).json({
        success: false,
        message: "Only new complaints can be deleted",
      });
    }

    complaint.isDeleted = true;
    complaint.deletedAt = new Date();
    complaint.deletedBy = req.user._id;
    await complaint.save();

    return res.status(200).json({
      success: true,
      message: "Complaint deleted successfully",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error deleting complaint: ${error.message}`,
    });
  }
};

// Nearby complaint discovery (Geospatial)
export const getNearbyComplaints = async (req, res) => {
  try {
    const lat = parseFloat(req.query.lat);
    const lng = parseFloat(req.query.lng);
    const radius = Math.min(parseInt(req.query.radius) || 500, 2000);

    if (isNaN(lat) || isNaN(lng)) {
      return res
        .status(400)
        .json({ success: false, message: "Invalid coordinates" });
    }

    const complaints = await Complaint.find({
      location: {
        $nearSphere: {
          $geometry: {
            type: "Point",
            coordinates: [lng, lat], // GeoJSON: [longitude, latitude]
          },
          $maxDistance: radius, // in meters
        },
      },
      status: { $ne: "resolved" }, // only show active issues
    })
      .select("category description location status timestamps createdAt")
      .limit(10)
      .lean();

    // Calculate distance for each complaint (haversine formula)
    const withDistance = complaints.map((c) => {
      const [cLng, cLat] = c.location.coordinates;
      const distMeters = haversineDistance(lat, lng, cLat, cLng);
      return { ...c, distanceMeters: Math.round(distMeters) };
    });

    res.json({
      success: true,
      count: withDistance.length,
      complaints: withDistance,
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// ── Haversine distance formula (meters) ──────────────────────────────────────
function haversineDistance(lat1, lng1, lat2, lng2) {
  const toRad = (deg) => (deg * Math.PI) / 180;
  const R = 6371000; // Earth radius in meters
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// Support/upvote a complaint to signal duplicate impact
export const supportComplaint = async (req, res) => {
  try {
    const complaintId = req.params.id;
    const complaint = await Complaint.findOne({
      _id: complaintId,
      ...ACTIVE_COMPLAINT_QUERY,
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: "Complaint not found",
      });
    }

    if (complaint.user.toString() === req.user._id.toString()) {
      return res.status(400).json({
        success: false,
        message: "You cannot support your own complaint",
      });
    }

    const alreadySupported = complaint.supporters.some(
      (userId) => userId.toString() === req.user._id.toString()
    );

    if (alreadySupported) {
      complaint.supporters = complaint.supporters.filter(
        (id) => id.toString() !== req.user._id.toString()
      );
      complaint.supportCount = complaint.supporters.length;
      await complaint.save();

      return res.status(200).json({
        success: true,
        message: "Support removed",
        supportCount: complaint.supportCount,
      });
    }

    complaint.supporters.push(req.user._id);
    complaint.supportCount = complaint.supporters.length;
    await complaint.save();

    // Push in-app notification to complaint owner
    try {
      const io = req.app.get("io");
      if (io) {
        await notifyUpvoted(io, {
          complaintOwnerId: complaint.user,
          complaintId: complaint._id,
          upvoterId: req.user._id,
          upvoterName: req.user.fullName,
          category: complaint.category,
        });
      }
    } catch (err) {
      console.error("Failed to notify upvote:", err.message);
    }

    return res.status(200).json({
      success: true,
      message: "Complaint supported successfully",
      supportCount: complaint.supportCount,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error supporting complaint: ${error.message}`,
    });
  }
};

// Get Public Feed (All users, local or global)
export const getPublicFeed = async (req, res) => {
  try {
    const { district, page = 1, limit = 20 } = req.query;
    const query = { ...ACTIVE_COMPLAINT_QUERY };

    if (district && district !== "all") {
      const safeDistrict = district.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const districtRegex = new RegExp(safeDistrict, "i");
      query.$or = [
        { city: districtRegex },
        { state: districtRegex },
        { landmark: districtRegex }
      ];
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);

    const complaints = await Complaint.find(query)
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(parseInt(limit))
      .populate("user", "fullName profilePic");

    const total = await Complaint.countDocuments(query);

    res.status(200).json({
      success: true,
      complaints,
      pagination: {
        total,
        currentPage: parseInt(page),
        totalPages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching feed: ${error.message}`,
    });
  }
};

// Get Public Complaint Stats (Dynamic Local vs Global)
export const getPublicStats = async (req, res) => {
  try {
    const { district } = req.query;
    const query = { ...ACTIVE_COMPLAINT_QUERY };

    if (district && district !== "all") {
      const safeDistrict = district.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const districtRegex = new RegExp(safeDistrict, "i");
      query.$or = [
        { city: districtRegex },
        { state: districtRegex }
      ];
    }

    const totalResolved = await Complaint.countDocuments({
      ...query,
      status: "resolved",
    });

    const totalActive = await Complaint.countDocuments({
      ...query,
      status: { $in: ["new", "in_progress"] },
    });

    res.status(200).json({
      success: true,
      stats: {
        totalResolved,
        totalActive,
        scope: district && district !== "all" ? district : "Nationwide"
      },
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: `Error fetching public stats: ${error.message}`,
    });
  }
};
