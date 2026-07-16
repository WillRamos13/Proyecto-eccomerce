package com.fastmarket.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BannerRequest(
        @NotBlank String titulo,
        String descripcion,
        String imagen,
        Boolean activo
) {}
