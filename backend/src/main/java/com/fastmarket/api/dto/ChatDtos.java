package com.fastmarket.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ChatDtos {
    public record ChatMessage(
            @NotBlank(message = "El rol del mensaje es obligatorio")
            String rol,

            @NotBlank(message = "El contenido del mensaje no puede estar vacío")
            @Size(max = 1000, message = "Cada mensaje del historial admite como máximo 1000 caracteres")
            String contenido
    ) {}

    public record ChatRequest(
            @NotBlank(message = "El mensaje no puede estar vacío")
            @Size(max = 1000, message = "El mensaje admite como máximo 1000 caracteres")
            String mensaje,

            @Valid
            @Size(max = 8, message = "El historial admite como máximo 8 mensajes")
            List<ChatMessage> historial
    ) {}

    public record ChatResponse(
            String respuesta,
            boolean usandoIa,
            boolean usandoDatosReales
    ) {}
}
