# Catálogo resumido de endpoints

La lista ayuda a demostrar que el proyecto contiene operaciones de registro, consulta, actualización y eliminación.

| Módulo | Método y ruta | Propósito |
|---|---|---|
| Salud | `GET /api/health` | Verificar disponibilidad |
| Auth | `POST /api/auth/registro/enviar-codigo` | Enviar verificación |
| Auth | `POST /api/auth/registro` | Registrar usuario |
| Auth | `POST /api/auth/login` | Autenticar |
| Auth | `POST /api/auth/recuperar/enviar-codigo` | Enviar código de recuperación |
| Auth | `POST /api/auth/recuperar` | Cambiar contraseña |
| Productos | `GET /api/productos` | Listar/buscar |
| Productos | `GET /api/productos/page` | Listar paginado |
| Productos | `GET /api/productos/{id}` | Consultar detalle |
| Productos | `POST /api/productos` | Crear |
| Productos | `PUT /api/productos/{id}` | Actualizar |
| Productos | `DELETE /api/productos/{id}` | Eliminar |
| Carrito | `GET /api/carritos/usuario/{usuarioId}` | Consultar |
| Carrito | `PUT /api/carritos/usuario/{usuarioId}` | Actualizar |
| Carrito | `DELETE /api/carritos/usuario/{usuarioId}` | Vaciar |
| Pedidos | `GET /api/pedidos` | Listar según rol |
| Pedidos | `GET /api/pedidos/page` | Listar paginado |
| Pedidos | `GET /api/pedidos/usuario/{usuarioId}` | Consultar por cliente |
| Pedidos | `GET /api/pedidos/{pedidoId}/historial` | Consultar trazabilidad |
| Pedidos | `POST /api/pedidos/usuario/{usuarioId}` | Crear pedido |
| Pedidos | `PUT /api/pedidos/{pedidoId}/estado` | Cambiar estado |
| Pedidos | `GET /api/pedidos/{pedidoId}` | Consultar un pedido autorizado |
| Pago | `POST /api/pagos/pedidos/{pedidoId}/preferencia` | Crear preferencia autenticada con total persistido |
| Pago | `POST /api/pagos/pedidos/{pedidoId}/confirmar?paymentId=...` | Verificar pago con el proveedor |
| Pago | `POST /api/pagos/pedidos/{pedidoId}/cancelar` | Cancelar reserva pendiente y revertir recursos |
| Pago | `PUT /api/pagos/pedidos/{pedidoId}/manual` | Confirmar pago manual como administrador |
| Pago | `POST /api/pagos/pedidos/{pedidoId}/reembolsar` | Reembolsar y cancelar de forma idempotente |
| Pago | `POST /api/pagos/webhook` | Procesar notificación verificándola servidor-servidor |
| Cupones | `GET /api/cupones` | Listar |
| Cupones | `POST /api/cupones` | Crear |
| Cupones | `PUT /api/cupones/{id}` | Actualizar |
| Cupones | `DELETE /api/cupones/{id}` | Eliminar |
| Cupones | `POST /api/cupones/aplicar` | Validar y calcular descuento |
| Usuarios | `GET /api/usuarios` | Listar |
| Usuarios | `POST /api/usuarios` | Crear |
| Usuarios | `PUT /api/usuarios/{id}` | Actualizar perfil |
| Usuarios | `PUT /api/usuarios/{id}/gestion` | Administrar estado/rol |
| Reclamos | `POST /api/reclamos` | Registrar |
| Reclamos | `GET /api/reclamos` | Listar |
| Reclamos | `PUT /api/reclamos/{id}` | Atender/cambiar estado |
| Banners | `GET /api/banners` | Listar |
| Banners | `POST /api/banners` | Crear |
| Banners | `PUT /api/banners/{id}` | Actualizar |
| Banners | `DELETE /api/banners/{id}` | Eliminar |
| Reportes | `GET /api/admin/reportes` | Consultar reporte protegido |
| Chat | `POST /api/chat` | Consultar asistente |
