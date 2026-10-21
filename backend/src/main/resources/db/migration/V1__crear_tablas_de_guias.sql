-- V1: tablas para guardar las guías y su historial de rastreo.
-- Flyway ejecuta este archivo una sola vez y nunca se modifica: los cambios futuros van en V2, V3...

-- Da los números consecutivos de guía (1, 2, 3...). El dígito verificador lo agrega el dominio.
CREATE SEQUENCE guia_secuencia START WITH 1 INCREMENT BY 1 MAXVALUE 999999999 NO CYCLE;

CREATE TABLE guias (
    numero               VARCHAR(13)  PRIMARY KEY,
    origen               VARCHAR(4)   NOT NULL,
    destino              VARCHAR(4)   NOT NULL,
    zona                 SMALLINT     NOT NULL,
    peso_real_kg         NUMERIC      NOT NULL,
    peso_volumetrico_kg  NUMERIC      NOT NULL,
    peso_cobrable_kg     INTEGER      NOT NULL,
    subtotal             NUMERIC(10, 2) NOT NULL,
    iva                  NUMERIC(10, 2) NOT NULL,
    total                NUMERIC(10, 2) NOT NULL,
    nombre_destinatario  VARCHAR(100) NOT NULL,
    -- Evita que dos empleados cambien la misma guía al mismo tiempo y uno borre el cambio del otro.
    version              BIGINT       NOT NULL DEFAULT 0,

    -- Segunda línea de defensa: el dominio ya valida esto, pero la base de datos tampoco acepta datos imposibles.
    CONSTRAINT guias_numero_formato    CHECK (numero ~ '^PEC[0-9]{10}$'),
    CONSTRAINT guias_origen_valido     CHECK (origen  IN ('CDMX', 'GDL', 'MTY', 'PUE', 'QRO', 'TIJ')),
    CONSTRAINT guias_destino_valido    CHECK (destino IN ('CDMX', 'GDL', 'MTY', 'PUE', 'QRO', 'TIJ')),
    CONSTRAINT guias_zona_valida       CHECK (zona BETWEEN 1 AND 4),
    CONSTRAINT guias_peso_valido       CHECK (peso_real_kg > 0 AND peso_real_kg <= 70),
    CONSTRAINT guias_total_cuadra      CHECK (total = subtotal + iva)
);

CREATE TABLE eventos_rastreo (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_guia  VARCHAR(13)  NOT NULL REFERENCES guias (numero),
    orden        INTEGER      NOT NULL,
    estado       VARCHAR(30)  NOT NULL,
    fecha_hora   TIMESTAMPTZ  NOT NULL,
    sucursal     VARCHAR(4)   NOT NULL,
    nota         VARCHAR(300) NOT NULL DEFAULT '',

    -- Cada guía tiene su historial en orden 0, 1, 2... sin repetir posiciones.
    CONSTRAINT eventos_orden_unico     UNIQUE (numero_guia, orden),
    CONSTRAINT eventos_estado_valido   CHECK (estado IN ('REGISTRADA', 'EN_TRANSITO', 'EN_SUCURSAL_DESTINO',
                                                         'INCIDENCIA', 'ENTREGADA', 'CANCELADA')),
    CONSTRAINT eventos_sucursal_valida CHECK (sucursal IN ('CDMX', 'GDL', 'MTY', 'PUE', 'QRO', 'TIJ'))
);
