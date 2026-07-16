(function (global) {
    "use strict";

    const CONFIGURACION = {
        moda: {
            tipos: {
                "Ropa": ["Marca", "Talla", "Color", "Material", "Composición", "Género", "Temporada"],
                "Calzado": ["Marca", "Modelo", "Número", "Color", "Material", "Tipo de suela", "Género"],
                "Accesorio": ["Marca", "Color", "Material", "Medidas", "Género"],
                "Bolso o mochila": ["Marca", "Modelo", "Color", "Material", "Medidas", "Capacidad"],
                "Joyería o bisutería": ["Marca", "Color", "Material", "Medidas", "Tipo de cierre"]
            },
            base: ["Marca", "Color", "Material", "Talla o medida", "Condición"]
        },
        tecnologia: {
            tipos: {
                "Celular": ["Marca", "Modelo", "Color", "Almacenamiento", "Memoria RAM", "Tamaño de pantalla", "Batería", "Garantía"],
                "Laptop": ["Marca", "Modelo", "Procesador", "Memoria RAM", "Almacenamiento", "Tamaño de pantalla", "Sistema operativo", "Garantía"],
                "Tablet": ["Marca", "Modelo", "Almacenamiento", "Memoria RAM", "Tamaño de pantalla", "Conectividad", "Garantía"],
                "Audífonos": ["Marca", "Modelo", "Color", "Conectividad", "Autonomía", "Tipo de conexión", "Garantía"],
                "Smartwatch": ["Marca", "Modelo", "Color", "Tamaño de pantalla", "Compatibilidad", "Autonomía", "Garantía"],
                "Accesorio tecnológico": ["Marca", "Modelo", "Color", "Compatibilidad", "Tipo de conexión", "Garantía"]
            },
            base: ["Marca", "Modelo", "Color", "Garantía", "Condición"]
        },
        hogar: {
            tipos: {
                "Mueble": ["Marca", "Material", "Color", "Medidas", "Peso", "Requiere armado"],
                "Electrodoméstico": ["Marca", "Modelo", "Color", "Capacidad", "Potencia", "Voltaje", "Garantía"],
                "Decoración": ["Marca", "Material", "Color", "Medidas", "Estilo"],
                "Cocina": ["Marca", "Material", "Color", "Capacidad", "Medidas", "Apto para"],
                "Iluminación": ["Marca", "Modelo", "Material", "Color de luz", "Potencia", "Voltaje", "Medidas"]
            },
            base: ["Marca", "Material", "Color", "Medidas", "Condición"]
        },
        estudio: {
            tipos: {
                "Libro": ["Título", "Autor", "Editorial", "Edición", "Idioma", "Número de páginas", "ISBN"],
                "Cuaderno": ["Marca", "Tamaño", "Número de hojas", "Tipo de rayado", "Material de tapa"],
                "Mochila": ["Marca", "Modelo", "Color", "Material", "Capacidad", "Medidas"],
                "Útil escolar": ["Marca", "Color", "Material", "Cantidad incluida", "Medidas"],
                "Organizador o escritorio": ["Marca", "Material", "Color", "Medidas", "Requiere armado"]
            },
            base: ["Marca", "Material", "Color", "Tamaño o medida", "Condición"]
        },
        belleza: {
            tipos: {
                "Maquillaje": ["Marca", "Línea", "Tono", "Acabado", "Contenido", "Tipo de piel", "Fecha de vencimiento"],
                "Cuidado facial": ["Marca", "Presentación", "Contenido", "Tipo de piel", "Ingredientes principales", "Modo de uso"],
                "Cuidado capilar": ["Marca", "Presentación", "Contenido", "Tipo de cabello", "Ingredientes principales", "Modo de uso"],
                "Perfume": ["Marca", "Nombre de fragancia", "Contenido", "Familia olfativa", "Concentración", "Género"],
                "Higiene personal": ["Marca", "Presentación", "Contenido", "Ingredientes principales", "Modo de uso"]
            },
            base: ["Marca", "Presentación", "Contenido", "Condición"]
        },
        deportes: {
            tipos: {
                "Ropa deportiva": ["Marca", "Talla", "Color", "Material", "Deporte recomendado", "Género"],
                "Calzado deportivo": ["Marca", "Modelo", "Número", "Color", "Material", "Deporte recomendado", "Tipo de suela"],
                "Equipo deportivo": ["Marca", "Modelo", "Material", "Medidas", "Peso", "Deporte recomendado", "Garantía"],
                "Accesorio deportivo": ["Marca", "Material", "Color", "Talla o medida", "Deporte recomendado"],
                "Artículo de entrenamiento": ["Marca", "Modelo", "Material", "Medidas", "Peso", "Nivel recomendado", "Garantía"]
            },
            base: ["Marca", "Material", "Talla o medida", "Deporte recomendado", "Condición"]
        },
        juguetes: {
            tipos: {
                "Figura o muñeco": ["Marca", "Personaje", "Material", "Medidas", "Edad recomendada", "Articulado"],
                "Juego de mesa": ["Marca", "Edad recomendada", "Número de jugadores", "Duración aproximada", "Contenido de la caja"],
                "Peluche": ["Marca", "Personaje", "Material", "Medidas", "Edad recomendada", "Lavable"],
                "Juguete educativo": ["Marca", "Material", "Edad recomendada", "Habilidad que desarrolla", "Número de piezas"],
                "Vehículo de juguete": ["Marca", "Modelo", "Material", "Escala", "Edad recomendada", "Requiere pilas"],
                "Juguete para exteriores": ["Marca", "Material", "Medidas", "Edad recomendada", "Peso máximo soportado"]
            },
            base: ["Marca", "Material", "Edad recomendada", "Medidas", "Condición"]
        }
    };

    function normalizar(valor) {
        return String(valor || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, " ")
            .trim();
    }

    function escaparAtributo(valor) {
        return String(valor || "")
            .replace(/&/g, "&amp;")
            .replace(/"/g, "&quot;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    function obtenerConfiguracion(categoria) {
        return CONFIGURACION[normalizar(categoria).replace(/ /g, "_")] || CONFIGURACION[categoria] || {
            tipos: {},
            base: ["Marca", "Modelo", "Color", "Material", "Medidas", "Garantía", "Condición"]
        };
    }

    function crear(opciones) {
        const categoria = document.getElementById(opciones.categoriaId);
        const tipo = document.getElementById(opciones.tipoId);
        const datalist = document.getElementById(opciones.datalistId);
        const toggle = document.getElementById(opciones.toggleId);
        const panel = document.getElementById(opciones.panelId);
        const sugerencias = document.getElementById(opciones.sugerenciasId);
        const lista = document.getElementById(opciones.listaId);
        const agregar = document.getElementById(opciones.agregarId);

        if (!categoria || !tipo || !toggle || !panel || !sugerencias || !lista || !agregar) {
            return crearControlVacio();
        }

        function configuracionActual() {
            return obtenerConfiguracion(categoria.value);
        }

        function actualizarTipos() {
            if (!datalist) return;
            const nombres = Object.keys(configuracionActual().tipos || {});
            datalist.innerHTML = nombres.map((nombre) => `<option value="${escaparAtributo(nombre)}"></option>`).join("");
        }

        function nombresAgregados() {
            return new Set(
                Array.from(lista.querySelectorAll("[data-caracteristica-nombre]"))
                    .map((input) => normalizar(input.value))
                    .filter(Boolean)
            );
        }

        function sugerenciasActuales() {
            const config = configuracionActual();
            const tipoIngresado = normalizar(tipo.value);
            const tipoCoincidente = Object.entries(config.tipos || {}).find(([nombre]) => normalizar(nombre) === tipoIngresado);
            const resultado = [...(config.base || []), ...(tipoCoincidente ? tipoCoincidente[1] : [])];
            return [...new Set(resultado)];
        }

        function actualizarSugerencias() {
            if (!toggle.checked) {
                sugerencias.innerHTML = "";
                return;
            }
            const existentes = nombresAgregados();
            const disponibles = sugerenciasActuales().filter((nombre) => !existentes.has(normalizar(nombre)));
            sugerencias.innerHTML = disponibles.length
                ? disponibles.map((nombre) => `<button type="button" class="chip-caracteristica" data-sugerir-caracteristica="${escaparAtributo(nombre)}">+ ${escaparAtributo(nombre)}</button>`).join("")
                : `<span class="sin-sugerencias">Ya agregaste las sugerencias disponibles. Puedes crear otra característica.</span>`;
        }

        function cambiarEstadoPanel(activo) {
            toggle.checked = !!activo;
            panel.classList.toggle("oculto", !activo);
            panel.setAttribute("aria-hidden", activo ? "false" : "true");
            actualizarSugerencias();
        }

        function agregarFila(nombre = "", valor = "", enfocar = false) {
            const clave = normalizar(nombre);
            if (clave) {
                const existente = Array.from(lista.querySelectorAll("[data-caracteristica-nombre]"))
                    .find((input) => normalizar(input.value) === clave);
                if (existente) {
                    existente.closest(".fila-caracteristica")?.querySelector("[data-caracteristica-valor]")?.focus();
                    return;
                }
            }

            const fila = document.createElement("div");
            fila.className = "fila-caracteristica";
            fila.innerHTML = `
                <input type="text" data-caracteristica-nombre maxlength="80" placeholder="Característica" aria-label="Nombre de la característica" value="${escaparAtributo(nombre)}">
                <input type="text" data-caracteristica-valor maxlength="500" placeholder="Valor" aria-label="Valor de la característica" value="${escaparAtributo(valor)}">
                <button type="button" class="quitar-caracteristica" data-quitar-caracteristica aria-label="Quitar característica">×</button>`;
            lista.appendChild(fila);
            actualizarSugerencias();
            if (enfocar) fila.querySelector("[data-caracteristica-nombre]")?.focus();
        }

        function obtenerCaracteristicas() {
            if (!toggle.checked) return {};
            const resultado = {};
            const claves = new Set();
            lista.querySelectorAll(".fila-caracteristica").forEach((fila) => {
                const nombre = String(fila.querySelector("[data-caracteristica-nombre]")?.value || "").trim();
                const valor = String(fila.querySelector("[data-caracteristica-valor]")?.value || "").trim();
                const clave = normalizar(nombre);
                if (!nombre || !valor || !clave || claves.has(clave)) return;
                claves.add(clave);
                resultado[nombre] = valor;
            });
            return resultado;
        }

        function buscarValor(mapa, ...nombres) {
            const buscados = nombres.map(normalizar);
            const entrada = Object.entries(mapa || {}).find(([nombre]) => buscados.includes(normalizar(nombre)));
            return entrada ? entrada[1] : "";
        }

        function obtenerCamposCompatibilidad() {
            const mapa = obtenerCaracteristicas();
            return {
                marca: buscarValor(mapa, "Marca"),
                modelo: buscarValor(mapa, "Modelo"),
                color: buscarValor(mapa, "Color"),
                material: buscarValor(mapa, "Material", "Composición"),
                talla: buscarValor(mapa, "Talla", "Talla o medida", "Número", "Tamaño o medida", "Medidas"),
                garantia: buscarValor(mapa, "Garantía"),
                condicion: buscarValor(mapa, "Condición")
            };
        }

        function datosLegacy(producto) {
            const mapa = {};
            const agregarSiExiste = (nombre, valor) => {
                const limpio = String(valor || "").trim();
                if (limpio) mapa[nombre] = limpio;
            };
            agregarSiExiste("Marca", producto?.marca);
            agregarSiExiste("Modelo", producto?.modelo);
            agregarSiExiste("Color", producto?.color);
            agregarSiExiste("Material", producto?.material);
            agregarSiExiste("Talla o medida", producto?.talla);
            agregarSiExiste("Garantía", producto?.garantia);
            agregarSiExiste("Condición", producto?.condicion);
            return mapa;
        }

        function establecerDatos(producto) {
            tipo.value = producto?.tipoProducto || "";
            lista.innerHTML = "";
            const personalizadas = producto?.caracteristicas && typeof producto.caracteristicas === "object" && !Array.isArray(producto.caracteristicas)
                ? producto.caracteristicas
                : {};
            const mapa = Object.keys(personalizadas).length ? personalizadas : datosLegacy(producto);
            Object.entries(mapa).forEach(([nombre, valor]) => {
                if (String(nombre || "").trim() && String(valor || "").trim()) agregarFila(nombre, valor);
            });
            cambiarEstadoPanel(Object.keys(mapa).length > 0);
            actualizarTipos();
            actualizarSugerencias();
        }

        function reiniciar() {
            tipo.value = "";
            lista.innerHTML = "";
            cambiarEstadoPanel(false);
            actualizarTipos();
        }

        toggle.addEventListener("change", () => cambiarEstadoPanel(toggle.checked));
        agregar.addEventListener("click", () => {
            cambiarEstadoPanel(true);
            agregarFila("", "", true);
        });
        sugerencias.addEventListener("click", (evento) => {
            const boton = evento.target.closest("[data-sugerir-caracteristica]");
            if (!boton) return;
            agregarFila(boton.dataset.sugerirCaracteristica || "", "", true);
        });
        lista.addEventListener("click", (evento) => {
            const boton = evento.target.closest("[data-quitar-caracteristica]");
            if (!boton) return;
            boton.closest(".fila-caracteristica")?.remove();
            actualizarSugerencias();
        });
        lista.addEventListener("input", actualizarSugerencias);
        categoria.addEventListener("change", () => {
            actualizarTipos();
            actualizarSugerencias();
        });
        tipo.addEventListener("input", actualizarSugerencias);

        actualizarTipos();
        cambiarEstadoPanel(false);

        return {
            getTipo: () => String(tipo.value || "").trim(),
            getCaracteristicas: obtenerCaracteristicas,
            getCamposCompatibilidad: obtenerCamposCompatibilidad,
            setData: establecerDatos,
            reset: reiniciar
        };
    }

    function crearControlVacio() {
        return {
            getTipo: () => "",
            getCaracteristicas: () => ({}),
            getCamposCompatibilidad: () => ({ marca: "", modelo: "", color: "", material: "", talla: "", garantia: "", condicion: "" }),
            setData: () => {},
            reset: () => {}
        };
    }

    global.FastMarketProductoCaracteristicas = { crear };
})(window);
