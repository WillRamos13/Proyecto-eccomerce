package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido_historial")
public class PedidoHistorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoPedido estadoNuevo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Column(length = 180)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Pedido getPedido(){return pedido;} public void setPedido(Pedido pedido){this.pedido=pedido;}
    public EstadoPedido getEstadoAnterior(){return estadoAnterior;} public void setEstadoAnterior(EstadoPedido estadoAnterior){this.estadoAnterior=estadoAnterior;}
    public EstadoPedido getEstadoNuevo(){return estadoNuevo;} public void setEstadoNuevo(EstadoPedido estadoNuevo){this.estadoNuevo=estadoNuevo;}
    public Usuario getActor(){return actor;} public void setActor(Usuario actor){this.actor=actor;}
    public String getMotivo(){return motivo;} public void setMotivo(String motivo){this.motivo=motivo;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime fecha){this.fecha=fecha;}
}
