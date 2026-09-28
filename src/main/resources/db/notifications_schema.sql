-- ====================================================================
-- Grand Prix Tracker - Tabla de Notificaciones
-- Ejecutar en: Supabase Dashboard -> SQL Editor
-- ====================================================================

-- 1. Tabla de Notificaciones
CREATE TABLE IF NOT EXISTS notificaciones (
    id_notificacion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID,                                       -- NULL para notificaciones globales / ofertas generales
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL,                             -- 'ORDER_CONFIRMATION', 'OFFER', 'SYSTEM', 'REMINDER'
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    url_destino VARCHAR(255),                              -- Ruta interna (ej: '/#servicios', '/pedidos/123')
    metadata TEXT,                                         -- Datos JSON adicionales (precios, códigos, etc.)
    creado_en TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Índices de optimización para Supabase / PostgreSQL
-- Consultas por usuario
CREATE INDEX IF NOT EXISTS idx_notificaciones_id_usuario 
    ON notificaciones(id_usuario);

-- Contador y filtro rápido de no leídas (campana de notificaciones)
CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario_leido 
    ON notificaciones(id_usuario, leido);

-- Orden cronológico descendente
CREATE INDEX IF NOT EXISTS idx_notificaciones_creado_en 
    ON notificaciones(creado_en DESC);

-- 3. Datos iniciales de prueba (opcionales)
INSERT INTO notificaciones (id_usuario, titulo, mensaje, tipo, leido, url_destino)
VALUES 
    (NULL, '🏁 ¡Oferta de Temporada!', '20% de descuento en traslados para el GP de Interlagos.', 'OFFER', FALSE, '/#servicios'),
    (NULL, '🎟️ Nuevas entradas disponibles', 'Se habilitaron paquetes VIP para el Gran Premio de Monza.', 'OFFER', FALSE, '/#calendario');
