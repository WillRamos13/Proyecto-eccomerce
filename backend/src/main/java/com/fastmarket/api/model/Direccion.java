package com.fastmarket.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "direcciones")
public class Direccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String direccion;
    @Column(columnDefinition = "TEXT")
    private String referencia;
    @Column(length = 100)
    private String distrito;
    @Column(length = 100)
    private String ciudad;
    @Column(nullable = false)
    private Boolean principal = false;

    public Direccion() {}
    public Direccion(Usuario usuario, String direccion, String referencia, String distrito, String ciudad, Boolean principal) { this.usuario=usuario; this.direccion=direccion; this.referencia=referencia; this.distrito=distrito; this.ciudad=ciudad; this.principal=principal; }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public String getDireccion(){return direccion;} public void setDireccion(String direccion){this.direccion=direccion;}
    public String getReferencia(){return referencia;} public void setReferencia(String referencia){this.referencia=referencia;}
    public String getDistrito(){return distrito;} public void setDistrito(String distrito){this.distrito=distrito;}
    public String getCiudad(){return ciudad;} public void setCiudad(String ciudad){this.ciudad=ciudad;}
    public Boolean getPrincipal(){return principal;} public void setPrincipal(Boolean principal){this.principal=principal;}
}
