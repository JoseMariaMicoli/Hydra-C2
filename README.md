```markdown
# Project Hydra-C2

```text
   _    _           _              _____ ___  
  | |  | |         | |            / ____|__ \ 
  | |__| |_   _  __| |_ __ __ _  | |       ) |
  |  __  | | | |/ _` | '__/ _` | | |      / / 
  | |  | | |_| | (_| | | | (_| | | |____ / /_ 
  |_|  |_|\__, |\__,_|_|  \__,_|  \_____|____|
           __/ |                              
          |___/                               

```

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
* [ ] Command & Tasking System (JSON Payloads)
* [ ] Persistence Module (Systemd/Registry)

---

## 🛠 Project Structure

### 🛰 The Hydra (C2 Server)

The brain of the operation, built with **Python & FastAPI**.

* **Features:**
* Dynamic ASCII Splash Screen on initialization.
* Automated SQLite database tracking for all "Heads."
* Asynchronous request handling for multiple concurrent connections.


* **Path:** `/hydra_c2/`

### 📱 Android Head

A stealthy background service built with **Kotlin**.

* **Features:**
* **Persistence:** Foreground Service with a `NotificationChannel` to prevent OS killing.
* **WakeLock:** Prevents CPU deep sleep during 60s heartbeat cycles.
* **Security:** Implements SSL-bypass for development handshakes.


* **Path:** `/hydra_android/`

### 💻 Desktop Head (The "Great Talon")

A high-performance, lightweight agent built with **Rust**.

* **Features:**
* **Runtime:** Powered by `Tokio` for non-blocking async operations.
* **Telemetry:** Automatically gathers Hostname and OS details via `sysinfo`.
* **Efficiency:** Minimal memory footprint (< 5MB RAM).


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
2. Deploy to emulator or physical device:

```bash
./gradlew installDebug
adb shell am start-foreground-service com.hydra.client/.HydraService

```

### 3. Desktop Client Setup

Compile and run the Rust binary:

```bash
cd hydra_desktop
cargo run

```

---

## 🔒 Security Policy & Persistence

> **Instruction [2026-01-11]:** All `.pem` (certificates) and `.db` (database) files must remain untracked. Never commit keys or active databases to the repository.

**Current Git Protection:**

```bash
# Ensure local DB is not indexed
git rm --cached hydra_c2/hydra_heads.db

```

---