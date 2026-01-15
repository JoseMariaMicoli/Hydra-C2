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

### **Project Hydra-C2 is a multi-headed C2 (Command and Control) framework.**

---

### ⚠️ DISCLAIMER

**For Educational and Authorized Security Testing Purposes Only.**
The use of this framework for attacking targets without prior mutual consent is illegal. It is the end user's responsibility to obey all applicable local, state, and federal laws. Developers assume no liability and are not responsible for any misuse or damage caused by this program.

---

### 🚀 Project Status: In Development (Update: 2026-01-12)

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
* [x] Live GPS Exfiltration (Single-ping & Automated Tracking Loop)
* [x] Audio Intelligence (Background Recording & Exfiltration - Desktop & Android)
* [x] Interactive Operator Console (v1.6 CLI with Session Targeting)
* [x] Intelligent Command Specification Manual (usage <cmd>)
* [ ] Remote Shell command execution (Android Head) In Progress
* [ ] Persistence Module (Systemd/Registry)
* [ ] Remote Control/Screen (TeamViewer style)
* [ ] Camera Snapshot (Mobile/Webcam)
* [x] Keylogging (Desktop Head)
* [ ] Contact/SMS Extraction (Android)
* [ ] Reverse Proxy / SOCKS5 Tunneling

---

## 🛠 Project Structure

### 🛰 The Hydra (C2 Server)

The brain of the operation, built with **Python & FastAPI**.

* **Dynamic Intelligence:** Platform-aware logging (Mobile vs Desktop).
* **Automated Tracking:** SQLite database tracking for all "Heads."
* **Command Dispatcher:** Sends platform-specific JSON payloads.
* **Output Collector:** Receives and logs remote shell results, GPS data, and Audio binary data.
* **File Manager**: Dedicated endpoints for /upload (Exfiltration) and /download (Looting).
* **Keylog Processor:** Automated timestamping and persistent appending for incoming keystroke data.
* **Path:** `/hydra_c2/`

### 📱 Android Head

A stealthy background service built with **Kotlin**.

* **Geospatial Intelligence**: High-accuracy coordinate retrieval and Automated Live Tracking (30s intervals).
* **Audio Intelligence:** Remote-triggered background microphone recording (MPEG4-AAC) with auto-exfiltration.
* **Network Intelligence:** Reports active SSID or Mobile Carrier name (Requires Location).
* **Vitals Reporting:** Real-time Battery percentage and OS version tracking.
* **Persistence:** Foreground Service with a `NotificationChannel` and `WakeLock`.
* **Action Execution:** Trigger hardware actions (e.g., Vibrator) via C2.
* **File Operations:** Full support for file exfiltration and payload ingestion.
* **Path:** `/hydra_android/`

### 💻 Desktop Head

A high-performance, lightweight agent built with **Rust**.

* **Features:**
* **Runtime:** Powered by `Tokio` for non-blocking async operations.
* **Audio Intelligence:** Persistent background microphone capture via `cpal` with automated WAV exfiltration.
* **Telemetry:** Gathers Hostname, OS version, and RAM details via `sysinfo`.
* **Shell Executor:** Executes arbitrary commands via `sh -c` and returns output.
* **File Transfer**: Built-in support for multipart file uploads and binary downloads.
* **Keylogging:** Event-driven X11/Windows/macOS keystroke capture via `rdev` with buffered exfiltration.
* **Path:** `/hydra_desktop/`

---

## ⚙️ Setup & Execution

### 1. Server Setup (Arch Linux)

Ensure your `.pem` files are in the server directory (they are ignored by git).

```bash
cd hydra_c2
python -m uvicorn main:app --host 0.0.0.0 --port 8443 --ssl-keyfile ./key.pem --ssl-certfile ./cert.pem

```

### 2. Android Client Setup

1. Open `hydra_android` in Android Studio.
2. Update the `BASE_URL` to your Arch Host IP (e.g., `https://192.168.1.50:8443`).
3. Deploy to emulator or physical device.

### 3. Desktop Client Setup

```bash
cd hydra_desktop
cargo run

```

---

