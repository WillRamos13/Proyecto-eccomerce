package com.fastmarket.api.repository;

import com.fastmarket.api.model.LoginEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface LoginEventoRepository extends JpaRepository<LoginEvento, Long> {
    List<LoginEvento> findByFechaBetweenOrderByFechaAsc(LocalDateTime desde, LocalDateTime hasta);
}
