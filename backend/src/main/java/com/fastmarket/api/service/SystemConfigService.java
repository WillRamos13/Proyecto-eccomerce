package com.fastmarket.api.service;

import com.fastmarket.api.model.SystemConfig;
import com.fastmarket.api.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class SystemConfigService {
    public static final String POWERBI_URL = "powerbi_url";
    public static final String COSTO_ENVIO = "costo_envio";

    private final SystemConfigRepository repository;

    public SystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    public Optional<String> obtener(String clave) {
        return repository.findById(clave).map(SystemConfig::getValor).filter(v -> v != null && !v.isBlank());
    }


    public BigDecimal obtenerDecimal(String clave, BigDecimal defecto) {
        return obtener(clave)
                .map(valor -> {
                    try {
                        return new BigDecimal(valor.trim());
                    } catch (RuntimeException ex) {
                        return defecto;
                    }
                })
                .orElse(defecto);
    }

    @Transactional
    public String guardar(String clave, String valor, Long usuarioId) {
        SystemConfig config = repository.findById(clave).orElse(new SystemConfig());
        config.setClave(clave);
        config.setValor(valor == null ? "" : valor.trim());
        config.setActualizadoPor(usuarioId);
        config.setActualizadoEn(java.time.LocalDateTime.now());
        repository.save(config);
        return config.getValor();
    }

    @Transactional
    public void eliminar(String clave, Long usuarioId) {
        SystemConfig config = repository.findById(clave).orElse(new SystemConfig(clave, "", usuarioId));
        config.setValor("");
        config.setActualizadoPor(usuarioId);
        config.setActualizadoEn(java.time.LocalDateTime.now());
        repository.save(config);
    }
}
