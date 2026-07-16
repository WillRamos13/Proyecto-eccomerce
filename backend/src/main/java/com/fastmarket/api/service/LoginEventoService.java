package com.fastmarket.api.service;

import com.fastmarket.api.model.LoginEvento;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.repository.LoginEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginEventoService {
    private final LoginEventoRepository repository;

    public LoginEventoService(LoginEventoRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(Usuario usuario) {
        LoginEvento evento = new LoginEvento();
        evento.setUsuario(usuario);
        evento.setCorreo(normalizar(usuario != null ? usuario.getCorreo() : null));
        evento.setExitoso(true);
        evento.setRol(usuario != null ? usuario.getRol() : null);
        evento.setDetalle("Inicio de sesión correcto");
        repository.save(evento);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String correo, Usuario usuario, String detalle) {
        LoginEvento evento = new LoginEvento();
        evento.setUsuario(usuario);
        evento.setCorreo(normalizar(correo));
        evento.setExitoso(false);
        evento.setRol(usuario != null ? usuario.getRol() : null);
        evento.setDetalle(limitar(detalle, 180));
        repository.save(evento);
    }

    private String normalizar(String correo) {
        String valor = correo == null ? "desconocido" : correo.trim().toLowerCase();
        return valor.isBlank() ? "desconocido" : limitar(valor, 160);
    }

    private String limitar(String valor, int max) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.length() <= max ? limpio : limpio.substring(0, max);
    }
}
