/* Powiadomienia o zaproszeniach do gry — WebSocket /topic/user/{username} */
(function () {
    "use strict";

    var username = window.__CURRENT_USER__;
    if (!username || typeof SockJS === "undefined" || typeof Stomp === "undefined") return;

    var host = document.getElementById("gameInviteToastHost");
    if (!host) {
        host = document.createElement("div");
        host.id = "gameInviteToastHost";
        document.body.appendChild(host);
    }

    function csrf() {
        var t = document.querySelector('meta[name="_csrf"]');
        var h = document.querySelector('meta[name="_csrf_header"]');
        var hd = {};
        if (t && h) hd[h.content] = t.content;
        return hd;
    }

    function escHtml(s) {
        return (s || "").replace(/[&<>"']/g, function (c) {
            return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
        });
    }

    function showInviteToast(data) {
        if (!data || data.type !== "GAME_INVITE") return;
        var id = "invite-toast-" + data.inviteId;
        if (document.getElementById(id)) return;

        var el = document.createElement("div");
        el.className = "fc-invite-toast";
        el.id = id;
        el.innerHTML =
            '<div class="fc-invite-toast-body">'
            + '<strong><i class="fa-solid fa-gamepad"></i> Zaproszenie do gry</strong>'
            + '<p>' + escHtml(data.message || (data.inviterName + " zaprasza do pokoju")) + '</p>'
            + '<span class="muted">Kod: <strong>' + escHtml(data.roomCode) + '</strong></span>'
            + '</div>'
            + '<div class="fc-invite-toast-actions">'
            + '<button type="button" class="fc-btn fc-btn-ready fc-btn-sm fc-toast-accept">Dołącz</button>'
            + '<button type="button" class="fc-btn fc-btn-ghost fc-btn-sm fc-toast-decline">Odrzuć</button>'
            + '</div>';

        host.appendChild(el);

        el.querySelector(".fc-toast-accept").addEventListener("click", function () {
            fetch("/api/game/invites/" + data.inviteId + "/accept", {
                method: "POST",
                headers: Object.assign({ "Content-Type": "application/json" }, csrf())
            })
                .then(function (r) { return r.json(); })
                .then(function (res) {
                    if (res.redirect) window.location.href = res.redirect;
                    else if (res.error && typeof pbAlert === "function") pbAlert({ message: res.error, variant: "error" });
                });
        });

        el.querySelector(".fc-toast-decline").addEventListener("click", function () {
            fetch("/api/game/invites/" + data.inviteId + "/decline", {
                method: "POST",
                headers: Object.assign({ "Content-Type": "application/json" }, csrf())
            }).finally(function () { el.remove(); });
        });
    }

    function connect() {
        var sock = new SockJS("/ws/notifications");
        var stomp = Stomp.over(sock);
        stomp.debug = null;
        stomp.connect({}, function () {
            stomp.subscribe("/topic/user/" + username, function (frame) {
                try {
                    showInviteToast(JSON.parse(frame.body));
                } catch (e) { /* ignore */ }
            });
        }, function () {
            setTimeout(connect, 5000);
        });
    }

    fetch("/api/game/invites", { headers: csrf() })
        .then(function (r) { return r.json(); })
        .then(function (list) {
            if (Array.isArray(list)) {
                list.forEach(function (inv) {
                    showInviteToast({
                        type: "GAME_INVITE",
                        inviteId: inv.id,
                        sessionId: inv.sessionId,
                        sessionName: inv.sessionName,
                        roomCode: inv.roomCode,
                        inviterName: inv.inviterName,
                        message: inv.inviterName + " zaprasza Cię do pokoju „" + inv.sessionName + "”"
                    });
                });
            }
        })
        .catch(function () {});

    connect();
})();
