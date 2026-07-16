let productos = [];
let bannersActivos = [];
let carrito = [];
let cuponAplicado = null;

let categoriaActual = "todos";
let busquedaActual = "";
let ordenActual = "normal";
let modoOfertas = false;
let slideActual = 0;
let intervaloSlider = null;

const categoriasBase = [
    { codigo: "moda", nombre: "Moda" },
    { codigo: "tecnologia", nombre: "Tecnología" },
    { codigo: "hogar", nombre: "Hogar" },
    { codigo: "estudio", nombre: "Estudio" },
    { codigo: "belleza", nombre: "Belleza" },
    { codigo: "deportes", nombre: "Deportes" },
    { codigo: "juguetes", nombre: "Juguetes" }
];

let categoriasOficiales = [...categoriasBase];

const imagenesCategoriaBase = {
    moda: "img/intro.png",
    tecnologia: "img/atencion.png",
    hogar: "img/envios.png",
    estudio: "img/fondo1.png",
    belleza: "img/intro.png",
    deportes: "img/fondo1.png",
    juguetes: "img/logo.png"
};

const tarjetasPromocionBase = [
    { titulo: "Descuentos destacados", descripcion: "25% OFF", imagen: "img/fondo1.png", url: "productos.html?ofertas=1" },
    { titulo: "Tecnología y accesorios", descripcion: "Ofertas disponibles", imagen: "img/intro.png", url: "productos.html?categoria=tecnologia" },
    { titulo: "Renueva tu estilo", descripcion: "Promos seleccionadas", imagen: "img/fondo1.png", url: "productos.html?categoria=moda" },
    { titulo: "Especial para estudiantes", descripcion: "Compra fácil y rápido", imagen: "img/envios.png", url: "productos.html?categoria=estudio" }
];

document.addEventListener("DOMContentLoaded", async () => {
    FastMarket.activarBuscador("buscador-header", "busqueda");
    FastMarket.activarMenuCliente();
    FastMarket.mostrarPanelCliente();
    FastMarket.activarChatBasico();

    const params = new URLSearchParams(window.location.search);
    modoOfertas = params.get("ofertas") === "1";
    const q = params.get("q");
    const categoriaUrl = params.get("categoria");

    if (categoriaUrl) categoriaActual = normalizarCategoria(categoriaUrl);
    actualizarVistaOfertas();

    if (q) {
        const buscarProducto = document.getElementById("buscar-producto");
        if (buscarProducto) buscarProducto.value = q;
        busquedaActual = q.toLowerCase();
    }

    await cargarCategorias();
    pintarFiltrosCategorias();
    activarEventos();

    if (window.FastMarketCart) {
        await FastMarketCart.cargar();
    } else {
        await cargarCarritoPersistente();
        actualizarContadorCarrito();
        mostrarCarrito();
    }

    await cargarBanners();
    await cargarProductos();
});

function actualizarVistaOfertas() {
    if (!modoOfertas) return;
    const titulo = document.querySelector(".titulo-productos h2");
    const descripcion = document.querySelector(".titulo-productos p");
    if (titulo) titulo.textContent = "Ofertas";
    if (descripcion) descripcion.textContent = "Productos con promoción disponibles en FastMarket.";
}

async function cargarCategorias() {
    try {
        const data = await FastMarket.request("/productos/categorias");
        if (Array.isArray(data) && data.length) {
            categoriasOficiales = data
                .map((categoria) => ({
                    codigo: normalizarCategoria(categoria.codigo || categoria.nombre),
                    nombre: categoria.nombre || categoria.codigo
                }))
                .filter((categoria) => categoria.codigo && categoria.nombre);
        }
    } catch {
        categoriasOficiales = [...categoriasBase];
    }
}

function pintarFiltrosCategorias() {
    const contenedor = document.getElementById("categorias-filtros");
    if (!contenedor) return;

    const botones = [
        `<button class="categoria-btn ${categoriaActual === "todos" ? "activo" : ""}" data-categoria="todos">Todos</button>`,
        ...categoriasOficiales.map((categoria) => `
            <button class="categoria-btn ${categoriaActual === categoria.codigo ? "activo" : ""}" data-categoria="${FastMarket.escapeHTML(categoria.codigo)}">
                ${FastMarket.escapeHTML(categoria.nombre)}
            </button>`)
    ];

    contenedor.innerHTML = botones.join("");
}

