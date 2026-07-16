package com.fastmarket.api.controller;

import com.fastmarket.api.dto.CarritoDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.CarritoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController {
    private final CarritoService carritoService;
    private final AuthTokenService authTokenService;

    public CarritoController(CarritoService carritoService, AuthTokenService authTokenService) {
        this.carritoService = carritoService;
        this.authTokenService = authTokenService;
    }

    @GetMapping("/usuario/{usuarioId}")
    public CarritoDtos.CarritoResponse obtener(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long usuarioId) {
        authTokenService.requerirUsuarioOAdmin(authorization, usuarioId);
        return carritoService.obtener(usuarioId);
    }

    @PutMapping("/usuario/{usuarioId}")
    public CarritoDtos.CarritoResponse sincronizar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long usuarioId, @RequestBody CarritoDtos.SincronizarCarritoRequest request) {
        authTokenService.requerirUsuarioOAdmin(authorization, usuarioId);
        return carritoService.sincronizar(usuarioId, request);
    }

    @DeleteMapping("/usuario/{usuarioId}")
    public void limpiar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long usuarioId) {
        authTokenService.requerirUsuarioOAdmin(authorization, usuarioId);
        carritoService.limpiar(usuarioId);
    }
}
