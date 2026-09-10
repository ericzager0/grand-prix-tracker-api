-- ====================================================================
-- Grand Prix Tracker - Script de Creación de Índices para Supabase
-- Ejecutar en: Supabase Dashboard -> SQL Editor
-- Objetivo: Reducir escaneos secuenciales y consumo de CPU/RAM en PostgreSQL
-- ====================================================================

-- 1. Tabla: eventos_f1
-- Índice para filtrado por temporada (ej: temporada actual)
CREATE INDEX IF NOT EXISTS idx_eventos_f1_temporada 
    ON eventos_f1(temporada);

-- Índice para ordenamiento cronológico por fecha de inicio
CREATE INDEX IF NOT EXISTS idx_eventos_f1_fecha_inicio 
    ON eventos_f1(fecha_inicio ASC);

-- Índice para Foreign Key con circuitos (agiliza el JOIN FETCH)
CREATE INDEX IF NOT EXISTS idx_eventos_f1_id_circuito 
    ON eventos_f1(id_circuito);

-- Índice compuesto para consultas combinadas de temporada y ordenamiento cronológico
CREATE INDEX IF NOT EXISTS idx_eventos_f1_temp_fecha 
    ON eventos_f1(temporada, fecha_inicio ASC);

-- Índice para filtrado por estado (ej: 'Proximo', 'Finalizado')
CREATE INDEX IF NOT EXISTS idx_eventos_f1_estado 
    ON eventos_f1(estado);


-- 2. Tabla: circuitos
-- Índice para Foreign Key con ciudades (agiliza el JOIN FETCH)
CREATE INDEX IF NOT EXISTS idx_circuitos_id_ciudad 
    ON circuitos(id_ciudad);

-- Índice para búsquedas por nombre de circuito
CREATE INDEX IF NOT EXISTS idx_circuitos_nombre 
    ON circuitos(nombre);


-- 3. Tabla: ciudades
-- Índice para Foreign Key con países (agiliza el JOIN FETCH)
CREATE INDEX IF NOT EXISTS idx_ciudades_id_pais 
    ON ciudades(id_pais);

-- Índice para búsquedas por nombre de ciudad
CREATE INDEX IF NOT EXISTS idx_ciudades_nombre 
    ON ciudades(nombre);


-- 4. Tabla: paises
-- Índice para filtrado por continente
CREATE INDEX IF NOT EXISTS idx_paises_continente 
    ON paises(continente);

