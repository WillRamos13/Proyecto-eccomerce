let productoActual = null;
let relacionados = [];
let cantidad = 1;

document.addEventListener("DOMContentLoaded", async () => {
    FastMarket.activarBuscador("buscador", "busqueda");
    FastMarket.activarMenuCliente();
    if (window.FastMarketCart) await FastMarketCart.cargar();

    document.getElementById("sumar")?.addEventListener("click", () => cambiarCantidad(1));
    document.getElementById("restar")?.addEventListener("click", () => cambiarCantidad(-1));
    document.getElementById("agregar-carrito")?.addEventListener("click", () => agregarCarrito(false));
    document.getElementById("comprar-ahora")?.addEventListener("click", () => agregarCarrito(true));

    await cargarProducto();
});

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

async function obtenerCarritoActual() {
    try {
        const data = await FastMarket.obtenerCarrito();
        return (data.items || []).map(normalizarItemCarrito);
    } catch {
        return JSON.parse(localStorage.getItem("fastmarket_carrito") || sessionStorage.getItem("fastmarket_checkout_carrito") || "[]").map(normalizarItemCarrito);
    }
}

async function cargarProducto() {
    const id = new URLSearchParams(window.location.search).get("id");
    const mensaje = document.getElementById("mensaje-detalle");

    if (!id) {
        if (mensaje) mensaje.textContent = "Producto no encontrado.";
        return;
    }

    try {
        productoActual = await FastMarket.request(`/productos/${id}`);
        const todos = await FastMarket.request("/productos");
        relacionados = todos.filter((p) => Number(p.id) !== Number(id));
        pintarProducto();
        pintarMiniaturas();
        pintarRelacionados();
    } catch (error) {
        if (mensaje) mensaje.textContent = error.message;
    }
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

function pintarProducto() {
    if (!productoActual) return;

    const imagenes = obtenerImagenesProducto(productoActual);
    const imagen = imagenes[0] || "img/logo.png";
    const img = document.getElementById("producto-img");
    if (img) {
        img.src = imagen;
        img.alt = productoActual.nombre || "Producto";
        img.onerror = () => img.src = "img/logo.png";
    }

    setText("ruta-nombre", productoActual.nombre || "Producto");
    setText("producto-nombre", productoActual.nombre || "Producto");
    setText("producto-descripcion", productoActual.descripcion || "Producto disponible en FastMarket.");
    setText("producto-precio", FastMarket.money(productoActual.precio));
    setText("producto-precio-antes", productoActual.precioAntes ? FastMarket.money(productoActual.precioAntes) : "");

    pintarCaracteristicas();
    pintarInformacionAdicional();

    const etiqueta = document.getElementById("etiqueta-oferta");
    if (etiqueta) etiqueta.classList.toggle("oculto", !productoActual.oferta);

    const agregar = document.getElementById("agregar-carrito");
    const comprar = document.getElementById("comprar-ahora");
    const stock = Number(productoActual.stock || 0);
    const sinStock = stock <= 0;

    if (agregar) agregar.disabled = sinStock;
    if (comprar) comprar.disabled = sinStock;

    const stockEl = document.getElementById("producto-stock");
    if (stockEl) {
        stockEl.className = sinStock ? "stock-agotado" : "stock-disponible";
        stockEl.textContent = sinStock ? "Sin stock" : "En stock";
    }

    if (sinStock) setText("mensaje-detalle", "Producto sin stock disponible.");
}

function pintarMiniaturas() {
    const contenedor = document.getElementById("miniaturas-producto");
    const imagenPrincipal = document.getElementById("producto-img");
    if (!contenedor || !imagenPrincipal || !productoActual) return;

    const imagenes = obtenerImagenesProducto(productoActual);

    contenedor.innerHTML = imagenes.map((src, index) => `
        <button class="miniatura-producto ${index === 0 ? "activa" : ""}" type="button" aria-label="Imagen ${index + 1}">
            <img src="${FastMarket.escapeHTML(src)}" alt="${FastMarket.escapeHTML(productoActual.nombre || "Producto")}" onerror="this.src='img/logo.png'">
        </button>
    `).join("");

    contenedor.querySelectorAll(".miniatura-producto").forEach((boton, index) => {
        boton.addEventListener("click", () => {
            contenedor.querySelectorAll(".miniatura-producto").forEach((item) => item.classList.remove("activa"));
            boton.classList.add("activa");
            imagenPrincipal.src = imagenes[index] || "img/logo.png";
        });
    });
}

function pintarCaracteristicas() {
    const lista = document.getElementById("caracteristicas-producto");
    if (!lista || !productoActual) return;

    const caracteristicas = obtenerCaracteristicasProducto(productoActual);
    const stock = Number(productoActual.stock || 0);
    const items = [];

    if (productoActual.tipoProducto) items.push(`Tipo: ${productoActual.tipoProducto}`);
    Object.entries(caracteristicas).slice(0, 6).forEach(([nombre, valor]) => items.push(`${nombre}: ${valor}`));
    if (productoActual.detallesAdicionales) items.push(productoActual.detallesAdicionales);
    items.push(stock > 0 ? "Disponible para compra inmediata" : "Consulta disponibilidad antes de comprar");

    lista.innerHTML = items.map((item) => `<li>${FastMarket.escapeHTML(item)}</li>`).join("");
}

function obtenerCaracteristicasProducto(producto) {
    if (producto?.caracteristicas && typeof producto.caracteristicas === "object" && !Array.isArray(producto.caracteristicas)) {
        return Object.fromEntries(
            Object.entries(producto.caracteristicas)
                .map(([nombre, valor]) => [String(nombre || "").trim(), String(valor || "").trim()])
                .filter(([nombre, valor]) => nombre && valor)
        );
    }

    const resultado = {};
    const agregar = (nombre, valor) => {
        const limpio = String(valor || "").trim();
        if (limpio) resultado[nombre] = limpio;
    };
    agregar("Marca", producto?.marca);
    agregar("Modelo", producto?.modelo);
    agregar("Color", producto?.color);
    agregar("Material", producto?.material);
    agregar("Talla o medida", producto?.talla);
    agregar("Garantía", producto?.garantia);
    agregar("Condición", producto?.condicion);
    return resultado;
}

function pintarInformacionAdicional() {
    const contenedor = document.getElementById("informacion-producto");
    if (!contenedor || !productoActual) return;

    const filas = [
        ["Categoría", formatearCategoria(productoActual.categoria)]
    ];
    if (productoActual.tipoProducto) filas.push(["Tipo de producto", productoActual.tipoProducto]);
    Object.entries(obtenerCaracteristicasProducto(productoActual)).forEach(([nombre, valor]) => filas.push([nombre, valor]));

    contenedor.innerHTML = filas.map(([nombre, valor]) => `
        <div>
            <dt>${FastMarket.escapeHTML(nombre)}:</dt>
            <dd>${FastMarket.escapeHTML(valor)}</dd>
        </div>`).join("");
}

function formatearCategoria(valor) {
    const categorias = {
        moda: "Moda",
        tecnologia: "Tecnología",
        hogar: "Hogar",
        estudio: "Estudio",
        belleza: "Belleza",
        deportes: "Deportes",
        juguetes: "Juguetes"
    };
    return categorias[valor] || valor || "General";
}

function cambiarCantidad(valor) {
    if (!productoActual) return;
    const max = Number(productoActual.stock || 0);
    cantidad = Math.max(1, Math.min(max || 1, cantidad + valor));
    setText("cantidad", cantidad);
}

async function agregarCarrito(irCheckout) {
    if (!productoActual) return;

    if (window.FastMarketCart) {
        const agregado = await FastMarketCart.agregar(productoActual, cantidad);
        if (agregado) {
            setText("mensaje-detalle", "Producto agregado al carrito.");
            if (irCheckout) {
                FastMarketCart.guardarBackupCheckout();
                FastMarket.prepararCheckoutCarrito?.(FastMarketCart.obtenerItems?.() || [], null);
                window.location.href = "checkout.html";
            }
        }
        return;
    }

    let carrito = await obtenerCarritoActual();
    const existente = carrito.find((item) => Number(item.id) === Number(productoActual.id));
    const actual = existente ? existente.cantidad : 0;

    if (actual + cantidad > Number(productoActual.stock || 0)) {
        setText("mensaje-detalle", "No hay stock suficiente.");
        return;
    }

    if (existente) {
        existente.cantidad += cantidad;
    } else {
        carrito.push({
            id: productoActual.id,
            nombre: productoActual.nombre,
            precio: Number(productoActual.precio),
            imagen: obtenerImagenesProducto(productoActual)[0] || productoActual.imagen || "img/logo.png",
            stock: Number(productoActual.stock),
            cantidad
        });
    }

    FastMarket.prepararCheckoutCarrito?.(carrito, null);
    localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
    sessionStorage.setItem("fastmarket_checkout_carrito", JSON.stringify(carrito));
    await FastMarket.sincronizarCarrito(carrito, null).catch(() => {
        localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
        FastMarket.prepararCheckoutCarrito?.(carrito, null);
    });
    setText("mensaje-detalle", "Producto agregado al carrito.");

    if (irCheckout) {
        FastMarket.prepararCheckoutCarrito?.(carrito, null);
        window.location.href = "checkout.html";
    }
}

function pintarRelacionados() {
    const contenedor = document.getElementById("productos-relacionados");
    if (!contenedor || !productoActual) return;

    const lista = relacionados
        .filter((p) => p.categoria === productoActual.categoria)
        .slice(0, 4);

    const final = lista.length ? lista : relacionados.slice(0, 4);

    contenedor.innerHTML = final.map((p) => `
        <article class="card-relacionado">
            <img src="${FastMarket.escapeHTML(obtenerImagenesProducto(p)[0] || "img/logo.png")}" alt="${FastMarket.escapeHTML(p.nombre)}" onerror="this.src='img/logo.png'">
            <div>
                <h3>${FastMarket.escapeHTML(p.nombre)}</h3>
                <p>${FastMarket.money(p.precio)}</p>
                <a href="detalle-producto.html?id=${p.id}">Ver producto</a>
            </div>
        </article>
    `).join("");
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value || "";
}
