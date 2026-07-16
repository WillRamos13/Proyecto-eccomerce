const FastMarket = (() => {
    const API_URL = (() => {
        const normalizar = (url) => {
            const limpia = String(url || "").trim().replace(/\/+$/, "");
            if (!limpia) return "";
            return limpia.endsWith("/api") ? limpia : `${limpia}/api`;
        };

        const desdeWindow = normalizar(window.FASTMARKET_API_URL);
        if (desdeWindow) return desdeWindow;

        const meta = document.querySelector('meta[name="fastmarket-api-url"]');
        const desdeMeta = normalizar(meta?.content);
        if (desdeMeta) return desdeMeta;

        const host = window.location.hostname;
        if (host === "localhost" || host === "127.0.0.1") {
            return "http://localhost:8080/api";
        }

        if (window.location.protocol === "http:" || window.location.protocol === "https:") {
            return `${window.location.origin}/api`;
        }
        return "http://localhost:8080/api";
    })();

    const STORAGE = {
        sesion: "fastmarket_sesion",
        cliente: "fastmarket_cliente",
        admin: "fastmarket_admin",
        token: "fastmarket_token",
        carrito: "fastmarket_carrito",
        cupon: "fastmarket_cupon",
        checkoutCarrito: "fastmarket_checkout_carrito",
        checkoutCupon: "fastmarket_checkout_cupon",
        carritoMirrorUsuario: "fastmarket_carrito_usuario_id"
    };

    function leerJSONStorage(storage, nuevaClave, claveAnterior, defecto = null) {
        const valor = storage.getItem(nuevaClave) || storage.getItem(claveAnterior);
        try { return valor ? JSON.parse(valor) : defecto; } catch { return defecto; }
    }

    function getSesion() {
        return leerJSONStorage(sessionStorage, STORAGE.sesion, "fastmarket_sesion", null);
    }

    function getCliente() {
        const sesion = getSesion();
        return sesion?.usuario || leerJSONStorage(sessionStorage, STORAGE.cliente, "fastmarket_cliente", null) || null;
    }

    function getToken() {
        return getSesion()?.token || sessionStorage.getItem(STORAGE.token) || sessionStorage.getItem("fastmarket_token") || "";
    }

    function guardarSesion(authResponse) {
        const usuario = authResponse.usuario || authResponse;
        const token = authResponse.token || "";
        sessionStorage.setItem(STORAGE.sesion, JSON.stringify({ usuario, token }));
        sessionStorage.setItem(STORAGE.cliente, JSON.stringify(usuario));

        if (["ADMIN", "VENDEDOR"].includes(usuario.rol)) {
            sessionStorage.setItem(STORAGE.admin, JSON.stringify(usuario));
        } else {
            sessionStorage.removeItem(STORAGE.admin); localStorage.removeItem(STORAGE.admin);
            sessionStorage.removeItem("fastmarket_admin"); localStorage.removeItem("fastmarket_admin");
        }

        if (token) sessionStorage.setItem(STORAGE.token, token);
        return usuario;
    }

    function actualizarUsuario(usuario) {
        const sesion = getSesion() || {};
        sessionStorage.setItem(STORAGE.sesion, JSON.stringify({ usuario, token: sesion.token || getToken() }));
        sessionStorage.setItem(STORAGE.cliente, JSON.stringify(usuario));
        if (["ADMIN", "VENDEDOR"].includes(usuario.rol)) sessionStorage.setItem(STORAGE.admin, JSON.stringify(usuario));
    }

    function cerrarSesion() {
        [STORAGE.sesion, STORAGE.cliente, STORAGE.admin, STORAGE.token, "fastmarket_sesion", "fastmarket_cliente", "fastmarket_admin", "fastmarket_token"].forEach((clave) => {
            sessionStorage.removeItem(clave);
            localStorage.removeItem(clave);
        });
        localStorage.removeItem(STORAGE.carritoMirrorUsuario);
    }

    function headers(auth = false) {
        const h = { "Content-Type": "application/json" };
        if (auth) {
            const token = getToken();
            if (token) h.Authorization = `Bearer ${token}`;
        }
        return h;
    }

    async function request(path, options = {}) {
        const { method = "GET", body = null, auth = false, raw = false } = options;

        const config = { method, headers: headers(auth) };
        if (body !== null && body !== undefined) config.body = JSON.stringify(body);

        let res;
        try {
            res = await fetch(`${API_URL}${path}`, config);
        } catch (error) {
            throw new Error(`No se pudo conectar con el backend. Revisa frontend/config.js y la configuración CORS y que CORS permita tu frontend. API actual: ${API_URL}`);
        }

        const text = await res.text();
        let data = null;

        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text;
        }

        if (!res.ok) {
            const mensaje = data?.error || data?.detalle || "Error al conectar con el backend";
            throw new Error(mensaje);
        }

        return raw ? { res, data } : data;
    }

    function requireCliente(redirect = true) {
        const usuario = getCliente();
        if (!usuario && redirect) window.location.href = "login.html";
        return usuario;
    }

    function requireAdmin(redirect = true) {
        const usuario = getCliente();
        if (!usuario || usuario.rol !== "ADMIN") {
            if (redirect) window.location.href = "login.html";
            return null;
        }
        return usuario;
    }

    function money(value) {
        return `S/ ${Number(value || 0).toFixed(2)}`;
    }

    function escapeHTML(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }


    function notify(mensaje, tipo = "info") {
        let el = document.getElementById("toast");
        if (!el) {
            el = document.createElement("div");
            el.id = "toast";
            document.body.appendChild(el);
        }
        el.textContent = mensaje;
        el.className = "";
        el.classList.add("activo", `toast-${tipo}`);
        clearTimeout(el._timer);
        el._timer = setTimeout(() => el.classList.remove("activo"), 3200);
    }

    function confirmAction(mensaje) {
        return new Promise((resolve) => {
            let overlay = document.getElementById("fm-confirm-overlay");
            if (overlay) overlay.remove();

            overlay = document.createElement("div");
            overlay.id = "fm-confirm-overlay";
            overlay.innerHTML = `
                <div class="fm-confirm-card" role="dialog" aria-modal="true">
                    <h3>Confirmar acción</h3>
                    <p>${escapeHTML(mensaje || "Confirma la acción")}</p>
                    <div class="fm-confirm-actions">
                        <button type="button" class="fm-confirm-cancelar">Cancelar</button>
                        <button type="button" class="fm-confirm-aceptar">Confirmar</button>
                    </div>
                </div>`;
            document.body.appendChild(overlay);

            const cerrar = (valor) => { overlay.remove(); resolve(valor); };
            overlay.querySelector(".fm-confirm-cancelar")?.addEventListener("click", () => cerrar(false));
            overlay.querySelector(".fm-confirm-aceptar")?.addEventListener("click", () => cerrar(true));
            overlay.addEventListener("click", (e) => { if (e.target === overlay) cerrar(false); });
            document.addEventListener("keydown", function esc(e) {
                if (e.key === "Escape") {
                    document.removeEventListener("keydown", esc);
                    cerrar(false);
                }
            });
        });
    }

    function leerJSONSeguro(valor, defecto = null) {
        try { return valor ? JSON.parse(valor) : defecto; } catch { return defecto; }
    }

    function normalizarItemCarrito(item) {
        return {
            id: Number(item?.productoId || item?.id),
            productoId: Number(item?.productoId || item?.id),
            nombre: item?.nombre || item?.productoNombre || "Producto",
            imagen: item?.imagen || "img/logo.png",
            precio: Number(item?.precio ?? item?.precioUnitario ?? 0),
            stock: Number(item?.stockDisponible ?? item?.stock ?? 999999),
            cantidad: Number(item?.cantidad || 1)
        };
    }

    function normalizarCarrito(items) {
        const mapa = new Map();
        (items || []).forEach((item) => {
            const normalizado = normalizarItemCarrito(item);
            if (!normalizado.id || normalizado.cantidad <= 0) return;
            const existente = mapa.get(normalizado.id);
            if (existente) {
                existente.cantidad += normalizado.cantidad;
                existente.stock = Math.max(Number(existente.stock || 0), Number(normalizado.stock || 0));
                if ((!existente.nombre || existente.nombre === "Producto") && normalizado.nombre) existente.nombre = normalizado.nombre;
                if ((!existente.imagen || existente.imagen === "img/logo.png") && normalizado.imagen) existente.imagen = normalizado.imagen;
                if (!existente.precio && normalizado.precio) existente.precio = normalizado.precio;
            } else {
                mapa.set(normalizado.id, normalizado);
            }
        });
        return Array.from(mapa.values());
    }

    function obtenerCarritoLocal() {
        const local = leerJSONSeguro(localStorage.getItem(STORAGE.carrito), []);
        const checkout = leerJSONSeguro(sessionStorage.getItem(STORAGE.checkoutCarrito), []);
        const backup = leerJSONSeguro(sessionStorage.getItem("fastmarket_carrito_backup"), []);
        const mejor = Array.isArray(local) && local.length ? local : (Array.isArray(checkout) && checkout.length ? checkout : backup);
        return normalizarCarrito(mejor || []);
    }

    function obtenerCuponLocal() {
        return leerJSONSeguro(localStorage.getItem(STORAGE.cupon), null) || leerJSONSeguro(sessionStorage.getItem(STORAGE.checkoutCupon), null);
    }

    function guardarSnapshotCarrito(items = [], cupon = null, usuarioId = null) {
        const normalizados = normalizarCarrito(items);
        const cuponData = typeof cupon === "object" && cupon !== null ? cupon : (cupon ? { codigo: cupon } : null);
        const codigo = normalizados.length && cuponData?.codigo ? String(cuponData.codigo).trim().toUpperCase() : null;
        localStorage.setItem(STORAGE.carrito, JSON.stringify(normalizados));
        sessionStorage.setItem(STORAGE.checkoutCarrito, JSON.stringify(normalizados));
        sessionStorage.setItem("fastmarket_carrito_backup", JSON.stringify(normalizados));
        if (usuarioId) localStorage.setItem(STORAGE.carritoMirrorUsuario, String(usuarioId));
        if (codigo) {
            const cuponNormalizado = JSON.stringify({
                codigo,
                descripcion: cuponData.descripcion || "",
                descuento: Number(cuponData.descuento || 0),
                subtotalAplicable: Number(cuponData.subtotalAplicable || 0)
            });
            localStorage.setItem(STORAGE.cupon, cuponNormalizado);
            sessionStorage.setItem(STORAGE.checkoutCupon, cuponNormalizado);
        } else {
            localStorage.removeItem(STORAGE.cupon);
            sessionStorage.removeItem(STORAGE.checkoutCupon);
        }
        return normalizados;
    }

    function prepararCheckoutCarrito(items = [], cupon = null) {
        const usuario = getCliente();
        return guardarSnapshotCarrito(items, cupon, usuario?.id || null);
    }

    function carritoLocalPerteneceAlUsuario(usuarioId) {
        return Boolean(usuarioId) && localStorage.getItem(STORAGE.carritoMirrorUsuario) === String(usuarioId);
    }


    async function obtenerConfigPublica() {
        return request("/config/public");
    }

    async function obtenerCarrito() {
        const usuario = getCliente();
        if (!usuario) {
            const cuponLocal = obtenerCuponLocal();
            return { items: obtenerCarritoLocal(), cuponCodigo: cuponLocal?.codigo || null, descuento: Number(cuponLocal?.descuento || 0), invitado: true };
        }
        try {
            return await request(`/carritos/usuario/${usuario.id}`, { auth: true });
        } catch {
            const cuponLocal = obtenerCuponLocal();
            return { items: obtenerCarritoLocal(), cuponCodigo: cuponLocal?.codigo || null, descuento: Number(cuponLocal?.descuento || 0), invitado: true };
        }
    }

    async function sincronizarCarrito(items, cuponCodigo = null) {
        const cuponData = typeof cuponCodigo === "object" && cuponCodigo !== null ? cuponCodigo : (cuponCodigo ? { codigo: cuponCodigo } : null);
        const codigoFinal = cuponData?.codigo ? String(cuponData.codigo).trim().toUpperCase() : null;
        const snapshot = guardarSnapshotCarrito(items || [], cuponData, getCliente()?.id || null);
        const normalizados = snapshot.map((item) => ({
            productoId: Number(item.productoId || item.id),
            cantidad: Number(item.cantidad || 1)
        })).filter((item) => item.productoId && item.cantidad > 0);

        const usuario = getCliente();
        if (!usuario) {
            return { items: snapshot, cuponCodigo: codigoFinal, descuento: Number(cuponData?.descuento || 0), invitado: true };
        }

        const actualizado = await request(`/carritos/usuario/${usuario.id}`, {
            method: "PUT",
            auth: true,
            body: { cuponCodigo: codigoFinal, items: normalizados }
        });
        guardarSnapshotCarrito(actualizado.items || snapshot, {
            codigo: actualizado.cuponCodigo || codigoFinal,
            descuento: Number(actualizado.descuento || cuponData?.descuento || 0),
            descripcion: cuponData?.descripcion || ""
        }, usuario.id);
        return actualizado;
    }

    async function limpiarCarritoUsuario() {
        const usuario = getCliente();
        localStorage.removeItem(STORAGE.carrito);
        localStorage.removeItem(STORAGE.cupon);
        sessionStorage.removeItem(STORAGE.checkoutCarrito);
        sessionStorage.removeItem(STORAGE.checkoutCupon);
        sessionStorage.removeItem("fastmarket_carrito_backup");
        localStorage.removeItem(STORAGE.carritoMirrorUsuario);
        if (usuario) {
            try { await request(`/carritos/usuario/${usuario.id}`, { method: "DELETE", auth: true }); } catch {}
        }
    }

    function marcarCargando(elemento, cargando = true, texto = "Procesando...") {
        if (!elemento) return;
        if (cargando) {
            elemento.dataset.fmTextoOriginal = elemento.textContent;
            elemento.textContent = texto;
            elemento.disabled = true;
            elemento.classList.add("cargando");
        } else {
            elemento.textContent = elemento.dataset.fmTextoOriginal || elemento.textContent;
            elemento.disabled = false;
            elemento.classList.remove("cargando");
        }
    }

    function normalizarEstado(estado) {
        return String(estado || "PENDIENTE").toUpperCase();
    }

    function estadoTexto(estado) {
        const estados = {
            PENDIENTE: "Pendiente",
            CONFIRMADO: "Confirmado",
            PREPARANDO: "Preparando",
            EN_CAMINO: "En camino",
            CAMINO: "En camino",
            ENTREGADO: "Entregado",
            CANCELADO: "Cancelado"
        };
        return estados[normalizarEstado(estado)] || estado;
    }

    function irBusqueda(texto) {
        const q = String(texto || "").trim();
        if (q) window.location.href = `productos.html?q=${encodeURIComponent(q)}`;
    }

    function activarBuscador(inputId = "buscador", contenedorId = "busqueda") {
        const contenedor = document.getElementById(contenedorId);
        const input = document.getElementById(inputId);

        if (contenedor) {
            contenedor.addEventListener("click", (e) => {
                e.stopPropagation();
                contenedor.classList.toggle("activo");
                if (input) input.focus();
            });
        }

        if (input) {
            input.addEventListener("click", (e) => e.stopPropagation());
            input.addEventListener("keydown", (e) => {
                if (e.key === "Enter") {
                    irBusqueda(input.value);
                    input.value = "";
                }
            });
        }

        document.addEventListener("click", () => {
            if (contenedor) contenedor.classList.remove("activo");
            const opcionesCliente = document.getElementById("opciones-cliente");
            if (opcionesCliente) opcionesCliente.classList.remove("activo");
        });
    }

    function activarMenuCliente() {
        const loginBtn = document.getElementById("login-btn");
        const clienteMenu = document.getElementById("cliente-menu");
        const btnCliente = document.getElementById("btn-cliente");
        const opcionesCliente = document.getElementById("opciones-cliente");
        const nombreCliente = document.getElementById("nombre-cliente");
        const cerrar = document.getElementById("cerrar-sesion-cliente");

        const usuario = getCliente();

        if (loginBtn && clienteMenu && nombreCliente) {
            if (usuario) {
                loginBtn.classList.add("oculto");
                clienteMenu.classList.remove("oculto");
                nombreCliente.textContent = usuario.nombre || "Cliente";
            } else {
                loginBtn.classList.remove("oculto");
                clienteMenu.classList.add("oculto");
                nombreCliente.textContent = "Cliente";
            }
        }

        if (btnCliente && opcionesCliente && !btnCliente.dataset.fmMenu) {
            btnCliente.dataset.fmMenu = "true";
            btnCliente.addEventListener("click", (e) => {
                e.stopPropagation();
                opcionesCliente.classList.toggle("activo");
            });
            opcionesCliente.addEventListener("click", (e) => e.stopPropagation());
        }

        if (cerrar && !cerrar.dataset.fmLogout) {
            cerrar.dataset.fmLogout = "true";
            cerrar.addEventListener("click", () => {
                cerrarSesion();
                window.location.href = "login.html";
            });
        }
    }

    function mostrarPanelCliente() {
        const overlay = document.getElementById("overlay-cliente");
        const panel = document.getElementById("panel-cliente");
        const cerrar = document.getElementById("cerrar-panel-cliente");
        const titulo = document.getElementById("titulo-panel-cliente");
        const contenido = document.getElementById("contenido-panel-cliente");
        if (!overlay || !panel || !titulo || !contenido) return;

        const cerrarPanel = () => {
            overlay.classList.remove("activo");
            panel.classList.remove("activo");
        };

        if (cerrar && !cerrar.dataset.fmCerrar) {
            cerrar.dataset.fmCerrar = "true";
            cerrar.addEventListener("click", cerrarPanel);
            overlay.addEventListener("click", cerrarPanel);
        }

        document.querySelectorAll("[data-cliente-panel]").forEach((boton) => {
            if (boton.dataset.fmPanel) return;
            boton.dataset.fmPanel = "true";
            boton.addEventListener("click", async () => {
                const usuario = requireCliente(true);
                if (!usuario) return;

                panel.classList.add("activo");
                overlay.classList.add("activo");

                if (boton.dataset.clientePanel === "perfil") {
                    titulo.textContent = "Mi perfil";
                    contenido.innerHTML = `
                        <div class="info-cliente-card">
                            <h3>Datos personales</h3>
                            <p><strong>Nombre:</strong> ${escapeHTML(usuario.nombre)}</p>
                            <p><strong>Correo:</strong> ${escapeHTML(usuario.correo)}</p>
                            <p><strong>Teléfono:</strong> ${escapeHTML(usuario.telefono || "No registrado")}</p>
                            <a href="perfil.html">Editar perfil</a>
                        </div>`;
                }

                if (boton.dataset.clientePanel === "direcciones") {
                    titulo.textContent = "Direcciones";
                    const direcciones = usuario.direcciones || [];
                    contenido.innerHTML = direcciones.length
                        ? direcciones.map((d, i) => `
                            <div class="info-cliente-card">
                                <h3>Dirección ${i + 1}</h3>
                                <p>${escapeHTML(d.direccion || d)}</p>
                                <small>${escapeHTML(d.referencia || "")}</small>
                            </div>`).join("")
                        : `<div class="info-cliente-card"><h3>Sin direcciones</h3><p>Puedes agregarlas desde tu perfil.</p><a href="perfil.html#direcciones">Agregar dirección</a></div>`;
                }

                if (boton.dataset.clientePanel === "seguridad") {
                    titulo.textContent = "Seguridad";
                    contenido.innerHTML = `<div class="info-cliente-card"><h3>Sesión activa</h3><p>Tu cuenta está iniciada en este navegador.</p></div>`;
                }
            });
        });
    }

    function activarChatBasico() {
        const boton = document.getElementById("chat-soporte");
        const chatBox = document.getElementById("chat-box");
        const cerrar = document.getElementById("cerrar-chat");
        const enviar = document.getElementById("enviar");
        const input = document.getElementById("mensaje");
        const mensajes = document.getElementById("chat-mensajes");
        if (!boton || !chatBox || !input || !mensajes) return;

        const historialChat = [];
        input.maxLength = 1000;

        if (boton.dataset.fmChatActivo) return;
        boton.dataset.fmChatActivo = "true";

        boton.addEventListener("click", () => {
            chatBox.style.display = "flex";
            if (!mensajes.dataset.fmBienvenida) {
                if (!mensajes.children.length) {
                    agregar("bot", "Hola 👋 Soy el asistente de FastMarket. Puedo ayudarte con productos, promociones, stock, compras, pedidos, envíos, pagos y tu cuenta.");
                }
                mensajes.dataset.fmBienvenida = "true";
            }
            input.focus();
        });

        if (cerrar) cerrar.addEventListener("click", () => chatBox.style.display = "none");

        const agregar = (tipo, texto) => {
            const div = document.createElement("div");
            div.className = tipo === "user" ? "mensaje usuario" : "mensaje bot";

            const contenido = document.createElement("div");
            contenido.className = "mensaje-contenido";
            contenido.style.whiteSpace = "pre-wrap";
            contenido.textContent = texto;

            div.appendChild(contenido);
            mensajes.appendChild(div);
            mensajes.scrollTop = mensajes.scrollHeight;
            return div;
        };

        const cambiarTextoMensaje = (mensaje, texto) => {
            if (!mensaje) return;
            const contenido = mensaje.querySelector(".mensaje-contenido");
            if (contenido) contenido.textContent = texto;
            else mensaje.textContent = texto;
            mensajes.scrollTop = mensajes.scrollHeight;
        };

        const responder = async () => {
            const texto = input.value.trim();
            if (!texto) return;

            const historialAnterior = historialChat.slice(-8);
            agregar("user", texto);
            historialChat.push({ rol: "user", contenido: texto });
            if (historialChat.length > 8) historialChat.splice(0, historialChat.length - 8);
            input.value = "";
            input.disabled = true;
            if (enviar) enviar.disabled = true;

            const mensajeCarga = agregar("bot", "Escribiendo...");

            try {
                const data = await request("/chat", {
                    method: "POST",
                    body: {
                        mensaje: texto,
                        historial: historialAnterior
                    },
                    auth: true
                });

                const respuesta = data?.respuesta || "No recibí respuesta del asistente.";
                cambiarTextoMensaje(mensajeCarga, respuesta);
                historialChat.push({ rol: "assistant", contenido: respuesta });
                if (historialChat.length > 8) historialChat.splice(0, historialChat.length - 8);
            } catch (error) {
                cambiarTextoMensaje(
                    mensajeCarga,
                    error?.message || "No pude conectarme con el asistente. Revisa el backend o la configuración de la IA."
                );
            } finally {
                input.disabled = false;
                if (enviar) enviar.disabled = false;
                input.focus();
            }
        };

        if (enviar) enviar.addEventListener("click", responder);
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") responder();
        });
    }

    return {
        API_URL,
        getSesion,
        getCliente,
        getToken,
        guardarSesion,
        actualizarUsuario,
        cerrarSesion,
        request,
        requireCliente,
        requireAdmin,
        money,
        escapeHTML,
        notify,
        confirmAction,
        obtenerConfigPublica,
        obtenerCarrito,
        obtenerCarritoLocal,
        obtenerCuponLocal,
        guardarSnapshotCarrito,
        prepararCheckoutCarrito,
        carritoLocalPerteneceAlUsuario,
        sincronizarCarrito,
        limpiarCarritoUsuario,
        marcarCargando,
        normalizarEstado,
        estadoTexto,
        irBusqueda,
        activarBuscador,
        activarMenuCliente,
        mostrarPanelCliente,
        activarChatBasico
    };
})();
