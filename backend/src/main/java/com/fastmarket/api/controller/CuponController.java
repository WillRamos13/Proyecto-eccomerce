package com.fastmarket.api.controller;

import com.fastmarket.api.dto.CuponDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.CuponService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cupones")
public class CuponController {
    private final CuponService cuponService;
    private final AuthTokenService authTokenService;

    public CuponController(CuponService cuponService, AuthTokenService authTokenService) {
        this.cuponService = cuponService;
        this.authTokenService = authTokenService;
    }

    @GetMapping
    public List<CuponDtos.CuponResponse> listar(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return cuponService.listar(actor);
    }

    @PostMapping
    public CuponDtos.CuponResponse crear(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody CuponDtos.CuponRequest request) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return cuponService.crear(actor, request);
    }

    @PutMapping("/{id}")
    public CuponDtos.CuponResponse actualizar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @RequestBody CuponDtos.CuponRequest request) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return cuponService.actualizar(actor, id, request);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        cuponService.eliminar(actor, id);
    }

    @GetMapping("/{id}/usos")
    public List<CuponDtos.CuponUsoResponse> usos(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return cuponService.listarUsos(actor, id);
    }

    @PostMapping("/aplicar")
    public CuponDtos.AplicarCuponResponse aplicar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CuponDtos.AplicarCuponRequest request
    ) {
        Long usuarioId = null;
        if (authorization != null && !authorization.isBlank()) {
            try {
                usuarioId = authTokenService.validar(authorization).usuarioId();
            } catch (SecurityException ignored) {
                usuarioId = null;
            }
        }
        return cuponService.aplicar(request, usuarioId);
    }
}
