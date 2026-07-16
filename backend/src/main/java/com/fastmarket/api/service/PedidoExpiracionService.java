package com.fastmarket.api.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/** Libera reservas de inventario y cupones cuando un pago queda abandonado. */
@Service
public class PedidoExpiracionService {
    private static final Logger LOGGER = Logger.getLogger(PedidoExpiracionService.class.getName());
    private final PedidoService pedidoService;

    public PedidoExpiracionService(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Scheduled(fixedDelayString = "${app.pagos.expiracion-check-ms:60000}")
    public void liberarReservasVencidas() {
        int cantidad = pedidoService.expirarReservasPendientes();
        if (cantidad > 0) LOGGER.info("Reservas de pago liberadas: " + cantidad);
    }
}
