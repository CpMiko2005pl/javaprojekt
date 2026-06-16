/* Lootbox CS2-style: karuzela przesuwajaca sie w prawo z easeOut, zatrzymuje na losowym itemie. */
(function () {
    "use strict";

    var openBtn = document.getElementById("lootboxOpenBtn");
    if (!openBtn) return;

    var strip = document.getElementById("lootboxStrip");
    var resultEl = document.getElementById("lootboxResult");
    var inventoryGrid = document.getElementById("inventoryGrid");
    var availableBadge = document.getElementById("availableBoxesBadge");
    var ITEM_WIDTH = 168; // px - musi pasowac do CSS .lootbox-card width + gap

    var rolling = false;

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

    function rarityClass(rarity) {
        return "rarity-" + (rarity || "COMMON").toLowerCase();
    }

    function buildCard(item) {
        var card = document.createElement("div");
        card.className = "lootbox-card " + rarityClass(item.rarity);
        card.innerHTML =
            '<div class="lootbox-card-icon" style="color:' + item.rarityColor + '">' +
            '<i class="' + (item.iconClass || "fa-solid fa-cube") + '"></i>' +
            '</div>' +
            '<div class="lootbox-card-name">' + escapeHtml(item.name) + '</div>' +
            '<div class="lootbox-card-rarity" style="color:' + item.rarityColor + '">' +
            (item.rarityLabel || "Common") + '</div>';
        return card;
    }

    function buildInventoryCard(item) {
        var card = document.createElement("div");
        card.className = "inventory-item " + rarityClass(item.rarity);
        card.style.borderColor = item.rarityColor;
        card.innerHTML =
            '<div class="inventory-icon" style="color:' + item.rarityColor + '">' +
            '<i class="' + (item.iconClass || "fa-solid fa-cube") + '"></i>' +
            '</div>' +
            '<div>' +
            '<strong>' + escapeHtml(item.name) + '</strong>' +
            '<span class="inventory-rarity" style="color:' + item.rarityColor + '">' +
            (item.rarityLabel || "Common") + ' &middot; ' + escapeHtml(item.category || "") +
            '</span>' +
            '</div>';
        return card;
    }

    function escapeHtml(s) {
        var d = document.createElement("div");
        d.textContent = s == null ? "" : String(s);
        return d.innerHTML;
    }

    function easeOutCubic(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    function spinCarousel(items, winnerIdx, onDone) {
        strip.innerHTML = "";
        items.forEach(function (it) { strip.appendChild(buildCard(it)); });

        // Pozycja docelowa: srodek wskaznika ma trafic w srodek karty winnerIdx.
        var stripContainer = strip.parentElement;
        var containerWidth = stripContainer.clientWidth;
        var pointerCenter = containerWidth / 2;
        // jitter -10..+10 dla odrobiny niedoskonalosci
        var jitter = Math.floor(Math.random() * 21) - 10;
        var targetCenter = winnerIdx * ITEM_WIDTH + ITEM_WIDTH / 2 + jitter;
        var endX = pointerCenter - targetCenter;

        // Animacja: 5500ms easeOutCubic od 0 do endX
        var startX = 0;
        var dur = 5500;
        var t0 = performance.now();
        strip.style.transform = "translate3d(0px, 0, 0)";
        strip.style.transition = "none";

        function frame(now) {
            var t = Math.min(1, (now - t0) / dur);
            var e = easeOutCubic(t);
            var x = startX + (endX - startX) * e;
            strip.style.transform = "translate3d(" + x + "px, 0, 0)";
            if (t < 1) requestAnimationFrame(frame);
            else onDone();
        }
        requestAnimationFrame(frame);
    }

    openBtn.addEventListener("click", function () {
        if (rolling) return;
        var current = parseInt(availableBadge ? availableBadge.textContent : "0", 10);
        if (!current || current <= 0) {
            showError("Nie masz dostepnych skrzynek - wroc jutro!");
            return;
        }

        rolling = true;
        openBtn.disabled = true;
        if (resultEl) resultEl.classList.remove("visible");
        if (resultEl) resultEl.innerHTML = "";
        /* Pokazuje karuzele jesli jest ukryta */
        var carousel = document.getElementById("lootboxCarousel");
        if (carousel) carousel.style.display = "block";

        fetch("/lootbox/open", { method: "POST", headers: authHeaders(false) })
            .then(function (r) { return r.json().then(function (d) { return { ok: r.ok, data: d }; }); })
            .then(function (res) {
                if (!res.ok) {
                    rolling = false;
                    openBtn.disabled = false;
                    showError(res.data.error || "Nie udalo sie otworzyc skrzynki.");
                    return;
                }
                var winnerIdx = 40;
                spinCarousel(res.data.strip, winnerIdx, function () {
                    var added = res.data.addedToInventory !== false;
                    showResult(res.data.winner, added, res.data.inventoryNote);
                    if (availableBadge) availableBadge.textContent = res.data.availableBoxes;
                    var boxCountLabel = document.getElementById("boxCountLabel");
                    if (boxCountLabel) boxCountLabel.textContent = res.data.availableBoxes;
                    if (added && inventoryGrid) {
                        var card = buildInventoryCard(res.data.winner);
                        card.classList.add("just-obtained");
                        inventoryGrid.insertBefore(card, inventoryGrid.firstChild);
                        if (typeof window.refreshInventoryPanel === "function") {
                            window.refreshInventoryPanel({ goFirstPage: true });
                        }
                    }
                    rolling = false;
                    openBtn.disabled = res.data.availableBoxes <= 0;
                });
            })
            .catch(function () {
                rolling = false;
                openBtn.disabled = false;
                showError("Blad polaczenia.");
            });
    });

    function showError(text) {
        if (!resultEl) {
            if (typeof pbAlert === "function") pbAlert({ message: text, variant: "error" });
            return;
        }
        resultEl.innerHTML =
            '<div class="lootbox-result-error"><i class="fa-solid fa-triangle-exclamation"></i> ' +
            escapeHtml(text) + '</div>';
        resultEl.classList.add("visible");
    }

    function showResult(item, added, note) {
        if (!resultEl) return;
        var dupNote = !added
            ? '<p class="lootbox-dup-note"><i class="fa-solid fa-clone"></i> ' +
              escapeHtml(note || "Masz juz ten przedmiot — nie dodano do ekwipunku.") + '</p>'
            : "";
        resultEl.innerHTML =
            '<div class="lootbox-result-card ' + rarityClass(item.rarity) + '" ' +
            'style="border-color:' + item.rarityColor + '">' +
            '<div class="lootbox-result-icon" style="color:' + item.rarityColor + '">' +
            '<i class="' + (item.iconClass || "fa-solid fa-cube") + '"></i>' +
            '</div>' +
            '<div>' +
            '<span class="lootbox-result-rarity" style="color:' + item.rarityColor + '">' +
            (item.rarityLabel || "Common") + '</span>' +
            '<strong>' + escapeHtml(item.name) + '</strong>' +
            '<p>' + escapeHtml(item.description || "") + '</p>' +
            dupNote +
            '</div>' +
            '</div>';
        resultEl.classList.add("visible");
        if (typeof showInventoryToast === "function") {
            if (added) {
                showInventoryToast("Wylosowano: " + item.name, false);
            } else {
                showInventoryToast(note || "Masz juz ten przedmiot.", true);
            }
        }
    }
})();
