-- V3: guías de demostración, una por cada estado, para que quien visite la página pueda rastrear algo real.
--
-- - Usan la secuencia 900000001 a 900000006 (PEC9000000019...). Las guías reales empiezan en 1,
--   así que nunca chocan, y se reconocen a simple vista.
-- - Los números tienen su dígito verificador de Luhn y los montos son los que calcula el cotizador.
-- - Los historiales respetan las transiciones permitidas (ver EstadoEnvio).
-- - Las fechas se cuentan desde que corre la migración, para que el historial se vea reciente.
-- - Los destinatarios son ficticios (además, el rastreo público nunca los muestra).

INSERT INTO guias (numero, origen, destino, zona, peso_real_kg, peso_volumetrico_kg, peso_cobrable_kg,
                   subtotal, iva, total, nombre_destinatario)
VALUES ('PEC9000000019', 'CDMX', 'GDL', 3, 3.5, 4.8,   5, 310.00,  49.60, 359.60, 'Laura Méndez'),
       ('PEC9000000027', 'MTY',  'TIJ', 4, 8,   12.0, 12, 675.00, 108.00, 783.00, 'Roberto Garza'),
       ('PEC9000000035', 'PUE',  'QRO', 2, 1.2, 1.0,   2, 168.00,  26.88, 194.88, 'Ana Torres'),
       ('PEC9000000043', 'GDL',  'CDMX', 3, 12, 19.2, 20, 685.00, 109.60, 794.60, 'Miguel Ángel Ruiz'),
       ('PEC9000000050', 'TIJ',  'MTY', 4, 5,   9.6,  10, 605.00,  96.80, 701.80, 'Sofía Herrera'),
       ('PEC9000000068', 'QRO',  'PUE', 2, 2,   1.8,   2, 168.00,  26.88, 194.88, 'Carlos Ramírez');

INSERT INTO eventos_rastreo (numero_guia, orden, estado, fecha_hora, sucursal, nota)
VALUES
    -- 1. Entregada: el recorrido completo.
    ('PEC9000000019', 0, 'REGISTRADA',          now() - interval '6 days',            'CDMX', 'Paquete recibido en sucursal'),
    ('PEC9000000019', 1, 'EN_TRANSITO',         now() - interval '5 days 19 hours',   'CDMX', 'Salió en ruta a Guadalajara'),
    ('PEC9000000019', 2, 'EN_SUCURSAL_DESTINO', now() - interval '4 days',            'GDL',  'Listo para recoger'),
    ('PEC9000000019', 3, 'ENTREGADA',           now() - interval '3 days',            'GDL',  'Entregado al destinatario'),
    -- 2. En tránsito.
    ('PEC9000000027', 0, 'REGISTRADA',          now() - interval '2 days',            'MTY',  'Paquete recibido en sucursal'),
    ('PEC9000000027', 1, 'EN_TRANSITO',         now() - interval '1 day 20 hours',    'MTY',  'Salió en ruta a Tijuana'),
    -- 3. En sucursal de destino: listo para recoger.
    ('PEC9000000035', 0, 'REGISTRADA',          now() - interval '3 days',            'PUE',  'Paquete recibido en sucursal'),
    ('PEC9000000035', 1, 'EN_TRANSITO',         now() - interval '2 days 20 hours',   'PUE',  'Salió en ruta a Querétaro'),
    ('PEC9000000035', 2, 'EN_SUCURSAL_DESTINO', now() - interval '1 day',             'QRO',  'Listo para recoger'),
    -- 4. Registrada: todavía se puede cancelar.
    ('PEC9000000043', 0, 'REGISTRADA',          now() - interval '3 hours',           'GDL',  'Paquete recibido en sucursal'),
    -- 5. Incidencia: un retraso (sirve para probar una queja en el chat).
    ('PEC9000000050', 0, 'REGISTRADA',          now() - interval '5 days',            'TIJ',  'Paquete recibido en sucursal'),
    ('PEC9000000050', 1, 'EN_TRANSITO',         now() - interval '4 days 18 hours',   'TIJ',  'Salió en ruta a Monterrey'),
    ('PEC9000000050', 2, 'INCIDENCIA',          now() - interval '2 days',            'TIJ',  'Retraso por cierre de carretera; seguirá en cuanto se libere'),
    -- 6. Cancelada.
    ('PEC9000000068', 0, 'REGISTRADA',          now() - interval '4 days',            'QRO',  'Paquete recibido en sucursal'),
    ('PEC9000000068', 1, 'CANCELADA',           now() - interval '3 days 23 hours',   'QRO',  'Cancelada a petición del cliente');
