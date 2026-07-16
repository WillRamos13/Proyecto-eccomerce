let vendedor = null;
let productos = [];
let pedidos = [];
let cupones = [];
let productPage = { page: 0, size: 20, totalPages: 1, totalElements: 0 };
let estadisticas = null;
let chartVentasDia = null;
let chartPedidosEstado = null;
let editorCaracteristicasProducto = null;

const estadosPedido = ["PENDIENTE", "CONFIRMADO", "PREPARANDO", "CAMINO", "ENTREGADO", "CANCELADO"];

document.addEventListener("DOMContentLoaded", async () => {
    vendedor = FastMarket.getCliente();
    if (!vendedor) {
        FastMarket.notify("Debes iniciar sesión como vendedor.", "warning");
        window.location.href = "login.html";
        return;
    }
    if (vendedor.rol === "ADMIN") {
        window.location.href = "admin.html";
        return;
    }
    if (vendedor.rol !== "VENDEDOR") {
        FastMarket.notify("No tienes permisos para acceder al panel vendedor.", "warning");
        window.location.href = "productos.html";
        return;
    }

    setText("nombre-vendedor", vendedor.nombre || "Vendedor");
    setText("nombre-vendedor-hero", vendedor.nombre || "vendedor");
    pintarPerfilVendedor(vendedor);
    activarNavegacion();
    editorCaracteristicasProducto = FastMarketProductoCaracteristicas.crear({
        categoriaId: "vp-categoria",
        tipoId: "vp-tipo-producto",
        datalistId: "vp-tipos-producto-opciones",
        toggleId: "vp-usar-caracteristicas",
        panelId: "vp-panel-caracteristicas",
        sugerenciasId: "vp-sugerencias-caracteristicas",
        listaId: "vp-lista-caracteristicas",
        agregarId: "vp-agregar-caracteristica"
    });
    activarProductos();
    activarPedidos();
    activarCupones();
    activarEstadisticas();
    document.getElementById("cerrar-sesion-vendedor")?.addEventListener("click", () => {
        FastMarket.cerrarSesion();
        window.location.href = "login.html";
    });

    await cargarTodo();
});

function pintarPerfilVendedor(usuario) {
    const nombre = usuario?.nombre || "Vendedor";
    setText("perfil-vendedor-nombre", nombre);
    setText("perfil-vendedor-correo", usuario?.correo || "Correo no registrado");
    setText("perfil-vendedor-rol", usuario?.rol || "VENDEDOR");
    setText("perfil-vendedor-telefono", usuario?.telefono || "No registrado");
    setText("perfil-vendedor-documento", usuario?.documento || "No registrado");
    setText("perfil-vendedor-inicial", nombre.trim().charAt(0).toUpperCase() || "V");
}

async function cargarTodo() {
    await Promise.allSettled([cargarProductos(), cargarPedidos(), cargarCupones(), cargarEstadisticas()]);
    pintarMetricas();
}

function activarNavegacion() {
    document.addEventListener("click", (e) => {
        const btn = e.target.closest("[data-panel]");
        if (!btn) return;
        e.preventDefault();
        mostrarPanel(btn.dataset.panel);
    });
}

