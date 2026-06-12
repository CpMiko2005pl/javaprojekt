# Serwer gry LAN + ngrok (192.168.100.48)

## Gra przez Internet (ngrok — staly link)

Twoja domena dev: **https://bruising-lizard-deflector.ngrok-free.dev**

```powershell
# 1. Skopiuj authtoken z https://dashboard.ngrok.com/get-started/your-authtoken
$env:NGROK_AUTHTOKEN = "twoj_token_tutaj"
python deploy\setup-ngrok.py
```

Po setupie gra jest dostepna pod stalym linkiem (nie zmienia sie przy restarcie).

Restart tunelu:
```bash
ssh miko@192.168.100.48
sudo systemctl restart ngrok-monopoly
tail -f ~/monopoly/logs/ngrok.log
```

---

# Serwer gry LAN (192.168.100.48)

## Szybki deploy

```powershell
cd c:\Users\miko\Desktop\javanowe\javaprojekt
.\mvnw.cmd package -DskipTests
python deploy\deploy-lan.py
```

## Gra online — jak to dziala

1. Serwer dziala na **http://192.168.100.48:8080**
2. Kazdy gracz w tej samej sieci otwiera ten adres i loguje sie (lub rejestruje)
3. Host: `/game` → **Utworz pokoj** → kopiuje **kod pokoju**
4. Inni: `/game` → **Dolacz po kodzie**
5. Ruchy synchronizuja sie na zywo (WebSocket STOMP)

## Konta testowe (tworzone przy pierwszym starcie)

| Login | Haslo |
|-------|-------|
| admin | admin123 |
| gracz | gracz123 |
| kuba | kuba123 |

## Zarzadzanie serwerem (SSH)

```bash
ssh miko@192.168.100.48
sudo systemctl status monopoly
sudo systemctl restart monopoly
tail -f ~/monopoly/logs/server.log
```

## Pliki na serwerze

- JAR: `/home/miko/monopoly/app.jar`
- Baza H2: `/home/miko/monopoly/data/monopoly.*`
- Logi: `/home/miko/monopoly/logs/server.log`
- Usluga systemd: `monopoly.service`
