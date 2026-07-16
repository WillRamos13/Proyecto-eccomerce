document.addEventListener("DOMContentLoaded", async () => {
    FastMarket.activarBuscador("buscador", "busqueda");
    FastMarket.activarMenuCliente();
    FastMarket.mostrarPanelCliente();

    const usuario = FastMarket.requireCliente(false);
    if (!usuario) {
        mostrarSinSesion();
        return;
    }

    await procesarRetornoMercadoPago();
    await cargarPedidos(usuario);
});

async function procesarRetornoMercadoPago() {
    const params = new URLSearchParams(window.location.search);
    const status = params.get("status");
    const paymentId = params.get("payment_id") || params.get("paymentId");
    const pedidoId = Number(params.get("pedidoId"));

    if (status === "approved" && paymentId && pedidoId) {
        try {
            await FastMarket.request(`/pagos/pedidos/${pedidoId}/confirmar?paymentId=${encodeURIComponent(paymentId)}`, {
                method: "POST",
                auth: true
            });
            await FastMarket.limpiarCarritoUsuario();
            FastMarket.notify("Pago verificado correctamente. Tu pedido fue confirmado.", "ok");
        } catch (error) {
            FastMarket.notify(`No se pudo verificar el pago: ${error.message}`, "error");
        }
    } else if (status === "pending") {
        FastMarket.notify("El pago está pendiente de confirmación.", "info");
    } else if (status === "failure") {
        FastMarket.notify("El pago no se completó. Puedes reintentarlo desde el pedido pendiente.", "error");
    }

    if (status) {
        const limpio = new URL(window.location.href);
        ["status", "payment_id", "paymentId", "collection_id", "collection_status", "merchant_order_id", "preference_id", "site_id", "processing_mode", "merchant_account_id"].forEach((key) => limpio.searchParams.delete(key));
        history.replaceState({}, "", limpio.pathname + (limpio.searchParams.toString() ? `?${limpio.searchParams}` : ""));
    }
}

let pedidos = [];

async function cargarPedidos(usuario) {
    const contenedor = document.getElementById("contenedor-pedidos");
    try {
        pedidos = await FastMarket.request(`/pedidos/usuario/${usuario.id}`, { auth: true });
        pintarResumen();
        pintarLista();

        const codigo = new URLSearchParams(window.location.search).get("pedido");
        const seleccionado = codigo
            ? pedidos.find((p) => p.codigo === codigo)
            : pedidos[0];

        if (seleccionado) mostrarDetalle(seleccionado);
        else mostrarVacio();
    } catch (error) {
        if (contenedor) contenedor.innerHTML = `<p class="mensaje-error">${FastMarket.escapeHTML(error.message)}</p>`;
        mostrarVacio();
    }
}

function mostrarSinSesion() {
    const contenedor = document.getElementById("contenedor-pedidos");
    if (contenedor) {
        contenedor.innerHTML = `
            <div class="pedido-card">
                <h3>Debes iniciar sesión</h3>
                <p>Inicia sesión para revisar tus pedidos reales.</p>
                <a href="login.html">Ir a iniciar sesión</a>
            </div>`;
    }
    mostrarVacio();
}

function pintarResumen() {
    const totalPedidos = document.getElementById("total-pedidos");
    const pendientes = document.getElementById("pedidos-pendientes");
    const camino = document.getElementById("pedidos-camino");
    const entregados = document.getElementById("pedidos-entregados");

    if (totalPedidos) totalPedidos.textContent = pedidos.length;
    if (pendientes) pendientes.textContent = pedidos.filter((p) => ["PENDIENTE", "CONFIRMADO", "PREPARANDO"].includes(FastMarket.normalizarEstado(p.estado))).length;
    if (camino) camino.textContent = pedidos.filter((p) => FastMarket.normalizarEstado(p.estado) === "CAMINO").length;
    if (entregados) entregados.textContent = pedidos.filter((p) => FastMarket.normalizarEstado(p.estado) === "ENTREGADO").length;
}

function pintarLista() {
    const contenedor = document.getElementById("contenedor-pedidos");
    if (!contenedor) return;

    contenedor.innerHTML = "";

    if (pedidos.length === 0) {
        contenedor.innerHTML = `
            <div class="pedido-card">
                <h3>No tienes pedidos</h3>
                <p>Cuando finalices una compra, aparecerá aquí.</p>
                <a href="productos.html">Ver productos</a>
            </div>`;
        return;
    }

    pedidos.forEach((pedido) => {
        const card = document.createElement("article");
        card.className = "pedido-card";
        card.innerHTML = `
            <h3>${FastMarket.escapeHTML(pedido.codigo)}</h3>
            <p>${formatearFecha(pedido.fecha)}</p>
            <strong>${FastMarket.money(pedido.total)}</strong>
            <span class="estado-${FastMarket.normalizarEstado(pedido.estado).toLowerCase()}">${FastMarket.estadoTexto(pedido.estado)}</span>
            <small>Pago: ${FastMarket.escapeHTML(textoEstadoPago(pedido.estadoPago))}</small>
            <button>Ver detalle</button>`;
        card.querySelector("button").addEventListener("click", () => mostrarDetalle(pedido));
        contenedor.appendChild(card);
    });
}