function mostrarPanel(id) {
    document.querySelectorAll(".vendor-panel").forEach((panel) => panel.classList.remove("activo"));
    document.getElementById(id)?.classList.add("activo");
    document.querySelectorAll(".vendor-nav button").forEach((btn) => btn.classList.toggle("activo", btn.dataset.panel === id));
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function activarProductos() {
    document.getElementById("form-vendedor-producto")?.addEventListener("submit", guardarProducto);
    document.getElementById("vp-limpiar")?.addEventListener("click", limpiarProducto);
    document.getElementById("vp-buscar")?.addEventListener("input", pintarProductos);
    document.getElementById("vp-imagen-file")?.addEventListener("change", cargarImagenProducto);
    document.getElementById("vp-prev")?.addEventListener("click", async () => {
        if (productPage.page > 0) {
            productPage.page--;
            await cargarProductos();
        }
    });
    document.getElementById("vp-next")?.addEventListener("click", async () => {
        if (productPage.page + 1 < productPage.totalPages) {
            productPage.page++;
            await cargarProductos();
        }
    });
    document.getElementById("vp-tabla")?.addEventListener("click", async (e) => {
        const editar = e.target.closest("[data-editar-producto]");
        const eliminar = e.target.closest("[data-eliminar-producto]");
        if (editar) editarProducto(Number(editar.dataset.editarProducto));
        if (eliminar) await eliminarProducto(Number(eliminar.dataset.eliminarProducto));
    });
}

async function cargarProductos() {
    try {
        const page = await FastMarket.request(`/productos/page?page=${productPage.page}&size=${productPage.size}`, { auth: true });
        productos = Array.isArray(page?.content) ? page.content.filter((p) => p.activo !== false) : [];
        productPage.totalPages = Math.max(1, Number(page?.totalPages || 1));
        productPage.totalElements = Number(page?.totalElements ?? productos.length);
        productPage.page = Math.min(Number(page?.number || 0), productPage.totalPages - 1);
        actualizarPaginacionProductos();
        pintarProductos();
        pintarMetricas();
    } catch (error) {
        toast(error.message);
    }
}

function pintarProductos() {
    const tbody = document.getElementById("vp-tabla");
    if (!tbody) return;
    const q = value("vp-buscar").toLowerCase();
    const lista = productos.filter((p) => `${p.nombre} ${p.descripcion} ${p.categoria} ${p.tipoProducto || ""} ${JSON.stringify(p.caracteristicas || {})}`.toLowerCase().includes(q));
    tbody.innerHTML = lista.length ? "" : `<tr><td colspan="6">No tienes productos registrados.</td></tr>`;
    lista.forEach((p) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${FastMarket.escapeHTML(p.nombre)}</strong><br><small>${FastMarket.escapeHTML(detalleCortoProducto(p))}</small></td>
            <td>${formatearCategoria(p.categoria)}</td>
            <td><strong>${FastMarket.money(p.precio)}</strong>${p.precioAntes ? `<br><small>Antes: ${FastMarket.money(p.precioAntes)}</small>` : ""}</td>
            <td>${stockBadge(p.stock)}</td>
            <td>${p.oferta ? `<span class="estado-oferta">Oferta</span>` : `<span class="estado-normal">Normal</span>`}</td>
            <td><button class="btn-mini" data-editar-producto="${p.id}">Editar</button> <button class="btn-mini secondary" data-eliminar-producto="${p.id}">Eliminar</button></td>`;
        tbody.appendChild(tr);
    });
}

function detalleCortoProducto(p) {
    const caracteristicas = p.caracteristicas && typeof p.caracteristicas === "object"
        ? Object.values(p.caracteristicas).filter(Boolean).slice(0, 2)
        : [];
    const detalles = [p.tipoProducto, ...caracteristicas, p.descripcion].filter(Boolean);
    return detalles.join(" · ") || "Sin descripción";
}

async function guardarProducto(e) {
    e.preventDefault();
    const id = value("vp-id");
    const imagenesProducto = obtenerImagenesFormulario("vp-imagenes", value("vp-imagen"));
    const caracteristicas = editorCaracteristicasProducto?.getCaracteristicas() || {};
    const compatibilidad = editorCaracteristicasProducto?.getCamposCompatibilidad() || {};
    const payload = {
        nombre: value("vp-nombre"),
        categoria: value("vp-categoria"),
        tipoProducto: editorCaracteristicasProducto?.getTipo() || "",
        precio: Number(value("vp-precio")),
        precioAntes: value("vp-precio-antes") ? Number(value("vp-precio-antes")) : null,
        stock: Number(value("vp-stock")),
        imagenes: imagenesProducto,
        imagen: imagenesProducto[0] || "img/logo.png",
        descripcion: value("vp-descripcion"),
        caracteristicas,
        ...compatibilidad,
        detallesAdicionales: value("vp-detalles-adicionales"),
        oferta: checked("vp-oferta"),
        destacado: checked("vp-destacado")
    };

    if (!payload.nombre || !payload.categoria || payload.precio <= 0 || payload.stock < 0) {
        toast("Completa correctamente los datos del producto.");
        return;
    }

    try {
        await FastMarket.request(id ? `/productos/${id}` : "/productos", {
            method: id ? "PUT" : "POST",
            body: payload,
            auth: true
        });
        toast(id ? "Producto actualizado." : "Producto creado.");
        limpiarProducto();
        await cargarProductos();
    } catch (error) {
        toast(error.message);
    }
}

function editarProducto(id) {
    const p = productos.find((x) => Number(x.id) === Number(id));
    if (!p) return;
    setValue("vp-id", p.id);
    setValue("vp-nombre", p.nombre);
    setValue("vp-categoria", p.categoria);
    setValue("vp-precio", p.precio);
    setValue("vp-precio-antes", p.precioAntes || "");
    setValue("vp-stock", p.stock);
    const imagenes = obtenerImagenesProducto(p);
    setValue("vp-imagen", imagenes[0] || p.imagen || "");
    setValue("vp-imagenes", JSON.stringify(imagenes));
    setValue("vp-descripcion", p.descripcion || "");
    editorCaracteristicasProducto?.setData(p);
    setValue("vp-detalles-adicionales", p.detallesAdicionales || "");
    setChecked("vp-oferta", !!p.oferta);
    setChecked("vp-destacado", !!p.destacado);
    setText("vp-form-title", "Editar producto");
    pintarPreviewImagenes("vp-preview", "vp-preview-lista", imagenes);
    mostrarPanel("panel-productos");
}

async function eliminarProducto(id) {
    if (!(await FastMarket.confirmAction("¿Seguro que deseas eliminar este producto?"))) return;
    try {
        await FastMarket.request(`/productos/${id}`, { method: "DELETE", auth: true });
        toast("Producto eliminado de tu catálogo.");
        await cargarProductos();
    } catch (error) {
        toast(error.message);
    }
}

function limpiarProducto() {
    document.getElementById("form-vendedor-producto")?.reset();
    setValue("vp-id", "");
    setValue("vp-imagen", "");
    setValue("vp-imagenes", "");
    setText("vp-form-title", "Agregar producto");
    editorCaracteristicasProducto?.reset();
    document.getElementById("vp-preview")?.classList.add("oculto");
    const lista = document.getElementById("vp-preview-lista");
    if (lista) lista.innerHTML = "";
}

function cargarImagenProducto(e) {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    if (files.some((file) => !file.type.startsWith("image/"))) return toast("Selecciona solo imágenes válidas.");

    Promise.all(files.map(leerImagenReducida)).then((nuevasImagenes) => {
        const actuales = obtenerImagenesFormulario("vp-imagenes", value("vp-imagen"))
            .filter((img) => img !== "img/logo.png");
        const imagenes = unirImagenes(actuales, nuevasImagenes).slice(0, 8);
        setValue("vp-imagen", imagenes[0] || "img/logo.png");
        setValue("vp-imagenes", JSON.stringify(imagenes));
        pintarPreviewImagenes("vp-preview", "vp-preview-lista", imagenes);
        e.target.value = "";
    }).catch(() => toast("No se pudieron procesar las imágenes."));
}

function unirImagenes(...grupos) {
    const resultado = [];
    grupos.flat().forEach((img) => {
        const limpio = String(img || "").trim();
        if (limpio && !resultado.includes(limpio)) resultado.push(limpio);
    });
    return resultado;
}

function leerImagenReducida(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = reject;
        reader.onload = () => {
            const img = new Image();
            img.onerror = reject;
            img.onload = () => {
                const max = 1200;
                let { width, height } = img;
                if (width > max || height > max) {
                    const escala = Math.min(max / width, max / height);
                    width = Math.round(width * escala);
                    height = Math.round(height * escala);
                }
                const canvas = document.createElement("canvas");
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext("2d");
                ctx.drawImage(img, 0, 0, width, height);
                resolve(canvas.toDataURL("image/jpeg", 0.82));
            };
            img.src = reader.result;
        };
        reader.readAsDataURL(file);
    });
}

function obtenerImagenesFormulario(hiddenId, imagenPrincipal = "") {
    const raw = value(hiddenId);
    let imagenes = [];
    try {
        const parsed = JSON.parse(raw || "[]");
        if (Array.isArray(parsed)) imagenes = parsed;
    } catch {
        imagenes = raw ? raw.split("\n") : [];
    }
    if (!imagenes.length && imagenPrincipal) imagenes = [imagenPrincipal];
    imagenes = imagenes.map((img) => String(img || "").trim()).filter(Boolean);
    return imagenes.length ? [...new Set(imagenes)] : ["img/logo.png"];
}

function obtenerImagenesProducto(producto) {
    let imagenes = [];
    if (Array.isArray(producto?.imagenes)) imagenes = producto.imagenes;
    if (typeof producto?.imagenes === "string" && producto.imagenes.trim()) {
        try {
            const parsed = JSON.parse(producto.imagenes);
            if (Array.isArray(parsed)) imagenes = parsed;
        } catch {
            imagenes = producto.imagenes.split("\n");
        }
    }
    if (!imagenes.length && producto?.imagen) imagenes = [producto.imagen];
    imagenes = imagenes.map((img) => String(img || "").trim()).filter(Boolean);
    return imagenes.length ? [...new Set(imagenes)] : ["img/logo.png"];
}

function pintarPreviewImagenes(previewId, listaId, imagenes) {
    const preview = document.getElementById(previewId);
    const lista = document.getElementById(listaId);
    if (!preview || !lista) return;
    const finales = (imagenes || []).map((img) => String(img || "").trim()).filter(Boolean);
    lista.innerHTML = finales.map((src, index) => `
        <figure class="preview-item">
            <img src="${FastMarket.escapeHTML(src)}" alt="Imagen ${index + 1}" onerror="this.src='img/logo.png'">
            ${index === 0 ? "<figcaption>Principal</figcaption>" : ""}
        </figure>
    `).join("");
    preview.classList.toggle("oculto", finales.length === 0);
}

function actualizarPaginacionProductos() {
    setText("vp-page-info", `Página ${productPage.page + 1} de ${productPage.totalPages}`);
    const prev = document.getElementById("vp-prev");
    const next = document.getElementById("vp-next");
    if (prev) prev.disabled = productPage.page <= 0;
    if (next) next.disabled = productPage.page + 1 >= productPage.totalPages;
}

function activarPedidos() {
    document.getElementById("vp-pedidos")?.addEventListener("click", async (e) => {
        const historial = e.target.closest("[data-historial-pedido]");
        if (historial) await verHistorialPedido(Number(historial.dataset.historialPedido));
    });
}

async function cargarPedidos() {
    try {
        pedidos = await FastMarket.request(`/pedidos/vendedor/${vendedor.id}`, { auth: true });
        pintarPedidos();
        pintarMetricas();
    } catch (error) {
        toast(error.message);
    }
}

function pintarPedidos() {
    const tbody = document.getElementById("vp-pedidos");
    if (!tbody) return;
    tbody.innerHTML = pedidos.length ? "" : `<tr><td colspan="7">Todavía no tienes pedidos vinculados.</td></tr>`;
    pedidos.forEach((p) => {
        const productosTexto = (p.items || []).map((i) => `${i.productoNombre} x${i.cantidad}`).join(", ");
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${FastMarket.escapeHTML(p.codigo)}</strong><br><small>${fecha(p.fecha)}</small></td>
            <td>${FastMarket.escapeHTML(p.usuarioNombre || "Cliente")}</td>
            <td>${FastMarket.escapeHTML(productosTexto)}</td>
            <td>${FastMarket.money(p.total)}</td>
            <td>${FastMarket.estadoTexto(p.estado)}</td>
            <td><span class="estado-normal">Solo admin</span></td>
            <td><button class="btn-mini secondary" data-historial-pedido="${p.id}">Ver</button></td>`;
        tbody.appendChild(tr);
    });
}


