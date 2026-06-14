/* Plansza "Kampus PB" — widok 3D (canvas) + REST API rozgrywki. */
(function () {
    "use strict";

    const sessionId = document.getElementById("main").dataset.session;
    const canvas = document.getElementById("board");
    const ctx = canvas.getContext("2d");
    const SIZE = canvas.width;
    const CELL = SIZE / 11;
    const PAD = 6;

    const TILES = [
        "START", "Akademik Alfa", "Kasa Miejska", "Akademik Beta", "Podatek Stypendialny",
        "Dworzec PKP", "Wydz. Elektryczny", "Szansa", "Wydz. Mechaniczny", "Wydz. Budownictwa",
        "Akademik (areszt)", "Klub Gwint", "Elektrownia", "Bar Mleczny", "Biblioteka PB",
        "Dworzec PKS", "Wydz. Informatyki", "Kasa Miejska", "Wydz. Zarzadzania", "Wydz. Architektury",
        "Parking", "Rynek Kosciuszki", "Szansa", "Galeria Alfa", "Spodki (Rektorat)",
        "Dworzec Fabryczny", "Palac Branickich", "Opera Podlaska", "Wodociagi", "Stadion Miejski",
        "Idz do Akademika", "Las Zwierzyniecki", "Galeria Biala", "Kasa Miejska", "Politechnika",
        "Lotnisko Krywlany", "Szansa", "Aula Magna", "Podatek Luksusowy", "Kampus PB"
    ];

    const STRIPE = {
        1: "#8d6e63", 3: "#8d6e63",
        6: "#4da3ff", 8: "#4da3ff", 9: "#4da3ff",
        11: "#ff4d9a", 13: "#ff4d9a", 14: "#ff4d9a",
        16: "#ff9a3c", 18: "#ff9a3c", 19: "#ff9a3c",
        21: "#ff4757", 23: "#ff4757", 24: "#ff4757",
        26: "#ffd24a", 27: "#ffd24a", 29: "#ffd24a",
        31: "#2ecc71", 32: "#2ecc71", 34: "#2ecc71",
        37: "#5b6cff", 39: "#5b6cff",
        5: "#b8bcc8", 15: "#b8bcc8", 25: "#b8bcc8", 35: "#b8bcc8",
        12: "#8892a8", 28: "#8892a8"
    };

    const CORNERS = { 0: "🏁", 10: "🔒", 20: "🅿", 30: "🚓" };
    const SPECIAL = { 2: "💰", 7: "❓", 17: "💰", 22: "❓", 33: "💰", 36: "❓" };

    function tileCell(pos) {
        if (pos <= 10) return { col: 10 - pos, row: 10 };
        if (pos <= 20) return { col: 0, row: 10 - (pos - 10) };
        if (pos <= 30) return { col: pos - 20, row: 0 };
        return { col: 10, row: pos - 30 };
    }

    function side(pos) {
        if (pos < 10) return "bottom";
        if (pos < 20) return "left";
        if (pos < 30) return "top";
        return "right";
    }

    function wrapText(text, max) {
        const words = text.split(" ");
        const lines = [];
        let line = "";
        for (const w of words) {
            if ((line + " " + w).trim().length > max) {
                if (line) lines.push(line.trim());
                line = w;
            } else {
                line = (line + " " + w).trim();
            }
        }
        if (line) lines.push(line);
        return lines.slice(0, 3);
    }

    function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }

    function parseHex(hex) {
        const h = hex.replace("#", "");
        return {
            r: parseInt(h.slice(0, 2), 16),
            g: parseInt(h.slice(2, 4), 16),
            b: parseInt(h.slice(4, 6), 16)
        };
    }

    function rgb(c) { return "rgb(" + c.r + "," + c.g + "," + c.b + ")"; }

    function mix(c, t, amount) {
        return {
            r: clamp(Math.round(c.r + (t.r - c.r) * amount), 0, 255),
            g: clamp(Math.round(c.g + (t.g - c.g) * amount), 0, 255),
            b: clamp(Math.round(c.b + (t.b - c.b) * amount), 0, 255)
        };
    }

    function shade(hex, amount) {
        const c = parseHex(hex);
        const target = amount > 0 ? { r: 255, g: 255, b: 255 } : { r: 0, g: 0, b: 0 };
        return rgb(mix(c, target, Math.abs(amount)));
    }

    function tileDepth(pos) {
        if (CORNERS[pos]) return 16;
        if (STRIPE[pos]) return 13;
        if (SPECIAL[pos]) return 10;
        return 8;
    }

    function outwardFaces(col, row) {
        const cx = 5, cy = 5;
        return {
            front: row >= cy,
            back: row < cy,
            right: col >= cx,
            left: col < cx
        };
    }

    function drawFace(x, y, w, h, color) {
        ctx.fillStyle = color;
        ctx.fillRect(x, y, w, h);
    }

    function isDarkMode() {
        return document.documentElement.classList.contains('pb-dark');
    }

    function drawTile3D(col, row, pos) {
        const depth = tileDepth(pos);
        const x = col * CELL + PAD;
        const y = row * CELL + PAD;
        const w = CELL - PAD * 2;
        const h = CELL - PAD * 2;
        const faces = outwardFaces(col, row);

        const dark = isDarkMode();
        const base = CORNERS[pos] ? "#243049" : (STRIPE[pos] ? (dark ? "#1c2438" : "#eef2fb") : (dark ? "#151c2e" : "#dce4f5"));
        const sideDark = shade(base, -0.45);
        const sideMid = shade(base, -0.25);

        if (faces.front) drawFace(x, y + h, w, depth, sideMid);
        if (faces.back) drawFace(x, y - depth, w, depth, sideDark);
        if (faces.right) drawFace(x + w, y, depth, h, sideMid);
        if (faces.left) drawFace(x - depth, y, depth, h, sideDark);

        if (faces.front && faces.right) {
            ctx.fillStyle = shade(base, -0.55);
            ctx.fillRect(x + w, y + h, depth, depth);
        }
        if (faces.front && faces.left) {
            ctx.fillStyle = shade(base, -0.55);
            ctx.fillRect(x - depth, y + h, depth, depth);
        }
        if (faces.back && faces.right) {
            ctx.fillStyle = shade(base, -0.6);
            ctx.fillRect(x + w, y - depth, depth, depth);
        }
        if (faces.back && faces.left) {
            ctx.fillStyle = shade(base, -0.6);
            ctx.fillRect(x - depth, y - depth, depth, depth);
        }

        const grad = ctx.createLinearGradient(x, y, x + w, y + h);
        grad.addColorStop(0, shade(base, 0.35));
        grad.addColorStop(0.45, base);
        grad.addColorStop(1, shade(base, -0.12));
        ctx.fillStyle = grad;
        ctx.fillRect(x, y, w, h);

        ctx.strokeStyle = "rgba(255,255,255,0.35)";
        ctx.lineWidth = 1;
        ctx.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);

        const stripe = STRIPE[pos];
        if (stripe) {
            const s = side(pos), t = 10;
            const sg = ctx.createLinearGradient(x, y, x, y + t);
            sg.addColorStop(0, shade(stripe, 0.25));
            sg.addColorStop(1, stripe);
            ctx.fillStyle = sg;
            if (s === "bottom") ctx.fillRect(x + 2, y + 2, w - 4, t);
            else if (s === "top") ctx.fillRect(x + 2, y + h - t - 2, w - 4, t);
            else if (s === "left") ctx.fillRect(x + w - t - 2, y + 2, t, h - 4);
            else ctx.fillRect(x + 2, y + 2, t, h - 4);
            drawMiniBuilding(x + w / 2, y + h / 2 + 4, stripe);
        }

        const textMain  = dark ? "#c8d8f0" : "#1a2233";
        const textMuted = dark ? "#8899b8" : "#5a6478";
        const textSpec  = dark ? "#8899b8" : "#3d4660";
        const textStripe = dark ? "#aabbd4" : "#2a3348";

        ctx.fillStyle = textMain;
        ctx.textAlign = "center";
        if (CORNERS[pos]) {
            ctx.font = "24px Segoe UI Emoji, sans-serif";
            ctx.fillText(CORNERS[pos], x + w / 2, y + h / 2 + 2);
            ctx.fillStyle = textMuted;
            ctx.font = "700 7px Segoe UI, sans-serif";
            ctx.fillText(TILES[pos].toUpperCase().slice(0, 12), x + w / 2, y + h - 8);
        } else if (SPECIAL[pos]) {
            ctx.font = "20px Segoe UI Emoji, sans-serif";
            ctx.fillText(SPECIAL[pos], x + w / 2, y + h / 2 - 2);
            ctx.fillStyle = textSpec;
            ctx.font = "600 7.5px Segoe UI, sans-serif";
            const lines = wrapText(TILES[pos], 12);
            let ty = y + h / 2 + 14;
            for (const ln of lines) { ctx.fillText(ln, x + w / 2, ty); ty += 9; }
        } else if (!STRIPE[pos]) {
            ctx.font = "600 8px Segoe UI, sans-serif";
            const lines = wrapText(TILES[pos], 11);
            let ty = y + h / 2 - (lines.length - 1) * 4;
            for (const ln of lines) { ctx.fillText(ln, x + w / 2, ty); ty += 10; }
        } else {
            ctx.font = "600 7.5px Segoe UI, sans-serif";
            ctx.fillStyle = textStripe;
            const lines = wrapText(TILES[pos], 10);
            let ty = y + h - 10 - (lines.length - 1) * 8;
            for (const ln of lines) { ctx.fillText(ln, x + w / 2, ty); ty += 8; }
        }
    }

    function drawMiniBuilding(cx, cy, color) {
        const bw = 14, bh = 20, bd = 7;
        const bx = cx - bw / 2;
        const by = cy - bh / 2 - 4;

        ctx.fillStyle = shade(color, -0.35);
        ctx.fillRect(bx + bw, by + 4, bd, bh - 4);
        ctx.fillStyle = shade(color, -0.5);
        ctx.fillRect(bx, by + bh, bw + bd, bd);

        const tg = ctx.createLinearGradient(bx, by, bx + bw, by + bh);
        tg.addColorStop(0, shade(color, 0.4));
        tg.addColorStop(1, color);
        ctx.fillStyle = tg;
        ctx.fillRect(bx, by, bw, bh);

        ctx.fillStyle = "rgba(255,255,255,0.75)";
        ctx.fillRect(bx + 3, by + 5, 3, 4);
        ctx.fillRect(bx + 8, by + 5, 3, 4);
        ctx.fillRect(bx + 3, by + 12, 3, 4);
        ctx.fillRect(bx + 8, by + 12, 3, 4);
    }

    function drawCenterPlatform() {
        const inset = CELL * 1.15;
        const x = inset, y = inset;
        const w = SIZE - inset * 2;
        const h = SIZE - inset * 2;
        const depth = 22;

        const platformGrad = ctx.createLinearGradient(x, y, x + w, y + h);
        platformGrad.addColorStop(0, "#1a2744");
        platformGrad.addColorStop(0.5, "#243a66");
        platformGrad.addColorStop(1, "#152238");

        ctx.fillStyle = shade("#152238", -0.3);
        ctx.fillRect(x + 8, y + h, w, depth);
        ctx.fillStyle = shade("#152238", -0.45);
        ctx.fillRect(x + w, y + 8, depth, h);

        ctx.fillStyle = platformGrad;
        ctx.beginPath();
        ctx.roundRect(x, y, w, h, 18);
        ctx.fill();

        const inner = ctx.createRadialGradient(x + w / 2, y + h / 2, 20, x + w / 2, y + h / 2, w * 0.55);
        inner.addColorStop(0, "rgba(255, 210, 74, 0.18)");
        inner.addColorStop(1, "rgba(0,0,0,0)");
        ctx.fillStyle = inner;
        ctx.beginPath();
        ctx.roundRect(x + 12, y + 12, w - 24, h - 24, 14);
        ctx.fill();

        ctx.strokeStyle = "rgba(255, 214, 90, 0.45)";
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.roundRect(x + 8, y + 8, w - 16, h - 16, 16);
        ctx.stroke();

        ctx.save();
        ctx.translate(x + w / 2, y + h / 2 - 8);
        ctx.fillStyle = "#ffd24a";
        ctx.font = "900 38px Segoe UI, sans-serif";
        ctx.textAlign = "center";
        ctx.shadowColor = "rgba(0,0,0,0.45)";
        ctx.shadowBlur = 12;
        ctx.fillText("KAMPUS PB", 0, 0);
        ctx.shadowBlur = 0;
        ctx.fillStyle = "#9eb4dc";
        ctx.font = "700 16px Segoe UI, sans-serif";
        ctx.fillText("Business Tour Edition", 0, 28);
        ctx.fillStyle = "#6f86ad";
        ctx.font = "600 13px Segoe UI, sans-serif";
        ctx.fillText("Gra o Przetrwanie", 0, 50);
        ctx.restore();
    }

    function drawBoardShadow() {
        ctx.save();
        ctx.fillStyle = "rgba(0,0,0,0.28)";
        ctx.beginPath();
        ctx.ellipse(SIZE / 2, SIZE - 18, SIZE * 0.38, 28, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
    }

    function drawBoard() {
        ctx.clearRect(0, 0, SIZE, SIZE);

        const dark = isDarkMode();
        const sky = ctx.createLinearGradient(0, 0, 0, SIZE);
        if (dark) {
            sky.addColorStop(0, "#060a12");
            sky.addColorStop(0.55, "#0a1020");
            sky.addColorStop(1, "#0d1628");
        } else {
            sky.addColorStop(0, "#87c8ff");
            sky.addColorStop(0.55, "#5ba8ef");
            sky.addColorStop(1, "#3d7fc7");
        }
        ctx.fillStyle = sky;
        ctx.fillRect(0, 0, SIZE, SIZE);

        drawBoardShadow();

        ctx.fillStyle = dark ? "#0f1c14" : "#2d8a4e";
        ctx.fillRect(PAD, SIZE - 42, SIZE - PAD * 2, 34);
        ctx.fillStyle = dark ? "#0a1510" : "#256e3f";
        for (let i = 0; i < 14; i++) {
            ctx.beginPath();
            ctx.arc(PAD + 30 + i * 56, SIZE - 34, 16, 0, Math.PI * 2);
            ctx.fill();
        }

        for (let pos = 0; pos < 40; pos++) {
            const { col, row } = tileCell(pos);
            drawTile3D(col, row, pos);
        }

        drawCenterPlatform();
    }

    function drawToken3D(x, y, color, label) {
        const r = 9;
        ctx.save();
        ctx.fillStyle = "rgba(0,0,0,0.35)";
        ctx.beginPath();
        ctx.ellipse(x, y + 10, r * 0.9, r * 0.35, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = shade(color, -0.35);
        ctx.beginPath();
        ctx.ellipse(x, y + 4, r * 0.75, r * 0.55, 0, 0, Math.PI * 2);
        ctx.fill();

        const g = ctx.createRadialGradient(x - 3, y - 6, 2, x, y - 2, r);
        g.addColorStop(0, shade(color, 0.45));
        g.addColorStop(1, color);
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(x, y - 2, r, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = "#10141d";
        ctx.lineWidth = 2;
        ctx.stroke();

        ctx.fillStyle = "#fff";
        ctx.font = "700 8px Segoe UI, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(label, x, y);
        ctx.restore();
    }

    function drawTokens(players) {
        const grouped = {};
        players.forEach(function (p, idx) {
            if (!grouped[p.position]) grouped[p.position] = [];
            grouped[p.position].push({ p: p, idx: idx });
        });

        Object.keys(grouped).forEach(function (pos) {
            const { col, row } = tileCell(parseInt(pos, 10));
            const cxp = col * CELL + CELL / 2;
            const cyp = row * CELL + CELL / 2;
            grouped[pos].forEach(function (item, i) {
                const ox = (i % 3) * 16 - 16;
                const oy = Math.floor(i / 3) * 14 - 6;
                drawToken3D(cxp + ox, cyp + oy, item.p.color, String(item.idx + 1));
            });
        });
    }

    function csrf() {
        const t = document.querySelector('meta[name="_csrf"]');
        const h = document.querySelector('meta[name="_csrf_header"]');
        return { header: h ? h.content : null, token: t ? t.content : null };
    }

    function authHeaders(json) {
        const c = csrf();
        const h = {};
        if (json) h["Content-Type"] = "application/json";
        if (c.header && c.token) h[c.header] = c.token;
        return h;
    }

    let myPlayerId = null;

    function renderSide(state) {
        const box = document.getElementById("players");
        box.innerHTML = "";
        const sel = document.getElementById("toPlayer");
        sel.innerHTML = "";
        myPlayerId = null;

        state.players.forEach(function (p) {
            const turn = state.currentTurnPlayerId === p.id ? " turn" : "";
            const chip = document.createElement("div");
            chip.className = "player-chip" + turn;
            chip.innerHTML =
                '<span class="token-dot" style="background:' + p.color + '"></span>' +
                '<span class="player-name">' + escapeHtml(p.name) + (p.isMe ? " (Ty)" : "") + '</span>' +
                '<span class="cash">' + p.cash + ' zl</span>';
            box.appendChild(chip);
            if (p.isMe) myPlayerId = p.id;
        });

        state.players.filter(function (p) { return p.id !== myPlayerId; }).forEach(function (p) {
            const opt = document.createElement("option");
            opt.value = p.id;
            opt.textContent = p.name + " (" + p.cash + " zl)";
            sel.appendChild(opt);
        });

        const panel = document.getElementById("transferPanel");
        if (myPlayerId === null) {
            panel.style.opacity = ".5";
            document.getElementById("transferBtn").disabled = true;
        } else {
            panel.style.opacity = "1";
            document.getElementById("transferBtn").disabled = false;
        }

        if (state.dice1 != null) {
            document.getElementById("d1").textContent = state.dice1;
            document.getElementById("d2").textContent = state.dice2;
        }
        if (state.message) {
            document.getElementById("log").textContent = state.message;
        }
    }

    function escapeHtml(s) {
        const d = document.createElement("div");
        d.textContent = s;
        return d.innerHTML;
    }

    function render(state) {
        drawBoard();
        drawTokens(state.players);
        renderSide(state);
    }

    async function loadState() {
        const res = await fetch("/api/game/" + sessionId + "/state");
        render(await res.json());
    }

    document.getElementById("rollBtn").addEventListener("click", async function () {
        this.disabled = true;
        try {
            const res = await fetch("/api/game/" + sessionId + "/roll",
                { method: "POST", headers: authHeaders(false) });
            render(await res.json());
        } finally {
            this.disabled = false;
        }
    });

    document.getElementById("transferBtn").addEventListener("click", async function () {
        const errBox = document.getElementById("transferErr");
        errBox.style.display = "none";
        const toPlayerId = parseInt(document.getElementById("toPlayer").value, 10);
        const amount = parseInt(document.getElementById("amount").value, 10);
        if (!toPlayerId || !amount || amount <= 0) {
            errBox.textContent = "Podaj odbiorce i dodatnia kwote.";
            errBox.style.display = "block";
            return;
        }
        const res = await fetch("/api/game/" + sessionId + "/transfer", {
            method: "POST",
            headers: authHeaders(true),
            body: JSON.stringify({ toPlayerId: toPlayerId, amount: amount })
        });
        const data = await res.json();
        if (!res.ok) {
            errBox.textContent = data.error || "Nie udalo sie przelac.";
            errBox.style.display = "block";
            return;
        }
        render(data);
    });

    drawBoard();
    loadState();

    // Przerysuj planszę po zmianie motywu
    const _themeObserver = new MutationObserver(() => drawBoard());
    _themeObserver.observe(document.documentElement, { attributeFilter: ['class'] });
})();