function mostrarDetalle(pedido) {
    const detalleVacio = document.getElementById("detalle-vacio");
    const detalleContenido = document.getElementById("detalle-contenido");

    if (detalleVacio) detalleVacio.classList.add("oculto");
    if (detalleContenido) detalleContenido.classList.remove("oculto");

    setText("detalle-codigo", pedido.codigo);
    setText("detalle-fecha", formatearFecha(pedido.fecha));
    setText("detalle-estado", FastMarket.estadoTexto(pedido.estado));
    setText("detalle-cliente", pedido.usuarioNombre);
    setText("detalle-total", FastMarket.money(pedido.total));
    setText("detalle-metodo", pedido.metodoPago || "Pago contra entrega");
    setText("detalle-pago", textoEstadoPago(pedido.estadoPago));
    setText("detalle-estimada", estimarEntrega(pedido));
    configurarAccionesPago(pedido);
    setText("detalle-direccion", `${pedido.direccionEntrega || ""} ${pedido.referenciaEntrega ? " - " + pedido.referenciaEntrega : ""}`);

    const productos = document.getElementById("detalle-productos");
    if (productos) {
        productos.innerHTML = (pedido.items || []).map((item) => `
            <div class="producto-pedido">
                <img src="${FastMarket.escapeHTML(item.imagen || "img/logo.png")}" alt="${FastMarket.escapeHTML(item.productoNombre)}" onerror="this.src='img/logo.png'">
                <div>
                    <h4>${FastMarket.escapeHTML(item.productoNombre)}</h4>
                    <p>Cantidad: ${item.cantidad}</p>
                    <strong>${FastMarket.money(item.subtotal)}</strong>
                </div>
            </div>
        `).join("");
    }

    activarPasos(pedido.estado);

    const mapa = document.getElementById("mapa-pedido");
    if (mapa) {
        const dir = encodeURIComponent(pedido.direccionEntrega || "Ica Perú");
        mapa.src = `https://www.google.com/maps?q=${dir}&output=embed`;
    }
}

function configurarAccionesPago(pedido) {
    const acciones = document.getElementById("acciones-pago-pedido");
    const reintentar = document.getElementById("btn-reintentar-pago");
    const cancelar = document.getElementById("btn-cancelar-reserva");
    const pendienteMercadoPago = pedido?.metodoPago === "Mercado Pago"
        && pedido?.estadoPago === "PENDIENTE"
        && FastMarket.normalizarEstado(pedido?.estado) !== "CANCELADO";

    acciones?.classList.toggle("oculto", !pendienteMercadoPago);
    if (!pendienteMercadoPago) return;

    if (reintentar) {
        reintentar.onclick = () => {
            window.location.href = `mercado-pago.html?pedidoId=${encodeURIComponent(pedido.id)}`;
        };
    }
    if (cancelar) {
        cancelar.onclick = async () => {
            const confirmado = await FastMarket.confirmAction("¿Cancelar la reserva? El stock y el cupón serán devueltos.");
            if (!confirmado) return;
            cancelar.disabled = true;
            try {
                await FastMarket.request(`/pagos/pedidos/${pedido.id}/cancelar`, { method: "POST", auth: true });
                FastMarket.notify("Reserva cancelada correctamente.", "info");
                const usuario = FastMarket.getCliente();
                if (usuario) await cargarPedidos(usuario);
            } catch (error) {
                FastMarket.notify(error.message, "error");
                cancelar.disabled = false;
            }
        };
    }
}

function mostrarVacio() {
    const detalleVacio = document.getElementById("detalle-vacio");
    const detalleContenido = document.getElementById("detalle-contenido");
    if (detalleVacio) detalleVacio.classList.remove("oculto");
    if (detalleContenido) detalleContenido.classList.add("oculto");
}

function activarPasos(estado) {
    const actual = FastMarket.normalizarEstado(estado);
    const orden = {
        PENDIENTE: 0,
        CONFIRMADO: 1,
        PREPARANDO: 1,
        CAMINO: 2,
        ENTREGADO: 4,
        CANCELADO: -1
    };
    const valor = orden[actual] ?? 0;

    document.querySelectorAll(".paso-seguimiento").forEach((paso) => paso.classList.remove("activo"));

    if (valor >= 1) document.querySelector('[data-paso="preparacion"]')?.classList.add("activo");
    if (valor >= 2) document.querySelector('[data-paso="camino"]')?.classList.add("activo");
    if (valor >= 3) document.querySelector('[data-paso="reparto"]')?.classList.add("activo");
    if (valor >= 4) document.querySelector('[data-paso="entregado"]')?.classList.add("activo");
}

function estimarEntrega(pedido) {
    const estado = FastMarket.normalizarEstado(pedido.estado);
    if (estado === "ENTREGADO") return "Entregado";
    if (estado === "CANCELADO") return "Cancelado";
    return pedido.horarioEntrega ? `Horario preferido: ${pedido.horarioEntrega}` : "Por coordinar";
}

function textoEstadoPago(estado) {
    const textos = { PENDIENTE: "Pendiente", APROBADO: "Aprobado", RECHAZADO: "Rechazado", CANCELADO: "Cancelado", REEMBOLSADO: "Reembolsado" };
    return textos[estado] || "Pendiente";
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value || "";
}

function formatearFecha(fecha) {
    if (!fecha) return "";
    return new Date(fecha).toLocaleString("es-PE", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}
