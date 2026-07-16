let productos = [];
let banners = [];
let pedidos = [];
let usuarios = [];
let indexContenidos = [];
let cupones = [];
let vendedores = [];
let usuarioActual = null;
const TIPOS_INDEX_PERMITIDOS = ["destacado", "promocion", "opinion", "ayuda"];
const adminPages = { productos: { page: 0, size: 20, totalPages: 1 }, pedidos: { page: 0, size: 20, totalPages: 1 } };
let filtroVendedorProductos = "todos";
let filtroVendedorPedidos = "todos";
let editorCaracteristicasProducto = null;
let reporteAdmin = null;

function validarPasswordSegura(password) {
    return Boolean(
        password &&
        password.length >= 8 &&
        !/\s/.test(password) &&
        /[a-z]/.test(password) &&
        /[A-Z]/.test(password) &&
        /\d/.test(password)
    );
}

function mensajePasswordSegura() {
    return "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y no debe contener espacios.";
}

document.addEventListener("DOMContentLoaded", async () => {
    const admin = FastMarket.getCliente();
    if (!admin) {
        FastMarket.notify("Debes iniciar sesión.", "warning");
        window.location.href = "login.html";
        return;
    }
    if (admin.rol === "VENDEDOR") {
        window.location.href = "vendedor.html";
        return;
    }
    if (admin.rol !== "ADMIN") {
        FastMarket.notify("No tienes permisos para entrar al panel administrador.", "warning");
        window.location.href = "productos.html";
        return;
    }
    usuarioActual = admin;
    document.body.classList.toggle("modo-vendedor", false);
    setText("nombre-admin", admin.nombre || (admin.rol === "VENDEDOR" ? "Vendedor" : "Administrador"));
    pintarPerfilAdmin(admin);
    activarMenu();
    activarPaneles();
    editorCaracteristicasProducto = FastMarketProductoCaracteristicas.crear({
        categoriaId: "categoria",
        tipoId: "tipo-producto",
        datalistId: "tipos-producto-opciones",
        toggleId: "usar-caracteristicas",
        panelId: "panel-caracteristicas",
        sugerenciasId: "sugerencias-caracteristicas",
        listaId: "lista-caracteristicas",
        agregarId: "agregar-caracteristica"
    });
    activarProductos();
    activarBanners();
    activarPedidos();
    activarUsuarios();
    activarCupones();
    activarIndex();
    activarReportes();
    configurarPermisosPanel();
    marcarMenuActivo("panel-inicio");

    await cargarTodo();
});

function pintarPerfilAdmin(usuario) {
    const nombre = usuario?.nombre || "Administrador";
    setText("perfil-admin-nombre", nombre);
    setText("perfil-admin-correo", usuario?.correo || "Correo no registrado");
    setText("perfil-admin-rol", usuario?.rol || "ADMIN");
    setText("perfil-admin-telefono", usuario?.telefono || "No registrado");
    setText("perfil-admin-documento", usuario?.documento || "No registrado");
    setText("perfil-admin-inicial", nombre.trim().charAt(0).toUpperCase() || "A");
}

async function cargarTodo() {
    await Promise.allSettled([
        cargarProductos(),
        cargarBanners(),
        cargarPedidos(),
        cargarUsuarios(),
        cargarCupones(),
        cargarVendedores(),
        cargarIndices(),
        usuarioActual?.rol === "ADMIN" ? cargarIndexContenidos() : Promise.resolve()
    ]);
    pintarVendedoresAdmin();
    await cargarReportes();
}

function activarMenu() {
    const btn = document.getElementById("btn-menu-admin");
    const opciones = document.getElementById("opciones-admin");
    const salir = document.getElementById("cerrar-sesion");

    btn?.addEventListener("click", (e) => {
        e.stopPropagation();
        opciones?.classList.toggle("activo");
    });

    document.addEventListener("click", () => opciones?.classList.remove("activo"));

    salir?.addEventListener("click", (e) => {
        e.preventDefault();
        FastMarket.cerrarSesion();
        window.location.href = "login.html";
    });
}

function activarPaneles() {
    document.addEventListener("click", async (e) => {
        const productosVendor = e.target.closest("[data-ver-productos-vendedor]");
        if (productosVendor) {
            e.preventDefault();
            filtroVendedorProductos = productosVendor.dataset.verProductosVendedor;
            const select = document.getElementById("filtro-vendedor-productos");
            if (select) select.value = filtroVendedorProductos;
            adminPages.productos.page = 0;
            await cargarProductos();
            mostrarPanel("panel-productos");
            return;
        }

        const pedidosVendor = e.target.closest("[data-ver-pedidos-vendedor]");
        if (pedidosVendor) {
            e.preventDefault();
            filtroVendedorPedidos = pedidosVendor.dataset.verPedidosVendedor;
            const select = document.getElementById("filtro-vendedor-pedidos");
            if (select) select.value = filtroVendedorPedidos;
            adminPages.pedidos.page = 0;
            await cargarPedidos();
            mostrarPanel("panel-ventas");
            return;
        }

        const btn = e.target.closest("[data-panel]");
        if (!btn) return;
        e.preventDefault();
        if (btn.dataset.panel === "panel-productos") {
            filtroVendedorProductos = "todos";
            const select = document.getElementById("filtro-vendedor-productos");
            if (select) select.value = "todos";
            adminPages.productos.page = 0;
            await cargarProductos();
        }
        if (btn.dataset.panel === "panel-ventas") {
            filtroVendedorPedidos = "todos";
            const select = document.getElementById("filtro-vendedor-pedidos");
            if (select) select.value = "todos";
            adminPages.pedidos.page = 0;
            await cargarPedidos();
        }
        mostrarPanel(btn.dataset.panel);
    });
}

