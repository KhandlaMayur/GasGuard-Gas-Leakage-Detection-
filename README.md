# GasGuard 🛡️🔥
### Real-Time Gas Leakage Detection & Monitoring System

GasGuard is a comprehensive, production-ready Android application designed for real-time gas leakage detection, device monitoring, incident response, and safety analytics. Built with modern Android development standards, it integrates seamlessly with ESP8266/NodeMCU hardware modules via Firebase Realtime Database.

---

## 🚀 Key Features

- **Authentication & User Management**: Secure registration, login, logout, and session persistence powered by Firebase Authentication with user data isolation.
- **Device Registration & Management**: Multi-device support allowing users to claim, monitor, and edit metadata (Device Name, Location, Threshold) for multiple gas sensors.
- **Real-Time Monitoring Dashboard**: Live reactive UI updating instantly when gas levels change in Firebase without requiring manual refreshes.
- **Intelligent Safety Status Calculation**: Client-side calculation engine categorizing gas levels into `SAFE`, `WARNING`, `DANGER`, and `SENSOR_ERROR` based on user-configurable thresholds.
- **Alert Management & Acknowledgement**:
  - Automatic creation of active alerts upon entering `WARNING` or `DANGER` states.
  - Severity escalation handling (WARNING → DANGER) without duplicating alerts.
  - User acknowledgement workflow ("I HAVE TAKEN ACTION") moving incidents to Alert History.
- **Acknowledgement Deadline & Countdown System**: High-stakes 60-second countdown timer for active incidents, with automatic expiry handling (`EXPIRED`) upon timeout.
- **Android Local Notifications**: High-priority local notifications with rich text, stable notification IDs, custom channels, and deep-link navigation directly to incident details.
- **Gas Level History, Charts, and Analytics**:
  - Historical reading logs with delta-based duplicate prevention.
  - Interactive line charts powered by Vico Charts.
  - Time-based filtering (`TODAY`, `7 DAYS`, `30 DAYS`, `ALL`) and statistical safety analytics (Min, Max, Average, Warning/Danger counts).
- **Hardware-Ready Monitoring & Simulation**: Structured separation between static metadata (`devices/{deviceId}`) and dynamic sensor data (`users/{userId}/devices/{deviceId}/liveData`), ensuring plug-and-play compatibility with physical ESP8266/NodeMCU microcontrollers.

---

## 🏛️ Architecture & Tech Stack

GasGuard strictly follows **Clean Architecture** and **MVVM (Model-View-ViewModel)** principles:

- **UI / Presentation**: Jetpack Compose, Material 3, Navigation Compose (Type-Safe Routes).
- **Dependency Injection**: Hilt (SingletonComponent, ViewModel).
- **Concurrency & State**: Kotlin Coroutines, Flow, StateFlow, `callbackFlow`.
- **Backend / Database**: Firebase Authentication, Firebase Realtime Database.
- **Charts & Visualization**: Vico Charts (Compose M3).
- **Package Name**: `com.gasguard.gasguard`

---

## 📂 Firebase Realtime Database Structure

```text
root
├── devices
│   └── {deviceId}
│       ├── deviceId: "DEVICE_001"
│       ├── deviceName: "Kitchen Gas Sensor"
│       ├── location: "Kitchen"
│       ├── threshold: 30.0
│       ├── ownerUserId: "{userId}"
│       └── createdAt: timestamp
│
└── users
    └── {userId}
        ├── devices
        │   └── {deviceId}
        │       └── liveData
        │           ├── gasLevel: 45.5
        │           ├── gasRaw: 3897
        │           ├── sensorStatus: "NORMAL"
        │           ├── lastHeartbeat: timestamp
        │           └── lastUpdated: timestamp
        ├── alerts
        │   └── {alertId}
        │       ├── alertId
        │       ├── deviceId
        │       ├── severity
        │       ├── status (ACTIVE / ACKNOWLEDGED / EXPIRED)
        │       ├── createdAt
        │       └── acknowledgementDeadline
        └── readings
            └── {deviceId}
                └── {readingId}
                    ├── gasLevel
                    ├── threshold
                    ├── status
                    └── timestamp
```

---

## 🛠️ Getting Started

### Prerequisites
- Android Studio Ladybug / Koala or newer.
- JDK 11 or higher.
- Android SDK (Compile SDK 37, Min SDK 26).

### Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/KhandlaMayur/GasGuard-Gas-Leakage-Detection-.git
   ```
2. Open the project in Android Studio.
3. Ensure your `app/google-services.json` file is correctly placed in the `app/` module directory with your Firebase project configuration supporting package name `com.gasguard.gasguard`.
4. Configure release signing (optional for debug builds) by placing your `key.properties` file and release keystore (`.jks`) in the project root.
5. Sync project with Gradle files and run the application on an emulator or physical device.

---

## 📦 Publishing / Release Build
To generate a signed production Android App Bundle (`.aab`) for Google Play Console upload:
```bash
./gradlew bundleRelease
```
The output bundle will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

---

## 📄 License
This project is developed for advanced gas safety monitoring and educational/production deployment.