function normalizarCategoria(valor) {
    return String(valor || "todos")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "") || "todos";
}

function codigoCategoria(valor) {
    const clave = normalizarCategoria(valor);
    const encontrada = categoriasOficiales.find((categoria) => categoria.codigo === clave || normalizarCategoria(categoria.nombre) === clave);
    return encontrada?.codigo || clave;
}

function nombreCategoria(valor) {
    const codigo = codigoCategoria(valor);
    const categoria = categoriasOficiales.find((item) => item.codigo === codigo);
    return categoria?.nombre || String(valor || "General");
}

function imagenPrincipalProducto(producto) {
    if (producto?.imagen) return producto.imagen;
    if (Array.isArray(producto?.imagenes) && producto.imagenes.length) return producto.imagenes[0];
    return "img/logo.png";
}

function productosPorCategoria(codigo) {
    return productos.filter((producto) => codigoCategoria(producto.categoria) === codigo);
}

function esProductoOferta(producto) {
    return Boolean(producto.oferta) || Number(producto.precioAntes || 0) > Number(producto.precio || 0);
}

function etiquetaDescuento(producto) {
    const precio = Number(producto.precio || 0);
    const antes = Number(producto.precioAntes || 0);
    if (antes > precio && precio > 0) {
        return `-${Math.round((1 - precio / antes) * 100)}%`;
    }
    return "Oferta";
}

function actualizarBotonesCategoria() {
    document.querySelectorAll(".categoria-btn").forEach((btn) => {
        btn.classList.toggle("activo", (btn.dataset.categoria || "todos") === categoriaActual);
    });
}

