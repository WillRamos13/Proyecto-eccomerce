package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "carrito_items", uniqueConstraints = @UniqueConstraint(name = "uk_carrito_producto", columnNames = {"carrito_id", "producto_id"}))
public class CarritoItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Carrito getCarrito(){return carrito;} public void setCarrito(Carrito carrito){this.carrito=carrito;}
    public Producto getProducto(){return producto;} public void setProducto(Producto producto){this.producto=producto;}
    public Integer getCantidad(){return cantidad;} public void setCantidad(Integer cantidad){this.cantidad=cantidad;}
    public BigDecimal getPrecioUnitario(){return precioUnitario;} public void setPrecioUnitario(BigDecimal precioUnitario){this.precioUnitario=precioUnitario;}
}
