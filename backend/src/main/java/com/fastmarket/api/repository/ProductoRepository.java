package com.fastmarket.api.repository;

import com.fastmarket.api.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrueOrderByIdDesc();
    List<Producto> findByActivoTrueAndOfertaTrueOrderByIdDesc();
    List<Producto> findByActivoTrueAndDestacadoTrueOrderByIdDesc();
    List<Producto> findByActivoTrueAndOfertaTrueAndDestacadoTrueOrderByIdDesc();
    List<Producto> findByVendedorIdOrderByIdDesc(Long vendedorId);
    List<Producto> findByVendedorIdAndActivoTrueOrderByIdDesc(Long vendedorId);
    Page<Producto> findByVendedorId(Long vendedorId, Pageable pageable);
    Page<Producto> findByVendedorIdAndActivoTrue(Long vendedorId, Pageable pageable);
    Page<Producto> findByActivoTrue(Pageable pageable);

    long countByActivoTrue();
    long countByActivoTrueAndOfertaTrue();
    long countByVendedorIdAndActivoTrue(Long vendedorId);
    long countByVendedorIdAndActivoTrueAndOfertaTrue(Long vendedorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(p.stock), 0) from Producto p where p.activo = true")
    Long sumarStockTotalActivo();

    @Query("select coalesce(sum(p.stock), 0) from Producto p where p.activo = true and p.vendedor.id = ?1")
    Long sumarStockTotalActivoPorVendedor(Long vendedorId);
}