function aplicarCategoria(categoria, moverAlListado = false) {
    categoriaActual = normalizarCategoria(categoria);
    actualizarBotonesCategoria();
    mostrarProductos();

    const url = new URL(window.location.href);
    if (categoriaActual === "todos") url.searchParams.delete("categoria");
    else url.searchParams.set("categoria", categoriaActual);
    window.history.replaceState({}, "", url);

    if (moverAlListado) {
        document.getElementById("productos-section")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

function moverCarrusel(id, direccion) {
    const contenedor = document.getElementById(id);
    if (!contenedor) return;
    const distancia = Math.max(260, Math.round(contenedor.clientWidth * 0.85));
    contenedor.scrollBy({ left: distancia * Number(direccion || 1), behavior: "smooth" });
}

function normalizarItemCarrito(item) {
    return {
        id: Number(item.productoId || item.id),
        nombre: item.nombre || item.productoNombre || "Producto",
        precio: Number(item.precio ?? item.precioUnitario ?? 0),
        imagen: item.imagen || "img/logo.png",
        stock: Number(item.stockDisponible ?? item.stock ?? 0),
        cantidad: Number(item.cantidad || 1)
    };
}

function combinarCarritos(remoto, local) {
    const mapa = new Map();
    [...(remoto || []), ...(local || [])].forEach((item) => {
        const normalizado = normalizarItemCarrito(item);
        if (!normalizado.id || normalizado.cantidad <= 0) return;
        const existente = mapa.get(normalizado.id);
        if (existente) {
            existente.cantidad = Math.min(Number(normalizado.stock || existente.stock || 999999), existente.cantidad + normalizado.cantidad);
        } else {
            mapa.set(normalizado.id, normalizado);
        }
    });
    return Array.from(mapa.values());
}

async function cargarCarritoPersistente() {
    const usuario = FastMarket.getCliente();
    const local = (FastMarket.obtenerCarritoLocal?.() || JSON.parse(localStorage.getItem("fastmarket_carrito") || "[]")).map(normalizarItemCarrito);
    const cuponLocal = FastMarket.obtenerCuponLocal?.() || JSON.parse(localStorage.getItem("fastmarket_cupon") || sessionStorage.getItem("fastmarket_checkout_cupon") || "null");
    const localEsMirror = usuario && FastMarket.carritoLocalPerteneceAlUsuario?.(usuario.id);

    try {
        const data = await FastMarket.obtenerCarrito();
        const remoto = (data.items || []).map(normalizarItemCarrito);

        if (usuario && !localEsMirror && local.length) {
            carrito = combinarCarritos(remoto, local);
            const codigo = cuponLocal?.codigo || data.cuponCodigo || null;
            FastMarket.prepararCheckoutCarrito?.(carrito, cuponLocal || null);
            const sincronizado = await FastMarket.sincronizarCarrito(carrito, codigo);
            carrito = (sincronizado.items || []).map(normalizarItemCarrito);
            cuponAplicado = sincronizado.cuponCodigo ? { codigo: sincronizado.cuponCodigo, descuento: Number(sincronizado.descuento || 0), descripcion: cuponLocal?.descripcion || "" } : cuponLocal;
        } else if (usuario) {
            carrito = remoto.length ? remoto : local;
            cuponAplicado = data.cuponCodigo ? { codigo: data.cuponCodigo, descuento: Number(data.descuento || 0), descripcion: cuponLocal?.descripcion || "" } : cuponLocal;
        } else {
            carrito = local;
            cuponAplicado = cuponLocal;
        }

        FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
        const inputCupon = document.getElementById("cupon-carrito");
        if (inputCupon && cuponAplicado?.codigo) inputCupon.value = cuponAplicado.codigo;
    } catch {
        carrito = local.map(normalizarItemCarrito);
        cuponAplicado = cuponLocal;
        FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
    }
}

function activarEventos() {
    const buscarProducto = document.getElementById("buscar-producto");
    const ordenarProducto = document.getElementById("ordenar-producto");
    const botonesCategoria = document.querySelectorAll(".categoria-btn");
    const btnAnterior = document.getElementById("anterior");
    const btnSiguiente = document.getElementById("siguiente");
    const carritoIcono = document.getElementById("carrito");
    const cerrarCarrito = document.getElementById("cerrar-carrito");
    const overlayCarrito = document.getElementById("overlay-carrito");
    const finalizarCompra = document.getElementById("finalizar-compra");
    const vaciarCarrito = document.getElementById("vaciar-carrito");
    const listaCarrito = document.getElementById("carrito-lista");
    const btnCupon = document.getElementById("aplicar-cupon-carrito");
    const inputCupon = document.getElementById("cupon-carrito");

    if (buscarProducto) {
        buscarProducto.addEventListener("input", () => {
            busquedaActual = buscarProducto.value.toLowerCase().trim();
            mostrarProductos();
        });
    }

    if (ordenarProducto) {
        ordenarProducto.addEventListener("change", () => {
            ordenActual = ordenarProducto.value;
            mostrarProductos();
        });
    }

    botonesCategoria.forEach((btn) => {
        btn.addEventListener("click", () => aplicarCategoria(btn.dataset.categoria || "todos", false));
    });

    const cuerpoProductos = document.getElementById("cuerpo-productos");
    if (cuerpoProductos) {
        cuerpoProductos.addEventListener("click", (e) => {
            const btnScroll = e.target.closest("[data-scroll-target]");
            if (btnScroll) {
                moverCarrusel(btnScroll.dataset.scrollTarget, btnScroll.dataset.scroll || 1);
                return;
            }

            const btnCategoria = e.target.closest("[data-filtrar-categoria]");
            if (btnCategoria) {
                aplicarCategoria(btnCategoria.dataset.filtrarCategoria || "todos", true);
            }
        });
    }

    if (btnAnterior) btnAnterior.addEventListener("click", () => moverSlide(-1));
    if (btnSiguiente) btnSiguiente.addEventListener("click", () => moverSlide(1));
    if (!window.FastMarketCart) {
        if (carritoIcono) carritoIcono.addEventListener("click", (e) => {
            e.preventDefault();
            abrirCarrito();
        });
        if (cerrarCarrito) cerrarCarrito.addEventListener("click", cerrarPanelCarrito);
        if (overlayCarrito) overlayCarrito.addEventListener("click", cerrarPanelCarrito);
        if (vaciarCarrito) vaciarCarrito.addEventListener("click", () => {
            carrito = [];
            guardarCarrito();
        });
        if (finalizarCompra) finalizarCompra.addEventListener("click", () => {
            if (carrito.length === 0) {
                FastMarket.notify("Tu carrito está vacío.", "warning");
                return;
            }
            FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
            localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
            sessionStorage.setItem("fastmarket_checkout_carrito", JSON.stringify(carrito));
            window.location.href = "checkout.html";
        });
        if (inputCupon && cuponAplicado?.codigo) inputCupon.value = cuponAplicado.codigo;
        if (btnCupon) btnCupon.addEventListener("click", aplicarCuponCarrito);

        if (listaCarrito) {
            listaCarrito.addEventListener("click", (e) => {
                const btn = e.target.closest("[data-accion]");
                if (!btn) return;
                cambiarCantidad(Number(btn.dataset.id), btn.dataset.accion);
            });
        }
    }
}

async function cargarProductos() {
    const contenedor = document.getElementById("productos-contenedor");
    try {
        productos = await FastMarket.request("/productos");
        renderizarEscaparates();
        mostrarProductos();
    } catch (error) {
        if (contenedor) {
            contenedor.innerHTML = `<div class="mensaje-error">No se pudieron cargar productos: ${FastMarket.escapeHTML(error.message)}</div>`;
        }
        renderizarEscaparates();
    }
}

function renderizarEscaparates() {
    pintarPromociones();
    pintarCategoriasDestacadas();
    pintarProductosDestacados();
    pintarFilasPorCategoria();
}

function pintarPromociones() {
    const contenedor = document.getElementById("promociones-carrusel");
    if (!contenedor) return;

    contenedor.innerHTML = "";

    const promocionesAdmin = bannersActivos
        .filter((banner) => banner && banner.imagen && (banner.id || banner.titulo || banner.descripcion))
        .slice(0, 12);

    if (promocionesAdmin.length) {
        promocionesAdmin.forEach((banner) => {
            const card = document.createElement("a");
            card.className = "promo-card promo-card-admin";
            card.href = "productos.html?ofertas=1";
            const titulo = banner.titulo || "Promoción FastMarket";
            const descripcion = banner.descripcion || "Ver ofertas";
            card.innerHTML = `
                <img src="${FastMarket.escapeHTML(banner.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(titulo)}" onerror="this.src='img/logo.png'">
                <div class="promo-overlay"></div>
                <div class="promo-info">
                    <span>${FastMarket.escapeHTML(descripcion)}</span>
                    <h3>${FastMarket.escapeHTML(titulo)}</h3>
                    <p>Ver promoción</p>
                </div>`;
            contenedor.appendChild(card);
        });
        return;
    }

    const productosOferta = productos.filter(esProductoOferta).slice(0, 12);
    if (productosOferta.length) {
        productosOferta.forEach((producto) => {
            const card = document.createElement("article");
            card.className = "promo-card promo-card-producto";
            card.tabIndex = 0;
            card.setAttribute("role", "link");
            card.innerHTML = `
                <img src="${FastMarket.escapeHTML(imagenPrincipalProducto(producto))}" alt="${FastMarket.escapeHTML(producto.nombre)}" onerror="this.src='img/logo.png'">
                <div class="promo-overlay"></div>
                <div class="promo-info">
                    <span>${FastMarket.escapeHTML(etiquetaDescuento(producto))}</span>
                    <h3>${FastMarket.escapeHTML(producto.nombre)}</h3>
                    <p>${FastMarket.money(producto.precio)} ${producto.precioAntes ? `<small>${FastMarket.money(producto.precioAntes)}</small>` : ""}</p>
                </div>`;
            card.addEventListener("click", () => {
                window.location.href = `detalle-producto.html?id=${producto.id}`;
            });
            card.addEventListener("keydown", (e) => {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    window.location.href = `detalle-producto.html?id=${producto.id}`;
                }
            });
            contenedor.appendChild(card);
        });
        return;
    }

    tarjetasPromocionBase.forEach((promo) => {
        const card = document.createElement("a");
        card.className = "promo-card";
        card.href = promo.url;
        card.innerHTML = `
            <img src="${FastMarket.escapeHTML(promo.imagen)}" alt="${FastMarket.escapeHTML(promo.titulo)}" onerror="this.src='img/logo.png'">
            <div class="promo-overlay"></div>
            <div class="promo-info">
                <span>${FastMarket.escapeHTML(promo.descripcion)}</span>
                <h3>${FastMarket.escapeHTML(promo.titulo)}</h3>
                <p>Ver promoción</p>
            </div>`;
        contenedor.appendChild(card);
    });
}

function pintarCategoriasDestacadas() {
    const contenedor = document.getElementById("categorias-carrusel");
    if (!contenedor) return;

    contenedor.innerHTML = "";
    categoriasOficiales.forEach((categoria) => {
        const productosCategoria = productosPorCategoria(categoria.codigo);
        const productoBase = productosCategoria.find((producto) => imagenPrincipalProducto(producto));
        const imagen = productoBase ? imagenPrincipalProducto(productoBase) : (imagenesCategoriaBase[categoria.codigo] || "img/logo.png");

        const card = document.createElement("button");
        card.type = "button";
        card.className = "categoria-tarjeta";
        card.dataset.filtrarCategoria = categoria.codigo;
        card.innerHTML = `
            <img src="${FastMarket.escapeHTML(imagen)}" alt="${FastMarket.escapeHTML(categoria.nombre)}" onerror="this.src='img/logo.png'">
            <span>${FastMarket.escapeHTML(categoria.nombre)}</span>
            <small>${productosCategoria.length} producto${productosCategoria.length === 1 ? "" : "s"}</small>`;
        contenedor.appendChild(card);
    });
}

function pintarProductosDestacados() {
    const contenedor = document.getElementById("destacados-carrusel");
    if (!contenedor) return;

    let destacados = productos.filter((producto) => Boolean(producto.destacado) && Number(producto.stock || 0) > 0);
    if (!destacados.length) destacados = productos.filter(esProductoOferta);
    if (!destacados.length) destacados = productos.slice(0, 12);

    contenedor.innerHTML = "";

    if (!destacados.length) {
        contenedor.innerHTML = `<div class="mini-vacio">Pronto mostraremos productos destacados.</div>`;
        return;
    }

    destacados.slice(0, 12).forEach((producto) => contenedor.appendChild(crearMiniProductoCard(producto)));
}

function pintarFilasPorCategoria() {
    const contenedor = document.getElementById("filas-categorias");
    if (!contenedor) return;

    contenedor.innerHTML = "";
    const filasConProductos = categoriasOficiales
        .map((categoria) => ({ categoria, productos: productosPorCategoria(categoria.codigo).slice(0, 12) }))
        .filter((fila) => fila.productos.length);

    if (!filasConProductos.length) {
        contenedor.innerHTML = `
            <div class="fila-categoria fila-categoria-vacia">
                <div class="seccion-top">
                    <div>
                        <span class="seccion-etiqueta">Categorías</span>
                        <h2>Productos por categoría</h2>
                        <p>Pronto mostraremos productos organizados por categoría.</p>
                    </div>
                </div>
            </div>`;
        return;
    }

    filasConProductos.forEach(({ categoria, productos: items }) => {
        const idCarrusel = `fila-categoria-${categoria.codigo}`;
        const bloque = document.createElement("section");
        bloque.className = "fila-categoria";
        bloque.innerHTML = `
            <div class="seccion-top">
                <div>
                    <span class="seccion-etiqueta">${FastMarket.escapeHTML(categoria.nombre)}</span>
                    <h2>${FastMarket.escapeHTML(categoria.nombre)}</h2>
                    <p>Productos disponibles en esta categoría.</p>
                </div>
                <div class="controles-carrusel">
                    <button type="button" data-filtrar-categoria="${FastMarket.escapeHTML(categoria.codigo)}">Ver todos</button>
                    <button type="button" data-scroll-target="${FastMarket.escapeHTML(idCarrusel)}" data-scroll="-1">‹</button>
                    <button type="button" data-scroll-target="${FastMarket.escapeHTML(idCarrusel)}" data-scroll="1">›</button>
                </div>
            </div>
            <div class="carrusel-lineal productos-linea" id="${FastMarket.escapeHTML(idCarrusel)}"></div>`;

        const carrusel = bloque.querySelector(".productos-linea");
        items.forEach((producto) => carrusel.appendChild(crearMiniProductoCard(producto)));
        contenedor.appendChild(bloque);
    });
}

function crearMiniProductoCard(producto) {
    const card = document.createElement("article");
    const sinStock = Number(producto.stock || 0) <= 0;
    card.className = "mini-producto";
    card.tabIndex = 0;
    card.setAttribute("role", "link");
    card.setAttribute("aria-label", `Ver detalle de ${producto.nombre}`);
    card.innerHTML = `
        <div class="mini-producto-img">
            <img src="${FastMarket.escapeHTML(imagenPrincipalProducto(producto))}" alt="${FastMarket.escapeHTML(producto.nombre)}" onerror="this.src='img/logo.png'">
            ${esProductoOferta(producto) ? `<span>${FastMarket.escapeHTML(etiquetaDescuento(producto))}</span>` : ""}
        </div>
        <div class="mini-producto-info">
            <small>${FastMarket.escapeHTML(nombreCategoria(producto.categoria))}</small>
            <h3>${FastMarket.escapeHTML(producto.nombre)}</h3>
            <div class="mini-precio">
                <strong>${FastMarket.money(producto.precio)}</strong>
                ${producto.precioAntes ? `<em>${FastMarket.money(producto.precioAntes)}</em>` : ""}
            </div>
            <button type="button" class="mini-producto-btn" ${sinStock ? "disabled" : ""}>${sinStock ? "Sin stock" : "Agregar"}</button>
        </div>`;

    card.addEventListener("click", () => {
        window.location.href = `detalle-producto.html?id=${producto.id}`;
    });

    card.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            window.location.href = `detalle-producto.html?id=${producto.id}`;
        }
    });

    const boton = card.querySelector(".mini-producto-btn");
    if (boton) {
        boton.addEventListener("click", (e) => {
            e.stopPropagation();
            agregarAlCarrito(producto, 1);
        });
    }

    return card;
}

