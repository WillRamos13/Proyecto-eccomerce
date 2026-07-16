package com.fastmarket.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "index_contenidos")
public class IndexContenido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String tipo;
    @Column(nullable = false, length = 100)
    private String clave;
    @Column(nullable = false, length = 180)
    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    @Column(columnDefinition = "TEXT")
    private String imagen;
    @Column(length = 220)
    private String enlace;
    @Column(nullable = false)
    private Boolean activo = true;
    @Column(name = "orden_item", nullable = false)
    private Integer orden = 1;

    public IndexContenido() {}
    public IndexContenido(String tipo, String clave, String titulo, String descripcion, String imagen, String enlace, Boolean activo, Integer orden) {
        this.tipo=tipo; this.clave=clave; this.titulo=titulo; this.descripcion=descripcion; this.imagen=imagen; this.enlace=enlace; this.activo=activo; this.orden=orden;
    }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTipo(){return tipo;} public void setTipo(String tipo){this.tipo=tipo;}
    public String getClave(){return clave;} public void setClave(String clave){this.clave=clave;}
    public String getTitulo(){return titulo;} public void setTitulo(String titulo){this.titulo=titulo;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public String getImagen(){return imagen;} public void setImagen(String imagen){this.imagen=imagen;}
    public String getEnlace(){return enlace;} public void setEnlace(String enlace){this.enlace=enlace;}
    public Boolean getActivo(){return activo;} public void setActivo(Boolean activo){this.activo=activo;}
    public Integer getOrden(){return orden;} public void setOrden(Integer orden){this.orden=orden;}
}
