document.addEventListener("DOMContentLoaded", () => {
    const formRegistro = document.getElementById("form-registro");
    const btnRegistro = document.getElementById("btn-registro");
    const btnReenviar = document.getElementById("reenviar-codigo");
    const codigoPanel = document.getElementById("codigo-panel");
    const codigoVerificacion = document.getElementById("codigo-verificacion");

    const registroCorreo = document.getElementById("registro-correo");
    const registroNombre = document.getElementById("registro-nombre");
    const registroApellidos = document.getElementById("registro-apellidos");
    const registroDocumento = document.getElementById("registro-documento");
    const registroCelular = document.getElementById("registro-celular");
    const registroPassword = document.getElementById("registro-password");
    const mensajeRegistro = document.getElementById("mensaje-registro");

    const reglas = {
        length: document.getElementById("rule-length"),
        lower: document.getElementById("rule-lower"),
        number: document.getElementById("rule-number"),
        space: document.getElementById("rule-space"),
        upper: document.getElementById("rule-upper"),
        special: document.getElementById("rule-special")
    };

    let codigoEnviado = false;
    let correoCodigoEnviado = "";

    function mostrarMensaje(elemento, texto, tipo = "error") {
        if (!elemento) return;
        elemento.textContent = texto;
        elemento.classList.remove("error", "ok");
        elemento.classList.add(tipo);
    }

    function validarCorreo(correo) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo);
    }

    function validarPassword(password) {
        const prohibidos = /[\\\/"'°¬¿¡Ññ]/;
        return {
            length: password.length >= 8,
            lower: /[a-z]/.test(password),
            number: /\d/.test(password),
            space: !/\s/.test(password),
            upper: /[A-Z]/.test(password),
            special: !prohibidos.test(password)
        };
    }

    function pintarRegla(elemento, cumple) {
        if (!elemento) return;
        elemento.classList.remove("ok", "bad");
        if (registroPassword.value.length > 0) {
            elemento.classList.add(cumple ? "ok" : "bad");
        }
    }

    function datosRegistro() {
        const nombre = registroNombre.value.trim();
        const apellidos = registroApellidos.value.trim();

        return {
            correo: registroCorreo.value.trim().toLowerCase(),
            nombre,
            apellidos,
            nombreCompleto: `${nombre} ${apellidos}`.trim(),
            documento: registroDocumento.value.trim(),
            celular: registroCelular.value.trim(),
            password: registroPassword.value,
            codigo: codigoVerificacion ? codigoVerificacion.value.trim() : ""
        };
    }

    function resetearCodigoSiCambioCorreo() {
        const correoActual = registroCorreo.value.trim().toLowerCase();

        if (codigoEnviado && correoActual !== correoCodigoEnviado) {
            codigoEnviado = false;
            correoCodigoEnviado = "";
            if (codigoVerificacion) codigoVerificacion.value = "";
            if (codigoPanel) codigoPanel.classList.add("oculto");
            btnRegistro.textContent = "Enviar código";
        }
    }

    function validarFormularioRegistro() {
        resetearCodigoSiCambioCorreo();

        const datos = datosRegistro();
        const pass = validarPassword(datos.password);

        pintarRegla(reglas.length, pass.length);
        pintarRegla(reglas.lower, pass.lower);
        pintarRegla(reglas.number, pass.number);
        pintarRegla(reglas.space, pass.space);
        pintarRegla(reglas.upper, pass.upper);
        pintarRegla(reglas.special, pass.special);

        const passwordOk = Object.values(pass).every(Boolean);
        const formularioOk =
            validarCorreo(datos.correo) &&
            datos.nombre.length >= 2 &&
            datos.apellidos.length >= 2 &&
            datos.documento.length >= 8 &&
            datos.celular.replace(/\D/g, "").length >= 9 &&
            passwordOk;

        if (!codigoEnviado) {
            btnRegistro.disabled = !formularioOk;
        } else {
            btnRegistro.disabled = !formularioOk || datos.codigo.length !== 6;
        }

        return formularioOk;
    }

    async function enviarCodigo() {
        const datos = datosRegistro();

        if (!validarFormularioRegistro()) {
            mostrarMensaje(mensajeRegistro, "Completa correctamente los datos antes de solicitar el código.");
            return;
        }

        btnRegistro.disabled = true;
        btnRegistro.textContent = "Enviando...";
        mostrarMensaje(mensajeRegistro, "Enviando código de verificación...", "ok");

        try {
            const respuesta = await FastMarket.request("/auth/registro/enviar-codigo", {
                method: "POST",
                body: {
                    correo: datos.correo,
                    nombre: datos.nombreCompleto
                }
            });

            codigoEnviado = true;
            correoCodigoEnviado = datos.correo;
            if (codigoPanel) codigoPanel.classList.remove("oculto");
            btnRegistro.textContent = "Crear cuenta";
            if (codigoVerificacion) codigoVerificacion.focus();
            mostrarMensaje(mensajeRegistro, respuesta?.mensaje || "Te enviamos un código a tu correo.", "ok");
        } catch (error) {
            mostrarMensaje(mensajeRegistro, error.message || "No se pudo enviar el código.");
            btnRegistro.textContent = "Enviar código";
        } finally {
            validarFormularioRegistro();
        }
    }

    async function registrarCuenta() {
        const datos = datosRegistro();

        if (!validarFormularioRegistro() || datos.codigo.length !== 6) {
            mostrarMensaje(mensajeRegistro, "Ingresa el código de 6 dígitos enviado a tu correo.");
            return;
        }

        btnRegistro.disabled = true;
        btnRegistro.textContent = "Creando cuenta...";
        mostrarMensaje(mensajeRegistro, "Verificando código...", "ok");

        try {
            const respuesta = await FastMarket.request("/auth/registro", {
                method: "POST",
                body: {
                    nombre: datos.nombreCompleto,
                    correo: datos.correo,
                    telefono: datos.celular,
                    documento: datos.documento,
                    password: datos.password,
                    codigoVerificacion: datos.codigo
                }
            });

            const usuario = FastMarket.guardarSesion(respuesta);
            mostrarMensaje(mensajeRegistro, "Cuenta creada correctamente. Redirigiendo...", "ok");

            setTimeout(() => {
                window.location.href = usuario.rol === "ADMIN" ? "admin.html" : (usuario.rol === "VENDEDOR" ? "vendedor.html" : "productos.html");
            }, 700);
        } catch (error) {
            mostrarMensaje(mensajeRegistro, error.message || "No se pudo crear la cuenta.");
            btnRegistro.textContent = "Crear cuenta";
            validarFormularioRegistro();
        }
    }

    document.querySelectorAll(".toggle-password").forEach((boton) => {
        boton.addEventListener("click", () => {
            const input = document.getElementById(boton.dataset.target);
            if (!input) return;
            const visible = input.type === "text";
            input.type = visible ? "password" : "text";
            boton.textContent = visible ? "Mostrar" : "Ocultar";
        });
    });

    [
        registroCorreo,
        registroNombre,
        registroApellidos,
        registroDocumento,
        registroCelular,
        registroPassword,
        codigoVerificacion
    ].forEach((elemento) => {
        if (!elemento) return;
        elemento.addEventListener("input", validarFormularioRegistro);
        elemento.addEventListener("change", validarFormularioRegistro);
    });

    if (codigoVerificacion) {
        codigoVerificacion.addEventListener("input", () => {
            codigoVerificacion.value = codigoVerificacion.value.replace(/\D/g, "").slice(0, 6);
            validarFormularioRegistro();
        });
    }

    btnReenviar?.addEventListener("click", enviarCodigo);

    formRegistro?.addEventListener("submit", async (e) => {
        e.preventDefault();
        if (!codigoEnviado) {
            await enviarCodigo();
        } else {
            await registrarCuenta();
        }
    });

    validarFormularioRegistro();
});
