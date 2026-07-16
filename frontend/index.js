document.addEventListener("DOMContentLoaded", async () => {
    const despliegue = document.getElementById("despliegue");
    if (despliegue) setTimeout(() => despliegue.classList.add("abajo"), 0);

    FastMarket.activarBuscador("buscador", "busqueda");
    FastMarket.activarMenuCliente();
    FastMarket.mostrarPanelCliente();
    FastMarket.activarChatBasico();

    await Promise.allSettled([
        cargarContenidoIndex(),
        cargarProductosIndex()
    ]);
});

async function cargarContenidoIndex() {
    try {
        const contenidos = await FastMarket.request("/index-contenido?activo=true");
        aplicarDestacadosIndex(contenidos.filter((c) => c.tipo === "destacado"));
        aplicarPromocionesIndex(contenidos.filter((c) => c.tipo === "promocion"));
    } catch (error) {
        console.warn("No se pudo cargar contenido del index:", error.message);
    }
}

function ordenarContenido(items) {
    return [...items].sort((a, b) => Number(a.orden || 0) - Number(b.orden || 0));
}

function esImagen(valor) {
    return /^https?:\/\//i.test(valor || "") || /^data:image\//i.test(valor || "") || /\.(png|jpg|jpeg|gif|webp|svg)$/i.test(valor || "") || String(valor || "").startsWith("img/");
}

function aplicarDestacadosIndex(items) {
    if (!items.length) return;

    const intro = items.find((item) => item.clave === "intro") || items[0];
    const titulo = document.querySelector("#destacados .titulo-section h2");
    const desc = document.querySelector("#destacados .titulo-section p");

    if (intro && titulo) titulo.textContent = intro.titulo || titulo.textContent;
    if (intro && desc) desc.textContent = intro.descripcion || desc.textContent;

    const cards = ordenarContenido(items.filter((item) => item.clave !== "intro" && item.clave !== "boton"));
    const grid = document.querySelector("#destacados .productos-grid");

    if (grid && cards.length) {
        grid.innerHTML = cards.slice(0, 4).map((item) => {
            const visual = item.imagen || "🛍️";
            const media = esImagen(visual)
                ? `<img src="${FastMarket.escapeHTML(visual)}" alt="${FastMarket.escapeHTML(item.titulo)}" onerror="this.style.display='none'">`
                : FastMarket.escapeHTML(visual);

            return `
                <article class="producto-card">
                    <div class="producto-img">${media}</div>
                    <h3>${FastMarket.escapeHTML(item.titulo)}</h3>
                    <p>${FastMarket.escapeHTML(item.descripcion || "")}</p>
                    <span>${FastMarket.escapeHTML(item.enlace || "")}</span>
                </article>`;
        }).join("");
    }

    const boton = items.find((item) => item.clave === "boton");
    const link = document.querySelector("#destacados .boton-centro a");

    if (boton && link) {
        link.textContent = boton.titulo || "Ver catálogo completo";
        link.href = boton.enlace || "productos.html";
    }
}

function aplicarPromocionesIndex(items) {
    if (!items.length) return;

    const intro = items.find((item) => item.clave === "intro") || items[0];
    const titulo = document.querySelector("#ofertas .oferta-texto h2");
    const desc = document.querySelector("#ofertas .oferta-texto > p");

    if (intro && titulo) titulo.textContent = intro.titulo || titulo.textContent;
    if (intro && desc) desc.textContent = intro.descripcion || desc.textContent;

    const puntos = ordenarContenido(items.filter((item) => item.clave.startsWith("punto")));
    const lista = document.getElementById("oferta-lista");

    if (lista && puntos.length) {
        lista.innerHTML = puntos.map((item) => `<p>✔ ${FastMarket.escapeHTML(item.titulo)}</p>`).join("");
    }

    const boton = items.find((item) => item.clave === "boton");
    const link = document.getElementById("boton-oferta-index");

    if (boton && link) {
        link.textContent = boton.titulo || "Ver ofertas disponibles";
        link.href = "productos.html?ofertas=1";
    }

    const oferta = items.find((item) => item.clave === "oferta");
    const caja = document.querySelector("#ofertas .oferta-caja");

    if (oferta && caja) {
        caja.dataset.personalizado = "true";
        caja.innerHTML = `
            <h3>${FastMarket.escapeHTML(oferta.titulo || "Oferta")}</h3>
            <p>${FastMarket.escapeHTML(oferta.descripcion || "")}</p>
            <small>${FastMarket.escapeHTML(oferta.enlace || "")}</small>`;
    }
}


async function cargarProductosIndex() {
    try {
        const productos = await FastMarket.request("/productos");
        pintarOferta(productos.filter((p) => p.oferta)[0]);
    } catch (error) {
        console.warn("No se pudieron cargar productos del index:", error.message);
    }
}

function pintarDestacados(productos) {
    const grid = document.querySelector("#destacados .productos-grid");
    if (!grid || !productos.length) return;

    grid.innerHTML = productos.map((p) => `
        <article class="producto-card">
            <a href="detalle-producto.html?id=${p.id}">
                <img src="${FastMarket.escapeHTML(p.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(p.nombre)}" onerror="this.src='img/logo.png'">
            </a>
            <h3>${FastMarket.escapeHTML(p.nombre)}</h3>
            <p>${FastMarket.escapeHTML(p.descripcion || "")}</p>
            <span>${FastMarket.money(p.precio)}</span>
        </article>
    `).join("");
}

function pintarOferta(producto) {
    if (!producto) return;
    const caja = document.querySelector("#ofertas .oferta-caja");
    if (!caja || caja.dataset.personalizado === "true") return;

    const descuento = producto.precioAntes
        ? Math.round((1 - Number(producto.precio) / Number(producto.precioAntes)) * 100)
        : 0;

    caja.innerHTML = `
        <h3>${descuento > 0 ? `-${descuento}%` : "Oferta"}</h3>
        <p>${FastMarket.escapeHTML(producto.nombre)}</p>
        <small>${FastMarket.money(producto.precio)} ${producto.precioAntes ? `antes ${FastMarket.money(producto.precioAntes)}` : ""}</small>
    `;
}
