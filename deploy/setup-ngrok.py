#!/usr/bin/env python3
"""
Konfiguracja ngrok na serwerze LAN (192.168.100.48) — staly link do gry online.

Uzycie (wklej authtoken z https://dashboard.ngrok.com/get-started/your-authtoken):
  set NGROK_AUTHTOKEN=twoj_token
  python deploy/setup-ngrok.py
"""
import os
import sys
import paramiko

HOST = os.environ.get("DEPLOY_HOST", "192.168.100.48")
USER = os.environ.get("DEPLOY_USER", "miko")
PASSWORD = os.environ.get("DEPLOY_PASSWORD", "miko")
REMOTE_DIR = "/home/miko/monopoly"
NGROK_DOMAIN = os.environ.get("NGROK_DOMAIN", "bruising-lizard-deflector.ngrok-free.dev")
APP_PORT = os.environ.get("APP_PORT", "8080")
AUTHTOKEN = os.environ.get("NGROK_AUTHTOKEN", "").strip()


def run(ssh, cmd, sudo=False):
    full = f"sudo -S {cmd}" if sudo else cmd
    stdin, stdout, stderr = ssh.exec_command(full, get_pty=sudo)
    if sudo:
        stdin.write(PASSWORD + "\n")
        stdin.flush()
    out = stdout.read().decode()
    err = stderr.read().decode()
    code = stdout.channel.recv_exit_status()
    if code != 0:
        raise RuntimeError(f"CMD failed ({code}): {cmd}\n{out}\n{err}")
    return out


def main():
    if not AUTHTOKEN:
        print("Ustaw zmienna NGROK_AUTHTOKEN (token z panelu ngrok).", file=sys.stderr)
        print("  set NGROK_AUTHTOKEN=...   (PowerShell)", file=sys.stderr)
        print("  python deploy/setup-ngrok.py", file=sys.stderr)
        sys.exit(1)

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PASSWORD, timeout=30)

    run(ssh, f"mkdir -p {REMOTE_DIR}/logs")

    # Instalacja ngrok (apt lub binarka)
    _, stdout, _ = ssh.exec_command("command -v ngrok && ngrok version 2>&1 | head -1")
    if "ngrok" not in stdout.read().decode():
        run(ssh,
             "curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc "
             "| sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null && "
             "echo 'deb https://ngrok-agent.s3.amazonaws.com bookworm main' "
             "| sudo tee /etc/apt/sources.list.d/ngrok.list >/dev/null && "
             "sudo apt-get update -qq && "
             "sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq ngrok",
             sudo=True)

    run(ssh, f"ngrok config add-authtoken {AUTHTOKEN}")

    ngrok_service = f"""[Unit]
Description=ngrok tunnel — Politechnika Monopoly
After=network.target monopoly.service
Wants=monopoly.service

[Service]
Type=simple
User={USER}
ExecStart=/usr/local/bin/ngrok http {APP_PORT} --url=https://{NGROK_DOMAIN} --log=stdout
Restart=always
RestartSec=5
StandardOutput=append:{REMOTE_DIR}/logs/ngrok.log
StandardError=append:{REMOTE_DIR}/logs/ngrok.log

[Install]
WantedBy=multi-user.target
"""
    run(ssh, f"cat > /tmp/ngrok-monopoly.service << 'EOFSVC'\n{ngrok_service}EOFSVC")
    run(ssh, "mv /tmp/ngrok-monopoly.service /etc/systemd/system/ngrok-monopoly.service", sudo=True)
    run(ssh, "systemctl daemon-reload", sudo=True)
    run(ssh, "systemctl enable ngrok-monopoly", sudo=True)
    run(ssh, "systemctl restart ngrok-monopoly", sudo=True)

    import time
    time.sleep(4)
    _, stdout, _ = ssh.exec_command("systemctl is-active ngrok-monopoly")
    status = stdout.read().decode().strip()
    _, stdout, _ = ssh.exec_command(f"tail -8 {REMOTE_DIR}/logs/ngrok.log 2>/dev/null || true")
    logs = stdout.read().decode()

    ssh.close()

    print("\n=== ngrok GOTOWY ===")
    print(f"Status: {status}")
    print(f"Publiczny link (staly): https://{NGROK_DOMAIN}")
    print(f"Lobby: https://{NGROK_DOMAIN}/game")
    print(f"\nOstatnie logi ngrok:\n{logs}")
    print("\nKazdy gracz wchodzi na ten link, zaklada konto i dolacza po kodzie pokoju.")


if __name__ == "__main__":
    main()
