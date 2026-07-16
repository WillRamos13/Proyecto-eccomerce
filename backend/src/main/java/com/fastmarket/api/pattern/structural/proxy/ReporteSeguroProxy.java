package com.fastmarket.api.pattern.structural.proxy;

import com.fastmarket.api.dto.ReporteDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.ReporteService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Patrón Protection Proxy: controla el acceso a reportes administrativos antes
 * de delegar la operación costosa al servicio real.
 */
@Service
public class ReporteSeguroProxy {
    private final AuthTokenService authTokenService;
    private final ReporteService reporteService;

    public ReporteSeguroProxy(AuthTokenService authTokenService, ReporteService reporteService) {
        this.authTokenService = authTokenService;
        this.reporteService = reporteService;
    }

    public ReporteDtos.ReporteAdminResponse generar(
            String authorization,
            LocalDate desde,
            LocalDate hasta
    ) {
        authTokenService.requerirAdmin(authorization);
        return reporteService.generar(desde, hasta);
    }
}
