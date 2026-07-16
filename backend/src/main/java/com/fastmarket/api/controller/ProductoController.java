package com.fastmarket.api.controller;

import com.fastmarket.api.dto.ProductoRequest;
import com.fastmarket.api.model.CategoriaProducto;
import com.fastmarket.api.dto.ProductoDtos;
import com.fastmarket.api.repository.CategoriaRepository;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoService productoService;
    private final AuthTokenService authTokenService;
    private final CategoriaRepository categoriaRepository;

    public ProductoController(ProductoService productoService, AuthTokenService authTokenService, CategoriaRepository categoriaRepository) {
        this.productoService = productoService;
        this.authTokenService = authTokenService;
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public List<ProductoDtos.ProductoResponse> listar(
            @RequestParam(required = false) Boolean oferta,
            @RequestParam(required = false) Boolean destacado,
            @RequestParam(required = false) Boolean incluirInactivos,
            @RequestParam(required = false) Long vendedorId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (vendedorId != null) {
            authTokenService.requerirAdmin(authorization);
            return productoService.listarPorVendedor(vendedorId);
        }
        if (Boolean.TRUE.equals(incluirInactivos)) {
            AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
            return productoService.listarParaPanel(actor, incluirInactivos);
        }
        return productoService.listar(oferta, destacado, incluirInactivos);
    }

    @GetMapping("/page")
    public Page<ProductoDtos.ProductoResponse> listarPaginado(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean incluirInactivos
    ) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return productoService.listarPaginado(actor, page, size, incluirInactivos);
    }

    @GetMapping("/categorias")
    public List<CategoriaProducto.CategoriaResponse> listarCategorias() {
        List<CategoriaProducto.CategoriaResponse> categorias = categoriaRepository.findByActivoTrueOrderByIdAsc().stream()
                .map(categoria -> new CategoriaProducto.CategoriaResponse(categoria.getCodigo(), categoria.getNombre()))
                .toList();
        return categorias.isEmpty() ? CategoriaProducto.listar() : categorias;
    }

    @GetMapping("/{id}")
    public ProductoDtos.ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    public ProductoDtos.ProductoResponse crear(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody ProductoRequest request) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return productoService.crear(actor, request);
    }

    @PutMapping("/{id}")
    public ProductoDtos.ProductoResponse actualizar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        return productoService.actualizar(actor, id, request);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdminOVendedor(authorization);
        productoService.eliminar(actor, id);
    }
}
