/* Codzienne Kolo Fortuny — jasny motyw PB, wielolinijkowe etykiety w segmentach. */
(function () {
    "use strict";

    const segments = window.WHEEL_SEGMENTS || [];
    const n = segments.length || 1;
    const canvas = document.getElementById("wheel");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const size = canvas.width;
    const cx = size / 2;
    const cy = size / 2;
    const r = size / 2 - 14;
    const segAngleDeg = 360 / n;
    const segAngle = (2 * Math.PI) / n;
    const labelRadius = r - 36;

    /* Pastele zgodne z light.css / PB Design System */
    const COLORS = [
        "#C8EDD8", "#DDD0F5", "#C8DCFF", "#FFE8C2",
        "#FFD4DE", "#C4EDF5", "#D0F5E0", "#E8DDFA"
    ];

    function splitLabel(text, maxLen) {
        const words = String(text || "").split(/\s+/).filter(Boolean);
        const lines = [];
        let cur = "";
        words.forEach(function (w) {
            const next = cur ? cur + " " + w : w;
            if (next.length <= maxLen) {
                cur = next;
            } else {
                if (cur) lines.push(cur);
                cur = w.length > maxLen ? w.slice(0, maxLen - 1) + "…" : w;
            }
        });
        if (cur) lines.push(cur);
        return lines.slice(0, 3);
    }

    function chordWidthAt(radius) {
        return 2 * radius * Math.sin(segAngle / 2);
    }

    function fontSizeForSegment(text) {
        const len = (text || "").length;
        const chord = chordWidthAt(labelRadius);
        let fs = Math.floor(chord * 0.11);
        if (len > 28) fs -= 2;
        if (len > 36) fs -= 2;
        return Math.max(10, Math.min(13, fs));
    }

    function drawWheel() {
        ctx.clearRect(0, 0, size, size);

        ctx.save();
        ctx.beginPath();
        ctx.arc(cx, cy, r + 8, 0, 2 * Math.PI);
        ctx.fillStyle = "#ffffff";
        ctx.fill();
        ctx.strokeStyle = "rgba(0,166,81,0.28)";
        ctx.lineWidth = 6;
        ctx.stroke();
        ctx.restore();

        for (let i = 0; i < n; i++) {
            const start = -Math.PI / 2 + i * segAngle;
            const end = start + segAngle;

            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.arc(cx, cy, r, start, end);
            ctx.closePath();
            ctx.fillStyle = COLORS[i % COLORS.length];
            ctx.fill();
            ctx.strokeStyle = "rgba(255,255,255,0.95)";
            ctx.lineWidth = 2;
            ctx.stroke();

            const raw = segments[i] || "";
            const maxChars = Math.max(8, Math.floor(chordWidthAt(labelRadius) / 7.2));
            const lines = splitLabel(raw, maxChars);
            const fs = fontSizeForSegment(raw);
            const lineHeight = fs * 1.12;

            ctx.save();
            ctx.translate(cx, cy);
            ctx.rotate(start + segAngle / 2);
            ctx.textAlign = "right";
            ctx.textBaseline = "middle";
            ctx.font = "700 " + fs + "px 'Plus Jakarta Sans', 'Segoe UI', sans-serif";

            const startY = -((lines.length - 1) * lineHeight) / 2;
            lines.forEach(function (line, li) {
                const y = startY + li * lineHeight;
                ctx.fillStyle = "#0f172a";
                ctx.fillText(line, labelRadius, y);
            });
            ctx.restore();
        }

        ctx.beginPath();
        ctx.arc(cx, cy, 38, 0, 2 * Math.PI);
        ctx.fillStyle = "#ffffff";
        ctx.fill();
        ctx.strokeStyle = "#00A651";
        ctx.lineWidth = 4;
        ctx.stroke();

        const grd = ctx.createLinearGradient(cx - 20, cy - 20, cx + 20, cy + 20);
        grd.addColorStop(0, "#00A651");
        grd.addColorStop(1, "#6A1B9A");
        ctx.fillStyle = grd;
        ctx.font = "800 17px 'Space Grotesk', 'Segoe UI', sans-serif";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText("PB", cx, cy);
    }

    drawWheel();

    const btn = document.getElementById("spinBtn");
    const reward = document.getElementById("reward");
    const info = document.getElementById("spinInfo");
    let spinning = false;
    let currentRotation = 0;

    if (!window.CAN_SPIN) {
        if (btn) btn.disabled = true;
        if (info) info.textContent = "Dzis juz losowales. Wroc jutro!";
    }

    function csrf() {
        const t = document.querySelector('meta[name="_csrf"]');
        const h = document.querySelector('meta[name="_csrf_header"]');
        return { header: h ? h.content : null, token: t ? t.content : null };
    }

    if (!btn) return;
    btn.addEventListener("click", async function () {
        if (spinning || !window.CAN_SPIN) return;
        spinning = true;
        btn.disabled = true;
        if (reward) reward.textContent = "";

        try {
            const c = csrf();
            const headers = { "Content-Type": "application/json" };
            if (c.header && c.token) headers[c.header] = c.token;
            const res = await fetch("/wheel/spin", { method: "POST", headers });
            const data = await res.json();

            if (!data.spun) {
                if (info) info.textContent = data.message;
                spinning = false;
                btn.disabled = true;
                return;
            }

            const idx = data.rewardIndex;
            currentRotation += 360 * 6 + (360 - (idx + 0.5) * segAngleDeg);
            canvas.style.transition = "transform 4.5s cubic-bezier(0.17, 0.67, 0.12, 0.99)";
            canvas.style.transform = "rotate(" + currentRotation + "deg)";

            setTimeout(function () {
                if (reward) reward.textContent = "🎉 " + data.rewardLabel;
                if (info) info.textContent = data.message + " (Daily streak: " + data.dailyStreak + " 🔥)";
                spinning = false;
                window.CAN_SPIN = false;
            }, 4700);
        } catch (e) {
            if (info) info.textContent = "Blad podczas losowania.";
            spinning = false;
            btn.disabled = false;
        }
    });
})();
