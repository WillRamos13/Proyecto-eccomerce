package com.fastmarket.api.controller;

import com.fastmarket.api.dto.BannerRequest;
import com.fastmarket.api.model.Banner;
import com.fastmarket.api.service.AuthTokenService;
import com.fastmarket.api.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {
    private final BannerService bannerService;
    private final AuthTokenService authTokenService;

    public BannerController(BannerService bannerService, AuthTokenService authTokenService) {
        this.bannerService = bannerService;
        this.authTokenService = authTokenService;
    }

    @GetMapping
    public List<Banner> listar(@RequestParam(required = false) Boolean activo) {
        return bannerService.listar(activo);
    }

    @GetMapping("/{id}")
    public Banner obtener(@PathVariable Long id) {
        return bannerService.obtener(id);
    }

    @PostMapping
    public Banner crear(@RequestHeader(value = "Authorization", required = false) String authorization, @Valid @RequestBody BannerRequest request) {
        authTokenService.requerirAdmin(authorization);
        return bannerService.crear(request);
    }

    @PutMapping("/{id}")
    public Banner actualizar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        authTokenService.requerirAdmin(authorization);
        return bannerService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        authTokenService.requerirAdmin(authorization);
        bannerService.eliminar(id);
    }
}