async function cargarBanners() {
    try {
        const banners = await FastMarket.request("/banners?activo=true");
        bannersActivos = banners.length ? banners : [
            { titulo: "", descripcion: "", imagen: "img/fondo1.png" },
            { titulo: "", descripcion: "", imagen: "img/intro.png" }
        ];
    } catch {
        bannersActivos = [
            { titulo: "", descripcion: "", imagen: "img/fondo1.png" },
            { titulo: "", descripcion: "", imagen: "img/intro.png" }
        ];
    }
    pintarSlider();
}

function pintarSlider() {
    const slider = document.getElementById("slider-contenedor");
    const puntos = document.getElementById("puntos");
    if (!slider || !puntos) return;

    slider.innerHTML = "";
    puntos.innerHTML = "";

    bannersActivos.forEach((banner, index) => {
        const slide = document.createElement("div");
        slide.className = "slide";
        const titulo = banner.titulo || "";
        const descripcion = banner.descripcion || "";
        slide.innerHTML = `
            <img src="${FastMarket.escapeHTML(banner.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(titulo || "Banner FastMarket")}" onerror="this.src='img/logo.png'">
            ${(titulo || descripcion) ? `
                <div class="slide-info">
                    ${titulo ? `<h2>${FastMarket.escapeHTML(titulo)}</h2>` : ""}
                    ${descripcion ? `<p>${FastMarket.escapeHTML(descripcion)}</p>` : ""}
                </div>` : ""}`;
        slider.appendChild(slide);

        const punto = document.createElement("span");
        punto.className = "punto";
        punto.addEventListener("click", () => {
            slideActual = index;
            actualizarSlider();
        });
        puntos.appendChild(punto);
    });

    slideActual = 0;
    actualizarSlider();
    clearInterval(intervaloSlider);
    intervaloSlider = setInterval(() => moverSlide(1), 5000);
}

