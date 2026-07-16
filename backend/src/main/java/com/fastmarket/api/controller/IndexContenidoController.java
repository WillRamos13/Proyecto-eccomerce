package com.fastmarket.api.controller;

import com.fastmarket.api.dto.IndexContenidoRequest;
import com.fastmarket.api.model.IndexContenido;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.IndexContenidoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/index-contenido")
public class IndexContenidoController {
    private final IndexContenidoService service;
    private final AuthTokenService authTokenService;

    public IndexContenidoController(IndexContenidoService service, AuthTokenService authTokenService) {
        this.service = service;
        this.authTokenService = authTokenService;
    }

    @GetMapping
    public List<IndexContenido> listar(@RequestParam(required = false) String tipo, @RequestParam(required = false) Boolean activo) {
        return service.listar(tipo, activo);
    }

    @GetMapping("/{id}")
    public IndexContenido obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/tipo/{tipo}")
    public List<IndexContenido> listarPorTipo(@PathVariable String tipo, @RequestParam(required = false) Boolean activo) {
        return service.listar(tipo, activo);
    }

    @PostMapping
    public IndexContenido crear(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody IndexContenidoRequest request) {
        authTokenService.requerirAdmin(authorization);
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public IndexContenido actualizar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @Valid @RequestBody IndexContenidoRequest request) {
        authTokenService.requerirAdmin(authorization);
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        authTokenService.requerirAdmin(authorization);
        service.eliminar(id);
    }
}
