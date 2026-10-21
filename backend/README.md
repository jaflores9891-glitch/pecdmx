# backend — El negocio (Java)

Servicio principal de PECDMX: guías, estados del envío, sucursales, tarifas, cotización, usuarios y roles.

- Java 25 + Spring Boot 4, Maven (con Maven Wrapper: no hace falta instalar Maven).
- Base de datos: PostgreSQL, con tablas creadas por Flyway (`src/main/resources/db/migration/`).
- Seguridad: Spring Security con gafetes JWT y contraseñas cifradas con BCrypt.
- Tests: JUnit 5 + Testcontainers, con un PostgreSQL 17 real en Docker.

## Arquitectura hexagonal
```
mx.pecdmx.backend
├── dominio/          Reglas del negocio en Java puro: sucursales, tarifas, cotización, guías, usuarios
├── aplicacion/       Casos de uso (puertos de entrada) y lo que necesitan del exterior (puertos de salida)
└── infraestructura/  Adaptadores: API web, PostgreSQL, JWT, BCrypt y la configuración de Spring
```
El dominio y la aplicación no dependen de Spring ni de la base de datos: se prueban en milisegundos.

## Correr los tests
Necesitas Docker encendido (Testcontainers levanta PostgreSQL solo).
```bash
./mvnw verify
```

## Variables de entorno
Están en [`../.env.example`](../.env.example). En tu laptop van en un archivo `.env` (ignorado por git);
en producción, en el panel del servicio.

| Variable | Para qué |
|---|---|
| `PECDMX_JWT_SECRETO` | Llave para firmar los gafetes. Mínimo 32 caracteres; sin ella el servicio no arranca. |
| `PECDMX_GERENTE_CORREO`, `PECDMX_GERENTE_NOMBRE`, `PECDMX_GERENTE_CONTRASENA` | Primer gerente. Se crea al arrancar solo si no hay usuarios. |
