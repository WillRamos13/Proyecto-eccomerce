package com.fastmarket.api.controller;

import com.fastmarket.api.dto.AuthDtos;
import com.fastmarket.api.dto.UsuarioDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final AuthTokenService authTokenService;

    public UsuarioController(UsuarioService usuarioService, AuthTokenService authTokenService) {
        this.usuarioService = usuarioService;
        this.authTokenService = authTokenService;
    }

    @GetMapping
    public List<AuthDtos.UsuarioResponse> listar(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authTokenService.requerirAdmin(authorization);
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public AuthDtos.UsuarioResponse obtener(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        authTokenService.requerirUsuarioOAdmin(authorization, id);
        return usuarioService.obtener(id);
    }

    @PostMapping
    public AuthDtos.UsuarioResponse crearDesdeAdmin(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody UsuarioDtos.CrearUsuarioAdminRequest request) {
        authTokenService.requerirAdmin(authorization);
        return usuarioService.crearDesdeAdmin(request);
    }

    @PutMapping("/{id}/gestion")
    public AuthDtos.UsuarioResponse actualizarDesdeAdmin(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @RequestBody UsuarioDtos.ActualizarUsuarioAdminRequest request) {
        AuthTokenService.TokenData admin = authTokenService.requerirAdmin(authorization);
        return usuarioService.actualizarDesdeAdmin(id, admin.usuarioId(), request);
    }

    @PutMapping("/{id}")
    public AuthDtos.UsuarioResponse actualizar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @RequestBody UsuarioDtos.ActualizarUsuarioRequest request) {
        authTokenService.requerirUsuarioOAdmin(authorization, id);
        return usuarioService.actualizar(id, request);
    }

    @PostMapping("/{id}/direcciones")
    public AuthDtos.UsuarioResponse agregarDireccion(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @RequestBody UsuarioDtos.AgregarDireccionRequest request) {
        authTokenService.requerirUsuarioOAdmin(authorization, id);
        return usuarioService.agregarDireccion(id, request);
    }

    @PutMapping("/{id}/password")
    public AuthDtos.UsuarioResponse cambiarPassword(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @RequestBody UsuarioDtos.CambiarPasswordRequest request) {
        authTokenService.requerirUsuarioOAdmin(authorization, id);
        return usuarioService.cambiarPassword(id, request);
    }
}
