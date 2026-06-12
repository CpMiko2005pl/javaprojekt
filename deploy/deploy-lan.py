#!/usr/bin/env python3
"""Wdrozenie serwera gry na maszynie LAN przez SSH/SFTP."""
import os
import sys
import glob
import paramiko

HOST = os.environ.get("DEPLOY_HOST", "192.168.100.48")
USER = os.environ.get("DEPLOY_USER", "miko")
PASSWORD = os.environ.get("DEPLOY_PASSWORD", "miko")
REMOTE_DIR = "/home/miko/monopoly"
JAR_GLOB = os.path.join(os.path.dirname(__file__), "..", "target", "*.jar")


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
    jars = [j for j in glob.glob(JAR_GLOB) if "original" not in os.path.basename(j)]
    if not jars:
        print("Brak JAR w target/. Uruchom: mvnw package -DskipTests", file=sys.stderr)
        sys.exit(1)
    jar = max(jars, key=os.path.getmtime)

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PASSWORD, timeout=30)

    run(ssh, f"mkdir -p {REMOTE_DIR}/data {REMOTE_DIR}/logs")

    _, stdout, _ = ssh.exec_command("java -version 2>&1 | head -1")
    if "version" not in stdout.read().decode():
        run(ssh, "apt-get update -qq", sudo=True)
        run(ssh, "DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jre-headless", sudo=True)

    sftp = ssh.open_sftp()
    remote_jar = f"{REMOTE_DIR}/app.jar"
    print(f"Upload {jar} -> {remote_jar}")
    sftp.put(jar, remote_jar)
    sftp.close()

    service = f"""[Unit]
Description=Politechnika Monopoly LAN Server
After=network.target

[Service]
Type=simple
User={USER}
WorkingDirectory={REMOTE_DIR}
ExecStart=/usr/bin/java -Xms256m -Xmx1024m -jar {REMOTE_DIR}/app.jar --spring.profiles.active=lan
Restart=on-failure
RestartSec=5
StandardOutput=append:{REMOTE_DIR}/logs/server.log
StandardError=append:{REMOTE_DIR}/logs/server.log

[Install]
WantedBy=multi-user.target
"""
    run(ssh, f"cat > /tmp/monopoly.service << 'EOFSVC'\n{service}EOFSVC")
    run(ssh, "mv /tmp/monopoly.service /etc/systemd/system/monopoly.service", sudo=True)
    run(ssh, "systemctl daemon-reload", sudo=True)
    run(ssh, "systemctl enable monopoly", sudo=True)
    run(ssh, "systemctl restart monopoly", sudo=True)

    # firewall — jesli ufw aktywny
    stdin, stdout, stderr = ssh.exec_command("sudo -S ufw status 2>/dev/null", get_pty=True)
    stdin.write(PASSWORD + "\n")
    stdin.flush()
    ufw = stdout.read().decode()
    if "Status: active" in ufw:
        run(ssh, "ufw allow 8080/tcp", sudo=True)

    stdin, stdout, stderr = ssh.exec_command("sleep 8; curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/ || true")
    code = stdout.read().decode().strip()
    status = run(ssh, "systemctl is-active monopoly", sudo=True).strip()

    ssh.close()
    print(f"\n=== GOTOWE ===")
    print(f"Status uslugi: {status}")
    print(f"HTTP test: {code}")
    print(f"Gra online: http://{HOST}:8080")
    print(f"Lobby:      http://{HOST}:8080/game")
    print(f"Logi:       ssh {USER}@{HOST} 'tail -f {REMOTE_DIR}/logs/server.log'")


if __name__ == "__main__":
    main()
