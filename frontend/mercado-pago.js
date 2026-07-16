document.addEventListener("DOMContentLoaded", async () => {
    FastMarket.activarMenuCliente();

    const usuario = FastMarket.requireCliente(true);
    if (!usuario) return;

    const btnProcesar = document.getElementById("btn-procesar-pago");
    const btnCancelar = document.getElementById("btn-cancelar-pago");
    const estadoPago = document.getElementById("estado-pago");
    const params = new URLSearchParams(window.location.search);
    const pedidoId = Number(params.get("pedidoId"));
    const retorno = params.get("status");
    let pedido = null;

    if (!pedidoId) {
        mostrarError("No se encontró el pedido que deseas pagar.");
        btnProcesar.disabled = true;
        return;
    }

    try {
        pedido = await FastMarket.request(`/pedidos/${pedidoId}`, { auth: true });
        mostrarDatosServidor(pedido);
        manejarEstadoRetorno(retorno);
    } catch (error) {
        mostrarError(error.message);
        btnProcesar.disabled = true;
    }

    btnProcesar.addEventListener("click", async () => {
        if (!pedido || pedido.estadoPago !== "PENDIENTE") {
            mostrarError("Este pedido ya no tiene un pago pendiente.");
            return;
        }
        btnProcesar.disabled = true;
        mostrarProcesando("Creando una preferencia segura con el total guardado en el servidor...");
        try {
            const response = await FastMarket.request(`/pagos/pedidos/${pedidoId}/preferencia`, {
                method: "POST",
                auth: true
            });
            const urlPago = response?.initPoint || response?.sandboxInitPoint;
            if (!urlPago) throw new Error("Mercado Pago no devolvió una URL válida de pago.");
            window.location.assign(urlPago);
        } catch (error) {
            mostrarError(error.message);
            btnProcesar.disabled = false;
        }
    });

    btnCancelar.addEventListener("click", async () => {
        const confirmar = await FastMarket.confirmAction("¿Deseas cancelar esta reserva? El stock y el cupón serán devueltos.");
        if (!confirmar) return;
        btnCancelar.disabled = true;
        try {
            await FastMarket.request(`/pagos/pedidos/${pedidoId}/cancelar`, { method: "POST", auth: true });
            FastMarket.notify("Pago cancelado. Tu carrito se conserva para que puedas corregirlo.", "info");
            window.location.href = "checkout.html";
        } catch (error) {
            mostrarError(error.message);
            btnCancelar.disabled = false;
        }
    });

    function mostrarDatosServidor(data) {
        document.getElementById("total-amount").textContent = FastMarket.money(data.total);
        document.getElementById("subtotal-mp").textContent = FastMarket.money(data.subtotal);
        document.getElementById("descuento-mp").textContent = `- ${FastMarket.money(data.descuento)}`;
        document.getElementById("envio-mp").textContent = FastMarket.money(data.costoEnvio);
        document.getElementById("total-mp").textContent = FastMarket.money(data.total);

        if (data.estadoPago === "APROBADO") {
            mostrarExito("Este pedido ya tiene un pago aprobado.");
            btnProcesar.disabled = true;
            btnCancelar.disabled = true;
        } else if (data.estado === "CANCELADO") {
            mostrarError("La reserva fue cancelada o venció. Crea un nuevo pedido desde el checkout.");
            btnProcesar.disabled = true;
            btnCancelar.disabled = true;
        }
    }

    function manejarEstadoRetorno(status) {
        if (status === "failure") mostrarError("Mercado Pago informó que el pago no se completó. Puedes reintentar o cancelar la reserva.");
        if (status === "pending") mostrarProcesando("El pago sigue pendiente de validación. También puedes revisarlo en Mis pedidos.");
    }

    function mostrarProcesando(mensaje) {
        estadoPago.innerHTML = `<div class="estado-pago processing"><div class="loader"></div><p><strong>${FastMarket.escapeHTML(mensaje)}</strong></p></div>`;
    }

    function mostrarExito(mensaje) {
        estadoPago.innerHTML = `<div class="estado-pago success"><p><strong>✓ ${FastMarket.escapeHTML(mensaje)}</strong></p></div>`;
    }

    function mostrarError(mensaje) {
        estadoPago.innerHTML = `<div class="estado-pago error"><p><strong>✗ ${FastMarket.escapeHTML(mensaje)}</strong></p></div>`;
    }
});
