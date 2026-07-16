package com.fastmarket.api.service;

import com.fastmarket.api.model.EstadoPedido;
import com.fastmarket.api.model.Pedido;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CorreoService {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${google.gmail.client-id:}")
    private String googleClientId;

    @Value("${google.gmail.client-secret:}")
    private String googleClientSecret;

    @Value("${google.gmail.refresh-token:}")
    private String googleRefreshToken;

    @Value("${app.mail.from:FastMarket <fastmarket2026utp@gmail.com>}")
    private String remitente;

    public CorreoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public boolean disponible() {
        return tieneTexto(googleClientId)
                && tieneTexto(googleClientSecret)
                && tieneTexto(googleRefreshToken)
                && tieneTexto(remitente);
    }

    public boolean enviarCodigoRegistro(String correo, String nombre, String codigo, int minutosValidez) {
        if (!disponible()) {
            System.out.println("[FastMarket DEV] Código de verificación para " + correo + ": " + codigo);
            return false;
        }

        String nombreCliente = nombre == null || nombre.isBlank()
                ? "cliente"
                : nombre.trim();

        String html = """
                <div style="font-family: Arial, sans-serif; background:#f6f6f6; padding:24px;">
                    <div style="max-width:520px; margin:auto; background:#ffffff; border-radius:16px; padding:28px; border:1px solid #eeeeee;">
                        <h2 style="color:#fd6403; margin-top:0;">Código de verificación FastMarket</h2>
                        <p>Hola <strong>%s</strong>,</p>
                        <p>Tu código para crear tu cuenta en FastMarket es:</p>
                        <div style="font-size:32px; font-weight:800; letter-spacing:8px; color:#111827; background:#fff3eb; padding:16px; text-align:center; border-radius:12px;">
                            %s
                        </div>
                        <p>Este código vence en <strong>%d minutos</strong>.</p>
                        <p style="color:#666666;">Si no solicitaste este registro, puedes ignorar este mensaje.</p>
                        <hr style="border:none; border-top:1px solid #eeeeee; margin:24px 0;">
                        <p style="font-size:12px; color:#888888;">FastMarket</p>
                    </div>
                </div>
                """.formatted(escaparHtml(nombreCliente), escaparHtml(codigo), minutosValidez);

        String texto = """
                Hola %s,

                Tu código de verificación para crear tu cuenta en FastMarket es:

                %s

                Este código vence en %d minutos.

                Si no solicitaste este registro, puedes ignorar este mensaje.

                FastMarket
                """.formatted(nombreCliente, codigo, minutosValidez);

        return enviarCorreo(correo, "Código de verificación FastMarket", texto, html,
                "[FastMarket DEV] Código de verificación para " + correo + ": " + codigo);
    }

    public boolean enviarCodigoRecuperacion(String correo, String codigo, int minutosValidez) {
        if (!disponible()) {
            System.out.println("[FastMarket DEV] Código de recuperación para " + correo + ": " + codigo);
            return false;
        }

        String html = """
                <div style="font-family: Arial, sans-serif; background:#f6f6f6; padding:24px;">
                    <div style="max-width:520px; margin:auto; background:#ffffff; border-radius:16px; padding:28px; border:1px solid #eeeeee;">
                        <h2 style="color:#fd6403; margin-top:0;">Código para recuperar tu contraseña</h2>
                        <p>Recibimos una solicitud para cambiar la contraseña de tu cuenta FastMarket.</p>
                        <div style="font-size:32px; font-weight:800; letter-spacing:8px; color:#111827; background:#fff3eb; padding:16px; text-align:center; border-radius:12px;">
                            %s
                        </div>
                        <p>Este código vence en <strong>%d minutos</strong>.</p>
                        <p style="color:#666666;">Si no solicitaste este cambio, ignora este mensaje.</p>
                    </div>
                </div>
                """.formatted(escaparHtml(codigo), minutosValidez);

        String texto = """
                Tu código para recuperar tu contraseña en FastMarket es:

                %s

                Este código vence en %d minutos.
                Si no solicitaste este cambio, ignora este mensaje.
                """.formatted(codigo, minutosValidez);

        return enviarCorreo(correo, "Código para recuperar contraseña FastMarket", texto, html,
                "[FastMarket DEV] Código de recuperación para " + correo + ": " + codigo);
    }

    public boolean enviarActualizacionPedido(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo) {
        if (pedido == null || pedido.getUsuario() == null || pedido.getUsuario().getCorreo() == null) {
            return false;
        }

        String correo = pedido.getUsuario().getCorreo();
        String nombre = pedido.getUsuario().getNombre() == null ? "cliente" : pedido.getUsuario().getNombre();
        String codigo = pedido.getCodigo() == null ? "Sin código" : pedido.getCodigo();
        String estado = nuevo == null ? "PENDIENTE" : nuevo.name();
        String asunto = anterior == null
                ? "Pedido registrado - " + codigo
                : "Actualización de pedido - " + codigo;

        String texto = "Hola " + nombre + ",\n\n"
                + "Tu pedido " + codigo + " ahora se encuentra en estado " + estado + ".\n"
                + "Total: S/ " + pedido.getTotal() + "\n\nFastMarket";

        String html = """
                <div style="font-family:Arial,sans-serif;background:#f6f6f6;padding:24px;">
                  <div style="max-width:520px;margin:auto;background:#fff;border-radius:16px;padding:28px;border:1px solid #eee;">
                    <h2 style="color:#fd6403;margin-top:0;">Actualización de pedido</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Tu pedido <strong>%s</strong> se encuentra en estado:</p>
                    <div style="font-size:24px;font-weight:800;color:#111827;background:#fff3eb;padding:14px;text-align:center;border-radius:12px;">%s</div>
                    <p>Total: <strong>S/ %s</strong></p>
                    <p style="font-size:12px;color:#777;">FastMarket - Sistema de Comercio Electrónico</p>
                  </div>
                </div>
                """.formatted(escaparHtml(nombre), escaparHtml(codigo), escaparHtml(estado), pedido.getTotal());

        return enviarCorreo(correo, asunto, texto, html,
                "[FastMarket DEV] Pedido " + codigo + " actualizado a " + estado + " para " + correo);
    }

    private boolean enviarCorreo(String destino, String asunto, String texto, String html, String mensajeDev) {
        try {
            String accessToken = obtenerAccessToken();
            String rawEmail = construirCorreoRaw(destino, asunto, texto, html);

            Map<String, String> payload = Map.of("raw", rawEmail);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GMAIL_SEND_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gmail API respondió " + response.statusCode() + ": " + response.body());
            }

            return true;
        } catch (Exception e) {
            System.out.println("[FastMarket MAIL] No se pudo enviar correo a " + destino + ": " + e.getMessage());
            System.out.println(mensajeDev);
            return false;
        }
    }

    private String obtenerAccessToken() throws Exception {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("client_id", googleClientId);
        parametros.put("client_secret", googleClientSecret);
        parametros.put("refresh_token", googleRefreshToken);
        parametros.put("grant_type", "refresh_token");

        String body = formUrlEncoded(parametros);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKEN_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google OAuth respondió " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> respuesta = objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object accessToken = respuesta.get("access_token");
        if (accessToken == null || accessToken.toString().isBlank()) {
            throw new IllegalStateException("Google OAuth no devolvió access_token");
        }

        return accessToken.toString();
    }

    private String construirCorreoRaw(String destino, String asunto, String texto, String html) {
        String boundary = "fastmarket_boundary_" + System.currentTimeMillis();
        String salto = "\r\n";

        String correo = "From: " + limpiarHeader(remitente) + salto
                + "To: " + limpiarHeader(destino) + salto
                + "Subject: " + codificarHeader(asunto) + salto
                + "MIME-Version: 1.0" + salto
                + "Content-Type: multipart/alternative; boundary=\"" + boundary + "\"" + salto
                + salto
                + "--" + boundary + salto
                + "Content-Type: text/plain; charset=UTF-8" + salto
                + "Content-Transfer-Encoding: base64" + salto
                + salto
                + codificarBase64Mime(texto) + salto
                + salto
                + "--" + boundary + salto
                + "Content-Type: text/html; charset=UTF-8" + salto
                + "Content-Transfer-Encoding: base64" + salto
                + salto
                + codificarBase64Mime(html) + salto
                + salto
                + "--" + boundary + "--" + salto;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(correo.getBytes(StandardCharsets.UTF_8));
    }

    private String formUrlEncoded(Map<String, String> parametros) {
        StringBuilder builder = new StringBuilder();
        parametros.forEach((clave, valor) -> {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(urlEncode(clave)).append('=').append(urlEncode(valor));
        });
        return builder.toString();
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }

    private String codificarHeader(String valor) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    private String codificarBase64Mime(String valor) {
        return Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    private String limpiarHeader(String valor) {
        return valor == null ? "" : valor.replace("\r", "").replace("\n", "").trim();
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