async function verHistorialPedido(id) {
    const panel = document.getElementById("vp-historial-pedido");
    if (!panel) return;
    try {
        const historial = await FastMarket.request(`/pedidos/${id}/historial`, { auth: true });
        panel.classList.remove("oculto");
        panel.innerHTML = `<h3>Historial del pedido</h3>${pintarHistorialPedido(historial)}`;
    } catch (error) {
        toast(error.message);
    }
}

function pintarHistorialPedido(historial) {
    if (!historial?.length) return `<p>No hay historial registrado.</p>`;
    return `<ul>${historial.map((h) => `<li><strong>${fecha(h.fecha)}</strong> — ${FastMarket.estadoTexto(h.estadoAnterior || "CREADO")} → ${FastMarket.estadoTexto(h.estadoNuevo)} por ${FastMarket.escapeHTML(h.actorNombre || "Sistema")}</li>`).join("")}</ul>`;
}

function activarCupones() {
    document.getElementById("form-vendedor-cupon")?.addEventListener("submit", guardarCupon);
    document.getElementById("vc-limpiar")?.addEventListener("click", limpiarCupon);
    document.getElementById("vc-buscar")?.addEventListener("input", pintarCupones);
    document.getElementById("vc-tabla")?.addEventListener("click", async (e) => {
        const editar = e.target.closest("[data-editar-cupon]");
        const eliminar = e.target.closest("[data-eliminar-cupon]");
        const usos = e.target.closest("[data-usos-cupon]");
        if (editar) editarCupon(Number(editar.dataset.editarCupon));
        if (eliminar) await eliminarCupon(Number(eliminar.dataset.eliminarCupon));
        if (usos) await verUsosCupon(Number(usos.dataset.usosCupon));
    });
}

