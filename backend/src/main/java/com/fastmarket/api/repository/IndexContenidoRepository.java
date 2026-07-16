package com.fastmarket.api.repository;

import com.fastmarket.api.model.IndexContenido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndexContenidoRepository extends JpaRepository<IndexContenido, Long> {
    List<IndexContenido> findAllByOrderByOrdenAscIdAsc();
    List<IndexContenido> findByTipoOrderByOrdenAscIdAsc(String tipo);
    List<IndexContenido> findByTipoAndActivoTrueOrderByOrdenAscIdAsc(String tipo);
    List<IndexContenido> findByActivoTrueOrderByOrdenAscIdAsc();
    Optional<IndexContenido> findByTipoAndClave(String tipo, String clave);
}
