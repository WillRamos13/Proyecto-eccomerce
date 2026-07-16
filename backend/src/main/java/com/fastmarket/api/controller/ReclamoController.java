package com.fastmarket.api.controller;

import com.fastmarket.api.dto.ReclamoDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.ReclamoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reclamos")
public class ReclamoController {
    private final ReclamoService reclamoService;
    private final AuthTokenService authTokenService;

    public ReclamoController(ReclamoService reclamoService, AuthTokenService authTokenService) {
        this.reclamoService = reclamoService;
        this.authTokenService = authTokenService;
    }

    @PostMapping
    public ReclamoDtos.ReclamoResponse crear(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ReclamoDtos.CrearReclamoRequest request
    ) {
        AuthTokenService.TokenData actor = authTokenService.validar(authorization);
        return reclamoService.crear(actor.usuarioId(), request);
    }

    @GetMapping("/mios")
    public List<ReclamoDtos.ReclamoResponse> mios(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        AuthTokenService.TokenData actor = authTokenService.validar(authorization);
        return reclamoService.listarMios(actor.usuarioId());
    }

    @GetMapping
    public List<ReclamoDtos.ReclamoResponse> todos(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authTokenService.requerirAdmin(authorization);
        return reclamoService.listarTodos();
    }

    @PutMapping("/{id}")
    public ReclamoDtos.ReclamoResponse actualizar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody ReclamoDtos.ActualizarReclamoRequest request
    ) {
        authTokenService.requerirAdmin(authorization);
        return reclamoService.actualizar(id, request);
    }
}
