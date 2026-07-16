package com.fastmarket.api.repository;

import com.fastmarket.api.model.CuponUso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuponUsoRepository extends JpaRepository<CuponUso, Long> {
    long countByCuponIdAndUsuarioId(Long cuponId, Long usuarioId);
    boolean existsByCuponIdAndUsuarioId(Long cuponId, Long usuarioId);
    Optional<CuponUso> findByPedidoId(Long pedidoId);
    List<CuponUso> findByCuponIdOrderByFechaDesc(Long cuponId);
    List<CuponUso> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
