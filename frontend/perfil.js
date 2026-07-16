document.addEventListener("DOMContentLoaded", async () => {
    FastMarket.activarBuscador("buscador", "busqueda");
    FastMarket.activarMenuCliente();
    FastMarket.activarChatBasico();

    const usuario = FastMarket.requireCliente(false);
    if (!usuario) {
        document.getElementById("mensaje-sin-sesion")?.classList.remove("oculto");
        return;
    }

    await refrescarUsuario(usuario.id);
    activarEventos();
});

let usuarioActual = null;
let pedidosUsuario = [];
let reclamosUsuario = [];

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
    return "La nueva contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y no debe contener espacios.";
}

async function refrescarUsuario(id) {
    try {
        usuarioActual = await FastMarket.request(`/usuarios/${id}`, { auth: true });
        FastMarket.actualizarUsuario(usuarioActual);
        try {
            pedidosUsuario = await FastMarket.request(`/pedidos/usuario/${id}`, { auth: true });
        } catch {
            pedidosUsuario = [];
        }
        try {
            reclamosUsuario = await FastMarket.request(`/reclamos/mios`, { auth: true });
        } catch {
            reclamosUsuario = [];
        }
        pintarPerfil();
    } catch (error) {
        mostrarMensaje("mensaje-perfil", error.message);
    }
}

function activarEventos() {
    document.getElementById("cerrar-sesion-cliente")?.addEventListener("click", cerrarSesion);
    document.getElementById("btn-cerrar-lateral")?.addEventListener("click", cerrarSesion);
    document.getElementById("form-perfil")?.addEventListener("submit", guardarPerfil);
    document.getElementById("form-direccion")?.addEventListener("submit", agregarDireccion);
    document.getElementById("form-password")?.addEventListener("submit", cambiarPassword);
    document.getElementById("form-reclamo")?.addEventListener("submit", registrarReclamo);
}

function pintarPerfil() {
    if (!usuarioActual) return;

    setText("resumen-nombre", usuarioActual.nombre);
    setText("resumen-correo", usuarioActual.correo);
    setText("resumen-direcciones", (usuarioActual.direcciones || []).length);
    setText("resumen-pedidos", pedidosUsuario.length);

    const mensajeSinSesion = document.getElementById("mensaje-sin-sesion");
    if (mensajeSinSesion) mensajeSinSesion.classList.add("oculto");

    const nombre = document.getElementById("perfil-nombre");
    const correo = document.getElementById("perfil-correo");
    const telefono = document.getElementById("perfil-telefono");

    if (nombre) nombre.value = usuarioActual.nombre || "";
    if (correo) {
        correo.value = usuarioActual.correo || "";
        correo.disabled = true;
    }
    if (telefono) telefono.value = usuarioActual.telefono || "";

    pintarDirecciones();
    poblarPedidosReclamo();
    pintarReclamos();
}

function pintarDirecciones() {
    const lista = document.getElementById("lista-direcciones");
    if (!lista) return;

    const direcciones = usuarioActual?.direcciones || [];

    if (direcciones.length === 0) {
        lista.innerHTML = `<p>No tienes direcciones registradas.</p>`;
        return;
    }

    lista.innerHTML = direcciones.map((d, i) => `
        <div class="direccion-item">
            <h4>Dirección ${i + 1} ${d.principal ? "(principal)" : ""}</h4>
            <p>${FastMarket.escapeHTML(d.direccion)}</p>
            <small>${FastMarket.escapeHTML([d.referencia, d.distrito, d.ciudad].filter(Boolean).join(" - "))}</small>
        </div>
    `).join("");
}

async function guardarPerfil(e) {
    e.preventDefault();
    if (!usuarioActual) return;

    const nombre = document.getElementById("perfil-nombre").value.trim();
    const telefono = document.getElementById("perfil-telefono").value.trim();

    if (!nombre) {
        mostrarMensaje("mensaje-perfil", "El nombre es obligatorio.");
        return;
    }

    try {
        usuarioActual = await FastMarket.request(`/usuarios/${usuarioActual.id}`, {
            method: "PUT",
            auth: true,
            body: {
                nombre,
                telefono,
                documento: usuarioActual.documento
            }
        });
        FastMarket.actualizarUsuario(usuarioActual);
        pintarPerfil();
        FastMarket.activarMenuCliente();
        mostrarMensaje("mensaje-perfil", "Perfil actualizado correctamente.");
    } catch (error) {
        mostrarMensaje("mensaje-perfil", error.message);
    }
}

async function agregarDireccion(e) {
    e.preventDefault();
    if (!usuarioActual) return;

    const input = document.getElementById("nueva-direccion");
    const valor = input.value.trim();

    if (!valor) {
        mostrarMensaje("mensaje-direccion", "Escribe una dirección.");
        return;
    }

    try {
        usuarioActual = await FastMarket.request(`/usuarios/${usuarioActual.id}/direcciones`, {
            method: "POST",
            auth: true,
            body: {
                direccion: valor,
                referencia: "",
                distrito: "Ica",
                ciudad: "Ica",
                principal: (usuarioActual.direcciones || []).length === 0
            }
        });
        FastMarket.actualizarUsuario(usuarioActual);
        input.value = "";
        pintarPerfil();
        mostrarMensaje("mensaje-direccion", "Dirección agregada correctamente.");
    } catch (error) {
        mostrarMensaje("mensaje-direccion", error.message);
    }
}

