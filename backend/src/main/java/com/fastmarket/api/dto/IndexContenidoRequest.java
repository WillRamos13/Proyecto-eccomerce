package com.fastmarket.api.dto;

import jakarta.validation.constraints.NotBlank;

public record IndexContenidoRequest(
        @NotBlank String tipo,
        @NotBlank String clave,
        @NotBlank String titulo,
        String descripcion,
        String imagen,
        String enlace,
        Boolean activo,
        Integer orden
) {}
