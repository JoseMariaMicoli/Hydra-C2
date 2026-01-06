import asyncio
import ssl
import json

class HydraC2:
    def __init__(self, host='0.0.0.0', port=4444):
        self.host = host
        self.port = port
        self.clients = {}

    def get_ssl_context(self):
        # Create a SERVER_AUTH context
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        
        # Enforce TLS 1.3 only
        context.minimum_version = ssl.TLSVersion.TLSv1_3
        
        # Load your self-signed certificate and private key
        context.load_cert_chain(certfile="hydra.crt", keyfile="hydra.key")
        return context

    async def handle_client(self, reader, writer):
        # The connection is already encrypted by the time we get here
        addr = writer.get_extra_info('peername')
        print(f"[+] Encrypted Head Attached: {addr}")

        try:
            while True:
                data = await reader.read(4096)
                if not data:
                    break
                
                # Protocol: JSON
                message = json.loads(data.decode('utf-8'))
                print(f"[*] Received from {addr}: {message}")
                
                # Route the message based on 'type'
                if message.get('type') == 'status':
                    response = {"status": "received", "command": "wait"}
                    writer.write(json.dumps(response).encode('utf-8'))
                    await writer.drain()

        except Exception as e:
            print(f"[!] Error: {e}")
        finally:
            writer.close()

    async def start_server(self):
        ssl_context = self.get_ssl_context()
        server = await asyncio.start_server(
            self.handle_client, self.host, self.port, ssl=ssl_context
        )
        print(f"[*] Hydra-C2 Core Online (TLS 1.3) on {self.host}:{self.port}")
        async with server:
            await server.serve_forever()

if __name__ == "__main__":
    asyncio.run(HydraC2().start_server())