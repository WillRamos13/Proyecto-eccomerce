package com.fastmarket.api.model;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum CategoriaProducto {
    MODA("moda", "Moda"),
    TECNOLOGIA("tecnologia", "Tecnología"),
    HOGAR("hogar", "Hogar"),
    ESTUDIO("estudio", "Estudio"),
    BELLEZA("belleza", "Belleza"),
    DEPORTES("deportes", "Deportes"),
    JUGUETES("juguetes", "Juguetes");

    private final String codigo;
    private final String nombre;

    CategoriaProducto(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public static List<CategoriaResponse> listar() {
        return Arrays.stream(values())
                .map(categoria -> new CategoriaResponse(categoria.codigo, categoria.nombre))
                .toList();
    }

    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Selecciona una categoría");
        }

        String clave = normalizarClave(valor);
        for (CategoriaProducto categoria : values()) {
            if (categoria.codigo.equals(clave) || normalizarClave(categoria.nombre).equals(clave)) {
                return categoria.codigo;
            }
        }

        throw new IllegalArgumentException("Categoría inválida. Usa una de estas: " + nombresPermitidos());
    }

    public static String nombresPermitidos() {
        return Arrays.stream(values())
                .map(CategoriaProducto::getNombre)
                .collect(Collectors.joining(", "));
    }

    private static String normalizarClave(String valor) {
        String sinTildes = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public record CategoriaResponse(String codigo, String nombre) {}
}
