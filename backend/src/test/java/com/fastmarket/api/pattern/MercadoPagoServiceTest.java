package com.fastmarket.api.pattern;

import com.fastmarket.api.dto.PagoDtos;
import com.fastmarket.api.exception.PagoDuplicadoException;
import com.fastmarket.api.model.*;
import com.fastmarket.api.pattern.structural.adapter.PasarelaPago;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.MercadoPagoService;
import com.fastmarket.api.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MercadoPagoServiceTest {

    @Test
    void construyePreferenciaSoloConDatosPersistidosDelPedido() {
        PasarelaPago pasarela = mock(PasarelaPago.class);
        PedidoService pedidos = mock(PedidoService.class);
        MercadoPagoService service = new MercadoPagoService(pasarela, pedidos, "https://tienda.test", "https://api.test");
        AuthTokenService.TokenData actor = new AuthTokenService.TokenData(7L, "ana@test.com", Rol.CLIENTE, Long.MAX_VALUE);
        Pedido pedido = pedido(10L, "PED-SEGURA", new BigDecimal("158.00"));

        when(pedidos.obtenerPedidoAutorizado(actor, 10L)).thenReturn(pedido);
        when(pasarela.crearPreferencia(any())).thenReturn(
                new PagoDtos.PreferenciaCreada("Mercado Pago", "PREF-1", null, "https://pago.test", null));

        PagoDtos.PreferenciaCreada respuesta = service.crearPreferencia(actor, 10L);

        assertEquals("PREF-1", respuesta.id());
        ArgumentCaptor<PagoDtos.PreferenciaPago> captor = ArgumentCaptor.forClass(PagoDtos.PreferenciaPago.class);
        verify(pasarela).crearPreferencia(captor.capture());
        assertEquals(new BigDecimal("158.00"), captor.getValue().monto());
        assertEquals("ana@test.com", captor.getValue().correoPagador());
        assertEquals("PED-SEGURA", captor.getValue().codigoPedido());
        verify(pedidos).registrarPreferencia(10L, "PREF-1");
    }

    @Test
    void rechazaPagoAprobadoConMontoDistinto() {
        PasarelaPago pasarela = mock(PasarelaPago.class);
        PedidoService pedidos = mock(PedidoService.class);
        MercadoPagoService service = new MercadoPagoService(pasarela, pedidos, "https://tienda.test", "https://api.test");
        AuthTokenService.TokenData actor = new AuthTokenService.TokenData(7L, "ana@test.com", Rol.CLIENTE, Long.MAX_VALUE);
        Pedido pedido = pedido(10L, "PED-SEGURA", new BigDecimal("158.00"));
        when(pedidos.obtenerPedidoAutorizado(actor, 10L)).thenReturn(pedido);
        when(pasarela.consultarPago("123")).thenReturn(new PagoDtos.PagoVerificado(
                "123", "approved", "PED-SEGURA", new BigDecimal("1.00"), "PREF-1", LocalDateTime.now()));

        assertThrows(SecurityException.class, () -> service.confirmarRetorno(actor, 10L, "123"));
        verify(pedidos, never()).confirmarPagoMercadoPago(anyLong(), any());
    }

    @Test
    void reembolsaSegundoPagoAprobadoDelMismoPedido() {
        PasarelaPago pasarela = mock(PasarelaPago.class);
        PedidoService pedidos = mock(PedidoService.class);
        MercadoPagoService service = new MercadoPagoService(pasarela, pedidos, "https://tienda.test", "https://api.test");
        AuthTokenService.TokenData actor = new AuthTokenService.TokenData(7L, "ana@test.com", Rol.CLIENTE, Long.MAX_VALUE);
        Pedido pedido = pedido(10L, "PED-SEGURA", new BigDecimal("158.00"));
        PagoDtos.PagoVerificado pago = new PagoDtos.PagoVerificado(
                "PAGO-2", "approved", "PED-SEGURA", new BigDecimal("158.00"), null, LocalDateTime.now());

        when(pedidos.obtenerPedidoAutorizado(actor, 10L)).thenReturn(pedido);
        when(pasarela.consultarPago("PAGO-2")).thenReturn(pago);
        when(pedidos.confirmarPagoMercadoPago(10L, pago)).thenThrow(new PagoDuplicadoException("duplicado"));
        when(pedidos.buscarPorCodigo("PED-SEGURA")).thenReturn(pedido);

        service.confirmarRetorno(actor, 10L, "PAGO-2");

        verify(pasarela).reembolsar("PAGO-2");
    }

    private Pedido pedido(Long id, String codigo, BigDecimal total) {
        Usuario usuario = new Usuario("Ana", "ana@test.com", "hash", Rol.CLIENTE);
        usuario.setId(7L);
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setCodigo(codigo);
        pedido.setUsuario(usuario);
        pedido.setTotal(total);
        pedido.setMetodoPago(PedidoService.MERCADO_PAGO);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        pedido.setFechaExpiracionPago(LocalDateTime.now().plusMinutes(20));
        return pedido;
    }
}
