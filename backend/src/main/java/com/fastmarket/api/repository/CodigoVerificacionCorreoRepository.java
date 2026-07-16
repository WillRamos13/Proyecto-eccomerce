package com.fastmarket.api.repository;

import com.fastmarket.api.model.CodigoVerificacionCorreo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoVerificacionCorreoRepository extends JpaRepository<CodigoVerificacionCorreo, Long> {
    Optional<CodigoVerificacionCorreo> findTopByCorreoIgnoreCaseAndTipoAndUsadoFalseOrderByCreadoEnDesc(
            String correo,
            String tipo
    );
}
