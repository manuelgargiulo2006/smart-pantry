// Smart Pantry - piccoli comportamenti dell'interfaccia
document.addEventListener("DOMContentLoaded", () => {

    // ---------- menu su smartphone ----------
    const toggle = document.querySelector(".menu-toggle");
    const sidebar = document.querySelector(".sidebar");
    if (toggle && sidebar) {
        toggle.addEventListener("click", () => {
            const open = sidebar.classList.toggle("open");
            toggle.setAttribute("aria-expanded", String(open));
        });
    }

    // ---------- moduli che chiamano l'AI: mostra "sto pensando..." ----------
    document.querySelectorAll("form.ai-form").forEach(form => {
        form.addEventListener("submit", () => form.classList.add("is-loading"));
    });

    // ---------- conferma prima di azioni irreversibili ----------
    // (gestita più in basso con una finestra di dialogo grafica: vedi SmartPantryUI.confirm)

    // ---------- form della dispensa ----------
    const productSelect = document.getElementById("productId");
    const expiryInput = document.getElementById("expirationDate");
    const purchaseInput = document.getElementById("purchaseDate");
    const storageSelect = document.getElementById("storage");

    const addDays = (isoDate, days) => {
        const d = isoDate ? new Date(isoDate + "T00:00:00") : new Date();
        d.setDate(d.getDate() + days);
        const pad = n => String(n).padStart(2, "0");
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    };

    // propone la scadenza in base alla durata media del prodotto, se non è ancora stata scelta
    if (productSelect && expiryInput) {
        productSelect.addEventListener("change", () => {
            const option = productSelect.selectedOptions[0];
            const days = option ? parseInt(option.dataset.shelfLife, 10) : NaN;
            if (!expiryInput.value && !Number.isNaN(days)) {
                expiryInput.value = addDays(purchaseInput ? purchaseInput.value : null, days);
            }
        });
    }

    // ricerca del prodotto tramite codice a barre
    const barcodeInput = document.getElementById("barcode");
    const barcodeMsg = document.getElementById("barcode-msg");
    if (barcodeInput && productSelect) {
        const lookup = () => {
            const code = barcodeInput.value.trim();
            if (!code) {
                barcodeMsg.textContent = "";
                return;
            }
            const option = [...productSelect.options].find(o => o.dataset.barcode === code);
            if (option) {
                productSelect.value = option.value;
                productSelect.dispatchEvent(new Event("change"));
                barcodeMsg.textContent = "Trovato: " + option.textContent.trim();
                barcodeMsg.className = "hint";
            } else {
                barcodeMsg.textContent = "Nessun prodotto del catalogo ha questo codice a barre.";
                barcodeMsg.className = "field-error";
            }
        };
        barcodeInput.addEventListener("change", lookup);
        barcodeInput.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                lookup();
            }
        });
    }

    // stima della scadenza con l'AI
    const estimateBtn = document.getElementById("estimate-btn");
    const estimateBox = document.getElementById("estimate-box");
    if (estimateBtn && productSelect && expiryInput) {
        estimateBtn.addEventListener("click", async () => {
            if (!productSelect.value) {
                showEstimate("Scegli prima il prodotto.", true);
                return;
            }
            const label = estimateBtn.innerHTML;
            estimateBtn.disabled = true;
            estimateBtn.textContent = "Stima in corso...";
            const params = new URLSearchParams({
                productId: productSelect.value,
                storage: storageSelect ? storageSelect.value : "DISPENSA",
                purchaseDate: purchaseInput ? purchaseInput.value : ""
            });
            try {
                const response = await fetch(estimateBtn.dataset.url + "?" + params, {
                    headers: {"Accept": "application/json"}
                });
                const data = await response.json();
                if (!response.ok) {
                    showEstimate(data.error || "Stima non disponibile.", true);
                } else {
                    expiryInput.value = data.date;
                    showEstimate(`Circa ${data.days} giorni: scade ${data.dateLabel}. ${data.advice}`, false);
                }
            } catch (e) {
                showEstimate("Impossibile contattare il server. Riprova.", true);
            } finally {
                estimateBtn.disabled = false;
                estimateBtn.innerHTML = label;
            }
        });
    }

    function showEstimate(text, isError) {
        estimateBox.textContent = text;
        estimateBox.classList.add("visible");
        estimateBox.classList.toggle("error", isError);
    }

    // ---------- revisione dello scontrino: righe escluse più chiare ----------
    document.querySelectorAll(".receipt-row input[type=checkbox]").forEach(box => {
        const row = box.closest(".receipt-row");
        const update = () => row.classList.toggle("excluded", !box.checked);
        box.addEventListener("change", update);
        update();
    });
});

