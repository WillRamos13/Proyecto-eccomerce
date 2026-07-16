package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido_items")
public class PedidoItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    @Column(nullable = false, length = 160)
    private String productoNombre;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    public PedidoItem() {}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Pedido getPedido(){return pedido;} public void setPedido(Pedido pedido){this.pedido=pedido;}
    public Producto getProducto(){return producto;} public void setProducto(Producto producto){this.producto=producto;}
    public String getProductoNombre(){return productoNombre;} public void setProductoNombre(String productoNombre){this.productoNombre=productoNombre;}
    public Integer getCantidad(){return cantidad;} public void setCantidad(Integer cantidad){this.cantidad=cantidad;}
    public BigDecimal getPrecioUnitario(){return precioUnitario;} public void setPrecioUnitario(BigDecimal precioUnitario){this.precioUnitario=precioUnitario;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal subtotal){this.subtotal=subtotal;}
    public Usuario getVendedor(){return vendedor;} public void setVendedor(Usuario vendedor){this.vendedor=vendedor;}
}
