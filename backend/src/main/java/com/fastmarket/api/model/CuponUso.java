package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cupon_usos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cupon_uso_usuario", columnNames = {"cupon_id", "usuario_id"}),
                @UniqueConstraint(name = "uk_cupon_uso_pedido", columnNames = {"pedido_id"})
        }
)
public class CuponUso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cupon_id", nullable = false)
    private Cupon cupon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "descuento_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Cupon getCupon(){return cupon;} public void setCupon(Cupon cupon){this.cupon=cupon;}
    public Usuario getUsuario(){return usuario;} public void setUsuario(Usuario usuario){this.usuario=usuario;}
    public Pedido getPedido(){return pedido;} public void setPedido(Pedido pedido){this.pedido=pedido;}
    public BigDecimal getDescuentoAplicado(){return descuentoAplicado;} public void setDescuentoAplicado(BigDecimal descuentoAplicado){this.descuentoAplicado=descuentoAplicado;}
    public LocalDateTime getFecha(){return fecha;} public void setFecha(LocalDateTime fecha){this.fecha=fecha;}
}
