package com.fastmarket.api.pattern.creational.builder;

import com.fastmarket.api.model.EstadoPedido;
import com.fastmarket.api.model.Pedido;
import com.fastmarket.api.model.PedidoItem;
import com.fastmarket.api.model.Producto;
import com.fastmarket.api.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Patrón Builder: construye un pedido válido paso a paso sin exponer una
 * secuencia extensa de setters al servicio de aplicación.
 */
public final class PedidoBuilder {
    private final Pedido pedido;

    private PedidoBuilder(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario del pedido es obligatorio");
        }
        this.pedido = new Pedido();
        this.pedido.setUsuario(usuario);
        this.pedido.setEstado(EstadoPedido.PENDIENTE);
        this.pedido.setFecha(LocalDateTime.now());
    }

    public static PedidoBuilder nuevo(Usuario usuario) {
        return new PedidoBuilder(usuario);
    }

    public PedidoBuilder conCodigo(String codigo) {
        pedido.setCodigo(requerido(codigo, "El código del pedido es obligatorio"));
        return this;
    }

    public PedidoBuilder conEntrega(
            String direccion,
            String referencia,
            String horario,
            String telefono
    ) {
        pedido.setDireccionEntrega(requerido(direccion, "La dirección de entrega es obligatoria"));
        pedido.setReferenciaEntrega(valor(referencia, ""));
        pedido.setHorarioEntrega(requerido(horario, "El horario de entrega es obligatorio"));
        String telefonoFinal = telefono == null || telefono.isBlank() ? pedido.getUsuario().getTelefono() : telefono;
        pedido.setTelefonoEntrega(requerido(telefonoFinal, "El teléfono de entrega es obligatorio"));
        return this;
    }

    public PedidoBuilder conMetodoPago(String metodoPago) {
        pedido.setMetodoPago(valor(metodoPago, "Pago contra entrega"));
        return this;
    }

    public PedidoBuilder conCupon(String codigoCupon) {
        pedido.setCuponCodigo(codigoCupon == null || codigoCupon.isBlank()
                ? null
                : codigoCupon.trim().toUpperCase());
        return this;
    }

    public PedidoBuilder agregarProducto(Producto producto, int cantidad) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        BigDecimal subtotalItem = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setProductoNombre(producto.getNombre());
        item.setCantidad(cantidad);
        item.setPrecioUnitario(producto.getPrecio());
        item.setSubtotal(subtotalItem);
        item.setVendedor(producto.getVendedor());
        pedido.getItems().add(item);
        return this;
    }

    public BigDecimal subtotalActual() {
        return pedido.getItems().stream()
                .map(PedidoItem::getSubtotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PedidoBuilder conTotales(BigDecimal subtotal, BigDecimal costoEnvio, BigDecimal descuento) {
        BigDecimal subtotalSeguro = noNegativo(subtotal);
        BigDecimal envioSeguro = noNegativo(costoEnvio);
        BigDecimal descuentoSeguro = noNegativo(descuento);
        BigDecimal total = subtotalSeguro.subtract(descuentoSeguro).add(envioSeguro);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        pedido.setSubtotal(subtotalSeguro);
        pedido.setCostoEnvio(envioSeguro);
        pedido.setDescuento(descuentoSeguro);
        pedido.setTotal(total);
        return this;
    }

    public Pedido build() {
        requerido(pedido.getCodigo(), "El código del pedido es obligatorio");
        if (pedido.getItems().isEmpty()) {
            throw new IllegalStateException("El pedido debe contener al menos un producto");
        }
        if (pedido.getTotal() == null) {
            throw new IllegalStateException("Los totales del pedido no fueron calculados");
        }
        return pedido;
    }

    private static BigDecimal noNegativo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return valor;
    }

    private static String requerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    private static String valor(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }
}
