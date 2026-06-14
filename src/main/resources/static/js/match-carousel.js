/* Karuzela historii meczow — strzalki przewijaja do starszych (max 6 wpisow). */
(function () {
    "use strict";

    function initCarousel(wrap) {
        var track = wrap.querySelector(".hist-carousel-track");
        var prev = wrap.querySelector(".hist-carousel-prev");
        var next = wrap.querySelector(".hist-carousel-next");
        if (!track || !prev || !next) return;

        var slides = track.querySelectorAll(".hist-slide");
        if (slides.length === 0) return;

        var index = 0;

        function visibleCount() {
            if (window.innerWidth < 640) return 1;
            if (window.innerWidth < 960) return 2;
            return 3;
        }

        function maxIndex() {
            return Math.max(0, slides.length - visibleCount());
        }

        function update() {
            var vis = visibleCount();
            var max = maxIndex();
            if (index > max) index = max;
            var slideW = slides[0].offsetWidth;
            var gap = 10;
            track.style.transform = "translateX(-" + (index * (slideW + gap)) + "px)";
            prev.disabled = index <= 0;
            next.disabled = index >= max;
            prev.style.opacity = prev.disabled ? "0.35" : "1";
            next.style.opacity = next.disabled ? "0.35" : "1";
        }

        prev.addEventListener("click", function () {
            if (index > 0) { index--; update(); }
        });
        next.addEventListener("click", function () {
            if (index < maxIndex()) { index++; update(); }
        });
        window.addEventListener("resize", update);
        update();
    }

    document.querySelectorAll(".hist-carousel-wrap").forEach(initCarousel);
})();