function moverSlide(direccion) {
    if (!bannersActivos.length) return;
    slideActual = (slideActual + direccion + bannersActivos.length) % bannersActivos.length;
    actualizarSlider();
}

function actualizarSlider() {
    const slider = document.getElementById("slider-contenedor");
    const puntos = document.querySelectorAll(".punto");
    if (slider) slider.style.transform = `translateX(-${slideActual * 100}%)`;
    puntos.forEach((p, i) => p.classList.toggle("activo", i === slideActual));
}

function mostrarProductos() {
    const contenedor = document.getElementById("productos-contenedor");
    const mensajeVacio = document.getElementById("mensaje-vacio");
    if (!contenedor) return;

    let lista = productos.filter((p) => {
        const categoriaOk = categoriaActual === "todos" || codigoCategoria(p.categoria) === categoriaActual;
        const texto = `${p.nombre} ${p.descripcion} ${nombreCategoria(p.categoria)} ${p.tipoProducto || ""} ${JSON.stringify(p.caracteristicas || {})}`.toLowerCase();
        const ofertaOk = !modoOfertas || Boolean(p.oferta) || Number(p.precioAntes || 0) > Number(p.precio || 0);
        return categoriaOk && ofertaOk && texto.includes(busquedaActual);
    });

    if (ordenActual === "precio-menor") lista.sort((a, b) => Number(a.precio) - Number(b.precio));
    if (ordenActual === "precio-mayor") lista.sort((a, b) => Number(b.precio) - Number(a.precio));
    if (ordenActual === "nombre") lista.sort((a, b) => a.nombre.localeCompare(b.nombre));
    if (ordenActual === "ofertas") lista = lista.filter((p) => p.oferta);

    contenedor.innerHTML = "";
    if (mensajeVacio) mensajeVacio.style.display = lista.length ? "none" : "block";

    lista.forEach((producto) => {
        const card = document.createElement("article");
        card.className = "producto-card";
        card.tabIndex = 0;
        card.setAttribute("role", "link");
        card.setAttribute("aria-label", `Ver detalle de ${producto.nombre}`);

        const sinStock = Number(producto.stock) <= 0;
        const detalleUrl = `detalle-producto.html?id=${producto.id}`;

        card.innerHTML = `
            <div class="producto-imagen-link">
                <img src="${FastMarket.escapeHTML(imagenPrincipalProducto(producto))}" alt="${FastMarket.escapeHTML(producto.nombre)}" onerror="this.src='img/logo.png'">
                <div class="badges-producto">
                    ${producto.oferta ? `<span class="badge-oferta">Oferta</span>` : ""}
                    ${sinStock ? `<span class="badge-stock">Sin stock</span>` : ""}
                </div>
            </div>
            <div class="producto-info">
                <p class="categoria-producto">${FastMarket.escapeHTML(nombreCategoria(producto.categoria))}</p>
                <h3>${FastMarket.escapeHTML(producto.nombre)}</h3>
                <p class="descripcion-producto">${FastMarket.escapeHTML(producto.descripcion || "Producto disponible en FastMarket.")}</p>
                <div class="precio-box">
                    <strong>${FastMarket.money(producto.precio)}</strong>
                    ${producto.precioAntes ? `<span>${FastMarket.money(producto.precioAntes)}</span>` : ""}
                </div>
                <small class="stock-producto">Stock: ${Number(producto.stock || 0)}</small>
                <button class="btn-agregar" ${sinStock ? "disabled" : ""}>Agregar al carrito</button>
            </div>`;

        card.addEventListener("click", () => {
            window.location.href = detalleUrl;
        });

        card.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                window.location.href = detalleUrl;
            }
        });

        const btnAgregar = card.querySelector(".btn-agregar");
        if (btnAgregar) {
            btnAgregar.addEventListener("click", (e) => {
                e.stopPropagation();
                agregarAlCarrito(producto, 1);
            });
        }

        contenedor.appendChild(card);
    });
}

