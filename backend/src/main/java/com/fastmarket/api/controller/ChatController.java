package com.fastmarket.api.controller;

import com.fastmarket.api.dto.ChatDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.OpenAiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final OpenAiChatService chatService;
    private final AuthTokenService authTokenService;

    public ChatController(OpenAiChatService chatService, AuthTokenService authTokenService) {
        this.chatService = chatService;
        this.authTokenService = authTokenService;
    }

    @PostMapping
    public ChatDtos.ChatResponse responder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ChatDtos.ChatRequest request
    ) {
        Long usuarioId = obtenerUsuarioIdSiExiste(authorization);
        return chatService.responder(request.mensaje(), request.historial(), usuarioId);
    }

    private Long obtenerUsuarioIdSiExiste(String authorization) {
        try {
            if (authorization == null || authorization.isBlank()) return null;
            return authTokenService.validar(authorization).usuarioId();
        } catch (Exception e) {
            return null;
        }
    }
}
