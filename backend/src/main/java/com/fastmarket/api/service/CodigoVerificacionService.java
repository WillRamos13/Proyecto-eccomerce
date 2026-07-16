package com.fastmarket.api.service;

import com.fastmarket.api.model.CodigoVerificacionCorreo;
import com.fastmarket.api.repository.CodigoVerificacionCorreoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class CodigoVerificacionService {
    private static final String TIPO_REGISTRO = "REGISTRO";
    private static final String TIPO_RECUPERACION = "RECUPERACION_PASSWORD";

    private final CodigoVerificacionCorreoRepository codigoRepository;
    private final PasswordService passwordService;
    private final CorreoService correoService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.verificacion.codigo-minutos:10}")
    private int minutosValidez;

    public CodigoVerificacionService(
            CodigoVerificacionCorreoRepository codigoRepository,
            PasswordService passwordService,
            CorreoService correoService
    ) {
        this.codigoRepository = codigoRepository;
        this.passwordService = passwordService;
        this.correoService = correoService;
    }

    @Transactional
    public void enviarCodigoRegistro(String correoOriginal, String nombre) {
        String correo = normalizarCorreo(correoOriginal);
        String codigo = generarCodigo();

        CodigoVerificacionCorreo entidad = new CodigoVerificacionCorreo();
        entidad.setCorreo(correo);
        entidad.setTipo(TIPO_REGISTRO);
        entidad.setCodigoHash(passwordService.encriptar(codigo));
        entidad.setExpiraEn(LocalDateTime.now().plusMinutes(minutosValidez));
        entidad.setUsado(false);
        entidad.setIntentos(0);

        codigoRepository.save(entidad);
        correoService.enviarCodigoRegistro(correo, nombre, codigo, minutosValidez);
    }

    @Transactional
    public void enviarCodigoRecuperacion(String correoOriginal) {
        String correo = normalizarCorreo(correoOriginal);
        String codigo = generarCodigo();

        CodigoVerificacionCorreo entidad = new CodigoVerificacionCorreo();
        entidad.setCorreo(correo);
        entidad.setTipo(TIPO_RECUPERACION);
        entidad.setCodigoHash(passwordService.encriptar(codigo));
        entidad.setExpiraEn(LocalDateTime.now().plusMinutes(minutosValidez));
        entidad.setUsado(false);
        entidad.setIntentos(0);

        codigoRepository.save(entidad);
        correoService.enviarCodigoRecuperacion(correo, codigo, minutosValidez);
    }

    @Transactional
    public void validarCodigoRecuperacion(String correoOriginal, String codigoIngresado) {
        validarCodigo(correoOriginal, codigoIngresado, TIPO_RECUPERACION, "Solicita un código antes de cambiar tu contraseña");
    }

    @Transactional
    public void validarCodigoRegistro(String correoOriginal, String codigoIngresado) {
        validarCodigo(correoOriginal, codigoIngresado, TIPO_REGISTRO, "Solicita un código de verificación antes de registrarte");
    }

    private void validarCodigo(String correoOriginal, String codigoIngresado, String tipo, String mensajeSinCodigo) {
        String correo = normalizarCorreo(correoOriginal);

        if (codigoIngresado == null || codigoIngresado.isBlank()) {
            throw new IllegalArgumentException("Ingresa el código enviado a tu correo");
        }

        CodigoVerificacionCorreo entidad = codigoRepository
                .findTopByCorreoIgnoreCaseAndTipoAndUsadoFalseOrderByCreadoEnDesc(correo, tipo)
                .orElseThrow(() -> new IllegalArgumentException(mensajeSinCodigo));

        if (entidad.getExpiraEn().isBefore(LocalDateTime.now())) {
            entidad.setUsado(true);
            codigoRepository.save(entidad);
            throw new IllegalArgumentException("El código venció. Solicita uno nuevo");
        }

        if (entidad.getIntentos() >= 5) {
            entidad.setUsado(true);
            codigoRepository.save(entidad);
            throw new IllegalArgumentException("Demasiados intentos. Solicita un código nuevo");
        }

        entidad.setIntentos(entidad.getIntentos() + 1);

        if (!passwordService.coincide(codigoIngresado.trim(), entidad.getCodigoHash())) {
            codigoRepository.save(entidad);
            throw new IllegalArgumentException("Código de verificación incorrecto");
        }

        entidad.setUsado(true);
        codigoRepository.save(entidad);
    }

    private String generarCodigo() {
        int numero = 100000 + random.nextInt(900000);
        return String.valueOf(numero);
    }

    private String normalizarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        return correo.trim().toLowerCase();
    }
}