async function cargarCupones() {
    try {
        cupones = await FastMarket.request("/cupones", { auth: true });
        pintarCupones();
        pintarMetricas();
    } catch (error) {
        toast(error.message);
    }
}

function pintarCupones() {
    const tbody = document.getElementById("vc-tabla");
    if (!tbody) return;
    const q = value("vc-buscar").toLowerCase();
    const lista = cupones.filter((c) => `${c.codigo} ${c.descripcion}`.toLowerCase().includes(q));
    tbody.innerHTML = lista.length ? "" : `<tr><td colspan="6">No tienes cupones registrados.</td></tr>`;
    lista.forEach((c) => {
        const descuento = `${Number(c.porcentaje || 0) > 0 ? `${c.porcentaje}%` : ""}${Number(c.montoFijo || 0) > 0 ? ` S/ ${Number(c.montoFijo).toFixed(2)}` : ""}`.trim();
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${FastMarket.escapeHTML(c.codigo)}</strong><br><small>${FastMarket.escapeHTML(c.descripcion || "")}</small></td>
            <td>${descuento || "-"}<br><small>Mín: ${FastMarket.money(c.montoMinimo || 0)}</small></td>
            <td>${vigenciaCupon(c)}</td>
            <td>${c.usosActuales || 0}${c.usosMaximos ? `/${c.usosMaximos}` : ""}</td>
            <td><span class="${c.activo ? "estado-activo" : "estado-inactivo"}">${c.activo ? "Activo" : "Inactivo"}</span></td>
            <td><button class="btn-mini" data-editar-cupon="${c.id}">Editar</button> <button class="btn-mini secondary" data-usos-cupon="${c.id}">Usos</button> <button class="btn-mini secondary" data-eliminar-cupon="${c.id}">Eliminar</button></td>`;
        tbody.appendChild(tr);
    });
}

async function guardarCupon(e) {
    e.preventDefault();
    const id = value("vc-id");
    const payload = {
        codigo: value("vc-codigo"),
        descripcion: value("vc-descripcion"),
        tipo: "VENDEDOR",
        porcentaje: Number(value("vc-porcentaje") || 0),
        montoFijo: Number(value("vc-monto") || 0),
        montoMinimo: Number(value("vc-minimo") || 0),
        usosMaximos: value("vc-usos") ? Number(value("vc-usos")) : null,
        fechaInicio: value("vc-inicio") || null,
        fechaFin: value("vc-fin") || null,
        activo: checked("vc-activo")
    };
    if (!payload.codigo) return toast("Ingresa el código del cupón.");
    if (payload.porcentaje <= 0 && payload.montoFijo <= 0) return toast("Ingresa porcentaje o monto fijo.");
    try {
        await FastMarket.request(id ? `/cupones/${id}` : "/cupones", { method: id ? "PUT" : "POST", body: payload, auth: true });
        toast(id ? "Cupón actualizado." : "Cupón creado.");
        limpiarCupon();
        await cargarCupones();
    } catch (error) {
        toast(error.message);
    }
}

function editarCupon(id) {
    const c = cupones.find((x) => Number(x.id) === Number(id));
    if (!c) return;
    setValue("vc-id", c.id);
    setValue("vc-codigo", c.codigo || "");
    setValue("vc-descripcion", c.descripcion || "");
    setValue("vc-porcentaje", c.porcentaje || "");
    setValue("vc-monto", c.montoFijo || "");
    setValue("vc-minimo", c.montoMinimo || "");
    setValue("vc-usos", c.usosMaximos || "");
    setValue("vc-inicio", toDatetimeLocal(c.fechaInicio));
    setValue("vc-fin", toDatetimeLocal(c.fechaFin));
    setChecked("vc-activo", !!c.activo);
    setText("vc-form-title", "Editar cupón");
    mostrarPanel("panel-cupones");
}

async function verUsosCupon(id) {
    const panel = document.getElementById("vc-usos-panel");
    if (!panel) return;
    try {
        const usos = await FastMarket.request(`/cupones/${id}/usos`, { auth: true });
        panel.classList.remove("oculto");
        panel.innerHTML = `<h3>Usos del cupón</h3>${usos?.length ? `<ul>${usos.map((u) => `<li><strong>${fecha(u.fecha)}</strong> — ${FastMarket.escapeHTML(u.usuarioNombre || "Cliente")} ahorró ${FastMarket.money(u.descuentoAplicado)} en ${FastMarket.escapeHTML(u.pedidoCodigo || "pedido")}</li>`).join("")}</ul>` : `<p>Este cupón todavía no tiene usos.</p>`}`;
    } catch (error) {
        toast(error.message);
    }
}

async function eliminarCupon(id) {
    if (!(await FastMarket.confirmAction("¿Seguro que deseas desactivar este cupón?"))) return;
    try {
        await FastMarket.request(`/cupones/${id}`, { method: "DELETE", auth: true });
        toast("Cupón desactivado.");
        await cargarCupones();
    } catch (error) {
        toast(error.message);
    }
}

function limpiarCupon() {
    document.getElementById("form-vendedor-cupon")?.reset();
    setValue("vc-id", "");
    setValue("vc-inicio", "");
    setValue("vc-fin", "");
    setChecked("vc-activo", true);
    setText("vc-form-title", "Agregar cupón");
}


function vigenciaCupon(c) {
    const inicio = c.fechaInicio ? fecha(c.fechaInicio) : "Ahora";
    const fin = c.fechaFin ? fecha(c.fechaFin) : "30 días por defecto";
    return `<small>${FastMarket.escapeHTML(inicio)}<br>hasta ${FastMarket.escapeHTML(fin)}</small>`;
}

function toDatetimeLocal(valor) {
    if (!valor) return "";
    const d = new Date(valor);
    if (Number.isNaN(d.getTime())) return "";
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function activarEstadisticas() {
    document.getElementById("est-rango-dias")?.addEventListener("change", async (e) => {
        await cargarEstadisticas(Number(e.target.value) || 14);
    });
}

async function cargarEstadisticas(dias) {
    if (!vendedor?.id) return;
    const rango = dias || Number(value("est-rango-dias") || 14);
    try {
        estadisticas = await FastMarket.request(`/pedidos/vendedor/${vendedor.id}/estadisticas?dias=${rango}`, { auth: true });
        pintarEstadisticas();
    } catch (error) {
        toast(error.message);
    }
}

function pintarEstadisticas() {
    if (!estadisticas) return;
    const resumen = estadisticas.resumen || {};
    setText("est-ventas-hoy", FastMarket.money(resumen.ventasHoy || 0));
    setText("est-ventas-semana", FastMarket.money(resumen.ventasSemana || 0));
    setText("est-ventas-mes", FastMarket.money(resumen.ventasMes || 0));
    setText("est-ventas-total", FastMarket.money(resumen.ventasTotal || 0));
    setText("est-pedidos-total", resumen.pedidosTotal || 0);
    setText("est-unidades", resumen.unidadesVendidas || 0);
    setText("est-ticket", FastMarket.money(resumen.ticketPromedio || 0));

    const sinDatos = document.getElementById("est-sin-datos");
    if (sinDatos) sinDatos.classList.toggle("oculto", Number(resumen.pedidosTotal || 0) > 0);

    pintarGraficoVentasPorDia(estadisticas.ventasPorDia || []);
    pintarGraficoPedidosPorEstado(estadisticas.porEstado || []);
    pintarTopProductos(estadisticas.topProductos || []);
}

function pintarGraficoVentasPorDia(datos) {
    const canvas = document.getElementById("grafico-ventas-dia");
    if (!canvas || typeof Chart === "undefined") return;
    const etiquetas = datos.map((d) => new Date(`${d.fecha}T00:00:00`).toLocaleDateString("es-PE", { day: "2-digit", month: "short" }));
    const valores = datos.map((d) => Number(d.total || 0));

    if (chartVentasDia) chartVentasDia.destroy();
    chartVentasDia = new Chart(canvas.getContext("2d"), {
        type: "line",
        data: {
            labels: etiquetas,
            datasets: [{
                label: "Ventas (S/)",
                data: valores,
                borderColor: "#fd6403",
                backgroundColor: "rgba(253, 100, 3, .15)",
                tension: .3,
                fill: true,
                pointRadius: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { callback: (v) => `S/ ${v}` } } }
        }
    });
}

function pintarGraficoPedidosPorEstado(datos) {
    const canvas = document.getElementById("grafico-pedidos-estado");
    if (!canvas || typeof Chart === "undefined") return;
    const colores = {
        PENDIENTE: "#f59e0b",
        CONFIRMADO: "#3b82f6",
        PREPARANDO: "#8b5cf6",
        CAMINO: "#06b6d4",
        ENTREGADO: "#22c55e",
        CANCELADO: "#ef4444"
    };
    const etiquetas = datos.map((d) => FastMarket.estadoTexto(d.estado));
    const valores = datos.map((d) => Number(d.cantidad || 0));
    const fondos = datos.map((d) => colores[d.estado] || "#9ca3af");

    if (chartPedidosEstado) {
        chartPedidosEstado.destroy();
        chartPedidosEstado = null;
    }
    if (!datos.length) return;
    chartPedidosEstado = new Chart(canvas.getContext("2d"), {
        type: "doughnut",
        data: { labels: etiquetas, datasets: [{ data: valores, backgroundColor: fondos, borderWidth: 0 }] },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: "bottom" } }
        }
    });
}

function pintarTopProductos(lista) {
    const tbody = document.getElementById("est-top-productos");
    if (!tbody) return;
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="3">Todavía no hay productos vendidos.</td></tr>`;
        return;
    }
    tbody.innerHTML = "";
    lista.forEach((p) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${FastMarket.escapeHTML(p.nombre || "Producto")}</td>
            <td>${p.unidadesVendidas || 0}</td>
            <td>${FastMarket.money(p.totalVentas || 0)}</td>`;
        tbody.appendChild(tr);
    });
}

function pintarMetricas() {
    setText("metric-productos", productPage.totalElements || productos.length);
    setText("metric-pedidos", pedidos.length);
    setText("metric-ventas", FastMarket.money(pedidos.reduce((sum, p) => sum + Number(p.total || 0), 0)));
    setText("metric-cupones", cupones.length);
}

function stockBadge(stock) {
    const n = Number(stock || 0);
    if (n <= 0) return `<span class="stock-empty">Agotado</span>`;
    if (n <= 5) return `<span class="stock-low">Bajo · ${n}</span>`;
    return `<span class="stock-ok">${n} unidades</span>`;
}

function formatearCategoria(valor) {
    const map = {
        moda: "Moda",
        tecnologia: "Tecnología",
        hogar: "Hogar",
        estudio: "Estudio",
        belleza: "Belleza",
        deportes: "Deportes",
        juguetes: "Juguetes"
    };
    return map[valor] || valor || "";
}

function value(id) { return String(document.getElementById(id)?.value || "").trim(); }
function setValue(id, val) { const el = document.getElementById(id); if (el) el.value = val ?? ""; }
function checked(id) { return !!document.getElementById(id)?.checked; }
function setChecked(id, val) { const el = document.getElementById(id); if (el) el.checked = !!val; }
function setText(id, val) { const el = document.getElementById(id); if (el) el.textContent = val ?? ""; }
function fecha(valor) { return valor ? new Date(valor).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" }) : ""; }
function toast(mensaje) { FastMarket.notify ? FastMarket.notify(mensaje) : alert(mensaje); }
