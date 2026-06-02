/* Three.js — podglad planszy 3D (strona glowna, wg politechnika_monopoly.html) */
(function () {
    "use strict";

    var container = document.getElementById("canvas-container");
    if (!container || typeof THREE === "undefined") return;

    var loader = document.getElementById("canvas-loader");
    var resetBtn = document.getElementById("reset-cam-btn");
    var scene, camera, renderer, boardGroup;
    var isDragging = false;
    var prev = { x: 0, y: 0 };
    var rotSpeed = 0.005;

    function init() {
        scene = new THREE.Scene();
        scene.background = new THREE.Color(0x0f172a);
        scene.fog = new THREE.FogExp2(0x0f172a, 0.04);

        var aspect = container.clientWidth / container.clientHeight;
        camera = new THREE.PerspectiveCamera(45, aspect, 0.1, 100);
        resetCamera();

        renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        renderer.setSize(container.clientWidth, container.clientHeight);
        renderer.setPixelRatio(window.devicePixelRatio || 1);
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        container.appendChild(renderer.domElement);

        scene.add(new THREE.AmbientLight(0xffffff, 0.5));

        var key = new THREE.DirectionalLight(0x38bdf8, 1.2);
        key.position.set(10, 15, 10);
        key.castShadow = true;
        scene.add(key);

        var fill = new THREE.DirectionalLight(0xa855f7, 0.8);
        fill.position.set(-10, 10, -10);
        scene.add(fill);

        var spot = new THREE.SpotLight(0xffffff, 1);
        spot.position.set(0, 12, 0);
        spot.angle = Math.PI / 4;
        spot.penumbra = 0.8;
        scene.add(spot);

        boardGroup = new THREE.Group();
        scene.add(boardGroup);
        buildBoard();

        bindControls();
        window.addEventListener("resize", onResize);
        if (resetBtn) resetBtn.addEventListener("click", function () {
            resetCamera();
            boardGroup.rotation.set(0, 0, 0);
        });

        animate();
        setTimeout(function () {
            if (loader) {
                loader.classList.add("hidden");
                setTimeout(function () { loader.style.display = "none"; }, 500);
            }
        }, 700);
    }

    function resetCamera() {
        camera.position.set(0, 14, 16);
        camera.lookAt(0, 0, 0);
    }

    function buildBoard() {
        var boardSize = 10;
        var main = new THREE.Mesh(
            new THREE.BoxGeometry(boardSize, 0.4, boardSize),
            new THREE.MeshPhongMaterial({ color: 0x1e293b, shininess: 80 })
        );
        main.receiveShadow = true;
        boardGroup.add(main);

        var inner = new THREE.Mesh(
            new THREE.BoxGeometry(boardSize - 2, 0.45, boardSize - 2),
            new THREE.MeshPhongMaterial({ color: 0x0f172a })
        );
        inner.position.y = 0.01;
        inner.receiveShadow = true;
        boardGroup.add(inner);

        var towerColors = [0x38bdf8, 0xa855f7, 0x10b981];
        for (var i = 0; i < 3; i++) {
            var h = 1.5 + Math.random() * 2;
            var b = new THREE.Mesh(
                new THREE.BoxGeometry(1, h, 1),
                new THREE.MeshPhongMaterial({ color: towerColors[i], transparent: true, opacity: 0.85, shininess: 100 })
            );
            b.position.set(-1.8 + i * 1.8, h / 2 + 0.2, -1.5 + (i % 2) * 1.5);
            b.castShadow = true;
            boardGroup.add(b);
        }

        var colors = [0x38bdf8, 0xa855f7, 0x10b981, 0xf59e0b, 0xef4444, 0x14b8a6];
        var tileSize = boardSize / 8;
        var half = boardSize / 2;
        var offset = tileSize / 2;

        function addTile(x, z, color, building) {
            var tile = new THREE.Mesh(
                new THREE.BoxGeometry(tileSize * 0.9, 0.48, tileSize * 0.9),
                new THREE.MeshPhongMaterial({ color: color, shininess: 90, specular: 0xffffff })
            );
            tile.position.set(x, 0.02, z);
            tile.receiveShadow = true;
            boardGroup.add(tile);
            if (building) {
                var bh = 0.5 + Math.random() * 0.6;
                var buildingMesh = new THREE.Mesh(
                    new THREE.BoxGeometry(tileSize * 0.4, bh, tileSize * 0.4),
                    new THREE.MeshPhongMaterial({ color: 0xffffff, shininess: 100 })
                );
                buildingMesh.position.set(x, 0.24 + bh / 2, z);
                buildingMesh.castShadow = true;
                boardGroup.add(buildingMesh);
            }
        }

        for (var t = 0; t < 8; t++) {
            var coord = -half + offset + t * tileSize;
            addTile(coord, half - offset, colors[t % colors.length], t % 3 === 0);
            addTile(coord, -half + offset, colors[(t + 2) % colors.length], t % 4 === 1);
        }
        for (var s = 1; s < 7; s++) {
            var c = -half + offset + s * tileSize;
            addTile(-half + offset, c, colors[(s + 1) % colors.length], s % 3 === 1);
            addTile(half - offset, c, colors[(s + 3) % colors.length], s % 3 === 2);
        }

        var piece = new THREE.Mesh(
            new THREE.ConeGeometry(0.3, 0.6, 6),
            new THREE.MeshPhongMaterial({ color: 0xec4899, shininess: 120 })
        );
        piece.position.set(half - offset, 0.6, half - offset);
        piece.castShadow = true;
        boardGroup.add(piece);
        boardGroup.userData.piece = piece;
    }

    function bindControls() {
        container.addEventListener("mousedown", function (e) {
            isDragging = true;
            prev = { x: e.clientX, y: e.clientY };
        });
        container.addEventListener("mousemove", function (e) {
            if (!isDragging) return;
            var dx = e.clientX - prev.x;
            var dy = e.clientY - prev.y;
            boardGroup.rotation.y += dx * rotSpeed;
            var nx = boardGroup.rotation.x + dy * rotSpeed;
            if (nx > -0.5 && nx < 0.5) boardGroup.rotation.x = nx;
            prev = { x: e.clientX, y: e.clientY };
        });
        window.addEventListener("mouseup", function () { isDragging = false; });

        container.addEventListener("wheel", function (e) {
            e.preventDefault();
            camera.position.z += e.deltaY * 0.02;
            camera.position.z = Math.max(10, Math.min(camera.position.z, 28));
            camera.position.y = camera.position.z * 0.85;
        }, { passive: false });
    }

    function onResize() {
        camera.aspect = container.clientWidth / container.clientHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(container.clientWidth, container.clientHeight);
    }

    var bounce = 0;
    function animate() {
        requestAnimationFrame(animate);
        if (!isDragging) boardGroup.rotation.y += 0.002;
        if (boardGroup.userData.piece) {
            bounce += 0.05;
            boardGroup.userData.piece.position.y = 0.6 + Math.abs(Math.sin(bounce)) * 0.4;
            boardGroup.userData.piece.rotation.y += 0.01;
        }
        renderer.render(scene, camera);
    }

    init();
})();
