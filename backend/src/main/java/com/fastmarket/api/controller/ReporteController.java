package com.fastmarket.api.controller;

import com.fastmarket.api.dto.ReporteDtos;
import com.fastmarket.api.pattern.structural.proxy.ReporteSeguroProxy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reportes")
public class ReporteController {
    private final ReporteSeguroProxy reporteSeguroProxy;

    public ReporteController(ReporteSeguroProxy reporteSeguroProxy) {
        this.reporteSeguroProxy = reporteSeguroProxy;
    }

    @GetMapping
    public ReporteDtos.ReporteAdminResponse obtener(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return reporteSeguroProxy.generar(authorization, desde, hasta);
    }
}
