package com.fastmarket.api.service;

import com.fastmarket.api.dto.ProductoRequest;
import com.fastmarket.api.dto.ProductoDtos;
import com.fastmarket.api.model.CategoriaProducto;
import com.fastmarket.api.model.Producto;
import com.fastmarket.api.model.Rol;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.repository.ProductoRepository;
import com.fastmarket.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.Normalizer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProductoService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProductoService(ProductoRepository productoRepository, UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Page<ProductoDtos.ProductoResponse> listarPaginado(AuthTokenService.TokenData actor, int page, int size, Boolean incluirInactivos) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "id"));
        boolean mostrarInactivos = Boolean.TRUE.equals(incluirInactivos);
        if (actor.rol() == Rol.VENDEDOR) {
            return (mostrarInactivos
                    ? productoRepository.findByVendedorId(actor.usuarioId(), pageable)
                    : productoRepository.findByVendedorIdAndActivoTrue(actor.usuarioId(), pageable))
                    .map(DtoMapper::toProductoResponse);
        }
        return (mostrarInactivos ? productoRepository.findAll(pageable) : productoRepository.findByActivoTrue(pageable))
                .map(DtoMapper::toProductoResponse);
    }

    public List<ProductoDtos.ProductoResponse> listar(Boolean oferta, Boolean destacado, Boolean incluirInactivos) {
        if (Boolean.TRUE.equals(incluirInactivos)) return productoRepository.findAll().stream().map(DtoMapper::toProductoResponse).toList();
        if (Boolean.TRUE.equals(oferta) && Boolean.TRUE.equals(destacado)) return productoRepository.findByActivoTrueAndOfertaTrueAndDestacadoTrueOrderByIdDesc().stream().map(DtoMapper::toProductoResponse).toList();
        if (Boolean.TRUE.equals(oferta)) return productoRepository.findByActivoTrueAndOfertaTrueOrderByIdDesc().stream().map(DtoMapper::toProductoResponse).toList();
        if (Boolean.TRUE.equals(destacado)) return productoRepository.findByActivoTrueAndDestacadoTrueOrderByIdDesc().stream().map(DtoMapper::toProductoResponse).toList();
        return productoRepository.findByActivoTrueOrderByIdDesc().stream().map(DtoMapper::toProductoResponse).toList();
    }

    public List<ProductoDtos.ProductoResponse> listarParaPanel(AuthTokenService.TokenData actor, Boolean incluirInactivos) {
        if (actor.rol() == Rol.VENDEDOR) {
            return (Boolean.TRUE.equals(incluirInactivos)
                    ? productoRepository.findByVendedorIdOrderByIdDesc(actor.usuarioId())
                    : productoRepository.findByVendedorIdAndActivoTrueOrderByIdDesc(actor.usuarioId()))
                    .stream().map(DtoMapper::toProductoResponse).toList();
        }
        return (Boolean.TRUE.equals(incluirInactivos) ? productoRepository.findAll() : productoRepository.findByActivoTrueOrderByIdDesc())
                .stream().map(DtoMapper::toProductoResponse).toList();
    }

    public List<ProductoDtos.ProductoResponse> listarPorVendedor(Long vendedorId) {
        return productoRepository.findByVendedorIdOrderByIdDesc(vendedorId).stream().map(DtoMapper::toProductoResponse).toList();
    }

    public ProductoDtos.ProductoResponse obtener(Long id) {
        Producto producto = productoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (!Boolean.TRUE.equals(producto.getActivo())) throw new IllegalArgumentException("Producto no disponible");
        return DtoMapper.toProductoResponse(producto);
    }

    @Transactional
    public ProductoDtos.ProductoResponse crear(AuthTokenService.TokenData actor, ProductoRequest request) {
        Producto producto = new Producto();
        producto.setActivo(true);
        aplicarDatos(producto, actor, request);
        return DtoMapper.toProductoResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoDtos.ProductoResponse actualizar(AuthTokenService.TokenData actor, Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        validarPropietarioOVendedor(actor, producto);
        producto.setActivo(true);
        aplicarDatos(producto, actor, request);
        return DtoMapper.toProductoResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(AuthTokenService.TokenData actor, Long id) {
        Producto producto = productoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        validarPropietarioOVendedor(actor, producto);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private void aplicarDatos(Producto producto, AuthTokenService.TokenData actor, ProductoRequest request) {
        producto.setNombre(request.nombre().trim());
        producto.setCategoria(CategoriaProducto.normalizar(request.categoria()));
        producto.setTipoProducto(limpiarTexto(request.tipoProducto()));
        producto.setPrecio(request.precio());
        producto.setPrecioAntes(request.precioAntes());
        producto.setStock(request.stock());
        List<String> imagenes = limpiarImagenes(request);
        producto.setImagen(imagenes.isEmpty() ? "img/logo.png" : imagenes.get(0));
        producto.setImagenes(String.join("\n", imagenes));
        producto.setDescripcion(request.descripcion() == null ? "" : request.descripcion().trim());

        Map<String, String> caracteristicas = limpiarCaracteristicas(request.caracteristicas());
        if (request.caracteristicas() == null) {
            agregarCaracteristicaSiFalta(caracteristicas, "Marca", request.marca());
            agregarCaracteristicaSiFalta(caracteristicas, "Modelo", request.modelo());
            agregarCaracteristicaSiFalta(caracteristicas, "Color", request.color());
            agregarCaracteristicaSiFalta(caracteristicas, "Material", request.material());
            agregarCaracteristicaSiFalta(caracteristicas, "Talla o medida", request.talla());
            agregarCaracteristicaSiFalta(caracteristicas, "Garantía", request.garantia());
            agregarCaracteristicaSiFalta(caracteristicas, "Condición", request.condicion());
        }

        producto.setCaracteristicas(serializarCaracteristicas(caracteristicas));
        producto.setMarca(primerValor(request.marca(), buscarCaracteristica(caracteristicas, "marca")));
        producto.setModelo(primerValor(request.modelo(), buscarCaracteristica(caracteristicas, "modelo")));
        producto.setColor(primerValor(request.color(), buscarCaracteristica(caracteristicas, "color")));
        producto.setMaterial(primerValor(request.material(), buscarCaracteristica(caracteristicas, "material", "composicion")));
        producto.setTalla(primerValor(request.talla(), buscarCaracteristica(caracteristicas, "talla", "talla o medida", "numero", "tamano o medida", "medidas")));
        producto.setGarantia(primerValor(request.garantia(), buscarCaracteristica(caracteristicas, "garantia")));
        producto.setCondicion(primerValor(request.condicion(), buscarCaracteristica(caracteristicas, "condicion")));
        producto.setDetallesAdicionales(limpiarTexto(request.detallesAdicionales()));
        producto.setOferta(Boolean.TRUE.equals(request.oferta()));
        producto.setDestacado(Boolean.TRUE.equals(request.destacado()));

        if (actor.rol() == Rol.VENDEDOR) {
            Usuario vendedor = usuarioRepository.findById(actor.usuarioId()).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            producto.setVendedor(vendedor);
            return;
        }

        if (request.vendedorId() != null) {
            Usuario vendedor = usuarioRepository.findById(request.vendedorId()).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            if (vendedor.getRol() != Rol.VENDEDOR) throw new IllegalArgumentException("El usuario seleccionado no es vendedor");
            producto.setVendedor(vendedor);
        } else {
            producto.setVendedor(null);
        }
    }

    private String limpiarTexto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String primerValor(String principal, String alternativo) {
        String limpioPrincipal = limpiarTexto(principal);
        return limpioPrincipal.isBlank() ? limpiarTexto(alternativo) : limpioPrincipal;
    }

    private Map<String, String> limpiarCaracteristicas(Map<String, String> recibidas) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (recibidas == null) return resultado;

        Map<String, String> clavesNormalizadas = new LinkedHashMap<>();
        for (Map.Entry<String, String> entrada : recibidas.entrySet()) {
            if (entrada.getKey() == null || entrada.getValue() == null) continue;
            String nombre = entrada.getKey().trim();
            String valor = entrada.getValue().trim();
            if (nombre.isBlank() || valor.isBlank()) continue;

            String clave = normalizarClave(nombre);
            if (clave.isBlank() || clavesNormalizadas.containsKey(clave)) continue;
            clavesNormalizadas.put(clave, nombre);
            resultado.put(nombre, valor);
            if (resultado.size() >= 40) break;
        }
        return resultado;
    }

    private void agregarCaracteristicaSiFalta(Map<String, String> caracteristicas, String nombre, String valor) {
        String limpio = limpiarTexto(valor);
        if (limpio.isBlank() || buscarCaracteristica(caracteristicas, nombre) != null) return;
        caracteristicas.put(nombre, limpio);
    }

    private String buscarCaracteristica(Map<String, String> caracteristicas, String... nombres) {
        if (caracteristicas == null || caracteristicas.isEmpty()) return null;
        List<String> buscados = java.util.Arrays.stream(nombres).map(this::normalizarClave).toList();
        for (Map.Entry<String, String> entrada : caracteristicas.entrySet()) {
            if (buscados.contains(normalizarClave(entrada.getKey()))) return entrada.getValue();
        }
        return null;
    }

    private String normalizarClave(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String serializarCaracteristicas(Map<String, String> caracteristicas) {
        try {
            return JSON.writeValueAsString(caracteristicas == null ? Map.of() : caracteristicas);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron guardar las características del producto", e);
        }
    }


    private List<String> limpiarImagenes(ProductoRequest request) {
        List<String> resultado = new ArrayList<>();

        if (request.imagenes() != null) {
            for (String item : request.imagenes()) {
                if (item == null) continue;
                String limpio = item.trim();
                if (!limpio.isBlank() && !resultado.contains(limpio)) resultado.add(limpio);
            }
        }

        if (resultado.isEmpty() && request.imagen() != null && !request.imagen().isBlank()) {
            resultado.add(request.imagen().trim());
        }

        if (resultado.isEmpty()) resultado.add("img/logo.png");
        return resultado;
    }

    private void validarPropietarioOVendedor(AuthTokenService.TokenData actor, Producto producto) {
        if (actor.rol() == Rol.ADMIN) return;
        Long vendedorId = producto.getVendedor() != null ? producto.getVendedor().getId() : null;
        if (actor.rol() == Rol.VENDEDOR && vendedorId != null && vendedorId.equals(actor.usuarioId())) return;
        throw new SecurityException("No autorizado para modificar este producto");
    }
}
