package com.fastmarket.api.controller;

import com.fastmarket.api.dto.AuthDtos;
import com.fastmarket.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro/enviar-codigo")
    public Map<String, String> enviarCodigoRegistro(@Valid @RequestBody AuthDtos.EnviarCodigoRegistroRequest request) {
        authService.enviarCodigoRegistro(request);
        return Map.of("mensaje", "Te enviamos un código de verificación a tu correo.");
    }

    @PostMapping("/registro")
    public AuthDtos.AuthResponse registrar(@Valid @RequestBody AuthDtos.RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/recuperar/enviar-codigo")
    public Map<String, String> enviarCodigoRecuperacion(@Valid @RequestBody AuthDtos.SolicitarRecuperacionRequest request) {
        authService.solicitarRecuperacionPassword(request);
        return Map.of("mensaje", "Si el correo existe, te enviaremos un código para recuperar tu contraseña.");
    }

    @PostMapping("/recuperar")
    public Map<String, String> recuperar(@Valid @RequestBody AuthDtos.RecuperarPasswordRequest request) {
        authService.recuperarPassword(request);
        return Map.of("mensaje", "Contraseña actualizada correctamente");
    }
}