function agregarAlCarrito(producto, cantidad) {
    if (window.FastMarketCart) {
        FastMarketCart.agregar(producto, cantidad);
        return;
    }
    const item = carrito.find((p) => Number(p.id) === Number(producto.id));
    const cantidadActual = item ? item.cantidad : 0;
    if (cantidadActual + cantidad > Number(producto.stock)) {
        FastMarket.notify("No hay stock suficiente para ese producto.", "warning");
        return;
    }

    if (item) {
        item.cantidad += cantidad;
    } else {
        carrito.push({
            id: producto.id,
            nombre: producto.nombre,
            precio: Number(producto.precio),
            imagen: producto.imagen,
            stock: Number(producto.stock),
            cantidad
        });
    }

    guardarCarrito();
    abrirCarrito();
}

function cambiarCantidad(id, accion) {
    const item = carrito.find((p) => Number(p.id) === Number(id));
    if (!item) return;

    if (accion === "sumar") {
        if (item.cantidad >= Number(item.stock || 0)) {
            FastMarket.notify("No hay más stock disponible.", "warning");
            return;
        }
        item.cantidad++;
    }

    if (accion === "restar") item.cantidad--;
    if (accion === "eliminar" || item.cantidad <= 0) {
        carrito = carrito.filter((p) => Number(p.id) !== Number(id));
    }

    guardarCarrito();
}