function mostrarPanel(id) {
    document.querySelectorAll(".panel-admin").forEach((panel) => panel.classList.remove("activo"));
    document.getElementById(id)?.classList.add("activo");
    document.getElementById("opciones-admin")?.classList.remove("activo");
    marcarMenuActivo(id);
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function marcarMenuActivo(id) {
    document.querySelectorAll("#opciones-admin [data-panel]").forEach((btn) => {
        btn.classList.toggle("activo", btn.dataset.panel === id);
    });
}


function activarProductos() {
    document.getElementById("form-producto")?.addEventListener("submit", guardarProducto);
    document.getElementById("btn-limpiar")?.addEventListener("click", limpiarProducto);
    document.getElementById("imagen-producto")?.addEventListener("change", cargarImagenesProducto);
    document.getElementById("buscar-admin")?.addEventListener("input", pintarProductos);
    document.getElementById("filtro-categoria")?.addEventListener("change", pintarProductos);
    document.getElementById("filtro-vendedor-productos")?.addEventListener("change", async (e) => {
        filtroVendedorProductos = e.target.value || "todos";
        adminPages.productos.page = 0;
        await cargarProductos();
    });
    document.getElementById("btn-productos-generales")?.addEventListener("click", async () => {
        filtroVendedorProductos = "general";
        const select = document.getElementById("filtro-vendedor-productos");
        if (select) select.value = "general";
        adminPages.productos.page = 0;
        await cargarProductos();
        mostrarPanel("panel-productos");
    });
    document.getElementById("btn-productos-todos")?.addEventListener("click", async () => {
        filtroVendedorProductos = "todos";
        const select = document.getElementById("filtro-vendedor-productos");
        if (select) select.value = "todos";
        adminPages.productos.page = 0;
        await cargarProductos();
        mostrarPanel("panel-productos");
    });
    document.getElementById("productos-prev")?.addEventListener("click", async () => { if (adminPages.productos.page > 0) { adminPages.productos.page--; await cargarProductos(); } });
    document.getElementById("productos-next")?.addEventListener("click", async () => { if (adminPages.productos.page + 1 < adminPages.productos.totalPages) { adminPages.productos.page++; await cargarProductos(); } });
    document.getElementById("tabla-productos")?.addEventListener("click", async (e) => {
        const editar = e.target.closest("[data-editar-producto]");
        const eliminar = e.target.closest("[data-eliminar-producto]");
        if (editar) editarProducto(Number(editar.dataset.editarProducto));
        if (eliminar) await eliminarProducto(Number(eliminar.dataset.eliminarProducto));
    });
}

async function cargarProductos() {
    try {
        if (filtroVendedorProductos !== "todos") {
            const lista = await FastMarket.request(filtroVendedorProductos === "general" ? "/productos?incluirInactivos=true" : `/productos?vendedorId=${Number(filtroVendedorProductos)}`, { auth: true });
            productos = Array.isArray(lista) ? lista : [];
            if (filtroVendedorProductos === "general") productos = productos.filter((p) => !p.vendedorId);
            adminPages.productos.totalPages = 1;
            adminPages.productos.page = 0;
        } else {
            const page = await FastMarket.request(`/productos/page?page=${adminPages.productos.page}&size=${adminPages.productos.size}&incluirInactivos=true`, { auth: true });
            productos = Array.isArray(page?.content) ? page.content : [];
            adminPages.productos.totalPages = Math.max(1, Number(page?.totalPages || 1));
            adminPages.productos.page = Math.min(Number(page?.number || 0), adminPages.productos.totalPages - 1);
        }
        actualizarPaginacion("productos");
        actualizarTituloProductos();
        poblarSelectProductosCupon();
        pintarProductos();
    } catch (error) {
        toast(error.message);
    }
}

function actualizarTituloProductos() {
    const titulo = document.getElementById("titulo-productos-admin");
    const subtitulo = document.getElementById("subtitulo-productos-admin");
    if (!titulo || !subtitulo) return;
    if (filtroVendedorProductos === "todos") {
        titulo.textContent = "Catálogo general de productos";
        subtitulo.textContent = "Vista administrativa completa: productos de tienda y productos asignados a vendedores.";
        return;
    }
    if (filtroVendedorProductos === "general") {
        titulo.textContent = "Productos generales de la tienda";
        subtitulo.textContent = "Solo productos propios de FastMarket, sin vendedor asignado.";
        return;
    }
    const vendedor = vendedores.find((v) => Number(v.id) === Number(filtroVendedorProductos));
    titulo.textContent = `Productos de ${vendedor?.nombre || "vendedor"}`;
    subtitulo.textContent = "Vista filtrada: aquí solo aparecen los productos del vendedor seleccionado.";
}

function pintarProductos() {
    const tbody = document.getElementById("tabla-productos");
    if (!tbody) return;

    const texto = document.getElementById("buscar-admin")?.value.toLowerCase().trim() || "";
    const categoria = document.getElementById("filtro-categoria")?.value || "todos";

    const lista = productos.filter((p) => {
        const textoOk = `${p.nombre} ${p.descripcion} ${p.categoria} ${p.tipoProducto || ""} ${JSON.stringify(p.caracteristicas || {})}`.toLowerCase().includes(texto);
        const catOk = categoria === "todos" || p.categoria === categoria;
        return textoOk && catOk;
    });

    tbody.innerHTML = lista.length ? "" : `<tr><td colspan="8">No hay productos.</td></tr>`;

    lista.forEach((p) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><img src="${FastMarket.escapeHTML(p.imagen || "img/logo.png")}" class="img-tabla" alt="${FastMarket.escapeHTML(p.nombre)}" onerror="this.src='img/logo.png'"></td>
            <td><strong>${FastMarket.escapeHTML(p.nombre)}</strong><br><small>${FastMarket.escapeHTML(detalleCortoProducto(p))}</small></td>
            <td>${formatearCategoria(p.categoria)}</td>
            <td><strong>${FastMarket.money(p.precio)}</strong>${p.precioAntes ? `<br><small>Antes: ${FastMarket.money(p.precioAntes)}</small>` : ""}</td>
            <td>${stockBadge(p.stock)}</td>
            <td>
                <span class="${p.activo === false ? "estado-cancelado" : (p.oferta ? "estado-oferta" : "estado-normal")}">${p.activo === false ? "Inactivo" : (p.oferta ? "Oferta" : "Normal")}</span>
                ${p.destacado ? `<br><small>Destacado</small>` : ""}
            </td>
            <td>${FastMarket.escapeHTML(p.vendedorNombre || "Tienda")}</td>
            <td>
                <button class="btn-editar" data-editar-producto="${p.id}">${p.activo === false ? "Reactivar" : "Editar"}</button>
                <button class="btn-eliminar" data-eliminar-producto="${p.id}">Eliminar</button>
            </td>`;
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

    const id = value("producto-id");
    const imagenesProducto = obtenerImagenesFormulario("imagenes-producto-valor", value("imagen-producto-valor"));
    const caracteristicas = editorCaracteristicasProducto?.getCaracteristicas() || {};
    const compatibilidad = editorCaracteristicasProducto?.getCamposCompatibilidad() || {};
    const payload = {
        nombre: value("nombre"),
        categoria: value("categoria"),
        tipoProducto: editorCaracteristicasProducto?.getTipo() || "",
        precio: Number(value("precio")),
        precioAntes: value("precioAntes") ? Number(value("precioAntes")) : null,
        stock: Number(value("stock")),
        imagenes: imagenesProducto,
        imagen: imagenesProducto[0] || "img/logo.png",
        descripcion: value("descripcion"),
        caracteristicas,
        ...compatibilidad,
        detallesAdicionales: value("detalles-adicionales"),
        oferta: checked("oferta"),
        destacado: checked("destacado"),
        vendedorId: value("producto-vendedor") ? Number(value("producto-vendedor")) : null
    };

    if (!payload.nombre || !payload.categoria || payload.precio <= 0 || payload.stock < 0) {
        toast("Completa los datos del producto correctamente.");
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
        await cargarIndices();
    } catch (error) {
        toast(error.message);
    }
}

function editarProducto(id) {
    const p = productos.find((x) => Number(x.id) === Number(id));
    if (!p) return;

    setValue("producto-id", p.id);
    setValue("nombre", p.nombre);
    setValue("categoria", p.categoria);
    setValue("precio", p.precio);
    setValue("precioAntes", p.precioAntes || "");
    setValue("stock", p.stock);
    const imagenes = obtenerImagenesProducto(p);
    setValue("imagen-producto-valor", imagenes[0] || p.imagen || "");
    setValue("imagenes-producto-valor", JSON.stringify(imagenes));
    setValue("descripcion", p.descripcion || "");
    editorCaracteristicasProducto?.setData(p);
    setValue("detalles-adicionales", p.detallesAdicionales || "");
    setChecked("oferta", !!p.oferta);
    setChecked("destacado", !!p.destacado);
    setValue("producto-vendedor", p.vendedorId || "");
    setText("titulo-form", "Editar producto");

    pintarPreviewImagenes("preview-producto", "preview-producto-lista", imagenes);

    mostrarPanel("panel-productos");
}

async function eliminarProducto(id) {
    if (!(await FastMarket.confirmAction("¿Seguro que deseas eliminar este producto del catálogo?"))) return;
    try {
        await FastMarket.request(`/productos/${id}`, { method: "DELETE", auth: true });
        toast("Producto desactivado. Los pedidos antiguos se conservan.");
        await cargarProductos();
        await cargarIndices();
    } catch (error) {
        toast(error.message);
    }
}

function limpiarProducto() {
    document.getElementById("form-producto")?.reset();
    setValue("producto-id", "");
    setValue("imagen-producto-valor", "");
    setValue("imagenes-producto-valor", "");
    setText("titulo-form", "Agregar producto");
    editorCaracteristicasProducto?.reset();
    document.getElementById("preview-producto")?.classList.add("oculto");
    const lista = document.getElementById("preview-producto-lista");
    if (lista) lista.innerHTML = "";
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

function cargarImagenesProducto(e) {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    if (files.some((file) => !file.type.startsWith("image/"))) {
        toast("Selecciona solo imágenes válidas.");
        return;
    }

    Promise.all(files.map(leerImagenReducida)).then((nuevasImagenes) => {
        const actuales = obtenerImagenesFormulario("imagenes-producto-valor", value("imagen-producto-valor"))
            .filter((img) => img !== "img/logo.png");
        const imagenes = unirImagenes(actuales, nuevasImagenes).slice(0, 8);
        setValue("imagenes-producto-valor", JSON.stringify(imagenes));
        setValue("imagen-producto-valor", imagenes[0] || "img/logo.png");
        pintarPreviewImagenes("preview-producto", "preview-producto-lista", imagenes);
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

function activarBanners() {
    document.getElementById("form-banner")?.addEventListener("submit", guardarBanner);
    document.getElementById("btn-limpiar-banner")?.addEventListener("click", limpiarBanner);
    document.getElementById("banner-imagen")?.addEventListener("change", (e) => cargarImagen(e, "banner-imagen-valor", "preview-banner", "preview-banner-img"));
    document.getElementById("buscar-banner")?.addEventListener("input", pintarBanners);
    document.getElementById("lista-banners")?.addEventListener("click", async (e) => {
        const editar = e.target.closest("[data-editar-banner]");
        const eliminar = e.target.closest("[data-eliminar-banner]");
        if (editar) editarBanner(Number(editar.dataset.editarBanner));
        if (eliminar) await eliminarBanner(Number(eliminar.dataset.eliminarBanner));
    });
}

async function cargarBanners() {
    try {
        banners = await FastMarket.request("/banners");
        pintarBanners();
    } catch (error) {
        toast(error.message);
    }
}

function pintarBanners() {
    const contenedor = document.getElementById("lista-banners");
    if (!contenedor) return;

    const texto = document.getElementById("buscar-banner")?.value.toLowerCase().trim() || "";
    const lista = banners.filter((b) => `${b.titulo} ${b.descripcion}`.toLowerCase().includes(texto));

    contenedor.innerHTML = lista.length ? "" : `<div class="banner-card-admin"><h3>No hay banners.</h3></div>`;

    lista.forEach((b) => {
        const card = document.createElement("article");
        card.className = "banner-card-admin";
        card.innerHTML = `
            <img src="${FastMarket.escapeHTML(b.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(b.titulo)}" onerror="this.src='img/logo.png'">
            <div class="banner-card-info">
                <h3>${FastMarket.escapeHTML(b.titulo)}</h3>
                <p>${FastMarket.escapeHTML(b.descripcion || "")}</p>
                <span class="${b.activo ? "estado-banner-activo" : "estado-banner-inactivo"}">${b.activo ? "Activo" : "Inactivo"}</span>
                <div class="banner-card-acciones">
                    <button class="btn-editar-banner" data-editar-banner="${b.id}">Editar</button>
                    <button class="btn-eliminar-banner" data-eliminar-banner="${b.id}">Eliminar</button>
                </div>
            </div>`;
        contenedor.appendChild(card);
    });
}

async function guardarBanner(e) {
    e.preventDefault();

    const id = value("banner-id");
    const payload = {
        titulo: value("banner-titulo"),
        descripcion: value("banner-descripcion"),
        imagen: value("banner-imagen-valor") || "img/logo.png",
        activo: checked("banner-activo")
    };

    if (!payload.titulo || !payload.descripcion) {
        toast("Completa título y descripción del banner.");
        return;
    }

    try {
        await FastMarket.request(id ? `/banners/${id}` : "/banners", {
            method: id ? "PUT" : "POST",
            body: payload,
            auth: true
        });
        toast(id ? "Banner actualizado." : "Banner creado.");
        limpiarBanner();
        await cargarBanners();
    } catch (error) {
        toast(error.message);
    }
}

function editarBanner(id) {
    const b = banners.find((x) => Number(x.id) === Number(id));
    if (!b) return;

    setValue("banner-id", b.id);
    setValue("banner-titulo", b.titulo);
    setValue("banner-descripcion", b.descripcion || "");
    setValue("banner-imagen-valor", b.imagen || "");
    setChecked("banner-activo", !!b.activo);
    setText("titulo-form-banner", "Editar banner");

    const preview = document.getElementById("preview-banner");
    const img = document.getElementById("preview-banner-img");
    if (img) img.src = b.imagen || "img/logo.png";
    preview?.classList.remove("oculto");

    mostrarPanel("panel-banners");
}

async function eliminarBanner(id) {
    if (!(await FastMarket.confirmAction("¿Seguro que deseas eliminar este banner?"))) return;
    try {
        await FastMarket.request(`/banners/${id}`, { method: "DELETE", auth: true });
        toast("Banner eliminado.");
        await cargarBanners();
    } catch (error) {
        toast(error.message);
    }
}

function limpiarBanner() {
    document.getElementById("form-banner")?.reset();
    setValue("banner-id", "");
    setValue("banner-imagen-valor", "");
    setText("titulo-form-banner", "Agregar banner");
    document.getElementById("preview-banner")?.classList.add("oculto");
}


function activarPedidos() {
    document.getElementById("filtro-vendedor-pedidos")?.addEventListener("change", async (e) => {
        filtroVendedorPedidos = e.target.value || "todos";
        adminPages.pedidos.page = 0;
        await cargarPedidos();
    });
    document.getElementById("tabla-pedidos")?.addEventListener("change", async (e) => {
        const select = e.target.closest("[data-estado-pedido]");
        if (!select) return;
        await actualizarEstadoPedido(Number(select.dataset.estadoPedido), select.value);
    });
    document.getElementById("tabla-pedidos")?.addEventListener("click", async (e) => {
        const historial = e.target.closest("[data-historial-pedido]");
        if (historial) await verHistorialPedido(Number(historial.dataset.historialPedido));
    });
    document.getElementById("pedidos-prev")?.addEventListener("click", async () => { if (adminPages.pedidos.page > 0) { adminPages.pedidos.page--; await cargarPedidos(); } });
    document.getElementById("pedidos-next")?.addEventListener("click", async () => { if (adminPages.pedidos.page + 1 < adminPages.pedidos.totalPages) { adminPages.pedidos.page++; await cargarPedidos(); } });
}

async function cargarPedidos() {
    try {
        if (filtroVendedorPedidos !== "todos") {
            pedidos = await FastMarket.request(`/pedidos/vendedor/${Number(filtroVendedorPedidos)}`, { auth: true });
            adminPages.pedidos.totalPages = 1;
            adminPages.pedidos.page = 0;
        } else {
            const page = await FastMarket.request(`/pedidos/page?page=${adminPages.pedidos.page}&size=${adminPages.pedidos.size}`, { auth: true });
            pedidos = Array.isArray(page?.content) ? page.content : [];
            adminPages.pedidos.totalPages = Math.max(1, Number(page?.totalPages || 1));
            adminPages.pedidos.page = Math.min(Number(page?.number || 0), adminPages.pedidos.totalPages - 1);
        }
        actualizarPaginacion("pedidos");
        actualizarTituloPedidos();
        pintarPedidos();
    } catch (error) {
        toast(error.message);
    }
}

function actualizarTituloPedidos() {
    const titulo = document.getElementById("titulo-pedidos-admin");
    const subtitulo = document.getElementById("subtitulo-pedidos-admin");
    if (!titulo || !subtitulo) return;
    if (filtroVendedorPedidos === "todos") {
        titulo.textContent = "Pedidos y ventas generales";
        subtitulo.textContent = "Vista administrativa completa de pedidos registrados.";
        return;
    }
    const vendedor = vendedores.find((v) => Number(v.id) === Number(filtroVendedorPedidos));
    titulo.textContent = `Pedidos de ${vendedor?.nombre || "vendedor"}`;
    subtitulo.textContent = "Vista filtrada: pedidos que contienen productos de este vendedor.";
}

function pintarPedidos() {
    const tbody = document.getElementById("tabla-pedidos");
    if (!tbody) return;

    tbody.innerHTML = pedidos.length ? "" : `<tr><td colspan="8">No hay pedidos registrados.</td></tr>`;

    pedidos.forEach((p) => {
        const productosTexto = (p.items || []).map((i) => `${i.productoNombre} x${i.cantidad}`).join(", ");
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${FastMarket.escapeHTML(p.codigo)}</strong><br><small>${fecha(p.fecha)}</small></td>
            <td>${FastMarket.escapeHTML(p.usuarioNombre || "")}</td>
            <td>${FastMarket.escapeHTML(productosTexto)}</td>
            <td>${FastMarket.money(p.total)}</td>
            <td>${p.descuento ? FastMarket.money(p.descuento) : "-"}${p.cuponCodigo ? `<br><small>${FastMarket.escapeHTML(p.cuponCodigo)}</small>` : ""}</td>
            <td>${FastMarket.estadoTexto(p.estado)}</td>
            <td>
                <select data-estado-pedido="${p.id}">
                    ${["PENDIENTE","CONFIRMADO","PREPARANDO","CAMINO","ENTREGADO","CANCELADO"].map((estado) => `<option value="${estado}" ${FastMarket.normalizarEstado(p.estado) === estado ? "selected" : ""}>${FastMarket.estadoTexto(estado)}</option>`).join("")}
                </select>
            </td>
            <td><button type="button" class="btn-secundario" data-historial-pedido="${p.id}">Ver</button></td>`;
        tbody.appendChild(tr);
    });
}

