(function () {
    "use strict";
    var btn = document.getElementById("mobile-menu-btn");
    var menu = document.getElementById("mobile-menu");
    if (!btn || !menu) return;

    btn.addEventListener("click", function () {
        menu.classList.toggle("open");
    });

    menu.querySelectorAll("a").forEach(function (link) {
        link.addEventListener("click", function () {
            menu.classList.remove("open");
        });
    });
})();
