import mongoose from "mongoose";
import bcrypt from "bcrypt";
import { signSession, validPassword } from "../services/authSecurity.js";

// Query projections do not protect newly created or explicitly selected documents.
// Strip authentication material at both response serialization boundaries.
const removePrivateAuthFields = (_document, result) => {
  for (const field of ["password", "otp", "resetPasswordToken", "resetPasswordExpires", "fcmToken", "sessionVersion"]) {
    delete result[field];
  }
  return result;
};

const userSchema = new mongoose.Schema(
  {
    email: {
      type: String,
      required: true,
      unique: true,
    },

    fullName: {
      type: String,
      required: true,
    },

    password: {
      type: String,
      select: false,
    },

    sessionVersion: { type: Number, default: 0, select: false },

    googleId: {
      type: String,
      default: null,
    },

    isGoogleUser: {
      type: Boolean,
      default: false,
    },

    profilePic: {
      type: String,
      default: "",
    },

    address: {
      type: String,
    },

    homeDistrict: {
      type: String,
      default: "",
    },

    isAdmin: {
      type: Boolean,
      default: false,
      required: true,
    },

    otp: {
      type: Number,
    },

    isVerified: {
      type: Boolean,
      default: false,
    },
    resetPasswordToken: {
      type: String,
      default: null,
      select: false,
    },

    resetPasswordExpires: {
      type: Date,
      default: null,
      select: false,
    },

    // Last known GPS location (updated from the Android app)
    lastLocation: {
      type: {
        type: String,
        enum: ["Point"],
      },
      coordinates: {
        type: [Number], // [longitude, latitude]
      },
    },

    lastLocationUpdatedAt: {
      type: Date,
      default: null,
    },
    fcmToken: {
      type: String,
      default: null,
      select: false,
    },
    civicPoints: {
      type: Number,
      default: 0,
    },
    rank: {
      type: String,
      enum: [
        "citizen",
        "neighborhood_watch",
        "community_hero",
        "district_guardian",
        "civic_champion",
      ],
      default: "citizen",
    },
  },
  {
    timestamps: true,
    toJSON: { transform: removePrivateAuthFields },
    toObject: { transform: removePrivateAuthFields },
  }
);

userSchema.index({ lastLocation: "2dsphere" }, { partialFilterExpression: { "lastLocation.coordinates": { $exists: true } } });

userSchema.methods.getJWT = function () {
  return signSession(this);
};

userSchema.methods.checkPassword = async function (passwordInputByUser) {
  if (!this.password || !validPassword(passwordInputByUser, 1)) return false;

  return await bcrypt.compare(passwordInputByUser, this.password);
};

const User = mongoose.model("User", userSchema);

export default User;
