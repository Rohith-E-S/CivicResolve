export const PASSWORD_POLICY = "Use at least 8 characters and at most 72 UTF-8 bytes (some characters use more than one byte).";

export const passwordError = (password) => {
  if (typeof password !== "string" || [...password].length < 8) {
    return "Password must be at least 8 characters.";
  }
  if (new TextEncoder().encode(password).length > 72) {
    return "Password must be at most 72 UTF-8 bytes.";
  }
  return "";
};

export const googleLoginPayload = (response) => {
  if (!response?.credential) throw new Error("Google did not return a credential. Please try again.");
  return { credential: response.credential };
};

export const signupPayload = (pending, email, signupToken) => {
  if (!pending || pending.email !== email) throw new Error("Signup session expired. Please register again.");
  if (!signupToken) throw new Error("Email verification expired. Please register again.");
  const error = passwordError(pending.password);
  if (error) throw new Error(error);
  return { ...pending, email, signupToken };
};
