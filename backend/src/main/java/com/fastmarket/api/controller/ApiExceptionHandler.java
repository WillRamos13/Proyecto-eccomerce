package com.fastmarket.api.controller;

import com.fastmarket.api.exception.AutenticacionException;
import com.fastmarket.api.exception.AutorizacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<Map<String, String>> manejarAutenticacion(AutenticacionException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", mensajeSeguro(ex, "Debes iniciar sesión")));
    }

    @ExceptionHandler({AutorizacionException.class, SecurityException.class})
    public ResponseEntity<Map<String, String>> manejarAutorizacion(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", mensajeSeguro(ex, "No tienes permiso para realizar esta acción")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", mensajeSeguro(ex, "Solicitud inválida")));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> manejarIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", mensajeSeguro(ex, "La operación no puede realizarse en el estado actual")));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> manejarStockConcurrente(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El stock o el cupón cambió mientras realizabas la compra. Actualiza tu carrito e inténtalo nuevamente."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of("error", mensaje));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> manejarBaseDatos(DataAccessException ex) {
        String codigo = codigoError();
        LOGGER.error("Error de base de datos [{}]", codigo, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuestaInterna(codigo));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarGeneral(Exception ex) {
        String codigo = codigoError();
        LOGGER.error("Error interno [{}]", codigo, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuestaInterna(codigo));
    }

    private Map<String, String> respuestaInterna(String codigo) {
        Map<String, String> respuesta = new LinkedHashMap<>();
        respuesta.put("error", "Ocurrió un error interno. Inténtalo nuevamente.");
        respuesta.put("codigo", codigo);
        return respuesta;
    }

    private String mensajeSeguro(Exception ex, String defecto) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? defecto : ex.getMessage();
    }

    private String codigoError() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
