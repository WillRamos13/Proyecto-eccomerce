const FastMarketCart = (() => {
    let carrito = [];
    let cuponAplicado = null;
    let cargado = false;
    let guardando = null;

    function parseJSON(valor, defecto) {
        try { return valor ? JSON.parse(valor) : defecto; } catch { return defecto; }
    }

    function obtenerLocal() {
        return parseJSON(localStorage.getItem("fastmarket_carrito"), []) || [];
    }

    function obtenerBackupCheckout() {
        return parseJSON(sessionStorage.getItem("fastmarket_checkout_carrito") || sessionStorage.getItem("fastmarket_carrito_backup"), []) || [];
    }

    function obtenerCuponLocal() {
        return parseJSON(localStorage.getItem("fastmarket_cupon") || sessionStorage.getItem("fastmarket_checkout_cupon"), null);
    }

    function normalizarItem(item) {
        return {
            id: Number(item.productoId || item.id),
            nombre: item.nombre || item.productoNombre || "Producto",
            precio: Number(item.precio || item.precioUnitario || 0),
            imagen: item.imagen || "img/logo.png",
            stock: Number(item.stockDisponible ?? item.stock ?? 999999),
            cantidad: Number(item.cantidad || 1)
        };
    }

    function combinarCarritos(...listas) {
        const mapa = new Map();
        listas.flat().forEach((item) => {
            const normalizado = normalizarItem(item);
            if (!normalizado.id || normalizado.cantidad <= 0) return;
            const actual = mapa.get(normalizado.id);
            if (actual) {
                actual.cantidad = Math.min(Number(normalizado.stock || actual.stock || 999999), actual.cantidad + normalizado.cantidad);
                actual.stock = Math.max(Number(actual.stock || 0), Number(normalizado.stock || 0));
            } else {
                mapa.set(normalizado.id, normalizado);
            }
        });
        return Array.from(mapa.values());
    }

    function renderEstructura() {
        let overlay = document.getElementById("overlay-carrito");
        if (!overlay) {
            overlay = document.createElement("div");
            overlay.id = "overlay-carrito";
            document.body.appendChild(overlay);
        }
        overlay.className = "fm-cart-overlay";

        let panel = document.getElementById("panel-carrito");
        if (!panel) {
            panel = document.createElement("aside");
            panel.id = "panel-carrito";
            document.body.appendChild(panel);
        }
        panel.className = "fm-cart-panel";
        panel.innerHTML = `
            <div class="fm-cart-head">
                <div>
                    <h2>Mi carrito</h2>
                    <p id="carrito-subtitulo">Productos seleccionados</p>
                </div>
                <button type="button" class="fm-cart-close" id="cerrar-carrito" aria-label="Cerrar carrito">×</button>
            </div>
            <div class="fm-cart-body" id="carrito-lista"></div>
            <div class="fm-cart-foot">
                <div class="fm-cart-coupon">
                    <label for="cupon-carrito">Cupón de descuento</label>
                    <div class="fm-cart-coupon-row">
                        <input type="text" id="cupon-carrito" placeholder="Código de cupón">
                        <button type="button" id="aplicar-cupon-carrito">Aplicar</button>
                    </div>
                    <p id="mensaje-cupon-carrito" class="fm-cart-message"></p>
                </div>
                <div class="fm-cart-totals">
                    <div class="fm-cart-total-row"><span>Subtotal</span><strong id="subtotal-carrito">S/ 0.00</strong></div>
                    <div class="fm-cart-total-row oculto" id="fila-descuento-carrito"><span>Descuento</span><strong id="descuento-carrito">- S/ 0.00</strong></div>
                    <div class="fm-cart-total-row final"><span>Total</span><strong id="total-carrito">S/ 0.00</strong></div>
                </div>
                <div class="fm-cart-buttons">
                    <button type="button" class="fm-cart-checkout" id="finalizar-compra">Finalizar compra</button>
                    <button type="button" class="fm-cart-clear" id="vaciar-carrito">Vaciar</button>
                    <a class="fm-cart-continue" href="productos.html">Seguir comprando</a>
                </div>
            </div>`;
    }

    function prepararBotonHeader() {
        const boton = document.getElementById("carrito");
        if (!boton || boton.dataset.fmCartHeader === "true") return;
        boton.dataset.fmCartHeader = "true";
        boton.classList.add("fm-cart-trigger");
        if (!boton.querySelector("#contador-carrito") && !boton.querySelector(".contador-carrito")) {
            const contador = document.createElement("span");
            contador.id = "contador-carrito";
            contador.className = "contador-carrito";
            contador.textContent = "0";
            boton.appendChild(contador);
        }
        boton.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            abrir();
        });
    }

    function eventosPanel() {
        document.getElementById("cerrar-carrito")?.addEventListener("click", cerrar);
        document.getElementById("overlay-carrito")?.addEventListener("click", cerrar);
        document.getElementById("vaciar-carrito")?.addEventListener("click", async () => {
            carrito = [];
            cuponAplicado = null;
            await guardar();
            FastMarket.notify("Carrito vacío.", "info");
        });
        document.getElementById("finalizar-compra")?.addEventListener("click", irCheckout);
        document.getElementById("aplicar-cupon-carrito")?.addEventListener("click", aplicarCupon);
        document.getElementById("carrito-lista")?.addEventListener("click", async (e) => {
            const btn = e.target.closest("[data-cart-action]");
            if (!btn) return;
            await cambiarCantidad(Number(btn.dataset.id), btn.dataset.cartAction);
        });
    }

    async function cargar() {
        const usuario = FastMarket.getCliente();
        const localGuardado = obtenerLocal();
        const backupCheckout = obtenerBackupCheckout();
        const respaldo = (backupCheckout.length ? backupCheckout : localGuardado).map(normalizarItem);
        const localEsMirror = usuario && FastMarket.carritoLocalPerteneceAlUsuario?.(usuario.id);
        const cuponLocal = obtenerCuponLocal();
        try {
            const data = await FastMarket.obtenerCarrito();
            const remoto = (data.items || []).map(normalizarItem);
            if (usuario && !localEsMirror && respaldo.length) {
                carrito = combinarCarritos(remoto, respaldo);
                const sincronizado = await FastMarket.sincronizarCarrito(carrito, cuponLocal?.codigo || data.cuponCodigo || null);
                carrito = (sincronizado.items || []).map(normalizarItem);
                cuponAplicado = sincronizado.cuponCodigo ? { codigo: sincronizado.cuponCodigo, descuento: Number(sincronizado.descuento || 0), descripcion: cuponLocal?.descripcion || "" } : cuponLocal;
            } else if (usuario) {
                carrito = remoto.length ? remoto : respaldo;
                cuponAplicado = data.cuponCodigo ? { codigo: data.cuponCodigo, descuento: Number(data.descuento || 0), descripcion: cuponLocal?.descripcion || "" } : cuponLocal;
            } else {
                carrito = respaldo;
                cuponAplicado = cuponLocal;
            }
        } catch {
            carrito = respaldo;
            cuponAplicado = cuponLocal;
        }
        cargado = true;
        guardarBackupCheckout();
        pintar();
        return carrito;
    }

    async function guardar() {
        localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
        if (cuponAplicado?.codigo) localStorage.setItem("fastmarket_cupon", JSON.stringify({ codigo: cuponAplicado.codigo, descuento: Number(cuponAplicado.descuento || 0), descripcion: cuponAplicado.descripcion || "" }));
        else localStorage.removeItem("fastmarket_cupon");
        guardarBackupCheckout();

        if (guardando) await guardando.catch(() => {});
        if (!window.FastMarket?.getCliente?.()) {
            pintar();
            return;
        }
        guardando = FastMarket.sincronizarCarrito(carrito, carrito.length ? cuponAplicado || null : null)
            .then((data) => {
                if (FastMarket.getCliente()) {
                    carrito = (data.items || []).map(normalizarItem);
                    cuponAplicado = data.cuponCodigo ? { codigo: data.cuponCodigo, descuento: Number(data.descuento || 0) } : null;
                }
                guardarBackupCheckout();
                pintar();
                return data;
            })
            .catch(() => {
                localStorage.setItem("fastmarket_carrito", JSON.stringify(carrito));
                guardarBackupCheckout();
            })
            .finally(() => { guardando = null; });
        await guardando;
    }

    function guardarBackupCheckout() {
        if (window.FastMarket?.prepararCheckoutCarrito) {
            FastMarket.prepararCheckoutCarrito(carrito || [], cuponAplicado || null);
            return;
        }
        sessionStorage.setItem("fastmarket_checkout_carrito", JSON.stringify(carrito || []));
        sessionStorage.setItem("fastmarket_carrito_backup", JSON.stringify(carrito || []));
        if (cuponAplicado?.codigo) sessionStorage.setItem("fastmarket_checkout_cupon", JSON.stringify({ codigo: cuponAplicado.codigo, descuento: Number(cuponAplicado.descuento || 0), descripcion: cuponAplicado.descripcion || "" }));
        else sessionStorage.removeItem("fastmarket_checkout_cupon");
    }

    function pintar() {
        const lista = document.getElementById("carrito-lista");
        const total = document.getElementById("total-carrito");
        const subtotalEl = document.getElementById("subtotal-carrito");
        const descuentoEl = document.getElementById("descuento-carrito");
        const filaDescuento = document.getElementById("fila-descuento-carrito");
        const inputCupon = document.getElementById("cupon-carrito");
        if (inputCupon && cuponAplicado?.codigo && inputCupon.value !== cuponAplicado.codigo) inputCupon.value = cuponAplicado.codigo;
        actualizarContador();
        if (!lista || !total) return;

        if (!carrito.length) {
            lista.innerHTML = `<div class="fm-cart-empty"><strong>Tu carrito está vacío.</strong><p>Agrega productos para continuar con tu compra.</p></div>`;
            if (subtotalEl) subtotalEl.textContent = FastMarket.money(0);
            if (descuentoEl) descuentoEl.textContent = `- ${FastMarket.money(0)}`;
            filaDescuento?.classList.add("oculto");
            total.textContent = FastMarket.money(0);
            return;
        }

        lista.innerHTML = carrito.map((item) => `
            <article class="fm-cart-item">
                <img src="${FastMarket.escapeHTML(item.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(item.nombre)}" onerror="this.src='img/logo.png'">
                <div class="fm-cart-info">
                    <h3>${FastMarket.escapeHTML(item.nombre)}</h3>
                    <p>${FastMarket.money(item.precio)}</p>
                    <div class="fm-cart-actions">
                        <button type="button" data-cart-action="restar" data-id="${item.id}">−</button>
                        <span>${item.cantidad}</span>
                        <button type="button" data-cart-action="sumar" data-id="${item.id}">+</button>
                        <button type="button" class="fm-cart-remove" data-cart-action="eliminar" data-id="${item.id}">Quitar</button>
                    </div>
                </div>
            </article>`).join("");

        const subtotal = carrito.reduce((s, item) => s + Number(item.precio) * Number(item.cantidad), 0);
        const descuento = Number(cuponAplicado?.descuento || 0);
        if (subtotalEl) subtotalEl.textContent = FastMarket.money(subtotal);
        if (descuentoEl) descuentoEl.textContent = `- ${FastMarket.money(descuento)}`;
        filaDescuento?.classList.toggle("oculto", descuento <= 0);
        total.textContent = FastMarket.money(Math.max(0, subtotal - descuento));
    }

    function actualizarContador() {
        const totalCantidad = carrito.reduce((s, item) => s + Number(item.cantidad || 0), 0);
        document.querySelectorAll("#contador-carrito, .contador-carrito").forEach((el) => el.textContent = String(totalCantidad));
    }

    async function agregar(producto, cantidad = 1) {
        if (!cargado) await cargar();
        const stock = Number(producto.stockDisponible ?? producto.stock ?? 999999);
        const id = Number(producto.productoId || producto.id);
        const actual = carrito.find((item) => Number(item.id) === id);
        const cantidadActual = actual ? Number(actual.cantidad) : 0;
        if (cantidadActual + Number(cantidad) > stock) {
            FastMarket.notify("No hay stock suficiente para ese producto.", "warning");
            return false;
        }
        if (actual) {
            actual.cantidad += Number(cantidad);
        } else {
            const imagen = Array.isArray(producto.imagenes) && producto.imagenes.length ? producto.imagenes[0] : producto.imagen;
            carrito.push({
                id,
                nombre: producto.nombre || "Producto",
                precio: Number(producto.precio || 0),
                imagen: imagen || "img/logo.png",
                stock,
                cantidad: Number(cantidad)
            });
        }
        await guardar();
        abrir();
        FastMarket.notify("Producto agregado al carrito.", "success");
        return true;
    }

    async function cambiarCantidad(id, accion) {
        const item = carrito.find((p) => Number(p.id) === Number(id));
        if (!item) return;
        if (accion === "sumar") {
            if (Number(item.cantidad) >= Number(item.stock || 999999)) {
                FastMarket.notify("No hay más stock disponible.", "warning");
                return;
            }
            item.cantidad++;
        }
        if (accion === "restar") item.cantidad--;
        if (accion === "eliminar" || item.cantidad <= 0) carrito = carrito.filter((p) => Number(p.id) !== Number(id));
        if (!carrito.length) cuponAplicado = null;
        await guardar();
    }

    async function aplicarCupon() {
        const input = document.getElementById("cupon-carrito");
        const mensaje = document.getElementById("mensaje-cupon-carrito");
        const codigo = input?.value.trim().toUpperCase();
        const pintarMensaje = (texto, tipo) => {
            if (!mensaje) return;
            mensaje.textContent = texto;
            mensaje.classList.remove("ok", "error");
            if (tipo) mensaje.classList.add(tipo);
        };
        if (!codigo) {
            cuponAplicado = null;
            await guardar();
            pintarMensaje("Cupón eliminado.", "ok");
            return;
        }
        if (!carrito.length) {
            pintarMensaje("Agrega productos antes de aplicar un cupón.", "error");
            return;
        }
        try {
            pintarMensaje("Validando cupón...", "ok");
            const respuesta = await FastMarket.request("/cupones/aplicar", {
                method: "POST",
                auth: true,
                body: { codigo, items: carrito.map((item) => ({ productoId: Number(item.id), cantidad: Number(item.cantidad) })) }
            });
            cuponAplicado = respuesta;
            await guardar();
            pintarMensaje(`${respuesta.mensaje} Descuento: ${FastMarket.money(respuesta.descuento)}`, "ok");
        } catch (error) {
            cuponAplicado = null;
            await guardar();
            pintarMensaje(error.message, "error");
        }
    }

    async function irCheckout() {
        if (!cargado) await cargar();
        if (!carrito.length) {
            FastMarket.notify("Tu carrito está vacío.", "warning");
            return;
        }
        FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
        await guardar();
        FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);
        window.location.href = "checkout.html";
    }

    function abrir() {
        renderEstructura();
        eventosPanel();
        pintar();
        document.getElementById("overlay-carrito")?.classList.add("activo");
        document.getElementById("panel-carrito")?.classList.add("activo");
    }

    function cerrar() {
        document.getElementById("overlay-carrito")?.classList.remove("activo");
        document.getElementById("panel-carrito")?.classList.remove("activo");
    }

    function inicializar() {
        renderEstructura();
        prepararBotonHeader();
        eventosPanel();
        cargar();
    }

    document.addEventListener("DOMContentLoaded", inicializar);

    return { inicializar, cargar, guardar, agregar, abrir, cerrar, actualizarContador, obtenerItems: () => [...carrito], guardarBackupCheckout };
})();
