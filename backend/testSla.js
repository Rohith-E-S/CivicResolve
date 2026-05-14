import mongoose from "mongoose";
import dotenv from "dotenv";
dotenv.config();

const SLA_LIMITS = {
  new:                      24,
  under_review:             48,
};

const schema = new mongoose.Schema({
  status: String,
  sla: Object
});

schema.pre('save', function(next) {
  if (this.isModified('status')) {
     this.sla = { test: true };
  }
  next(); // <--- Is this where it throws?
});

const Model = mongoose.model("TestSla", schema);

async function run() {
  await mongoose.connect(process.env.MONGO_URI);
  const doc = new Model({ status: "new" });
  try {
    await doc.save();
    console.log("Success");
  } catch (e) {
    console.log("Error:", e.message);
  }
  process.exit();
}
run();
