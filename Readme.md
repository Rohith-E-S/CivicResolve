# Complaint Register Portal

## Project Overview
This is a full-stack application designed for registering and managing complaints. The system consists of a web frontend, a mobile Android application, and a Node.js backend. It features real-time chat functionality, authentication, and location-based mapping capabilities.

## Architecture & Technologies
The repository is structured into three main components:

### 1. Backend (`/backend`)
A RESTful API server with real-time web socket support.
- **Runtime:** Node.js
- **Framework:** Express.js
- **Database:** MongoDB (via Mongoose)
- **Real-time:** Socket.IO for live chat/messaging between users and admins
- **Authentication:** JWT (JSON Web Tokens) & bcrypt
- **Integrations:** Cloudinary (for media uploads) and Nodemailer (for emails/OTPs)

### 2. Frontend (`/frontend`)
A modern, responsive web application for users and administrators.
- **Framework:** React 19 (via Vite)
- **Styling:** TailwindCSS
- **Routing:** React Router DOM
- **Maps:** Leaflet & React-Leaflet
- **State/Data Fetching:** Axios
- **Real-time:** Socket.IO Client

### 3. Android (`/android`)
A native Android mobile client for the platform.
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose
- **Networking:** Retrofit, OkHttp, Moshi
- **Real-time:** Socket.IO Client
- **Image Loading:** Coil
- **Location Services:** Google Play Services Location

## Building and Running

### Backend
1. Navigate to the backend directory: `cd backend`
2. Install dependencies: `npm install`
3. Configure environment variables (create a `.env` file with MongoDB URI, JWT secrets, Cloudinary credentials, etc.)
4. Start the development server: `npm run dev` (Runs on port 4000 by default)

### Frontend
1. Navigate to the frontend directory: `cd frontend`
2. Install dependencies: `npm install`
3. Start the Vite development server: `npm run dev` (Runs on port 5173 by default)
4. Build for production: `npm run build`

### Android
1. Open the `/android` directory in Android Studio.
2. Sync the project with Gradle files.
3. Set up an emulator or connect a physical device.
4. Run the app (`app` module) from Android Studio.

## Development Conventions
- **API Communication:** The frontend and Android apps communicate with the backend via REST endpoints for CRUD operations and authentication. Real-time updates (e.g., chat messages related to complaints) are handled via Socket.IO.
- **Authentication:** Token-based authentication using cookies (backend configures `credentials: true` for CORS).
- **Styling:** The frontend relies heavily on TailwindCSS for utility-first styling, alongside custom CSS in `index.css` and `App.css`.
- **Android UI:** Fully implemented using modern Jetpack Compose (declarative UI), avoiding older XML-based layouts for screens.
