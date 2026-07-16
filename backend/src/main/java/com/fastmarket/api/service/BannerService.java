package com.fastmarket.api.service;

import com.fastmarket.api.dto.BannerRequest;
import com.fastmarket.api.model.Banner;
import com.fastmarket.api.repository.BannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BannerService {
    private final BannerRepository bannerRepository;
    public BannerService(BannerRepository bannerRepository) { this.bannerRepository = bannerRepository; }

    public List<Banner> listar(Boolean activo) {
        if (Boolean.TRUE.equals(activo)) return bannerRepository.findByActivoTrue();
        return bannerRepository.findAll();
    }
    public Banner obtener(Long id) { return bannerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Banner no encontrado")); }
    @Transactional public Banner crear(BannerRequest request) { Banner banner = new Banner(); aplicarDatos(banner, request); return bannerRepository.save(banner); }
    @Transactional public Banner actualizar(Long id, BannerRequest request) { Banner banner = obtener(id); aplicarDatos(banner, request); return bannerRepository.save(banner); }
    public void eliminar(Long id) { if (!bannerRepository.existsById(id)) throw new IllegalArgumentException("Banner no encontrado"); bannerRepository.deleteById(id); }
    private void aplicarDatos(Banner banner, BannerRequest request) { banner.setTitulo(request.titulo()); banner.setDescripcion(request.descripcion()); banner.setImagen(request.imagen()); banner.setActivo(request.activo() == null || request.activo()); }
}
