package com.fastmarket.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductoRequest(
        @NotBlank String nombre,
        @NotBlank String categoria,
        String tipoProducto,
        Map<String, String> caracteristicas,
        @NotNull @Positive BigDecimal precio,
        BigDecimal precioAntes,
        @NotNull @PositiveOrZero Integer stock,
        String imagen,
        List<String> imagenes,
        String descripcion,
        String marca,
        String modelo,
        String color,
        String material,
        String talla,
        String garantia,
        String condicion,
        String detallesAdicionales,
        Boolean oferta,
        Boolean destacado,
        Long vendedorId
) {}
