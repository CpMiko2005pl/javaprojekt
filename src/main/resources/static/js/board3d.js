/* Plansza Politechnika Monopoly - widok lekko pod skosem (Business Tour style),
   spojne ciemne sukno + drewniana rama, czytelne pola z opcja PNG, paski wlasciciela. */
(function () {
    "use strict";
    console.info("[board3d] build 20260614-pb-redesign");

    var main = document.getElementById("main");
    if (!main) return;

    var sessionId = main.dataset.session;
    /* BUG FIX: uzyj username z atrybutu HTML zamiast polegac na isMe z WS (ktore zawsze = false).
       Dzieki temu fixIsMe dziala poprawnie nawet gdy WS message przyjdzie przed REST fetch. */
    var myUsername = main.dataset.username || null;
    var myPlayerId = null;
    if (main.dataset.playerId) {
        myPlayerId = Number(main.dataset.playerId);
    }

    var container = document.getElementById("game-canvas-container");
    if (!container || typeof THREE === "undefined") return;

    var loader = document.getElementById("game-canvas-loader");
    var zoomInBtn = document.getElementById("zoom-in-btn");
    var zoomOutBtn = document.getElementById("zoom-out-btn");
    var rollBtn = document.getElementById("rollBtn");
    var liveBadge = document.getElementById("liveBadge");

    var TILE_NAMES = [];
    var TILE_EFFECTS = [];

    /* Slugi - mapowanie indeksu pola na nazwe pliku PNG w /img/tiles/<slug>.png */
    var TILE_IMAGE_KEYS = {
        0:  "start",
        1:  "akademik-alfa",
        2:  "stypendium",
        3:  "akademik-beta",
        4:  "podatek",
        5:  "dwor-mejera",
        6:  "akademik-gamma",
        7:  "szansa-usos",
        8:  "akademik-delta",
        9:  "akademik-epsilon",
        10: "dziekanat",
        11: "klub-gwint",
        12: "max-bistro",
        13: "gammajka",
        14: "klub-relax",
        15: "erasmus-pb",
        16: "acs",
        17: "stypendium",
        18: "korty-pb",
        19: "boisko-pb",
        20: "sesja",
        21: "wydzial-informatyki",
        22: "szansa-kolokwium",
        23: "wydzial-mechaniczny",
        24: "wydzial-elektryczny",
        25: "inno-eko-tech",
        26: "cnk",
        27: "biblioteka-pb",
        28: "bistro-pb",
        29: "radio-akadera",
        30: "idz-na-warunek",
        31: "centrum-sigma",
        32: "inkubator",
        33: "stypendium",
        34: "politechnet",
        35: "aula-duga",
        36: "szansa-kolokwium2",
        37: "wydzial-architektury",
        38: "podatek",
        39: "rektorat"
    };

    /* Kolor paska pola (kategoria) — zgodny z COLOR_GROUPS w GameEconomy. */
    var STRIPE = {
        1:  "#795548", 3:  "#795548",                    // brazowe
        6:  "#29B6F6", 8:  "#29B6F6", 9:  "#29B6F6",    // jasnoniebieskie
        11: "#9C27B0", 13: "#9C27B0", 14: "#9C27B0",    // fioletowe
        16: "#FF9800", 18: "#FF9800", 19: "#FF9800",    // pomaranczowe
        21: "#F44336", 23: "#F44336", 24: "#F44336",    // czerwone
        26: "#FFC107", 27: "#FFC107", 29: "#FFC107",    // zolte
        31: "#4CAF50", 32: "#4CAF50", 34: "#4CAF50",    // zielone
        37: "#1565C0", 39: "#1565C0"                     // granatowe
    };
    var CORNERS = { 0: true, 10: true, 20: true, 30: true };

    var scene, camera, renderer, boardPivot, diceGroup;
    var playerMeshes = {};
    var ownerMarkers = {};
    var tileFaces = {};
    var tileBlocks = {};
    var lastState = null;
    var lastChanceKey = "";
    var animating = false;
    var rollInFlight = false;
    var stompClient = null;
    var lastEventKey = "";
    var hudSeq = 0;
    var camDistance = 18;

    /* Stan kamery kinowej (Business Tour): wartosci docelowe + biezace,
       interpolowane co klatke. tx/ty/tz to punkt, na ktory patrzy kamera,
       distance to dystans od tego punktu, yaw to obrot wokol osi Y. */
    var camState = {
        tx: 0, tz: 0, dist: 18, tilt: 0.55, yaw: 0,
        ttx: 0, ttz: 0, tdist: 18, ttilt: 0.55, tyaw: 0,
        followPlayerId: null,
        cinematic: false, /* gdy true - mocniejszy zoom in na pionek */
        userPanned: false  /* gdy true - kamera nie auto-centruje sie */
    };

    /* Cache wczytanych obrazow PNG - zwracamy <img> gdy gotowe. */
    var imageCache = {};
    /* Tekstury per pole, by mozna bylo aktualizowac po zaladowaniu PNG. */
    var tileMaterials = {};

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

    /* Obrot tekstury pola tak, by tekst byl czytelny patrzac w strone srodka planszy. */
    function tileTextureRotation(pos) {
        if (pos === 0 || pos === 10 || pos === 20 || pos === 30) return 0;
        if (pos < 10) return 0;
        if (pos < 20) return -Math.PI / 2;
        if (pos < 30) return Math.PI;
        return Math.PI / 2;
    }

    function shortName(name, pos) {
        if (pos === 0) return "START";
        if (pos === 10) return "DZIEKANAT";
        if (pos === 20) return "PARKING";
        if (pos === 30) return "DO DZIEKANATU";
        if (pos === 39) return "META";
        var s = (name || "").split(" - ")[0].split(" \u2014 ")[0];
        return s;
    }

    function parsePrice(effect, pos) {
        if (pos === 0) return "+300 000 PLN";
        if (pos === 10) return "Lapowka 200 000 PLN";
        if (pos === 20) return "Postoj";
        if (pos === 30) return "-> Dziekanat";
        if (pos === 39) return "+300 000 PLN";
        if (!effect) return "";
        if (effect.indexOf("Szans") >= 0 || effect.indexOf("Losuj") >= 0) return "SZANSA";
        if (effect.indexOf("Resort") >= 0 || effect.indexOf("Dworzec") >= 0) return "RESORT";
        if (effect.indexOf("Stypendium") >= 0) return "+150 000 PLN";
        if (effect.indexOf("Podatek") >= 0) return "PODATEK 10%";
        if (effect.indexOf("Oplata") >= 0) {
            var m = effect.match(/(\d+)/);
            return m ? "Oplata " + m[1] + " PLN" : "OPLATA";
        }
        var mm = effect.match(/(\d[\d ]*)\s*PLN/);
        return mm ? mm[1].replace(/ /g, " ") + " PLN" : "";
    }

    /* Tekstura pola w stylu top-down: bialy kafelek, pasek koloru, miejsce na zdjecie + tytul + cena. */
    function makeTileTexture(pos, title, price, stripeHex, variant, image) {
        var canvas = document.createElement("canvas");
        canvas.width = 384;
        canvas.height = 384;
        var ctx = canvas.getContext("2d");

        ctx.fillStyle = "#ffffff";
        ctx.fillRect(0, 0, 384, 384);

        ctx.fillStyle = stripeHex || "#90A4AE";
        ctx.fillRect(0, 0, 384, 70);

        ctx.strokeStyle = "#37474F";
        ctx.lineWidth = 6;
        ctx.strokeRect(3, 3, 378, 378);

        var imageArea = { x: 24, y: 88, w: 336, h: 200 };
        ctx.fillStyle = "#ECEFF1";
        ctx.fillRect(imageArea.x, imageArea.y, imageArea.w, imageArea.h);

        if (image && image.complete && image.naturalWidth > 0) {
            var imgRatio = image.naturalWidth / image.naturalHeight;
            var areaRatio = imageArea.w / imageArea.h;
            var dw, dh, dx, dy;
            if (imgRatio > areaRatio) {
                dw = imageArea.w;
                dh = imageArea.w / imgRatio;
            } else {
                dh = imageArea.h;
                dw = imageArea.h * imgRatio;
            }
            dx = imageArea.x + (imageArea.w - dw) / 2;
            dy = imageArea.y + (imageArea.h - dh) / 2;
            try { ctx.drawImage(image, dx, dy, dw, dh); }
            catch (e) { drawPlaceholderIcon(ctx, imageArea, variant, pos); }
        } else {
            drawPlaceholderIcon(ctx, imageArea, variant, pos);
        }

        ctx.fillStyle = "#0D47A1";
        ctx.font = "bold 28px 'Plus Jakarta Sans', Arial, sans-serif";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        wrapFill(ctx, title.toUpperCase(), 192, 312, 340, 28);

        if (price) {
            ctx.fillStyle = "#1B5E20";
            ctx.font = "bold 26px 'Plus Jakarta Sans', Arial, sans-serif";
            ctx.fillText(price, 192, 356);
        }

        ctx.fillStyle = "rgba(0,0,0,.5)";
        ctx.font = "bold 16px Arial,sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(String(pos), 14, 88);

        var tex = new THREE.CanvasTexture(canvas);
        tex.anisotropy = 16;
        tex.needsUpdate = true;
        return tex;
    }

    function drawPlaceholderIcon(ctx, area, variant, pos) {
        ctx.save();
        var cx = area.x + area.w / 2;
        var cy = area.y + area.h / 2;
        ctx.fillStyle = "#90A4AE";
        ctx.font = "bold 80px 'Font Awesome 6 Free', Arial, sans-serif";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        var icon = "?";
        if (variant === "start") icon = "S";
        else if (variant === "jail") icon = "DZ";
        else if (variant === "parking") icon = "P";
        else if (variant === "goto") icon = "->";
        else if (variant === "meta") icon = "M";
        else if (pos === 5 || pos === 15 || pos === 25 || pos === 35) icon = "PKP";
        else if (pos === 7 || pos === 22 || pos === 36) icon = "?";
        else if (pos === 2 || pos === 17 || pos === 33) icon = "$";
        else if (pos === 28) icon = "WC";
        else icon = "PB";
        ctx.fillText(icon, cx, cy);
        ctx.restore();
    }

    function wrapFill(ctx, text, cx, y, maxW, lineH) {
        var words = String(text).split(" ");
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
        lines = lines.slice(0, 2);
        var startY = y - ((lines.length - 1) * lineH) / 2;
        lines.forEach(function (ln, i) { ctx.fillText(ln, cx, startY + i * lineH); });
    }

    function loadTileImage(pos, onReady) {
        var key = TILE_IMAGE_KEYS[pos];
        if (!key) { onReady(null); return; }
        if (imageCache[key] === "missing") { onReady(null); return; }
        if (imageCache[key] && imageCache[key].complete) { onReady(imageCache[key]); return; }

        var img = new Image();
        img.crossOrigin = "anonymous";
        img.onload = function () {
            imageCache[key] = img;
            onReady(img);
        };
        img.onerror = function () {
            imageCache[key] = "missing";
            onReady(null);
        };
        img.src = "/img/tiles/" + key + ".png";
    }

    function buildTileMaterial(pos, title, price, stripeHex, variant) {
        var mat = new THREE.MeshBasicMaterial({
            map: makeTileTexture(pos, title, price, stripeHex, variant, null)
        });
        tileMaterials[pos] = { mat: mat };
        loadTileImage(pos, function (img) {
            if (!img) return;
            var newTex = makeTileTexture(pos, title, price, stripeHex, variant, img);
            if (mat.map) mat.map.dispose();
            mat.map = newTex;
            mat.needsUpdate = true;
        });
        return mat;
    }

    function isDarkMode() {
        return document.documentElement.classList.contains("pb-dark");
    }

    function applySceneTheme() {
        if (!scene) return;
        var dark = isDarkMode();
        scene.background = new THREE.Color(dark ? 0x05060c : 0xe9f2ec);
    }

    function initThree() {
        scene = new THREE.Scene();
        var dark = isDarkMode();
        scene.background = new THREE.Color(dark ? 0x05060c : 0xe9f2ec);

        var aspect = container.clientWidth / Math.max(container.clientHeight, 520);
        camera = new THREE.PerspectiveCamera(38, aspect, 0.1, 200);
        positionCamera();

        renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
        renderer.setSize(container.clientWidth, Math.max(container.clientHeight, 520));
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        container.appendChild(renderer.domElement);

        /* Nasłuchuj zmiany motywu i aktualizuj tło sceny na żywo */
        new MutationObserver(applySceneTheme).observe(
            document.documentElement, { attributeFilter: ["class"] }
        );

        var lightTheme = !dark;
        scene.add(new THREE.HemisphereLight(0xffffff, lightTheme ? 0xcfe3d8 : 0x223344, lightTheme ? 0.85 : 0.7));
        var sun = new THREE.DirectionalLight(0xffffff, 0.85);
        sun.position.set(8, 22, 12);
        sun.castShadow = true;
        sun.shadow.mapSize.width = 1024;
        sun.shadow.mapSize.height = 1024;
        sun.shadow.camera.left = -10;
        sun.shadow.camera.right = 10;
        sun.shadow.camera.top = 10;
        sun.shadow.camera.bottom = -10;
        scene.add(sun);

        boardPivot = new THREE.Group();
        scene.add(boardPivot);

        window.addEventListener("resize", onResize);
        if (zoomInBtn) zoomInBtn.addEventListener("click", function () { setZoom(camDistance - 2.5); });
        if (zoomOutBtn) zoomOutBtn.addEventListener("click", function () { setZoom(camDistance + 2.5); });

        attachCameraControls();
        animateLoop();
        if (loader) {
            setTimeout(function () {
                loader.classList.add("hidden");
                setTimeout(function () { loader.style.display = "none"; }, 350);
            }, 350);
        }
    }

    function positionCamera() {
        /* Lekki ukos w stylu Business Tour: kamera podaza za pionkiem,
           interpolacja prowadzona w animateLoop(). */
        applyCameraTransform();
    }

    function applyCameraTransform() {
        var d = camState.dist;
        var tilt = camState.tilt;
        var yaw = camState.yaw;
        /* Offset kamery od targetu: za polozeniem (tilt na osi Z) i nad ziemia (Y).
           Yaw obraca offset wokol Y, dzieki czemu mozna obracac plansze. */
        var offX = d * tilt * Math.sin(yaw);
        var offZ = d * tilt * Math.cos(yaw);
        camera.position.x = camState.tx + offX;
        camera.position.y = d * (0.9 + tilt * 0.05);
        camera.position.z = camState.tz + offZ;
        camera.lookAt(camState.tx, 0, camState.tz);
    }

    function setZoom(z) {
        /* Zakres dist: 5 (mocny zoom in - widac szczegoly pol) do 30 (lot ptaka). */
        var newDist = Math.max(5, Math.min(z, 30));
        camState.tdist = newDist;
        camDistance = newDist;
    }

    function buildBoard(tileNames, tileEffects) {
        while (boardPivot.children.length) boardPivot.remove(boardPivot.children[0]);
        tileMaterials = {};
        ownerMarkers = {};
        tileFaces = {};
        tileBlocks = {};

        /* Jednolite drewniane podloze - bez zadnych jasnych ramek/wkladek. */
        var feltTex = makeFeltTexture();
        var base = new THREE.Mesh(
            new THREE.BoxGeometry(13.6, 0.4, 13.6),
            new THREE.MeshLambertMaterial({ map: feltTex, color: 0x2e7d32 })
        );
        base.position.y = -0.18;
        base.receiveShadow = true;
        boardPivot.add(base);

        /* Drewniana rama wokol planszy - ciemny mahoniowy odcien. */
        var frame = new THREE.Mesh(
            new THREE.BoxGeometry(14.4, 0.3, 14.4),
            new THREE.MeshLambertMaterial({ color: 0x4e342e })
        );
        frame.position.y = -0.32;
        frame.receiveShadow = true;
        boardPivot.add(frame);

        var centerPlane = new THREE.Mesh(
            new THREE.PlaneGeometry(8.4, 8.4),
            new THREE.MeshBasicMaterial({ map: makeCenterTexture(), transparent: true })
        );
        centerPlane.rotation.x = -Math.PI / 2;
        centerPlane.position.y = 0.04;
        boardPivot.add(centerPlane);

        for (var p = 0; p < 40; p++) {
            var w = posToWorld(p);
            var isCorner = CORNERS[p];
            var sz = isCorner ? 1.12 : 1.02;
            var name = tileNames[p] || ("Pole " + p);
            var effect = tileEffects[p] || "";
            var stripe = STRIPE[p];
            var variant = p === 0 ? "start" : (p === 10 ? "jail" : (p === 20 ? "parking" : (p === 30 ? "goto" : (p === 39 ? "meta" : null))));
            var title = shortName(name, p);
            var price = parsePrice(effect, p);
            var stripeColor = stripe || (p === 10 ? "#C62828" : (p === 0 || p === 39 ? "#2E7D32" : (p === 20 ? "#1565C0" : (p === 30 ? "#E65100" : (p === 5 || p === 15 || p === 25 || p === 35 ? "#37474F" : (p === 7 || p === 22 || p === 36 ? "#7B1FA2" : (p === 2 || p === 17 || p === 33 ? "#FBC02D" : "#90A4AE")))))));

            var faceMat = buildTileMaterial(p, title, price, stripeColor, variant);
            var sideMat = new THREE.MeshLambertMaterial({ color: 0xffffff });

            /* Przesun rogle pola na zewnatrz o polowe nadmiaru, by nie nachodziły na sąsiednie */
            var tx = w.x, tz = w.z;
            if (isCorner) {
                var shift = (sz - 1.02) / 2;
                tx += (w.col >= 5 ? shift : -shift);
                tz += (w.row >= 5 ? shift : -shift);
            }

            var tileBlock = new THREE.Mesh(
                new THREE.BoxGeometry(sz * 0.99, 0.18, sz * 0.99),
                sideMat
            );
            tileBlock.position.set(tx, 0.09, tz);
            tileBlock.castShadow = true;
            tileBlock.receiveShadow = true;
            boardPivot.add(tileBlock);
            tileBlocks[p] = tileBlock;

            var face = new THREE.Mesh(
                new THREE.PlaneGeometry(sz * 0.96, sz * 0.96),
                faceMat
            );
            face.rotation.x = -Math.PI / 2;
            face.rotation.z = tileTextureRotation(p);
            face.position.set(tx, 0.19, tz);
            boardPivot.add(face);
            tileFaces[p] = face;
        }

        diceGroup = new THREE.Group();
        diceGroup.visible = false;
        diceGroup.position.y = 0.55;
        boardPivot.add(diceGroup);
    }

    /* Niewielka tekstura sukna planszy - subtelne wlokna zamiast plaskiej zieleni. */
    function makeFeltTexture() {
        var c = document.createElement("canvas");
        c.width = 256; c.height = 256;
        var ctx = c.getContext("2d");
        ctx.fillStyle = "#1f5f2c";
        ctx.fillRect(0, 0, 256, 256);
        for (var i = 0; i < 600; i++) {
            ctx.fillStyle = "rgba(" + (50 + Math.random() * 30) + "," + (120 + Math.random() * 50) + "," + (50 + Math.random() * 30) + ",0.18)";
            ctx.fillRect(Math.random() * 256, Math.random() * 256, 2, 1);
        }
        var t = new THREE.CanvasTexture(c);
        t.wrapS = THREE.RepeatWrapping;
        t.wrapT = THREE.RepeatWrapping;
        t.repeat.set(6, 6);
        return t;
    }

    /* Tekstura logo na srodku planszy - odpowiada zielonemu suknu. */
    function makeCenterTexture() {
        var c = document.createElement("canvas");
        c.width = 1024; c.height = 1024;
        var ctx = c.getContext("2d");
        /* Przezroczyste tlo - widac sukno. */
        ctx.clearRect(0, 0, 1024, 1024);
        /* Cieniowane logo dla czytelnosci. */
        ctx.save();
        ctx.translate(512, 360);
        ctx.rotate(-Math.PI / 4);
        ctx.font = "bold 110px 'Plus Jakarta Sans', Arial, sans-serif";
        ctx.textAlign = "center";
        ctx.fillStyle = "rgba(0,0,0,0.35)";
        ctx.fillText("POLITECHNIKA", 6, 6);
        ctx.fillStyle = "#fff8e1";
        ctx.fillText("POLITECHNIKA", 0, 0);
        ctx.fillStyle = "rgba(0,0,0,0.35)";
        ctx.fillText("MONOPOLY", 6, 126);
        ctx.fillStyle = "#ffd54f";
        ctx.fillText("MONOPOLY", 0, 120);
        ctx.restore();
        ctx.save();
        ctx.translate(512, 720);
        ctx.rotate(-Math.PI / 4);
        ctx.font = "bold 56px Arial,sans-serif";
        ctx.textAlign = "center";
        ctx.fillStyle = "rgba(255,255,255,0.85)";
        ctx.fillText("Kampus PB - Edycja 2026", 0, 0);
        ctx.restore();
        return new THREE.CanvasTexture(c);
    }

    function makeDieMesh(color) {
        var g = new THREE.Group();
        var box = new THREE.Mesh(
            new THREE.BoxGeometry(0.6, 0.6, 0.6),
            new THREE.MeshLambertMaterial({ color: color || 0xd32f2f })
        );
        box.castShadow = true;
        g.add(box);
        var pipMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
        [[0, 0.31, 0.14], [0.14, 0.31, 0], [-0.14, 0.31, 0], [0, 0.31, -0.14]].forEach(function (o) {
            var pip = new THREE.Mesh(new THREE.SphereGeometry(0.05, 8, 8), pipMat);
            pip.position.set(o[0], o[1], o[2]);
            g.add(pip);
        });
        return g;
    }

    function showBoardDice(rolling, atPos) {
        if (!diceGroup) return;
        /* Kostki przy pionku rzucajacego (kamera tam patrzy) zamiast na srodku planszy —
           dzieki temu zawsze widac rzut, nawet przy zoomie na gracza. */
        if (atPos != null) {
            var w = posToWorld(atPos);
            diceGroup.position.set(w.x, 0.95, w.z);
        } else {
            diceGroup.position.set(0, 0.55, 0);
        }
        while (diceGroup.children.length) diceGroup.remove(diceGroup.children[0]);
        var die1 = makeDieMesh(0xe53935);
        die1.position.set(-0.5, 0, 0);
        var die2 = makeDieMesh(0xc62828);
        die2.position.set(0.5, 0, 0);
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
        return { ox: (index % 3) * 0.22 - 0.22, oz: Math.floor(index / 3) * 0.22 - 0.11 };
    }

    function setPlayerMeshPosition(mesh, pos, index, y) {
        var w = posToWorld(pos);
        var off = playerOffset(index);
        mesh.position.set(w.x + off.ox, y != null ? y : 0.4, w.z + off.oz);
    }

    /* ===== PIONKI 3D ze skrzynki — kazdy model to osobny GLB w /models/.
       Fallback (model jeszcze sie laduje / blad): dotychczasowy walec + kula. ===== */
    var pawnTemplates = {};      /* url -> znormalizowany wzorzec GLB */
    var pawnLoadsPending = {};   /* url -> [callback] */
    var PAWN_PRELOAD_PATHS = [
        "/models/CornPawn.glb", "/models/PawnSteve.glb", "/models/PawnCreeper.glb",
        "/models/PawnEnderman.glb", "/models/PawnSkeleton.glb", "/models/PawnPiglin.glb",
        "/models/PawnPillager.glb", "/models/PawnPenguin.glb", "/models/PawnGoblin.glb"
    ];
    var PAWN_HEIGHT = 0.62;

    function threeParent(obj) {
        return obj && (obj.parent || obj.parentNode);
    }

    function normalizePawnScene(model) {
        // Wymus aktualizacje macierzy swiatowej przed policzeniem bounding box
        model.updateMatrixWorld(true);
        var box = new THREE.Box3().setFromObject(model);
        var size = new THREE.Vector3();
        box.getSize(size);
        var maxDim = Math.max(size.x, size.y, size.z, 0.0001);
        // Normalizuj wzgledem najwyzszego wymiaru zeby model nie byl splacony
        model.scale.setScalar(PAWN_HEIGHT / Math.max(size.y > 0.01 ? size.y : maxDim, 0.0001));
        model.updateMatrixWorld(true);
        box.setFromObject(model);
        var center = new THREE.Vector3();
        box.getCenter(center);
        model.position.x -= center.x;
        model.position.z -= center.z;
        model.position.y -= box.min.y;
        model.traverse(function (o) {
            if (o.isMesh) {
                o.castShadow = true;
                o.receiveShadow = false;
                // Upewnij sie ze material jest widoczny z obu stron (glTF czasem: single-sided)
                if (o.material) {
                    var mats = Array.isArray(o.material) ? o.material : [o.material];
                    mats.forEach(function (m) {
                        if (m.side === THREE.BackSide) m.side = THREE.DoubleSide;
                    });
                }
            }
        });
        var grp = new THREE.Group();
        grp.add(model);
        return grp;
    }

    function refreshAllPawnVisuals() {
        Object.keys(playerMeshes).forEach(function (key) {
            var group = playerMeshes[key];
            setPawnVisual(group, group.userData.color, group.userData.pawnModel);
        });
    }

    function ensurePawnTemplate(url, callback) {
        if (!url) return;
        if (pawnTemplates[url]) {
            callback(pawnTemplates[url]);
            return;
        }
        if (!pawnLoadsPending[url]) pawnLoadsPending[url] = [];
        pawnLoadsPending[url].push(callback);
        if (pawnLoadsPending[url].length > 1) return;
        if (typeof THREE.GLTFLoader === "undefined") return;
        var loader = new THREE.GLTFLoader();
        loader.load(url, function (gltf) {
            pawnTemplates[url] = normalizePawnScene(gltf.scene.clone(true));
            var cbs = pawnLoadsPending[url] || [];
            delete pawnLoadsPending[url];
            cbs.forEach(function (cb) { cb(pawnTemplates[url]); });
            refreshAllPawnVisuals();
        }, undefined, function (err) {
            console.warn("[board3d] Nie udalo sie zaladowac pionka:", url, err);
            delete pawnLoadsPending[url];
        });
    }

    function loadPawnModels() {
        if (typeof THREE.GLTFLoader === "undefined") {
            console.warn("[board3d] GLTFLoader niedostepny — pionki 3D wylaczone");
            return;
        }
        PAWN_PRELOAD_PATHS.forEach(function (url) {
            ensurePawnTemplate(url, function () { /* preload */ });
        });
    }

    function tintPawnClone(group, colorHex) {
        var col = new THREE.Color(colorHex || "#e91e63");
        group.traverse(function (o) {
            if (!o.isMesh || !o.material) return;
            var mats = Array.isArray(o.material) ? o.material : [o.material];
            var cloned = mats.map(function (m) {
                var mat = m.clone();
                if (mat.emissive) mat.emissive.copy(col).multiplyScalar(0.22);
                else if (mat.color) mat.color.lerp(col, 0.12);
                return mat;
            });
            o.material = cloned.length === 1 ? cloned[0] : cloned;
        });
    }

    /* Buduje/wymienia widok pionka w grupie gracza (dziecko [0] = cien, reszta = pionek). */
    function setPawnVisual(group, colorHex, pawnModelUrl) {
        var col = new THREE.Color(colorHex || "#e91e63");
        var modelUrl = pawnModelUrl || null;

        function buildVisual(template) {
            // Usun stare wizualizacje (zachowaj cien na indeksie 0)
            for (var i = group.children.length - 1; i >= 1; i--) {
                group.remove(group.children[i]);
            }
            var visual = new THREE.Group();
            if (template) {
                var clone = template.clone(true);
                tintPawnClone(clone, colorHex);
                visual.add(clone);
                // Mala podstawa walcowa pod modelem
                var base = new THREE.Mesh(
                    new THREE.CylinderGeometry(0.19, 0.21, 0.05, 20),
                    new THREE.MeshLambertMaterial({ color: col })
                );
                base.position.y = 0.025;
                base.castShadow = true;
                visual.add(base);
            } else {
                // Fallback walec + kula (klasyczny pionek)
                var body = new THREE.Mesh(
                    new THREE.CylinderGeometry(0.13, 0.17, 0.42, 16),
                    new THREE.MeshLambertMaterial({ color: col })
                );
                body.position.y = 0.21;
                body.castShadow = true;
                visual.add(body);
                var head = new THREE.Mesh(
                    new THREE.SphereGeometry(0.14, 16, 16),
                    new THREE.MeshLambertMaterial({ color: col })
                );
                head.position.y = 0.5;
                head.castShadow = true;
                visual.add(head);
            }
            group.add(visual);
            group.userData.color = colorHex;
            group.userData.pawnModel = modelUrl;
        }

        if (modelUrl) {
            if (!pawnTemplates[modelUrl]) {
                // Model jeszcze sie laduje — zbuduj fallback natychmiast, potem zaaktualizuj
                buildVisual(null);
                ensurePawnTemplate(modelUrl, function (template) {
                    // Odbuduj z prawdziwym modelem gdy zaladowany
                    if (group.userData.pawnModel === modelUrl) {
                        buildVisual(template);
                    }
                });
                group.userData.pawnModel = modelUrl;
                return;
            }
            buildVisual(pawnTemplates[modelUrl]);
            return;
        }
        buildVisual(null);
    }

    function upsertPlayerMesh(p, index, pos) {
        var key = String(p.id);
        var mesh = playerMeshes[key];
        if (!mesh) {
            mesh = new THREE.Group();
            var shadow = new THREE.Mesh(
                new THREE.CircleGeometry(0.18, 16),
                new THREE.MeshBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.28 })
            );
            shadow.rotation.x = -Math.PI / 2;
            shadow.position.y = 0.005;
            mesh.add(shadow);
            setPawnVisual(mesh, p.color, p.pawnModel);
            boardPivot.add(mesh);
            playerMeshes[key] = mesh;
        } else if (mesh.userData.color !== p.color || mesh.userData.pawnModel !== (p.pawnModel || null)) {
            setPawnVisual(mesh, p.color, p.pawnModel);
        }
        /* Nie nadpisuj pozycji animowanego pionka - inaczej teleportuje sie podczas hopu. */
        if (!(mesh.userData && mesh.userData.animating)) {
            setPlayerMeshPosition(mesh, pos != null ? pos : p.position, index);
        }
        return mesh;
    }

    function animateDice(d1, d2, done, atPos) {
        /* Referencje do nowego badge wynikowego (nie ma juz #d1/#d2 w canvas-shell). */
        var badge   = document.getElementById("diceBadge");
        var bdD1    = document.getElementById("diceBadgeD1");
        var bdD2    = document.getElementById("diceBadgeD2");
        var bdSum   = document.getElementById("diceBadgeSum");
        /* Stary toast (diceToast) nadal istnieje w HTML - mozemy go tez chowac. */
        var toast   = document.getElementById("diceToast");
        var toastSum= document.getElementById("diceToastSum");
        if (toast) toast.classList.remove("visible");
        /* Startuj 3D kostki na planszy — przy pionku rzucajacego. */
        showBoardDice(true, atPos);
        if (badge) { badge.classList.remove("visible"); }
        if (bdD1) bdD1.textContent = "?";
        if (bdD2) bdD2.textContent = "?";
        if (bdSum) bdSum.textContent = "?";
        /* Gdy w kolejce czekaja kolejne ruchy (np. kilka botow), kreci szybciej —
           dzieki temu rozgrywka nie jest "opozniona" przy wielu graczach. */
        var fast = animationQueue.length > 0;
        var maxStep = fast ? 7 : 12;
        var tick = fast ? 40 : 55;
        var endDelay = fast ? 230 : 360;
        var step = 0;
        var iv = setInterval(function () {
            var r1 = Math.floor(Math.random() * 6) + 1;
            var r2 = Math.floor(Math.random() * 6) + 1;
            if (bdD1) bdD1.textContent = r1;
            if (bdD2) bdD2.textContent = r2;
            if (bdSum) bdSum.textContent = r1 + r2;
            step++;
            if (step >= maxStep) {
                clearInterval(iv);
                if (bdD1) bdD1.textContent = d1;
                if (bdD2) bdD2.textContent = d2;
                if (bdSum) bdSum.textContent = String(d1 + d2);
                if (toastSum) toastSum.textContent = String(d1 + d2);
                showBoardDice(false);
                /* Pokaż badge z wynikiem. */
                if (badge) badge.classList.add("visible");
                setTimeout(function () {
                    hideBoardDice();
                    /* Schowaj badge po zakończeniu animacji ruchu (zrobimy to w done-callback). */
                    done();
                }, endDelay);
            }
        }, tick);
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
        /* Szybciej, gdy kolejne ruchy czekaja w kolejce (kilku graczy/botow). */
        var fastMove = animationQueue.length > 0;
        var hopDur = fastMove ? 190 : 300;
        var hopGap = fastMove ? 25 : 55;

        /* Wlacz tryb kinowy: kamera podaza za pionkiem ze zblizeniem.
           Nowy ruch -> resetujemy pan/rotacje uzytkownika (recentruje sie). */
        camState.followPlayerId = playerId;
        camState.cinematic = true;
        camState.userPanned = false;
        mesh.userData = mesh.userData || {};
        mesh.userData.animating = true;

        function next() {
            if (step >= path.length) {
                mesh.userData.animating = false;
                /* Czekaj na decyzje gracza - jezeli backend ustawil pendingPurchase,
                   kamera trzyma sie pionka az decyzja zostanie podjeta.
                   Wpp. po krotkiej chwili oddalamy widok. */
                setTimeout(function () {
                    var stillPending = lastState && (
                        (lastState.pendingPurchase && lastState.pendingPurchase.deciderId === playerId) ||
                        (lastState.pendingPayment && lastState.pendingPayment.debtorId === playerId)
                    );
                    if (stillPending) {
                        /* Zostawiamy followPlayerId i cinematic = true,
                           updateCameraTarget bedzie podtrzymywac sledzenie. */
                        return;
                    }
                    camState.cinematic = false;
                    if (camState.followPlayerId === playerId) {
                        camState.followPlayerId = null;
                    }
                }, 900);
                done();
                return;
            }
            var targetPos = path[step];
            var start = { x: mesh.position.x, y: mesh.position.y, z: mesh.position.z };
            var endW = posToWorld(targetPos);
            var off = playerOffset(idx);
            var end = { x: endW.x + off.ox, y: 0.4, z: endW.z + off.oz };
            var t0 = performance.now();
            /* Hop dla efektu Business Tour - ladnie widac skok i ladowanie. */
            var dur = hopDur;
            /* Wysokosc skoku rosnie dla dluzszych ruchow - bardziej dynamiczne. */
            var jumpH = 0.55;

            function tween(now) {
                var t = Math.min(1, (now - t0) / dur);
                /* easeInOutQuad dla plynnego startu i ladowania. */
                var e = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
                mesh.position.x = start.x + (end.x - start.x) * e;
                mesh.position.z = start.z + (end.z - start.z) * e;
                /* Skok sinusoidalny: 0->szczyt->0 (jak w Business Tour). */
                mesh.position.y = 0.4 + Math.sin(t * Math.PI) * jumpH;
                if (t < 1) requestAnimationFrame(tween);
                else {
                    mesh.position.y = 0.4;
                    /* Krotka pauza pomiedzy skokami - wyrazny "lap" na polu. */
                    setTimeout(function () { step++; next(); }, hopGap);
                }
            }
            requestAnimationFrame(tween);
        }
        next();
    }

    function onResize() {
        var h = Math.max(container.clientHeight, 520);
        var aspect = container.clientWidth / h;
        camera.aspect = aspect;
        camera.updateProjectionMatrix();
        renderer.setSize(container.clientWidth, h);
    }

    function lerp(a, b, t) { return a + (b - a) * t; }

    /* Sterowanie kamera: drag = przesuwanie, prawy/shift = obrot, scroll = zoom. */
    function attachCameraControls() {
        var dragging = false;
        var rotating = false;
        var lastX = 0, lastY = 0;

        container.addEventListener("contextmenu", function (e) { e.preventDefault(); });

        container.addEventListener("mousedown", function (e) {
            lastX = e.clientX; lastY = e.clientY;
            if (e.button === 2 || e.shiftKey) {
                rotating = true;
                container.classList.add("rotating");
            } else if (e.button === 0) {
                dragging = true;
                container.classList.add("dragging");
            }
            camState.userPanned = true;
            e.preventDefault();
        });

        window.addEventListener("mousemove", function (e) {
            if (!dragging && !rotating) return;
            var dx = e.clientX - lastX;
            var dy = e.clientY - lastY;
            lastX = e.clientX; lastY = e.clientY;
            var d = camState.dist;
            if (rotating) {
                /* Obrot wokol osi Y: poziome przesuniecie myszy. */
                camState.tyaw -= dx * 0.008;
                camState.yaw -= dx * 0.008;
                /* Pionowe przesuniecie zmienia tilt (kat patrzenia). */
                var newTilt = Math.max(0.15, Math.min(camState.ttilt + dy * 0.004, 0.95));
                camState.ttilt = newTilt;
                camState.tilt = newTilt;
            } else if (dragging) {
                /* Pan: przesuwanie celu kamery w plaszczyznie XZ z uwzglednieniem yaw. */
                var panSpeed = d * 0.0022;
                var cos = Math.cos(camState.yaw);
                var sin = Math.sin(camState.yaw);
                camState.ttx -= (dx * cos - dy * sin) * panSpeed;
                camState.ttz -= (dx * sin + dy * cos) * panSpeed;
                camState.tx = camState.ttx;
                camState.tz = camState.ttz;
            }
        });

        function endDrag() {
            dragging = false;
            rotating = false;
            container.classList.remove("dragging");
            container.classList.remove("rotating");
        }
        window.addEventListener("mouseup", endDrag);
        window.addEventListener("blur", endDrag);

        container.addEventListener("wheel", function (e) {
            e.preventDefault();
            var step = e.deltaY > 0 ? 1.5 : -1.5;
            setZoom(camDistance + step);
        }, { passive: false });

        /* Touch: 1 palec = pan, 2 palce = pinch zoom + rotacja. */
        var touchStart = null;
        container.addEventListener("touchstart", function (e) {
            if (e.touches.length === 1) {
                touchStart = { x: e.touches[0].clientX, y: e.touches[0].clientY, mode: "pan" };
                camState.userPanned = true;
            } else if (e.touches.length === 2) {
                var dx = e.touches[0].clientX - e.touches[1].clientX;
                var dy = e.touches[0].clientY - e.touches[1].clientY;
                touchStart = { dist: Math.sqrt(dx * dx + dy * dy), startDist: camState.tdist, mode: "pinch" };
            }
        }, { passive: true });
        container.addEventListener("touchmove", function (e) {
            if (!touchStart) return;
            if (e.touches.length === 1 && touchStart.mode === "pan") {
                var dx = e.touches[0].clientX - touchStart.x;
                var dy = e.touches[0].clientY - touchStart.y;
                touchStart.x = e.touches[0].clientX; touchStart.y = e.touches[0].clientY;
                var d = camState.dist;
                var panSpeed = d * 0.0025;
                var cos = Math.cos(camState.yaw), sin = Math.sin(camState.yaw);
                camState.ttx -= (dx * cos - dy * sin) * panSpeed;
                camState.ttz -= (dx * sin + dy * cos) * panSpeed;
                camState.tx = camState.ttx; camState.tz = camState.ttz;
            } else if (e.touches.length === 2 && touchStart.mode === "pinch") {
                var dx2 = e.touches[0].clientX - e.touches[1].clientX;
                var dy2 = e.touches[0].clientY - e.touches[1].clientY;
                var newDist = Math.sqrt(dx2 * dx2 + dy2 * dy2);
                var ratio = touchStart.dist / Math.max(1, newDist);
                setZoom(touchStart.startDist * ratio);
            }
        }, { passive: true });
        container.addEventListener("touchend", function () { touchStart = null; }, { passive: true });

        /* Dwuklik resetuje kamere - powrot na widok ogolny / sledzenie aktywnego pionka. */
        container.addEventListener("dblclick", function () {
            camState.userPanned = false;
            camState.tyaw = 0;
            camState.ttilt = 0.55;
        });
    }

    /* Aktualizuje cel kamery, by sledzila pionek aktywnego/animowanego gracza. */
    function updateCameraTarget() {
        /* Szanuj reczne sterowanie kamera ZAWSZE — gdy gracz sam oddali/przesunie widok,
           nie nadpisujemy go (rowniez podczas decyzji o kupnie). Focus na graczu wynika
           wylacznie z followPlayerId ustawianego na czas ruchu pionka. */
        if (camState.userPanned) return;

        var followId = camState.followPlayerId;
        var targetMesh = followId != null ? playerMeshes[String(followId)] : null;
        if (targetMesh) {
            /* Sledzenie pionka - kamera blisko, czytelne pola. */
            camState.ttx = targetMesh.position.x;
            camState.ttz = targetMesh.position.z;
            /* Mocny zoom (~max wide-zoom): od pierwszego skoku widac szczegoly. */
            camState.tdist = camState.cinematic ? 6.8 : 11;
            camState.ttilt = camState.cinematic ? 0.38 : 0.5;
        } else {
            /* Domyslny widok calej planszy. */
            camState.ttx = 0;
            camState.ttz = 0;
            camState.tdist = camDistance;
            camState.ttilt = 0.55;
        }
    }

    function animateLoop() {
        requestAnimationFrame(animateLoop);
        hoverPulse += 0.08;
        if (diceGroup && diceGroup.visible && diceGroup.userData.spin) {
            diceGroup.rotation.y += 0.14;
            diceGroup.children.forEach(function (d, i) {
                d.rotation.x += 0.22 + i * 0.04;
                d.rotation.z += 0.16;
            });
        }
        Object.keys(playerMeshes).forEach(function (k) {
            var m = playerMeshes[k];
            /* Lekkie unoszenie tylko dla pionkow stojacych w miejscu (nie animowanych). */
            if (!m.userData || !m.userData.animating) {
                m.position.y = 0.4 + Math.abs(Math.sin(Date.now() * 0.003 + parseInt(k, 10))) * 0.04;
            }
        });
        pulseActiveTile();

        /* Plynna interpolacja kamery (Business Tour kinowy).
           W trybie cinematic szybsze tempo by kamera zwinniej dotrzymywala kroku. */
        updateCameraTarget();
        var posRate = camState.cinematic ? 0.11 : 0.07;
        var zoomRate = camState.cinematic ? 0.08 : 0.05;
        camState.tx = lerp(camState.tx, camState.ttx, posRate);
        camState.tz = lerp(camState.tz, camState.ttz, posRate);
        camState.dist = lerp(camState.dist, camState.tdist, zoomRate);
        camState.tilt = lerp(camState.tilt, camState.ttilt, zoomRate);
        camState.yaw = lerp(camState.yaw, camState.tyaw, 0.06);
        applyCameraTransform();

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

    function sameId(a, b) {
        return a != null && b != null && Number(a) === Number(b);
    }

    function fixIsMe(state) {
        /* Identifikacja gracza: playerId z HTML (pewne), potem username, na koncu isMe z REST. */
        if (myPlayerId == null && myUsername) {
            state.players.forEach(function (p) {
                if (p.name && p.name.toLowerCase() === myUsername.toLowerCase()) {
                    myPlayerId = Number(p.id);
                }
            });
        }
        if (myPlayerId == null) {
            state.players.forEach(function (p) { if (p.isMe) myPlayerId = Number(p.id); });
        }
        state.players.forEach(function (p) {
            p.isMe = sameId(p.id, myPlayerId);
        });
        state.myTurn = sameId(state.currentTurnPlayerId, myPlayerId);
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

    /* Maly kwadratowy znacznik wlasciciela (nie belka!) — widoczny w rogu pola */
    function updateOwnerMarkers(state) {
        if (!state.ownership) return;
        Object.keys(ownerMarkers).forEach(function (k) {
            boardPivot.remove(ownerMarkers[k].mesh);
        });
        ownerMarkers = {};
        Object.keys(state.ownership).forEach(function (posStr) {
            var pos = parseInt(posStr, 10);
            var ownerId = state.ownership[posStr];
            var owner = state.players.find(function (p) { return p.id === ownerId; });
            if (!owner) return;
            var w = posToWorld(pos);
            var col = new THREE.Color(owner.color || "#e91e63");
            /* Maly walec zamiast belki — znacznik koloru w rogu pola */
            var marker = new THREE.Mesh(
                new THREE.CylinderGeometry(0.09, 0.09, 0.06, 8),
                new THREE.MeshLambertMaterial({ color: col })
            );
            var dx = 0, dz = 0;
            if (pos < 10) { dx = 0.36; dz = -0.36; }
            else if (pos < 20) { dx = 0.36; dz = 0.36; }
            else if (pos < 30) { dx = -0.36; dz = 0.36; }
            else { dx = -0.36; dz = -0.36; }
            marker.position.set(w.x + dx, 0.22, w.z + dz);
            boardPivot.add(marker);
            ownerMarkers[pos] = { mesh: marker };
        });
    }

    function isStation(pos) { return pos === 5 || pos === 15 || pos === 25 || pos === 35; }
    function isChance(pos) { return pos === 7 || pos === 22 || pos === 36; }

    /* Lekkie pulsowanie pola, na ktorym stoi aktualny gracz - przez emissive. */
    var hoverPulse = 0;
    var activeTilePos = null;
    function updateActiveTileHighlight(state) {
        var active = state.players.find(function (p) { return p.id === state.currentTurnPlayerId; });
        if (!active) return;
        activeTilePos = active.position;
    }
    function pulseActiveTile() {
        if (activeTilePos == null) return;
        Object.keys(tileBlocks).forEach(function (k) {
            var block = tileBlocks[k];
            if (!block || !block.material || !block.material.emissive) return;
            if (parseInt(k, 10) === activeTilePos) {
                var v = 0.5 + Math.sin(hoverPulse) * 0.3;
                block.material.emissive.setRGB(v * 1.0, v * 0.85, v * 0.3);
            } else {
                block.material.emissive.setHex(0x000000);
            }
        });
    }

    function renderHud(state) {
        state = fixIsMe(state);
        lastState = state;
        hudSeq++;

        var box = document.getElementById("players");
        var cornerBox = document.getElementById("cornerPlayers");
        var sel = document.getElementById("toPlayer");
        if (box) box.innerHTML = "";
        if (cornerBox) cornerBox.innerHTML = "";
        if (sel) sel.innerHTML = "";

        state.players.forEach(function (p) {
            var turn = state.currentTurnPlayerId === p.id;
            var chip = document.createElement("div");
            chip.className = "player-chip" + (turn ? " turn" : "") + (p.bankrupt ? " bankrupt" : "");
            chip.innerHTML =
                '<span class="token-dot" style="background:' + p.color + '"></span>' +
                '<span class="player-name">' + escapeHtml(p.name) + (p.isMe ? " (Ty)" : "") +
                (p.bankrupt ? " [bankrut]" : "") + '</span>' +
                '<span class="cash">' + formatCash(p.cash) + ' PLN</span>';
            if (box) box.appendChild(chip);

            if (cornerBox) {
                var card = document.createElement("div");
                card.className = "bt-player-card" + (turn ? " active" : "") + (p.bankrupt ? " bankrupt" : "");
                card.style.borderColor = p.color;
                var nameHtml = p.bot
                    ? '<strong>' + escapeHtml(p.name) + ' <span style="color:var(--slate-400);font-size:.7rem;">BOT</span></strong>'
                    : '<strong><a href="/u/' + encodeURIComponent(p.name) + '" class="player-profile-link">' + escapeHtml(p.name) + '</a>' +
                      (p.isMe ? ' <span style="color:var(--brand-emerald);font-size:.68rem;">Ty</span>' : '') +
                      (p.bankrupt ? ' <span style="color:var(--brand-rose);font-size:.68rem;">💀</span>' : '') + '</strong>';
                card.innerHTML =
                    '<div class="bt-player-avatar" style="background:' + p.color + '">' +
                    escapeHtml(p.name.charAt(0).toUpperCase()) + '</div>' +
                    '<div class="bt-player-meta">' + nameHtml +
                    '<span>' + formatCash(p.cash) + ' PLN</span>' +
                    '</div>';
                cornerBox.appendChild(card);
            }
        });

        if (sel) {
            state.players.filter(function (p) { return p.id !== myPlayerId && !p.bankrupt; }).forEach(function (p) {
                var opt = document.createElement("option");
                opt.value = p.id;
                opt.textContent = p.name + " (" + formatCash(p.cash) + " PLN)";
                sel.appendChild(opt);
            });
        }

        var online = document.getElementById("onlineCount");
        if (online) online.textContent = state.players.length;

        /* BUG FIX: dodano pendingUpgrade do warunkow blokujacych przycisk */
        if (rollBtn) rollBtn.disabled = animating || rollInFlight || !state.myTurn || !!state.pendingPurchase
            || !!state.pendingPayment || !!state.pendingUpgrade || !!state.pendingBuyback || !!state.pendingTakeover || state.status === "FINISHED";
        updateCenterDicePanel(state);
        updateWinnerBanner(state);
        updateGameClock(state);
        /* Czy to JA wylosowalem te karte — wtedy widze modal, a nie dubluje toastem. */
        var iAmDrawer = state.movedPlayerId != null && sameId(state.movedPlayerId, myPlayerId);
        if (state.message) {
            var logEl = document.getElementById("log");
            if (logEl) logEl.textContent = state.message;
            if (!animating && animationQueue.length === 0) {
                if (!(state.chanceCard && iAmDrawer)) {
                    pushToastFromMessage(state.message, state);
                }
            }
        }
        updateTileInfo(state);
        updateOwnerMarkers(state);
        updateUpgradeMarkers(state);
        updateActiveTileHighlight(state);
        renderActionPanel(state);
        renderHandCards(state);
        renderMyProperties(state);
        /* Modal wylosowanej karty pokazujemy WYLACZNIE losujacemu. */
        if (state.chanceCard && iAmDrawer) showChanceCard(state, state.chanceCard);
        /* Gdy nic nie animujemy i nie ma decyzji — zwolnij kamere do widoku planszy,
           by przed wlasnym rzutem kamera nie wisiala w zoomie na innym graczu. */
        resetCameraIfIdle();
    }

    /* === ZEGAR GRY: odliczanie do limitu 60 min (po nim wygrywa najbogatszy) === */
    var __clockSeconds = null;
    var __clockSyncAt = 0;
    function updateGameClock(state) {
        var el = document.getElementById("gameClock");
        if (!el) return;
        if (state.status === "FINISHED") {
            __clockSeconds = null;
            el.textContent = "⏱ koniec";
            el.classList.remove("clock-low");
            return;
        }
        if (typeof state.secondsLeft === "number") {
            __clockSeconds = state.secondsLeft;
            __clockSyncAt = Date.now();
        }
        renderClock();
    }
    function renderClock() {
        var el = document.getElementById("gameClock");
        if (!el || __clockSeconds == null) return;
        var rem = Math.max(0, Math.round(__clockSeconds - (Date.now() - __clockSyncAt) / 1000));
        var m = Math.floor(rem / 60), s = rem % 60;
        el.textContent = "⏱ " + m + ":" + (s < 10 ? "0" + s : s);
        if (rem <= 300) el.classList.add("clock-low"); else el.classList.remove("clock-low");
    }
    setInterval(renderClock, 1000);

    /* === PANEL RZUTU: aktualizacja etykiety tury i stanu przycisku (nowy floating layout) === */
    function updateCenterDicePanel(state) {
        var label = document.getElementById("centerTurnLabel");
        var hint  = document.getElementById("centerHint");
        var rollPanel = document.getElementById("rollPanel");
        if (!rollPanel) return;

        var current = state.players.find(function (p) { return sameId(p.id, state.currentTurnPlayerId); })
            || state.players[0];
        var displayName = current ? (current.name || "Gracz") : "Gracz";

        /* Faza decyzji — ukryj przycisk rzutu (zastepuje go action panel) */
        if (state.pendingPurchase || state.pendingPayment || state.pendingUpgrade || state.pendingBuyback || state.pendingTakeover) {
            rollPanel.style.display = "none";
            return;
        }
        if (state.status === "FINISHED") {
            rollPanel.style.display = "none";
            if (label) label.textContent = "Gra zakonczona";
            if (hint) hint.textContent = state.winnerName ? (state.winnerName + " wygral!") : "Koniec";
            return;
        }
        rollPanel.style.display = "";

        if (label) {
            label.textContent = state.myTurn ? "Twoja kolej" : ("Tura: " + displayName);
        }
        if (hint) {
            if (state.myTurn && !animating && !rollInFlight) {
                if (state.canRollAgain) {
                    hint.textContent = "Dublet — rzuć ponownie!";
                    hint.style.background = "rgba(56,189,248,.18)";
                    hint.style.color = "#0284c7";
                } else {
                    hint.textContent = "Rzuć!";
                    hint.style.background = "rgba(16, 185, 129, .2)";
                    hint.style.color = "var(--brand-emerald)";
                }
            } else if (animating || rollInFlight) {
                hint.textContent = "Ruch...";
                hint.style.background = "rgba(245,158,11,.15)";
                hint.style.color = "var(--brand-gold)";
            } else {
                hint.textContent = "Czeka...";
                hint.style.background = "";
                hint.style.color = "var(--slate-400)";
            }
        }
    }

    /* === TOAST FEED: krotkie komunikaty u dolu planszy === */
    var lastToastKey = null;
    function pushToast(text, kind, iconCls) {
        var feed = document.getElementById("actionToasts");
        if (!feed) return;
        var t = document.createElement("div");
        t.className = "bt-toast toast-" + (kind || "info");
        t.innerHTML = '<i class="fa-solid ' + (iconCls || "fa-circle-info") + '"></i> <span>' + text + '</span>';
        feed.appendChild(t);
        /* Limit do 4 widocznych */
        while (feed.children.length > 4) feed.removeChild(feed.firstChild);
        requestAnimationFrame(function () { t.classList.add("visible"); });
        setTimeout(function () {
            t.classList.add("fade-out");
            setTimeout(function () { if (t.parentNode) t.parentNode.removeChild(t); }, 280);
        }, 4500);
    }

    /* Duzy banner "MONOPOL ZDOBYTY!" wyswietlany przez 4s w centrum ekranu. */
    function showMonopolBanner(text) {
        var b = document.createElement("div");
        b.style.cssText = "position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);" +
            "background:linear-gradient(135deg,#1565C0,#0D47A1);color:#fff;" +
            "padding:1.2rem 2.5rem;border-radius:14px;font-size:1.6rem;font-weight:700;" +
            "z-index:9990;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,.5);" +
            "animation:btMonopolIn .35s ease;pointer-events:none;";
        b.innerHTML = '<i class="fa-solid fa-crown" style="margin-right:.5rem;color:#FFD700;"></i>' +
            'MONOPOL ZDOBYTY!<br><span style="font-size:1rem;font-weight:400;opacity:.85;">' +
            escapeHtml(text) + '</span>';
        document.body.appendChild(b);
        setTimeout(function () {
            b.style.opacity = "0";
            b.style.transition = "opacity .4s";
            setTimeout(function () { if (b.parentNode) b.parentNode.removeChild(b); }, 450);
        }, 3600);
    }

    /* Generuje krotki komunikat z dlugiej wiadomosci serwera. */
    function pushToastFromMessage(message, state) {
        if (!message) return;
        var key = (state.movedPlayerId || "x") + "|" + (state.dice1 || "") + "|" + message.slice(0, 60);
        if (key === lastToastKey) return;
        lastToastKey = key;

        /* Wyciagnij imie aktora (przed " rzuca", " kupuje", " placi", itp.) */
        var actor = "";
        var m = message.match(/^([^.]+?) (rzuca|kupuje|wygrywa|pomija|placi|nie zdecydowal)/);
        if (m) actor = '<span class="toast-name">' + escapeHtml(m[1]) + '</span> ';

        /* Heurystyki */
        if (/kupuje/.test(message)) {
            var mm = message.match(/kupuje ([^"]+?) za (\d+) PLN/);
            if (mm) return pushToast(actor + "kupil " + escapeHtml(mm[1]) + " za " + mm[2] + " PLN", "buy", "fa-bag-shopping");
        }
        if (/wygrywa licytacje/.test(message)) {
            var mb = message.match(/licytacje na ([^"]+?) za (\d+)/);
            if (mb) return pushToast(actor + "wygrał licytacje na " + escapeHtml(mb[1]) + " (" + mb[2] + " PLN)", "bid", "fa-gavel");
        }
        if (/pomija pole/.test(message)) {
            var ms = message.match(/pomija pole ([^-]+?) -/);
            return pushToast(actor + "pomija pole" + (ms ? " " + escapeHtml(ms[1].trim()) : ""), "skip", "fa-forward");
        }
        if (/nie zdecydowal/.test(message)) {
            return pushToast(actor + "nie zdecydowal w czasie - pole wolne", "skip", "fa-clock");
        }
        if (/placi czynsz/.test(message)) {
            var mr = message.match(/placi czynsz (\d+) PLN dla ([^.]+?)\./);
            if (mr) return pushToast(actor + "placi czynsz " + mr[1] + " PLN -> " + escapeHtml(mr[2]), "rent", "fa-money-bill-transfer");
        }
        if (/Karta Szansy/.test(message)) {
            var mc = message.match(/Karta Szansy: "([^"]+)"/);
            if (mc) return pushToast(actor + "ciagnie kartę: " + escapeHtml(mc[1]), "chance", "fa-bolt");
        }
        if (/laduje w WIEZIENIU|trafia do (Dziekanatu|wiezienia)|WIEZIENIE|Dziekanat.*idziesz|prosto do Dziekanatu/i.test(message)) {
            return pushToast(actor + "laduje w Dziekanacie (Warunek)!", "jail", "fa-lock");
        }
        if (/BANKRUCTWO/.test(message)) {
            /* Pokaz overlay jezeli to moj gracz */
            if (lastState && lastState.players) {
                var me = lastState.players.find(function(p){ return p.isMe; });
                if (me && me.bankrupt) showGameEndOverlay(lastState);
            }
            return pushToast(actor + "bankrutuje!", "rent", "fa-skull");
        }
        if (/WYGRYWA GRE|WYGRYWA przez/.test(message)) {
            var mw = message.match(/([^ ]+(?:\s[^ ]+)*?) WYGRYWA/);
            return pushToast((mw ? escapeHtml(mw[1]) : "Gracz") + " wygrywa gre!", "buy", "fa-trophy");
        }
        if (/\[MONOPOL\]/.test(message)) {
            var mm2 = message.match(/\[MONOPOL\] ([^!]+)/);
            showMonopolBanner(mm2 ? mm2[1].trim() : "Monopol zdobyty!");
            return pushToast(mm2 ? escapeHtml(mm2[1].trim()) : "Monopol zdobyty!", "buy", "fa-crown");
        }
        if (/UWAGA!/.test(message)) {
            var mua = message.match(/UWAGA! ([^!]+!)/);
            return pushToast(mua ? escapeHtml(mua[1]) : "Gracz jest blisko wygranej!", "rent", "fa-triangle-exclamation");
        }
        if (/Brakuje siana/.test(message)) {
            return pushToast(actor + "nie stac na oplate — sprzedaj lub popros o pozyczke", "rent", "fa-coins");
        }
        if (/sprzedaje/.test(message)) {
            var msell = message.match(/sprzedaje ([^"]+?) za (\d+)/);
            if (msell) return pushToast(actor + "sprzedal " + escapeHtml(msell[1]) + " za " + msell[2] + " PLN", "skip", "fa-hand-holding-dollar");
        }
        if (/przejscie przez START/i.test(message)) {
            return pushToast(actor + "przeszedl przez START (+300 000 PLN)", "buy", "fa-flag-checkered");
        }
        if (/rzuca/.test(message)) {
            var md = message.match(/rzuca (\d+\+\d+=\d+).*Staje na: ([^.]+)\./);
            if (md) return pushToast(actor + "rzucil " + md[1] + " -> " + escapeHtml(md[2]), "roll", "fa-dice");
            var md2 = message.match(/rzuca (\d+\+\d+=\d+)/);
            if (md2) return pushToast(actor + "rzucil " + md2[1], "roll", "fa-dice");
        }
        /* fallback */
        var short = message.length > 70 ? message.slice(0, 67) + "..." : message;
        pushToast(escapeHtml(short), "info", "fa-circle-info");
    }

    /* === PANEL DECYZJI: KUP / POMIN / LICYTUJ === */
    var actionTimerInterval = null;
    var lastActionPanelKey = null;

    function clearActionTimer() {
        if (actionTimerInterval) {
            clearInterval(actionTimerInterval);
            actionTimerInterval = null;
        }
    }

    function startActionTimer(deadlineMs) {
        clearActionTimer();
        /* Calkowity czas (s) liczony raz na starcie — pasek to procent pozostalego,
           dziala tak samo dla 10s (kupno) jak i 30s (splata). */
        var totalSec = Math.max(1, Math.ceil((deadlineMs - Date.now()) / 1000));
        actionTimerInterval = setInterval(function () {
            var remaining = Math.max(0, Math.ceil((deadlineMs - Date.now()) / 1000));
            var bar = document.getElementById("actionTimerBar");
            var label = document.getElementById("actionTimerLabel");
            if (bar) bar.style.width = Math.min(100, (remaining / totalSec) * 100) + "%";
            if (label) label.textContent = remaining + "s";
            if (remaining <= 0) {
                clearActionTimer();
                if (label) label.textContent = "0s - czas minal";
                /* Serwer po uplywie czasu robi auto-skip/auto-splate. Jesli broadcast WS
                   sie zgubi, panel zostalby z martwymi przyciskami ("Brak aktywnego pola").
                   Po krotkiej karencji pobieramy swiezy stan, by panel sam sie zsynchronizowal. */
                setTimeout(function () {
                    fetch("/api/game/" + sessionId + "/state", { headers: authHeaders(false) })
                        .then(function (r) { return r.json(); })
                        .then(function (s) { applyStateWithAnimation(s); })
                        .catch(function () {});
                }, 1500);
            }
        }, 100);
    }

    var gameEndShown = false;
    var gameEndCountdown = null;

    function updateWinnerBanner(state) {
        var banner = document.getElementById("winnerBanner");
        if (!banner) return;
        if (state.status === "FINISHED" && state.winnerName) {
            banner.style.display = "block";
            banner.innerHTML = '<i class="fa-solid fa-trophy"></i> <strong>' + escapeHtml(state.winnerName) + '</strong> wygrywa gre!';
        } else {
            banner.style.display = "none";
            banner.innerHTML = "";
        }
        showGameEndOverlay(state);
    }

    function showGameEndOverlay(state) {
        if (gameEndShown) return;
        var overlay = document.getElementById("gameEndOverlay");
        if (!overlay) return;

        /* Pokaz overlay jezeli: gra sie skonczyla LUB moj gracz zbankrutowal */
        var iFinished = state.status === "FINISHED";
        var myPlayer = state.players ? state.players.find(function(p){ return p.isMe; }) : null;
        var iBankrupt = myPlayer && myPlayer.bankrupt;

        if (!iFinished && !iBankrupt) return;

        gameEndShown = true;
        var icon = document.getElementById("gameEndIcon");
        var title = document.getElementById("gameEndTitle");
        var sub = document.getElementById("gameEndSub");
        var timer = document.getElementById("gameEndTimer");

        if (iFinished && state.winnerName) {
            var iWon = myPlayer && String(myPlayer.id) === String(state.winnerId);
            if (iWon) {
                icon.innerHTML = '<i class="fa-solid fa-trophy" style="color:#f59e0b;"></i>';
                title.textContent = "ZWYCIĘSTWO!";
                title.style.color = "#f59e0b";
                sub.textContent = "Gratulacje, " + escapeHtml(state.winnerName) + "! Wygrałeś grę Kampus PB!";
            } else {
                icon.innerHTML = '<i class="fa-solid fa-flag-checkered" style="color:#64748b;"></i>';
                title.textContent = "KONIEC GRY";
                title.style.color = "#64748b";
                sub.textContent = "Wygrywa: " + escapeHtml(state.winnerName);
            }
        } else if (iFinished) {
            icon.innerHTML = '<i class="fa-solid fa-skull" style="color:#ef4444;"></i>';
            title.textContent = "REMIS";
            title.style.color = "#ef4444";
            sub.textContent = "Wszyscy zbankrutowali.";
        } else if (iBankrupt) {
            icon.innerHTML = '<i class="fa-solid fa-skull" style="color:#ef4444;"></i>';
            title.textContent = "BANKRUCTWO";
            title.style.color = "#ef4444";
            sub.textContent = "Straciłeś wszystkie zasoby. Gra trwa dla pozostałych graczy.";
        }

        overlay.style.display = "flex";

        /* Odliczanie do przekierowania tylko jesli gra sie skonczyla */
        if (iFinished) {
            var secs = 10;
            function tick() {
                if (timer) timer.textContent = "Powrót do profilu za " + secs + "s...";
                if (secs <= 0) {
                    clearInterval(gameEndCountdown);
                    window.location.href = "/dashboard";
                }
                secs--;
            }
            tick();
            gameEndCountdown = setInterval(tick, 1000);
        }
    }

    function closeFloatingPopups() {
        /* BUG FIX: zamknij popupy kart i transferu gdy pojawia sie panel akcji */
        var hp = document.getElementById("handCardsPanel");
        var tp = document.getElementById("transferPanel");
        if (hp) hp.style.display = "none";
        if (tp) tp.style.display = "none";
    }

    function renderActionPanel(state) {
        var ap = document.getElementById("actionPanel");
        if (!ap) return;
        /* Brak decyzji — chowamy panel ZAWSZE (rowniez w trakcie animacji), dzieki czemu
           panel "Zakup — decyduje Bot" nigdy nie zostaje na ekranie po decyzji bota. */
        if (!state.pendingPayment && !state.pendingUpgrade && !state.pendingPurchase && !state.pendingBuyback && !state.pendingTakeover) {
            ap.style.display = "none";
            ap.innerHTML = "";
            lastActionPanelKey = null;
            clearActionTimer();
            return;
        }
        /* Panel decyzji (kupno/oplata/ulepszenie) pokazujemy DOPIERO gdy pionek doleci
           na pole — czyli po zakonczeniu animacji ruchu, nie w jej trakcie. */
        if (animating) return;
        if (state.pendingPayment) {
            closeFloatingPopups();
            renderPaymentPanel(state, ap);
            return;
        }
        if (state.pendingBuyback) {
            closeFloatingPopups();
            renderBuybackPanel(state, ap);
            return;
        }
        if (state.pendingTakeover) {
            closeFloatingPopups();
            renderTakeoverPanel(state, ap);
            return;
        }
        if (state.pendingUpgrade) {
            closeFloatingPopups();
            renderUpgradePanel(state, ap);
            return;
        }
        var pp = state.pendingPurchase;
        var panelKey = pp.deciderId + "|" + pp.tileName + "|" + pp.basePrice;
        if (panelKey === lastActionPanelKey) return;
        lastActionPanelKey = panelKey;

        closeFloatingPopups();
        var amDecider = sameId(pp.deciderId, myPlayerId);
        var decider = state.players.find(function (p) { return sameId(p.id, pp.deciderId); }) || {};
        var deciderIsBot = decider.bot === true;

        var timerHtml = amDecider
            ? '<span id="actionTimerLabel" class="action-timer-label">25s</span>'
            : (deciderIsBot ? '<span class="action-timer-label" style="color:var(--brand-emerald)">BOT...</span>' : '');

        var html = '<div class="hud-panel-head">' +
            '<h2><i class="fa-solid fa-building" style="color:var(--brand-gold)"></i> Zakup</h2>' +
            timerHtml + '</div>';
        if (amDecider) html += '<div class="action-timer-track"><div id="actionTimerBar" class="action-timer-bar"></div></div>';

        html += '<p class="action-tile-name"><i class="fa-solid fa-flag"></i> ' + escapeHtml(pp.tileName) + '</p>';

        /* Karty postepu: Pole + 1 dom (kupujemy razem) -> 2 domy -> 3 domy -> Biurowiec */
        var stages = [
            { label: "Pole + 1 dom", icons: '<i class="fa-solid fa-house-chimney-window"></i>' },
            { label: "2 domy",       icons: '<i class="fa-solid fa-house-chimney-window"></i><i class="fa-solid fa-house-chimney-window"></i>' },
            { label: "3 domy",       icons: '<i class="fa-solid fa-house-chimney-window"></i><i class="fa-solid fa-house-chimney-window"></i><i class="fa-solid fa-house-chimney-window"></i>' },
            { label: "Biurowiec",    icons: '<i class="fa-solid fa-building"></i>' }
        ];
        html += '<div class="bt-upgrade-progress">';
        stages.forEach(function(st, i) {
            var cls = i === 0 ? "bt-prog-card--active" : "bt-prog-card--locked";
            html += '<div class="bt-prog-card ' + cls + '">' +
                '<div class="bt-prog-icon">' + st.icons + '</div>' +
                '<div class="bt-prog-label">' + st.label + '</div>' +
                (i === 0 ? '<span class="bt-prog-check">&#10003;</span>' : '') +
                '</div>';
        });
        html += '</div>';

        var baseRent = pp.baseRent || 0;
        html += '<div class="bt-purchase-meta">' +
            '<span>Czynsz po zakupie: <span class="rent-val">' + formatCash(baseRent) + ' PLN</span></span>' +
            '<span class="buyback-val">Odkup: ' + formatCash(pp.basePrice * 2) + ' PLN</span>' +
            '</div>';

        if (amDecider) {
            html += '<div class="action-buttons">' +
                '<button class="btn btn-bt-roll" id="btnBuy">' +
                '<i class="fa-solid fa-bag-shopping"></i> Kupuj za ' + formatCash(pp.basePrice) + ' PLN</button>' +
                '<button class="btn btn-secondary" id="btnSkip">Pomin</button>' +
                '</div>' +
                '<p class="muted small" style="text-align:center;">Masz 25s — po czasie pole wraca do banku.</p>';
        } else {
            html += '<p class="muted" style="text-align:center;">Decyduje: <strong>' +
                escapeHtml(decider.name || "Gracz") + '</strong></p>';
        }
        html += '<p class="error" id="actionErr" style="display:none;"></p>';
        ap.style.display = "block";
        ap.className = "bt-action-float bt-action-anim";
        ap.innerHTML = html;

        if (amDecider) startActionTimer(Date.now() + 25000);
        else clearActionTimer();

        var btnBuy = document.getElementById("btnBuy");
        if (btnBuy) btnBuy.addEventListener("click", function () { postAction("/buy"); });
        var btnSkip = document.getElementById("btnSkip");
        if (btnSkip) btnSkip.addEventListener("click", function () { postAction("/skip"); });
    }

    function renderPaymentPanel(state, ap) {
        var pp = state.pendingPayment;
        var debtor = state.players.find(function (p) { return p.id === pp.debtorId; }) || {};
        var sellKeys = pp.sellPrices ? Object.keys(pp.sellPrices).sort().join(",") : "";
        var panelKey = "pay|" + pp.debtorId + "|" + pp.amount + "|" + (debtor.cash != null ? debtor.cash : 0) + "|" + sellKeys;
        if (panelKey === lastActionPanelKey) return;
        lastActionPanelKey = panelKey;

        var amDebtor = state.players.some(function (p) { return p.isMe && p.id === pp.debtorId; });
        var debtorName = debtor.name || "Gracz";
        var creditor = pp.creditorId
            ? state.players.find(function (p) { return p.id === pp.creditorId; })
            : null;
        var creditorLabel = creditor ? creditor.name : "bank";

        var html = '<div class="hud-panel-head">' +
            '<h2>Brak siana</h2>' +
            (amDebtor ? '<span id="actionTimerLabel" class="action-timer-label">30s</span>' : '') +
            '</div>';
        if (amDebtor) {
            html += '<div class="action-timer-track"><div id="actionTimerBar" class="action-timer-bar"></div></div>';
        }
        html += '<p class="action-tile-name"><i class="fa-solid fa-coins"></i> ' + escapeHtml(pp.reason || "Oplata") + '</p>' +
            '<p class="muted action-tile-price">Do zaplaty: <strong>' + pp.amount + ' PLN</strong>' +
            (creditor ? ' dla <strong>' + escapeHtml(creditorLabel) + '</strong>' : '') + '</p>' +
            '<p class="muted small">Twoje siano: <strong>' + (debtor.cash != null ? debtor.cash : 0) + ' PLN</strong></p>';

        if (amDebtor) {
            var sellPrices = pp.sellPrices || {};
            var positions = Object.keys(sellPrices);
            if (positions.length > 0) {
                html += '<p class="action-bid-label">Sprzedaj nieruchomosc (50% ceny):</p><div class="action-buttons">';
                positions.forEach(function (posKey) {
                    var pos = parseInt(posKey, 10);
                    var tileName = (state.tileNames && state.tileNames[pos]) ? state.tileNames[pos] : ("Pole " + pos);
                    html += '<button class="btn btn-secondary btn-roll btn-sell" data-pos="' + pos + '">' +
                        escapeHtml(tileName) + ' (' + sellPrices[posKey] + ' PLN)</button>';
                });
                html += '</div>';
            } else {
                html += '<p class="muted small">Nie masz nieruchomosci do sprzedazy — popros innego gracza o pozyczke (panel Transfer).</p>';
            }
            html += '<div class="action-buttons" style="margin-top:0.5rem">' +
                '<button class="btn btn-bt-roll" id="btnPayDebt"><i class="fa-solid fa-check"></i> Oplac ' + pp.amount + ' PLN</button>' +
                '<button class="btn btn-secondary btn-roll" id="btnBankrupt"><i class="fa-solid fa-skull"></i> Bankructwo</button>' +
                '</div>' +
                '<p class="muted small">Sprzedaj pola, popros o pozyczke lub oplac gdy masz wystarczajaco siana. Po 30s auto-splata/bankructwo.</p>';
        } else {
            html += '<p class="muted small"><strong>' + escapeHtml(debtorName) + '</strong> musi uzbierac ' + pp.amount +
                ' PLN. Mozesz przelac mu siano w panelu Transfer (pozyczka).</p>';
        }
        html += '<p class="error" id="actionErr" style="display:none;"></p>';
        ap.style.display = "block";
        ap.className = "bt-action-float bt-action-anim";
        ap.innerHTML = html;

        if (amDebtor) startActionTimer(Date.now() + 30000);
        else clearActionTimer();

        if (camState.cinematic !== true) {
            camState.cinematic = true;
            camState.followPlayerId = pp.debtorId;
        }

        document.querySelectorAll(".btn-sell").forEach(function (btn) {
            btn.addEventListener("click", function () {
                var pos = parseInt(btn.getAttribute("data-pos"), 10);
                postAction("/sell", { position: pos });
            });
        });
        var btnPay = document.getElementById("btnPayDebt");
        if (btnPay) btnPay.addEventListener("click", function () { postAction("/pay-debt"); });
        var btnBankrupt = document.getElementById("btnBankrupt");
        if (btnBankrupt) btnBankrupt.addEventListener("click", function () { postAction("/bankrupt"); });
    }

    var actionInFlight = false;

    function postAction(path, body) {
        if (actionInFlight) return;
        actionInFlight = true;
        var errBox = document.getElementById("actionErr");
        var btnBuy = document.getElementById("btnBuy");
        var btnSkip = document.getElementById("btnSkip");
        if (btnBuy) btnBuy.disabled = true;
        if (btnSkip) btnSkip.disabled = true;
        var opts = { method: "POST", headers: authHeaders(!!body) };
        if (body) opts.body = JSON.stringify(body);
        fetch("/api/game/" + sessionId + path, opts)
            .then(function (r) {
                return r.json().catch(function () { return {}; }).then(function (d) {
                    return { ok: r.ok, status: r.status, data: d };
                });
            })
            .then(function (res) {
                if (!res.ok) {
                    var errMsg = (res.data && res.data.error) || (res.status >= 500
                        ? "Blad serwera (" + res.status + "). Odswiezam stan..."
                        : "Blad operacji.");
                    if (errBox) {
                        errBox.textContent = errMsg;
                        errBox.style.display = "block";
                    }
                    lastActionPanelKey = null;
                    fetch("/api/game/" + sessionId + "/state", { headers: authHeaders(false) })
                        .then(function (r) { return r.json(); })
                        .then(function (s) { applyStateWithAnimation(s); })
                        .catch(function () {});
                    return;
                }
                lastActionPanelKey = null;
                applyStateWithAnimation(res.data);
            })
            .catch(function () {
                if (btnBuy) btnBuy.disabled = false;
                if (btnSkip) btnSkip.disabled = false;
            })
            .finally(function () { actionInFlight = false; });
    }

    /* === MODAL KARTY SZANSY === */
    function chanceKey(card, state) {
        return (card.title || "") + "|" + (card.moneyEffect || 0) + "|" + (state.movedPlayerId || "");
    }
    function showChanceCard(state, card) {
        var key = chanceKey(card, state);
        if (key === lastChanceKey) return;
        lastChanceKey = key;
        var modal = document.getElementById("chanceModal");
        if (!modal) return;
        var t = document.getElementById("chanceTitle");
        var d = document.getElementById("chanceDesc");
        var e = document.getElementById("chanceEffect");
        var k = document.getElementById("chanceKind");
        if (t) t.textContent = card.title || "Karta Szansy";
        if (d) d.textContent = card.description || "";
        if (k) k.textContent = (card.moneyEffect || 0) >= 0 ? "SZANSA - PB" : "WYDARZENIE - PB";
        if (e) {
            var v = card.moneyEffect || 0;
            e.textContent = v > 0 ? "+" + v + " PLN" : (v < 0 ? v + " PLN" : "Bez efektu finansowego");
            e.className = "chance-effect " + (v > 0 ? "chance-effect-good" : (v < 0 ? "chance-effect-bad" : ""));
        }
        modal.classList.add("visible");
        modal.classList.remove("flip-out");
        clearTimeout(showChanceCard._t);
        showChanceCard._t = setTimeout(function () {
            modal.classList.add("flip-out");
            setTimeout(function () { modal.classList.remove("visible"); modal.classList.remove("flip-out"); }, 500);
        }, 4500);
    }

    /* === KOLEJKA ANIMACJI ===
       Rozwiazuje race condition: gdy podczas Twojej animacji przychodzi WS-state
       z ruchem bota, NIE wolno upsertPlayerMesh natychmiast (pionek by sie teleportowal).
       Zamiast tego dodajemy state do kolejki i animujemy po zakonczeniu poprzedniego ruchu.
       Dedup po eventKey eliminuje duplikaty (REST response + WS broadcast dla tego samego ruchu). */
    var animationQueue = [];
    var processedKeys = {};
    var animSafetyTimer = null;   /* wymusza zakonczenie zawieszonej animacji (rAF w tle) */
    var animToken = 0;            /* uniewaznia spoznione callbacki po force-complete */

    /* Zwolnij kamere do widoku planszy, gdy nie ma decyzji i nic sie nie animuje. */
    function resetCameraIfIdle() {
        if (animating) return;
        if (!lastState) return;
        var pend = lastState.pendingPurchase || lastState.pendingPayment || lastState.pendingUpgrade || lastState.pendingBuyback || lastState.pendingTakeover;
        if (!pend) { camState.cinematic = false; camState.followPlayerId = null; }
    }

    /* Awaryjne zakonczenie: gdy animacja utknie (np. karta w tle wstrzymala rAF),
       wymuszamy synchronizacje stanu, by plansza nie "stala". Porzucamy zalegle ruchy
       (nie odtwarzamy starych animacji botow po fakcie). */
    function forceCompleteAnimation() {
        animToken++;
        animSafetyTimer = null;
        animating = false;
        rollInFlight = false;
        animationQueue.length = 0;
        try {
            var badge = document.getElementById("diceBadge");
            if (badge) badge.classList.remove("visible");
            hideBoardDice();
            if (lastState && lastState.players) {
                lastState.players.forEach(function (p, idx) { upsertPlayerMesh(p, idx, p.position); });
                resetCameraIfIdle();
                renderHud(lastState);
            }
        } catch (e) { /* ignore */ }
    }
    /* Bezpieczne ostatnie znane state pod ktore renderujemy gdy nie animujemy. */
    function applyStateWithAnimation(state) {
        if (!state) return;
        state = fixIsMe(state);
        var moved = state.movedPlayerId;
        var from = state.fromPosition;
        var to = state.toPosition;
        var hasMove = moved != null && from != null && to != null && from !== to;
        var hasDice = state.dice1 != null && state.dice2 != null;

        if (hasMove && hasDice) {
            var key = eventKey(state);
            if (processedKeys[key]) {
                /* Duplikat (REST + WS daly to samo wydarzenie) - juz animowany lub w kolejce.
                   Nic nie robimy: gdy animacja sie skonczy renderHud i tak ustawi finalny stan. */
                return;
            }
            /* Sprawdz czy ten klucz nie jest juz w kolejce. */
            for (var qi = 0; qi < animationQueue.length; qi++) {
                if (animationQueue[qi]._animKey === key) return;
            }
            state._animKey = key;
            /* Zapamietujemy najnowszy stan (dla watchdoga / kamery), ale NIE renderujemy
               jeszcze panelu — panel kupna/akcji ma sie pojawic dopiero gdy pionek doleci
               na pole (na koncu animacji), inaczej "pole wyswietla sie juz z kupnem". */
            lastState = state;
            animationQueue.push(state);
            /* COLLAPSE: animujemy tylko NAJNOWSZY ruch. Gdy boty rzucaja szybciej niz
               trwa animacja, stare ruchy lądowałyby w kolejce i odtwarzaly sie z opoznieniem
               (np. "przed moim rzutem juz cos sie rusza"). Zostawiamy ostatni, reszte
               oznaczamy jako obsluzona — koncowe pozycje i tak ustawi finalny stan. */
            if (animationQueue.length > 1) {
                var keep = animationQueue[animationQueue.length - 1];
                for (var z = 0; z < animationQueue.length - 1; z++) {
                    processedKeys[animationQueue[z]._animKey] = true;
                }
                animationQueue.length = 0;
                animationQueue.push(keep);
            }
            processAnimationQueue();
            return;
        }

        /* State bez animacji ruchu - zachowuje sie inaczej w zaleznosci czy trwa animacja:
           - Gdy animating=true: NIE ruszamy pozycji istniejacych pionkow (chronimy animacje).
                                 Po skonczeniu animacji, callback i tak ustawi koncowe pozycje.
           - Gdy animating=false: bezpiecznie ustawiamy pozycje wszystkich pionkow. */
        refreshUiOnly(state);
        if (!animating && animationQueue.length === 0) {
            /* Nic sie nie animuje i nic nie czeka w kolejce — bezpiecznie ustaw wszystkie pionki. */
            state.players.forEach(function (p, idx) { upsertPlayerMesh(p, idx, p.position); });
        } else {
            /* Trwa lub czeka animacja ruchu — NIE przestawiaj istniejacych pionkow (teleport!),
               twórz tylko meshe nowych graczy. Pozycje ustawi callback po animacji. */
            state.players.forEach(function (p, idx) {
                if (!playerMeshes[String(p.id)]) upsertPlayerMesh(p, idx, p.position);
            });
        }
        /* Odswiezamy badge z wynikiem rzutu jesli jest. */
        if (state.dice1 != null) {
            var bdD1 = document.getElementById("diceBadgeD1");
            var bdD2 = document.getElementById("diceBadgeD2");
            var bdSm = document.getElementById("diceBadgeSum");
            if (bdD1) bdD1.textContent = state.dice1;
            if (bdD2) bdD2.textContent = state.dice2;
            if (bdSm) bdSm.textContent = String(state.dice1 + state.dice2);
        }
    }

    /* UI bez ruszania pionkow: HUD, action panel, toasts itp.
       Wywolywane rowniez gdy animating=true (np. bot zdecydowal w trakcie animacji ruchu).
       renderHud sprawdza rollBtn.disabled uwzgledniajac animating, wiec guzik bedzie poprawny
       zaraz po skonczeniu animacji (bo seqAtStart != hudSeq zablokuje nadpisanie). */
    function refreshUiOnly(state) {
        renderHud(state);
        if (state.chanceCard) showChanceCard(state, state.chanceCard);
        /* Jesli animacja trwa - rollBtn tymczasowo disabled jest ok (animateMove go trzyma).
           Gdy animacja skonczy sie, processAnimationQueue zobaczy hudSeq > seqAtStart
           i NIE nadpisze UI starym stanem - zrobi tylko updateCenterDicePanel(lastState). */
    }

    function processAnimationQueue() {
        if (animating) return;
        if (animationQueue.length === 0) return;
        var state = animationQueue.shift();
        animating = true;
        processedKeys[state._animKey] = true;
        /* Cleanup zeby slownik nie rosl w nieskonczonosc po dlugich rozgrywkach. */
        var allKeys = Object.keys(processedKeys);
        if (allKeys.length > 80) {
            for (var c = 0; c < 40; c++) delete processedKeys[allKeys[c]];
        }
        lastEventKey = state._animKey;
        if (rollBtn) rollBtn.disabled = true;
        /* Zapamiętaj numer sekwencji przed animacja. Jesli w trakcie animacji
           przyjdzie swiezszy stan (np. bot zdecydowal o zakupie) to renderHud(state)
           z kolejki nie nadpisze nowszego UI. */
        var seqAtStart = hudSeq;
        var myToken = ++animToken;
        if (animSafetyTimer) clearTimeout(animSafetyTimer);
        animSafetyTimer = setTimeout(forceCompleteAnimation, 9000);

        animateDice(state.dice1, state.dice2, function () {
            if (myToken !== animToken) return;   /* animacja zostala juz wymuszona/anulowana */
            animateMove(state.movedPlayerId, state.fromPosition, state.toPosition, state.players, function () {
                if (myToken !== animToken) return;
                if (animSafetyTimer) { clearTimeout(animSafetyTimer); animSafetyTimer = null; }
                /* Schowaj badge z wynikiem rzutu po zakończeniu ruchu pionka. */
                var badge = document.getElementById("diceBadge");
                if (badge) badge.classList.remove("visible");
                /* Zawsze ustawiamy pozycje pionkow na finalne z tego ruchu. */
                state.players.forEach(function (p, idx) { upsertPlayerMesh(p, idx, p.position); });
                animating = false;
                /* Po animacji: polacz stan ruchu z ewentualnym nowszym stanem z WS. */
                var finalState = state;
                if (hudSeq !== seqAtStart && lastState) {
                    /* lastState jest NAJNOWSZY — jego pending* sa prawda. NIE robimy OR ze
                       starym stanem, bo wtedy panel "decyduje Bot" wracalby po decyzji bota. */
                    finalState = Object.assign({}, state, lastState, {
                        pendingPurchase: lastState.pendingPurchase,
                        pendingPayment: lastState.pendingPayment,
                        pendingUpgrade: lastState.pendingUpgrade,
                        pendingBuyback: lastState.pendingBuyback,
                        pendingTakeover: lastState.pendingTakeover,
                        ownership: lastState.ownership || state.ownership,
                        players: lastState.players || state.players,
                        dice1: state.dice1 != null ? state.dice1 : lastState.dice1,
                        dice2: state.dice2 != null ? state.dice2 : lastState.dice2,
                        movedPlayerId: state.movedPlayerId != null ? state.movedPlayerId : lastState.movedPlayerId,
                        fromPosition: state.fromPosition != null ? state.fromPosition : lastState.fromPosition,
                        toPosition: state.toPosition != null ? state.toPosition : lastState.toPosition,
                        message: lastState.message || state.message,
                        /* Pola prywatne — WS broadcast nie zawiera tych pol (null).
                           Priorytet: animacja > lastState > trwaly cache. */
                        myPropertyCards: state.myPropertyCards || lastState.myPropertyCards || propertyCardsData,
                        myHandCards: state.myHandCards || lastState.myHandCards || handCardsData,
                        myTurn: state.myTurn,
                        canRollAgain: state.canRollAgain
                    });
                    finalState = fixIsMe(finalState);
                }
                renderHud(finalState);
                rollInFlight = false;
                if (animationQueue.length === 0) {
                    updateOwnerMarkers(finalState);
                    updateUpgradeMarkers(finalState);
                    if (finalState.message) {
                        pushToastFromMessage(finalState.message, finalState);
                    }
                }
                if (animationQueue.length > 0) {
                    setTimeout(processAnimationQueue, 200);
                }
            });
        });   /* kostka rzuca sie na srodku planszy (bez atPos) */
    }

    var wsWasConnected = false;
    
    
    /* === SYSTEM EMOTEK I NAKLEJEK === */
    var reactionMeshes = {}; // playerId -> Group

    function showReaction(playerId, code, iconCls) {
        try {
            var pawn = playerMeshes[String(playerId)];
            if (!pawn) return;
            
            if (reactionMeshes[playerId]) {
                boardPivot.remove(reactionMeshes[playerId]);
                delete reactionMeshes[playerId];
            }

            var group = new THREE.Group();
            var sprite;

            if (iconCls) {
                sprite = makeIconSprite(iconCls);
            } else {
                sprite = makeTextSprite(code);
            }

            if (!sprite) return;

            group.add(sprite);
            group.position.copy(pawn.position);
            group.position.y += 0.85; 
            boardPivot.add(group);
            reactionMeshes[playerId] = group;

            var startTime = Date.now();
            var duration = 3000;
            function animateReaction() {
                var elapsed = Date.now() - startTime;
                var t = elapsed / duration;
                if (t >= 1) {
                    if (reactionMeshes[playerId] === group) {
                        boardPivot.remove(group);
                        delete reactionMeshes[playerId];
                    }
                    return;
                }
                group.position.y += 0.002;
                if (sprite.material) sprite.material.opacity = 1 - Math.pow(t, 2);
                requestAnimationFrame(animateReaction);
            }
            animateReaction();
        } catch (e) { console.error("Reaction error:", e); }
    }

    function makeTextSprite(text) {
        try {
            var canvas = document.createElement('canvas');
            canvas.width = 128; canvas.height = 128;
            var ctx = canvas.getContext('2d');
            ctx.font = '70px Arial';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(text || "", 64, 64);
            var tex = new THREE.CanvasTexture(canvas);
            var mat = new THREE.SpriteMaterial({ map: tex, transparent: true });
            var s = new THREE.Sprite(mat);
            s.scale.set(0.6, 0.6, 1);
            return s;
        } catch (e) { return null; }
    }

    function makeIconSprite(iconCls) {
        try {
            var canvas = document.createElement('canvas');
            canvas.width = 128; canvas.height = 128;
            var ctx = canvas.getContext('2d');
            ctx.fillStyle = '#fff';
            ctx.shadowColor = 'rgba(0,0,0,0.5)';
            ctx.shadowBlur = 10;
            ctx.font = '900 70px "Font Awesome 6 Free"';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            var char = "\uf118"; 
            if (iconCls.includes("mug-hot")) char = "\uf7b6";
            if (iconCls.includes("pizza-slice")) char = "\uf818";
            if (iconCls.includes("trophy")) char = "\uf091";
            ctx.fillText(char, 64, 64);
            var tex = new THREE.CanvasTexture(canvas);
            var mat = new THREE.SpriteMaterial({ map: tex, transparent: true });
            var s = new THREE.Sprite(mat);
            s.scale.set(0.6, 0.6, 1);
            return s;
        } catch (e) { return null; }
    }

    window.sendReaction = function(code) {
        if (!sessionId) return;
        fetch("/api/game/" + sessionId + "/react", {
            method: "POST",
            headers: authHeaders(true),
            body: JSON.stringify({ code: code })
        }).catch(function(e){ console.error("Send reaction error:", e); });
    };

    function connectWebSocket() {
        if (typeof SockJS === "undefined" || typeof Stomp === "undefined") return;
        var socket = new SockJS("/ws/game");
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, function () {
            if (liveBadge) { liveBadge.textContent = "Online"; liveBadge.style.color = "#2e7d32"; }
            
              

              
                stompClient.subscribe("/topic/game/" + sessionId + "/reactions", function (msg) {
                    try {
                        var data = JSON.parse(msg.body);
                        if (data.type === "REACTION") {
                            showReaction(data.playerId, data.code, data.icon);
                        }
                    } catch(e) { console.error("WS reaction error:", e); }
                });

                stompClient.subscribe("/topic/game/" + sessionId, function (msg) {
                applyStateWithAnimation(JSON.parse(msg.body));
            });
            /* Po reconnect synchronizuj stan: bot mogl rzucic offline, REST da nam najnowsza wersje. */
            if (wsWasConnected) {
                fetch("/api/game/" + sessionId + "/state")
                    .then(function (r) { return r.json(); })
                    .then(function (state) {
                        /* Brak animacji - tylko UI sync (key nie pasuje wiec nie animuje 2x). */
                        var prevQueueLen = animationQueue.length;
                        applyStateWithAnimation(state);
                        if (animationQueue.length > prevQueueLen) {
                            /* Jesli REST zwrocil ruch ktorego nie znalismy - bedzie animowany przez kolejke. */
                        }
                    })
                    .catch(function () {});
            }
            wsWasConnected = true;
        }, function () {
            if (liveBadge) liveBadge.textContent = "Offline";
            setTimeout(connectWebSocket, 4000);
        });
    }

    /* Polling: co 4s sprawdz stan gdy WS offline.
       Dodatkowo co 5s: "watchdog" - jesli WS online ale rollBtn jest disabled a powinno byc
       enabled (lastState.myTurn=true, !animating, !pendingPurchase), to znaczy ze zgubionym WS
       wiadomosci - odswiezamy stan. */
    setInterval(function () {
        if (!stompClient || !stompClient.connected) {
            fetch("/api/game/" + sessionId + "/state")
                .then(function (r) { return r.json(); })
                .then(function (s) { applyStateWithAnimation(s); })
                .catch(function () {});
        }
    }, 4000);

    setInterval(function () {
        /* BUG FIX: dodano pendingPayment i pendingUpgrade — watchdog nie odpala sie podczas faz decyzji */
        if (!animating && animationQueue.length === 0 &&
            lastState && lastState.myTurn &&
            !lastState.pendingPurchase && !lastState.pendingPayment && !lastState.pendingUpgrade &&
            !lastState.pendingBuyback && !lastState.pendingTakeover &&
            lastState.status !== "FINISHED" &&
            rollBtn && rollBtn.disabled) {
            fetch("/api/game/" + sessionId + "/state")
                .then(function (r) { return r.json(); })
                .then(function (s) { applyStateWithAnimation(s); })
                .catch(function () {});
        }
    }, 5000);

    function loadState() {
        fetch("/api/game/" + sessionId + "/state", { headers: authHeaders(false) })
            .then(function (r) {
                return r.json().then(function (d) { return { ok: r.ok, data: d }; });
            })
            .then(function (res) {
                if (!res.ok) {
                    if (loader) loader.innerHTML = "<span>" + ((res.data && res.data.error) || "Blad ladowania gry.") + "</span>";
                    return;
                }
                var state = res.data;
                if (state.status === "WAITING") {
                    window.location.href = "/game/" + sessionId;
                    return;
                }
                if (state.status === "FINISHED") {
                    window.location.href = "/game";
                    return;
                }
                if (state.tileNames) {
                    TILE_NAMES = state.tileNames;
                    TILE_EFFECTS = state.tileEffects || [];
                    buildBoard(TILE_NAMES, TILE_EFFECTS);
                }
                applyStateWithAnimation(state);
            })
            .catch(function () {
                if (loader) loader.innerHTML = "<span>Nie udalo sie polaczyc z serwerem.</span>";
            });
    }

    if (rollBtn) {
        rollBtn.addEventListener("click", function () {
            if (!lastState) {
                pushToast("Ladowanie stanu gry...", "info", "fa-spinner");
                loadState();
                return;
            }
            lastState = fixIsMe(lastState);
            if (animating) {
                pushToast("Poczekaj na koniec animacji.", "info", "fa-hourglass-half");
                return;
            }
            if (!lastState.myTurn) {
                var cur = lastState.players.find(function (p) { return sameId(p.id, lastState.currentTurnPlayerId); });
                pushToast("Tura: " + (cur ? cur.name : "innego gracza"), "info", "fa-clock");
                return;
            }
            if (lastState.pendingPurchase || lastState.pendingPayment || lastState.pendingUpgrade || lastState.pendingBuyback || lastState.pendingTakeover) {
                pushToast("Najpierw zakoncz decyzje na planszy.", "info", "fa-hand");
                return;
            }
            if (rollInFlight) return;
            rollInFlight = true;
            rollBtn.disabled = true;
            fetch("/api/game/" + sessionId + "/roll", { method: "POST", headers: authHeaders(false) })
                .then(function (r) { return r.json().then(function (d) { return { ok: r.ok, data: d }; }); })
                .then(function (res) {
                    if (!res.ok) {
                        rollInFlight = false;
                        var errMsg = (res.data && res.data.error) || "Blad rzutu.";
                        pushToast(errMsg, "info", "fa-triangle-exclamation");
                        rollBtn.disabled = lastState && lastState.myTurn && !animating && !rollInFlight;
                        fetch("/api/game/" + sessionId + "/state", { headers: authHeaders(false) })
                            .then(function (r) { return r.json(); })
                            .then(function (s) { applyStateWithAnimation(s); })
                            .catch(function () {});
                        return;
                    }
                    applyStateWithAnimation(res.data);
                })
                .catch(function () {
                    rollInFlight = false;
                    pushToast("Blad polaczenia przy rzucie.", "info", "fa-triangle-exclamation");
                    if (lastState) rollBtn.disabled = lastState.myTurn && !animating;
                });
        });
    }

    var transferBtn = document.getElementById("transferBtn");
    if (transferBtn) {
        transferBtn.addEventListener("click", function () {
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
    }

    initThree();
    loadPawnModels();
    loadBuildingModels();
    loadState();
    // ============================================================
    // KARTY W RECE
    // ============================================================
    var pendingDestroyCard = false;

    var handCardsData = [];    /* BUG FIX: cache kart — nie czyscimy przy WS broadcast (null) */
    var propertyCardsData = []; /* BUG FIX: cache posesji — tak samo jak handCardsData */

    function renderHandCards(state) {
        var list  = document.getElementById("handCardsList");
        var count = document.getElementById("handCardCount");
        if (!list) return;

        /* BUG FIX: jesli serwer nie wyslal kart (WS broadcast = null), zachowaj ostatnie dane */
        if (state.myHandCards !== null && state.myHandCards !== undefined) {
            handCardsData = state.myHandCards;
        }
        var cards = handCardsData;

        if (count) count.textContent = cards.length > 0 ? cards.length : "";
        var toggle = document.getElementById("handCardsToggle");
        if (toggle) {
            toggle.disabled = !cards || cards.length === 0;
            toggle.style.opacity = cards.length > 0 ? "1" : "0.45";
            toggle.title = cards.length > 0 ? "Twoje karty (" + cards.length + ")" : "Brak kart w tej grze";
        }

        if (!cards || cards.length === 0) {
            list.innerHTML = '<p class="muted" style="font-size:.82rem;padding:.4rem;">Brak kart — rozdane na poczatku gry.</p>';
            return;
        }

        var html = "";
        cards.forEach(function(card) {
            html += '<details class="hand-card-expand">' +
                '<summary class="hand-card">' +
                '<div class="hand-card-icon"><i class="' + escapeHtml(card.iconClass) + '"></i></div>' +
                '<div class="hand-card-body">' +
                '<strong>' + escapeHtml(card.label) + '</strong>' +
                '<span class="muted hand-card-teaser">Kliknij po szczegoly</span>' +
                '</div>' +
                '<span class="hand-card-chevron"><i class="fa-solid fa-chevron-down"></i></span>' +
                '</summary>' +
                '<div class="hand-card-detail">' +
                '<p>' + escapeHtml(card.description) + '</p>' +
                '<button class="btn btn-small btn-play-card" data-type="' + escapeHtml(card.type) + '">' +
                '<i class="fa-solid fa-play"></i> Zagraj karte</button>' +
                '</div></details>';
        });
        list.innerHTML = html;

        list.querySelectorAll(".btn-play-card").forEach(function(btn) {
            btn.addEventListener("click", function() {
                var cardType = btn.getAttribute("data-type");
                if (cardType === "DESTROY_PROPERTY") {
                    showDestroyTargetPanel(state, cardType);
                } else if (cardType === "TELEPORT") {
                    showTeleportPanel(state);
                } else if (cardType === "FREE_UPGRADE") {
                    showFreeUpgradePanel(state);
                } else {
                    playCard(cardType, null);
                }
            });
        });
    }

    function showDestroyTargetPanel(state, cardType) {
        var panel = document.getElementById("destroyTargetPanel");
        var sel   = document.getElementById("destroyTargetSelect");
        if (!panel || !sel) return;

        sel.innerHTML = "";
        var me = state.players.find(function(p) { return p.isMe; });
        // Zbierz pola nalezace do RYWALI
        state.players.forEach(function(p) {
            if (!p.isMe && !p.bankrupt && p.ownedPositions) {
                p.ownedPositions.forEach(function(pos) {
                    var opt = document.createElement("option");
                    opt.value = pos;
                    var names = state.tileNames || [];
                    opt.textContent = (names[pos] || "Pole " + pos) + " [" + p.name + "]";
                    sel.appendChild(opt);
                });
            }
        });

        if (sel.options.length === 0) {
            pushToast("Brak pol rywali do zniszczenia.", "info", "fa-circle-info");
            return;
        }

        panel.style.display = "block";

        var confirmBtn = document.getElementById("destroyConfirmBtn");
        var cancelBtn  = document.getElementById("destroyCancelBtn");

        if (confirmBtn) {
            confirmBtn.onclick = function() {
                var targetPos = parseInt(sel.value, 10);
                panel.style.display = "none";
                playCard("DESTROY_PROPERTY", targetPos);
            };
        }
        if (cancelBtn) {
            cancelBtn.onclick = function() { panel.style.display = "none"; };
        }
    }

    function showTeleportPanel(state) {
        var panel = document.getElementById("destroyTargetPanel");
        var sel   = document.getElementById("destroyTargetSelect");
        var title = document.getElementById("destroyTargetTitle");
        if (!panel || !sel) return;

        if (title) title.textContent = "Teleport — wybierz pole docelowe";
        sel.innerHTML = "";
        var names = (state && state.tileNames) || [];
        for (var i = 0; i < 40; i++) {
            var opt = document.createElement("option");
            opt.value = i;
            opt.textContent = i + ": " + (names[i] || "Pole " + i);
            sel.appendChild(opt);
        }

        panel.style.display = "block";

        var confirmBtn = document.getElementById("destroyConfirmBtn");
        var cancelBtn  = document.getElementById("destroyCancelBtn");
        if (confirmBtn) {
            confirmBtn.onclick = function() {
                var targetPos = parseInt(sel.value, 10);
                panel.style.display = "none";
                playCard("TELEPORT", targetPos);
            };
        }
        if (cancelBtn) {
            cancelBtn.onclick = function() { panel.style.display = "none"; };
        }
    }

    function showFreeUpgradePanel(state) {
        var panel = document.getElementById("destroyTargetPanel");
        var sel   = document.getElementById("destroyTargetSelect");
        var title = document.getElementById("destroyTargetTitle");
        if (!panel || !sel) return;

        if (title) title.textContent = "Darmowe ulepszenie — wybierz pole z monopolem";
        sel.innerHTML = "";

        if (!state || !state.players) { pushToast("Brak danych stanu gry.", "rent", "fa-circle-xmark"); return; }
        var me = state.players.find(function(p) { return p.isMe; });
        if (!me || !me.ownedPositions) { pushToast("Nie posiadasz zadnych pol.", "info", "fa-circle-info"); return; }

        var names = state.tileNames || [];
        var added = 0;
        me.ownedPositions.forEach(function(pos) {
            if (!state.ownership) return;
            var opt = document.createElement("option");
            opt.value = pos;
            var lvl = (me.propertyLevels && me.propertyLevels[pos]) || 0;
            opt.textContent = pos + ": " + (names[pos] || "Pole " + pos) + " [Lvl " + lvl + "]";
            sel.appendChild(opt);
            added++;
        });

        if (added === 0) {
            pushToast("Nie posiadasz pol mozliwych do ulepszenia.", "info", "fa-circle-info");
            return;
        }

        panel.style.display = "block";

        var confirmBtn = document.getElementById("destroyConfirmBtn");
        var cancelBtn  = document.getElementById("destroyCancelBtn");
        if (confirmBtn) {
            confirmBtn.onclick = function() {
                var targetPos = parseInt(sel.value, 10);
                panel.style.display = "none";
                playCard("FREE_UPGRADE", targetPos);
            };
        }
        if (cancelBtn) {
            cancelBtn.onclick = function() { panel.style.display = "none"; };
        }
    }

    function playCard(cardType, targetPos) {
        var body = { cardType: cardType };
        if (targetPos != null) body.targetPos = targetPos;
        fetch("/api/game/" + sessionId + "/play-card", {
            method: "POST",
            headers: authHeaders(true),
            body: JSON.stringify(body)
        })
        .then(function(r) { return r.json().then(function(d) { return { ok: r.ok, data: d }; }); })
        .then(function(res) {
            if (!res.ok) {
                pushToast(res.data.error || "Blad zagrycia karty.", "rent", "fa-circle-xmark");
                return;
            }
            var panel = document.getElementById("handCardsPanel");
            if (panel) panel.style.display = "none";
            pushToast("Karta zagrana!", "info", "fa-solid fa-cards-blank");
            applyStateWithAnimation(res.data);
        });
    }

    // ============================================================
    // PANEL ULEPSZENIA NIERUCHOMOSCI
    // ============================================================
    /* Generuje karty postepu ulepszenia w stylu Business Tour.
       currentLevel = aktualny poziom PRZED ulepszeniem (0=pole, 1=1dom, 2=2domy, 3=3domy).
       Podswietlona karta = currentLevel+1 (co budujemy). */
    function buildProgressCards(currentLevel) {
        var stages = [
            { label: "Pole",      icons: '<i class="fa-solid fa-square-full" style="font-size:.5rem;opacity:.55"></i>' },
            { label: "1 dom",     icons: '<i class="fa-solid fa-house-chimney-window"></i>' },
            { label: "2 domy",    icons: '<i class="fa-solid fa-house-chimney-window"></i><i class="fa-solid fa-house-chimney-window"></i>' },
            { label: "3 domy",    icons: '<i class="fa-solid fa-house-chimney-window"></i><i class="fa-solid fa-house-chimney-window"></i><i class="fa-solid fa-house-chimney-window"></i>' },
            { label: "Biurowiec", icons: '<i class="fa-solid fa-building"></i>' }
        ];
        var nextLevel = currentLevel + 1; /* poziom ktory budujemy */
        var html = '<div class="bt-upgrade-progress">';
        stages.slice(0, 4).forEach(function(st, i) {
            /* Gdy budujemy Biurowiec (nextLevel=4), ostatnia karta (i=3) staje sie "Biurowiec" i jest aktywna */
            var isBiurowiecCard = (nextLevel >= 4 && i === 3);
            var isActive = (i === nextLevel) || isBiurowiecCard;
            var isDone   = (i < nextLevel) && !isBiurowiecCard;
            var cls   = isDone ? "bt-prog-card--done" : (isActive ? "bt-prog-card--active" : "bt-prog-card--locked");
            var check = isDone ? '<span class="bt-prog-check">&#10003;</span>' : '';
            var label = isBiurowiecCard ? stages[4].label : st.label;
            var icons = isBiurowiecCard ? stages[4].icons : st.icons;
            html += '<div class="bt-prog-card ' + cls + '">' +
                '<div class="bt-prog-icon">' + icons + '</div>' +
                '<div class="bt-prog-label">' + label + '</div>' +
                check + '</div>';
        });
        html += '</div>';
        return html;
    }

    function renderUpgradePanel(state, ap) {
        var pu = state.pendingUpgrade;
        if (!pu) { ap.style.display = "none"; ap.innerHTML = ""; return; }

        var amDecider = state.players.some(function(p) { return p.isMe && p.id === pu.deciderId; });
        var decider   = state.players.find(function(p) { return p.id === pu.deciderId; }) || {};

        /* currentLevel: 0=grunt, 1=1dom, 2=2domy, 3=3domy; budujemy currentLevel+1 */
        var nextLevelLabels = ["1 dom", "2 domy", "3 domy", "Biurowiec"];
        var nextLabel = nextLevelLabels[pu.currentLevel] || "ulepszenie";
        var nextIcon  = pu.currentLevel >= 3 ? "fa-building" : "fa-house-chimney-window";

        var html = '<div class="hud-panel-head">' +
            '<h2><i class="fa-solid ' + nextIcon + '"></i> Ulepszenie</h2>';
        if (amDecider) html += '<span id="actionTimerLabel" class="action-timer-label">15s</span>';
        html += '</div>';
        if (amDecider) html += '<div class="action-timer-track"><div id="actionTimerBar" class="action-timer-bar"></div></div>';
        html += '<p class="action-tile-name"><i class="fa-solid fa-flag"></i> ' + escapeHtml(pu.tileName) + '</p>';

        html += buildProgressCards(pu.currentLevel);

        if (amDecider) {
            html += '<div class="bt-purchase-meta">' +
                '<span>Nowy czynsz: <span class="rent-val">' + formatCash(pu.newRent) + ' PLN</span></span>' +
                '</div>' +
                '<div class="action-buttons">' +
                '<button class="btn btn-bt-roll" id="btnUpgrade">' +
                '<i class="fa-solid ' + nextIcon + '"></i> Buduj ' + nextLabel + ' — ' + formatCash(pu.cost) + ' PLN</button>' +
                '<button class="btn btn-secondary" id="btnSkipUpgrade">Pomin</button>' +
                '</div>' +
                '<p class="muted small" style="text-align:center;">Brak siana? Sprzedaj posesje w panelu po lewej (70%).</p>';
        } else {
            html += '<p class="muted" style="text-align:center;">Decyduje: <strong>' +
                escapeHtml(decider.name || "Gracz") + '</strong> — buduje ' + nextLabel + '.</p>';
        }
        html += '<p class="error" id="actionErr" style="display:none;"></p>';
        ap.style.display = "block";
        ap.className = "bt-action-float bt-action-anim";
        ap.innerHTML = html;

        if (amDecider) startActionTimer(Date.now() + 15000);
        else clearActionTimer();

        var btnUpgrade = document.getElementById("btnUpgrade");
        if (btnUpgrade) btnUpgrade.addEventListener("click", function() { postAction("/upgrade"); });
        var btnSkipUpgrade = document.getElementById("btnSkipUpgrade");
        if (btnSkipUpgrade) btnSkipUpgrade.addEventListener("click", function() { postAction("/skip-upgrade"); });
    }

    function renderBuybackPanel(state, ap) {
        var pb = state.pendingBuyback;
        if (!pb) { ap.style.display = "none"; ap.innerHTML = ""; return; }

        var amVictim = state.players.some(function(p) { return p.isMe && p.id === pb.victimId; });
        var panelKey = "buyback|" + pb.victimId + "|" + pb.position + "|" + pb.price;
        if (panelKey === lastActionPanelKey) return;
        lastActionPanelKey = panelKey;

        var html = '<div class="hud-panel-head"><h2><i class="fa-solid fa-rotate-left"></i> Odkup posesji</h2>';
        if (amVictim) html += '<span id="actionTimerLabel" class="action-timer-label">30s</span>';
        html += '</div>';
        if (amVictim) html += '<div class="action-timer-track"><div id="actionTimerBar" class="action-timer-bar"></div></div>';
        html += '<p class="action-tile-name"><i class="fa-solid fa-flag"></i> ' + escapeHtml(pb.tileName) + '</p>' +
            '<p class="muted">Pole ma teraz <strong>' + escapeHtml(pb.holderName || "rywal") + '</strong>.</p>' +
            '<p class="muted action-tile-price">Odkup za <strong>' + pb.price + ' PLN</strong> (2x cena zakupu)</p>';

        if (amVictim) {
            var me = state.players.find(function(p) { return p.isMe; });
            html += '<p class="muted small">Twoje siano: <strong>' + (me && me.cash != null ? me.cash : 0) + ' PLN</strong></p>' +
                '<p class="muted small">Brakuje? Sprzedaj inna nieruchomosc z panelu <strong>Moje posesje</strong> po lewej.</p>' +
                '<div class="action-buttons">' +
                '<button class="btn btn-bt-roll" id="btnBuyback"><i class="fa-solid fa-rotate-left"></i> Odkup za ' + pb.price + ' PLN</button>' +
                '<button class="btn btn-secondary" id="btnSkipBuyback">Rezygnuj</button>' +
                '</div>';
        } else {
            html += '<p class="muted small">Gracz moze odkupic swoje pole lub zrezygnowac.</p>';
        }
        html += '<p class="error" id="actionErr" style="display:none;"></p>';
        ap.style.display = "block";
        ap.className = "bt-action-float bt-action-anim";
        ap.innerHTML = html;

        if (amVictim) startActionTimer(Date.now() + 30000);
        else clearActionTimer();

        var btnBuyback = document.getElementById("btnBuyback");
        if (btnBuyback) btnBuyback.addEventListener("click", function() { postAction("/buyback"); });
        var btnSkip = document.getElementById("btnSkipBuyback");
        if (btnSkip) btnSkip.addEventListener("click", function() { postAction("/skip-buyback"); });
    }

    /* Business Tour: wykup cudzej dzialki po wyladowaniu i oplaceniu czynszu. */
    function renderTakeoverPanel(state, ap) {
        var pt = state.pendingTakeover;
        if (!pt) { ap.style.display = "none"; ap.innerHTML = ""; return; }

        var amBuyer = state.players.some(function(p) { return p.isMe && sameId(p.id, pt.buyerId); });
        var panelKey = "takeover|" + pt.buyerId + "|" + pt.position + "|" + pt.price;
        if (panelKey === lastActionPanelKey) return;
        lastActionPanelKey = panelKey;

        var html = '<div class="hud-panel-head"><h2><i class="fa-solid fa-handshake"></i> Wykup dzialki</h2>';
        if (amBuyer) html += '<span id="actionTimerLabel" class="action-timer-label">15s</span>';
        html += '</div>';
        if (amBuyer) html += '<div class="action-timer-track"><div id="actionTimerBar" class="action-timer-bar"></div></div>';
        html += '<p class="action-tile-name"><i class="fa-solid fa-flag"></i> ' + escapeHtml(pt.tileName) + '</p>' +
            '<p class="muted">Wlasciciel: <strong>' + escapeHtml(pt.sellerName || "rywal") + '</strong>.</p>' +
            '<p class="muted action-tile-price">Wykup za <strong>' + pt.price + ' PLN</strong> (2x cena + ulepszenia)</p>';

        if (amBuyer) {
            var me = state.players.find(function(p) { return p.isMe; });
            html += '<p class="muted small">Twoje siano: <strong>' + (me && me.cash != null ? me.cash : 0) + ' PLN</strong></p>' +
                '<div class="action-buttons">' +
                '<button class="btn btn-bt-roll" id="btnBuyout"><i class="fa-solid fa-handshake"></i> Wykup za ' + pt.price + ' PLN</button>' +
                '<button class="btn btn-secondary" id="btnSkipBuyout">Pomin</button>' +
                '</div>' +
                '<p class="muted small" style="text-align:center;">Masz 15s — po czasie pole zostaje u wlasciciela.</p>';
        } else {
            html += '<p class="muted small">Gracz moze wykupic to pole lub pominac.</p>';
        }
        html += '<p class="error" id="actionErr" style="display:none;"></p>';
        ap.style.display = "block";
        ap.className = "bt-action-float bt-action-anim";
        ap.innerHTML = html;

        if (amBuyer) startActionTimer(Date.now() + 15000);
        else clearActionTimer();

        var btnBuyout = document.getElementById("btnBuyout");
        if (btnBuyout) btnBuyout.addEventListener("click", function() { postAction("/buyout"); });
        var btnSkipBuyout = document.getElementById("btnSkipBuyout");
        if (btnSkipBuyout) btnSkipBuyout.addEventListener("click", function() { postAction("/skip-buyout"); });
    }

    function propertyLevelLabel(level) {
        if (level >= 4) return "Biurowiec";
        if (level === 3) return "3 domy";
        if (level === 2) return "2 domy";
        if (level === 1) return "1 dom";
        return "Sam grunt";
    }

    function levelDots(level) {
        var MAX = 4;
        var icons = [
            '<i class="fa-solid fa-house" style="font-size:.55rem;"></i>',
            '<i class="fa-solid fa-house" style="font-size:.55rem;"></i>',
            '<i class="fa-solid fa-house-chimney" style="font-size:.6rem;"></i>',
            '<i class="fa-solid fa-building" style="font-size:.62rem;"></i>'
        ];
        var html = '<span style="display:inline-flex;gap:2px;align-items:center;">';
        for (var i = 1; i <= MAX; i++) {
            var filled = i <= level;
            html += '<span style="display:inline-flex;align-items:center;justify-content:center;width:14px;height:14px;border-radius:4px;' +
                (filled
                    ? 'background:rgba(245,158,11,.2);color:var(--brand-gold);border:1px solid rgba(245,158,11,.4);'
                    : 'background:rgba(255,255,255,.04);color:rgba(255,255,255,.18);border:1px solid rgba(255,255,255,.08);') +
                '">' + icons[i-1] + '</span>';
        }
        html += '</span>';
        return html;
    }

    function renderMyProperties(state) {
        var list = document.getElementById("propertiesList");
        var badge = document.getElementById("propertiesCount");
        /* BUG FIX: WS broadcast nie zawiera myPropertyCards (null) — uzywamy cache.
           Aktualizujemy cache TYLKO gdy serwer jawnie wyslal dane (REST response). */
        if (state.myPropertyCards !== null && state.myPropertyCards !== undefined) {
            propertyCardsData = state.myPropertyCards;
        }
        var cards = propertyCardsData;
        if (!list) return;
        if (badge) badge.textContent = cards.length > 0 ? String(cards.length) : "";
        if (cards.length === 0) {
            list.innerHTML = '<p style="padding:.5rem .4rem;color:var(--slate-500);font-size:.75rem;">' +
                '<i class="fa-solid fa-city" style="margin-right:.3rem;opacity:.35;"></i>Brak nieruchomosci.</p>';
            return;
        }
        var debtPhase = state.pendingPayment && state.players.some(function(p) {
            return p.isMe && p.id === state.pendingPayment.debtorId;
        });
        var html = '';
        cards.forEach(function(card) {
            var isUpgradeTarget = state.pendingUpgrade && state.pendingUpgrade.position === card.position;
            var lvl = card.level || 1;
            var dots = levelDots(lvl);
            var levelLbl = escapeHtml(card.levelLabel || propertyLevelLabel(lvl));
            html += '<details class="bt-property-item' + (isUpgradeTarget ? ' bt-property-item--active' : '') + '"' +
                (debtPhase || isUpgradeTarget ? ' open' : '') + '>' +
                '<summary>' +
                '<span class="bt-prop-name">' + escapeHtml(card.tileName) + '</span>' +
                '<span class="bt-property-meta">' + levelLbl + '</span>' +
                '</summary>' +
                '<div class="bt-property-body">' +
                '<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:.3rem;">' +
                dots +
                '<span style="font-size:.72rem;color:var(--slate-400);">Poziom ' + lvl + '/4</span>' +
                '</div>' +
                '<div class="bt-property-stats">' +
                '<div class="bt-prop-stat"><span class="lbl">Czynsz</span><strong class="rent-highlight">' + formatCash(card.currentRent) + ' PLN</strong></div>' +
                '<div class="bt-prop-stat"><span class="lbl">Sprzedaz (70%)</span><span>' + formatCash(card.bankSellPrice) + ' PLN</span></div>';
            if (card.canUpgradeFurther && card.upgradeCost > 0) {
                html += '<div class="bt-prop-stat" style="grid-column:1/-1;"><span class="lbl">Nastepny poziom</span>' +
                    '<span>' + formatCash(card.upgradeCost) + ' PLN <span style="color:var(--brand-emerald);">→ ' + formatCash(card.nextRent) + ' PLN czynszu</span></span></div>';
            }
            html += '</div>';
            if (isUpgradeTarget) {
                html += '<p class="bt-prop-alert"><i class="fa-solid fa-hammer"></i> Stoisz tutaj — mozesz ulepszyc w panelu akcji.</p>';
            }
            html += '<button type="button" class="btn btn-secondary btn-small btn-sell-prop" style="width:100%;margin-top:.25rem;" data-pos="' + card.position + '">' +
                '<i class="fa-solid fa-landmark"></i> Sprzedaj do banku — ' + formatCash(card.bankSellPrice) + ' PLN' +
                '</button>' +
                '</div></details>';
        });
        list.innerHTML = html;
        list.querySelectorAll(".btn-sell-prop").forEach(function(btn) {
            btn.addEventListener("click", function(e) {
                e.preventDefault();
                e.stopPropagation();
                var pos = parseInt(btn.getAttribute("data-pos"), 10);
                var sell = function() { postAction("/sell", { position: pos }); };
                if (typeof pbConfirm === "function") {
                    pbConfirm({
                        title: "Sprzedać nieruchomość?",
                        message: "Sprzedać tę nieruchomość do banku za 70% ceny gruntu? Ulepszenia przepadają.",
                        confirmText: "Sprzedaj",
                        danger: true
                    }).then(function(ok) { if (ok) sell(); });
                } else sell();
            });
        });
    }

    // ============================================================
    // RENDEROWANIE DOMKOW / HOTELI NA PLANSZY 3D
    // Model: /models/Buildings.glb — 4 unikalne budynki (1 per poziom upgradu)
    //   levelTemplates[1] = PublicBuilding_6  (vol 37,  h 3.23) — 1 dom (maly domek)
    //   levelTemplates[2] = PublicBuilding_9  (vol 174, h 7.21) — 2 domy (sredni budynek)
    //   levelTemplates[3] = PublicBuilding_8  (vol 211, h 6.72) — 3 domy (willa/blok)
    //   levelTemplates[4] = PublicBuilding_1  (vol 465, h 10.1) — Biurowiec
    // ============================================================
    var upgradeMarkers = {};
    var levelTemplates = [null, null, null, null, null]; /* indeks = poziom 1-4 */
    var buildingsLoadFailed = false;
    /* aliasy dla starszych ref */
    var houseTemplate = null;
    var hotelTemplate = null;
    var tinyTemplate  = null;
    var smallTemplate = null;
    var largeTemplate = null;
    var BUILDING_BASE_Y = 0.2;

    /* Wyciaga konkretny budynek z zaladowanej sceny GLTF.
       Szuka bezposrednio mesha "_Pack3_0" (np. "PublicBuilding_6_Pack3_0"),
       klonuje go, skaluje do targetHeight i centruje. */
    function extractBuildingTemplate(scene, namePart, targetHeight) {
        /* Szukamy MESHA o nazwie zawierajacej namePart — np. "PublicBuilding_6_Pack3_0" */
        var meshNode = null;
        scene.traverse(function (o) {
            if (!meshNode && o.isMesh && o.name && o.name.indexOf(namePart) >= 0) {
                meshNode = o;
            }
        });
        if (!meshNode) {
            console.warn("[board3d] Brak mesha budynku:", namePart);
            return null;
        }
        /* Klonujemy mesh (geometria jest wspoldzielona, material klonujemy oddzielnie) */
        var meshClone = meshNode.clone(false);
        if (meshClone.geometry) meshClone.geometry = meshNode.geometry; /* wspolna geometria jest OK */
        if (meshNode.material) {
            var mats = Array.isArray(meshNode.material) ? meshNode.material : [meshNode.material];
            meshClone.material = mats.map(function (m) {
                var mc = m.clone();
                mc.side = THREE.FrontSide;
                return mc;
            });
            if (meshClone.material.length === 1) meshClone.material = meshClone.material[0];
        }
        meshClone.castShadow = true;
        meshClone.receiveShadow = true;
        /* Wyzeruj pozycje/rotacje — kopia jest w przestrzeni lokalnej */
        meshClone.position.set(0, 0, 0);
        meshClone.rotation.set(0, 0, 0);
        meshClone.scale.set(1, 1, 1);
        /* Oblicz bounding box surowej geometrii */
        var box = new THREE.Box3().setFromObject(meshClone);
        var size = new THREE.Vector3();
        box.getSize(size);
        var maxDim = Math.max(size.x, size.y, size.z);
        if (maxDim < 0.001) {
            console.warn("[board3d] Budynek zerowego rozmiaru:", namePart);
            return null;
        }
        /* Skaluj tak zeby wysokosc = targetHeight */
        var scaleVal = targetHeight / (size.y > 0.01 ? size.y : maxDim);
        meshClone.scale.setScalar(scaleVal);
        /* Po skalowaniu przelicz BB i wypozycjonuj (dol = y 0) */
        box.setFromObject(meshClone);
        var center = new THREE.Vector3();
        box.getCenter(center);
        meshClone.position.x -= center.x;
        meshClone.position.z -= center.z;
        meshClone.position.y -= box.min.y;
        /* Obrot -90 deg wokol X bo GLTF z FBX czesto ma Y-up vs Z-up */
        var grp = new THREE.Group();
        grp.add(meshClone);
        console.log("[board3d] Template " + namePart + " OK, scale=" + scaleVal.toFixed(4) + ", targetH=" + targetHeight);
        return grp;
    }

    function loadBuildingModels() {
        if (typeof THREE.GLTFLoader === "undefined") {
            console.warn("[board3d] GLTFLoader niedostepny — uzywam fallback boxow");
            buildingsLoadFailed = true;
            if (lastState) updateUpgradeMarkers(lastState);
            return;
        }
        var loader = new THREE.GLTFLoader();
        loader.load("/models/Buildings.glb", function (gltf) {
            /* 4 unikalne budynki - jeden per poziom upgradu, od najmniejszego do największego */
            levelTemplates[1] = extractBuildingTemplate(gltf.scene, "PublicBuilding_6", 0.45); /* maly domek — 1 dom */
            levelTemplates[2] = extractBuildingTemplate(gltf.scene, "PublicBuilding_9", 0.60); /* sredni budynek — 2 domy */
            levelTemplates[3] = extractBuildingTemplate(gltf.scene, "PublicBuilding_8", 0.72); /* willa — 3 domy */
            levelTemplates[4] = extractBuildingTemplate(gltf.scene, "PublicBuilding_1", 0.90); /* biurowiec */
            /* aliasy */
            houseTemplate = levelTemplates[1];
            hotelTemplate = levelTemplates[4];
            tinyTemplate  = levelTemplates[1];
            smallTemplate = levelTemplates[2];
            largeTemplate = levelTemplates[4];
            var loaded = levelTemplates.filter(Boolean).length;
            console.log("[board3d] Buildings.glb: zaladowano " + loaded + "/4 szablonow budynkow");
            if (lastState) updateUpgradeMarkers(lastState);
        }, undefined, function (err) {
            console.warn("[board3d] Blad ladowania Buildings.glb:", err);
            buildingsLoadFailed = true;
            if (lastState) updateUpgradeMarkers(lastState);
        });
    }

    function tintBuildingClone(group, colorHex) {
        var col = new THREE.Color(colorHex || "#ffd700");
        group.traverse(function (o) {
            if (o.isMesh && o.material) {
                o.material = o.material.clone();
                if (o.material.emissive) o.material.emissive.copy(col).multiplyScalar(0.14);
            }
        });
    }

    function placeBuildingMarker(key, template, x, y, z, colorHex, scaleMul, rotY) {
        var marker = template.clone(true);
        tintBuildingClone(marker, colorHex);
        if (scaleMul && scaleMul !== 1) marker.scale.multiplyScalar(scaleMul);
        marker.position.set(x, y, z);
        if (rotY) marker.rotation.y = rotY;
        boardPivot.add(marker);
        upgradeMarkers[key] = marker;
    }

    function addFallbackHouse(posStr, hi, wx, wz, col, offX, offZ) {
        var hh = 0.26, hw = 0.17;
        var house = new THREE.Mesh(
            new THREE.BoxGeometry(hw, hh, hw),
            new THREE.MeshLambertMaterial({ color: 0x4caf50 })
        );
        house.position.set(wx + offX, (hh / 2) + BUILDING_BASE_Y, wz + offZ);
        house.castShadow = true;
        var hRoof = new THREE.Mesh(
            new THREE.ConeGeometry(hw * 0.75, 0.12, 4),
            new THREE.MeshLambertMaterial({ color: col })
        );
        hRoof.position.set(wx + offX, hh + BUILDING_BASE_Y + 0.06, wz + offZ);
        hRoof.rotation.y = Math.PI / 4;
        boardPivot.add(house);
        boardPivot.add(hRoof);
        upgradeMarkers[posStr + "_h" + hi] = house;
        upgradeMarkers[posStr + "_hr" + hi] = hRoof;
    }

    function addFallbackHotel(posStr, wx, wz, col) {
        var h = 0.38, w2 = 0.22;
        var marker = new THREE.Mesh(
            new THREE.BoxGeometry(w2, h, w2),
            new THREE.MeshLambertMaterial({ color: 0xe53935 })
        );
        marker.position.set(wx, (h / 2) + BUILDING_BASE_Y, wz);
        marker.castShadow = true;
        var roofH = 0.16;
        var roof = new THREE.Mesh(
            new THREE.ConeGeometry(w2 * 0.75, roofH, 4),
            new THREE.MeshLambertMaterial({ color: col })
        );
        roof.position.set(wx, h + BUILDING_BASE_Y + roofH / 2, wz);
        roof.rotation.y = Math.PI / 4;
        boardPivot.add(marker);
        boardPivot.add(roof);
        upgradeMarkers[posStr + "_hotel"] = marker;
        upgradeMarkers[posStr + "_hotel_roof"] = roof;
    }

    function updateUpgradeMarkers(state) {
        Object.keys(upgradeMarkers).forEach(function (k) {
            var m = upgradeMarkers[k];
            if (m && threeParent(m)) boardPivot.remove(m);
        });
        upgradeMarkers = {};

        if (!boardPivot || !state || !state.players) return;
        state.players.forEach(function (p) {
            if (!p.propertyLevels) return;
            Object.keys(p.propertyLevels).forEach(function (posStr) {
                var level = Number(p.propertyLevels[posStr]);
                if (!level || level <= 0) return;
                var pos = parseInt(posStr, 10);
                var w = posToWorld(pos);
                var col = p.color || "#ffd700";

                /* Kierunek od krawedzi pola do centrum planszy */
                var dx = 0, dz = 0;
                if (pos < 10) dz = -0.42;
                else if (pos < 20) dx = 0.42;
                else if (pos < 30) dz = 0.42;
                else dx = -0.42;

                /* Srodek budynku: nieznacznie przesunieta w strone centrum */
                var bx = w.x + dx * 0.55;
                var bz = w.z + dz * 0.55;

                /* Rotacja: gorne i dolne pola biegna poziomo (os X) — obróc model o 90° */
                var rotY = 0;
                if (pos > 0 && pos < 10) rotY = Math.PI / 2;
                else if (pos > 20 && pos < 30) rotY = Math.PI / 2;

                /* 1 unikalny model per poziom upgradu (nie wielokrotnosci tego samego) */
                var clampedLevel = Math.min(level, 4);
                var tmpl = levelTemplates[clampedLevel] || levelTemplates[3] || levelTemplates[2] || levelTemplates[1];

                if (tmpl) {
                    placeBuildingMarker(posStr + "_bld", tmpl, bx, BUILDING_BASE_Y, bz, col, 1.0, rotY);
                } else {
                    /* Fallback box jezeli GLB jeszcze sie laduje */
                    if (level >= 3) {
                        addFallbackHotel(posStr, bx, bz, new THREE.Color(col));
                    } else {
                        addFallbackHouse(posStr, 0, bx, bz, new THREE.Color(col), 0, 0);
                    }
                }
            });
        });
    }

    /* ===== TOGGLE: Moje posesje ===== */
    (function () {
        var toggle = document.getElementById("propertiesToggle");
        var panel  = document.getElementById("propertiesPanel");
        if (toggle && panel) {
            toggle.addEventListener("click", function () {
                panel.style.display = panel.style.display === "none" ? "block" : "none";
            });
        }
    })();

    /* ===== TOGGLE: Karty w rece ===== */
    (function () {
        var toggle = document.getElementById("handCardsToggle");
        var panel  = document.getElementById("handCardsPanel");
        var close  = document.getElementById("handCardsClose");
        if (toggle && panel) {
            toggle.addEventListener("click", function () {
                if (toggle.disabled) return;
                var transferPanel = document.getElementById("transferPanel");
                if (transferPanel) transferPanel.style.display = "none";
                panel.style.display = panel.style.display === "none" ? "block" : "none";
            });
        }
        if (close && panel) {
            close.addEventListener("click", function () { panel.style.display = "none"; });
        }
    })();

    /* ===== TOGGLE: Przelewanie ===== */
    (function () {
        var toggle = document.getElementById("transferToggle");
        var panel  = document.getElementById("transferPanel");
        var close  = document.getElementById("transferClose");
        if (toggle && panel) {
            toggle.addEventListener("click", function () {
                var handPanel = document.getElementById("handCardsPanel");
                if (handPanel) handPanel.style.display = "none";
                panel.style.display = panel.style.display === "none" ? "block" : "none";
            });
        }
        if (close && panel) {
            close.addEventListener("click", function () { panel.style.display = "none"; });
        }
    })();

    connectWebSocket();
})();
