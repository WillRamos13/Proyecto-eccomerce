package com.fastmarket.api.repository;

import com.fastmarket.api.model.Rol;
import com.fastmarket.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    List<Usuario> findByRolOrderByNombreAsc(Rol rol);
}