async function guardarCarrito() {
    const codigo = carrito.length ? cuponAplicado?.codigo || null : null;
    if (carrito.length === 0) cuponAplicado = null;
    try {
        FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
        localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
        sessionStorage.setItem("fastmarket_checkout_carrito", JSON.stringify(carrito));
        const sincronizado = await FastMarket.sincronizarCarrito(carrito, codigo);
        if (FastMarket.getCliente()) {
            carrito = (sincronizado.items || []).map(normalizarItemCarrito);
            cuponAplicado = sincronizado.cuponCodigo ? { codigo: sincronizado.cuponCodigo, descuento: Number(sincronizado.descuento || 0) } : null;
            FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
        }
    } catch {
        localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
        if (!codigo) localStorage.removeItem("fastmarket_cupon");
    }
    actualizarContadorCarrito();
    mostrarCarrito();
}

function actualizarContadorCarrito() {
    const contador = document.getElementById("contador-carrito");
    if (contador) contador.textContent = carrito.reduce((s, p) => s + Number(p.cantidad), 0);
}

function mostrarCarrito() {
    const lista = document.getElementById("carrito-lista");
    const total = document.getElementById("total-carrito");
    const subtotalEl = document.getElementById("subtotal-carrito");
    const descuentoEl = document.getElementById("descuento-carrito");
    const filaDescuento = document.getElementById("fila-descuento-carrito");
    if (!lista || !total) return;

    lista.innerHTML = "";

    if (carrito.length === 0) {
        lista.innerHTML = `<p class="carrito-vacio">Tu carrito está vacío.</p>`;
        if (subtotalEl) subtotalEl.textContent = FastMarket.money(0);
        if (descuentoEl) descuentoEl.textContent = `- ${FastMarket.money(0)}`;
        filaDescuento?.classList.add("oculto");
        total.textContent = FastMarket.money(0);
        return;
    }

    carrito.forEach((item) => {
        const div = document.createElement("div");
        div.className = "item-carrito";
        div.innerHTML = `
            <img src="${FastMarket.escapeHTML(item.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(item.nombre)}" onerror="this.src='img/logo.png'">
            <div>
                <h4>${FastMarket.escapeHTML(item.nombre)}</h4>
                <p>${FastMarket.money(item.precio)}</p>
                <div class="cantidad-carrito">
                    <button data-accion="restar" data-id="${item.id}">-</button>
                    <span>${item.cantidad}</span>
                    <button data-accion="sumar" data-id="${item.id}">+</button>
                    <button data-accion="eliminar" data-id="${item.id}">Eliminar</button>
                </div>
            </div>`;
        lista.appendChild(div);
    });

    const suma = carrito.reduce((s, p) => s + Number(p.precio) * Number(p.cantidad), 0);
    const descuento = Number(cuponAplicado?.descuento || 0);
    if (subtotalEl) subtotalEl.textContent = FastMarket.money(suma);
    if (descuentoEl) descuentoEl.textContent = `- ${FastMarket.money(descuento)}`;
    filaDescuento?.classList.toggle("oculto", descuento <= 0);
    total.textContent = FastMarket.money(Math.max(0, suma - descuento));
}

