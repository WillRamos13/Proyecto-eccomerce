package com.fastmarket.api.service;

import com.fastmarket.api.dto.AuthDtos;
import com.fastmarket.api.dto.UsuarioDtos;
import com.fastmarket.api.model.Direccion;
import com.fastmarket.api.model.EstadoUsuario;
import com.fastmarket.api.model.Rol;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.repository.DireccionRepository;
import com.fastmarket.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final PasswordService passwordService;

    public UsuarioService(UsuarioRepository usuarioRepository, DireccionRepository direccionRepository, PasswordService passwordService) {
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
        this.passwordService = passwordService;
    }

    public List<AuthDtos.UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(DtoMapper::toUsuarioResponse).toList();
    }

    public AuthDtos.UsuarioResponse obtener(Long id) {
        return DtoMapper.toUsuarioResponse(obtenerEntidad(id));
    }

    @Transactional
    public AuthDtos.UsuarioResponse crearDesdeAdmin(UsuarioDtos.CrearUsuarioAdminRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (request.correo() == null || request.correo().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        validarPasswordSegura(request.password());

        String correo = request.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordService.encriptar(request.password()));
        usuario.setTelefono(limpiar(request.telefono()));
        usuario.setDocumento(limpiar(request.documento()));
        usuario.setRol(request.rol() == null ? Rol.CLIENTE : request.rol());
        usuario.setEstado(request.estado() == null ? EstadoUsuario.ACTIVO : request.estado());

        return DtoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public AuthDtos.UsuarioResponse actualizarDesdeAdmin(Long id, Long adminActualId, UsuarioDtos.ActualizarUsuarioAdminRequest request) {
        Usuario usuario = obtenerEntidad(id);

        if (request.nombre() != null && !request.nombre().isBlank()) usuario.setNombre(request.nombre().trim());
        usuario.setTelefono(limpiar(request.telefono()));
        usuario.setDocumento(limpiar(request.documento()));

        Rol nuevoRol = request.rol() == null ? usuario.getRol() : request.rol();
        EstadoUsuario nuevoEstado = request.estado() == null ? usuario.getEstado() : request.estado();

        if (adminActualId != null && adminActualId.equals(usuario.getId())) {
            if (nuevoRol != Rol.ADMIN || nuevoEstado != EstadoUsuario.ACTIVO) {
                throw new IllegalArgumentException("No puedes quitarte tu propio rol de administrador ni desactivar tu propia cuenta");
            }
        }

        usuario.setRol(nuevoRol);
        usuario.setEstado(nuevoEstado);

        if (request.passwordNueva() != null && !request.passwordNueva().isBlank()) {
            validarPasswordSegura(request.passwordNueva());
            usuario.setPassword(passwordService.encriptar(request.passwordNueva()));
        }

        return DtoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public AuthDtos.UsuarioResponse actualizar(Long id, UsuarioDtos.ActualizarUsuarioRequest request) {
        Usuario usuario = obtenerEntidad(id);
        if (request.nombre() != null && !request.nombre().isBlank()) usuario.setNombre(request.nombre().trim());
        usuario.setTelefono(limpiar(request.telefono()));
        usuario.setDocumento(limpiar(request.documento()));
        return DtoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public AuthDtos.UsuarioResponse agregarDireccion(Long id, UsuarioDtos.AgregarDireccionRequest request) {
        Usuario usuario = obtenerEntidad(id);

        if (request.direccion() == null || request.direccion().isBlank()) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }

        if (Boolean.TRUE.equals(request.principal()) || usuario.getDirecciones().isEmpty()) {
            usuario.getDirecciones().forEach(direccion -> direccion.setPrincipal(false));
        }

        Direccion direccion = new Direccion(
                usuario,
                request.direccion().trim(),
                limpiar(request.referencia()),
                limpiar(request.distrito()),
                limpiar(request.ciudad()),
                usuario.getDirecciones().isEmpty() || Boolean.TRUE.equals(request.principal())
        );

        usuario.getDirecciones().add(direccion);
        direccionRepository.save(direccion);
        return DtoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public AuthDtos.UsuarioResponse cambiarPassword(Long id, UsuarioDtos.CambiarPasswordRequest request) {
        Usuario usuario = obtenerEntidad(id);

        if (request.passwordActual() == null || request.passwordActual().isBlank()) {
            throw new IllegalArgumentException("Ingresa tu contraseña actual");
        }

        if (!passwordService.coincide(request.passwordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }

        validarPasswordSegura(request.passwordNueva());

        usuario.setPassword(passwordService.encriptar(request.passwordNueva()));
        return DtoMapper.toUsuarioResponse(usuarioRepository.save(usuario));
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

    private Usuario obtenerEntidad(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
