# HealthCare Mobile App

A full-stack Android healthcare application enabling patients to book doctor appointments, manage their medical schedules, and communicate with doctors through real-time chat. Built with Java, Firebase, and modern Android architecture patterns.

---

## Overview

HealthCare Mobile App is a role-based healthcare platform that serves both **patients** and **doctors**. Patients can browse doctors by specialty, view availability, book appointments, and chat with their doctors in real-time. Doctors can manage their schedules, view patient lists, and respond to messages. The app uses Firebase as its backend, providing real-time data synchronization, authentication, and serverless cloud functions for automated appointment management.

---

## Features

### Authentication & Role Management

- **Email/Password Authentication** — Secure sign-up and login via Firebase Auth
- **Google Sign-In** — One-tap sign-in using Android Credential Manager
- **Role-Based Access Control** — Distinct interfaces for Patients and Doctors, enforced via Firebase Custom Claims
- **Seamless Role Routing** — AuthActivity detects user role and routes to the appropriate dashboard

### Patient Features

- **Home Dashboard** — Personalized home screen showing quick-access medical service categories, recommended doctors, and today's appointment schedule
- **Doctor Discovery** — Browse all available doctors with search and filter by name, specialty, or hospital
- **Doctor Profiles** — View detailed doctor profiles including specialty, experience, patient count, rating, and bio
- **Appointment Booking** — Select available time slots from the doctor's schedule and confirm appointments with conflict validation
- **Appointment Management** — View all appointments with tab filtering (All / Upcoming / Completed / Cancelled), search functionality, and ability to cancel upcoming appointments
- **Real-Time Chat** — Initiate and continue conversations with doctors with real-time messaging
- **Chat List** — View all conversations with unread message counts and last message previews
- **Patient Profile** — View and manage personal medical profile including name, age, gender, medical history, and blood group

### Doctor Features

- **Doctor Dashboard** — Overview screen with quick actions for appointments, chat, and patient management
- **Appointment Management** — View and manage all patient appointments
- **Patient List** — Browse all registered patients with search functionality
- **Availability Management** — Set and manage available scheduling days and time slots
- **Real-Time Chat** — Respond to patient messages with real-time delivery
- **Chat List** — View all patient conversations with unread counts

### Backend / Cloud Functions

- **Automatic Appointment Expiration** — Scheduled Cloud Function that automatically marks stale upcoming appointments (older than 7 days) as "Expired" daily
- **Role Claim Assignment** — Server-side function to set custom auth claims for secure role enforcement

---

## Architecture & Design Patterns

| Pattern                       | Implementation                                                                                                                                     |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Repository Pattern**        | `AppRepository` — Singleton data access layer encapsulating all Firestore, Auth, and Realtime Database operations                                  |
| **Fragment-Based Navigation** | Multiple fragments managed via `FragmentManager` for modular UI screens                                                                            |
| **Callback/Observer Pattern** | Listener interfaces and callbacks for asynchronous Firestore and Realtime Database operations                                                      |
| **Model-Data Separation**     | Dedicated `data/models/` package with clean POJOs (`Appointment`, `Doctor`, `Message`, `Conversation`, etc.)                                       |
| **Role-Based Routing**        | `AuthActivity` acts as a router, directing users to `MainActivity` (Patient) or `DoctorDashboardActivity` (Doctor) based on Firebase Custom Claims |
| **Data Validation**           | Models include business logic for state validation (e.g., `Appointment.canTransitionTo()`, `Appointment.isTerminalState()`)                        |

---

## Project Structure

```
health-care-mobile-app/
├── app/src/main/java/com/project/healthcare/
│   ├── MainActivity.java                 # Patient dashboard host
│   ├── DoctorDashboardActivity.java      # Doctor dashboard host
│   ├── auth/
│   │   ├── AuthActivity.java             # Auth routing / launcher
│   │   ├── LoginFragment.java            # Email/password login
│   │   └── SignupFragment.java           # Registration with role selection
│   ├── data/
│   │   ├── AppRepository.java            # Singleton repository (Firestore, Auth, RTDB)
│   │   ├── UserProfileDataUtil.java      # Profile data builder
│   │   └── models/
│   │       ├── Appointment.java          # Appointment entity with state machine
│   │       ├── Conversation.java         # Chat conversation entity
│   │       ├── Doctor.java               # Doctor profile entity
│   │       ├── DoctorAvailabilitySlot.java  # Availability slot entity
│   │       ├── DoctorPatient.java        # Doctor-patient reference entity
│   │       └── Message.java              # Chat message entity
│   ├── fragments/
│   │   ├── HomeFragment.java             # Patient home / dashboard
│   │   ├── ScheduleFragment.java         # Patient appointment list
│   │   ├── DoctorDetailFragment.java     # Doctor profile & booking
│   │   ├── AllDoctorsFragment.java       # Browse all doctors
│   │   ├── ChatListFragment.java         # Conversation list
│   │   ├── ChatFragment.java             # Real-time messaging
│   │   ├── ProfileFragment.java          # Patient profile
│   │   ├── DoctorHomeFragment.java       # Doctor dashboard
│   │   ├── DoctorAppointmentsFragment.java  # Doctor appointment list
│   │   ├── DoctorPatientListFragment.java   # Doctor patient list
│   │   ├── DoctorAvailabilityFragment.java  # Manage availability
│   │   └── DoctorChatFragment.java       # Doctor-side messaging
│   └── adapters/
│       ├── DoctorAdapter.java            # Doctor list adapter
│       ├── AppointmentAdapter.java       # Appointment list adapter
│       ├── ChatListAdapter.java          # Conversation list adapter
│       ├── MessageAdapter.java           # Chat message adapter
│       ├── ServiceGridAdapter.java       # Medical services grid adapter
│       └── PatientAdapter.java           # Patient list adapter
├── functions/
│   ├── index.js                          # Firebase Cloud Functions
│   └── package.json                      # Node.js dependencies
└── gradle/
    └── libs.versions.toml                # Version catalog
```

