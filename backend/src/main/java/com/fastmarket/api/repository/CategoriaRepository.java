package com.fastmarket.api.repository;

import com.fastmarket.api.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    Optional<Categoria> findByCodigoIgnoreCase(String codigo);
    List<Categoria> findByActivoTrueOrderByIdAsc();
}
