package com.fastmarket.api.dto;

import com.fastmarket.api.model.EstadoUsuario;
import com.fastmarket.api.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AuthDtos {

    public record EnviarCodigoRegistroRequest(
            @NotBlank @Email String correo,
            String nombre
    ) {}

    public record RegistroRequest(
            @NotBlank String nombre,
            @NotBlank @Email String correo,
            @NotBlank
                    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
                    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S+$", message = "La contraseña debe tener mayúscula, minúscula, número y no debe contener espacios")
                    String password,
            String telefono,
            String documento,
            String direccion,
            @NotBlank String codigoVerificacion
    ) {}

    public record LoginRequest(
            @NotBlank @Email String correo,
            @NotBlank String password
    ) {}

    public record DireccionResponse(
            Long id,
            String direccion,
            String referencia,
            String distrito,
            String ciudad,
            Boolean principal
    ) {}

    public record SolicitarRecuperacionRequest(
            @NotBlank @Email String correo
    ) {}

    public record RecuperarPasswordRequest(
            @NotBlank @Email String correo,
            @NotBlank String codigoVerificacion,
            @NotBlank
                    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
                    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S+$", message = "La contraseña debe tener mayúscula, minúscula, número y no debe contener espacios")
                    String passwordNueva
    ) {}

    public record UsuarioResponse(
            Long id,
            String nombre,
            String correo,
            String telefono,
            String documento,
            Rol rol,
            EstadoUsuario estado,
            List<DireccionResponse> direcciones
    ) {}

    public record AuthResponse(
            UsuarioResponse usuario,
            String token
    ) {}
}
