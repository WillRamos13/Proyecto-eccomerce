/**
 * Configuración portable del frontend.
 * - Mismo dominio que el backend: deja la cadena vacía.
 * - Frontend y backend separados: coloca la URL pública del backend, por ejemplo
 *   window.FASTMARKET_API_URL = "https://api.midominio.com";
 */
window.FASTMARKET_API_URL = window.FASTMARKET_API_URL || "";
