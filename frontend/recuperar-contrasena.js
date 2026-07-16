const formRecuperar = document.getElementById("form-recuperar");
const correo = document.getElementById("correo");
const codigo = document.getElementById("codigo-verificacion");
const nuevaPassword = document.getElementById("nueva-password");
const confirmarPassword = document.getElementById("confirmar-password");
const mensajeRecuperar = document.getElementById("mensaje-recuperar");
const btnEnviarCodigo = document.getElementById("btn-enviar-codigo");

btnEnviarCodigo?.addEventListener("click", async () => {
    const correoValor = correo.value.trim().toLowerCase();

    if (!validarCorreo(correoValor)) {
        mostrarMensaje("Ingresa un correo válido para enviarte el código.", "error");
        return;
    }

    try {
        FastMarket.marcarCargando(btnEnviarCodigo, true, "Enviando...");
        const resp = await FastMarket.request("/auth/recuperar/enviar-codigo", {
            method: "POST",
            body: { correo: correoValor }
        });
        mostrarMensaje(resp?.mensaje || "Revisa tu correo e ingresa el código recibido.", "ok");
        codigo?.focus();
    } catch (error) {
        mostrarMensaje(error.message, "error");
    } finally {
        FastMarket.marcarCargando(btnEnviarCodigo, false);
    }
});

formRecuperar.addEventListener("submit", async (e) => {
    e.preventDefault();

    const correoValor = correo.value.trim().toLowerCase();
    const codigoValor = codigo.value.trim();
    const nuevaValor = nuevaPassword.value.trim();
    const confirmarValor = confirmarPassword.value.trim();

    if (correoValor === "" || codigoValor === "" || nuevaValor === "" || confirmarValor === "") {
        mostrarMensaje("Completa todos los campos, incluido el código recibido.", "error");
        return;
    }

    if (!validarCorreo(correoValor)) {
        mostrarMensaje("Ingresa un correo válido.", "error");
        return;
    }

    if (!/^\d{6}$/.test(codigoValor)) {
        mostrarMensaje("El código debe tener 6 dígitos.", "error");
        return;
    }

    if (!validarPasswordSegura(nuevaValor)) {
        mostrarMensaje(mensajePasswordSegura(), "error");
        return;
    }

    if (nuevaValor !== confirmarValor) {
        mostrarMensaje("Las contraseñas no coinciden.", "error");
        return;
    }

    try {
        FastMarket.marcarCargando(formRecuperar.querySelector('button[type="submit"]'), true, "Actualizando...");
        await FastMarket.request("/auth/recuperar", {
            method: "POST",
            body: {
                correo: correoValor,
                codigoVerificacion: codigoValor,
                passwordNueva: nuevaValor
            }
        });

        FastMarket.cerrarSesion();
        mostrarMensaje("Contraseña actualizada correctamente. Vuelve a iniciar sesión.", "ok");

        setTimeout(() => {
            window.location.href = "login.html";
        }, 1200);
    } catch (error) {
        mostrarMensaje(error.message, "error");
    } finally {
        FastMarket.marcarCargando(formRecuperar.querySelector('button[type="submit"]'), false);
    }
});

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

function validarCorreo(valor) {
    return valor.includes("@") && valor.includes(".");
}

function mostrarMensaje(texto, tipo) {
    mensajeRecuperar.textContent = texto;
    mensajeRecuperar.style.color = tipo === "ok" ? "#16a34a" : "#dc2626";
}
