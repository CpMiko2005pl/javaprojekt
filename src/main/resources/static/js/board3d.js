/* Business Tour style — jasna plansza PB, stale pole, czytelne nazwy + ceny */
(function () {
    "use strict";

    var main = document.getElementById("main");
    if (!main) return;

    var sessionId = main.dataset.session;
    var container = document.getElementById("game-canvas-container");
    if (!container || typeof THREE === "undefined") return;

    var loader = document.getElementById("game-canvas-loader");
    var zoomInBtn = document.getElementById("zoom-in-btn");
    var zoomOutBtn = document.getElementById("zoom-out-btn");
    var rollBtn = document.getElementById("rollBtn");
    var liveBadge = document.getElementById("liveBadge");

    var TILE_NAMES = [];
    var TILE_EFFECTS = [];
    var STRIPE = {
        1: "#795548", 3: "#795548",
        6: "#2196F3", 8: "#2196F3", 9: "#2196F3",
        11: "#E91E63", 13: "#E91E63", 14: "#E91E63",
        16: "#FF9800", 18: "#FF9800", 19: "#FF9800",
        21: "#F44336", 23: "#F44336", 24: "#F44336",
        26: "#FFC107", 27: "#FFC107", 29: "#FFC107",
        31: "#4CAF50", 32: "#4CAF50", 34: "#4CAF50",
        37: "#3F51B5", 39: "#3F51B5"
    };
    var CORNERS = { 0: true, 10: true, 20: true, 30: true };

    var scene, camera, renderer, boardPivot, diceGroup;
    var playerMeshes = {};
    var myPlayerId = null;
    var lastState = null;
    var animating = false;
    var stompClient = null;
    var lastEventKey = "";
    var orthoZoom = 3.05;

    function tileLabelRotation(pos) {
        if (pos <= 10) return 0;
        if (pos <= 20) return -Math.PI / 2;
        if (pos <= 30) return Math.PI;
        return Math.PI / 2;
    }

    function eventKey(state) {
        return (state.movedPlayerId || "") + ":" + (state.fromPosition != null ? state.fromPosition : "") +
            ":" + (state.toPosition != null ? state.toPosition : "") +
            ":" + (state.dice1 || "") + ":" + (state.dice2 || "") + ":" + (state.message || "").slice(0, 40);
    }

    function posToWorld(pos) {
        var col, row;
        if (pos <= 10) { col = 10 - pos; row = 10; }
        else if (pos <= 20) { col = 0; row = 10 - (pos - 10); }
        else if (pos <= 30) { col = pos - 20; row = 0; }
        else { col = 10; row = pos - 30; }
        var cell = 1.02;
        return { x: (col - 5) * cell, z: (row - 5) * cell, col: col, row: row };
    }

    function shortName(name, pos) {
        if (pos === 0) return "START";
        if (pos === 10) return "DZIEKANAT";
        if (pos === 20) return "PARKING";
        if (pos === 30) return "DO DZIEK.";
        if (pos === 39) return "META";
        var s = (name || "").split(" — ")[0].split(" - ")[0];
        if (s.length > 13) s = s.slice(0, 12) + "…";
        return s.toUpperCase();
    }

    function parsePrice(effect, pos) {
        if (pos === 0) return "+200 zł";
        if (pos === 10) return "100 zł";
        if (pos === 30) return "→ Dziekanat";
        if (!effect) return "";
        if (effect.indexOf("Szans") >= 0 || effect.indexOf("Losuj") >= 0) return "SZANSA";
        if (effect.indexOf("Dworzec") >= 0) return "DWORZEC";
        if (effect.indexOf("Stypendium") >= 0) return "+200 zł";
        if (effect.indexOf("Oplata") >= 0 || effect.indexOf("WIEZIENIE") >= 0) {
            var m = effect.match(/(\d+)/);
            return m ? m[1] + " zł" : "OPŁATA";
        }
        var m = effect.match(/(\d+)\s*PLN/);
        return m ? m[1] + " zł" : "";
    }

    /** Tekstura pola jak w Business Tour: pasek koloru + nazwa + cena */
    function makeTileTexture(title, price, stripeHex, variant) {
        var canvas = document.createElement("canvas");
        canvas.width = 320;
        canvas.height = 420;
        var ctx = canvas.getContext("2d");

        ctx.fillStyle = "#ffffff";
        ctx.fillRect(0, 0, 320, 420);
        ctx.fillStyle = stripeHex || "#78909C";
        ctx.fillRect(0, 0, 320, 72);

        ctx.strokeStyle = "#455A64";
        ctx.lineWidth = 7;
        ctx.strokeRect(4, 4, 312, 412);

        if (variant === "start") {
            ctx.fillStyle = "#2E7D32";
            ctx.font = "bold 52px Arial,sans-serif";
            ctx.textAlign = "center";
            ctx.fillText("START", 160, 185);
            ctx.fillStyle = "#388E3C";
            ctx.font = "bold 38px Arial,sans-serif";
            ctx.fillText("+200 zł", 160, 250);
            ctx.font = "28px Arial,sans-serif";
            ctx.fillText("▶", 160, 310);
        } else if (variant === "jail") {
            ctx.fillStyle = "#C62828";
            ctx.font = "bold 40px Arial,sans-serif";
            ctx.textAlign = "center";
            ctx.fillText("DZIEKANAT", 160, 165);
            ctx.font = "bold 34px Arial,sans-serif";
            ctx.fillText("100 zł", 160, 225);
            ctx.font = "bold 26px Arial,sans-serif";
            ctx.fillText("WIĘZIENIE", 160, 285);
        } else if (variant === "parking") {
            ctx.fillStyle = "#1565C0";
            ctx.font = "bold 38px Arial,sans-serif";
            ctx.textAlign = "center";
            ctx.fillText("PARKING", 160, 175);
            ctx.font = "26px Arial,sans-serif";
            ctx.fillText("Kampus PB", 160, 235);
            ctx.fillStyle = "#2E7D32";
            ctx.font = "bold 30px Arial,sans-serif";
            ctx.fillText("FREE", 160, 295);
        } else if (variant === "goto") {
            ctx.fillStyle = "#E65100";
            ctx.font = "bold 34px Arial,sans-serif";
            ctx.textAlign = "center";
            wrapFill(ctx, "DO DZIEK.", 160, 165, 280, 34);
            ctx.font = "bold 28px Arial,sans-serif";
            ctx.fillText("→ Więzienie", 160, 260);
        } else {
            ctx.fillStyle = "#212121";
            ctx.font = "bold 30px Arial,sans-serif";
            ctx.textAlign = "center";
            wrapFill(ctx, title, 160, 155, 290, 34);
            ctx.fillStyle = "#1B5E20";
            ctx.font = "bold 36px Arial,sans-serif";
            ctx.fillText(price || " ", 160, 340);
        }

        var tex = new THREE.CanvasTexture(canvas);
        tex.anisotropy = 8;
        return tex;
    }

    function wrapFill(ctx, text, cx, y, maxW, lineH) {
        var words = text.split(" ");
        var line = "";
        var lines = [];
        words.forEach(function (w) {
            var test = line ? line + " " + w : w;
            if (ctx.measureText(test).width > maxW && line) {
                lines.push(line);
                line = w;
            } else line = test;
        });
        if (line) lines.push(line);
        lines = lines.slice(0, 3);
        var startY = y - ((lines.length - 1) * lineH) / 2;
        lines.forEach(function (ln, i) { ctx.fillText(ln, cx, startY + i * lineH); });
    }

    function initThree() {
        scene = new THREE.Scene();
        scene.background = new THREE.Color(0x8ecae6);

        var aspect = container.clientWidth / Math.max(container.clientHeight, 520);
        camera = new THREE.OrthographicCamera(
            -orthoZoom * aspect, orthoZoom * aspect, orthoZoom, -orthoZoom, 0.1, 300
        );
        camera.position.set(11, 18, 11);
        camera.lookAt(0, 0, 0);
        camera.updateProjectionMatrix();

        renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
        renderer.setSize(container.clientWidth, Math.max(container.clientHeight, 520));
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        container.appendChild(renderer.domElement);

        scene.add(new THREE.HemisphereLight(0xffffff, 0x7cb342, 0.85));
        var sun = new THREE.DirectionalLight(0xffffff, 0.65);
        sun.position.set(10, 22, 8);
        sun.castShadow = true;
        scene.add(sun);

        boardPivot = new THREE.Group();
        boardPivot.rotation.x = -0.62;
        boardPivot.rotation.y = 0.785;
        scene.add(boardPivot);

        window.addEventListener("resize", onResize);
        if (zoomInBtn) zoomInBtn.addEventListener("click", function () { setZoom(orthoZoom * 0.9); });
        if (zoomOutBtn) zoomOutBtn.addEventListener("click", function () { setZoom(orthoZoom * 1.1); });

        animateLoop();
        if (loader) {
            setTimeout(function () {
                loader.classList.add("hidden");
                setTimeout(function () { loader.style.display = "none"; }, 350);
            }, 350);
        }
    }

    function setZoom(z) {
        orthoZoom = Math.max(2.6, Math.min(z, 5.2));
        onResize();
    }

    function buildBoard(tileNames, tileEffects) {
        while (boardPivot.children.length) boardPivot.remove(boardPivot.children[0]);

        var grass = new THREE.Mesh(
            new THREE.BoxGeometry(13.2, 0.22, 13.2),
            new THREE.MeshLambertMaterial({ color: 0x5cb85c })
        );
        grass.receiveShadow = true;
        boardPivot.add(grass);

        var boardRim = new THREE.Mesh(
            new THREE.BoxGeometry(12.4, 0.08, 12.4),
            new THREE.MeshLambertMaterial({ color: 0xfafafa })
        );
        boardRim.position.y = 0.12;
        boardPivot.add(boardRim);

        var innerGrass = new THREE.Mesh(
            new THREE.BoxGeometry(8.8, 0.26, 8.8),
            new THREE.MeshLambertMaterial({ color: 0x7bc96f })
        );
        innerGrass.position.y = 0.04;
        boardPivot.add(innerGrass);

        var centerTex = (function () {
            var c = document.createElement("canvas");
            c.width = 512; c.height = 512;
            var ctx = c.getContext("2d");
            ctx.fillStyle = "#ffffff";
            ctx.fillRect(0, 0, 512, 512);
            ctx.fillStyle = "#1565C0";
            ctx.fillRect(0, 0, 512, 70);
            ctx.strokeStyle = "#37474F";
            ctx.lineWidth = 8;
            ctx.strokeRect(4, 4, 504, 504);
            ctx.fillStyle = "#1565C0";
            ctx.font = "bold 52px Arial,sans-serif";
            ctx.textAlign = "center";
            ctx.fillText("POLITECHNIKA", 256, 200);
            ctx.fillText("BIAŁOSTOCKA", 256, 265);
            ctx.fillStyle = "#2E7D32";
            ctx.font = "bold 36px Arial,sans-serif";
            ctx.fillText("Kampus PB", 256, 340);
            var t = new THREE.CanvasTexture(c);
            return t;
        })();
        var centerPlane = new THREE.Mesh(
            new THREE.PlaneGeometry(5.5, 5.5),
            new THREE.MeshBasicMaterial({ map: centerTex })
        );
        centerPlane.rotation.x = -Math.PI / 2;
        centerPlane.position.y = 0.28;
        boardPivot.add(centerPlane);

        for (var p = 0; p < 40; p++) {
            var w = posToWorld(p);
            var isCorner = CORNERS[p];
            var sz = isCorner ? 1.18 : 1.02;
            var name = tileNames[p] || ("Pole " + p);
            var effect = tileEffects[p] || "";
            var stripe = STRIPE[p];
            var variant = p === 0 ? "start" : (p === 10 ? "jail" : (p === 20 ? "parking" : (p === 30 ? "goto" : null)));
            var title = shortName(name, p);
            var price = parsePrice(effect, p);

            var sideMat = new THREE.MeshLambertMaterial({ color: 0xffffff });
            var tileBlock = new THREE.Mesh(
                new THREE.BoxGeometry(sz, 0.2, sz),
                sideMat
            );
            tileBlock.position.set(w.x, 0.15, w.z);
            tileBlock.castShadow = true;
            tileBlock.receiveShadow = true;
            boardPivot.add(tileBlock);

            var tex = makeTileTexture(title, price, stripe || (p === 10 ? "#C62828" : (p === 20 ? "#1565C0" : "#78909C")), variant);
            var face = new THREE.Mesh(
                new THREE.PlaneGeometry(sz * 0.96, sz * 0.96),
                new THREE.MeshBasicMaterial({ map: tex })
            );
            face.rotation.x = -Math.PI / 2;
            face.rotation.y = tileLabelRotation(p);
            face.position.set(w.x, 0.27, w.z);
            boardPivot.add(face);

            if (stripe && p !== 10 && p !== 0) {
                var bh = 0.28 + (p % 4) * 0.06;
                var bld = new THREE.Mesh(
                    new THREE.BoxGeometry(0.26, bh, 0.26),
                    new THREE.MeshLambertMaterial({ color: 0xffffff })
                );
                bld.position.set(w.x, 0.28 + bh / 2, w.z);
                bld.castShadow = true;
                boardPivot.add(bld);
                var roof = new THREE.Mesh(
                    new THREE.ConeGeometry(0.2, 0.14, 4),
                    new THREE.MeshLambertMaterial({ color: new THREE.Color(stripe) })
                );
                roof.position.set(w.x, 0.28 + bh + 0.08, w.z);
                roof.rotation.y = Math.PI / 4;
                boardPivot.add(roof);
            }
        }

        diceGroup = new THREE.Group();
        diceGroup.visible = false;
        diceGroup.position.y = 0.55;
        boardPivot.add(diceGroup);
    }

    function makeDieMesh(color) {
        var g = new THREE.Group();
        var box = new THREE.Mesh(
            new THREE.BoxGeometry(0.55, 0.55, 0.55),
            new THREE.MeshLambertMaterial({ color: color || 0xd32f2f })
        );
        box.castShadow = true;
        g.add(box);
        var pipMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
        [[0, 0.28, 0.12], [0.12, 0.28, 0], [-0.12, 0.28, 0], [0, 0.28, -0.12]].forEach(function (o) {
            var pip = new THREE.Mesh(new THREE.SphereGeometry(0.05, 8, 8), pipMat);
            pip.position.set(o[0], o[1], o[2]);
            g.add(pip);
        });
        return g;
    }

    function showBoardDice(rolling) {
        if (!diceGroup) return;
        while (diceGroup.children.length) diceGroup.remove(diceGroup.children[0]);
        var die1 = makeDieMesh(0xe53935);
        die1.position.set(-0.45, 0, 0);
        var die2 = makeDieMesh(0xc62828);
        die2.position.set(0.45, 0, 0);
        diceGroup.add(die1);
        diceGroup.add(die2);
        diceGroup.visible = true;
        diceGroup.userData.spin = !!rolling;
        if (!rolling) {
            die1.rotation.set(0.3, 0.5, 0.1);
            die2.rotation.set(-0.2, 0.8, 0.3);
        }
    }

    function hideBoardDice() {
        if (diceGroup) diceGroup.visible = false;
    }

    function playerOffset(index) {
        return { ox: (index % 3) * 0.24 - 0.24, oz: Math.floor(index / 3) * 0.2 };
    }

    function setPlayerMeshPosition(mesh, pos, index, y) {
        var w = posToWorld(pos);
        var off = playerOffset(index);
        mesh.position.set(w.x + off.ox, y != null ? y : 0.55, w.z + off.oz);
    }

    function upsertPlayerMesh(p, index, pos) {
        var key = String(p.id);
        var mesh = playerMeshes[key];
        var col = new THREE.Color(p.color || "#e91e63");
        if (!mesh) {
            mesh = new THREE.Group();
            var shadow = new THREE.Mesh(
                new THREE.CircleGeometry(0.16, 16),
                new THREE.MeshBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.25 })
            );
            shadow.rotation.x = -Math.PI / 2;
            shadow.position.y = 0.02;
            mesh.add(shadow);
            var body = new THREE.Mesh(
                new THREE.CylinderGeometry(0.12, 0.15, 0.38, 14),
                new THREE.MeshLambertMaterial({ color: col })
            );
            body.position.y = 0.22;
            body.castShadow = true;
            mesh.add(body);
            var head = new THREE.Mesh(
                new THREE.SphereGeometry(0.14, 14, 14),
                new THREE.MeshLambertMaterial({ color: col })
            );
            head.position.y = 0.48;
            head.castShadow = true;
            mesh.add(head);
            boardPivot.add(mesh);
            playerMeshes[key] = mesh;
        } else {
            mesh.children[1].material.color.copy(col);
            mesh.children[2].material.color.copy(col);
        }
        setPlayerMeshPosition(mesh, pos != null ? pos : p.position, index);
        return mesh;
    }

    function animateDice(d1, d2, done) {
        var el1 = document.getElementById("d1");
        var el2 = document.getElementById("d2");
        var toast = document.getElementById("diceToast");
        var toastSum = document.getElementById("diceToastSum");
        if (toast) toast.classList.add("visible");
        if (toastSum) toastSum.textContent = "…";
        el1.classList.add("die-rolling");
        el2.classList.add("die-rolling");
        showBoardDice(true);
        var step = 0;
        var iv = setInterval(function () {
            el1.textContent = Math.floor(Math.random() * 6) + 1;
            el2.textContent = Math.floor(Math.random() * 6) + 1;
            step++;
            if (step >= 18) {
                clearInterval(iv);
                el1.textContent = d1;
                el2.textContent = d2;
                if (toastSum) toastSum.textContent = String(d1 + d2);
                el1.classList.remove("die-rolling");
                el2.classList.remove("die-rolling");
                showBoardDice(false);
                setTimeout(function () {
                    hideBoardDice();
                    if (toast) toast.classList.remove("visible");
                    done();
                }, 450);
            }
        }, 70);
    }

    function buildPath(from, to) {
        var path = [];
        if (from === to) return path;
        var steps = (to - from + 40) % 40;
        if (steps === 0) steps = 40;
        for (var i = 1; i <= steps; i++) path.push((from + i) % 40);
        return path;
    }

    function animateMove(playerId, from, to, players, done) {
        if (from == null || to == null || from === to) { done(); return; }
        var mesh = playerMeshes[String(playerId)];
        if (!mesh) { done(); return; }
        var path = buildPath(from, to);
        var idx = players.findIndex(function (p) { return p.id === playerId; });
        if (idx < 0) idx = 0;
        var step = 0;

        function next() {
            if (step >= path.length) { done(); return; }
            var targetPos = path[step];
            var start = { x: mesh.position.x, y: mesh.position.y, z: mesh.position.z };
            var endW = posToWorld(targetPos);
            var off = playerOffset(idx);
            var end = { x: endW.x + off.ox, y: 0.85, z: endW.z + off.oz };
            var t0 = performance.now();
            var dur = 190;

            function tween(now) {
                var t = Math.min(1, (now - t0) / dur);
                var e = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
                mesh.position.x = start.x + (end.x - start.x) * e;
                mesh.position.z = start.z + (end.z - start.z) * e;
                mesh.position.y = start.y + (end.y - start.y) * e + Math.sin(t * Math.PI) * 0.35;
                if (t < 1) requestAnimationFrame(tween);
                else { mesh.position.y = 0.55; step++; next(); }
            }
            requestAnimationFrame(tween);
        }
        next();
    }

    function onResize() {
        var h = Math.max(container.clientHeight, 520);
        var aspect = container.clientWidth / h;
        camera.left = -orthoZoom * aspect;
        camera.right = orthoZoom * aspect;
        camera.top = orthoZoom;
        camera.bottom = -orthoZoom;
        camera.updateProjectionMatrix();
        renderer.setSize(container.clientWidth, h);
    }

    function animateLoop() {
        requestAnimationFrame(animateLoop);
        if (diceGroup && diceGroup.visible && diceGroup.userData.spin) {
            diceGroup.rotation.y += 0.14;
            diceGroup.children.forEach(function (d, i) {
                d.rotation.x += 0.22 + i * 0.04;
                d.rotation.z += 0.16;
            });
        }
        Object.keys(playerMeshes).forEach(function (k) {
            var m = playerMeshes[k];
            m.position.y = 0.55 + Math.abs(Math.sin(Date.now() * 0.003 + parseInt(k, 10))) * 0.04;
        });
        if (renderer && scene && camera) renderer.render(scene, camera);
    }

    function csrf() {
        var t = document.querySelector('meta[name="_csrf"]');
        var h = document.querySelector('meta[name="_csrf_header"]');
        return { header: h ? h.content : null, token: t ? t.content : null };
    }

    function authHeaders(json) {
        var c = csrf();
        var headers = {};
        if (json) headers["Content-Type"] = "application/json";
        if (c.header && c.token) headers[c.header] = c.token;
        return headers;
    }

    function formatCash(n) {
        return String(n == null ? 0 : n).replace(/\B(?=(\d{3})+(?!\d))/g, " ");
    }

    function escapeHtml(s) {
        var d = document.createElement("div");
        d.textContent = s;
        return d.innerHTML;
    }

    function fixIsMe(state) {
        if (myPlayerId == null) {
            state.players.forEach(function (p) { if (p.isMe) myPlayerId = p.id; });
        }
        state.players.forEach(function (p) { p.isMe = p.id === myPlayerId; });
        state.myTurn = state.currentTurnPlayerId === myPlayerId;
        return state;
    }

    function updateTileInfo(state) {
        var box = document.getElementById("tileInfo");
        if (!box || !state.players.length) return;
        var active = state.players.find(function (p) { return p.id === state.currentTurnPlayerId; });
        if (!active) return;
        var pos = active.position;
        var name = (state.tileNames && state.tileNames[pos]) || TILE_NAMES[pos] || "";
        var effect = (state.tileEffects && state.tileEffects[pos]) || TILE_EFFECTS[pos] || "";
        box.innerHTML = "<strong>Pole " + (pos + 1) + "/40:</strong> " + escapeHtml(name) +
            "<br><span class='muted'>" + escapeHtml(effect) + "</span>";
    }

    function renderHud(state) {
        state = fixIsMe(state);
        lastState = state;

        var box = document.getElementById("players");
        var cornerBox = document.getElementById("cornerPlayers");
        var sel = document.getElementById("toPlayer");
        if (box) box.innerHTML = "";
        if (cornerBox) cornerBox.innerHTML = "";
        sel.innerHTML = "";

        var corners = ["bt-corner-tl", "bt-corner-tr", "bt-corner-bl", "bt-corner-br"];

        state.players.forEach(function (p, idx) {
            var turn = state.currentTurnPlayerId === p.id;
            var chip = document.createElement("div");
            chip.className = "player-chip" + (turn ? " turn" : "");
            chip.innerHTML =
                '<span class="token-dot" style="background:' + p.color + '"></span>' +
                '<span class="player-name">' + escapeHtml(p.name) + (p.isMe ? " (Ty)" : "") + '</span>' +
                '<span class="cash">' + formatCash(p.cash) + ' zł</span>';
            if (box) box.appendChild(chip);

            if (cornerBox) {
                var card = document.createElement("div");
                card.className = "bt-player-card" + (turn ? " active" : "");
                card.style.borderColor = p.color;
                card.innerHTML =
                    '<div class="bt-player-avatar" style="background:' + p.color + '">' +
                    escapeHtml(p.name.charAt(0).toUpperCase()) + '</div>' +
                    '<div class="bt-player-meta">' +
                    '<strong>' + escapeHtml(p.name) + (p.isMe ? " · Ty" : "") + '</strong>' +
                    '<span>' + formatCash(p.cash) + ' zł</span>' +
                    '</div>';
                card.classList.add(corners[idx % 4]);
                cornerBox.appendChild(card);
            }
        });

        state.players.filter(function (p) { return p.id !== myPlayerId; }).forEach(function (p) {
            var opt = document.createElement("option");
            opt.value = p.id;
            opt.textContent = p.name + " (" + formatCash(p.cash) + " zł)";
            sel.appendChild(opt);
        });

        var online = document.getElementById("onlineCount");
        if (online) online.textContent = state.players.length;

        rollBtn.disabled = animating || !state.myTurn;
        if (state.message) document.getElementById("log").textContent = state.message;
        updateTileInfo(state);
    }

    function applyStateWithAnimation(state) {
        var key = eventKey(state);
        if (key === lastEventKey && state.movedPlayerId == null && !state.message) return;
        if (state.movedPlayerId != null || state.dice1 != null) lastEventKey = key;

        state = fixIsMe(state);
        var moved = state.movedPlayerId;
        var from = state.fromPosition;
        var to = state.toPosition;
        var hasMove = moved != null && from != null && to != null && from !== to;
        var hasDice = state.dice1 != null && state.dice2 != null;

        if (hasDice && hasMove && !animating) {
            animating = true;
            rollBtn.disabled = true;
            animateDice(state.dice1, state.dice2, function () {
                animateMove(moved, from, to, state.players, function () {
                    state.players.forEach(function (p, idx) { upsertPlayerMesh(p, idx, p.position); });
                    renderHud(state);
                    animating = false;
                });
            });
        } else {
            state.players.forEach(function (p, idx) { upsertPlayerMesh(p, idx, p.position); });
            renderHud(state);
            if (state.dice1 != null) {
                document.getElementById("d1").textContent = state.dice1;
                document.getElementById("d2").textContent = state.dice2;
            }
        }
    }

    function connectWebSocket() {
        if (typeof SockJS === "undefined" || typeof Stomp === "undefined") return;
        var socket = new SockJS("/ws/game");
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, function () {
            if (liveBadge) { liveBadge.textContent = "Online"; liveBadge.style.color = "var(--brand-emerald)"; }
            stompClient.subscribe("/topic/game/" + sessionId, function (msg) {
                applyStateWithAnimation(JSON.parse(msg.body));
            });
        }, function () {
            if (liveBadge) liveBadge.textContent = "Offline";
            setTimeout(connectWebSocket, 4000);
        });
    }

    function loadState() {
        fetch("/api/game/" + sessionId + "/state")
            .then(function (r) { return r.json(); })
            .then(function (state) {
                if (state.tileNames) {
                    TILE_NAMES = state.tileNames;
                    TILE_EFFECTS = state.tileEffects || [];
                    buildBoard(TILE_NAMES, TILE_EFFECTS);
                }
                applyStateWithAnimation(state);
            });
    }

    rollBtn.addEventListener("click", function () {
        if (animating || !lastState || !lastState.myTurn) return;
        rollBtn.disabled = true;
        fetch("/api/game/" + sessionId + "/roll", { method: "POST", headers: authHeaders(false) })
            .then(function (r) { return r.json().then(function (d) { return { ok: r.ok, data: d }; }); })
            .then(function (res) {
                if (!res.ok) {
                    document.getElementById("log").textContent = res.data.error || "Blad rzutu.";
                    rollBtn.disabled = lastState && lastState.myTurn && !animating;
                    return;
                }
                applyStateWithAnimation(res.data);
            })
            .catch(function () {
                if (lastState) rollBtn.disabled = lastState.myTurn && !animating;
            });
    });

    document.getElementById("transferBtn").addEventListener("click", function () {
        var errBox = document.getElementById("transferErr");
        errBox.style.display = "none";
        var toPlayerId = parseInt(document.getElementById("toPlayer").value, 10);
        var amount = parseInt(document.getElementById("amount").value, 10);
        if (!toPlayerId || !amount || amount <= 0) {
            errBox.textContent = "Podaj odbiorce i dodatnia kwote.";
            errBox.style.display = "block";
            return;
        }
        fetch("/api/game/" + sessionId + "/transfer", {
            method: "POST", headers: authHeaders(true),
            body: JSON.stringify({ toPlayerId: toPlayerId, amount: amount })
        })
            .then(function (r) { return r.json().then(function (d) { return { ok: r.ok, data: d }; }); })
            .then(function (res) {
                if (!res.ok) {
                    errBox.textContent = res.data.error || "Nie udalo sie przelac.";
                    errBox.style.display = "block";
                    return;
                }
                applyStateWithAnimation(res.data);
            });
    });

    initThree();
    loadState();
    connectWebSocket();
})();
