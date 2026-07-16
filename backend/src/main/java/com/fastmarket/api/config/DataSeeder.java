package com.fastmarket.api.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fastmarket.api.model.Banner;
import com.fastmarket.api.model.Categoria;
import com.fastmarket.api.model.CategoriaProducto;
import com.fastmarket.api.model.EstadoUsuario;
import com.fastmarket.api.model.Producto;
import com.fastmarket.api.model.Rol;
import com.fastmarket.api.model.Usuario;
import com.fastmarket.api.repository.BannerRepository;
import com.fastmarket.api.repository.CategoriaRepository;
import com.fastmarket.api.repository.ProductoRepository;
import com.fastmarket.api.repository.UsuarioRepository;
import com.fastmarket.api.service.PasswordService;

@Component
public class DataSeeder implements CommandLineRunner {
    private final ProductoRepository productoRepository;
    private final BannerRepository bannerRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordService passwordService;

    @Value("${app.admin.nombre:Administrador}")
    private String adminNombre;
    @Value("${app.admin.correo:admin@fastmarket.com}")
    private String adminCorreo;
    @Value("${app.admin.password:}")
    private String adminPassword;
    @Value("${app.admin.reset-password:false}")
    private boolean resetAdminPassword;

    public DataSeeder(ProductoRepository productoRepository, BannerRepository bannerRepository, CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository, PasswordService passwordService) {
        this.productoRepository = productoRepository;
        this.bannerRepository = bannerRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        crearAdmin();
        crearCategorias();
        crearProductos();
        crearBanners();
    }

    private void crearAdmin() {
        if (adminPassword == null || adminPassword.isBlank()) return;
        String correoAdminNormalizado = adminCorreo.trim().toLowerCase();
        Usuario admin = usuarioRepository.findByCorreoIgnoreCase(correoAdminNormalizado).orElse(null);
        if (admin == null) {
            admin = new Usuario(adminNombre, correoAdminNormalizado, passwordService.encriptar(adminPassword), Rol.ADMIN);
            admin.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(admin);
            return;
        }

        boolean modificado = false;

        if (admin.getRol() != Rol.ADMIN) {
            admin.setRol(Rol.ADMIN);
            modificado = true;
        }

        if (admin.getEstado() != EstadoUsuario.ACTIVO) {
            admin.setEstado(EstadoUsuario.ACTIVO);
            modificado = true;
        }

        if (admin.getNombre() == null || admin.getNombre().isBlank()) {
            admin.setNombre(adminNombre);
            modificado = true;
        }

        if (resetAdminPassword && !passwordService.coincide(adminPassword, admin.getPassword())) {
            admin.setPassword(passwordService.encriptar(adminPassword));
            modificado = true;
        } else if (!passwordService.esHashBcrypt(admin.getPassword())) {
            admin.setPassword(passwordService.encriptar(admin.getPassword()));
            modificado = true;
        }

        if (modificado) {
            usuarioRepository.save(admin);
        }
    }

    private void crearCategorias() {
        for (CategoriaProducto categoria : CategoriaProducto.values()) {
            if (categoriaRepository.existsByCodigoIgnoreCase(categoria.getCodigo())) continue;
            categoriaRepository.save(new Categoria(categoria.getCodigo(), categoria.getNombre()));
        }
    }

    private void crearProductos() {
        if (productoRepository.count() > 0) return;

        productoRepository.save(new Producto("Casaca ligera", "moda", new BigDecimal("119.90"), new BigDecimal("149.90"), 6, "img/productos/casaca.png", "Casaca cómoda y fácil de combinar.", true, true));
        productoRepository.save(new Producto("Audífonos inalámbricos", "tecnologia", new BigDecimal("79.90"), new BigDecimal("99.90"), 12, "img/productos/audifonos.png", "Audífonos cómodos para música, clases y llamadas.", true, true));
        productoRepository.save(new Producto("Lámpara LED", "hogar", new BigDecimal("39.90"), new BigDecimal("49.90"), 15, "img/productos/lampara.png", "Ideal para escritorio, dormitorio o sala.", true, false));
        productoRepository.save(new Producto("Set de resaltadores", "estudio", new BigDecimal("18.90"), new BigDecimal("24.90"), 20, "img/productos/resaltadores.png", "Resaltadores de colores para apuntes, oficina y universidad.", true, false));
        productoRepository.save(new Producto("Crema hidratante", "belleza", new BigDecimal("29.90"), new BigDecimal("39.90"), 14, "img/productos/crema.png", "Crema de uso diario para cuidado personal.", true, true));
        productoRepository.save(new Producto("Balón deportivo", "deportes", new BigDecimal("54.90"), new BigDecimal("69.90"), 9, "img/productos/balon.png", "Balón resistente para entrenamiento y recreación.", true, false));
        productoRepository.save(new Producto("Set de bloques didácticos", "juguetes", new BigDecimal("45.90"), null, 11, "img/productos/bloques.png", "Juego didáctico para entretenimiento y aprendizaje.", false, true));
    }

    private void crearBanners() {
        if (bannerRepository.count() > 0) return;
        bannerRepository.save(new Banner("Ofertas disponibles", "Promociones destacadas para tus compras.", "img/fondo1.png", true));
        bannerRepository.save(new Banner("Productos destacados", "Encuentra novedades y descuentos especiales.", "img/abrir.png", true));
        bannerRepository.save(new Banner("Compra segura", "Atención rápida y seguimiento de pedidos.", "img/intro.png", true));
    }
}
