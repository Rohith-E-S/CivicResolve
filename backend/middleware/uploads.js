import multer from "multer";
import fs from "fs";
import crypto from "crypto";

const ALLOWED_MIME_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif"];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const path = "uploads/";
    if (!fs.existsSync(path)) {
      fs.mkdirSync(path, { recursive: true });
    }
    cb(null, path); // temp folder
  },
  // Never build disk names from client-supplied originalname (path
  // separators / traversal); a random name is enough since the file is
  // only read server-side before upload to Cloudinary
  filename: (req, file, cb) => {
    cb(null, Date.now() + "-" + crypto.randomUUID());
  },
});

export const upload = multer({
  storage,
  limits: { fileSize: MAX_FILE_SIZE },
  fileFilter: (req, file, cb) => {
    if (ALLOWED_MIME_TYPES.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error("Only image files (jpeg, png, webp, gif, heic) are allowed"));
    }
  },
});
