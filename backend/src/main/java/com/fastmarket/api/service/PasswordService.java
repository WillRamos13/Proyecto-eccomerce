package com.fastmarket.api.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encriptar(String passwordPlano) {
        return encoder.encode(passwordPlano);
    }

    public boolean coincide(String passwordPlano, String passwordGuardado) {
        if (passwordGuardado == null) return false;
        if (esHashBcrypt(passwordGuardado)) {
            return encoder.matches(passwordPlano, passwordGuardado);
        }
        return passwordGuardado.equals(passwordPlano);
    }

    public boolean esHashBcrypt(String valor) {
        return valor != null && (valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$"));
    }
}
