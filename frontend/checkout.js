document.addEventListener("DOMContentLoaded", () => {
    FastMarket.activarBuscador("buscador", "busqueda");
    FastMarket.activarMenuCliente();
    const form = document.getElementById("form-checkout");
    const mensajeLogin = document.getElementById("mensaje-login");
    const mensajeCheckout = document.getElementById("mensaje-checkout");
    const btnConfirmar = document.getElementById("btn-confirmar");
    const pagoInputs = document.querySelectorAll('input[name="pago"]');

    const nombre = document.getElementById("nombre");
    const correo = document.getElementById("correo");
    const telefono = document.getElementById("telefono");
    const direccion = document.getElementById("direccion");
    const referencia = document.getElementById("referencia");
    const horario = document.getElementById("horario");
    const inputCupon = document.getElementById("codigo-cupon");
    const btnCupon = document.getElementById("aplicar-cupon");
    const mensajeCupon = document.getElementById("mensaje-cupon");

    let carrito = [];
    let cuponAplicado = null;
    let costoEnvio = 8;

    cargarDatosCliente();
    inicializarCheckout();

    if (form) form.addEventListener("submit", confirmarPedido);
    if (btnCupon) btnCupon.addEventListener("click", aplicarCupon);
    pagoInputs.forEach((input) => input.addEventListener("change", actualizarTextoBotonPago));
    actualizarTextoBotonPago();

    function actualizarTextoBotonPago() {
        const pago = document.querySelector('input[name="pago"]:checked')?.value;
        if (!btnConfirmar) return;
        btnConfirmar.textContent = pago === "Mercado Pago" ? "Pagar con Mercado Pago" : "Confirmar pedido";
    }

    async function inicializarCheckout() {
        await cargarConfigPublica();
        await cargarCarritoPersistente();
    }

    async function cargarConfigPublica() {
        try {
            const config = await FastMarket.obtenerConfigPublica();
            costoEnvio = Number(config?.costoEnvio ?? 8);
            if (!Number.isFinite(costoEnvio) || costoEnvio < 0) costoEnvio = 8;
        } catch {
            costoEnvio = 8;
        }
    }

    function normalizarItemCarrito(item) {
        return {
            id: Number(item.productoId || item.id),
            nombre: item.nombre || item.productoNombre || "Producto",
            imagen: item.imagen || "img/logo.png",
            precio: Number(item.precio ?? item.precioUnitario ?? 0),
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
        const leerJSON = (valor, defecto) => {
            try { return valor ? JSON.parse(valor) : defecto; } catch { return defecto; }
        };
        const checkoutBackup = leerJSON(sessionStorage.getItem("fastmarket_checkout_carrito"), []);
        const backup = leerJSON(sessionStorage.getItem("fastmarket_carrito_backup"), []);
        const local = leerJSON(localStorage.getItem("fastmarket_carrito"), []);
        const cuponLocal = FastMarket.obtenerCuponLocal?.() || leerJSON(localStorage.getItem("fastmarket_cupon") || sessionStorage.getItem("fastmarket_checkout_cupon"), null);
        const respaldoLocal = (checkoutBackup.length ? checkoutBackup : (local.length ? local : backup)).map(normalizarItemCarrito);
        const localEsMirror = usuario && FastMarket.carritoLocalPerteneceAlUsuario?.(usuario.id);

        let data = { items: [], cuponCodigo: null, descuento: 0 };
        try {
            data = await FastMarket.obtenerCarrito();
        } catch {
            data = { items: [], cuponCodigo: null, descuento: 0 };
        }
        const remoto = (data.items || []).map(normalizarItemCarrito);

        try {
            if (usuario && !localEsMirror && respaldoLocal.length) {
                const combinado = combinarCarritos(remoto, respaldoLocal);
                const sincronizado = await FastMarket.sincronizarCarrito(combinado, cuponLocal?.codigo || data.cuponCodigo || null);
                carrito = (sincronizado.items || []).map(normalizarItemCarrito);
                cuponAplicado = sincronizado.cuponCodigo ? { codigo: sincronizado.cuponCodigo, descuento: Number(sincronizado.descuento || 0), descripcion: cuponLocal?.descripcion || "" } : cuponLocal;
            } else if (usuario) {
                carrito = remoto.length ? remoto : respaldoLocal;
                cuponAplicado = data.cuponCodigo ? { codigo: data.cuponCodigo, descuento: Number(data.descuento || 0), descripcion: cuponLocal?.descripcion || "" } : cuponLocal;
            } else {
                carrito = respaldoLocal;
                cuponAplicado = cuponLocal;
            }
        } catch {
            carrito = respaldoLocal.length ? respaldoLocal : remoto;
            cuponAplicado = cuponLocal || (data.cuponCodigo ? { codigo: data.cuponCodigo, descuento: Number(data.descuento || 0) } : null);
        }

        FastMarket.prepararCheckoutCarrito?.(carrito, cuponAplicado || null);

        if (inputCupon && cuponAplicado?.codigo) inputCupon.value = cuponAplicado.codigo;
        mostrarResumen();
    }

    function cargarDatosCliente() {
        const usuario = FastMarket.getCliente();
        if (!usuario) {
            if (mensajeLogin) mensajeLogin.classList.remove("oculto");
            if (form) form.classList.add("oculto");
            return;
        }
        if (mensajeLogin) mensajeLogin.classList.add("oculto");
        if (form) form.classList.remove("oculto");
        nombre.value = usuario.nombre || "";
        correo.value = usuario.correo || "";
        telefono.value = usuario.telefono || "";
        const primeraDireccion = (usuario.direcciones || [])[0];
        if (primeraDireccion) {
            direccion.value = primeraDireccion.direccion || primeraDireccion;
            referencia.value = primeraDireccion.referencia || "";
        }
    }

    async function aplicarCupon() {
        const codigo = inputCupon?.value.trim().toUpperCase();
        if (!codigo) {
            cuponAplicado = null;
            await FastMarket.sincronizarCarrito(carrito, null);
            mostrarMensajeCupon("Cupón eliminado.", "ok");
            mostrarResumen();
            return;
        }
        if (!carrito.length) {
            mostrarMensajeCupon("Agrega productos antes de aplicar un cupón.", "error");
            return;
        }
        try {
            mostrarMensajeCupon("Validando cupón...", "ok");
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
            mostrarMensajeCupon(`${respuesta.mensaje} Descuento: ${FastMarket.money(respuesta.descuento)}`, "ok");
            mostrarResumen();
        } catch (error) {
            cuponAplicado = null;
            await FastMarket.sincronizarCarrito(carrito, null);
            mostrarMensajeCupon(error.message, "error");
            mostrarResumen();
        }
    }

    function mostrarMensajeCupon(texto, tipo) {
        if (!mensajeCupon) return;
        mensajeCupon.textContent = texto;
        mensajeCupon.classList.remove("ok", "error");
        mensajeCupon.classList.add(tipo);
    }

    function mostrarResumen() {
        const lista = document.getElementById("lista-checkout");
        const subtotalTexto = document.getElementById("subtotal");
        const envioTexto = document.getElementById("envio");
        const totalTexto = document.getElementById("total");
        const descuentoTexto = document.getElementById("descuento");
        const filaDescuento = document.getElementById("fila-descuento");

        if (!lista) return;
        lista.innerHTML = "";

        if (carrito.length === 0) {
            lista.innerHTML = `<p>No tienes productos en el carrito.</p>`;
            if (subtotalTexto) subtotalTexto.textContent = FastMarket.money(0);
            if (envioTexto) envioTexto.textContent = FastMarket.money(0);
            if (descuentoTexto) descuentoTexto.textContent = `- ${FastMarket.money(0)}`;
            if (totalTexto) totalTexto.textContent = FastMarket.money(0);
            filaDescuento?.classList.add("oculto");
            return;
        }

        carrito.forEach((item) => {
            const div = document.createElement("div");
            div.className = "item-checkout";
            div.innerHTML = `
                <img src="${FastMarket.escapeHTML(item.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(item.nombre)}" onerror="this.src='img/logo.png'">
                <div>
                    <h4>${FastMarket.escapeHTML(item.nombre)}</h4>
                    <p>Cantidad: ${item.cantidad}</p>
                    <strong>${FastMarket.money(Number(item.precio) * Number(item.cantidad))}</strong>
                </div>`;
            lista.appendChild(div);
        });

        const subtotal = carrito.reduce((s, item) => s + Number(item.precio) * Number(item.cantidad), 0);
        const descuento = Number(cuponAplicado?.descuento || 0);
        const envioCalculado = subtotal >= 250 ? 0 : costoEnvio;
        if (subtotalTexto) subtotalTexto.textContent = FastMarket.money(subtotal);
        if (envioTexto) envioTexto.textContent = FastMarket.money(envioCalculado);
        if (descuentoTexto) descuentoTexto.textContent = `- ${FastMarket.money(descuento)}`;
        if (filaDescuento) filaDescuento.classList.toggle("oculto", descuento <= 0);
        if (totalTexto) totalTexto.textContent = FastMarket.money(Math.max(0, subtotal - descuento + envioCalculado));
    }

    async function confirmarPedido(e) {
        e.preventDefault();
        const usuario = FastMarket.requireCliente(true);
        if (!usuario) return;
        if (carrito.length === 0) {
            mensajeCheckout.textContent = "Tu carrito está vacío.";
            return;
        }
        if (!direccion.value.trim()) {
            mensajeCheckout.textContent = "Ingresa una dirección de entrega.";
            return;
        }
        const pago = document.querySelector('input[name="pago"]:checked')?.value || "Pago contra entrega";
        const payload = {
            direccionEntrega: direccion.value.trim(),
            referenciaEntrega: referencia.value.trim(),
            horarioEntrega: horario.value,
            metodoPago: pago,
            telefonoEntrega: telefono.value.trim(),
            cuponCodigo: cuponAplicado?.codigo || "",
            items: carrito.map((item) => ({ productoId: Number(item.id), cantidad: Number(item.cantidad) }))
        };
        try {
            mensajeCheckout.textContent = "Confirmando pedido...";
            btnConfirmar.disabled = true;
            const pedido = await FastMarket.request(`/pedidos/usuario/${usuario.id}`, { method: "POST", body: payload, auth: true });

            // Para Mercado Pago el carrito se conserva hasta que el backend verifique el pago.
            if (pago === "Mercado Pago") {
                window.location.href = `mercado-pago.html?pedidoId=${encodeURIComponent(pedido.id)}`;
                return;
            }

            await FastMarket.limpiarCarritoUsuario();
            carrito = [];
            mensajeCheckout.textContent = `Pedido ${pedido.codigo} creado correctamente.`;
            window.location.href = `pedidos.html?pedido=${encodeURIComponent(pedido.codigo)}`;
        } catch (error) {
            mensajeCheckout.textContent = error.message;
            btnConfirmar.disabled = false;
        }
    }
});