async function aplicarCuponCarrito() {
    const input = document.getElementById("cupon-carrito");
    const mensaje = document.getElementById("mensaje-cupon-carrito");
    const codigo = input?.value.trim().toUpperCase();

    const pintar = (texto, tipo) => {
        if (!mensaje) return;
        mensaje.textContent = texto;
        mensaje.classList.remove("ok", "error");
        mensaje.classList.add(tipo);
    };

    if (!codigo) {
        cuponAplicado = null;
        FastMarket.sincronizarCarrito(carrito, null).catch(() => localStorage.removeItem("fastmarket_cupon"));
        pintar("Cupón eliminado.", "ok");
        mostrarCarrito();
        return;
    }

    if (!carrito.length) {
        pintar("Agrega productos antes de aplicar un cupón.", "error");
        return;
    }

    try {
        pintar("Validando cupón...", "ok");
        const respuesta = await FastMarket.request("/cupones/aplicar", {
            method: "POST",
            auth: true,
            body: {
                codigo,
                items: carrito.map((item) => ({ productoId: Number(item.id), cantidad: Number(item.cantidad) }))
            }
        });
        cuponAplicado = respuesta;
        await FastMarket.sincronizarCarrito(carrito, cuponAplicado);
        pintar(`${respuesta.mensaje} Descuento: ${FastMarket.money(respuesta.descuento)}`, "ok");
        mostrarCarrito();
    } catch (error) {
        cuponAplicado = null;
        await FastMarket.sincronizarCarrito(carrito, null).catch(() => localStorage.removeItem("fastmarket_cupon"));
        pintar(error.message, "error");
        mostrarCarrito();
    }
}

function abrirCarrito() {
    const overlay = document.getElementById("overlay-carrito");
    const panel = document.getElementById("panel-carrito");
    if (overlay) overlay.classList.add("activo");
    if (panel) panel.classList.add("activo");
    mostrarCarrito();
}

function cerrarPanelCarrito() {
    const overlay = document.getElementById("overlay-carrito");
    const panel = document.getElementById("panel-carrito");
    if (overlay) overlay.classList.remove("activo");
    if (panel) panel.classList.remove("activo");
}
