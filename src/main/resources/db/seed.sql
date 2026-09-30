-- ====================================================================
-- Grand Prix Tracker - Datos de prueba
-- Complementa los países, ciudades, circuitos y eventos que ya existen en Supabase
-- (se referencian por su UUID real). Solo agrega lo que faltaba.
-- Hoteles y habitaciones NO van acá: los carga el microservicio de hoteles.
-- Idempotente: UUIDs fijos + ON CONFLICT DO NOTHING, se puede correr más de una vez.
-- Prefijo de UUID de lo agregado acá:
--   11111111 paises · 22222222 ciudades · 55555555 entradas_gradas
--   88888888 vuelos · 99999999 clientes · aaaaaaaa metodos_pago
-- ====================================================================

BEGIN;

-- Origen de los vuelos.
INSERT INTO paises (id_pais, nombre, codigo_iso, continente) VALUES
    ('11111111-0000-4000-8000-000000000001', 'Argentina', 'AR', 'latin-america')
ON CONFLICT DO NOTHING;

INSERT INTO ciudades (id_ciudad, id_pais, nombre) VALUES
    ('22222222-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Buenos Aires')
ON CONFLICT DO NOTHING;

-- Entradas: 3 por evento. La VIP de Las Vegas tiene stock 0 (para probar 409).
INSERT INTO entradas_gradas (id_entrada, id_evento, nombre_tribuna, precio_usd, stock_disponible, tipo) VALUES
    -- Madrid
    ('55555555-0000-4000-8000-000000000001', '992ae124-3d59-4adb-9fd2-f0e825c605e8', 'Grada General',        140.00, 500, 'General'),
    ('55555555-0000-4000-8000-000000000002', '992ae124-3d59-4adb-9fd2-f0e825c605e8', 'Tribuna Principal',    420.00, 200, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000003', '992ae124-3d59-4adb-9fd2-f0e825c605e8', 'Paddock Club',        4200.00,   8, 'VIP'),
    -- Bakú
    ('55555555-0000-4000-8000-000000000004', 'f1a34d7f-3f76-446b-bed9-22fced10166e', 'General Admission',    110.00, 400, 'General'),
    ('55555555-0000-4000-8000-000000000005', 'f1a34d7f-3f76-446b-bed9-22fced10166e', 'Grandstand Absheron',  350.00, 150, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000006', 'f1a34d7f-3f76-446b-bed9-22fced10166e', 'Paddock Club',        3900.00,   6, 'VIP'),
    -- Singapur
    ('55555555-0000-4000-8000-000000000007', '28780217-2728-4321-865d-db4de3d9ec26', 'Walkabout Zone 4',     190.00, 500, 'General'),
    ('55555555-0000-4000-8000-000000000008', '28780217-2728-4321-865d-db4de3d9ec26', 'Pit Grandstand',       680.00, 200, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000009', '28780217-2728-4321-865d-db4de3d9ec26', 'Paddock Club',        6500.00,   6, 'VIP'),
    -- Austin
    ('55555555-0000-4000-8000-000000000010', 'b3ebbdfa-a64b-4e3f-a0a8-782af0b5b472', 'General Admission',    150.00, 500, 'General'),
    ('55555555-0000-4000-8000-000000000011', 'b3ebbdfa-a64b-4e3f-a0a8-782af0b5b472', 'Main Grandstand',      450.00, 200, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000012', 'b3ebbdfa-a64b-4e3f-a0a8-782af0b5b472', 'Paddock Club',        4500.00,  10, 'VIP'),
    -- Ciudad de México
    ('55555555-0000-4000-8000-000000000013', '9f2762ef-6bde-4ee4-8c43-2a701e887162', 'Zona General',         120.00, 600, 'General'),
    ('55555555-0000-4000-8000-000000000014', '9f2762ef-6bde-4ee4-8c43-2a701e887162', 'Foro Sol',             380.00, 300, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000015', '9f2762ef-6bde-4ee4-8c43-2a701e887162', 'Paddock Club',        3800.00,   8, 'VIP'),
    -- São Paulo
    ('55555555-0000-4000-8000-000000000016', 'c25c1812-9ced-4f15-ad77-842e52214f05', 'Setor G',              180.00, 400, 'General'),
    ('55555555-0000-4000-8000-000000000017', 'c25c1812-9ced-4f15-ad77-842e52214f05', 'Arquibancada A',       520.00, 150, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000018', 'c25c1812-9ced-4f15-ad77-842e52214f05', 'Paddock Club',        5200.00,   5, 'VIP'),
    -- Las Vegas
    ('55555555-0000-4000-8000-000000000019', '0807f206-e3b8-4855-9ea7-2f8b8db1cfb7', 'General',              250.00, 300, 'General'),
    ('55555555-0000-4000-8000-000000000020', '0807f206-e3b8-4855-9ea7-2f8b8db1cfb7', 'Grandstand Zona T',    900.00, 100, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000021', '0807f206-e3b8-4855-9ea7-2f8b8db1cfb7', 'Paddock Club',        9000.00,   0, 'VIP'),
    -- Lusail
    ('55555555-0000-4000-8000-000000000022', '4513e753-0b39-4dbd-84a1-01eb594cbf09', 'General Admission',    130.00, 400, 'General'),
    ('55555555-0000-4000-8000-000000000023', '4513e753-0b39-4dbd-84a1-01eb594cbf09', 'Main Grandstand',      480.00, 180, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000024', '4513e753-0b39-4dbd-84a1-01eb594cbf09', 'Paddock Club',        5000.00,   6, 'VIP'),
    -- Abu Dabi
    ('55555555-0000-4000-8000-000000000025', '691efae9-acc9-4c45-95dc-210acf720d83', 'Abu Dhabi Hill',       200.00, 350, 'General'),
    ('55555555-0000-4000-8000-000000000026', '691efae9-acc9-4c45-95dc-210acf720d83', 'Main Grandstand',      750.00, 180, 'Asiento Numerado'),
    ('55555555-0000-4000-8000-000000000027', '691efae9-acc9-4c45-95dc-210acf720d83', 'Yas Paddock Club',    7000.00,   6, 'VIP')
ON CONFLICT DO NOTHING;

-- Vuelos ida y vuelta desde Buenos Aires para los eventos que todavía no pasaron.
-- El LATAM a São Paulo tiene 1 asiento (para probar 409) y el último ya partió (para probar 400).
INSERT INTO vuelos (id_vuelo, aerolinea, origen_id_ciudad, destino_id_ciudad, fecha_salida, fecha_llegada, precio_usd, stock_asientos) VALUES
    ('88888888-0000-4000-8000-000000000001', 'Singapore Airlines',    '22222222-0000-4000-8000-000000000001', '1c3915ad-97c0-46cf-9a93-d73826bee539', '2026-10-06 22:00:00+00', '2026-10-08 06:00:00+00', 1850.00, 30),
    ('88888888-0000-4000-8000-000000000002', 'Singapore Airlines',    '1c3915ad-97c0-46cf-9a93-d73826bee539', '22222222-0000-4000-8000-000000000001', '2026-10-12 14:00:00+00', '2026-10-13 20:00:00+00', 1800.00, 30),
    ('88888888-0000-4000-8000-000000000003', 'American Airlines',     '22222222-0000-4000-8000-000000000001', '6f9d5fbb-c1a3-492e-a625-1cb1942282a6', '2026-10-21 22:00:00+00', '2026-10-22 10:30:00+00', 1150.00, 40),
    ('88888888-0000-4000-8000-000000000004', 'American Airlines',     '6f9d5fbb-c1a3-492e-a625-1cb1942282a6', '22222222-0000-4000-8000-000000000001', '2026-10-26 15:00:00+00', '2026-10-27 03:30:00+00', 1100.00, 40),
    ('88888888-0000-4000-8000-000000000005', 'Aeroméxico',            '22222222-0000-4000-8000-000000000001', 'c3be3c61-bb7c-433b-8249-b59b1eba96c9', '2026-10-28 09:00:00+00', '2026-10-28 18:30:00+00',  850.00, 50),
    ('88888888-0000-4000-8000-000000000006', 'Aeroméxico',            'c3be3c61-bb7c-433b-8249-b59b1eba96c9', '22222222-0000-4000-8000-000000000001', '2026-11-02 18:00:00+00', '2026-11-03 03:30:00+00',  820.00, 50),
    ('88888888-0000-4000-8000-000000000007', 'Aerolíneas Argentinas', '22222222-0000-4000-8000-000000000001', '4501eeb9-010b-468a-9024-4343f698cf58', '2026-11-05 12:00:00+00', '2026-11-05 15:00:00+00',  320.00, 60),
    ('88888888-0000-4000-8000-000000000008', 'LATAM',                 '22222222-0000-4000-8000-000000000001', '4501eeb9-010b-468a-9024-4343f698cf58', '2026-11-05 15:30:00+00', '2026-11-05 18:30:00+00',  290.00,  1),
    ('88888888-0000-4000-8000-000000000009', 'Aerolíneas Argentinas', '4501eeb9-010b-468a-9024-4343f698cf58', '22222222-0000-4000-8000-000000000001', '2026-11-09 19:00:00+00', '2026-11-09 22:00:00+00',  310.00, 60),
    ('88888888-0000-4000-8000-000000000010', 'American Airlines',     '22222222-0000-4000-8000-000000000001', '0ddcaf82-55ca-4c1c-af87-b777acca60d4', '2026-11-17 21:00:00+00', '2026-11-18 11:00:00+00', 1350.00, 30),
    ('88888888-0000-4000-8000-000000000011', 'American Airlines',     '0ddcaf82-55ca-4c1c-af87-b777acca60d4', '22222222-0000-4000-8000-000000000001', '2026-11-22 17:00:00+00', '2026-11-23 07:00:00+00', 1300.00, 30),
    ('88888888-0000-4000-8000-000000000012', 'Qatar Airways',         '22222222-0000-4000-8000-000000000001', 'a81419b4-d2d2-4bc7-9255-7db766e71a32', '2026-11-25 20:00:00+00', '2026-11-26 15:00:00+00', 1550.00, 35),
    ('88888888-0000-4000-8000-000000000013', 'Qatar Airways',         'a81419b4-d2d2-4bc7-9255-7db766e71a32', '22222222-0000-4000-8000-000000000001', '2026-11-30 09:00:00+00', '2026-12-01 03:00:00+00', 1500.00, 35),
    ('88888888-0000-4000-8000-000000000014', 'Emirates',              '22222222-0000-4000-8000-000000000001', 'd028fc05-78cd-4dd8-96d0-9134ff624468', '2026-12-02 20:00:00+00', '2026-12-03 16:00:00+00', 1650.00, 35),
    ('88888888-0000-4000-8000-000000000015', 'Emirates',              'd028fc05-78cd-4dd8-96d0-9134ff624468', '22222222-0000-4000-8000-000000000001', '2026-12-07 08:00:00+00', '2026-12-08 04:00:00+00', 1600.00, 35),
    ('88888888-0000-4000-8000-000000000016', 'Aerolíneas Argentinas', '22222222-0000-4000-8000-000000000001', '4501eeb9-010b-468a-9024-4343f698cf58', '2026-09-01 12:00:00+00', '2026-09-01 15:00:00+00',  300.00, 20)
ON CONFLICT DO NOTHING;

INSERT INTO clientes (id_cliente, nombre, apellido, email, telefono) VALUES
    ('99999999-0000-4000-8000-000000000001', 'Cliente', 'Prueba',  'cliente.prueba@grandprixtracker.test', '+54 11 5555-0001'),
    ('99999999-0000-4000-8000-000000000002', 'Ana',     'Pilotti', 'ana.pilotti@grandprixtracker.test',    '+54 11 5555-0002')
ON CONFLICT DO NOTHING;

INSERT INTO metodos_pago (id_metodo, id_cliente, tipo, ultimos_4_digitos, proveedor_token, fecha_expiracion, nombre_titular) VALUES
    ('aaaaaaaa-0000-4000-8000-000000000001', '99999999-0000-4000-8000-000000000001', 'Credito', '4242', 'tok_test_credito_4242', '12/28', 'Cliente Prueba'),
    ('aaaaaaaa-0000-4000-8000-000000000002', '99999999-0000-4000-8000-000000000001', 'Debito',  '8812', 'tok_test_debito_8812',  '08/27', 'Cliente Prueba'),
    ('aaaaaaaa-0000-4000-8000-000000000003', '99999999-0000-4000-8000-000000000002', 'Credito', '1111', 'tok_test_credito_1111', '05/29', 'Ana Pilotti')
ON CONFLICT DO NOTHING;

COMMIT;
