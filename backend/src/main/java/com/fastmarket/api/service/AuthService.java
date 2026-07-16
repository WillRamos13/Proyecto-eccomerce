package com.fastmarket.api.service;

import com.fastmarket.api.dto.AuthDtos;
import com.fastmarket.api.model.Direccion;
import com.fastmarket.api.model.EstadoUsuario;
import com.fastmarket.api.model.Rol;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;
    private final CodigoVerificacionService codigoVerificacionService;
    private final LoginAttemptService loginAttemptService;
    private final LoginEventoService loginEventoService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordService passwordService,
            AuthTokenService authTokenService,
            CodigoVerificacionService codigoVerificacionService,
            LoginAttemptService loginAttemptService,
            LoginEventoService loginEventoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
        this.codigoVerificacionService = codigoVerificacionService;
        this.loginAttemptService = loginAttemptService;
        this.loginEventoService = loginEventoService;
    }

    @Transactional
    public void enviarCodigoRegistro(AuthDtos.EnviarCodigoRegistroRequest request) {
        String correo = request.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        codigoVerificacionService.enviarCodigoRegistro(correo, request.nombre());
    }

    @Transactional
    public AuthDtos.AuthResponse registrar(AuthDtos.RegistroRequest request) {
        String correo = request.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        codigoVerificacionService.validarCodigoRegistro(correo, request.codigoVerificacion());
        validarPasswordSegura(request.password());

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordService.encriptar(request.password()));
        usuario.setTelefono(limpiar(request.telefono()));
        usuario.setDocumento(limpiar(request.documento()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setEstado(EstadoUsuario.ACTIVO);

        if (request.direccion() != null && !request.direccion().isBlank()) {
            Direccion direccion = new Direccion(usuario, request.direccion().trim(), "", "Ica", "Ica", true);
            usuario.getDirecciones().add(direccion);
        }

        Usuario guardado = usuarioRepository.save(usuario);
        return new AuthDtos.AuthResponse(DtoMapper.toUsuarioResponse(guardado), authTokenService.generarToken(guardado));
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String correo = request.correo().trim().toLowerCase();
        try {
            loginAttemptService.verificarPermitido(correo);
        } catch (IllegalArgumentException ex) {
            loginEventoService.registrarFallo(correo, null, "Acceso bloqueado temporalmente por intentos fallidos");
            throw ex;
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> {
                    loginAttemptService.registrarFallo(correo);
                    loginEventoService.registrarFallo(correo, null, "Correo no registrado o credenciales incorrectas");
                    return new IllegalArgumentException("Correo o contraseña incorrectos");
                });

        if (!passwordService.coincide(request.password(), usuario.getPassword())) {
            loginAttemptService.registrarFallo(correo);
            loginEventoService.registrarFallo(correo, usuario, "Contraseña incorrecta");
            throw new IllegalArgumentException("Correo o contraseña incorrectos");
        }

        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            loginEventoService.registrarFallo(correo, usuario, "Usuario inactivo");
            throw new IllegalArgumentException("Usuario inactivo");
        }

        if (!passwordService.esHashBcrypt(usuario.getPassword())) {
            usuario.setPassword(passwordService.encriptar(request.password()));
            usuarioRepository.save(usuario);
        }

        loginAttemptService.registrarExito(correo);
        loginEventoService.registrarExito(usuario);
        return new AuthDtos.AuthResponse(DtoMapper.toUsuarioResponse(usuario), authTokenService.generarToken(usuario));
    }

    @Transactional
    public void solicitarRecuperacionPassword(AuthDtos.SolicitarRecuperacionRequest request) {
        String correo = request.correo().trim().toLowerCase();

        if (!usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }

        codigoVerificacionService.enviarCodigoRecuperacion(correo);
    }

    @Transactional
    public void recuperarPassword(AuthDtos.RecuperarPasswordRequest request) {
        String correo = request.correo().trim().toLowerCase();
        codigoVerificacionService.validarCodigoRecuperacion(correo, request.codigoVerificacion());

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo actualizar la contraseña"));
        validarPasswordSegura(request.passwordNueva());
        usuario.setPassword(passwordService.encriptar(request.passwordNueva()));
        usuarioRepository.save(usuario);
        loginAttemptService.registrarExito(correo);
    }

    private void validarPasswordSegura(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener mínimo 8 caracteres");
        }
        if (password.chars().anyMatch(Character::isWhitespace)
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("La contraseña debe tener mayúscula, minúscula, número y no debe contener espacios");
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
