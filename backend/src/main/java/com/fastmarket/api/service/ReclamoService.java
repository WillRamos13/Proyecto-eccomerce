package com.fastmarket.api.service;

import com.fastmarket.api.dto.ReclamoDtos;
import com.fastmarket.api.model.*;
import com.fastmarket.api.repository.PedidoRepository;
import com.fastmarket.api.repository.ReclamoRepository;
import com.fastmarket.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ReclamoService {
    private final ReclamoRepository reclamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    public ReclamoService(ReclamoRepository reclamoRepository, UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository) {
        this.reclamoRepository = reclamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public ReclamoDtos.ReclamoResponse crear(Long usuarioId, ReclamoDtos.CrearReclamoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Pedido pedido = null;
        if (request.pedidoId() != null) {
            pedido = pedidoRepository.findById(request.pedidoId())
                    .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
            if (!pedido.getUsuario().getId().equals(usuarioId)) {
                throw new SecurityException("No puedes registrar un reclamo sobre un pedido de otro usuario");
            }
        }

        Reclamo reclamo = new Reclamo();
        reclamo.setCodigo(generarCodigo());
        reclamo.setUsuario(usuario);
        reclamo.setPedido(pedido);
        reclamo.setTipo(request.tipo());
        reclamo.setEstado(EstadoReclamo.ABIERTO);
        reclamo.setAsunto(request.asunto().trim());
        reclamo.setDescripcion(request.descripcion().trim());
        return toResponse(reclamoRepository.save(reclamo));
    }

    @Transactional(readOnly = true)
    public List<ReclamoDtos.ReclamoResponse> listarMios(Long usuarioId) {
        return reclamoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReclamoDtos.ReclamoResponse> listarTodos() {
        return reclamoRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReclamoDtos.ReclamoResponse actualizar(Long id, ReclamoDtos.ActualizarReclamoRequest request) {
        Reclamo reclamo = reclamoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reclamo no encontrado"));

        EstadoReclamo estadoAnterior = reclamo.getEstado();
        reclamo.setEstado(request.estado());
        reclamo.setRespuesta(limpiar(request.respuesta()));
        reclamo.setFechaActualizacion(LocalDateTime.now());

        boolean ahoraResuelto = request.estado() == EstadoReclamo.RESUELTO || request.estado() == EstadoReclamo.CERRADO;
        boolean antesResuelto = estadoAnterior == EstadoReclamo.RESUELTO || estadoAnterior == EstadoReclamo.CERRADO;
        if (ahoraResuelto && !antesResuelto) reclamo.setFechaResolucion(LocalDateTime.now());
        if (!ahoraResuelto) reclamo.setFechaResolucion(null);

        return toResponse(reclamoRepository.save(reclamo));
    }

    public ReclamoDtos.ReclamoResponse toResponse(Reclamo reclamo) {
        return new ReclamoDtos.ReclamoResponse(
                reclamo.getId(),
                reclamo.getCodigo(),
                reclamo.getUsuario().getId(),
                reclamo.getUsuario().getNombre(),
                reclamo.getUsuario().getCorreo(),
                reclamo.getPedido() != null ? reclamo.getPedido().getId() : null,
                reclamo.getPedido() != null ? reclamo.getPedido().getCodigo() : null,
                reclamo.getTipo(),
                reclamo.getEstado(),
                reclamo.getAsunto(),
                reclamo.getDescripcion(),
                reclamo.getRespuesta(),
                reclamo.getFechaCreacion(),
                reclamo.getFechaActualizacion(),
                reclamo.getFechaResolucion()
        );
    }

    private String generarCodigo() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String aleatorio = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "REC-" + fecha + "-" + aleatorio;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
