/* Match lobby — /game (aktywny pokoj) */
(function () {
    "use strict";

    var main = document.getElementById("main");
    var lobbyView = document.getElementById("lobbyView");
    if (!main || !lobbyView) return;

    var sessionId = main.dataset.session;
    if (!sessionId) return;

    var MAX_SLOTS = 4;
    var FRIENDS = window.__LOBBY_FRIENDS__ || [];
    var BOT_PRESETS = [
        { name: "Bot PB", color: "#00A651", icon: "fa-graduation-cap" },
        { name: "Bot CS", color: "#e11d48", icon: "fa-crosshairs" },
        { name: "Bot ML", color: "#38bdf8", icon: "fa-brain" }
    ];

    var lastState = null;
    var slotModal = document.getElementById("slotModal");

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

    function apiPost(path, body, cb) {
        var headers = Object.assign({ "Content-Type": "application/json" }, csrf());
        var opts = { method: "POST", headers: headers };
        if (body !== undefined) opts.body = JSON.stringify(body);
        fetch("/api/game/" + sessionId + path, opts)
            .then(function (r) { return r.json().then(function (d) { return { ok: r.ok, data: d }; }); })
            .then(function (res) {
                if (!res.ok && res.data && res.data.error) {
                    var hint = document.getElementById("startHint");
                    if (hint) hint.textContent = res.data.error;
                    return;
                }
                if (cb) cb(res.data);
            })
            .catch(function (e) { console.warn("API error", e); });
    }

    function openSlotModal(state) {
        if (!slotModal) return;
        var me = state.players.find(function (p) { return p.isMe; });
        if (!me || !me.leader) return;

        var inRoom = {};
        state.players.forEach(function (p) {
            if (!p.bot && p.name) inRoom[p.name.toLowerCase()] = true;
        });

        var listEl = document.getElementById("inviteFriendList");
        if (listEl) {
            var available = FRIENDS.filter(function (f) {
                return f.username && !inRoom[f.username.toLowerCase()];
            });
            if (available.length === 0 && FRIENDS.length > 0) {
                listEl.innerHTML = '<p class="muted">Wszyscy znajomi sa juz w pokoju lub zaproszeni.</p>';
            } else if (available.length === 0) {
                listEl.innerHTML = '<p class="muted">Brak znajomych — <a href="/friends">dodaj znajomych</a>.</p>';
            } else {
                listEl.innerHTML = available.map(function (f) {
                    return '<button type="button" class="fc-invite-friend-btn" data-friend-id="' + f.userId + '">'
                        + '<span class="fc-invite-friend-av">' + escHtml((f.username || "?").charAt(0).toUpperCase()) + '</span>'
                        + '<span>' + escHtml(f.username) + ' · LVL ' + (f.level || 1) + '</span>'
                        + '<i class="fa-solid fa-paper-plane"></i></button>';
                }).join("");
                listEl.querySelectorAll(".fc-invite-friend-btn").forEach(function (btn) {
                    btn.addEventListener("click", function () {
                        apiPost("/invite", { friendId: Number(btn.dataset.friendId) }, function () {
                            btn.disabled = true;
                            btn.innerHTML = '<span class="muted">Zaproszenie wysłane</span>';
                        });
                    });
                });
            }
        }

        var presetsEl = document.getElementById("botPresetsModal");
        if (presetsEl) {
            presetsEl.innerHTML = BOT_PRESETS.map(function (b) {
                return '<button type="button" class="fc-bot-preset" data-name="' + escHtml(b.name) + '" data-color="' + escHtml(b.color) + '">'
                    + '<span class="fc-bot-preset-av" style="background:' + escHtml(b.color) + '"><i class="fa-solid ' + escHtml(b.icon) + '"></i></span>'
                    + '<span>' + escHtml(b.name) + '</span></button>';
            }).join("");
            presetsEl.querySelectorAll(".fc-bot-preset").forEach(function (btn) {
                btn.addEventListener("click", function () {
                    apiPost("/add-bot", { name: btn.dataset.name, color: btn.dataset.color }, function () {
                        closeSlotModal();
                        if (lastState) renderSlots(lastState);
                    });
                });
            });
        }

        slotModal.style.display = "flex";
        slotModal.setAttribute("aria-hidden", "false");
    }

    function closeSlotModal() {
        if (!slotModal) return;
        slotModal.style.display = "none";
        slotModal.setAttribute("aria-hidden", "true");
    }

    if (slotModal) {
        slotModal.querySelectorAll("[data-close-modal]").forEach(function (el) {
            el.addEventListener("click", closeSlotModal);
        });
    }

    function renderSlots(state) {
        if (!state || !state.players) return;
        lastState = state;

        if (state.status === "ACTIVE") {
            window.location.href = "/game/" + sessionId;
            return;
        }
        if (state.status === "FINISHED") {
            window.location.href = "/game";
            return;
        }

        var me = state.players.find(function (p) { return p.isMe; });
        var isLeader = me && me.leader;
        var humans = state.players.filter(function (p) { return !p.bot; });
        var allHumansReady = humans.length > 0 && humans.every(function (p) { return p.ready; });
        var canAdd = state.players.length < MAX_SLOTS;

        var slotsEl = document.getElementById("playerSlots");
        if (slotsEl) {
            var html = "";
            for (var i = 0; i < MAX_SLOTS; i++) {
                var p = state.players[i];
                if (p) {
                    var readyCls = p.bot ? "fc-slot-bot" : (p.ready ? "fc-slot-ready" : "fc-slot-wait");
                    var statusIcon = p.bot
                        ? '<i class="fa-solid fa-robot"></i> BOT'
                        : (p.ready
                            ? '<i class="fa-solid fa-check"></i> Gotowy'
                            : '<i class="fa-solid fa-hourglass-half"></i> Czeka');
                    var crown = p.leader ? '<span class="fc-crown"><i class="fa-solid fa-crown"></i></span>' : '';
                    var removeBtn = (isLeader && p.bot)
                        ? '<button type="button" class="fc-slot-remove" data-bot-id="' + p.id + '" title="Usuń bota"><i class="fa-solid fa-xmark"></i></button>'
                        : '';
                    html += '<div class="fc-slot ' + readyCls + '">'
                        + crown + removeBtn
                        + '<div class="fc-slot-av" style="background:' + escHtml(p.color || "#475569") + '">'
                        + (p.bot ? '<i class="fa-solid fa-robot"></i>' : escHtml((p.name || "?").charAt(0).toUpperCase()))
                        + '</div>'
                        + '<div class="fc-slot-name">' + escHtml(p.name || "Gracz") + '</div>'
                        + '<div class="fc-slot-status">' + statusIcon + '</div>'
                        + '</div>';
                } else {
                    var emptyAction = isLeader && canAdd
                        ? '<div class="fc-slot-status fc-slot-add-hint"><i class="fa-solid fa-plus"></i> Zaproś / bot</div>'
                        : '<div class="fc-slot-status muted">Wolne miejsce</div>';
                    html += '<div class="fc-slot fc-slot-empty' + (isLeader && canAdd ? ' fc-slot-clickable' : '') + '" data-empty="1">'
                        + '<div class="fc-slot-av empty"><i class="fa-solid fa-user-plus"></i></div>'
                        + '<div class="fc-slot-name muted">Wolne miejsce</div>'
                        + emptyAction
                        + '</div>';
                }
            }
            slotsEl.innerHTML = html;

            slotsEl.querySelectorAll(".fc-slot-remove").forEach(function (btn) {
                btn.addEventListener("click", function (e) {
                    e.stopPropagation();
                    apiPost("/remove-bot/" + btn.dataset.botId, undefined, renderSlots);
                });
            });
            slotsEl.querySelectorAll(".fc-slot-clickable").forEach(function (slot) {
                slot.addEventListener("click", function () { openSlotModal(state); });
            });
        }

        var readyBtn = document.getElementById("readyBtn");
        if (readyBtn && me && !me.bot) {
            if (me.ready) {
                readyBtn.innerHTML = '<i class="fa-solid fa-times"></i> Cofnij gotowość';
                readyBtn.classList.add("fc-btn-unready");
            } else {
                readyBtn.innerHTML = '<i class="fa-solid fa-check"></i> Gotowy';
                readyBtn.classList.remove("fc-btn-unready");
            }
        }

        var startBtn = document.getElementById("startBtn");
        var startHint = document.getElementById("startHint");
        var canStart = allHumansReady && state.players.length >= 2;
        if (startBtn) {
            startBtn.style.display = isLeader ? "inline-flex" : "none";
            startBtn.disabled = !canStart;
            startBtn.classList.toggle("fc-btn-disabled", !canStart);
        }
        if (startHint) {
            if (isLeader && state.players.length < 2) {
                startHint.textContent = "Kliknij + na wolnym slocie — zaproś gracza lub dodaj bota.";
            } else if (isLeader && !allHumansReady) {
                startHint.textContent = "Czekaj aż wszyscy gracze klikną Gotowy.";
            } else if (isLeader && allHumansReady) {
                startHint.textContent = "Wszyscy gotowi — możesz wystartować mecz!";
            } else {
                startHint.textContent = "Host (korona) wystartuje mecz gdy wszyscy będą gotowi.";
            }
        }

        var lobbyStatus = document.getElementById("lobbyStatus");
        var readyCount = state.players.filter(function (p) { return p.ready || p.bot; }).length;
        if (lobbyStatus) {
            lobbyStatus.textContent = allHumansReady && humans.length > 0
                ? "Gotowi do startu"
                : "Oczekiwanie na graczy";
            lobbyStatus.classList.toggle("fc-status-ready", allHumansReady && humans.length > 0);
        }
        var counter = document.getElementById("readyCounter");
        if (counter) counter.textContent = readyCount + "/" + state.players.length + " gotowych";
    }

    fetch("/api/game/" + sessionId + "/state", { headers: Object.assign({}, csrf()) })
        .then(function (r) { return r.json(); })
        .then(renderSlots)
        .catch(function (e) { console.warn("Initial state", e); });

    var sock = new SockJS("/ws/game");
    var stomp = Stomp.over(sock);
    stomp.debug = null;
    stomp.connect({}, function () {
        stomp.subscribe("/topic/game/" + sessionId, function (frame) {
            try { renderSlots(JSON.parse(frame.body)); } catch (e) { /* ignore */ }
        });
    }, function () {
        setInterval(function () {
            fetch("/api/game/" + sessionId + "/state", { headers: Object.assign({}, csrf()) })
                .then(function (r) { return r.json(); })
                .then(renderSlots)
                .catch(function () {});
        }, 3000);
    });

    var readyBtn = document.getElementById("readyBtn");
    if (readyBtn) readyBtn.addEventListener("click", function () { apiPost("/ready", undefined, renderSlots); });

    var startBtn = document.getElementById("startBtn");
    if (startBtn) startBtn.addEventListener("click", function () {
        apiPost("/start", undefined, function (d) {
            if (d && d.status === "ACTIVE") window.location.href = "/game/" + sessionId;
            else renderSlots(d);
        });
    });

    var codeEl = document.getElementById("lobbyCode");
    var copyCodeBtn = document.getElementById("copyCodeBtn");
    if (copyCodeBtn && codeEl) {
        copyCodeBtn.addEventListener("click", function () {
            navigator.clipboard.writeText(codeEl.textContent.trim()).then(function () {
                copyCodeBtn.innerHTML = '<i class="fa-solid fa-check"></i> Skopiowano';
                setTimeout(function () {
                    copyCodeBtn.innerHTML = '<i class="fa-solid fa-copy"></i> Kopiuj kod';
                }, 2000);
            });
        });
    }

    var copyLinkBtn = document.getElementById("copyLinkBtn");
    if (copyLinkBtn) {
        copyLinkBtn.addEventListener("click", function () {
            var base = main.dataset.serverUrl || window.location.origin;
            var code = codeEl ? codeEl.textContent.trim() : "";
            var text = "Dołącz do Monopoly PB!\nKod: " + code + "\nSerwer: " + base + "\nWejdź: " + base + "/game";
            navigator.clipboard.writeText(text).then(function () {
                copyLinkBtn.innerHTML = '<i class="fa-solid fa-check"></i> Skopiowano';
                setTimeout(function () {
                    copyLinkBtn.innerHTML = '<i class="fa-solid fa-link"></i> Kopiuj link';
                }, 2000);
            });
        });
    }
})();
