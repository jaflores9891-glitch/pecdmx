-- V2: personas con cuenta (empleados y gerentes). Los clientes no tienen cuenta.

CREATE TABLE usuarios (
    correo             VARCHAR(254) PRIMARY KEY,
    nombre             VARCHAR(100) NOT NULL,
    rol                VARCHAR(20)  NOT NULL,
    sucursal           VARCHAR(4),
    -- Solo la "huella" BCrypt de la contraseña, nunca la contraseña real.
    contrasena_cifrada VARCHAR(100) NOT NULL,
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT usuarios_correo_minusculas CHECK (correo = lower(correo)),
    CONSTRAINT usuarios_rol_valido        CHECK (rol IN ('EMPLEADO', 'GERENTE')),
    CONSTRAINT usuarios_sucursal_valida   CHECK (sucursal IN ('CDMX', 'GDL', 'MTY', 'PUE', 'QRO', 'TIJ')),
    -- Un empleado tiene sucursal; un gerente no.
    CONSTRAINT usuarios_sucursal_segun_rol CHECK (
        (rol = 'EMPLEADO' AND sucursal IS NOT NULL) OR (rol = 'GERENTE' AND sucursal IS NULL))
);
