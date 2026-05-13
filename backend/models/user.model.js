import mongoose from "mongoose";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";

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
      minLength: 6,
    },

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
    },

    resetPasswordExpires: {
      type: Date,
      default: null,
    },

    // Last known GPS location (updated from the Android app)
    lastLocation: {
      type: {
        type: String,
        enum: ["Point"],
        default: "Point",
      },
      coordinates: {
        type: [Number], // [longitude, latitude]
        default: undefined, // not set until user shares location
      },
    },

    lastLocationUpdatedAt: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

userSchema.index({ lastLocation: "2dsphere" });

userSchema.methods.getJWT = function () {
  return jwt.sign({ _id: this.id }, process.env.JWT_SECRET_KEY, {
    expiresIn: "1d",
  });
};

userSchema.methods.checkPassword = async function (passwordInputByUser) {
  if (!this.password) return false;

  return await bcrypt.compare(passwordInputByUser, this.password);
};

const User = mongoose.model("User", userSchema);

export default User;