/* =====================================================================
   MIGLIORAMENTI INTERFACCIA
   Tema chiaro/scuro, notifiche a comparsa, finestra di conferma,
   ricerca rapida (Ctrl+K), filtro istantaneo, tabelle ordinabili,
   campi numerici con +/-, password visibile, contatore caratteri,
   copia e stampa della lista, notifiche del browser per le scadenze,
   scansione del codice a barre con la fotocamera.
   ===================================================================== */
const SmartPantryUI = (() => {

    const ICONS = "/img/icons.svg";
    const icon = name => `<svg class="icon" aria-hidden="true"><use href="${ICONS}#${name}"/></svg>`;

    const storage = {
        get(key) {
            try { return localStorage.getItem(key); } catch (e) { return null; }
        },
        set(key, value) {
            try { localStorage.setItem(key, value); } catch (e) { /* modalità privata: si ignora */ }
        }
    };

    /** Rimuove accenti e maiuscole per confronti "morbidi" (es. "pàtate" = "patate"). */
    const normalize = text => (text || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .trim();

    const isTyping = el => el && (el.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName));

    // ------------------------------------------------------------------ tema

    function currentTheme() {
        const explicit = document.documentElement.dataset.theme || storage.get("sp-theme");
        if (explicit === "dark" || explicit === "light") return explicit;
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function applyTheme(theme) {
        document.documentElement.dataset.theme = theme;
        storage.set("sp-theme", theme);
        document.querySelectorAll("[data-theme-toggle]").forEach(btn => {
            const label = theme === "dark" ? "Passa al tema chiaro" : "Passa al tema scuro";
            btn.setAttribute("aria-label", label);
            btn.title = label;
            btn.innerHTML = icon(theme === "dark" ? "i-sun" : "i-moon");
        });
    }

    function toggleTheme() {
        applyTheme(currentTheme() === "dark" ? "light" : "dark");
    }

    function themeButton() {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "tool-btn icon-only";
        btn.dataset.themeToggle = "";
        btn.addEventListener("click", toggleTheme);
        return btn;
    }

    // ------------------------------------------------------------------ notifiche a comparsa

    let toastStack;

    function stack() {
        if (!toastStack) {
            toastStack = document.createElement("div");
            toastStack.className = "toast-stack";
            toastStack.setAttribute("aria-live", "polite");
            document.body.appendChild(toastStack);
        }
        return toastStack;
    }

    function dismiss(el) {
        el.classList.add("leaving");
        el.addEventListener("animationend", () => el.remove(), { once: true });
        setTimeout(() => el.remove(), 400);
    }

    function makeDismissible(el, autoHide) {
        if (!el.querySelector(".flash-close")) {
            const close = document.createElement("button");
            close.type = "button";
            close.className = "flash-close";
            close.setAttribute("aria-label", "Chiudi");
            close.innerHTML = "&times;";
            close.addEventListener("click", () => dismiss(el));
            el.appendChild(close);
        }
        if (autoHide) {
            let timer = setTimeout(() => dismiss(el), 5000);
            el.addEventListener("mouseenter", () => clearTimeout(timer));
            el.addEventListener("mouseleave", () => { timer = setTimeout(() => dismiss(el), 2500); });
        }
    }

    function toast(message, type = "success") {
        const el = document.createElement("div");
        el.className = `flash flash-${type}`;
        el.setAttribute("role", type === "error" ? "alert" : "status");
        el.innerHTML = icon(type === "error" ? "i-alert" : "i-check") + "<span></span>";
        el.querySelector("span").textContent = message;
        stack().appendChild(el);
        makeDismissible(el, type !== "error");
    }

    /** I messaggi di conferma della pagina diventano notifiche in basso a destra. */
    function initFlashes() {
        document.querySelectorAll(".main .container > .flash-success, .main .container > .flash-error")
            .forEach(el => {
                stack().appendChild(el);
                makeDismissible(el, el.classList.contains("flash-success"));
            });
    }

    // ------------------------------------------------------------------ finestra di conferma

    let confirmDialog;

    function confirm(message, okLabel = "Conferma") {
        if (typeof HTMLDialogElement === "undefined") {
            return Promise.resolve(window.confirm(message));
        }
        if (!confirmDialog) {
            confirmDialog = document.createElement("dialog");
            confirmDialog.className = "confirm";
            confirmDialog.innerHTML = `
                <form method="dialog">
                    <div class="confirm-body">
                        <span class="confirm-icon">${icon("i-alert")}</span>
                        <div>
                            <h2>Sei sicuro?</h2>
                            <p class="confirm-message"></p>
                        </div>
                    </div>
                    <div class="confirm-actions">
                        <button class="btn btn-ghost" value="cancel">Annulla</button>
                        <button class="btn btn-danger-solid confirm-ok" value="ok"></button>
                    </div>
                </form>`;
            document.body.appendChild(confirmDialog);
        }
        confirmDialog.querySelector(".confirm-message").textContent = message;
        confirmDialog.querySelector(".confirm-ok").textContent = okLabel;
        confirmDialog.returnValue = "";
        confirmDialog.showModal();
        confirmDialog.querySelector(".btn-ghost").focus();
        return new Promise(resolve => {
            confirmDialog.addEventListener("close",
                () => resolve(confirmDialog.returnValue === "ok"), { once: true });
        });
    }

    function initConfirmForms() {
        document.querySelectorAll("form[data-confirm]").forEach(form => {
            form.addEventListener("submit", async event => {
                if (form.dataset.confirmed === "true") {
                    return;
                }
                event.preventDefault();
                if (await confirm(form.dataset.confirm, "Elimina")) {
                    form.dataset.confirmed = "true";
                    form.submit();
                }
            });
        });
    }

    // ------------------------------------------------------------------ ricerca rapida (Ctrl+K)

    let palette;

    function paletteItems() {
        const items = [];
        let group = "Menu";
        document.querySelectorAll(".sidebar .nav > *").forEach(el => {
            if (el.classList.contains("nav-group")) {
                group = el.textContent.trim();
            } else if (el.tagName === "A") {
                items.push({ label: el.textContent.replace(/\d+\s*$/, "").trim(), href: el.getAttribute("href"), group });
            }
        });
        if (items.length) {
            items.push({ label: "Aggiungi un prodotto alla dispensa", href: "/pantry/new", group: "Azioni" });
            items.push({ label: "Scarica la lista della spesa", href: "/shopping/export", group: "Azioni" });
            items.push({ label: "Cambia tema chiaro/scuro", action: toggleTheme, group: "Azioni" });
        }
        return items;
    }

    function openPalette() {
        const items = paletteItems();
        if (!items.length || typeof HTMLDialogElement === "undefined") {
            return;
        }
        if (!palette) {
            palette = document.createElement("dialog");
            palette.className = "palette";
            palette.innerHTML = `
                <input class="palette-input" type="text" placeholder="Vai a una pagina o cerca un'azione"
                       aria-label="Cerca" autocomplete="off">
                <ul class="palette-list" role="listbox"></ul>
                <div class="palette-hint">
                    <span><span class="kbd">&uarr;</span> <span class="kbd">&darr;</span> per scegliere</span>
                    <span><span class="kbd">Invio</span> per aprire</span>
                    <span><span class="kbd">Esc</span> per chiudere</span>
                </div>`;
            document.body.appendChild(palette);
            palette.addEventListener("click", e => { if (e.target === palette) palette.close(); });
        }
        const input = palette.querySelector(".palette-input");
        const list = palette.querySelector(".palette-list");
        let selected = 0;
        let visible = items;

        const render = () => {
            const q = normalize(input.value);
            visible = items.filter(i => normalize(i.label + " " + i.group).includes(q));
            selected = Math.min(selected, Math.max(visible.length - 1, 0));
            list.innerHTML = visible.length ? "" : `<li class="palette-empty">Nessun risultato</li>`;
            visible.forEach((item, idx) => {
                const li = document.createElement("li");
                const a = document.createElement("a");
                a.href = item.href || "#";
                a.innerHTML = `<span></span><span class="group"></span>`;
                a.firstChild.textContent = item.label;
                a.lastChild.textContent = item.group;
                a.classList.toggle("selected", idx === selected);
                a.addEventListener("click", e => run(item, e));
                li.appendChild(a);
                list.appendChild(li);
            });
        };

        const run = (item, event) => {
            if (item.action) {
                if (event) event.preventDefault();
                item.action();
                palette.close();
            } else if (!event) {
                window.location.href = item.href;
            }
        };

        input.value = "";
        input.oninput = () => { selected = 0; render(); };
        input.onkeydown = e => {
            if (e.key === "ArrowDown") { e.preventDefault(); selected = (selected + 1) % Math.max(visible.length, 1); render(); }
            if (e.key === "ArrowUp") { e.preventDefault(); selected = (selected - 1 + visible.length) % Math.max(visible.length, 1); render(); }
            if (e.key === "Enter" && visible[selected]) { e.preventDefault(); run(visible[selected]); }
        };
        render();
        palette.showModal();
        input.focus();
    }

    function initShortcuts() {
        document.addEventListener("keydown", e => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
                e.preventDefault();
                openPalette();
                return;
            }
            if (e.key === "/" && !isTyping(document.activeElement)) {
                const search = document.querySelector(".filters input[type=search]");
                if (search) {
                    e.preventDefault();
                    search.focus();
                    search.select();
                }
            }
        });
    }

    // ------------------------------------------------------------------ filtro istantaneo

    function initLiveFilter() {
        document.querySelectorAll("form.filters").forEach(form => {
            const input = form.querySelector("input[type=search]");
            if (!input) return;
            // cerca la tabella o l'elenco di ricette subito dopo il modulo dei filtri
            let target = form.nextElementSibling;
            while (target && !target.matches(".table-wrap, .recipe-list")) {
                target = target.nextElementSibling;
            }
            if (!target) return;
            const rows = target.matches(".recipe-list")
                ? [...target.querySelectorAll(":scope > .recipe")]
                : [...target.querySelectorAll("tbody tr")];
            if (!rows.length) return;

            const count = document.createElement("span");
            count.className = "filter-count";
            count.setAttribute("aria-live", "polite");
            form.appendChild(count);

            const hint = document.createElement("span");
            hint.className = "filter-count";
            hint.innerHTML = `premi <span class="kbd">/</span> per cercare`;
            form.appendChild(hint);

            const update = () => {
                const q = normalize(input.value);
                let shown = 0;
                rows.forEach(row => {
                    const match = !q || normalize(row.textContent).includes(q);
                    row.classList.toggle("filtered-out", !match);
                    if (match) shown++;
                });
                count.textContent = q ? `${shown} di ${rows.length}` : "";
                hint.hidden = Boolean(q);
            };
            input.addEventListener("input", update);
            update();
        });
    }

    // ------------------------------------------------------------------ tabelle ordinabili

    const MONTHS = { gen: 0, feb: 1, mar: 2, apr: 3, mag: 4, giu: 5, lug: 6, ago: 7, set: 8, ott: 9, nov: 10, dic: 11 };

    function sortKey(cell) {
        if (!cell) return "";
        if (cell.dataset.sort) return cell.dataset.sort;
        const dateEl = cell.querySelector(".expiry .date");
        const text = (dateEl ? dateEl.textContent : cell.textContent).trim();
        const date = text.match(/^(\d{1,2}) (\w{3})\w*\.? (\d{4})$/i);
        if (date && MONTHS[date[2].toLowerCase()] !== undefined) {
            return new Date(+date[3], MONTHS[date[2].toLowerCase()], +date[1]).getTime();
        }
        const number = text.replace(/\s*(gg|x)$/i, "").replace(",", ".");
        if (number !== "" && !isNaN(number)) return parseFloat(number);
        return normalize(text);
    }

    function initSortableTables() {
        document.querySelectorAll(".table-wrap table").forEach(table => {
            const tbody = table.tBodies[0];
            if (!tbody || tbody.rows.length < 2 || table.querySelector(".receipt-row")) return;
            table.querySelectorAll("thead th").forEach((th, col) => {
                const label = th.textContent.trim();
                if (!label || th.querySelector(".sr-only")) return;
                th.classList.add("sortable");
                th.tabIndex = 0;
                th.setAttribute("aria-sort", "none");
                const sort = () => {
                    const asc = th.getAttribute("aria-sort") !== "ascending";
                    table.querySelectorAll("th.sortable").forEach(o => o.setAttribute("aria-sort", "none"));
                    th.setAttribute("aria-sort", asc ? "ascending" : "descending");
                    const rows = [...tbody.rows];
                    rows.sort((a, b) => {
                        const ka = sortKey(a.cells[col]);
                        const kb = sortKey(b.cells[col]);
                        const cmp = typeof ka === "number" && typeof kb === "number"
                            ? ka - kb
                            : String(ka).localeCompare(String(kb), "it");
                        return asc ? cmp : -cmp;
                    });
                    rows.forEach(r => tbody.appendChild(r));
                };
                th.addEventListener("click", sort);
                th.addEventListener("keydown", e => {
                    if (e.key === "Enter" || e.key === " ") { e.preventDefault(); sort(); }
                });
            });
        });
    }

    // ------------------------------------------------------------------ campi dei moduli

    function initSteppers() {
        document.querySelectorAll(".form-grid .field input[type=number].input").forEach(input => {
            if (input.closest(".stepper")) return;
            const wrap = document.createElement("div");
            wrap.className = "stepper";
            if (input.style.maxWidth) {
                wrap.style.maxWidth = input.style.maxWidth;
                input.style.maxWidth = "";
            }
            input.parentNode.insertBefore(wrap, input);
            const minus = document.createElement("button");
            const plus = document.createElement("button");
            minus.type = plus.type = "button";
            minus.textContent = "−";
            plus.textContent = "+";
            minus.setAttribute("aria-label", "Diminuisci");
            plus.setAttribute("aria-label", "Aumenta");
            wrap.append(minus, input, plus);
            const step = delta => {
                const min = input.min !== "" ? Number(input.min) : -Infinity;
                const max = input.max !== "" ? Number(input.max) : Infinity;
                const current = input.value === "" ? (delta > 0 ? min - 1 : min) : Number(input.value);
                const next = Math.min(max, Math.max(min, (Number.isFinite(current) ? current : 0) + delta));
                input.value = Number.isFinite(next) ? next : 0;
                input.dispatchEvent(new Event("change", { bubbles: true }));
            };
            minus.addEventListener("click", () => step(-1));
            plus.addEventListener("click", () => step(1));
        });
    }

    function initPasswordToggles() {
        document.querySelectorAll("input[type=password].input").forEach(input => {
            const wrap = document.createElement("div");
            wrap.className = "password-wrap";
            input.parentNode.insertBefore(wrap, input);
            wrap.appendChild(input);
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "password-toggle";
            btn.textContent = "Mostra";
            btn.setAttribute("aria-label", "Mostra la password");
            btn.addEventListener("click", () => {
                const show = input.type === "password";
                input.type = show ? "text" : "password";
                btn.textContent = show ? "Nascondi" : "Mostra";
                btn.setAttribute("aria-label", show ? "Nascondi la password" : "Mostra la password");
            });
            wrap.appendChild(btn);
        });
    }

    function initCharCounters() {
        document.querySelectorAll("textarea[maxlength]").forEach(area => {
            const max = Number(area.getAttribute("maxlength"));
            const counter = document.createElement("span");
            counter.className = "char-count";
            area.insertAdjacentElement("afterend", counter);
            const update = () => {
                counter.textContent = `${area.value.length} / ${max}`;
                counter.classList.toggle("near-limit", area.value.length > max * 0.9);
            };
            area.addEventListener("input", update);
            update();
        });
    }

    /** Durante le chiamate all'AI mostra da quanti secondi si sta aspettando. */
    function initAiTimers() {
        document.querySelectorAll("form.ai-form").forEach(form => {
            form.addEventListener("submit", () => {
                const note = form.querySelector(".loading-note");
                if (!note) return;
                const base = note.textContent.trim();
                const start = Date.now();
                setInterval(() => {
                    const s = Math.round((Date.now() - start) / 1000);
                    note.lastChild.textContent = ` ${base} (${s} s)`;
                }, 1000);
            });
        });
    }

    // ------------------------------------------------------------------ lista della spesa

    function initShoppingTools() {
        if (!location.pathname.startsWith("/shopping")) return;
        const actions = document.querySelector(".page-actions");
        const items = [...document.querySelectorAll(".shop-list .shop-item:not(.bought) .what .product-name")];
        if (!actions) return;

        const copy = document.createElement("button");
        copy.type = "button";
        copy.className = "btn btn-ghost";
        copy.innerHTML = `${icon("i-check")} Copia lista`;
        copy.disabled = !items.length;
        copy.addEventListener("click", async () => {
            const lines = [];
            document.querySelectorAll(".shop-list > .category-label, .shop-list .shop-item:not(.bought)").forEach(el => {
                if (el.classList.contains("category-label")) {
                    lines.push((lines.length ? "\n" : "") + el.textContent.trim().toUpperCase());
                } else {
                    const name = el.querySelector(".product-name");
                    if (name) lines.push("- " + name.textContent.replace(/\s+/g, " ").trim());
                }
            });
            try {
                await navigator.clipboard.writeText("Lista della spesa\n\n" + lines.join("\n"));
                toast("Lista copiata: puoi incollarla in un messaggio");
            } catch (e) {
                toast("Impossibile copiare negli appunti da questo browser", "error");
            }
        });

        const print = document.createElement("button");
        print.type = "button";
        print.className = "btn btn-ghost";
        print.innerHTML = `${icon("i-receipt")} Stampa`;
        print.addEventListener("click", () => window.print());

        actions.prepend(copy, print);
    }

    // ------------------------------------------------------------------ revisione scontrino

    function initReceiptTools() {
        const boxes = [...document.querySelectorAll(".receipt-row input[type=checkbox]")];
        if (!boxes.length) return;
        const bar = document.createElement("div");
        bar.className = "inline-tools";
        bar.innerHTML = `
            <button type="button" class="btn btn-ghost btn-small" data-all="true">Seleziona tutti</button>
            <button type="button" class="btn btn-ghost btn-small" data-all="false">Deseleziona tutti</button>
            <span class="filter-count"></span>`;
        const count = bar.querySelector(".filter-count");
        const update = () => {
            const n = boxes.filter(b => b.checked).length;
            count.textContent = `${n} di ${boxes.length} prodotti da aggiungere`;
        };
        bar.querySelectorAll("button").forEach(btn => btn.addEventListener("click", () => {
            boxes.forEach(b => {
                b.checked = btn.dataset.all === "true";
                b.dispatchEvent(new Event("change"));
            });
            update();
        }));
        boxes.forEach(b => b.addEventListener("change", update));
        const table = document.querySelector(".receipt-row").closest(".table-wrap");
        table.parentNode.insertBefore(bar, table);
        update();
    }

    // ------------------------------------------------------------------ notifiche del browser

    function alertCount() {
        const badge = document.querySelector('.sidebar .nav a[href$="/pantry"] .count');
        return badge ? parseInt(badge.textContent, 10) || 0 : 0;
    }

    function notifyExpiring() {
        const count = alertCount();
        const today = new Date().toISOString().slice(0, 10);
        if (!count || storage.get("sp-notified") === today || Notification.permission !== "granted") return;
        storage.set("sp-notified", today);
        const n = new Notification("Smart Pantry", {
            body: count === 1
                ? "Hai 1 prodotto scaduto o in scadenza: controlla la dispensa."
                : `Hai ${count} prodotti scaduti o in scadenza: controlla la dispensa.`,
            icon: "/favicon.svg",
            tag: "smart-pantry-scadenze"
        });
        n.onclick = () => { window.focus(); window.location.href = "/dashboard"; };
    }

    function notificationButton() {
        if (!("Notification" in window)) return null;
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "tool-btn icon-only";
        const refresh = () => {
            const on = Notification.permission === "granted";
            btn.setAttribute("aria-pressed", String(on));
            btn.innerHTML = icon("i-bell");
            btn.title = on
                ? "Avvisi attivi: una notifica al giorno se ci sono prodotti in scadenza"
                : "Attiva gli avvisi del browser per i prodotti in scadenza";
            btn.setAttribute("aria-label", btn.title);
            btn.disabled = Notification.permission === "denied";
        };
        btn.addEventListener("click", async () => {
            if (Notification.permission === "default") {
                await Notification.requestPermission();
                refresh();
                if (Notification.permission === "granted") {
                    toast("Avvisi attivati: ti avviseremo quando qualcosa sta per scadere");
                    storage.set("sp-notified", "");
                    notifyExpiring();
                }
            } else if (Notification.permission === "granted") {
                toast("Gli avvisi sono già attivi. Puoi disattivarli dalle impostazioni del browser.");
            }
        });
        refresh();
        return btn;
    }

    // ------------------------------------------------------------------ scansione codice a barre

    function initBarcodeScanner() {
        const input = document.getElementById("barcode");
        if (!input || !("BarcodeDetector" in window) || !navigator.mediaDevices) return;
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "btn btn-ghost btn-small";
        btn.style.alignSelf = "flex-start";
        btn.innerHTML = `${icon("i-search")} Scansiona con la fotocamera`;
        input.insertAdjacentElement("afterend", btn);

        let stream = null;
        let box = null;
        const stop = () => {
            if (stream) stream.getTracks().forEach(t => t.stop());
            stream = null;
            if (box) box.remove();
            box = null;
            btn.innerHTML = `${icon("i-search")} Scansiona con la fotocamera`;
        };

        btn.addEventListener("click", async () => {
            if (stream) { stop(); return; }
            try {
                const detector = new BarcodeDetector({ formats: ["ean_13", "ean_8", "upc_a", "upc_e"] });
                stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
                box = document.createElement("div");
                box.className = "scanner";
                const video = document.createElement("video");
                video.playsInline = true;
                video.muted = true;
                video.srcObject = stream;
                box.appendChild(video);
                btn.insertAdjacentElement("afterend", box);
                btn.textContent = "Chiudi fotocamera";
                await video.play();
                const scan = async () => {
                    if (!stream) return;
                    try {
                        const codes = await detector.detect(video);
                        if (codes.length) {
                            input.value = codes[0].rawValue;
                            input.dispatchEvent(new Event("change"));
                            stop();
                            return;
                        }
                    } catch (e) { /* frame non ancora pronto */ }
                    setTimeout(scan, 300);
                };
                scan();
            } catch (e) {
                stop();
                toast("Impossibile usare la fotocamera: controlla i permessi del browser", "error");
            }
        });
    }

    // ------------------------------------------------------------------ avvio

    function initToolbar() {
        const footer = document.querySelector(".sidebar-footer");
        const tools = document.createElement("div");
        tools.className = "sidebar-tools";
        tools.appendChild(themeButton());
        if (footer) {
            const notif = notificationButton();
            if (notif) tools.appendChild(notif);
            const search = document.createElement("button");
            search.type = "button";
            search.className = "search-trigger";
            search.innerHTML = `${icon("i-search")} <span>Cerca</span> <span class="kbd">Ctrl K</span>`;
            search.addEventListener("click", openPalette);
            tools.prepend(search);
            footer.insertBefore(tools, footer.firstChild);
        } else {
            // pagine pubbliche (benvenuto, login, registrazione)
            tools.classList.add("floating-tools");
            document.body.appendChild(tools);
        }
        applyTheme(currentTheme());
    }

    function init() {
        initToolbar();
        initFlashes();
        initConfirmForms();
        initShortcuts();
        initLiveFilter();
        initSortableTables();
        initSteppers();
        initPasswordToggles();
        initCharCounters();
        initAiTimers();
        initShoppingTools();
        initReceiptTools();
        initBarcodeScanner();
        if ("Notification" in window) notifyExpiring();
    }

    return { init, toast, confirm, applyTheme };
})();

document.addEventListener("DOMContentLoaded", SmartPantryUI.init);
