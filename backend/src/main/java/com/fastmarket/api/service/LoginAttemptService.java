package com.fastmarket.api.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_INTENTOS = 5;
    private static final long BLOQUEO_MINUTOS = 10;

    private final Map<String, IntentoLogin> intentos = new ConcurrentHashMap<>();

    public void verificarPermitido(String correo) {
        IntentoLogin intento = intentos.get(normalizar(correo));
        if (intento == null) return;
        if (intento.bloqueadoHasta > Instant.now().toEpochMilli()) {
            throw new IllegalArgumentException("Demasiados intentos fallidos. Intenta nuevamente en unos minutos.");
        }
    }

    public void registrarExito(String correo) {
        intentos.remove(normalizar(correo));
    }

    public void registrarFallo(String correo) {
        String key = normalizar(correo);
        IntentoLogin intento = intentos.getOrDefault(key, new IntentoLogin());
        intento.fallos++;
        if (intento.fallos >= MAX_INTENTOS) {
            intento.bloqueadoHasta = Instant.now().plusSeconds(BLOQUEO_MINUTOS * 60).toEpochMilli();
            intento.fallos = 0;
        }
        intentos.put(key, intento);
    }

    private String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    private static class IntentoLogin {
        int fallos = 0;
        long bloqueadoHasta = 0;
    }
}
