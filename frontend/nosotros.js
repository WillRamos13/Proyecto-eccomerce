document.addEventListener("DOMContentLoaded", async () => {
    FastMarket.activarBuscador("buscador", "busqueda");
    FastMarket.activarMenuCliente();

    await cargarContenidoNosotros();
});

async function cargarContenidoNosotros() {
    try {
        const contenidos = await FastMarket.request("/index-contenido?activo=true");
        aplicarOpinionesNosotros(contenidos.filter((c) => c.tipo === "opinion"));
        aplicarAyudaNosotros(contenidos.filter((c) => c.tipo === "ayuda"));
    } catch (error) {
        console.warn("No se pudo cargar contenido de nosotros:", error.message);
    }
}

function ordenarContenido(items) {
    return [...items].sort((a, b) => Number(a.orden || 0) - Number(b.orden || 0));
}

function aplicarOpinionesNosotros(items) {
    if (!items.length) return;

    const intro = items.find((item) => item.clave === "intro") || items[0];
    const titulo = document.querySelector("#testimonios .titulo-section h2");
    const desc = document.querySelector("#testimonios .titulo-section p");

    if (intro && titulo) titulo.textContent = intro.titulo || titulo.textContent;
    if (intro && desc) desc.textContent = intro.descripcion || desc.textContent;

    const opiniones = ordenarContenido(items.filter((item) => item.clave !== "intro"));
    const grid = document.querySelector("#testimonios .testimonios-grid");

    if (grid && opiniones.length) {
        grid.innerHTML = opiniones.map((item) => `
            <article class="testimonio">
                <p>${FastMarket.escapeHTML(item.descripcion || "")}</p>
                <h4>- ${FastMarket.escapeHTML(item.titulo)}</h4>
            </article>`).join("");
    }
}

function aplicarAyudaNosotros(items) {
    if (!items.length) return;

    const intro = items.find((item) => item.clave === "intro") || items[0];
    const titulo = document.querySelector("#preguntas .titulo-section h2");
    const desc = document.querySelector("#preguntas .titulo-section p");

    if (intro && titulo) titulo.textContent = intro.titulo || titulo.textContent;
    if (intro && desc) desc.textContent = intro.descripcion || desc.textContent;

    const faqs = ordenarContenido(items.filter((item) => item.clave !== "intro"));
    const contenedor = document.querySelector("#preguntas .faq-contenedor");

    if (contenedor && faqs.length) {
        contenedor.innerHTML = faqs.map((item) => `
            <article class="faq-item">
                <h3>${FastMarket.escapeHTML(item.titulo)}</h3>
                <p>${FastMarket.escapeHTML(item.descripcion || "")}</p>
            </article>`).join("");
    }
}
