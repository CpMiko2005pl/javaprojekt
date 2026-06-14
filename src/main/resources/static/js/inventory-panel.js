/* Ekwipunek: zwijanie (localStorage) + paginacja po 6 przedmiotów. */
(function () {
    "use strict";

    var STORAGE_KEY = "pb_inventory_collapsed";
    var PER_PAGE = 6;

    var section = document.getElementById("inventorySection");
    var body = document.getElementById("inventoryBody");
    var toggleBtn = document.getElementById("inventoryToggleBtn");
    var grid = document.getElementById("inventoryGrid");
    var pagerWrap = document.getElementById("inventoryPagerWrap");
    var prevBtn = document.getElementById("inventoryPrev");
    var nextBtn = document.getElementById("inventoryNext");
    var pagerLabel = document.getElementById("inventoryPagerLabel");
    var countEl = document.getElementById("inventoryCount");

    if (!section || !grid) return;

    var currentPage = 0;

    function getItems() {
        return Array.prototype.slice.call(grid.querySelectorAll(".inventory-item"));
    }

    function updateCount() {
        if (countEl) countEl.textContent = String(getItems().length);
    }

    function applyPagination() {
        var items = getItems();
        var total = items.length;
        var pages = Math.max(1, Math.ceil(total / PER_PAGE));
        if (currentPage >= pages) currentPage = pages - 1;
        if (currentPage < 0) currentPage = 0;

        items.forEach(function (el, i) {
            var page = Math.floor(i / PER_PAGE);
            el.hidden = page !== currentPage;
        });

        var showPager = total > PER_PAGE;
        if (pagerWrap) pagerWrap.hidden = !showPager;
        if (pagerLabel) pagerLabel.textContent = (currentPage + 1) + " / " + pages;
        if (prevBtn) {
            prevBtn.disabled = currentPage <= 0;
            prevBtn.style.opacity = prevBtn.disabled ? "0.35" : "1";
        }
        if (nextBtn) {
            nextBtn.disabled = currentPage >= pages - 1;
            nextBtn.style.opacity = nextBtn.disabled ? "0.35" : "1";
        }
        updateCount();
    }

    function setCollapsed(collapsed) {
        section.classList.toggle("inventory-section--collapsed", collapsed);
        if (toggleBtn) {
            toggleBtn.setAttribute("aria-expanded", collapsed ? "false" : "true");
        }
        try {
            localStorage.setItem(STORAGE_KEY, collapsed ? "1" : "0");
        } catch (e) { /* ignore */ }
    }

    function loadCollapsedState() {
        var stored = null;
        try {
            stored = localStorage.getItem(STORAGE_KEY);
        } catch (e) { /* ignore */ }
        /* Domyślnie rozwińty — tylko zapisany "1" zwija. */
        setCollapsed(stored === "1");
    }

    if (toggleBtn) {
        toggleBtn.addEventListener("click", function () {
            setCollapsed(!section.classList.contains("inventory-section--collapsed"));
        });
    }

    if (prevBtn) {
        prevBtn.addEventListener("click", function () {
            if (currentPage > 0) {
                currentPage--;
                applyPagination();
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            var pages = Math.ceil(getItems().length / PER_PAGE);
            if (currentPage < pages - 1) {
                currentPage++;
                applyPagination();
            }
        });
    }

    loadCollapsedState();
    applyPagination();

    window.refreshInventoryPanel = function (opts) {
        if (opts && opts.goFirstPage) currentPage = 0;
        applyPagination();
    };
})();
