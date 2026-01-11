#!/bin/bash

# 1. Clean up previous attempts
rm -rf Hydra_Project

# 2. Create the project root
mkdir Hydra_Project && cd Hydra_Project
echo "[+] Hydra_Project root created."

# 3. Setup C2 Server (Python)
mkdir -p hydra_c2/api && mkdir -p hydra_c2/dashboard
cd hydra_c2
python3 -m venv .venv

# Detect activation path (Linux/macOS vs Windows)
if [ -f ".venv/bin/activate" ]; then
    source .venv/bin/activate
elif [ -f ".venv/Scripts/activate" ]; then
    source .venv/Scripts/activate
fi

# Corrected requirements.txt
cat <<EOT >> requirements.txt
fastapi
uvicorn[standard]
aiosqlite
cryptography
python-multipart
EOT

pip install --upgrade pip
pip install -r requirements.txt
touch main.py
cd ..
echo "[+] Python C2 initialized with virtual environment."

# 4. Setup Desktop Head (Rust)
# We check for cargo first so the script doesn't crash
if command -v cargo &> /dev/null; then
    cargo init hydra_desktop
    echo "[+] Rust Desktop Head initialized."
else
    mkdir hydra_desktop
    echo "[!] Warning: Cargo not found. Install Rust then run 'cargo init' in hydra_desktop."
fi

# 5. Setup Android Head (Kotlin - Error Proof)
mkdir -p hydra_android/app/src/main/kotlin/com/hydra/client
mkdir -p hydra_android/gradle/wrapper
touch hydra_android/build.gradle.kts
touch hydra_android/settings.gradle.kts
touch hydra_android/app/build.gradle.kts
touch hydra_android/app/src/main/AndroidManifest.xml

# Populate Root build.gradle.kts with the 'Legacy' method to avoid conflicts
cat <<EOT >> hydra_android/build.gradle.kts
buildscript {
    repositories { google(); mavenCentral() }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
    }
}
allprojects { repositories { google(); mavenCentral() } }
EOT

echo "[+] Android folder structure and root build script created."
echo "--------------------------------------------------"
echo "GENESIS SUCCESSFUL"
echo "Next step: cd Hydra_Project/hydra_c2 && source .venv/bin/activate"
echo "--------------------------------------------------"