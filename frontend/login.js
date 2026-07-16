document.addEventListener("DOMContentLoaded", () => {
    const formLogin = document.getElementById("form-login");
    const loginCorreo = document.getElementById("login-correo");
    const loginPassword = document.getElementById("login-password");
    const mensajeLogin = document.getElementById("mensaje-login");

    function mostrarMensaje(elemento, texto, tipo = "error") {
        if (!elemento) return;
        elemento.textContent = texto;
        elemento.classList.remove("error", "ok");
        elemento.classList.add(tipo);
    }

    function validarCorreo(correo) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo);
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

    formLogin?.addEventListener("submit", async (e) => {
        e.preventDefault();

        const correo = loginCorreo.value.trim().toLowerCase();
        const password = loginPassword.value.trim();

        if (!validarCorreo(correo)) {
            mostrarMensaje(mensajeLogin, "Ingresa un correo válido.");
            return;
        }

        if (!password) {
            mostrarMensaje(mensajeLogin, "Ingresa tu contraseña.");
            return;
        }

        const boton = formLogin.querySelector(".btn-auth");
        boton.disabled = true;
        boton.textContent = "Ingresando...";
        mostrarMensaje(mensajeLogin, "Validando tus datos...", "ok");

        try {
            const respuesta = await FastMarket.request("/auth/login", {
                method: "POST",
                body: { correo, password }
            });

            const usuario = FastMarket.guardarSesion(respuesta);
            mostrarMensaje(mensajeLogin, "Inicio de sesión correcto. Redirigiendo...", "ok");

            setTimeout(() => {
                const params = new URLSearchParams(window.location.search);
                const redirect = params.get("redirect");
                const carritoPendiente = (() => {
                    try {
                        return JSON.parse(sessionStorage.getItem("fastmarket_checkout_carrito") || localStorage.getItem("fastmarket_carrito") || "[]").length > 0;
                    } catch {
                        return false;
                    }
                })();

                if (usuario.rol === "ADMIN") {
                    window.location.href = "admin.html";
                    return;
                }
                if (usuario.rol === "VENDEDOR") {
                    window.location.href = "vendedor.html";
                    return;
                }

                if (redirect && !redirect.startsWith("http") && !redirect.includes("//")) {
                    window.location.href = redirect;
                    return;
                }

                window.location.href = carritoPendiente ? "checkout.html" : "productos.html";
            }, 600);
        } catch (error) {
            mostrarMensaje(mensajeLogin, error.message || "Correo o contraseña incorrectos.");
            boton.disabled = false;
            boton.textContent = "Entrar";
        }
    });
});
