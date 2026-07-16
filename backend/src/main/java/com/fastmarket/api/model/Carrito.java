package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carritos", uniqueConstraints = @UniqueConstraint(name = "uk_carritos_usuario", columnNames = "usuario_id"))
public class Carrito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "cupon_codigo", length = 40)
    private String cuponCodigo;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CarritoItem> items = new ArrayList<>();

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getCuponCodigo(){return cuponCodigo;} public void setCuponCodigo(String cuponCodigo){this.cuponCodigo=cuponCodigo;}
    public LocalDateTime getActualizadoEn(){return actualizadoEn;} public void setActualizadoEn(LocalDateTime actualizadoEn){this.actualizadoEn=actualizadoEn;}
    public List<CarritoItem> getItems(){return items;} public void setItems(List<CarritoItem> items){this.items=items;}
}
