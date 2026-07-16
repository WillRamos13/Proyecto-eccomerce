package com.fastmarket.api.dto;

import com.fastmarket.api.model.EstadoReclamo;
import com.fastmarket.api.model.TipoReclamo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReclamoDtos {
    public record CrearReclamoRequest(
            Long pedidoId,
            @NotNull TipoReclamo tipo,
            @NotBlank @Size(max = 160) String asunto,
            @NotBlank @Size(max = 4000) String descripcion
    ) {}

    public record ActualizarReclamoRequest(
            @NotNull EstadoReclamo estado,
            @Size(max = 4000) String respuesta
    ) {}

    public record ReclamoResponse(
            Long id,
            String codigo,
            Long usuarioId,
            String usuarioNombre,
            String usuarioCorreo,
            Long pedidoId,
            String pedidoCodigo,
            TipoReclamo tipo,
            EstadoReclamo estado,
            String asunto,
            String descripcion,
            String respuesta,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion,
            LocalDateTime fechaResolucion
    ) {}
}
