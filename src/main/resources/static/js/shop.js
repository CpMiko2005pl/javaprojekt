/* Sklep PB: kupno skrzynki za monety + karuzela CS2-style (jak dawny lootbox). */
(function () {
    "use strict";

    var buyButtons = document.querySelectorAll(".shop-buy-btn");
    if (!buyButtons.length) return;

    var strip = document.getElementById("lootboxStrip");
    var resultEl = document.getElementById("lootboxResult");
    var inventoryGrid = document.getElementById("inventoryGrid");
    var coinsBadge = document.getElementById("coinsBadge");
    var ITEM_WIDTH = 168; // px - musi pasowac do CSS .lootbox-card width + gap

    var rolling = false;

    function csrf() {
        var t = document.querySelector('meta[name="_csrf"]');
        var h = document.querySelector('meta[name="_csrf_header"]');
        return { header: h ? h.content : null, token: t ? t.content : null };
    }

    function authHeaders() {
        var c = csrf();
        var headers = {};
        if (c.header && c.token) headers[c.header] = c.token;
        return headers;
    }

    function rarityClass(rarity) {
        return "rarity-" + (rarity || "COMMON").toLowerCase();
    }

    function escapeHtml(s) {
        var d = document.createElement("div");
        d.textContent = s == null ? "" : String(s);
        return d.innerHTML;
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

    function easeOutCubic(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    function spinCarousel(items, winnerIdx, onDone) {
        strip.innerHTML = "";
        items.forEach(function (it) { strip.appendChild(buildCard(it)); });

        var stripContainer = strip.parentElement;
        var containerWidth = stripContainer.clientWidth;
        var pointerCenter = containerWidth / 2;
        var jitter = Math.floor(Math.random() * 21) - 10;
        var targetCenter = winnerIdx * ITEM_WIDTH + ITEM_WIDTH / 2 + jitter;
        var endX = pointerCenter - targetCenter;

        var dur = 5500;
        var t0 = performance.now();
        strip.style.transform = "translate3d(0px, 0, 0)";
        strip.style.transition = "none";

        function frame(now) {
            var t = Math.min(1, (now - t0) / dur);
            var x = (endX) * easeOutCubic(t);
            strip.style.transform = "translate3d(" + x + "px, 0, 0)";
            if (t < 1) requestAnimationFrame(frame);
            else onDone();
        }
        requestAnimationFrame(frame);
    }

    function setButtonsDisabled(state) {
        buyButtons.forEach(function (b) { b.disabled = state; });
    }

    function refreshAffordable(coins) {
        buyButtons.forEach(function (b) {
            var price = parseInt(b.getAttribute("data-price"), 10) || 0;
            b.disabled = coins < price;
        });
    }

    buyButtons.forEach(function (btn) {
        btn.addEventListener("click", function () {
            if (rolling) return;
            var box = btn.getAttribute("data-box");
            var price = parseInt(btn.getAttribute("data-price"), 10) || 0;
            var coins = parseInt(coinsBadge ? coinsBadge.textContent : "0", 10) || 0;
            if (coins < price) {
                showError("Za malo monet — potrzebujesz " + price + ".");
                return;
            }

            rolling = true;
            setButtonsDisabled(true);
            if (resultEl) { resultEl.classList.remove("visible"); resultEl.innerHTML = ""; }
            var carousel = document.getElementById("lootboxCarousel");
            if (carousel) carousel.style.display = "block";

            fetch("/shop/buy/" + encodeURIComponent(box), { method: "POST", headers: authHeaders() })
                .then(function (r) { return r.json().then(function (d) { return { ok: r.ok, data: d }; }); })
                .then(function (res) {
                    if (!res.ok) {
                        rolling = false;
                        refreshAffordable(coins);
                        showError(res.data.error || "Nie udalo sie kupic skrzynki.");
                        return;
                    }
                    spinCarousel(res.data.strip, 40, function () {
                        var added = res.data.addedToInventory !== false;
                        showResult(res.data.winner, added, res.data.inventoryNote);
                        if (coinsBadge) coinsBadge.textContent = res.data.coins;
                        if (added && inventoryGrid) {
                            var card = buildInventoryCard(res.data.winner);
                            card.classList.add("just-obtained");
                            inventoryGrid.insertBefore(card, inventoryGrid.firstChild);
                            if (typeof window.refreshInventoryPanel === "function") {
                                window.refreshInventoryPanel({ goFirstPage: true });
                            }
                        }
                        rolling = false;
                        refreshAffordable(res.data.coins);
                    });
                })
                .catch(function () {
                    rolling = false;
                    refreshAffordable(coins);
                    showError("Blad polaczenia.");
                });
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
            if (added) showInventoryToast("Wylosowano: " + item.name, false);
            else showInventoryToast(note || "Masz juz ten przedmiot.", true);
        }
    }
})();
