import aiosqlite
import os

DB_PATH = "hydra_heads.db"

async def init_db():
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS clients (
                id TEXT PRIMARY KEY,
                platform TEXT NOT NULL,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                ip_address TEXT,
                status TEXT DEFAULT 'active'
            )
        """)
        await db.commit()
    print(f"[+] Database initialized at {DB_PATH}")

async def register_client(client_id, platform, ip):
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            INSERT INTO clients (id, platform, ip_address) 
            VALUES (?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET 
                last_seen=CURRENT_TIMESTAMP,
                ip_address=excluded.ip_address
        """, (client_id, platform, ip))
        await db.commit()