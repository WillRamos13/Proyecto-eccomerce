package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "numeric(12,2) default 0")
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "cupon_codigo", length = 40)
    private String cuponCodigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 30, columnDefinition = "varchar(30) default 'PENDIENTE'")
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Column(name = "pago_id", length = 100, unique = true)
    private String pagoId;

    @Column(name = "preferencia_pago_id", length = 100)
    private String preferenciaPagoId;

    @Column(name = "referencia_pago", length = 160)
    private String referenciaPago;

    @Column(name = "monto_pagado", precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_expiracion_pago")
    private LocalDateTime fechaExpiracionPago;

    @Column(name = "stock_descontado", nullable = false, columnDefinition = "boolean default false")
    private Boolean stockDescontado = false;

    @Column(name = "cupon_uso_activo", nullable = false, columnDefinition = "boolean default false")
    private Boolean cuponUsoActivo = false;

    @Column(columnDefinition = "TEXT")
    private String direccionEntrega;

    @Column(columnDefinition = "TEXT")
    private String referenciaEntrega;

    @Column(length = 80)
    private String horarioEntrega;

    @Column(length = 80)
    private String metodoPago;

    @Column(length = 40)
    private String telefonoEntrega;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PedidoItem> items = new ArrayList<>();

    public Pedido() {}
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getCodigo(){return codigo;} public void setCodigo(String codigo){this.codigo=codigo;}
    public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal subtotal){this.subtotal=subtotal;}
    public BigDecimal getCostoEnvio(){return costoEnvio;} public void setCostoEnvio(BigDecimal costoEnvio){this.costoEnvio=costoEnvio;}
    public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal total){this.total=total;}
    public BigDecimal getDescuento(){return descuento;} public void setDescuento(BigDecimal descuento){this.descuento=descuento;}
    public String getCuponCodigo(){return cuponCodigo;} public void setCuponCodigo(String cuponCodigo){this.cuponCodigo=cuponCodigo;}
    public EstadoPedido getEstado(){return estado;} public void setEstado(EstadoPedido estado){this.estado=estado;}
    public EstadoPago getEstadoPago(){return estadoPago;} public void setEstadoPago(EstadoPago estadoPago){this.estadoPago=estadoPago;}
    public String getPagoId(){return pagoId;} public void setPagoId(String pagoId){this.pagoId=pagoId;}
    public String getPreferenciaPagoId(){return preferenciaPagoId;} public void setPreferenciaPagoId(String preferenciaPagoId){this.preferenciaPagoId=preferenciaPagoId;}
    public String getReferenciaPago(){return referenciaPago;} public void setReferenciaPago(String referenciaPago){this.referenciaPago=referenciaPago;}
    public BigDecimal getMontoPagado(){return montoPagado;} public void setMontoPagado(BigDecimal montoPagado){this.montoPagado=montoPagado;}
    public LocalDateTime getFechaPago(){return fechaPago;} public void setFechaPago(LocalDateTime fechaPago){this.fechaPago=fechaPago;}
    public LocalDateTime getFechaExpiracionPago(){return fechaExpiracionPago;} public void setFechaExpiracionPago(LocalDateTime fechaExpiracionPago){this.fechaExpiracionPago=fechaExpiracionPago;}
    public Boolean getStockDescontado(){return stockDescontado;} public void setStockDescontado(Boolean stockDescontado){this.stockDescontado=stockDescontado;}
    public Boolean getCuponUsoActivo(){return cuponUsoActivo;} public void setCuponUsoActivo(Boolean cuponUsoActivo){this.cuponUsoActivo=cuponUsoActivo;}
    public String getDireccionEntrega(){return direccionEntrega;} public void setDireccionEntrega(String direccionEntrega){this.direccionEntrega=direccionEntrega;}
    public String getReferenciaEntrega(){return referenciaEntrega;} public void setReferenciaEntrega(String referenciaEntrega){this.referenciaEntrega=referenciaEntrega;}
    public String getHorarioEntrega(){return horarioEntrega;} public void setHorarioEntrega(String horarioEntrega){this.horarioEntrega=horarioEntrega;}
    public String getMetodoPago(){return metodoPago;} public void setMetodoPago(String metodoPago){this.metodoPago=metodoPago;}
    public String getTelefonoEntrega(){return telefonoEntrega;} public void setTelefonoEntrega(String telefonoEntrega){this.telefonoEntrega=telefonoEntrega;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime fecha){this.fecha=fecha;}
    public List<PedidoItem> getItems(){return items;} public void setItems(List<PedidoItem> items){this.items=items;}
}
