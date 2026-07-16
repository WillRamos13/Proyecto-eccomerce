package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupones", uniqueConstraints = @UniqueConstraint(name = "uk_cupones_codigo", columnNames = "codigo"))
public class Cupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(length = 180)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCupon tipo = TipoCupon.GLOBAL;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Column(name = "categoria_objetivo", length = 80)
    private String categoriaObjetivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_objetivo_id")
    private Producto productoObjetivo;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje = BigDecimal.ZERO;

    @Column(name = "monto_fijo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoFijo = BigDecimal.ZERO;

    @Column(name = "monto_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoMinimo = BigDecimal.ZERO;

    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    @Column(name = "usos_actuales", nullable = false)
    private Integer usosActuales = 0;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getCodigo(){return codigo;} public void setCodigo(String codigo){this.codigo=codigo;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public TipoCupon getTipo(){return tipo;} public void setTipo(TipoCupon tipo){this.tipo=tipo;}
    public Usuario getVendedor(){return vendedor;} public void setVendedor(Usuario vendedor){this.vendedor=vendedor;}
    public String getCategoriaObjetivo(){return categoriaObjetivo;} public void setCategoriaObjetivo(String categoriaObjetivo){this.categoriaObjetivo=categoriaObjetivo;}
    public Producto getProductoObjetivo(){return productoObjetivo;} public void setProductoObjetivo(Producto productoObjetivo){this.productoObjetivo=productoObjetivo;}
    public BigDecimal getPorcentaje(){return porcentaje;} public void setPorcentaje(BigDecimal porcentaje){this.porcentaje=porcentaje;}
    public BigDecimal getMontoFijo(){return montoFijo;} public void setMontoFijo(BigDecimal montoFijo){this.montoFijo=montoFijo;}
    public BigDecimal getMontoMinimo(){return montoMinimo;} public void setMontoMinimo(BigDecimal montoMinimo){this.montoMinimo=montoMinimo;}
    public Integer getUsosMaximos(){return usosMaximos;} public void setUsosMaximos(Integer usosMaximos){this.usosMaximos=usosMaximos;}
    public Integer getUsosActuales(){return usosActuales;} public void setUsosActuales(Integer usosActuales){this.usosActuales=usosActuales;}
    public LocalDateTime getFechaInicio(){return fechaInicio;} public void setFechaInicio(LocalDateTime fechaInicio){this.fechaInicio=fechaInicio;}
    public LocalDateTime getFechaFin(){return fechaFin;} public void setFechaFin(LocalDateTime fechaFin){this.fechaFin=fechaFin;}
    public Boolean getActivo(){return activo;} public void setActivo(Boolean activo){this.activo=activo;}
    public LocalDateTime getCreadoEn(){return creadoEn;} public void setCreadoEn(LocalDateTime creadoEn){this.creadoEn=creadoEn;}
}
