package com.fastmarket.api.service;

import com.fastmarket.api.dto.AuthDtos;
import com.fastmarket.api.dto.PedidoDtos;
import com.fastmarket.api.dto.ProductoDtos;
import com.fastmarket.api.model.Direccion;
import com.fastmarket.api.model.Pedido;
import com.fastmarket.api.model.PedidoItem;
import com.fastmarket.api.model.Producto;
import com.fastmarket.api.model.Usuario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DtoMapper {
    private static final ObjectMapper JSON = new ObjectMapper();
    private DtoMapper() {}

    public static AuthDtos.DireccionResponse toDireccionResponse(Direccion direccion) {
        return new AuthDtos.DireccionResponse(
                direccion.getId(), direccion.getDireccion(), direccion.getReferencia(),
                direccion.getDistrito(), direccion.getCiudad(), direccion.getPrincipal()
        );
    }

    public static AuthDtos.UsuarioResponse toUsuarioResponse(Usuario usuario) {
        List<AuthDtos.DireccionResponse> direcciones = usuario.getDirecciones()
                .stream()
                .map(DtoMapper::toDireccionResponse)
                .toList();

        return new AuthDtos.UsuarioResponse(
                usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getTelefono(),
                usuario.getDocumento(), usuario.getRol(), usuario.getEstado(), direcciones
        );
    }

    public static PedidoDtos.ItemResponse toItemResponse(PedidoItem item) {
        Producto producto = item.getProducto();
        return new PedidoDtos.ItemResponse(
                producto != null ? producto.getId() : null,
                item.getProductoNombre(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal(),
                producto != null ? producto.getImagen() : null
        );
    }


    public static ProductoDtos.ProductoResponse toProductoResponse(Producto producto) {
        List<String> imagenes = imagenesProducto(producto);
        String imagenPrincipal = !imagenes.isEmpty() ? imagenes.get(0) : producto.getImagen();
        Map<String, String> caracteristicas = caracteristicasProducto(producto);

        return new ProductoDtos.ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getCategoria(),
                producto.getTipoProducto(),
                caracteristicas,
                producto.getPrecio(),
                producto.getPrecioAntes(),
                producto.getStock(),
                imagenPrincipal,
                imagenes,
                producto.getDescripcion(),
                producto.getMarca(),
                producto.getModelo(),
                producto.getColor(),
                producto.getMaterial(),
                producto.getTalla(),
                producto.getGarantia(),
                producto.getCondicion(),
                producto.getDetallesAdicionales(),
                Boolean.TRUE.equals(producto.getOferta()),
                Boolean.TRUE.equals(producto.getDestacado()),
                Boolean.TRUE.equals(producto.getActivo()),
                producto.getVendedor() != null ? producto.getVendedor().getId() : null,
                producto.getVendedor() != null ? producto.getVendedor().getNombre() : null,
                producto.getCreadoEn()
        );
    }

    private static Map<String, String> caracteristicasProducto(Producto producto) {
        Map<String, String> resultado = new LinkedHashMap<>();
        String raw = producto.getCaracteristicas();
        if (raw != null && !raw.isBlank()) {
            try {
                Map<String, String> guardadas = JSON.readValue(raw, new TypeReference<LinkedHashMap<String, String>>() {});
                guardadas.forEach((nombre, valor) -> agregarCaracteristica(resultado, nombre, valor));
            } catch (Exception ignored) {
                // Los productos antiguos no tenían este campo. Se completan debajo con sus datos heredados.
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

    private static void agregarCaracteristica(Map<String, String> resultado, String nombre, String valor) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String valorLimpio = valor == null ? "" : valor.trim();
        if (!nombreLimpio.isBlank() && !valorLimpio.isBlank()) resultado.put(nombreLimpio, valorLimpio);
    }

    private static void agregarCaracteristicaSiFalta(Map<String, String> resultado, String nombre, String valor) {
        if (resultado.keySet().stream().anyMatch(clave -> clave.equalsIgnoreCase(nombre))) return;
        agregarCaracteristica(resultado, nombre, valor);
    }

    private static List<String> imagenesProducto(Producto producto) {
        List<String> resultado = new ArrayList<>();

        if (producto.getImagenes() != null && !producto.getImagenes().isBlank()) {
            String raw = producto.getImagenes().trim();
            if (raw.startsWith("[")) {
                try {
                    List<String> desdeJson = JSON.readValue(raw, new TypeReference<List<String>>() {});
                    for (String item : desdeJson) {
                        String limpio = item == null ? "" : item.trim();
                        if (!limpio.isBlank() && !resultado.contains(limpio)) resultado.add(limpio);
                    }
                } catch (Exception ignored) {
                    agregarImagenesSeparadas(resultado, raw);
                }
            } else {
                agregarImagenesSeparadas(resultado, raw);
            }
        }

        String imagen = producto.getImagen();
        if (resultado.isEmpty() && imagen != null && !imagen.isBlank()) resultado.add(imagen.trim());
        if (resultado.isEmpty()) resultado.add("img/logo.png");
        return resultado;
    }

    private static void agregarImagenesSeparadas(List<String> resultado, String raw) {
        for (String item : raw.split("\n")) {
            String limpio = item == null ? "" : item.trim();
            if (!limpio.isBlank() && !resultado.contains(limpio)) resultado.add(limpio);
        }
    }

    public static PedidoDtos.PedidoResponse toPedidoResponse(Pedido pedido) {
        return toPedidoResponse(pedido, null);
    }

    public static PedidoDtos.PedidoResponse toPedidoResponse(Pedido pedido, Long vendedorId) {
        List<PedidoItem> itemsFiltrados = pedido.getItems().stream()
                .filter(item -> vendedorId == null || (item.getVendedor() != null && vendedorId.equals(item.getVendedor().getId())))
                .toList();

        List<PedidoDtos.ItemResponse> items = itemsFiltrados.stream()
                .map(DtoMapper::toItemResponse)
                .toList();

        BigDecimal subtotalVisible = vendedorId == null
                ? pedido.getSubtotal()
                : itemsFiltrados.stream()
                    .map(PedidoItem::getSubtotal)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoEnvioVisible = vendedorId == null ? pedido.getCostoEnvio() : BigDecimal.ZERO;
        BigDecimal descuentoVisible = vendedorId == null ? pedido.getDescuento() : BigDecimal.ZERO;
        BigDecimal totalVisible = vendedorId == null
                ? pedido.getTotal()
                : subtotalVisible.subtract(descuentoVisible).add(costoEnvioVisible);
        if (totalVisible.compareTo(BigDecimal.ZERO) < 0) totalVisible = BigDecimal.ZERO;

        return new PedidoDtos.PedidoResponse(
                pedido.getId(),
                pedido.getCodigo(),
                pedido.getUsuario().getId(),
                pedido.getUsuario().getNombre(),
                subtotalVisible,
                costoEnvioVisible,
                totalVisible,
                descuentoVisible,
                vendedorId == null ? pedido.getCuponCodigo() : null,
                pedido.getEstado(),
                pedido.getEstadoPago(),
                pedido.getPagoId(),
                pedido.getPreferenciaPagoId(),
                pedido.getReferenciaPago(),
                pedido.getMontoPagado(),
                pedido.getFechaPago(),
                pedido.getFechaExpiracionPago(),
                pedido.getDireccionEntrega(),
                pedido.getReferenciaEntrega(),
                pedido.getHorarioEntrega(),
                pedido.getMetodoPago(),
                pedido.getTelefonoEntrega(),
                pedido.getFecha(),
                items
        );
    }
}
