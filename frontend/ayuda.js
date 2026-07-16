document.addEventListener("DOMContentLoaded", () => {
    FastMarket.activarMenuCliente();

    const buscarAyuda = document.getElementById("buscar-ayuda");
    const faqItems = document.querySelectorAll(".faq-item");
    const sinResultados = document.getElementById("sin-resultados");

    if (buscarAyuda) {
        buscarAyuda.addEventListener("input", () => {
            const texto = buscarAyuda.value.trim().toLowerCase();
            let encontrados = 0;

            faqItems.forEach((item) => {
                const visible = item.textContent.toLowerCase().includes(texto);
                item.style.display = visible ? "block" : "none";
                if (visible) encontrados++;
            });

            if (sinResultados) sinResultados.classList.toggle("oculto", encontrados > 0);
        });
    }
});
