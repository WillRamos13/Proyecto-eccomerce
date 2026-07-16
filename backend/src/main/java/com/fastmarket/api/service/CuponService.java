package com.fastmarket.api.service;

import com.fastmarket.api.dto.CuponDtos;
import com.fastmarket.api.model.*;
import com.fastmarket.api.repository.CuponRepository;
import com.fastmarket.api.repository.CuponUsoRepository;
import com.fastmarket.api.repository.ProductoRepository;
import com.fastmarket.api.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CuponService {
    private static final long DURACION_PREDETERMINADA_DIAS = 30;
    private final CuponRepository cuponRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuponUsoRepository cuponUsoRepository;

    public CuponService(CuponRepository cuponRepository, ProductoRepository productoRepository, UsuarioRepository usuarioRepository, CuponUsoRepository cuponUsoRepository) {
        this.cuponRepository = cuponRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuponUsoRepository = cuponUsoRepository;
    }

    public List<CuponDtos.CuponResponse> listar(AuthTokenService.TokenData actor) {
        if (actor.rol() == Rol.VENDEDOR) {
            return cuponRepository.findByVendedorIdOrderByIdDesc(actor.usuarioId()).stream().map(this::toResponse).toList();
        }
        return cuponRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    public List<CuponDtos.CuponUsoResponse> listarUsos(AuthTokenService.TokenData actor, Long cuponId) {
        Cupon cupon = cuponRepository.findById(cuponId).orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado"));
        validarPermiso(actor, cupon);
        return cuponUsoRepository.findByCuponIdOrderByFechaDesc(cuponId).stream().map(this::toUsoResponse).toList();
    }

    @Transactional
    public CuponDtos.CuponResponse crear(AuthTokenService.TokenData actor, CuponDtos.CuponRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (cuponRepository.existsByCodigoIgnoreCase(codigo)) throw new IllegalArgumentException("Ya existe un cupón con ese código");

        Cupon cupon = new Cupon();
        cupon.setCodigo(codigo);
        aplicarDatos(cupon, actor, request);
        return toResponse(cuponRepository.save(cupon));
    }

    @Transactional
    public CuponDtos.CuponResponse actualizar(AuthTokenService.TokenData actor, Long id, CuponDtos.CuponRequest request) {
        Cupon cupon = cuponRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado"));
        validarPermiso(actor, cupon);

        String codigo = normalizarCodigo(request.codigo());
        if (!valor(cupon.getCodigo(), "").equalsIgnoreCase(codigo) && cuponRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Ya existe un cupón con ese código");
        }
        cupon.setCodigo(codigo);
        aplicarDatos(cupon, actor, request);
        return toResponse(cuponRepository.save(cupon));
    }

    @Transactional
    public void eliminar(AuthTokenService.TokenData actor, Long id) {
        Cupon cupon = cuponRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado"));
        validarPermiso(actor, cupon);
        cupon.setActivo(false);
        cuponRepository.save(cupon);
    }

    public CuponDtos.AplicarCuponResponse aplicar(CuponDtos.AplicarCuponRequest request) {
        return aplicar(request, null);
    }

    public CuponDtos.AplicarCuponResponse aplicar(CuponDtos.AplicarCuponRequest request, Long usuarioId) {
        if (request == null || request.codigo() == null || request.codigo().isBlank()) {
            throw new IllegalArgumentException("Ingresa un código de cupón.");
        }
        CalculoCupon calculo = calcularDescuento(request.codigo(), request.items(), usuarioId);
        return new CuponDtos.AplicarCuponResponse(
                calculo.cupon().getCodigo(),
                valor(calculo.cupon().getDescripcion(), ""),
                calculo.subtotalAplicable(),
                calculo.descuento(),
                calculo.descuento().compareTo(BigDecimal.ZERO) > 0 ? "Cupón aplicado correctamente." : "El cupón no genera descuento para estos productos."
        );
    }

    public CalculoCupon calcularDescuento(String codigo, List<CuponDtos.AplicarCuponItemRequest> items) {
        return calcularDescuento(codigo, items, null);
    }

    public CalculoCupon calcularDescuento(String codigo, List<CuponDtos.AplicarCuponItemRequest> items, Long usuarioId) {
        if (codigo == null || codigo.isBlank()) {
            return new CalculoCupon(null, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Cupon cupon = cuponRepository.findByCodigoIgnoreCase(normalizarCodigo(codigo))
                .orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado"));
        validarCuponActivo(cupon);
        validarUsoPorUsuario(cupon, usuarioId);

        BigDecimal subtotalAplicable = calcularSubtotalAplicable(cupon, items);
        if (subtotalAplicable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El cupón no aplica a los productos del carrito.");
        }

        BigDecimal montoMinimo = monto(cupon.getMontoMinimo());
        if (subtotalAplicable.compareTo(montoMinimo) < 0) {
            throw new IllegalArgumentException("El cupón requiere una compra mínima de S/ " + montoMinimo.setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal porcentaje = monto(cupon.getPorcentaje());
        BigDecimal montoFijo = monto(cupon.getMontoFijo());
        BigDecimal descuentoPorcentaje = subtotalAplicable.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal descuento = descuentoPorcentaje.add(montoFijo).setScale(2, RoundingMode.HALF_UP);
        if (descuento.compareTo(subtotalAplicable) > 0) descuento = subtotalAplicable;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) descuento = BigDecimal.ZERO;

        return new CalculoCupon(cupon, subtotalAplicable, descuento);
    }

    @Transactional
    public void registrarUso(Cupon cupon) {
        if (cupon == null || cupon.getId() == null) return;
        Cupon actualizado = cuponRepository.findById(cupon.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado"));
        validarCuponActivo(actualizado);
        int filasActualizadas = cuponRepository.incrementarUsoSiDisponible(actualizado.getId());
        if (filasActualizadas == 0) {
            throw new IllegalArgumentException("El cupón alcanzó su límite de usos");
        }
    }

    @Transactional
    public void registrarUso(Cupon cupon, Usuario usuario, Pedido pedido, BigDecimal descuento) {
        if (cupon == null || usuario == null || pedido == null) return;
        if (cuponUsoRepository.existsByCuponIdAndUsuarioId(cupon.getId(), usuario.getId())) {
            throw new IllegalArgumentException("Este usuario ya utilizó este cupón");
        }
        registrarUso(cupon);
        CuponUso uso = new CuponUso();
        uso.setCupon(cupon);
        uso.setUsuario(usuario);
        uso.setPedido(pedido);
        uso.setDescuentoAplicado(monto(descuento));
        try {
            cuponUsoRepository.saveAndFlush(uso);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Este usuario ya utilizó este cupón");
        }
    }

    /** Revierte exactamente una vez el uso asociado al pedido cancelado. */
    @Transactional
    public boolean revertirUso(Pedido pedido) {
        if (pedido == null || pedido.getId() == null) return false;
        CuponUso uso = cuponUsoRepository.findByPedidoId(pedido.getId()).orElse(null);
        if (uso == null) return false;
        Long cuponId = uso.getCupon() != null ? uso.getCupon().getId() : null;
        cuponUsoRepository.delete(uso);
        cuponUsoRepository.flush();
        if (cuponId != null) cuponRepository.decrementarUso(cuponId);
        return true;
    }

    private BigDecimal calcularSubtotalAplicable(Cupon cupon, List<CuponDtos.AplicarCuponItemRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("El carrito está vacío");
        BigDecimal subtotal = BigDecimal.ZERO;
        TipoCupon tipo = tipoSeguro(cupon);
        for (CuponDtos.AplicarCuponItemRequest item : items) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) continue;
            Producto producto = productoRepository.findById(item.productoId()).orElse(null);
            if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) continue;

            if (tipo == TipoCupon.VENDEDOR) {
                Long vendedorProductoId = producto.getVendedor() != null ? producto.getVendedor().getId() : null;
                Long vendedorCuponId = cupon.getVendedor() != null ? cupon.getVendedor().getId() : null;
                if (vendedorProductoId == null || vendedorCuponId == null || !vendedorProductoId.equals(vendedorCuponId)) continue;
            }
            if (tipo == TipoCupon.CATEGORIA) {
                String categoria = cupon.getCategoriaObjetivo();
                if (categoria == null || categoria.isBlank() || producto.getCategoria() == null || !producto.getCategoria().equalsIgnoreCase(categoria)) continue;
            }
            if (tipo == TipoCupon.PRODUCTO) {
                Long productoObjetivoId = cupon.getProductoObjetivo() != null ? cupon.getProductoObjetivo().getId() : null;
                if (productoObjetivoId == null || !productoObjetivoId.equals(producto.getId())) continue;
            }
            subtotal = subtotal.add(monto(producto.getPrecio()).multiply(BigDecimal.valueOf(item.cantidad())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarUsoPorUsuario(Cupon cupon, Long usuarioId) {
        if (usuarioId == null || cupon == null || cupon.getId() == null) return;
        if (cuponUsoRepository.existsByCuponIdAndUsuarioId(cupon.getId(), usuarioId)) {
            throw new IllegalArgumentException("Este usuario ya utilizó este cupón");
        }
    }

    private void validarCuponActivo(Cupon cupon) {
        if (!Boolean.TRUE.equals(cupon.getActivo())) throw new IllegalArgumentException("El cupón no está activo");
        LocalDateTime ahora = LocalDateTime.now();
        if (cupon.getFechaInicio() != null && cupon.getFechaInicio().isAfter(ahora)) throw new IllegalArgumentException("El cupón todavía no está disponible");
        if (cupon.getFechaFin() != null && cupon.getFechaFin().isBefore(ahora)) throw new IllegalArgumentException("El cupón venció");
        Integer usosMaximos = cupon.getUsosMaximos();
        int usosActuales = entero(cupon.getUsosActuales());
        if (usosMaximos != null && usosMaximos > 0 && usosActuales >= usosMaximos) {
            throw new IllegalArgumentException("El cupón alcanzó su límite de usos");
        }
    }

    private void aplicarDatos(Cupon cupon, AuthTokenService.TokenData actor, CuponDtos.CuponRequest request) {
        cupon.setDescripcion(limpiar(request.descripcion()));
        cupon.setPorcentaje(monto(request.porcentaje()));
        cupon.setMontoFijo(monto(request.montoFijo()));
        cupon.setMontoMinimo(monto(request.montoMinimo()));
        cupon.setUsosMaximos(request.usosMaximos());
        cupon.setUsosActuales(entero(cupon.getUsosActuales()));

        LocalDateTime fechaInicio = request.fechaInicio() == null ? LocalDateTime.now() : request.fechaInicio();
        LocalDateTime fechaFin = request.fechaFin() == null ? fechaInicio.plusDays(DURACION_PREDETERMINADA_DIAS) : request.fechaFin();
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin del cupón debe ser posterior a la fecha de inicio");
        }
        cupon.setFechaInicio(fechaInicio);
        cupon.setFechaFin(fechaFin);
        cupon.setActivo(request.activo() == null || Boolean.TRUE.equals(request.activo()));

        if (cupon.getPorcentaje().compareTo(BigDecimal.ZERO) <= 0 && cupon.getMontoFijo().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El cupón debe tener porcentaje o monto fijo de descuento");
        }
        if (cupon.getPorcentaje().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El porcentaje no puede superar 100%");
        }

        cupon.setVendedor(null);
        cupon.setCategoriaObjetivo(null);
        cupon.setProductoObjetivo(null);

        if (actor.rol() == Rol.VENDEDOR) {
            Usuario vendedor = usuarioRepository.findById(actor.usuarioId()).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            cupon.setTipo(TipoCupon.VENDEDOR);
            cupon.setVendedor(vendedor);
            return;
        }

        TipoCupon tipo = request.tipo() == null ? TipoCupon.GLOBAL : request.tipo();
        cupon.setTipo(tipo);
        if (tipo == TipoCupon.VENDEDOR) {
            if (request.vendedorId() == null) throw new IllegalArgumentException("Selecciona un vendedor para este cupón");
            Usuario vendedor = usuarioRepository.findById(request.vendedorId()).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            if (vendedor.getRol() != Rol.VENDEDOR) throw new IllegalArgumentException("El usuario seleccionado no es vendedor");
            cupon.setVendedor(vendedor);
        } else if (tipo == TipoCupon.CATEGORIA) {
            if (request.categoriaObjetivo() == null || request.categoriaObjetivo().isBlank()) throw new IllegalArgumentException("Selecciona una categoría para este cupón");
            cupon.setCategoriaObjetivo(CategoriaProducto.normalizar(request.categoriaObjetivo()));
        } else if (tipo == TipoCupon.PRODUCTO) {
            if (request.productoObjetivoId() == null) throw new IllegalArgumentException("Selecciona un producto para este cupón");
            Producto producto = productoRepository.findById(request.productoObjetivoId()).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            cupon.setProductoObjetivo(producto);
        }
    }

    private void validarPermiso(AuthTokenService.TokenData actor, Cupon cupon) {
        if (actor.rol() == Rol.ADMIN) return;
        Long vendedorId = cupon.getVendedor() != null ? cupon.getVendedor().getId() : null;
        if (actor.rol() == Rol.VENDEDOR && vendedorId != null && vendedorId.equals(actor.usuarioId())) return;
        throw new SecurityException("No autorizado para modificar este cupón");
    }

    private CuponDtos.CuponResponse toResponse(Cupon c) {
        return new CuponDtos.CuponResponse(
                c.getId(), valor(c.getCodigo(), ""), valor(c.getDescripcion(), ""), tipoSeguro(c),
                c.getVendedor() != null ? c.getVendedor().getId() : null,
                c.getVendedor() != null ? c.getVendedor().getNombre() : null,
                c.getCategoriaObjetivo(),
                c.getProductoObjetivo() != null ? c.getProductoObjetivo().getId() : null,
                c.getProductoObjetivo() != null ? c.getProductoObjetivo().getNombre() : null,
                monto(c.getPorcentaje()), monto(c.getMontoFijo()), monto(c.getMontoMinimo()), c.getUsosMaximos(), entero(c.getUsosActuales()),
                c.getFechaInicio(), c.getFechaFin(), Boolean.TRUE.equals(c.getActivo())
        );
    }

    private CuponDtos.CuponUsoResponse toUsoResponse(CuponUso uso) {
        return new CuponDtos.CuponUsoResponse(
                uso.getId(),
                uso.getCupon() != null ? uso.getCupon().getId() : null,
                uso.getCupon() != null ? uso.getCupon().getCodigo() : null,
                uso.getUsuario() != null ? uso.getUsuario().getId() : null,
                uso.getUsuario() != null ? uso.getUsuario().getNombre() : "Cliente",
                uso.getPedido() != null ? uso.getPedido().getId() : null,
                uso.getPedido() != null ? uso.getPedido().getCodigo() : null,
                monto(uso.getDescuentoAplicado()),
                uso.getFecha()
        );
    }

    private BigDecimal monto(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private int entero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private TipoCupon tipoSeguro(Cupon cupon) {
        return cupon == null || cupon.getTipo() == null ? TipoCupon.GLOBAL : cupon.getTipo();
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código del cupón es obligatorio");
        return codigo.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? "" : valor.trim();
    }

    private String valor(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    public record CalculoCupon(Cupon cupon, BigDecimal subtotalAplicable, BigDecimal descuento) {}
}
