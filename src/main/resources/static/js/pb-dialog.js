/**
 * Globalne modale zamiast alert() / confirm().
 * API: pbConfirm({ title, message, confirmText, cancelText, danger }) -> Promise<boolean>
 *       pbAlert({ title, message, okText, variant }) -> Promise<void>
 * Formularze: data-pb-confirm="tekst" [data-pb-confirm-title] [data-pb-confirm-danger]
 */
(function () {
    "use strict";

    var root, backdrop, panel, iconEl, titleEl, leadEl, actionsEl;
    var resolveFn = null;
    var mode = null; /* 'confirm' | 'alert' */

    function ensureDom() {
        if (root) return;
        root = document.createElement("div");
        root.id = "pbDialog";
        root.className = "pb-dialog";
        root.setAttribute("aria-hidden", "true");
        root.setAttribute("role", "dialog");
        root.setAttribute("aria-modal", "true");
        root.innerHTML =
            '<div class="pb-dialog-backdrop" data-pb-dialog-close></div>' +
            '<div class="pb-dialog-panel">' +
            '  <div class="pb-dialog-icon" aria-hidden="true"><i></i></div>' +
            '  <h2 class="pb-dialog-title"></h2>' +
            '  <p class="pb-dialog-lead"></p>' +
            '  <div class="pb-dialog-actions"></div>' +
            "</div>";
        document.body.appendChild(root);
        backdrop = root.querySelector(".pb-dialog-backdrop");
        panel = root.querySelector(".pb-dialog-panel");
        iconEl = root.querySelector(".pb-dialog-icon i");
        titleEl = root.querySelector(".pb-dialog-title");
        leadEl = root.querySelector(".pb-dialog-lead");
        actionsEl = root.querySelector(".pb-dialog-actions");

        backdrop.addEventListener("click", onCancel);
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && root.classList.contains("visible")) onCancel();
        });
    }

    function open() {
        root.classList.toggle("pb-dialog--dark", document.documentElement.classList.contains("pb-dark"));
        root.classList.add("visible");
        root.setAttribute("aria-hidden", "false");
        document.body.classList.add("pb-dialog-open");
    }

    function close() {
        root.classList.remove("visible");
        root.setAttribute("aria-hidden", "true");
        document.body.classList.remove("pb-dialog-open");
    }

    function onCancel() {
        if (!resolveFn) return;
        var r = resolveFn;
        resolveFn = null;
        close();
        if (mode === "confirm") r(false);
        else r();
    }

    function onConfirm() {
        if (!resolveFn) return;
        var r = resolveFn;
        resolveFn = null;
        close();
        if (mode === "confirm") r(true);
        else r();
    }

    function setIcon(cls, variant) {
        iconEl.className = cls;
        var wrap = iconEl.parentElement;
        wrap.className = "pb-dialog-icon pb-dialog-icon-" + (variant || "default");
    }

    function pbConfirm(opts) {
        ensureDom();
        opts = opts || {};
        mode = "confirm";
        setIcon(opts.icon || (opts.danger ? "fa-solid fa-triangle-exclamation" : "fa-solid fa-circle-question"),
            opts.danger ? "danger" : "default");
        titleEl.textContent = opts.title || "Na pewno?";
        leadEl.textContent = opts.message || "";
        actionsEl.innerHTML =
            '<button type="button" class="btn btn-bt btn-small" data-pb-dialog-cancel>' +
            (opts.cancelText || "Anuluj") + "</button>" +
            '<button type="button" class="btn btn-small ' + (opts.danger ? "btn-danger" : "btn-bt-roll") +
            '" data-pb-dialog-ok>' + (opts.confirmText || "Tak") + "</button>";
        actionsEl.querySelector("[data-pb-dialog-cancel]").addEventListener("click", onCancel);
        actionsEl.querySelector("[data-pb-dialog-ok]").addEventListener("click", onConfirm);
        open();
        return new Promise(function (resolve) { resolveFn = resolve; });
    }

    function pbAlert(opts) {
        ensureDom();
        opts = opts || {};
        mode = "alert";
        var variant = opts.variant || "info";
        var icons = {
            info: "fa-solid fa-circle-info",
            error: "fa-solid fa-triangle-exclamation",
            success: "fa-solid fa-circle-check"
        };
        setIcon(opts.icon || icons[variant] || icons.info, variant === "error" ? "danger" : variant);
        titleEl.textContent = opts.title || (variant === "error" ? "Ups…" : "Informacja");
        leadEl.textContent = opts.message || "";
        actionsEl.innerHTML =
            '<button type="button" class="btn btn-bt-roll btn-small" data-pb-dialog-ok>' +
            (opts.okText || "OK") + "</button>";
        actionsEl.querySelector("[data-pb-dialog-ok]").addEventListener("click", onConfirm);
        open();
        return new Promise(function (resolve) { resolveFn = resolve; });
    }

    function bindForms() {
        document.querySelectorAll("form[data-pb-confirm]").forEach(function (form) {
            if (form.dataset.pbDialogBound) return;
            form.dataset.pbDialogBound = "1";
            form.addEventListener("submit", function (e) {
                if (form.dataset.pbConfirmed === "1") {
                    delete form.dataset.pbConfirmed;
                    return;
                }
                e.preventDefault();
                pbConfirm({
                    title: form.getAttribute("data-pb-confirm-title") || "Na pewno?",
                    message: form.getAttribute("data-pb-confirm") || "",
                    confirmText: form.getAttribute("data-pb-confirm-yes") || "Tak",
                    cancelText: form.getAttribute("data-pb-confirm-no") || "Anuluj",
                    danger: form.hasAttribute("data-pb-confirm-danger")
                }).then(function (ok) {
                    if (ok) {
                        form.dataset.pbConfirmed = "1";
                        if (typeof form.requestSubmit === "function") form.requestSubmit();
                        else form.submit();
                    }
                });
            });
        });
    }

    window.pbConfirm = pbConfirm;
    window.pbAlert = pbAlert;

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bindForms);
    } else {
        bindForms();
    }
})();