---

## Technologies & Tools

| Category           | Technology                                                  |
| ------------------ | ----------------------------------------------------------- |
| **Language**       | Java                                                        |
| **Platform**       | Android (minSdk 24, targetSdk 36, compileSdk 36)            |
| **Build System**   | Gradle with Kotlin DSL & Version Catalog                    |
| **IDE**            | Android Studio                                              |
| **Authentication** | Firebase Authentication, Google Identity Credential Manager |
| **Database**       | Cloud Firestore, Firebase Realtime Database                 |
| **Backend**        | Firebase Cloud Functions (Node.js 18)                       |
| **UI Framework**   | Material Design 3, AndroidX, ConstraintLayout, RecyclerView |
| **Testing**        | JUnit 4, Espresso                                           |

---

## Dependencies

### Android Dependencies

| Library                                                   | Version |
| --------------------------------------------------------- | ------- |
| `androidx.appcompat:appcompat`                            | 1.7.1   |
| `com.google.android.material:material`                    | 1.13.0  |
| `androidx.activity:activity`                              | 1.13.0  |
| `androidx.constraintlayout:constraintlayout`              | 2.2.1   |
| `androidx.recyclerview:recyclerview`                      | 1.4.0   |
| `androidx.credentials:credentials`                        | 1.6.0   |
| `androidx.credentials:credentials-play-services-auth`     | 1.6.0   |
| `com.google.android.libraries.identity.googleid:googleid` | 1.2.0   |

### Firebase Dependencies

| Library                                  | Version         |
| ---------------------------------------- | --------------- |
| `com.google.firebase:firebase-auth`      | 24.0.1          |
| `com.google.firebase:firebase-firestore` | _(BOM managed)_ |
| `com.google.firebase:firebase-functions` | _(BOM managed)_ |
| `com.google.firebase:firebase-database`  | 22.0.1          |

### Testing Dependencies

| Library                                | Version |
| -------------------------------------- | ------- |
| `junit:junit`                          | 4.13.2  |
| `androidx.test.ext:junit`              | 1.3.0   |
| `androidx.test.espresso:espresso-core` | 3.7.0   |

### Firebase Cloud Functions Dependencies

| Library              | Version  |
| -------------------- | -------- |
| `firebase-admin`     | ^11.10.1 |
| `firebase-functions` | ^4.5.0   |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or higher
- A Firebase project with Firestore, Authentication, and Cloud Functions enabled
- Google Services configuration file (`google-services.json`)

### Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/Native-Mobile-Development-Journey/health-care-mobile-app.git
   cd health-care-mobile-app
   ```

2. **Add Firebase Configuration**
   - Place your `google-services.json` file in the `app/` directory
   - Ensure Firebase project has Firestore, Auth (Email/Password + Google), and Realtime Database enabled

3. **Deploy Cloud Functions** (optional)

   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

4. **Build & Run**
   - Open the project in Android Studio
   - Sync Gradle files
   - Run on an emulator or physical device (minSdk 24+)

---

## Best Practices & Code Quality

- **Repository Pattern** — Centralized data access through a singleton `AppRepository`, decoupling UI from data sources
- **Singleton Repository** — Thread-safe lazy initialization for consistent data access across the application
- **Clean Architecture Layers** — Clear separation between UI (Fragments/Activities), Data (Repository/Models), and Backend (Cloud Functions)
- **Version Catalog** — Centralized dependency management via `gradle/libs.versions.toml` for consistent versioning
- **Role-Based Security** — Firebase Custom Claims enforce server-side role verification, ensuring patients cannot access doctor-only features and vice versa
- **State Machine in Models** — `Appointment` entity implements proper state transitions with validation (`canTransitionTo`, `isTerminalState`) preventing invalid status changes
- **Conflict Validation** — Appointment booking checks for scheduling conflicts before confirming
- **Automated Cleanup** — Cloud Functions handle stale data expiration, maintaining data integrity without manual intervention
- **Comprehensive .gitignore** — Sensitive files (`google-services.json`, keystores, `local.properties`) are excluded from version control
- **Input Validation** — Forms validate user input before submission, with appropriate error messaging
- **Async Callbacks** — Consistent asynchronous data handling with proper error callbacks throughout the codebase

---

## License

This project was developed as an academic project for the Software & Mobile Development course.

---

## Repository

GitHub: [health-care-mobile-app](https://github.com/Native-Mobile-Development-Journey/health-care-mobile-app)

---

## Author

**Native-Mobile-Development-Journey**

GitHub: [Native-Mobile-Development-Journey](https://github.com/Native-Mobile-Development-Journey)
