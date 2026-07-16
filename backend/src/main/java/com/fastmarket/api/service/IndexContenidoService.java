package com.fastmarket.api.service;

import com.fastmarket.api.dto.IndexContenidoRequest;
import com.fastmarket.api.model.IndexContenido;
import com.fastmarket.api.repository.IndexContenidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IndexContenidoService {
    private final IndexContenidoRepository repository;
    public IndexContenidoService(IndexContenidoRepository repository) { this.repository = repository; }

    public List<IndexContenido> listar(String tipo, Boolean activo) {
        if (tipo != null && !tipo.isBlank() && Boolean.TRUE.equals(activo)) return repository.findByTipoAndActivoTrueOrderByOrdenAscIdAsc(tipo);
        if (tipo != null && !tipo.isBlank()) return repository.findByTipoOrderByOrdenAscIdAsc(tipo);
        if (Boolean.TRUE.equals(activo)) return repository.findByActivoTrueOrderByOrdenAscIdAsc();
        return repository.findAllByOrderByOrdenAscIdAsc();
    }
    public IndexContenido obtener(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contenido no encontrado")); }
    @Transactional public IndexContenido crear(IndexContenidoRequest request) { IndexContenido c = new IndexContenido(); aplicarDatos(c, request); return repository.save(c); }
    @Transactional public IndexContenido actualizar(Long id, IndexContenidoRequest request) { IndexContenido c = obtener(id); aplicarDatos(c, request); return repository.save(c); }
    public void eliminar(Long id) { if (!repository.existsById(id)) throw new IllegalArgumentException("Contenido no encontrado"); repository.deleteById(id); }
    private void aplicarDatos(IndexContenido c, IndexContenidoRequest request) {
        c.setTipo(request.tipo()); c.setClave(request.clave()); c.setTitulo(request.titulo()); c.setDescripcion(request.descripcion());
        c.setImagen(request.imagen()); c.setEnlace(request.enlace()); c.setActivo(request.activo() == null || request.activo());
        c.setOrden(request.orden() == null ? 1 : request.orden());
    }
}
