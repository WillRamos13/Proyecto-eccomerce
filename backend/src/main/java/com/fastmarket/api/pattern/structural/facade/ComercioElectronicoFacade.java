package com.fastmarket.api.pattern.structural.facade;

import com.fastmarket.api.dto.PagoDtos;
import com.fastmarket.api.dto.PedidoDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.MercadoPagoService;
import com.fastmarket.api.service.PedidoService;
import org.springframework.stereotype.Service;

/** Patrón Facade: coordina pedidos, inventario, cupones, carrito y pagos. */
@Service
public class ComercioElectronicoFacade {
    private final PedidoService pedidoService;
    private final MercadoPagoService mercadoPagoService;

    public ComercioElectronicoFacade(PedidoService pedidoService, MercadoPagoService mercadoPagoService) {
        this.pedidoService = pedidoService;
        this.mercadoPagoService = mercadoPagoService;
    }

    public PedidoDtos.PedidoResponse procesarPedido(Long usuarioId, PedidoDtos.CrearPedidoRequest request) {
        return pedidoService.crear(usuarioId, request);
    }

    public PagoDtos.PreferenciaCreada crearPreferenciaPago(AuthTokenService.TokenData actor, Long pedidoId) {
        return mercadoPagoService.crearPreferencia(actor, pedidoId);
    }
}
