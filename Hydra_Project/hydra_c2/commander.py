import sqlite3
import json

def send_command(client_id, action, params):
    conn = sqlite3.connect("hydra_heads.db")
    cursor = conn.cursor()
    
    payload = json.dumps(params)
    cursor.execute(
        "INSERT INTO tasks (client_id, action, payload) VALUES (?, ?, ?)",
        (client_id, action, payload)
    )
    
    conn.commit()
    conn.close()
    print(f"[+] Task '{action}' queued for {client_id}")

# Example usage:
if __name__ == "__main__":
    #target = "ANDROID-HEAD-01"
    #send_command(target, "vibrate", {"duration": 2000})
    target = "DESKTOP-HEAD-ALPHA"
    send_command(target, "msg", {"content": "System maintenance required."})