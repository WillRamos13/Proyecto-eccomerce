package com.fastmarket.api.repository;

import com.fastmarket.api.model.Reclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    List<Reclamo> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
    List<Reclamo> findAllByOrderByFechaCreacionDesc();
    List<Reclamo> findByFechaCreacionBetweenOrderByFechaCreacionDesc(LocalDateTime desde, LocalDateTime hasta);
}
