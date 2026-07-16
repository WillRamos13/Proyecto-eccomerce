package com.fastmarket.api.repository;

import com.fastmarket.api.model.Cupon;
import com.fastmarket.api.model.TipoCupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {
    Optional<Cupon> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    List<Cupon> findAllByOrderByIdDesc();
    List<Cupon> findByTipoOrderByIdDesc(TipoCupon tipo);
    List<Cupon> findByVendedorIdOrderByIdDesc(Long vendedorId);

    @Modifying
    @Query("""
            update Cupon c
            set c.usosActuales = c.usosActuales + 1
            where c.id = :id
              and (c.usosMaximos is null or c.usosMaximos <= 0 or c.usosActuales < c.usosMaximos)
            """)
    int incrementarUsoSiDisponible(@Param("id") Long id);

    @Modifying
    @Query("update Cupon c set c.usosActuales = case when c.usosActuales > 0 then c.usosActuales - 1 else 0 end where c.id = :id")
    int decrementarUso(@Param("id") Long id);
}
