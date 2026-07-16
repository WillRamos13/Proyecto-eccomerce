package com.fastmarket.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 140)
    private String nombre;
    @Column(nullable = false, unique = true, length = 160)
    private String correo;
    @JsonIgnore
    @Column(nullable = false, length = 160)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.CLIENTE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;
    @Column(length = 40)
    private String telefono;
    @Column(length = 30)
    private String documento;
    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Direccion> direcciones = new ArrayList<>();

    public Usuario() {}
    public Usuario(String nombre, String correo, String password, Rol rol) { this.nombre=nombre; this.correo=correo; this.password=password; this.rol=rol; }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getNombre(){return nombre;} public void setNombre(String nombre){this.nombre=nombre;}
    public String getCorreo(){return correo;} public void setCorreo(String correo){this.correo=correo;}
    public String getPassword(){return password;} public void setPassword(String password){this.password=password;}
    public Rol getRol(){return rol;} public void setRol(Rol rol){this.rol=rol;}
    public EstadoUsuario getEstado(){return estado;} public void setEstado(EstadoUsuario estado){this.estado=estado;}
    public String getTelefono(){return telefono;} public void setTelefono(String telefono){this.telefono=telefono;}
    public String getDocumento(){return documento;} public void setDocumento(String documento){this.documento=documento;}
    public LocalDateTime getCreadoEn(){return creadoEn;} public void setCreadoEn(LocalDateTime creadoEn){this.creadoEn=creadoEn;}
    public List<Direccion> getDirecciones(){return direcciones;} public void setDirecciones(List<Direccion> direcciones){this.direcciones=direcciones;}
}
