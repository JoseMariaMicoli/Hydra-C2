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

* [x] Secure SSL-Pinned Handshake
* [x] FastAPI Backend with SQLite Integration
* [x] Android "Ghost Service" Background Execution
* [x] Verified 200 OK Handshake
* [ ] Heartbeat Loop (60s intervals)

## 🛠 Project Structure

* `hydra_android/`: The Android client (Kotlin "Head").
* `hydra_c2/`: The Python FastAPI server (The "Hydra").
* `certs/`: (Local Only) SSL certificates (RSA-4096).

## ⚙️ Setup & Execution

### 1. Server Setup (Arch)

Ensure your `.pem` files are in the `hydra_c2` directory (added to `.gitignore`).

```bash
cd hydra_c2
python main.py

```

### 2. Android Client Setup

1. Open `hydra_android` in Android Studio.
2. Build and deploy to the emulator:

```bash
./gradlew installDebug

```

### 3. Verification

Trigger the service and monitor the logs:

```bash
adb shell am start-foreground-service com.hydra.client/.HydraService
adb logcat -s Hydra

```

---

## 🔒 Security Policy

> **Instruction [2026-01-11]:** All `.pem` and `.db` files must remain untracked. Ensure the local `hydra_heads.db` is removed from the git index before pushing.