async function actualizarEstadoPedido(id, estado) {
    try {
        await FastMarket.request(`/pedidos/${id}/estado?estado=${estado}`, {
            method: "PUT",
            auth: true
        });
        toast("Estado del pedido actualizado.");
        await cargarPedidos();
    } catch (error) {
        toast(error.message);
    }
}

async function verHistorialPedido(id) {
    const panel = document.getElementById("historial-pedido-panel");
    if (!panel) return;
    try {
        const historial = await FastMarket.request(`/pedidos/${id}/historial`, { auth: true });
        panel.classList.remove("oculto");
        panel.innerHTML = `<h3>Historial del pedido</h3>${pintarListaHistorial(historial, "pedido")}`;
    } catch (error) {
        toast(error.message);
    }
}

function pintarListaHistorial(items, tipo) {
    if (!items || !items.length) return `<p>No hay historial registrado.</p>`;
    const lis = items.map((h) => {
        if (tipo === "pedido") {
            return `<li><strong>${fecha(h.fecha)}</strong> — ${FastMarket.estadoTexto(h.estadoAnterior || "CREADO")} → ${FastMarket.estadoTexto(h.estadoNuevo)} por ${FastMarket.escapeHTML(h.actorNombre || "Sistema")}<br><small>${FastMarket.escapeHTML(h.motivo || "")}</small></li>`;
        }
        return `<li><strong>${fecha(h.fecha)}</strong> — ${FastMarket.escapeHTML(h.usuarioNombre || "Cliente")} usó ${FastMarket.escapeHTML(h.codigo || "")} en ${FastMarket.escapeHTML(h.pedidoCodigo || "pedido")} y ahorró ${FastMarket.money(h.descuentoAplicado)}</li>`;
    }).join("");
    return `<ul>${lis}</ul>`;
}

