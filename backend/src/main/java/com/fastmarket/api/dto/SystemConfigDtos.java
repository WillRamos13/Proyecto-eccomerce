package com.fastmarket.api.dto;

import jakarta.validation.constraints.NotBlank;

public class SystemConfigDtos {
    public record ConfigResponse(String clave, String valor) {}
    public record ConfigRequest(@NotBlank String valor) {}
}
