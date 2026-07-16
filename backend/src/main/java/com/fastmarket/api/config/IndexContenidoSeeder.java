package com.fastmarket.api.config;

import com.fastmarket.api.model.IndexContenido;
import com.fastmarket.api.repository.IndexContenidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class IndexContenidoSeeder implements CommandLineRunner {
    private final IndexContenidoRepository repository;
    public IndexContenidoSeeder(IndexContenidoRepository repository) { this.repository = repository; }

    @Override
    public void run(String... args) {
        guardarSiNoExiste("destacado", "intro", "Productos recomendados para ti", "Estos productos representan la idea de FastMarket: artículos útiles, llamativos y con buena relación entre precio y calidad.", null, null, true, 1);
        guardarSiNoExiste("destacado", "producto-1", "Audífonos inalámbricos", "Ideales para clases, música, llamadas y entretenimiento.", "🎧", "S/ 79.90", true, 2);
        guardarSiNoExiste("destacado", "producto-2", "Bolso urbano", "Diseño moderno para salir, estudiar o trabajar.", "👜", "S/ 59.90", true, 3);
        guardarSiNoExiste("destacado", "producto-3", "Smartwatch básico", "Control de actividad, notificaciones y diseño cómodo.", "⌚", "S/ 99.90", true, 4);
        guardarSiNoExiste("destacado", "producto-4", "Lámpara decorativa", "Perfecta para dormitorios, escritorios y espacios modernos.", "💡", "S/ 39.90", true, 5);
        guardarSiNoExiste("destacado", "boton", "Ver catálogo completo", null, null, "productos.html", true, 6);

        guardarSiNoExiste("promocion", "intro", "Ofertas que no puedes dejar pasar", "Aprovecha descuentos especiales en productos seleccionados. Nuestras ofertas pueden cambiar según temporada, disponibilidad y novedades de la tienda.", null, null, true, 1);
        guardarSiNoExiste("promocion", "punto-1", "Descuentos por lanzamiento", null, null, null, true, 2);
        guardarSiNoExiste("promocion", "punto-2", "Promociones semanales", null, null, null, true, 3);
        guardarSiNoExiste("promocion", "punto-3", "Productos con stock limitado", null, null, null, true, 4);
        guardarSiNoExiste("promocion", "boton", "Ver ofertas disponibles", null, null, "productos.html?ofertas=1", true, 5);
        guardarSiNoExiste("promocion", "oferta", "Oferta", "Mini figura Peppa Pig", null, "S/ 50.00 antes S/ 60.00", true, 6);

        guardarSiNoExiste("opinion", "intro", "Lo que valoran nuestros clientes", "La confianza se construye con buena atención, claridad y cumplimiento en cada pedido.", null, null, true, 1);
        guardarSiNoExiste("opinion", "opinion-1", "Compra sencilla y rápida", "Me gustó poder encontrar productos y ofertas en un solo lugar.", null, null, true, 2);

        guardarSiNoExiste("ayuda", "intro", "Preguntas frecuentes", "Antes de comprar, revisa estas respuestas rápidas sobre productos, envíos y atención.", null, null, true, 1);
        guardarSiNoExiste("ayuda", "pregunta-1", "¿Hacen envíos a todo Ica?", "Sí, los envíos se coordinan según la zona, disponibilidad y horario acordado.", null, null, true, 2);
        guardarSiNoExiste("ayuda", "pregunta-2", "¿Puedo preguntar antes de comprar?", "Sí. Puedes escribir por el chat, redes sociales o el contacto de la tienda.", null, null, true, 3);
        guardarSiNoExiste("ayuda", "pregunta-3", "¿Las ofertas siempre están disponibles?", "No siempre. Algunas promociones dependen del stock y pueden cambiar durante la semana.", null, null, true, 4);
        guardarSiNoExiste("ayuda", "pregunta-4", "¿Puedo ver más productos?", "Sí. En la página de productos podrás revisar el catálogo completo de FastMarket.", null, null, true, 5);
    }

    private void guardarSiNoExiste(String tipo, String clave, String titulo, String descripcion, String imagen, String enlace, Boolean activo, Integer orden) {
        repository.findByTipoAndClave(tipo, clave).orElseGet(() ->
                repository.save(new IndexContenido(tipo, clave, titulo, descripcion, imagen, enlace, activo, orden))
        );
    }
}