function activarUsuarios() {
    document.getElementById("form-usuario-admin")?.addEventListener("submit", guardarUsuarioAdmin);
    document.getElementById("btn-limpiar-usuario")?.addEventListener("click", limpiarUsuarioAdmin);
    document.getElementById("buscar-usuario")?.addEventListener("input", pintarUsuarios);
    document.getElementById("filtro-rol-usuario")?.addEventListener("change", pintarUsuarios);
    document.getElementById("tabla-usuarios")?.addEventListener("click", (e) => {
        const editar = e.target.closest("[data-editar-usuario]");
        if (editar) editarUsuarioAdmin(Number(editar.dataset.editarUsuario));
    });
}

async function cargarUsuarios() {
    if (usuarioActual?.rol !== "ADMIN") { usuarios = []; pintarUsuarios(); return; }
    try {
        usuarios = await FastMarket.request("/usuarios", { auth: true });
        pintarUsuarios();
    } catch (error) {
        toast(error.message);
    }
}

function pintarUsuarios() {
    const tbody = document.getElementById("tabla-usuarios");
    if (!tbody) return;

    const texto = document.getElementById("buscar-usuario")?.value.toLowerCase().trim() || "";
    const rol = document.getElementById("filtro-rol-usuario")?.value || "todos";

    const lista = usuarios.filter((u) => {
        const coincideTexto = `${u.nombre} ${u.correo} ${u.rol} ${u.estado}`.toLowerCase().includes(texto);
        const coincideRol = rol === "todos" || u.rol === rol;
        return coincideTexto && coincideRol;
    });

    tbody.innerHTML = lista.length ? "" : `<tr><td colspan="6">No hay usuarios.</td></tr>`;

    lista.forEach((u) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${FastMarket.escapeHTML(u.nombre || "")}</td>
            <td>${FastMarket.escapeHTML(u.correo || "")}</td>
            <td><span class="${u.rol === "ADMIN" ? "estado-oferta" : u.rol === "VENDEDOR" ? "estado-activo" : "estado-normal"}">${u.rol === "ADMIN" ? "Administrador" : u.rol === "VENDEDOR" ? "Vendedor" : "Cliente"}</span></td>
            <td><span class="${u.estado === "ACTIVO" ? "estado-activo" : "estado-inactivo"}">${FastMarket.escapeHTML(u.estado || "")}</span></td>
            <td>${FastMarket.escapeHTML(u.telefono || "No registrado")}</td>
            <td><button class="btn-editar" data-editar-usuario="${u.id}">Editar</button></td>`;
        tbody.appendChild(tr);
    });
}

async function guardarUsuarioAdmin(e) {
    e.preventDefault();

    const id = value("usuario-admin-id");
    const password = value("usuario-password");
    const payload = {
        nombre: value("usuario-nombre"),
        telefono: value("usuario-telefono"),
        documento: value("usuario-documento"),
        rol: value("usuario-rol") || "CLIENTE",
        estado: value("usuario-estado") || "ACTIVO"
    };

    if (!payload.nombre) {
        toast("Ingresa el nombre del usuario.");
        return;
    }

    if (id) {
        if (password) {
            if (!validarPasswordSegura(password)) {
                toast(mensajePasswordSegura());
                return;
            }
            payload.passwordNueva = password;
        }
    } else {
        payload.correo = value("usuario-correo").toLowerCase();
        payload.password = password;
        if (!payload.correo || !payload.correo.includes("@")) {
            toast("Ingresa un correo válido.");
            return;
        }
        if (!validarPasswordSegura(payload.password)) {
            toast(mensajePasswordSegura());
            return;
        }
    }

    try {
        await FastMarket.request(id ? `/usuarios/${id}/gestion` : "/usuarios", {
            method: id ? "PUT" : "POST",
            body: payload,
            auth: true
        });
        toast(id ? "Usuario actualizado." : "Usuario creado.");
        limpiarUsuarioAdmin();
        await cargarUsuarios();
    } catch (error) {
        toast(error.message);
    }
}

function editarUsuarioAdmin(id) {
    const usuario = usuarios.find((u) => Number(u.id) === Number(id));
    if (!usuario) return;

    setValue("usuario-admin-id", usuario.id);
    setValue("usuario-nombre", usuario.nombre || "");
    setValue("usuario-correo", usuario.correo || "");
    setValue("usuario-password", "");
    setValue("usuario-telefono", usuario.telefono || "");
    setValue("usuario-documento", usuario.documento || "");
    setValue("usuario-rol", usuario.rol || "CLIENTE");
    setValue("usuario-estado", usuario.estado || "ACTIVO");
    setText("titulo-form-usuario", "Editar usuario");

    const correo = document.getElementById("usuario-correo");
    if (correo) correo.disabled = true;

    mostrarPanel("panel-usuarios");
}


async function cargarVendedores() {
    if (usuarioActual?.rol !== "ADMIN") {
        vendedores = usuarioActual?.rol === "VENDEDOR" ? [usuarioActual] : [];
        llenarSelectVendedores();
        return;
    }
    try {
        vendedores = await FastMarket.request("/admin/vendedores", { auth: true });
        llenarSelectVendedores();
        pintarVendedoresAdmin();
    } catch (error) {
        vendedores = usuarios.filter((u) => u.rol === "VENDEDOR");
        llenarSelectVendedores();
    }
}

function llenarSelectVendedores() {
    const filtroProductos = document.getElementById("filtro-vendedor-productos");
    if (filtroProductos) {
        const actual = filtroProductos.value || filtroVendedorProductos;
        filtroProductos.innerHTML = `<option value="todos">Todos los productos</option><option value="general">Solo productos generales</option>` + vendedores.map((v) => `<option value="${v.id}">${FastMarket.escapeHTML(v.nombre)} - ${FastMarket.escapeHTML(v.correo || "")}</option>`).join("");
        filtroProductos.value = actual || "todos";
    }
    const filtroPedidos = document.getElementById("filtro-vendedor-pedidos");
    if (filtroPedidos) {
        const actual = filtroPedidos.value || filtroVendedorPedidos;
        filtroPedidos.innerHTML = `<option value="todos">Todos los pedidos</option>` + vendedores.map((v) => `<option value="${v.id}">${FastMarket.escapeHTML(v.nombre)} - ${FastMarket.escapeHTML(v.correo || "")}</option>`).join("");
        filtroPedidos.value = actual || "todos";
    }
    const selects = [document.getElementById("producto-vendedor"), document.getElementById("cupon-vendedor")].filter(Boolean);
    selects.forEach((select) => {
        const actual = select.value;
        const primera = select.id === "producto-vendedor" ? `<option value="">Producto general de tienda</option>` : `<option value="">Seleccionar vendedor</option>`;
        select.innerHTML = primera + vendedores.map((v) => `<option value="${v.id}">${FastMarket.escapeHTML(v.nombre)} - ${FastMarket.escapeHTML(v.correo || "")}</option>`).join("");
        select.value = actual;
    });
}

function pintarVendedoresAdmin() {
    const cont = document.getElementById("lista-vendedores-admin");
    if (!cont) return;
    const lista = vendedores.length ? vendedores : usuarios.filter((u) => u.rol === "VENDEDOR");
    cont.innerHTML = lista.length ? "" : `<div class="banner-card-admin"><h3>No hay vendedores registrados.</h3><p>Crea un usuario con rol VENDEDOR desde Usuarios y administradores.</p></div>`;
    lista.forEach((v) => {
        const prods = productos.filter((p) => Number(p.vendedorId) === Number(v.id));
        const pedidosVend = pedidos.filter((p) => (p.items || []).some((i) => prods.some((prod) => prod.id === i.productoId)));
        const card = document.createElement("article");
        card.className = "vendedor-card-admin";
        const nombresProductos = prods.slice(0, 4).map((p) => `• ${FastMarket.escapeHTML(p.nombre)}`).join("<br>") || "Sin productos registrados";
        const codigosPedidos = pedidosVend.slice(0, 4).map((p) => `• ${FastMarket.escapeHTML(p.codigo)} - ${FastMarket.money(p.total)}`).join("<br>") || "Sin pedidos registrados";
        card.innerHTML = `
            <h3>${FastMarket.escapeHTML(v.nombre)}</h3>
            <p>${FastMarket.escapeHTML(v.correo || "")}</p>
            <div class="vendedor-metricas">
                <span>📦 ${prods.length} productos</span>
                <span>🧾 ${pedidosVend.length} pedidos</span>
                <span>📞 ${FastMarket.escapeHTML(v.telefono || "Sin teléfono")}</span>
            </div>
            <div class="vendedor-detalle-card">
                <strong>Productos</strong><br>${nombresProductos}
                <br><br><strong>Pedidos</strong><br>${codigosPedidos}
            </div>
            <div class="banner-card-acciones">
                <button class="btn-editar" data-ver-productos-vendedor="${v.id}">Ver solo sus productos</button>
                <button class="btn-editar" data-ver-pedidos-vendedor="${v.id}">Ver solo sus pedidos</button>
            </div>`;
        cont.appendChild(card);
    });
}

function configurarPermisosPanel() {
    const esVendedor = usuarioActual?.rol === "VENDEDOR";
    document.querySelectorAll(".solo-admin").forEach((el) => el.classList.toggle("oculto", esVendedor));
    const grupoVendedorProducto = document.getElementById("grupo-vendedor-producto");
    if (grupoVendedorProducto) grupoVendedorProducto.classList.toggle("oculto", esVendedor);
    const tipoCupon = document.getElementById("cupon-tipo");
    if (tipoCupon && esVendedor) {
        tipoCupon.value = "VENDEDOR";
        tipoCupon.disabled = true;
        document.getElementById("grupo-cupon-vendedor")?.classList.add("oculto");
    }
}


function activarCupones() {
    document.getElementById("form-cupon")?.addEventListener("submit", guardarCupon);
    document.getElementById("btn-limpiar-cupon")?.addEventListener("click", limpiarCupon);
    document.getElementById("buscar-cupon")?.addEventListener("input", pintarCupones);
    document.getElementById("cupon-tipo")?.addEventListener("change", actualizarVisibilidadCuponVendedor);
    document.getElementById("tabla-cupones")?.addEventListener("click", async (e) => {
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
    } catch (error) {
        cupones = [];
    }
}

function pintarCupones() {
    const tbody = document.getElementById("tabla-cupones");
    if (!tbody) return;
    const texto = document.getElementById("buscar-cupon")?.value.toLowerCase().trim() || "";
    const lista = cupones.filter((c) => `${c.codigo} ${c.descripcion} ${c.tipo} ${c.vendedorNombre || ""}`.toLowerCase().includes(texto));
    tbody.innerHTML = lista.length ? "" : `<tr><td colspan="7">No hay cupones.</td></tr>`;
    lista.forEach((c) => {
        const descuento = `${Number(c.porcentaje || 0) > 0 ? `${c.porcentaje}%` : ""}${Number(c.montoFijo || 0) > 0 ? ` S/ ${Number(c.montoFijo).toFixed(2)}` : ""}`.trim();
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${FastMarket.escapeHTML(c.codigo)}</strong><br><small>${FastMarket.escapeHTML(c.descripcion || "")}</small></td>
            <td>${descripcionTipoCupon(c)}</td>
            <td>${descuento || "-"}<br><small>Mín: ${FastMarket.money(c.montoMinimo || 0)}</small></td>
            <td>${vigenciaCupon(c)}</td>
            <td>${c.usosActuales || 0}${c.usosMaximos ? `/${c.usosMaximos}` : ""}</td>
            <td><span class="${c.activo ? "estado-activo" : "estado-inactivo"}">${c.activo ? "Activo" : "Inactivo"}</span></td>
            <td><button class="btn-editar" data-editar-cupon="${c.id}">Editar</button><button class="btn-secundario" data-usos-cupon="${c.id}">Usos</button><button class="btn-eliminar" data-eliminar-cupon="${c.id}">Eliminar</button></td>`;
        tbody.appendChild(tr);
    });
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

