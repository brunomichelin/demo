(() => {
    "use strict";

    const elements = {
        grid: document.getElementById("wineGrid"),
        empty: document.getElementById("emptyState"),
        notice: document.getElementById("storageNotice"),
        dialog: document.getElementById("wineDialog"),
        form: document.getElementById("wineForm"),
        formError: document.getElementById("formError"),
        search: document.getElementById("wineSearch"),
        country: document.getElementById("filterCountry"),
        grape: document.getElementById("filterGrape"),
        type: document.getElementById("filterType"),
        vintage: document.getElementById("filterVintage"),
        rating: document.getElementById("filterRating"),
        price: document.getElementById("filterPrice")
    };
    let wines = [];
    let activeView = "all";
    let photoData = "";
    let photoReadPromise = Promise.resolve();

    const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"
    })[character]);
    const normalized = (value) => String(value ?? "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("pt-BR");
    const money = (value) => Number(value || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });
    const isTasted = (wine) => Boolean(wine.tastedAt);
    const ratingStars = (rating) => {
        const score = Math.max(0, Math.min(5, Number(rating) || 0));
        return `${"★".repeat(score)}${"☆".repeat(5 - score)}`;
    };
    const showNotice = (message) => {
        elements.notice.textContent = message;
        elements.notice.hidden = false;
    };

    async function apiRequest(path = "", options = {}) {
        const contextPath = window.location.pathname.replace(/\/[^/]*$/, "");
        const response = await fetch(`${contextPath}/api/v1/vinhos${path}`, {
            credentials: "same-origin",
            ...options,
            headers: { "Content-Type": "application/json", ...options.headers }
        });
        if (!response.ok) {
            let message = `Não foi possível concluir a operação (${response.status}).`;
            try {
                const body = await response.json();
                if (body.detail || body.message) message = body.detail || body.message;
            } catch (error) {
                // Some API errors have an empty response body.
            }
            throw new Error(message);
        }
        return response.status === 204 ? null : response.json();
    }

    async function loadWines() {
        try {
            wines = await apiRequest();
        } catch (error) {
            wines = [];
            showNotice(`Não foi possível carregar sua adega do banco de dados. ${error.message}`);
            console.error("Falha ao carregar vinhos da API.", error);
        }
        render();
    }

    function populateFilter(select, values, firstLabel) {
        const current = select.value;
        const unique = [...new Set(values.filter(Boolean).flatMap((value) => String(value).split(",").map((part) => part.trim()).filter(Boolean)))];
        select.replaceChildren(new Option(firstLabel, ""));
        unique.sort((a, b) => a.localeCompare(b, "pt-BR")).forEach((value) => select.add(new Option(value, value)));
        if (unique.includes(current)) select.value = current;
    }

    function refreshFilters() {
        populateFilter(elements.country, wines.map((wine) => wine.country), "Todos");
        populateFilter(elements.grape, wines.map((wine) => wine.grapes), "Todas");
        populateFilter(elements.vintage, wines.map((wine) => wine.vintage).filter(Boolean).map(String), "Todas");
    }

    function cardMarkup(wine, index) {
        const photo = typeof wine.photo === "string" && wine.photo.startsWith("data:image/") ? wine.photo : "";
        const tasted = isTasted(wine);
        const notes = wine.aroma || wine.palate || wine.appearance || "Uma nova história espera pela primeira degustação.";
        const country = wine.country ? `<span aria-hidden="true">✦</span> ${escapeHtml(wine.country)}` : "Origem não informada";
        const vintage = wine.vintage ? ` · ${escapeHtml(wine.vintage)}` : "";
        const price = Number(wine.price) > 0 ? money(wine.price) : "—";
        const rating = tasted && wine.rating ? `<div class="wine-rating"><span aria-label="${escapeHtml(wine.rating)} de 5 estrelas">${ratingStars(wine.rating)}</span><small>${escapeHtml(wine.rating)},0</small></div>` : `<div class="wine-rating"><small>Ainda não degustado</small></div>`;
        const imageStyle = photo ? ` style="background-image:url('${photo}')"` : "";
        const tone = ["", "tone-rose", "tone-gold", "tone-olive", "tone-blue"][index % 5];

        return `<article class="wine-card">
            <div class="wine-card-main">
                <div class="wine-card-art ${tone}${photo ? " has-photo" : ""}"${imageStyle} role="img" aria-label="${photo ? `Foto de ${escapeHtml(wine.name)}` : `Ilustração de uma garrafa de ${escapeHtml(wine.type || "vinho")}`}">
                    <span class="wine-mini-bottle"><span class="wine-mini-label">CASA<small>DA VINHA</small></span></span>
                    <span class="wine-card-type">${escapeHtml(wine.type || "VINHO").toLocaleUpperCase("pt-BR")}</span>
                </div>
                <div class="wine-card-copy">
                    <div class="wine-card-topline"><span class="wine-tag">${escapeHtml(wine.type || "Vinho")}${vintage}</span><button class="wine-heart${wine.favorite ? " is-favorite" : ""}" type="button" data-action="favorite" data-id="${escapeHtml(wine.id)}" aria-label="${wine.favorite ? "Remover dos favoritos" : "Adicionar aos favoritos"}">${wine.favorite ? "♥" : "♡"}</button></div>
                    <h3>${escapeHtml(wine.name)}</h3>
                    <p class="wine-producer">${escapeHtml(wine.producer || "Produtor não informado")}</p>
                    ${rating}
                    <div class="wine-card-origin">${country}</div>
                    <p class="wine-card-notes">${escapeHtml(notes)}</p>
                    <div class="wine-card-footer"><span>${escapeHtml(wine.grapes || "Castas não informadas")}</span><strong>${price}</strong></div>
                </div>
            </div>
            <details class="wine-card-details">
                <summary>Ficha completa</summary>
                <dl>
                    <div><dt>Origem</dt><dd>${escapeHtml([wine.region, wine.denomination, wine.country].filter(Boolean).join(" · ") || "Não informada")}</dd></div>
                    <div><dt>Corte / varietal</dt><dd>${escapeHtml(wine.blend || "Não informado")}</dd></div>
                    <div><dt>Perfil</dt><dd>${escapeHtml([wine.body, wine.alcohol ? `${wine.alcohol}% vol.` : ""].filter(Boolean).join(" · ") || "Não informado")}</dd></div>
                    <div><dt>Madeira</dt><dd>${escapeHtml(wine.oak || "Não informado")}</dd></div>
                    <div><dt>Degustação</dt><dd>${escapeHtml([wine.tastedAt ? new Date(`${wine.tastedAt}T12:00:00`).toLocaleDateString("pt-BR") : "", wine.occasion, wine.company].filter(Boolean).join(" · ") || "Ainda não provado")}</dd></div>
                    <div><dt>Aparência</dt><dd>${escapeHtml(wine.appearance || "—")}</dd></div>
                    <div><dt>Aroma e sabor</dt><dd>${escapeHtml([wine.aroma, wine.palate].filter(Boolean).join(" · ") || "—")}</dd></div>
                    <div><dt>Harmonização</dt><dd>${escapeHtml(wine.pairing || "—")}</dd></div>
                    <div><dt>Serviço / guarda</dt><dd>${escapeHtml([wine.temperature, wine.decanted ? `Decantado: ${wine.decanted.toLowerCase()}` : "", wine.cellaring].filter(Boolean).join(" · ") || "—")}</dd></div>
                    <div><dt>Compra</dt><dd>${escapeHtml([wine.retailer, wine.buyAgain ? `Compraria de novo: ${wine.buyAgain.toLowerCase()}` : ""].filter(Boolean).join(" · ") || "—")}</dd></div>
                </dl>
            </details>
            <div class="wine-card-actions">
                <button class="wine-card-action${wine.wishlist ? " is-selected" : ""}" type="button" data-action="wishlist" data-id="${escapeHtml(wine.id)}">${wine.wishlist ? "✓ Na lista quero beber" : "＋ Quero beber"}</button>
                <span class="wine-card-action">${tasted ? `Taça em ${escapeHtml(new Date(`${wine.tastedAt}T12:00:00`).toLocaleDateString("pt-BR"))}` : "Na lista de desejos"}</span>
                <button class="wine-card-action" type="button" data-action="edit" data-id="${escapeHtml(wine.id)}">Editar</button>
                <button class="wine-card-action wine-card-action-delete" type="button" data-action="delete" data-id="${escapeHtml(wine.id)}" aria-label="Remover ${escapeHtml(wine.name)}">×</button>
            </div>
        </article>`;
    }

    function currentFilters() {
        const query = normalized(elements.search.value.trim());
        const minRating = Number(elements.rating.value || 0);
        const priceRange = elements.price.value;
        return wines.filter((wine) => {
            const haystack = normalized([
                wine.name, wine.producer, wine.country, wine.region, wine.denomination,
                wine.grapes, wine.blend, wine.type, wine.appearance, wine.aroma, wine.palate,
                wine.pairing, wine.occasion, wine.company, wine.retailer
            ].join(" "));
            if (query && !haystack.includes(query)) return false;
            if (elements.country.value && wine.country !== elements.country.value) return false;
            if (elements.grape.value && !String(wine.grapes || "").split(",").map((item) => item.trim()).includes(elements.grape.value)) return false;
            if (elements.type.value && wine.type !== elements.type.value) return false;
            if (elements.vintage.value && String(wine.vintage) !== elements.vintage.value) return false;
            if (minRating && (!wine.rating || Number(wine.rating) < minRating)) return false;
            if (priceRange) {
                const price = Number(wine.price || 0);
                const inRange = priceRange === "0-100" ? price > 0 && price <= 100
                    : priceRange === "100-250" ? price > 100 && price <= 250
                        : priceRange === "250-500" ? price > 250 && price <= 500
                            : price > 500;
                if (!inRange) return false;
            }
            if (activeView === "favorites" && !wine.favorite) return false;
            if (activeView === "wishlist" && !wine.wishlist) return false;
            return true;
        });
    }

    function updateStats() {
        const tasted = wines.filter(isTasted);
        const rated = tasted.filter((wine) => Number(wine.rating) > 0);
        const total = rated.reduce((sum, wine) => sum + Number(wine.rating), 0);
        const spend = tasted.reduce((sum, wine) => sum + Number(wine.price || 0), 0);
        const wishlistCount = wines.filter((wine) => wine.wishlist).length;
        document.getElementById("statTasted").textContent = String(tasted.length);
        document.getElementById("statRating").innerHTML = `${rated.length ? (total / rated.length).toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 }) : "—"}<small> / 5</small>`;
        document.getElementById("statWishlist").textContent = String(wishlistCount);
        document.getElementById("statSpent").textContent = money(spend);
        document.getElementById("wishlistTabCount").textContent = String(wishlistCount);
        document.getElementById("wineCount").textContent = `${wines.length} ${wines.length === 1 ? "vinho" : "vinhos"}`;
    }

    function updateInsights() {
        const tasted = wines.filter((wine) => isTasted(wine) && Number(wine.rating) > 0);
        const byGrape = new Map();
        tasted.forEach((wine) => {
            String(wine.grapes || "").split(",").map((grape) => grape.trim()).filter(Boolean).forEach((grape) => {
                const current = byGrape.get(grape) || { sum: 0, count: 0 };
                current.sum += Number(wine.rating);
                current.count += 1;
                byGrape.set(grape, current);
            });
        });
        const preferences = [...byGrape.entries()].filter(([, value]) => value.count >= 2).sort((a, b) => (b[1].sum / b[1].count) - (a[1].sum / a[1].count));
        const insight = document.getElementById("preferenceInsight");
        if (preferences.length) {
            const [grape, scores] = preferences[0];
            const average = (scores.sum / scores.count).toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
            insight.textContent = `Você costuma dar notas mais altas para ${grape}: média ${average} em ${scores.count} degustações.`;
        } else if (tasted.length >= 2) {
            const best = [...tasted].sort((a, b) => Number(b.rating) - Number(a.rating))[0];
            insight.textContent = `Por enquanto, ${best.grapes || best.type || "este estilo"} conquistou sua maior nota. Deguste mais para descobrir novos padrões.`;
        } else {
            insight.textContent = "Suas próximas descobertas começam com a primeira taça. Registre suas avaliações para revelar seus estilos favoritos.";
        }

        const counts = new Map();
        wines.filter(isTasted).forEach((wine) => {
            if (wine.country) counts.set(wine.country, (counts.get(wine.country) || 0) + 1);
        });
        const countries = [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5);
        const flags = { Argentina: "🇦🇷", Brasil: "🇧🇷", Chile: "🇨🇱", Itália: "🇮🇹", Portugal: "🇵🇹", França: "🇫🇷", Espanha: "🇪🇸", Austrália: "🇦🇺", "Estados Unidos": "🇺🇸", Uruguai: "🇺🇾" };
        const coordinates = {
            Argentina: [101, 140], Brasil: [115, 115], Chile: [91, 137], Itália: [197, 68],
            Portugal: [181, 78], França: [190, 65], Espanha: [187, 76], Austrália: [301, 130],
            "Estados Unidos": [77, 58], Uruguai: [105, 136]
        };
        document.getElementById("wineMapMarkers").innerHTML = countries
            .filter(([country]) => coordinates[country])
            .map(([country]) => {
                const [cx, cy] = coordinates[country];
                return `<circle class="wine-map-marker" cx="${cx}" cy="${cy}" r="4"><title>${escapeHtml(country)}</title></circle>`;
            }).join("");
        document.getElementById("countryList").innerHTML = countries.map(([country, count]) => `<div class="wine-country-row"><span class="wine-country-flag" aria-hidden="true">${flags[country] || "✦"}</span><span>${escapeHtml(country)}</span><strong>${count} ${count === 1 ? "rótulo" : "rótulos"}</strong></div>`).join("");
        document.getElementById("countryEmpty").hidden = countries.length > 0;
        document.getElementById("countryCount").textContent = String(counts.size);
    }

    function render() {
        refreshFilters();
        updateStats();
        updateInsights();
        const shown = currentFilters();
        elements.grid.innerHTML = shown.map(cardMarkup).join("");
        elements.empty.hidden = shown.length > 0;
        elements.grid.hidden = shown.length === 0;
    }

    function openForm(wine = null) {
        elements.form.reset();
        elements.formError.textContent = "";
        elements.form.elements.wineId.value = wine ? wine.id : "";
        document.getElementById("dialogTitle").textContent = wine ? "Editar vinho" : "Adicionar vinho";
        elements.form.querySelector('button[type="submit"]').innerHTML = wine ? 'Salvar alterações <span aria-hidden="true">→</span>' : 'Guardar na adega <span aria-hidden="true">→</span>';
        if (wine) {
            const formFields = {
                name: wine.name, producer: wine.producer, vintage: wine.vintage, country: wine.country,
                region: wine.region, denomination: wine.denomination, type: wine.type, grapes: wine.grapes,
                blend: wine.blend, alcohol: wine.alcohol, body: wine.body, oak: wine.oak,
                tastedAt: wine.tastedAt, occasion: wine.occasion, company: wine.company, rating: wine.rating,
                price: wine.price, retailer: wine.retailer, temperature: wine.temperature, decanted: wine.decanted,
                cellaring: wine.cellaring, buyAgain: wine.buyAgain, pairing: wine.pairing,
                appearance: wine.appearance, aroma: wine.aroma, palate: wine.palate
            };
            Object.entries(formFields).forEach(([name, value]) => {
                if (value !== null && value !== undefined) elements.form.elements[name].value = value;
            });
        }
        photoData = wine && typeof wine.photo === "string" ? wine.photo : "";
        photoReadPromise = Promise.resolve();
        elements.dialog.showModal();
        elements.form.elements.name.focus();
    }

    document.getElementById("openWineForm").addEventListener("click", openForm);
    document.getElementById("emptyAddWine").addEventListener("click", openForm);
    document.getElementById("closeWineForm").addEventListener("click", () => elements.dialog.close());
    document.getElementById("cancelWineForm").addEventListener("click", () => elements.dialog.close());

    elements.dialog.addEventListener("click", (event) => {
        if (event.target === elements.dialog) elements.dialog.close();
    });

    elements.form.elements.photo.addEventListener("change", (event) => {
        const file = event.target.files[0];
        photoData = "";
        if (!file) return;
        if (!["image/png", "image/jpeg", "image/webp", "image/gif"].includes(file.type)) {
            elements.formError.textContent = "Escolha uma imagem PNG, JPEG, WebP ou GIF.";
            event.target.value = "";
            return;
        }
        if (file.size > 2 * 1024 * 1024) {
            elements.formError.textContent = "A imagem deve ter no máximo 2 MB.";
            event.target.value = "";
            return;
        }
        const reader = new FileReader();
        photoReadPromise = new Promise((resolve) => {
            reader.addEventListener("load", () => {
            photoData = typeof reader.result === "string" && reader.result.startsWith("data:image/") ? reader.result : "";
                resolve();
            });
            reader.addEventListener("error", () => {
                elements.formError.textContent = "Não foi possível ler a imagem selecionada.";
                resolve();
            });
        });
        reader.readAsDataURL(file);
    });

    elements.form.addEventListener("submit", async (event) => {
        event.preventDefault();
        elements.formError.textContent = "";
        if (!elements.form.reportValidity()) return;
        await photoReadPromise;
        const data = new FormData(elements.form);
        const wine = {
            nome: String(data.get("name") || "").trim(),
            produtor: String(data.get("producer") || "").trim() || null,
            safra: numberOrNull(data.get("vintage")),
            pais: String(data.get("country") || "").trim() || null,
            regiao: String(data.get("region") || "").trim() || null,
            denominacao: String(data.get("denomination") || "").trim() || null,
            tipo: String(data.get("type") || "") || null,
            uvas: String(data.get("grapes") || "").trim() || null,
            corte: String(data.get("blend") || "").trim() || null,
            teorAlcoolico: numberOrNull(data.get("alcohol")),
            corpo: String(data.get("body") || "") || null,
            madeira: String(data.get("oak") || "").trim() || null,
            dataDegustacao: String(data.get("tastedAt") || "") || null,
            ocasiao: String(data.get("occasion") || "").trim() || null,
            companhia: String(data.get("company") || "").trim() || null,
            nota: numberOrNull(data.get("rating")),
            aparencia: String(data.get("appearance") || "").trim() || null,
            aroma: String(data.get("aroma") || "").trim() || null,
            saborFinal: String(data.get("palate") || "").trim() || null,
            harmonizacao: String(data.get("pairing") || "").trim() || null,
            comprariaNovamente: String(data.get("buyAgain") || "") || null,
            precoPago: numberOrNull(data.get("price")),
            localCompra: String(data.get("retailer") || "").trim() || null,
            temperaturaServico: String(data.get("temperature") || "").trim() || null,
            decantado: String(data.get("decanted") || "") || null,
            potencialGuarda: String(data.get("cellaring") || "").trim() || null,
            foto: photoData || null,
            favorito: null,
            queroBeber: null
        };
        const wineId = String(data.get("wineId") || "");
        try {
            await apiRequest(wineId ? `/${wineId}` : "", {
                method: wineId ? "PUT" : "POST",
                body: JSON.stringify(wine)
            });
        } catch (error) {
            elements.formError.textContent = error.message;
            console.error("Falha ao salvar vinho no banco de dados.", error);
            return;
        }
        elements.dialog.close();
        activeView = "all";
        document.querySelectorAll(".wine-view").forEach((button) => button.classList.toggle("is-active", button.dataset.view === activeView));
        await loadWines();
    });

    elements.grid.addEventListener("click", async (event) => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;
        const wine = wines.find((item) => String(item.id) === button.dataset.id);
        if (!wine) return;
        if (button.dataset.action === "edit") {
            openForm(wine);
            return;
        }
        if (button.dataset.action === "delete") {
            if (!window.confirm(`Remover “${wine.name}” da sua adega?`)) return;
            try {
                await apiRequest(`/${wine.id}`, { method: "DELETE" });
                await loadWines();
            } catch (error) {
                showNotice(error.message);
                console.error("Falha ao remover vinho da adega.", error);
            }
        } else {
            const property = button.dataset.action === "favorite" ? "favorite" : "wishlist";
            const apiProperty = property === "favorite" ? "favorito" : "queroBeber";
            try {
                await apiRequest(`/${wine.id}/preferencias`, {
                    method: "PATCH",
                    body: JSON.stringify({ [apiProperty]: !wine[property] })
                });
                await loadWines();
            } catch (error) {
                showNotice(error.message);
                console.error("Falha ao atualizar preferências do vinho.", error);
            }
        }
    });

    [elements.search, elements.country, elements.grape, elements.type, elements.vintage, elements.rating, elements.price].forEach((control) => {
        control.addEventListener(control === elements.search ? "input" : "change", () => {
            const shown = currentFilters();
            elements.grid.innerHTML = shown.map(cardMarkup).join("");
            elements.empty.hidden = shown.length > 0;
            elements.grid.hidden = shown.length === 0;
        });
    });

    document.getElementById("clearFilters").addEventListener("click", () => {
        elements.search.value = "";
        [elements.country, elements.grape, elements.type, elements.vintage, elements.rating, elements.price].forEach((control) => { control.value = ""; });
        activeView = "all";
        document.querySelectorAll(".wine-view").forEach((button) => button.classList.toggle("is-active", button.dataset.view === activeView));
        render();
    });

    document.querySelectorAll(".wine-view").forEach((button) => {
        button.addEventListener("click", () => {
            activeView = button.dataset.view;
            document.querySelectorAll(".wine-view").forEach((view) => view.classList.toggle("is-active", view === button));
            const shown = currentFilters();
            elements.grid.innerHTML = shown.map(cardMarkup).join("");
            elements.empty.hidden = shown.length > 0;
            elements.grid.hidden = shown.length === 0;
        });
    });

    function numberOrNull(value) {
        return value === null || String(value).trim() === "" ? null : Number(value);
    }

    loadWines();
})();
