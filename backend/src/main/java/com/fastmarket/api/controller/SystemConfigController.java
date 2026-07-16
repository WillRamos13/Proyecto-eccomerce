package com.fastmarket.api.controller;

import com.fastmarket.api.dto.SystemConfigDtos;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.SystemConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {
    private final SystemConfigService configService;
    private final AuthTokenService authTokenService;

    public SystemConfigController(SystemConfigService configService, AuthTokenService authTokenService) {
        this.configService = configService;
        this.authTokenService = authTokenService;
    }


    @GetMapping("/public")
    public Map<String, String> obtenerPublico() {
        return Map.of(
                "costoEnvio", configService.obtenerDecimal(SystemConfigService.COSTO_ENVIO, new java.math.BigDecimal("8.00")).toPlainString()
        );
    }

    @GetMapping("/powerbi")
    public SystemConfigDtos.ConfigResponse obtenerPowerBi(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authTokenService.requerirAdminOVendedor(authorization);
        return new SystemConfigDtos.ConfigResponse(SystemConfigService.POWERBI_URL, configService.obtener(SystemConfigService.POWERBI_URL).orElse(""));
    }

    @PutMapping("/powerbi")
    public Map<String, String> guardarPowerBi(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SystemConfigDtos.ConfigRequest request
    ) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdmin(authorization);
        String valor = configService.guardar(SystemConfigService.POWERBI_URL, request.valor(), actor.usuarioId());
        return Map.of("mensaje", "Reporte Power BI guardado en la base de datos", "valor", valor);
    }

    @DeleteMapping("/powerbi")
    public Map<String, String> limpiarPowerBi(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthTokenService.TokenData actor = authTokenService.requerirAdmin(authorization);
        configService.eliminar(SystemConfigService.POWERBI_URL, actor.usuarioId());
        return Map.of("mensaje", "Reporte Power BI eliminado de la base de datos");
    }
}
