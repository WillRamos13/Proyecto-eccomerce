package com.fastmarket.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductoDtos {
    public record ProductoResponse(
            Long id,
            String nombre,
            String categoria,
            String tipoProducto,
            Map<String, String> caracteristicas,
            BigDecimal precio,
            BigDecimal precioAntes,
            Integer stock,
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
            Boolean activo,
            Long vendedorId,
            String vendedorNombre,
            LocalDateTime creadoEn
    ) {}
}
