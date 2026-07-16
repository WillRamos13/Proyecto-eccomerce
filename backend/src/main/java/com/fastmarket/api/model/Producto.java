package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(name = "tipo_producto", length = 120)
    private String tipoProducto;

    @Column(name = "caracteristicas", columnDefinition = "TEXT")
    private String caracteristicas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_antes", precision = 12, scale = 2)
    private BigDecimal precioAntes;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(columnDefinition = "TEXT")
    private String imagen;

    @Column(name = "imagenes", columnDefinition = "TEXT")
    private String imagenes;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 100)
    private String marca;

    @Column(length = 120)
    private String modelo;

    @Column(length = 80)
    private String color;

    @Column(length = 120)
    private String material;

    @Column(length = 80)
    private String talla;

    @Column(length = 120)
    private String garantia;

    @Column(length = 80)
    private String condicion;

    @Column(name = "detalles_adicionales", columnDefinition = "TEXT")
    private String detallesAdicionales;

    @Column(nullable = false)
    private Boolean oferta = false;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Producto() {}

    public Producto(String nombre, String categoria, BigDecimal precio, BigDecimal precioAntes, Integer stock, String imagen, String descripcion, Boolean oferta, Boolean destacado) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.precioAntes = precioAntes;
        this.stock = stock;
        this.imagen = imagen;
        this.imagenes = imagen;
        this.descripcion = descripcion;
        this.oferta = oferta;
        this.destacado = destacado;
        this.activo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getTipoProducto() { return tipoProducto; }
    public void setTipoProducto(String tipoProducto) { this.tipoProducto = tipoProducto; }
    public String getCaracteristicas() { return caracteristicas; }
    public void setCaracteristicas(String caracteristicas) { this.caracteristicas = caracteristicas; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public BigDecimal getPrecioAntes() { return precioAntes; }
    public void setPrecioAntes(BigDecimal precioAntes) { this.precioAntes = precioAntes; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getImagenes() { return imagenes; }
    public void setImagenes(String imagenes) { this.imagenes = imagenes; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }
    public String getGarantia() { return garantia; }
    public void setGarantia(String garantia) { this.garantia = garantia; }
    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }
    public String getDetallesAdicionales() { return detallesAdicionales; }
    public void setDetallesAdicionales(String detallesAdicionales) { this.detallesAdicionales = detallesAdicionales; }
    public Boolean getOferta() { return oferta; }
    public void setOferta(Boolean oferta) { this.oferta = oferta; }
    public Boolean getDestacado() { return destacado; }
    public void setDestacado(Boolean destacado) { this.destacado = destacado; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
