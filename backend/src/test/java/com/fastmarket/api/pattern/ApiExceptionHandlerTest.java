package com.fastmarket.api.pattern;

import com.fastmarket.api.controller.ApiExceptionHandler;
import com.fastmarket.api.exception.AutenticacionException;
import com.fastmarket.api.exception.AutorizacionException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void diferencia401Y403() {
        assertEquals(HttpStatus.UNAUTHORIZED, handler.manejarAutenticacion(new AutenticacionException("Token inválido")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, handler.manejarAutorizacion(new AutorizacionException("Sin permiso")).getStatusCode());
    }

    @Test
    void noExponeDetalleDeBaseDeDatos() {
        var respuesta = handler.manejarBaseDatos(new DataAccessResourceFailureException("tabla_secreta password=123"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertFalse(respuesta.getBody().toString().contains("tabla_secreta"));
        assertTrue(respuesta.getBody().containsKey("codigo"));
    }
}
