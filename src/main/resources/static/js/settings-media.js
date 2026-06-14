(function () {
    function bindFileInput(inputId, nameId, previewImgId, previewWrapId, placeholderId, urlInputId, onDirty) {
        var input = document.getElementById(inputId);
        var nameEl = document.getElementById(nameId);
        var img = document.getElementById(previewImgId);
        var wrap = previewWrapId ? document.getElementById(previewWrapId) : null;
        var placeholder = placeholderId ? document.getElementById(placeholderId) : null;
        var urlInput = urlInputId ? document.getElementById(urlInputId) : null;
        if (!input || !nameEl) return;

        function notifyDirty() {
            if (typeof onDirty === "function") onDirty();
        }

        function showPreview(src) {
            if (!src) return;
            if (!img) {
                img = document.createElement("img");
                img.id = previewImgId;
                img.alt = "Podgląd";
                img.style.width = "100%";
                img.style.height = "100%";
                img.style.objectFit = "cover";
                if (wrap) wrap.insertBefore(img, wrap.firstChild);
            }
            img.src = src;
            img.style.display = "";
            if (wrap) wrap.classList.remove("lp-media-preview-empty");
            if (placeholder) placeholder.style.display = "none";
        }

        input.addEventListener("change", function () {
            var file = input.files && input.files[0];
            if (!file) {
                nameEl.textContent = "Nie wybrano pliku";
                return;
            }
            nameEl.textContent = file.name;
            notifyDirty();
            if (urlInput) urlInput.value = "";

            if (!file.type.startsWith("image/")) return;
            var reader = new FileReader();
            reader.onload = function (ev) {
                showPreview(ev.target.result);
            };
            reader.readAsDataURL(file);
        });

        if (urlInput) {
            urlInput.addEventListener("input", function () {
                notifyDirty();
                var val = urlInput.value.trim();
                if (val) {
                    input.value = "";
                    nameEl.textContent = "Nie wybrano pliku";
                    showPreview(val);
                } else if (img && img.dataset.default) {
                    img.src = img.dataset.default;
                }
            });
        }
    }

    function initProfileSaveButton() {
        var form = document.querySelector('form[action*="/settings/profile"]');
        var btn = document.getElementById("profileSaveBtn");
        var label = document.getElementById("profileSaveLabel");
        var icon = document.getElementById("profileSaveIcon");
        if (!form || !btn || !label) return;

        var saved = !!document.querySelector(".alert-success");
        var baseline = snapshotForm(form);

        function setSavedState() {
            saved = true;
            baseline = snapshotForm(form);
            btn.disabled = true;
            btn.classList.add("lp-btn-saved");
            label.textContent = "Zapisano";
            if (icon) icon.className = "fa-solid fa-check";
        }

        function setDirtyState() {
            if (!saved) return;
            saved = false;
            btn.disabled = false;
            btn.classList.remove("lp-btn-saved");
            label.textContent = "Zapisz zmiany";
            if (icon) icon.className = "fa-solid fa-floppy-disk";
        }

        function checkDirty() {
            if (snapshotForm(form) !== baseline) setDirtyState();
        }

        if (saved) setSavedState();

        form.querySelectorAll("input, textarea, select").forEach(function (el) {
            el.addEventListener("input", checkDirty);
            el.addEventListener("change", checkDirty);
        });

        form.addEventListener("submit", function () {
            btn.disabled = true;
            label.textContent = "Zapisywanie…";
        });

        bindFileInput("avatarFile", "avatarFileName", "avatarPreviewImg", "avatarPreviewWrap", null, "avatarUrl", checkDirty);
        bindFileInput("bannerFile", "bannerFileName", "bannerPreviewImg", "bannerPreviewWrap", "bannerPlaceholder", "bannerUrl", checkDirty);
    }

    function snapshotForm(form) {
        var parts = [];
        form.querySelectorAll("input, textarea, select").forEach(function (el) {
            if (el.type === "file" || el.type === "hidden") return;
            parts.push(el.name + "=" + (el.value || ""));
        });
        var avatarFile = document.getElementById("avatarFile");
        var bannerFile = document.getElementById("bannerFile");
        if (avatarFile && avatarFile.files && avatarFile.files[0]) parts.push("avatarFile=" + avatarFile.files[0].name);
        if (bannerFile && bannerFile.files && bannerFile.files[0]) parts.push("bannerFile=" + bannerFile.files[0].name);
        return parts.join("|");
    }

    initProfileSaveButton();
})();
