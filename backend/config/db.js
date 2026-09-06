import mongoose from "mongoose";

export const connectDB = async () => {
  try {
    mongoose.connection.on("connected", () => {
      console.log("MongoDB Connected");
    });

    mongoose.connection.on("error", (err) => {
      console.log("MongoDB Connection Error:", err);
    });

    mongoose.connection.on("disconnected", () => {
      console.log("MongoDB Disconnected");
    });

    let mongoUrl = process.env.MONGODB_URL.trim().replace(/\/$/, '');
    // Handle Atlas URIs with query params (?appName=...) and trailing slash
    // Insert DB name before '?' if present: ...mongodb.net/?appName=X -> ...mongodb.net/problemRegPortal?appName=X
    if (mongoUrl.includes('?')) {
      const qIndex = mongoUrl.indexOf('?');
      const base = mongoUrl.slice(0, qIndex).replace(/\/$/, '');
      const query = mongoUrl.slice(qIndex + 1);
      mongoUrl = `${base}/problemRegPortal?${query}`;
    } else {
      mongoUrl = `${mongoUrl}/problemRegPortal`;
    }
    await mongoose.connect(mongoUrl);
  } catch (error) {
    console.log("Error connecting DB:", error);
  }
};
