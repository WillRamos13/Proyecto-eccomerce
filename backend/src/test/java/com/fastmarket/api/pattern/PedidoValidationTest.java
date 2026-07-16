package com.fastmarket.api.pattern;

import com.fastmarket.api.dto.PedidoDtos;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PedidoValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaEntregaYMetodoInvalidos() {
        var request = new PedidoDtos.CrearPedidoRequest(
                "", "", "", "Criptomoneda", "abc", null,
                List.of(new PedidoDtos.ItemRequest(1L, 1)));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void aceptaDatosValidos() {
        var request = new PedidoDtos.CrearPedidoRequest(
                "Av. Principal 123", "Puerta azul", "Mañana", "Mercado Pago", "+51 999 999 999", "FAST10",
                List.of(new PedidoDtos.ItemRequest(1L, 1)));
        assertTrue(validator.validate(request).isEmpty());
    }
}
