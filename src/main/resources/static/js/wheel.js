/* Codzienne Kolo Fortuny - rysowanie na canvasie + animacja losowania. */
(function () {
    "use strict";

    const segments = window.WHEEL_SEGMENTS || [];
    const n = segments.length;
    const canvas = document.getElementById("wheel");
    const ctx = canvas.getContext("2d");
    const size = canvas.width;
    const cx = size / 2, cy = size / 2, r = size / 2 - 6;
    const segAngleDeg = 360 / n;
    const segAngle = (2 * Math.PI) / n;

    const COLORS = ["#38bdf8", "#a855f7", "#10b981", "#f59e0b", "#f43f5e", "#6366f1", "#14b8a6", "#ec4899"];

    function drawWheel() {
        ctx.clearRect(0, 0, size, size);
        for (let i = 0; i < n; i++) {
            const start = -Math.PI / 2 + i * segAngle;
            const end = start + segAngle;
            // wycinek
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.arc(cx, cy, r, start, end);
            ctx.closePath();
            ctx.fillStyle = COLORS[i % COLORS.length];
            ctx.fill();
            ctx.strokeStyle = "#0d1017";
            ctx.lineWidth = 2;
            ctx.stroke();
            // tekst
            ctx.save();
            ctx.translate(cx, cy);
            ctx.rotate(start + segAngle / 2);
            ctx.textAlign = "right";
            ctx.fillStyle = "#11151c";
            ctx.font = "600 13px Segoe UI, sans-serif";
            const label = segments[i].length > 22 ? segments[i].slice(0, 21) + "…" : segments[i];
            ctx.fillText(label, r - 14, 5);
            ctx.restore();
        }
        // srodek
        ctx.beginPath();
        ctx.arc(cx, cy, 26, 0, 2 * Math.PI);
        ctx.fillStyle = "#0d1017";
        ctx.fill();
        ctx.strokeStyle = "#ff5a1f";
        ctx.lineWidth = 3;
        ctx.stroke();
    }

    drawWheel();

    const btn = document.getElementById("spinBtn");
    const reward = document.getElementById("reward");
    const info = document.getElementById("spinInfo");
    let spinning = false;
    let currentRotation = 0;

    if (!window.CAN_SPIN) {
        info.textContent = "Dzis juz losowales. Wroc jutro!";
    }

    function csrf() {
        const t = document.querySelector('meta[name="_csrf"]');
        const h = document.querySelector('meta[name="_csrf_header"]');
        return { header: h ? h.content : null, token: t ? t.content : null };
    }

    btn.addEventListener("click", async function () {
        if (spinning) return;
        spinning = true;
        btn.disabled = true;
        reward.textContent = "";

        try {
            const c = csrf();
            const headers = { "Content-Type": "application/json" };
            if (c.header && c.token) headers[c.header] = c.token;
            const res = await fetch("/wheel/spin", { method: "POST", headers });
            const data = await res.json();

            if (!data.spun) {
                info.textContent = data.message;
                spinning = false;
                btn.disabled = true;
                return;
            }

            // Docelowy obrot: srodek wylosowanego pola pod wskaznikiem (gora).
            const idx = data.rewardIndex;
            const target = 360 * 6 - (idx + 0.5) * segAngleDeg;
            currentRotation = target;
            canvas.style.transform = "rotate(" + currentRotation + "deg)";

            setTimeout(function () {
                reward.textContent = "🎉 " + data.rewardLabel;
                info.textContent = data.message + " (Daily streak: " + data.dailyStreak + " 🔥)";
            }, 4600);
        } catch (e) {
            info.textContent = "Blad podczas losowania.";
            spinning = false;
            btn.disabled = false;
        }
    });
})();
