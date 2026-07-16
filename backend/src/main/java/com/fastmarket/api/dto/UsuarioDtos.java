package com.fastmarket.api.dto;

import com.fastmarket.api.model.EstadoUsuario;
import com.fastmarket.api.model.Rol;

public class UsuarioDtos {
    public record ActualizarUsuarioRequest(String nombre, String telefono, String documento) {}
    public record AgregarDireccionRequest(String direccion, String referencia, String distrito, String ciudad, Boolean principal) {}
    public record CambiarPasswordRequest(String passwordActual, String passwordNueva) {}

    public record CrearUsuarioAdminRequest(
            String nombre,
            String correo,
            String password,
            String telefono,
            String documento,
            Rol rol,
            EstadoUsuario estado
    ) {}

    public record ActualizarUsuarioAdminRequest(
            String nombre,
            String telefono,
            String documento,
            Rol rol,
            EstadoUsuario estado,
            String passwordNueva
    ) {}
}