Here are the specific updates for your README. I have overhauled the **Usage** section to reflect the new v1.6 Interactive CLI and updated the **Project Status** to show our recent progress.

### 📝 README.md Updates

Replace the existing **Project Status** and **🕹 Usage** sections with the following:

---

### 🚀 Project Status: In Development (Update: 2026-01-15)

* [x] **Interactive Operator Console (v1.6 CLI with Session Targeting)**
* [x] **Intelligent Command Specification Manual (`usage <cmd>`)**
* [x] Secure SSL-Pinned Handshake (Android/FastAPI)
* [x] **Audio Intelligence (Background Recording & Exfiltration - Desktop & Android)**
* [x] Live GPS Exfiltration (Single-ping & Automated Tracking Loop)
* [x] Keylogging (Desktop Head - Event Driven)
* [ ] **Android Accessibility Keylogger (Next Phase)**
* [ ] Remote Shell command execution (Android Head) - *In Progress*

---

## 🕹 Usage (The Commander v1.6)

The `commander.py` has been upgraded from a one-shot script to a **Persistent Interactive Console**.

### 1. Launch the Console

```bash
python commander.py

```

### 2. Basic Workflow

Once inside the Hydra shell, use the following flow to manage your "Heads":

```hydra
(hydra) > usage             # Display the high-level command box
(hydra) > list              # Identify active Heads (ID, Platform, Status)
(hydra) > use ANDROID-01    # Lock session to a specific target
(hydra:ANDROID-01) >        # Prompt updates to show active target

```

### 3. Deep Command Intelligence

For detailed syntax, examples, and descriptions of any module, use the contextual help:

```hydra
(hydra) > usage shell       # Detailed manual for remote execution
(hydra) > usage location    # Manual for GPS & Live Tracking
(hydra) > usage record      # Manual for Audio Intelligence

```

### 4. Interactive Examples

| Goal | Command |
| --- | --- |
| **Live Tracking** | `location start` |
| **Ambient Audio** | `record start` |
| **Capture Keys** | `keylog start` |
| **Remote Shell** | `shell "cat /etc/passwd"` |
| **Exfiltrate File** | `upload /sdcard/Photos/dcim.jpg` |

---

## 🔧 Troubleshooting & Performance (Arch Linux)

### 🖥 GPU & Emulator Stability

On machines with AMD Vega/Integrated graphics, use **SwiftShader (CPU)** to avoid "Broken Pipe" or "Write Lock" crashes:

```bash
/opt/android-sdk/emulator/emulator -avd HydraTester -gpu swiftshader_indirect -no-snapshot-load -no-audio

```

### 🎙 Audio "SUSPENDED" Fix (Desktop Head)

If Desktop recording returns 0 samples on Arch (Pipewire/Wireplumber), the hardware node is likely suspended. Force it active:

```bash
# Unmute and wake the analog input source
pactl set-source-mute alsa_input.pci-0000_04_00.6.analog-stereo 0

# Check status (Must be IDLE or RUNNING to capture data)
pactl list short sources

```

### 🔐 Permission & Physical Device Bypass (Android 11+)

To ensure hardware access (Mic/GPS/SSID) and prevent the system from killing the service:

```bash
# Hardware & Network Permissions
adb shell pm grant com.hydra.client android.permission.RECORD_AUDIO
adb shell pm grant com.hydra.client android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.hydra.client android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.hydra.client android.permission.ACCESS_BACKGROUND_LOCATION
adb shell appops set com.hydra.client WIFI_SCAN allow

# Power Management & SmartManager Bypass
adb shell dumpsys deviceidle whitelist +com.hydra.client
adb shell am set-standby-bucket com.hydra.client active

# Service Start (Physical)
adb shell am start-foreground-service -n com.hydra.client/com.hydra.client.HydraService

```

### 🌍 Simulating GPS Movement

If testing on a static emulator, force a coordinate update:

```bash
adb emu geo fix -31.4167 -64.1833

```

---

## 🔒 Security Policy & Persistence

> **Instruction [2026-01-12]:** All `.pem` (certificates) and `.db` (database) files must remain untracked. Never commit keys or active databases to the repository.

---