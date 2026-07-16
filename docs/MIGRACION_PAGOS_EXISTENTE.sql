-- Ejecutar solo sobre una base FastMarket creada con una versión anterior.
-- Hacer copia de seguridad antes de aplicar.
BEGIN;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS estado_pago varchar(30) DEFAULT 'PENDIENTE' NOT NULL;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS pago_id varchar(100);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS preferencia_pago_id varchar(100);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS referencia_pago varchar(160);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS monto_pagado numeric(12,2);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS fecha_pago timestamp;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS fecha_expiracion_pago timestamp;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS stock_descontado boolean DEFAULT false NOT NULL;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS cupon_uso_activo boolean DEFAULT false NOT NULL;

-- Un identificador de Mercado Pago solo puede conciliarse con un pedido.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pedido_pago_id ON pedidos(pago_id) WHERE pago_id IS NOT NULL;

-- Los pedidos históricos entregados contra entrega se consideran cobrados.
UPDATE pedidos
SET estado_pago='APROBADO', monto_pagado=total, fecha_pago=COALESCE(fecha_pago, fecha)
WHERE estado='ENTREGADO' AND metodo_pago='Pago contra entrega' AND estado_pago='PENDIENTE';

-- Elimina duplicados históricos antes de crear el índice único, conservando el primer uso.
DELETE FROM cupon_usos a USING cupon_usos b
WHERE a.id > b.id AND a.cupon_id=b.cupon_id AND a.usuario_id=b.usuario_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cupon_uso_usuario ON cupon_usos(cupon_id, usuario_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cupon_uso_pedido ON cupon_usos(pedido_id) WHERE pedido_id IS NOT NULL;
COMMIT;