function descripcionTipoCupon(c) {
    if (c.tipo === "VENDEDOR") return `Vendedor<br><small>${FastMarket.escapeHTML(c.vendedorNombre || "")}</small>`;
    if (c.tipo === "CATEGORIA") return `Categoría<br><small>${FastMarket.escapeHTML(c.categoriaObjetivo || "")}</small>`;
    if (c.tipo === "PRODUCTO") return `Producto<br><small>${FastMarket.escapeHTML(c.productoObjetivoNombre || "")}</small>`;
    return "Global";
}

function actualizarVisibilidadCuponVendedor() {
    const tipo = value("cupon-tipo");
    document.getElementById("grupo-cupon-vendedor")?.classList.toggle("oculto", tipo !== "VENDEDOR" || usuarioActual?.rol === "VENDEDOR");
    document.getElementById("grupo-cupon-categoria")?.classList.toggle("oculto", tipo !== "CATEGORIA");
    document.getElementById("grupo-cupon-producto")?.classList.toggle("oculto", tipo !== "PRODUCTO");
}

function poblarSelectProductosCupon() {
    const select = document.getElementById("cupon-producto");
    if (!select) return;
    const actual = select.value;
    select.innerHTML = `<option value="">Seleccionar producto</option>` + productos.map((p) => `<option value="${p.id}">${FastMarket.escapeHTML(p.nombre || "Producto")}</option>`).join("");
    if (actual) select.value = actual;
}


async function guardarCupon(e) {
    e.preventDefault();
    const id = value("cupon-id");
    const payload = {
        codigo: value("cupon-codigo"),
        descripcion: value("cupon-descripcion"),
        tipo: usuarioActual?.rol === "VENDEDOR" ? "VENDEDOR" : value("cupon-tipo"),
        vendedorId: value("cupon-vendedor") ? Number(value("cupon-vendedor")) : null,
        categoriaObjetivo: value("cupon-categoria") || null,
        productoObjetivoId: value("cupon-producto") ? Number(value("cupon-producto")) : null,
        porcentaje: Number(value("cupon-porcentaje") || 0),
        montoFijo: Number(value("cupon-monto") || 0),
        montoMinimo: Number(value("cupon-minimo") || 0),
        usosMaximos: value("cupon-usos") ? Number(value("cupon-usos")) : null,
        fechaInicio: value("cupon-inicio") || null,
        fechaFin: value("cupon-fin") || null,
        activo: checked("cupon-activo")
    };
    if (!payload.codigo) return toast("Ingresa el código del cupón.");
    if (payload.porcentaje <= 0 && payload.montoFijo <= 0) return toast("Ingresa porcentaje o monto fijo de descuento.");
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
    setValue("cupon-id", c.id);
    setValue("cupon-codigo", c.codigo || "");
    setValue("cupon-descripcion", c.descripcion || "");
    setValue("cupon-tipo", c.tipo || "GLOBAL");
    setValue("cupon-vendedor", c.vendedorId || "");
    setValue("cupon-categoria", c.categoriaObjetivo || "moda");
    setValue("cupon-producto", c.productoObjetivoId || "");
    setValue("cupon-porcentaje", c.porcentaje || "");
    setValue("cupon-monto", c.montoFijo || "");
    setValue("cupon-minimo", c.montoMinimo || "");
    setValue("cupon-usos", c.usosMaximos || "");
    setValue("cupon-inicio", toDatetimeLocal(c.fechaInicio));
    setValue("cupon-fin", toDatetimeLocal(c.fechaFin));
    setChecked("cupon-activo", !!c.activo);
    setText("titulo-form-cupon", "Editar cupón");
    actualizarVisibilidadCuponVendedor();
    mostrarPanel("panel-cupones");
}