async function cambiarPassword(e) {
    e.preventDefault();
    if (!usuarioActual) return;

    const actual = document.getElementById("password-actual");
    const nueva = document.getElementById("password-nueva");
    const confirmar = document.getElementById("password-confirmar");

    if (!actual.value || !nueva.value || !confirmar.value) {
        mostrarMensaje("mensaje-password", "Completa todos los campos.");
        return;
    }

    if (!validarPasswordSegura(nueva.value)) {
        mostrarMensaje("mensaje-password", mensajePasswordSegura());
        return;
    }

    if (nueva.value !== confirmar.value) {
        mostrarMensaje("mensaje-password", "Las contraseñas no coinciden.");
        return;
    }

    try {
        await FastMarket.request(`/usuarios/${usuarioActual.id}/password`, {
            method: "PUT",
            auth: true,
            body: {
                passwordActual: actual.value,
                passwordNueva: nueva.value
            }
        });

        actual.value = "";
        nueva.value = "";
        confirmar.value = "";
        mostrarMensaje("mensaje-password", "Contraseña actualizada correctamente.");
    } catch (error) {
        mostrarMensaje("mensaje-password", error.message);
    }
}


function poblarPedidosReclamo() {
    const select = document.getElementById("reclamo-pedido");
    if (!select) return;
    const actual = select.value;
    select.innerHTML = `<option value="">Sin pedido asociado</option>` + pedidosUsuario.map((pedido) =>
        `<option value="${pedido.id}">${FastMarket.escapeHTML(pedido.codigo || `Pedido ${pedido.id}`)} · ${FastMarket.money(pedido.total)} · ${FastMarket.escapeHTML(textoEstadoReclamo(pedido.estado))}</option>`
    ).join("");
    if (actual && pedidosUsuario.some((p) => String(p.id) === String(actual))) select.value = actual;
}

async function registrarReclamo(e) {
    e.preventDefault();
    if (!usuarioActual) return;
    const pedidoId = document.getElementById("reclamo-pedido")?.value || "";
    const tipo = document.getElementById("reclamo-tipo")?.value || "OTRO";
    const asunto = document.getElementById("reclamo-asunto")?.value.trim() || "";
    const descripcion = document.getElementById("reclamo-descripcion")?.value.trim() || "";
    if (!asunto || !descripcion) {
        mostrarMensaje("mensaje-reclamo", "Completa el asunto y la descripción del reclamo.");
        return;
    }
    const boton = e.currentTarget.querySelector("button[type='submit']");
    if (boton) boton.disabled = true;
    try {
        await FastMarket.request("/reclamos", {
            method: "POST",
            auth: true,
            body: { pedidoId: pedidoId ? Number(pedidoId) : null, tipo, asunto, descripcion }
        });
        reclamosUsuario = await FastMarket.request("/reclamos/mios", { auth: true });
        e.currentTarget.reset();
        poblarPedidosReclamo();
        pintarReclamos();
        mostrarMensaje("mensaje-reclamo", "Reclamo registrado correctamente. Podrás revisar aquí la respuesta.");
    } catch (error) {
        mostrarMensaje("mensaje-reclamo", error.message);
    } finally {
        if (boton) boton.disabled = false;
    }
}

function pintarReclamos() {
    const cont = document.getElementById("lista-reclamos");
    if (!cont) return;
    if (!reclamosUsuario.length) {
        cont.innerHTML = `<p>No tienes reclamos registrados.</p>`;
        return;
    }
    cont.innerHTML = reclamosUsuario.map((r) => `
        <article class="reclamo-cliente-item">
            <div class="reclamo-cliente-cabecera">
                <div>
                    <h3>${FastMarket.escapeHTML(r.codigo)} · ${FastMarket.escapeHTML(r.asunto)}</h3>
                    <small>${FastMarket.escapeHTML(new Date(r.fechaCreacion).toLocaleString("es-PE"))} · ${FastMarket.escapeHTML(textoEstadoReclamo(r.tipo))}${r.pedidoCodigo ? ` · Pedido ${FastMarket.escapeHTML(r.pedidoCodigo)}` : ""}</small>
                </div>
                <span class="reclamo-estado ${String(r.estado || "ABIERTO").toLowerCase()}">${FastMarket.escapeHTML(textoEstadoReclamo(r.estado))}</span>
            </div>
            <p>${FastMarket.escapeHTML(r.descripcion)}</p>
            ${r.respuesta ? `<div class="reclamo-respuesta"><strong>Respuesta de FastMarket</strong><p>${FastMarket.escapeHTML(r.respuesta)}</p></div>` : `<small>El equipo todavía no ha registrado una respuesta.</small>`}
        </article>`).join("");
}

function textoEstadoReclamo(valor) {
    const mapa = {
        PENDIENTE: "Pendiente", CONFIRMADO: "Confirmado", PREPARANDO: "Preparando", CAMINO: "En camino", ENTREGADO: "Entregado", CANCELADO: "Cancelado",
        PRODUCTO: "Producto", ENTREGA: "Entrega", PAGO: "Pago", DEVOLUCION: "Devolución", ATENCION: "Atención", OTRO: "Otro",
        ABIERTO: "Abierto", EN_REVISION: "En revisión", RESUELTO: "Resuelto", CERRADO: "Cerrado"
    };
    return mapa[valor] || String(valor || "").replaceAll("_", " ");
}

function cerrarSesion() {
    FastMarket.cerrarSesion();
    window.location.href = "login.html";
}

function mostrarMensaje(id, texto) {
    const el = document.getElementById(id);
    if (el) el.textContent = texto;
}

function setText(id, texto) {
    const el = document.getElementById(id);
    if (el) el.textContent = texto || "---";
}
