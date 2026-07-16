package com.fastmarket.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuraciones_sistema")
public class SystemConfig {
    @Id
    @Column(length = 80)
    private String clave;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    private Long actualizadoPor;

    public SystemConfig() {}

    public SystemConfig(String clave, String valor, Long actualizadoPor) {
        this.clave = clave;
        this.valor = valor;
        this.actualizadoPor = actualizadoPor;
        this.actualizadoEn = LocalDateTime.now();
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
    public Long getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(Long actualizadoPor) { this.actualizadoPor = actualizadoPor; }
}
