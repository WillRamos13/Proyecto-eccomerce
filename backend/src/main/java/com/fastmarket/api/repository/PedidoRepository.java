package com.fastmarket.api.repository;

import com.fastmarket.api.model.EstadoPago;
import com.fastmarket.api.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = ?1")
    Optional<Pedido> findByIdForUpdate(Long id);

    Optional<Pedido> findByCodigo(String codigo);
    List<Pedido> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
    List<Pedido> findAllByOrderByFechaDesc();
    List<Pedido> findByFechaBetweenOrderByFechaAsc(LocalDateTime desde, LocalDateTime hasta);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Pedido> findByEstadoPagoAndFechaExpiracionPagoBefore(EstadoPago estadoPago, LocalDateTime limite);

    @Query("select distinct p from Pedido p join p.items i where i.vendedor.id = ?1 order by p.fecha desc")
    List<Pedido> findByVendedorIdOrderByFechaDesc(Long vendedorId);

    @Query(value = "select distinct p from Pedido p join p.items i where i.vendedor.id = ?1", countQuery = "select count(distinct p.id) from Pedido p join p.items i where i.vendedor.id = ?1")
    Page<Pedido> findByVendedorId(Long vendedorId, Pageable pageable);

    @Query("select count(distinct p.id) from Pedido p join p.items i where i.vendedor.id = ?1")
    long countByVendedorId(Long vendedorId);
}