async function verUsosCupon(id) {
    const panel = document.getElementById("historial-cupon-panel");
    if (!panel) return;
    try {
        const usos = await FastMarket.request(`/cupones/${id}/usos`, { auth: true });
        panel.classList.remove("oculto");
        panel.innerHTML = `<h3>Historial de usos del cupón</h3>${pintarListaHistorial(usos, "cupon")}`;
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
    document.getElementById("form-cupon")?.reset();
    setValue("cupon-id", "");
    setValue("cupon-tipo", usuarioActual?.rol === "VENDEDOR" ? "VENDEDOR" : "GLOBAL");
    setValue("cupon-inicio", "");
    setValue("cupon-fin", "");
    setChecked("cupon-activo", true);
    setText("titulo-form-cupon", "Agregar cupón");
    actualizarVisibilidadCuponVendedor();
}

function limpiarUsuarioAdmin() {
    document.getElementById("form-usuario-admin")?.reset();
    setValue("usuario-admin-id", "");
    setValue("usuario-rol", "CLIENTE");
    setValue("usuario-estado", "ACTIVO");
    setText("titulo-form-usuario", "Agregar usuario");

    const correo = document.getElementById("usuario-correo");
    if (correo) correo.disabled = false;
}

async function cargarIndices() {
    try {
        const i = await FastMarket.request("/admin/indices", { auth: true });
        setText("total-productos", i.totalProductos);
        setText("total-ofertas", i.totalOfertas);
        setText("total-stock", i.totalStock);
        setText("total-pedidos", i.totalPedidos);
        setText("total-vendedores", i.totalVendedores ?? vendedores.length);
    } catch {
        setText("total-productos", productos.length);
        setText("total-ofertas", productos.filter((p) => p.oferta).length);
        setText("total-stock", productos.reduce((s, p) => s + Number(p.stock || 0), 0));
        setText("total-pedidos", pedidos.length);
    }
}


function activarIndex() {
    document.getElementById("form-index")?.addEventListener("submit", guardarIndex);
    document.getElementById("btn-limpiar-index")?.addEventListener("click", limpiarIndex);
    document.getElementById("buscar-index")?.addEventListener("input", pintarIndex);
    document.getElementById("tabla-index")?.addEventListener("click", async (e) => {
        const editar = e.target.closest("[data-editar-index]");
        const eliminar = e.target.closest("[data-eliminar-index]");
        if (editar) editarIndex(Number(editar.dataset.editarIndex));
        if (eliminar) await eliminarIndex(Number(eliminar.dataset.eliminarIndex));
    });
}

async function cargarIndexContenidos() {
    try {
        const contenidos = await FastMarket.request("/index-contenido");
        indexContenidos = contenidos.filter((c) => TIPOS_INDEX_PERMITIDOS.includes(c.tipo));
        pintarIndex();
    } catch (error) {
        toast(error.message);
    }
}

function pintarIndex() {
    const tbody = document.getElementById("tabla-index");
    if (!tbody) return;

    const texto = document.getElementById("buscar-index")?.value.toLowerCase().trim() || "";
    const lista = indexContenidos.filter((c) => TIPOS_INDEX_PERMITIDOS.includes(c.tipo) && `${c.tipo} ${c.clave} ${c.titulo} ${c.descripcion}`.toLowerCase().includes(texto));

    tbody.innerHTML = lista.length ? "" : `<tr><td colspan="5">No hay contenido.</td></tr>`;

    lista.forEach((c) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><span class="estado-oferta">${formatearTipoIndex(c.tipo)}</span></td>
            <td>${FastMarket.escapeHTML(c.clave)}</td>
            <td><strong>${FastMarket.escapeHTML(c.titulo)}</strong><br><small>${FastMarket.escapeHTML(c.descripcion || "")}</small></td>
            <td><span class="${c.activo ? "estado-activo" : "estado-inactivo"}">${c.activo ? "Activo" : "Inactivo"}</span></td>
            <td>
                <button class="btn-editar" data-editar-index="${c.id}">Editar</button>
                <button class="btn-eliminar" data-eliminar-index="${c.id}">Eliminar</button>
            </td>`;
        tbody.appendChild(tr);
    });
}

async function guardarIndex(e) {
    e.preventDefault();

    const id = value("index-id");
    const payload = {
        tipo: value("index-tipo"),
        clave: value("index-clave"),
        titulo: value("index-titulo"),
        descripcion: value("index-descripcion"),
        imagen: value("index-imagen"),
        enlace: value("index-enlace"),
        orden: Number(value("index-orden") || 1),
        activo: checked("index-activo")
    };

    if (!TIPOS_INDEX_PERMITIDOS.includes(payload.tipo)) {
        toast("Solo puedes editar Destacados, Promociones, Opiniones y Ayuda.");
        return;
    }

    if (!payload.tipo || !payload.clave || !payload.titulo) {
        toast("Completa sección, clave y título.");
        return;
    }

    try {
        await FastMarket.request(id ? `/index-contenido/${id}` : "/index-contenido", {
            method: id ? "PUT" : "POST",
            body: payload,
            auth: true
        });
        toast(id ? "Contenido actualizado." : "Contenido creado.");
        limpiarIndex();
        await cargarIndexContenidos();
    } catch (error) {
        toast(error.message);
    }
}

function editarIndex(id) {
    const c = indexContenidos.find((x) => Number(x.id) === Number(id));
    if (!c) return;

    setValue("index-id", c.id);
    setValue("index-tipo", c.tipo);
    setValue("index-clave", c.clave);
    setValue("index-titulo", c.titulo);
    setValue("index-descripcion", c.descripcion || "");
    setValue("index-imagen", c.imagen || "");
    setValue("index-enlace", c.enlace || "");
    setValue("index-orden", c.orden || 1);
    setChecked("index-activo", !!c.activo);
    setText("titulo-form-index", "Editar contenido");
    mostrarPanel("panel-index");
}

async function eliminarIndex(id) {
    if (!(await FastMarket.confirmAction("¿Seguro que deseas eliminar este contenido?"))) return;
    try {
        await FastMarket.request(`/index-contenido/${id}`, { method: "DELETE", auth: true });
        toast("Contenido eliminado.");
        await cargarIndexContenidos();
    } catch (error) {
        toast(error.message);
    }
}

function limpiarIndex() {
    document.getElementById("form-index")?.reset();
    setValue("index-id", "");
    setValue("index-tipo", "destacado");
    setChecked("index-activo", true);
    setText("titulo-form-index", "Agregar contenido");
}


function activarReportes() {
    document.getElementById("reporte-periodo")?.addEventListener("change", () => configurarPeriodoReporte(true));
    document.getElementById("btn-actualizar-reporte")?.addEventListener("click", cargarReportes);
    document.getElementById("btn-exportar-reporte")?.addEventListener("click", exportarReporteCSV);
    document.getElementById("rep-reclamos-tabla")?.addEventListener("click", async (e) => {
        const btn = e.target.closest("[data-guardar-reclamo]");
        if (btn) await guardarReclamoAdmin(Number(btn.dataset.guardarReclamo));
    });
    configurarPeriodoReporte(false);
}

function configurarPeriodoReporte(cargar = false) {
    const periodo = value("reporte-periodo") || "30";
    const desde = document.getElementById("reporte-desde");
    const hasta = document.getElementById("reporte-hasta");
    if (!desde || !hasta) return;

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    let inicio = new Date(hoy);
    let fin = new Date(hoy);
    const personalizado = periodo === "personalizado";

    if (periodo === "7") inicio.setDate(inicio.getDate() - 6);
    else if (periodo === "30") inicio.setDate(inicio.getDate() - 29);
    else if (periodo === "90") inicio.setDate(inicio.getDate() - 89);
    else if (periodo === "mes") inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    else if (periodo === "anio") inicio = new Date(hoy.getFullYear(), 0, 1);

    desde.disabled = !personalizado;
    hasta.disabled = !personalizado;
    if (!personalizado || !desde.value || !hasta.value) {
        desde.value = fechaISOReporte(inicio);
        hasta.value = fechaISOReporte(fin);
    }
    if (cargar) cargarReportes();
}

async function cargarReportes() {
    const desde = value("reporte-desde");
    const hasta = value("reporte-hasta");
    const estado = document.getElementById("reporte-estado");
    if (!desde || !hasta) {
        if (estado) estado.textContent = "Selecciona las fechas del reporte.";
        return;
    }
    if (desde > hasta) {
        if (estado) {
            estado.textContent = "La fecha inicial no puede ser posterior a la fecha final.";
            estado.className = "reporte-estado error";
        }
        return;
    }

    if (estado) {
        estado.textContent = "Consultando estadísticas...";
        estado.className = "reporte-estado cargando";
    }
    const boton = document.getElementById("btn-actualizar-reporte");
    if (boton) boton.disabled = true;

    try {
        reporteAdmin = await FastMarket.request(`/admin/reportes?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}`, { auth: true });
        pintarReporteAdmin(reporteAdmin);
        if (estado) {
            const generado = reporteAdmin?.periodo?.generadoEn ? fecha(reporteAdmin.periodo.generadoEn) : "ahora";
            estado.textContent = `Periodo: ${formatearFechaCorta(desde)} al ${formatearFechaCorta(hasta)}. Actualizado ${generado}.`;
            estado.className = "reporte-estado correcto";
        }
    } catch (error) {
        reporteAdmin = null;
        if (estado) {
            estado.textContent = error.message;
            estado.className = "reporte-estado error";
        }
        toast(error.message);
    } finally {
        if (boton) boton.disabled = false;
    }
}

function pintarReporteAdmin(data) {
    const r = data?.resumen || {};
    setText("rep-usuarios-total", formatoNumero(r.usuariosTotales));
    setText("rep-usuarios-nuevos", `${formatoNumero(r.usuariosNuevos)} nuevos · ${formatoNumero(r.usuariosActivos)} activos`);
    setText("rep-sesiones", formatoNumero(r.sesionesExitosas));
    setText("rep-sesiones-unicas", `${formatoNumero(r.usuariosUnicosConSesion)} usuarios únicos`);
    setText("rep-sesiones-fallidas", formatoNumero(r.sesionesFallidas));
    setText("rep-ventas", formatoNumero(r.ventasValidas));
    setText("rep-pedidos-total", `${formatoNumero(r.pedidosRegistrados)} pedidos totales`);
    setText("rep-ingresos", FastMarket.money(r.ingresos));
    setText("rep-ticket", `Ticket promedio ${FastMarket.money(r.ticketPromedio)}`);
    setText("rep-unidades", formatoNumero(r.unidadesVendidas));
    setText("rep-clientes", `${formatoNumero(r.clientesCompradores)} compradores`);
    setText("rep-entregados", formatoNumero(r.pedidosEntregados));
    setText("rep-tasa-entrega", `${formatoPorcentaje(r.tasaEntrega)} de entrega`);
    setText("rep-cancelados", formatoNumero(r.pedidosCancelados));
    setText("rep-tasa-cancelacion", `${formatoPorcentaje(r.tasaCancelacion)} de cancelación`);
    setText("rep-reclamos", formatoNumero(r.reclamos));
    const resolucion = r.horasPromedioResolucion == null ? "sin casos resueltos" : `promedio ${Number(r.horasPromedioResolucion).toFixed(1)} h`;
    setText("rep-reclamos-pendientes", `${formatoNumero(r.reclamosPendientes)} pendientes · ${resolucion}`);
    setText("rep-stock-critico", formatoNumero(r.productosStockBajo));
    setText("rep-stock-agotado", `${formatoNumero(r.productosAgotados)} agotados · ${formatoNumero(r.stockTotal)} unidades`);
    setText("rep-descuentos", FastMarket.money(r.descuentosOtorgados));
    setText("rep-envios", `Envíos ${FastMarket.money(r.ingresosEnvio)}`);
    setText("rep-recurrentes", formatoNumero(r.clientesRecurrentes));

    const actividad = data?.actividadDiaria || [];
    pintarGraficoReporte("grafico-ventas-diarias", actividad, [
        { clave: "ventas", nombre: "Ventas", clase: "", formato: (v) => FastMarket.money(v) }
    ]);
    pintarGraficoReporte("grafico-sesiones-diarias", actividad, [
        { clave: "sesionesExitosas", nombre: "Correctos", clase: "", formato: formatoNumero },
        { clave: "sesionesFallidas", nombre: "Fallidos", clase: "secundaria", formato: formatoNumero }
    ]);
    pintarGraficoReporte("grafico-actividad-diaria", actividad, [
        { clave: "usuariosNuevos", nombre: "Usuarios", clase: "", formato: formatoNumero },
        { clave: "pedidos", nombre: "Pedidos", clase: "secundaria", formato: formatoNumero },
        { clave: "reclamos", nombre: "Reclamos", clase: "terciaria", formato: formatoNumero }
    ]);

    pintarDistribucionReporte("rep-tabla-roles", data?.usuariosPorRol);
    pintarDistribucionReporte("rep-tabla-estados", data?.pedidosPorEstado);
    pintarDistribucionReporte("rep-tabla-pagos", data?.ventasPorMetodoPago, true);
    pintarDistribucionReporte("rep-tabla-reclamos-tipo", data?.reclamosPorTipo);
    pintarDistribucionReporte("rep-tabla-reclamos-estado", data?.reclamosPorEstado);
    pintarDistribucionReporte("rep-tabla-categorias", data?.productosPorCategoria);
    pintarTopProductosReporte(data?.productosMasVendidos || []);
    pintarStockReporte(data?.stockCritico || []);
    pintarReclamosReporte(data?.reclamosRecientes || []);
}

function pintarGraficoReporte(id, datos, series) {
    const cont = document.getElementById(id);
    if (!cont) return;
    if (!datos?.length) {
        cont.innerHTML = `<div class="grafico-vacio">No hay datos para el periodo seleccionado.</div>`;
        return;
    }

    const maximo = Math.max(0, ...datos.flatMap((d) => series.map((s) => Number(d[s.clave] || 0))));
    const ancho = Math.max(100, datos.length * Math.max(34, series.length * 17));
    const columnas = datos.map((d) => {
        const barras = series.map((serie) => {
            const valor = Number(d[serie.clave] || 0);
            const alto = maximo > 0 ? Math.max(valor > 0 ? 4 : 1, Math.round((valor / maximo) * 178)) : 1;
            const titulo = `${serie.nombre}: ${serie.formato ? serie.formato(valor) : valor}`;
            return `<i class="grafico-barra ${serie.clase || ""}" style="height:${alto}px" title="${FastMarket.escapeHTML(titulo)}"></i>`;
        }).join("");
        return `<div class="grafico-columna"><div class="grafico-columna-barras">${barras}</div><span class="grafico-etiqueta" title="${FastMarket.escapeHTML(d.fecha)}">${etiquetaFechaGrafico(d.fecha)}</span></div>`;
    }).join("");
    const leyenda = series.map((serie) => `<span class="${serie.clase || ""}">${FastMarket.escapeHTML(serie.nombre)}</span>`).join("");
    cont.innerHTML = `<div class="grafico-barras-contenido" style="width:${ancho}px;min-width:100%">${columnas}</div><div class="grafico-leyenda">${leyenda}</div>`;
}

function pintarDistribucionReporte(id, lista = [], mostrarMonto = false) {
    const cont = document.getElementById(id);
    if (!cont) return;
    const datos = (lista || []).filter((item) => Number(item.cantidad || 0) > 0 || mostrarMonto);
    if (!datos.length) {
        cont.innerHTML = `<p class="grafico-vacio" style="min-height:100px">Sin datos</p>`;
        return;
    }
    const total = datos.reduce((acc, item) => acc + Number(item.cantidad || 0), 0);
    cont.innerHTML = datos.map((item) => {
        const cantidad = Number(item.cantidad || 0);
        const ancho = total > 0 ? Math.max(cantidad > 0 ? 4 : 0, (cantidad / total) * 100) : 0;
        const valor = mostrarMonto ? `${formatoNumero(cantidad)} · ${FastMarket.money(item.total)}` : formatoNumero(cantidad);
        return `<div class="tabla-resumen-fila"><span>${FastMarket.escapeHTML(item.nombre || "Sin especificar")}</span><strong>${FastMarket.escapeHTML(valor)}</strong><div class="tabla-resumen-progreso"><i style="width:${ancho}%"></i></div></div>`;
    }).join("");
}

function pintarTopProductosReporte(lista) {
    const tbody = document.getElementById("rep-top-productos");
    if (!tbody) return;
    tbody.innerHTML = lista.length ? lista.map((p, i) => `<tr><td><strong>${i + 1}. ${FastMarket.escapeHTML(p.nombre)}</strong></td><td>${formatoNumero(p.unidades)}</td><td>${FastMarket.money(p.ventas)}</td></tr>`).join("") : `<tr><td colspan="3">No se registraron ventas en este periodo.</td></tr>`;
}

function pintarStockReporte(lista) {
    const tbody = document.getElementById("rep-stock-tabla");
    if (!tbody) return;
    tbody.innerHTML = lista.length ? lista.map((p) => `<tr><td><strong>${FastMarket.escapeHTML(p.nombre)}</strong></td><td>${FastMarket.escapeHTML(p.categoria || "")}</td><td><span class="stock-numero ${Number(p.stock) <= 0 ? "agotado" : "bajo"}">${formatoNumero(p.stock)}</span></td><td>${FastMarket.escapeHTML(p.vendedor || "Tienda")}</td></tr>`).join("") : `<tr><td colspan="4">No hay productos con stock crítico.</td></tr>`;
}

function pintarReclamosReporte(lista) {
    const tbody = document.getElementById("rep-reclamos-tabla");
    if (!tbody) return;
    if (!lista.length) {
        tbody.innerHTML = `<tr><td colspan="6">No se registraron reclamos en este periodo.</td></tr>`;
        return;
    }
    const estados = ["ABIERTO", "EN_REVISION", "RESUELTO", "CERRADO"];
    tbody.innerHTML = lista.map((r) => `<tr data-reclamo-fila="${r.id}">
        <td><strong>${FastMarket.escapeHTML(r.codigo)}</strong><br><small>${FastMarket.escapeHTML(fecha(r.fechaCreacion))}</small></td>
        <td><strong>${FastMarket.escapeHTML(r.usuarioNombre)}</strong><br><small>${FastMarket.escapeHTML(r.usuarioCorreo)}</small></td>
        <td>${FastMarket.escapeHTML(textoEnumReporte(r.tipo))}<br><small>${FastMarket.escapeHTML(r.pedidoCodigo || "Sin pedido asociado")}</small></td>
        <td class="reclamo-descripcion"><strong>${FastMarket.escapeHTML(r.asunto)}</strong><br>${FastMarket.escapeHTML(r.descripcion)}</td>
        <td><select data-reclamo-estado>${estados.map((estado) => `<option value="${estado}" ${r.estado === estado ? "selected" : ""}>${FastMarket.escapeHTML(textoEnumReporte(estado))}</option>`).join("")}</select><textarea data-reclamo-respuesta maxlength="4000" placeholder="Respuesta para el cliente">${FastMarket.escapeHTML(r.respuesta || "")}</textarea></td>
        <td><button type="button" class="btn-editar btn-guardar-reclamo" data-guardar-reclamo="${r.id}">Guardar</button></td>
    </tr>`).join("");
}

async function guardarReclamoAdmin(id) {
    const fila = document.querySelector(`[data-reclamo-fila="${id}"]`);
    if (!fila) return;
    const estado = fila.querySelector("[data-reclamo-estado]")?.value || "ABIERTO";
    const respuesta = fila.querySelector("[data-reclamo-respuesta]")?.value.trim() || "";
    const boton = fila.querySelector("[data-guardar-reclamo]");
    if (boton) boton.disabled = true;
    try {
        await FastMarket.request(`/reclamos/${id}`, { method: "PUT", auth: true, body: { estado, respuesta } });
        toast("Reclamo actualizado correctamente.");
        await cargarReportes();
    } catch (error) {
        toast(error.message);
    } finally {
        if (boton) boton.disabled = false;
    }
}

function exportarReporteCSV() {
    if (!reporteAdmin) {
        toast("Primero actualiza el reporte.");
        return;
    }
    const filas = [["Sección", "Indicador", "Fecha", "Cantidad", "Monto"]];
    const r = reporteAdmin.resumen || {};
    [
        ["Resumen", "Usuarios totales", "", r.usuariosTotales, ""],
        ["Resumen", "Usuarios nuevos", "", r.usuariosNuevos, ""],
        ["Resumen", "Sesiones exitosas", "", r.sesionesExitosas, ""],
        ["Resumen", "Sesiones fallidas", "", r.sesionesFallidas, ""],
        ["Resumen", "Pedidos registrados", "", r.pedidosRegistrados, ""],
        ["Resumen", "Ventas válidas", "", r.ventasValidas, r.ingresos],
        ["Resumen", "Unidades vendidas", "", r.unidadesVendidas, ""],
        ["Resumen", "Reclamos", "", r.reclamos, ""]
    ].forEach((fila) => filas.push(fila));
    (reporteAdmin.actividadDiaria || []).forEach((d) => {
        filas.push(["Actividad diaria", "Ventas", d.fecha, d.pedidos, d.ventas]);
        filas.push(["Actividad diaria", "Sesiones exitosas", d.fecha, d.sesionesExitosas, ""]);
        filas.push(["Actividad diaria", "Sesiones fallidas", d.fecha, d.sesionesFallidas, ""]);
        filas.push(["Actividad diaria", "Usuarios nuevos", d.fecha, d.usuariosNuevos, ""]);
        filas.push(["Actividad diaria", "Reclamos", d.fecha, d.reclamos, ""]);
    });
    (reporteAdmin.productosMasVendidos || []).forEach((p) => filas.push(["Productos", p.nombre, "", p.unidades, p.ventas]));
    (reporteAdmin.reclamosRecientes || []).forEach((rec) => filas.push(["Reclamos", `${rec.codigo} - ${rec.asunto}`, rec.fechaCreacion, rec.estado, ""]));

    const csv = "\uFEFF" + filas.map((fila) => fila.map(celdaCSV).join(";")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const enlace = document.createElement("a");
    enlace.href = URL.createObjectURL(blob);
    enlace.download = `fastmarket_reporte_${value("reporte-desde")}_${value("reporte-hasta")}.csv`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(enlace.href);
}

function celdaCSV(valor) {
    let texto = String(valor ?? "");
    if (/^[=+\-@]/.test(texto)) texto = `'${texto}`;
    texto = texto.replaceAll('"', '""');
    return `"${texto}"`;
}

function fechaISOReporte(fechaValor) {
    const y = fechaValor.getFullYear();
    const m = String(fechaValor.getMonth() + 1).padStart(2, "0");
    const d = String(fechaValor.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

function formatearFechaCorta(valor) {
    if (!valor) return "";
    return new Date(`${valor}T00:00:00`).toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function etiquetaFechaGrafico(valor) {
    if (!valor) return "";
    return new Date(`${valor}T00:00:00`).toLocaleDateString("es-PE", { day: "2-digit", month: "2-digit" });
}

function formatoNumero(valor) {
    return Number(valor || 0).toLocaleString("es-PE");
}

function formatoPorcentaje(valor) {
    return `${Number(valor || 0).toLocaleString("es-PE", { maximumFractionDigits: 2 })}%`;
}

function textoEnumReporte(valor) {
    const mapa = { EN_REVISION: "En revisión", DEVOLUCION: "Devolución", ATENCION: "Atención" };
    if (mapa[valor]) return mapa[valor];
    return String(valor || "").toLowerCase().replaceAll("_", " ").replace(/^./, (c) => c.toUpperCase());
}

function cargarImagen(e, inputId, previewId, imgId) {
    const archivo = e.target.files[0];
    if (!archivo) return;
    if (!archivo.type.startsWith("image/")) {
        toast("Selecciona una imagen válida.");
        return;
    }

    const reader = new FileReader();
    reader.onload = () => {
        setValue(inputId, reader.result);
        const img = document.getElementById(imgId);
        if (img) img.src = reader.result;
        document.getElementById(previewId)?.classList.remove("oculto");
    };
    reader.readAsDataURL(archivo);
}

function value(id) {
    const el = document.getElementById(id);
    return el ? String(el.value || "").trim() : "";
}

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val ?? "";
}

function checked(id) {
    return !!document.getElementById(id)?.checked;
}

function setChecked(id, val) {
    const el = document.getElementById(id);
    if (el) el.checked = !!val;
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val ?? "";
}

function toast(mensaje) {
    const el = document.getElementById("toast");
    if (!el) {
        if (FastMarket.notify) FastMarket.notify(mensaje);
        return;
    }
    el.textContent = mensaje;
    el.classList.add("activo");
    setTimeout(() => el.classList.remove("activo"), 2800);
}


function stockBadge(stock) {
    const n = Number(stock || 0);
    if (n <= 0) return `<span class="estado-stock estado-stock-agotado">Agotado</span>`;
    if (n <= 5) return `<span class="estado-stock estado-stock-bajo">Stock bajo · ${n}</span>`;
    return `<span class="estado-stock estado-stock-ok">${n} unidades</span>`;
}

function formatearTipoIndex(tipo) {
    const nombres = {
        destacado: "Destacados",
        promocion: "Promociones",
        opinion: "Opiniones",
        ayuda: "Ayuda"
    };
    return nombres[tipo] || tipo;
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

function actualizarPaginacion(tipo) {
    const estado = adminPages[tipo];
    const info = document.getElementById(`${tipo}-page-info`);
    const prev = document.getElementById(`${tipo}-prev`);
    const next = document.getElementById(`${tipo}-next`);
    if (info) info.textContent = `Página ${estado.page + 1} de ${estado.totalPages}`;
    if (prev) prev.disabled = estado.page <= 0;
    if (next) next.disabled = estado.page + 1 >= estado.totalPages;
}

function fecha(valor) {
    if (!valor) return "";
    return new Date(valor).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" });
}
