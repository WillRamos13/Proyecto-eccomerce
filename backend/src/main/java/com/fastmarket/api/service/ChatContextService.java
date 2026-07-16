package com.fastmarket.api.service;

import com.fastmarket.api.model.Pedido;
import com.fastmarket.api.model.PedidoItem;
import com.fastmarket.api.model.Producto;
import com.fastmarket.api.repository.PedidoRepository;
import com.fastmarket.api.repository.ProductoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatContextService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final BigDecimal COSTO_ENVIO_DEFECTO = new BigDecimal("8.00");
    private static final BigDecimal ENVIO_GRATIS_DESDE = new BigDecimal("250.00");
    private static final DateTimeFormatter FECHA_PEDIDO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Set<String> TERMINOS_GENERICOS = Set.of(
            "producto", "productos", "articulo", "articulos", "catalogo", "precio", "precios",
            "cuanto", "cuesta", "cuestan", "stock", "disponible", "disponibles", "tienen", "tienes",
            "quiero", "quisiera", "comprar", "compra", "venden", "vende", "mostrar", "muestra",
            "buscar", "busco", "recomienda", "recomiendame", "mejor", "barato", "barata", "baratos",
            "oferta", "ofertas", "promocion", "promociones", "descuento", "descuentos", "rebaja",
            "marca", "modelo", "color", "talla", "material", "garantia", "condicion", "detalle", "detalles",
            "que", "cual", "cuales", "como", "donde", "cuando", "hay", "con", "para", "por", "del",
            "las", "los", "una", "uno", "unos", "unas", "este", "esta", "ese", "esa", "favor",
            "algo", "de", "la", "el", "un", "mi", "tu", "si", "no", "en", "es", "lo", "al",
            "se", "me", "te", "ya", "y", "o", "a"
    );

    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final SystemConfigService systemConfigService;

    public ChatContextService(
            ProductoRepository productoRepository,
            PedidoRepository pedidoRepository,
            SystemConfigService systemConfigService
    ) {
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.systemConfigService = systemConfigService;
    }

    public ChatContext construirContexto(String mensaje, Long usuarioId) {
        return construirContexto(mensaje, mensaje, usuarioId);
    }

    public ChatContext construirContexto(String mensaje, String consultaContextual, Long usuarioId) {
        String textoActual = normalizar(mensaje);
        String textoContextual = normalizar(consultaContextual);
        BigDecimal costoEnvio = systemConfigService.obtenerDecimal(SystemConfigService.COSTO_ENVIO, COSTO_ENVIO_DEFECTO);
        if (costoEnvio.signum() < 0) costoEnvio = COSTO_ENVIO_DEFECTO;

        boolean hablaOfertas = contiene(textoContextual,
                "oferta", "ofertas", "promocion", "promociones", "descuento", "descuentos", "rebaja");
        boolean hablaProductos = hablaOfertas || contiene(textoContextual,
                "producto", "productos", "catalogo", "precio", "precios", "cuesta", "cuestan", "stock",
                "disponible", "disponibles", "comprar", "articulo", "articulos", "venden", "tienen",
                "hay", "quiero", "busco", "marca", "modelo", "color", "talla", "material", "garantia", "condicion", "recomienda",
                "recomiendame", "barato", "barata", "caro", "cara", "categoria", "categorias");
        boolean hablaPedidos = contiene(textoActual,
                "pedido", "pedidos", "orden", "ordenes", "seguimiento", "rastrear", "estado de mi compra",
                "mi compra", "mis compras");
        boolean hablaCategorias = contiene(textoActual, "categoria", "categorias", "tipos de productos", "que venden", "que productos venden");

        List<Producto> productosActivosCache = null;
        if (!hablaProductos && !hablaPedidos && textoContextual.split("\\s+").length <= 4) {
            productosActivosCache = productoRepository.findByActivoTrueOrderByIdDesc();
            hablaProductos = coincideConsultaConCatalogo(textoContextual, productosActivosCache);
        }

        List<Producto> productosSeleccionados = List.of();
        List<String> categorias = List.of();
        boolean busquedaEspecifica = false;
        boolean usaDatos = false;

        if (hablaProductos || hablaCategorias) {
            List<Producto> base = hablaOfertas
                    ? productoRepository.findByActivoTrueAndOfertaTrueOrderByIdDesc()
                    : productosActivosCache != null ? productosActivosCache : productoRepository.findByActivoTrueOrderByIdDesc();

            List<String> terminos = terminosSignificativos(textoContextual);
            busquedaEspecifica = !terminos.isEmpty();
            productosSeleccionados = seleccionarProductos(base, textoContextual, terminos, 12);
            List<Producto> productosParaCategorias = productosActivosCache != null
                    ? productosActivosCache
                    : productoRepository.findByActivoTrueOrderByIdDesc();
            categorias = obtenerCategorias(productosParaCategorias);
            usaDatos = true;
        }

        List<Pedido> pedidos = List.of();
        if (hablaPedidos) {
            if (usuarioId != null) {
                pedidos = pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream().limit(5).toList();
            }
            usaDatos = true;
        }

        StringBuilder contexto = new StringBuilder();
        contexto.append("Información funcional confirmada de FastMarket:\n");
        contexto.append("- FastMarket es una tienda online con Inicio, Productos, carrito, checkout, Mis pedidos, perfil, reclamos y Centro de ayuda.\n");
        contexto.append("- No existe una página independiente de Ofertas. Las promociones se ven en Inicio o en Productos usando el filtro de ofertas.\n");
        contexto.append("- Para comprar, el cliente inicia sesión, agrega productos al carrito, entra al checkout, completa la entrega y confirma el pedido.\n");
        contexto.append("- Métodos de pago disponibles: Pago contra entrega, Yape / Plin, Transferencia bancaria y Mercado Pago.\n");
        contexto.append("- El costo base de envío configurado es ").append(dinero(costoEnvio)).append(".\n");
        contexto.append("- El envío es gratuito cuando el subtotal alcanza ").append(dinero(ENVIO_GRATIS_DESDE)).append(".\n");
        contexto.append("- La cobertura y el horario exacto de entrega dependen de la dirección, zona y disponibilidad; no están definidos de forma universal.\n");
        contexto.append("- Los cupones se aplican en el checkout y el backend valida si están activos, vigentes y son aplicables al carrito.\n");
        contexto.append("- Registro: nombre, apellidos, correo, documento, celular, contraseña segura y código de verificación de 6 dígitos enviado al correo.\n");
        contexto.append("- Recuperación de contraseña: solicitar código al correo registrado, ingresar el código de 6 dígitos y definir una contraseña nueva.\n");
        contexto.append("- Una contraseña válida tiene mínimo 8 caracteres, mayúscula, minúscula, número y no contiene espacios.\n");
        contexto.append("- Estados del pedido: PENDIENTE, CONFIRMADO, PREPARANDO, CAMINO, ENTREGADO y CANCELADO.\n");
        contexto.append("- El chatbot orienta, pero no ejecuta pagos, compras, cancelaciones ni modificaciones de cuenta o pedidos.\n");
        contexto.append("- Los clientes autenticados pueden registrar reclamos desde Mi perfil > Reclamos y atención, asociarlos opcionalmente a un pedido y revisar la respuesta del administrador.\n- Si falta información comercial o de soporte, dirige al Centro de ayuda o a los datos de contacto visibles en el pie de página.\n\n");

        if (hablaOfertas) {
            contexto.append("Productos reales en oferta coincidentes con la consulta:\n");
            anexarProductos(contexto, productosSeleccionados,
                    "- No hay ofertas activas que coincidan con la consulta.");
        } else if (hablaProductos || hablaCategorias) {
            contexto.append("Productos reales activos coincidentes con la consulta:\n");
            anexarProductos(contexto, productosSeleccionados,
                    busquedaEspecifica
                            ? "- No se encontraron productos activos que coincidan con la búsqueda."
                            : "- No hay productos activos registrados en este momento.");
        }

        if (hablaCategorias) {
            contexto.append("Categorías reales disponibles:\n");
            if (categorias.isEmpty()) contexto.append("- No hay categorías disponibles.\n");
            else categorias.forEach(categoria -> contexto.append("- ").append(categoria).append("\n"));
            contexto.append("\n");
        }

        if (hablaPedidos) {
            contexto.append("Pedidos del cliente autenticado:\n");
            if (usuarioId == null) {
                contexto.append("- El usuario no ha iniciado sesión o su token no pudo validarse.\n");
            } else if (pedidos.isEmpty()) {
                contexto.append("- El usuario no tiene pedidos registrados.\n");
            } else {
                pedidos.forEach(pedido -> contexto.append(formatearPedido(pedido)).append("\n"));
            }
            contexto.append("\n");
        }

        return new ChatContext(
                contexto.toString(),
                usaDatos,
                productosSeleccionados,
                pedidos,
                categorias,
                costoEnvio,
                usuarioId != null,
                busquedaEspecifica,
                hablaOfertas
        );
    }

    public String respuestaLocal(String mensaje, ChatContext contexto) {
        String texto = normalizar(mensaje);

        if (texto.isBlank()) {
            return "Escribe tu consulta y te ayudaré con productos, compras, pagos, envíos, pedidos o tu cuenta.";
        }

        if (esSoloSaludo(texto)) {
            return "¡Hola! 👋 Soy el asistente de FastMarket. Puedo ayudarte con productos, precios, stock, promociones, carrito, pagos, envíos, pedidos y tu cuenta.";
        }

        if (esAgradecimiento(texto)) {
            return "¡Con gusto! 😊 Estoy aquí para ayudarte con cualquier consulta sobre FastMarket.";
        }

        if (esDespedida(texto)) {
            return "¡Hasta luego! Gracias por visitar FastMarket. 👋";
        }

        if (contiene(texto, "reclamo", "queja", "incidencia", "problema con mi compra")) {
            return "Para registrar un reclamo, inicia sesión y entra a Mi perfil > Reclamos y atención. Puedes seleccionar el pedido relacionado, indicar el tipo de problema, describir lo ocurrido y luego revisar allí el estado y la respuesta del administrador.";
        }

        if (contiene(texto, "hablar con una persona", "asesor", "soporte", "atencion al cliente", "contacto")) {
            return "Para atención personal, revisa el Centro de ayuda o los datos de contacto visibles en el pie de página de FastMarket. También puedes registrar un reclamo desde Mi perfil. No compartas contraseñas ni códigos de verificación por el chat.";
        }

        if (contiene(texto, "devolucion", "devolver", "reembolso", "cambio de producto")) {
            return "Revisa el pedido en Mis pedidos y registra el caso desde Mi perfil > Reclamos y atención, seleccionando el tipo Devolución. El equipo de FastMarket podrá evaluar y responder tu solicitud.";
        }

        if (contiene(texto, "olvide mi contraseña", "olvide la contraseña", "recuperar contraseña", "cambiar contraseña", "codigo de recuperacion")) {
            return "Para recuperar tu contraseña:\n1. Entra a Recuperar contraseña.\n2. Escribe tu correo registrado y solicita el código.\n3. Ingresa el código de 6 dígitos recibido.\n4. Crea una contraseña nueva de mínimo 8 caracteres, con mayúscula, minúscula y número, sin espacios.";
        }

        if (contiene(texto, "registrarme", "crear cuenta", "registro", "como me registro")) {
            return "Para crear tu cuenta, entra a Registrarse y completa nombre, apellidos, correo, documento, celular y contraseña. Después solicita e ingresa el código de 6 dígitos enviado a tu correo.";
        }

        if (contiene(texto, "iniciar sesion", "inicio sesion", "entrar a mi cuenta", "no puedo ingresar", "login")) {
            return "Entra a Iniciar sesión, escribe tu correo y contraseña registrados y presiona Entrar. Si olvidaste la contraseña, usa la opción Recuperar contraseña.";
        }

        if (contiene(texto, "editar perfil", "cambiar mis datos", "mi direccion", "mis direcciones", "actualizar telefono", "actualizar correo")) {
            return "Inicia sesión y entra a Perfil. Desde allí puedes revisar y actualizar tus datos personales y direcciones disponibles.";
        }

        if (contiene(texto, "que significa pendiente", "que significa confirmado", "que significa preparando",
                "que significa camino", "que significa entregado", "que significa cancelado", "estados del pedido")) {
            return "Los estados significan:\n• PENDIENTE: pedido recibido y pendiente de revisión.\n• CONFIRMADO: pedido aceptado.\n• PREPARANDO: productos en preparación.\n• CAMINO: pedido en reparto.\n• ENTREGADO: entrega finalizada.\n• CANCELADO: pedido anulado.";
        }

        if (contiene(texto, "cancelar pedido", "anular pedido", "quiero cancelar")) {
            return "No puedo cancelar pedidos desde el chatbot. Revisa el pedido en Mis pedidos y comunícate con soporte para solicitar la evaluación de la cancelación.";
        }

        if (contiene(texto, "pedido", "pedidos", "orden", "ordenes", "seguimiento", "rastrear", "estado de mi compra")) {
            if (!contexto.usuarioAutenticado()) {
                return "Para consultar pedidos reales debes iniciar sesión. Después entra a Mis pedidos o vuelve a preguntarme por el estado de tu compra.";
            }
            if (contexto.pedidos().isEmpty()) {
                return "No encontré pedidos registrados en tu cuenta. Cuando confirmes una compra, aparecerá en Mis pedidos.";
            }

            Pedido pedidoMencionado = buscarPedidoMencionado(texto, contexto.pedidos());
            if (pedidoMencionado != null) {
                return resumenPedidoCliente(pedidoMencionado)
                        + "\n\nPara ver el seguimiento y detalle completo, entra a Mis pedidos.";
            }

            return "Estos son tus pedidos más recientes:\n"
                    + contexto.pedidos().stream()
                    .limit(5)
                    .map(this::lineaPedidoCliente)
                    .collect(Collectors.joining("\n"))
                    + "\n\nPara ver productos, dirección y seguimiento, entra a Mis pedidos.";
        }

        if (contiene(texto, "cupon", "cupones", "codigo de descuento", "aplicar descuento")) {
            return "Los cupones se aplican en el checkout:\n1. Agrega productos al carrito.\n2. Entra a Finalizar compra.\n3. Escribe el código en Cupón de descuento.\n4. Presiona Aplicar.\nEl sistema verificará si el cupón está activo, vigente y es válido para tu carrito.";
        }

        if (contiene(texto, "carrito", "agregar al carrito", "quitar del carrito", "vaciar carrito", "cambiar cantidad")) {
            return "En Productos puedes presionar Agregar al carrito. Dentro del carrito puedes cambiar cantidades o quitar artículos. Cuando esté listo, continúa al checkout para confirmar la compra.";
        }

        if (contiene(texto, "como comprar", "finalizar compra", "confirmar compra", "checkout", "hacer una compra")) {
            return "Para comprar:\n1. Inicia sesión.\n2. Agrega productos al carrito.\n3. Entra al checkout.\n4. Confirma tus datos y dirección.\n5. Elige el método de pago.\n6. Presiona Confirmar pedido o Pagar con Mercado Pago.";
        }

        if (contiene(texto, "pago", "pagos", "yape", "plin", "efectivo", "contra entrega", "transferencia", "mercado pago", "tarjeta")) {
            return "FastMarket ofrece estos métodos de pago:\n• Pago contra entrega.\n• Yape / Plin.\n• Transferencia bancaria.\n• Mercado Pago para completar el pago en su plataforma.\nEl método se selecciona durante el checkout.";
        }

        if (contiene(texto, "envio gratis", "delivery gratis", "cuando es gratis el envio")) {
            return "El envío es gratuito cuando el subtotal de productos alcanza S/ 250.00. Los descuentos se aplican por separado en el checkout.";
        }

        if (contiene(texto, "costo de envio", "cuanto cuesta el envio", "precio del envio", "delivery cuesta")) {
            return "El costo base de envío configurado es " + dinero(contexto.costoEnvio())
                    + ". Si el subtotal llega a S/ 250.00, el envío es gratuito.";
        }

        if (contiene(texto, "envio", "envios", "delivery", "entrega", "direccion de entrega", "zona de reparto", "horario de entrega", "cuanto demora")) {
            return "La entrega se coordina según dirección, zona, horario y disponibilidad. En el checkout debes ingresar dirección, referencia, teléfono y elegir mañana, tarde o noche. El tiempo exacto y la cobertura deben confirmarse con la tienda.";
        }

        if (contiene(texto, "categoria", "categorias", "que venden", "que productos venden", "tipos de productos")) {
            if (contexto.categorias().isEmpty()) {
                return "Actualmente no encontré categorías activas registradas.";
            }
            return "Estas son las categorías disponibles en FastMarket:\n• "
                    + String.join("\n• ", contexto.categorias())
                    + "\n\nPuedes explorarlas en Productos.";
        }

        boolean consultaOferta = contiene(texto,
                "oferta", "ofertas", "promocion", "promociones", "descuento", "descuentos", "rebaja");
        boolean consultaProducto = consultaOferta
                || (contexto.busquedaProductoEspecifica() && !contexto.productos().isEmpty())
                || contiene(texto,
                "producto", "productos", "catalogo", "precio", "precios", "cuesta", "cuestan", "stock",
                "disponible", "disponibles", "comprar", "articulo", "articulos", "venden", "tienen",
                "hay", "quiero", "busco", "marca", "modelo", "color", "talla", "material", "garantia", "condicion", "recomienda",
                "recomiendame", "barato", "barata", "caro", "cara");

        if (consultaProducto) {
            if (contexto.productos().isEmpty()) {
                if (consultaOferta) {
                    return contexto.busquedaProductoEspecifica()
                            ? "No encontré una oferta activa que coincida con tu búsqueda. Revisa Productos y activa el filtro de ofertas para ver las promociones disponibles."
                            : "Por ahora no hay ofertas activas registradas. Puedes revisar el catálogo porque las promociones cambian según stock y temporada.";
                }
                return contexto.busquedaProductoEspecifica()
                        ? "No encontré productos activos que coincidan con esa búsqueda. Prueba con el nombre, categoría, marca o modelo."
                        : "Por ahora no hay productos activos registrados en FastMarket.";
            }

            List<Producto> productos = ordenarParaConsulta(contexto.productos(), texto);
            boolean pideDetalles = contiene(texto, "detalle", "detalles", "caracteristicas", "especificaciones",
                    "marca", "modelo", "color", "talla", "material", "garantia", "condicion");

            String encabezado = consultaOferta
                    ? "Estas promociones están disponibles ahora:\n"
                    : contiene(texto, "recomienda", "recomiendame", "mejor")
                    ? "Estas opciones reales pueden servirte:\n"
                    : "Encontré estos productos:\n";

            String listado = productos.stream()
                    .limit(6)
                    .map(producto -> lineaProductoCliente(producto, pideDetalles || contexto.busquedaProductoEspecifica()))
                    .collect(Collectors.joining("\n"));

            String cierre = consultaOferta
                    ? "\n\nPuedes verlos en Productos usando el filtro de ofertas."
                    : "\n\nAbre Productos para ver imágenes, detalles y agregar al carrito.";

            return encabezado + listado + cierre;
        }

        if (contiene(texto, "ayuda", "que puedes hacer", "como funciona el chat")) {
            return "Puedo ayudarte con:\n• Productos, categorías, precios y stock.\n• Promociones disponibles.\n• Carrito, cupones y checkout.\n• Métodos de pago y envíos.\n• Registro, inicio de sesión y contraseña.\n• Estado de tus pedidos cuando hayas iniciado sesión.";
        }

        return "No tengo información suficiente para responder esa consulta con seguridad. Puedo ayudarte con productos, promociones, carrito, pagos, envíos, pedidos, registro o recuperación de contraseña.";
    }

    private void anexarProductos(StringBuilder contexto, List<Producto> productos, String mensajeVacio) {
        if (productos.isEmpty()) {
            contexto.append(mensajeVacio).append("\n\n");
            return;
        }
        productos.forEach(producto -> contexto.append(formatearProducto(producto)).append("\n"));
        contexto.append("\n");
    }


    private boolean coincideConsultaConCatalogo(String texto, List<Producto> productos) {
        List<String> terminos = terminosSignificativos(texto);
        if (terminos.isEmpty() || productos == null || productos.isEmpty()) return false;
        return productos.stream().anyMatch(producto -> puntuarProducto(producto, texto, terminos) > 0);
    }

    private List<Producto> seleccionarProductos(
            List<Producto> base,
            String consulta,
            List<String> terminos,
            int limite
    ) {
        if (base == null || base.isEmpty()) return List.of();
        if (terminos.isEmpty()) return base.stream().limit(limite).toList();

        String consultaNormalizada = normalizar(consulta);
        return base.stream()
                .map(producto -> new ProductoPuntuado(producto, puntuarProducto(producto, consultaNormalizada, terminos)))
                .filter(item -> item.puntaje() > 0)
                .sorted(Comparator.comparingInt(ProductoPuntuado::puntaje).reversed()
                        .thenComparing(item -> item.producto().getId(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limite)
                .map(ProductoPuntuado::producto)
                .toList();
    }

    private int puntuarProducto(Producto producto, String consulta, List<String> terminos) {
        String nombre = normalizar(producto.getNombre());
        String categoria = normalizar(producto.getCategoria());
        String tipoProducto = normalizar(producto.getTipoProducto());
        String marca = normalizar(producto.getMarca());
        String modelo = normalizar(producto.getModelo());
        String secundarios = normalizar(String.join(" ",
                seguroVacio(producto.getDescripcion()),
                textoCaracteristicas(producto),
                seguroVacio(producto.getColor()),
                seguroVacio(producto.getMaterial()),
                seguroVacio(producto.getTalla()),
                seguroVacio(producto.getCondicion()),
                seguroVacio(producto.getDetallesAdicionales())
        ));

        int puntaje = 0;
        if (!nombre.isBlank() && consulta.contains(nombre)) puntaje += 30;
        if (!categoria.isBlank() && consulta.contains(categoria)) puntaje += 16;
        if (!tipoProducto.isBlank() && consulta.contains(tipoProducto)) puntaje += 16;
        if (!marca.isBlank() && consulta.contains(marca)) puntaje += 14;
        if (!modelo.isBlank() && consulta.contains(modelo)) puntaje += 14;

        for (String termino : terminos) {
            if (coincide(nombre, termino)) puntaje += 9;
            if (coincide(categoria, termino)) puntaje += 7;
            if (coincide(tipoProducto, termino)) puntaje += 7;
            if (coincide(marca, termino)) puntaje += 6;
            if (coincide(modelo, termino)) puntaje += 6;
            if (coincide(secundarios, termino)) puntaje += 2;
        }
        return puntaje;
    }

    private boolean coincide(String campo, String termino) {
        if (campo == null || campo.isBlank() || termino == null || termino.isBlank()) return false;
        if (campo.contains(termino)) return true;
        if (termino.endsWith("es") && termino.length() > 4 && campo.contains(termino.substring(0, termino.length() - 2))) return true;
        if (termino.endsWith("s") && termino.length() > 3 && campo.contains(termino.substring(0, termino.length() - 1))) return true;
        return campo.endsWith("s") && termino.contains(campo.substring(0, campo.length() - 1));
    }

    private List<String> terminosSignificativos(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return List.of(texto.split("\\s+"))
                .stream()
                .filter(token -> token.length() >= 2)
                .filter(token -> !TERMINOS_GENERICOS.contains(token))
                .distinct()
                .limit(8)
                .toList();
    }

    private List<String> obtenerCategorias(List<Producto> productos) {
        if (productos == null) return List.of();
        return productos.stream()
                .map(Producto::getCategoria)
                .filter(categoria -> categoria != null && !categoria.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<Producto> ordenarParaConsulta(List<Producto> productos, String texto) {
        List<Producto> resultado = new ArrayList<>(productos);
        if (contiene(texto, "mas barato", "más barato", "menor precio", "economico", "economica")) {
            resultado.sort(Comparator.comparing(Producto::getPrecio, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if (contiene(texto, "mas caro", "más caro", "mayor precio")) {
            resultado.sort(Comparator.comparing(Producto::getPrecio, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if (contiene(texto, "disponible", "stock", "comprar")) {
            resultado.sort(Comparator.comparingInt((Producto p) -> Optional.ofNullable(p.getStock()).orElse(0)).reversed());
        }
        return resultado;
    }

    private Pedido buscarPedidoMencionado(String texto, List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            String codigo = normalizar(pedido.getCodigo());
            if (!codigo.isBlank() && texto.contains(codigo)) return pedido;
        }
        return null;
    }

    private String lineaProductoCliente(Producto p, boolean incluirDetalles) {
        int stock = Optional.ofNullable(p.getStock()).orElse(0);
        StringBuilder linea = new StringBuilder("• ").append(seguro(p.getNombre()));

        if (p.getPrecio() != null) linea.append(" — ").append(dinero(p.getPrecio()));
        if (Boolean.TRUE.equals(p.getOferta()) && p.getPrecioAntes() != null && p.getPrecio() != null
                && p.getPrecioAntes().compareTo(p.getPrecio()) > 0) {
            linea.append(" (antes ").append(dinero(p.getPrecioAntes())).append(", ")
                    .append(porcentajeDescuento(p)).append("% de descuento)");
        }
        linea.append(stock > 0 ? " — stock: " + stock : " — agotado");

        if (incluirDetalles) {
            List<String> detalles = new ArrayList<>();
            agregarDetalle(detalles, "categoría", p.getCategoria());
            agregarDetalle(detalles, "tipo", p.getTipoProducto());
            caracteristicasProducto(p).entrySet().stream().limit(8)
                    .forEach(entrada -> agregarDetalle(detalles, entrada.getKey(), entrada.getValue()));
            if (!detalles.isEmpty()) linea.append("\n  ").append(String.join(" | ", detalles));
        }

        return linea.toString();
    }

    private void agregarDetalle(List<String> detalles, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) detalles.add(etiqueta + ": " + valor.trim());
    }

    private int porcentajeDescuento(Producto p) {
        if (p.getPrecioAntes() == null || p.getPrecio() == null || p.getPrecioAntes().signum() <= 0) return 0;
        return p.getPrecioAntes().subtract(p.getPrecio())
                .multiply(new BigDecimal("100"))
                .divide(p.getPrecioAntes(), 0, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .intValue();
    }

    private String lineaPedidoCliente(Pedido pedido) {
        return "• " + seguro(pedido.getCodigo())
                + " — " + pedido.getEstado()
                + " — " + dinero(pedido.getTotal())
                + (pedido.getFecha() != null ? " — " + pedido.getFecha().format(FECHA_PEDIDO) : "");
    }

    private String resumenPedidoCliente(Pedido pedido) {
        String items = Optional.ofNullable(pedido.getItems()).orElse(List.of()).stream()
                .limit(8)
                .map(item -> "• " + item.getCantidad() + " x " + seguro(item.getProductoNombre()))
                .collect(Collectors.joining("\n"));

        StringBuilder respuesta = new StringBuilder();
        respuesta.append("Pedido ").append(seguro(pedido.getCodigo())).append("\n")
                .append("Estado: ").append(pedido.getEstado()).append("\n")
                .append("Total: ").append(dinero(pedido.getTotal()));
        if (pedido.getFecha() != null) respuesta.append("\nFecha: ").append(pedido.getFecha().format(FECHA_PEDIDO));
        if (pedido.getMetodoPago() != null && !pedido.getMetodoPago().isBlank()) {
            respuesta.append("\nMétodo de pago: ").append(pedido.getMetodoPago().trim());
        }
        if (!items.isBlank()) respuesta.append("\nProductos:\n").append(items);
        return respuesta.toString();
    }

    private String formatearProducto(Producto p) {
        return "- ID " + p.getId()
                + ": " + seguro(p.getNombre())
                + " | categoría: " + seguro(p.getCategoria())
                + " | tipo: " + seguro(p.getTipoProducto())
                + " | características: " + textoCaracteristicas(p)
                + " | precio: " + dinero(p.getPrecio())
                + (p.getPrecioAntes() != null ? " | precio anterior: " + dinero(p.getPrecioAntes()) : "")
                + " | stock: " + Optional.ofNullable(p.getStock()).orElse(0)
                + " | oferta: " + siNo(p.getOferta())
                + " | destacado: " + siNo(p.getDestacado())
                + " | descripción: " + recortar(seguro(p.getDescripcion()), 160);
    }

    private Map<String, String> caracteristicasProducto(Producto producto) {
        Map<String, String> resultado = new LinkedHashMap<>();
        String raw = producto.getCaracteristicas();
        if (raw != null && !raw.isBlank()) {
            try {
                Map<String, String> guardadas = JSON.readValue(raw, new TypeReference<LinkedHashMap<String, String>>() {});
                guardadas.forEach((nombre, valor) -> agregarCaracteristica(resultado, nombre, valor));
            } catch (Exception ignored) {
                // Compatibilidad con productos creados antes de las características dinámicas.
            }
        }
        if (resultado.isEmpty()) {
            agregarCaracteristicaSiFalta(resultado, "Marca", producto.getMarca());
            agregarCaracteristicaSiFalta(resultado, "Modelo", producto.getModelo());
            agregarCaracteristicaSiFalta(resultado, "Color", producto.getColor());
            agregarCaracteristicaSiFalta(resultado, "Material", producto.getMaterial());
            agregarCaracteristicaSiFalta(resultado, "Talla o medida", producto.getTalla());
            agregarCaracteristicaSiFalta(resultado, "Garantía", producto.getGarantia());
            agregarCaracteristicaSiFalta(resultado, "Condición", producto.getCondicion());
        }
        return resultado;
    }

    private void agregarCaracteristica(Map<String, String> destino, String nombre, String valor) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String valorLimpio = valor == null ? "" : valor.trim();
        if (!nombreLimpio.isBlank() && !valorLimpio.isBlank()) destino.put(nombreLimpio, valorLimpio);
    }

    private void agregarCaracteristicaSiFalta(Map<String, String> destino, String nombre, String valor) {
        if (destino.keySet().stream().anyMatch(clave -> normalizar(clave).equals(normalizar(nombre)))) return;
        agregarCaracteristica(destino, nombre, valor);
    }

    private String textoCaracteristicas(Producto producto) {
        return caracteristicasProducto(producto).entrySet().stream()
                .map(entrada -> entrada.getKey() + ": " + entrada.getValue())
                .collect(Collectors.joining("; "));
    }

    private String formatearPedido(Pedido p) {
        String items = Optional.ofNullable(p.getItems()).orElse(List.of()).stream()
                .map(this::formatearItem)
                .collect(Collectors.joining(", "));

        return "- Pedido " + seguro(p.getCodigo())
                + " | estado: " + p.getEstado()
                + " | total: " + dinero(p.getTotal())
                + " | fecha: " + p.getFecha()
                + " | método pago: " + seguro(p.getMetodoPago())
                + " | items: " + items;
    }

    private String formatearItem(PedidoItem item) {
        return item.getCantidad() + " x " + seguro(item.getProductoNombre()) + " (" + dinero(item.getSubtotal()) + ")";
    }

    private boolean esSoloSaludo(String texto) {
        boolean saludo = contiene(texto, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "hey");
        boolean incluyeConsulta = contiene(texto,
                "producto", "productos", "precio", "cuesta", "stock", "oferta", "pedido", "envio",
                "pago", "carrito", "comprar", "contraseña", "registro", "cupon");
        return saludo && !incluyeConsulta && texto.split("\\s+").length <= 5;
    }

    private boolean esAgradecimiento(String texto) {
        return contiene(texto, "gracias", "muchas gracias", "te agradezco")
                && texto.split("\\s+").length <= 7;
    }

    private boolean esDespedida(String texto) {
        return contiene(texto, "adios", "hasta luego", "nos vemos", "chau", "chao")
                && texto.split("\\s+").length <= 6;
    }

    private String dinero(BigDecimal valor) {
        if (valor == null) return "S/ 0.00";
        return "S/ " + valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String seguro(String valor) {
        return valor == null || valor.isBlank() ? "No registrado" : valor.trim();
    }

    private String seguroVacio(String valor) {
        return valor == null ? "" : valor;
    }

    private String siNo(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "sí" : "no";
    }

    private String recortar(String valor, int max) {
        if (valor == null) return "";
        if (valor.length() <= max) return valor;
        return valor.substring(0, max - 3) + "...";
    }

    private boolean contiene(String texto, String... palabras) {
        String rodeado = " " + normalizar(texto) + " ";
        for (String palabra : palabras) {
            String buscada = normalizar(palabra);
            if (buscada.isBlank()) continue;
            if (buscada.contains(" ")) {
                if (rodeado.contains(" " + buscada + " ")) return true;
            } else if (rodeado.contains(" " + buscada + " ")) {
                return true;
            }
        }
        return false;
    }

    private String normalizar(String texto) {
        String base = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ProductoPuntuado(Producto producto, int puntaje) {}

    public record ChatContext(
            String texto,
            boolean usandoDatosReales,
            List<Producto> productos,
            List<Pedido> pedidos,
            List<String> categorias,
            BigDecimal costoEnvio,
            boolean usuarioAutenticado,
            boolean busquedaProductoEspecifica,
            boolean ofertasSolicitadas
    ) {}
}
