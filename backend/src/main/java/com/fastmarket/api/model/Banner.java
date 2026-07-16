package com.fastmarket.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "banners")
public class Banner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    @Column(columnDefinition = "TEXT")
    private String imagen;
    @Column(nullable = false)
    private Boolean activo = true;

    public Banner() {}
    public Banner(String titulo, String descripcion, String imagen, Boolean activo) { this.titulo=titulo; this.descripcion=descripcion; this.imagen=imagen; this.activo=activo; }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTitulo(){return titulo;} public void setTitulo(String titulo){this.titulo=titulo;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String descripcion){this.descripcion=descripcion;}
    public String getImagen(){return imagen;} public void setImagen(String imagen){this.imagen=imagen;}
    public Boolean getActivo(){return activo;} public void setActivo(Boolean activo){this.activo=activo;}
}
