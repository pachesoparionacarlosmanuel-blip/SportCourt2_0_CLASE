-- Normaliza el campo `rol` de la tabla `usuarios` a los valores de negocio
-- oficiales del proyecto: 'admin' y 'usuario' (minusculas, en espanol).
--
-- Spring Security sigue trabajando internamente con ROLE_ADMIN / ROLE_USUARIO
-- (ver LoginController), esos valores NO se guardan en la base de datos.
--
-- Este script no requiere un motor de migraciones (no hay Flyway/Liquibase
-- en el proyecto); ejecutarlo manualmente una sola vez contra la base para
-- limpiar datos historicos que hayan quedado en ingles y/o mayusculas
-- ('ADMIN', 'USER', 'User', etc.).

UPDATE usuarios
SET rol = CASE LOWER(TRIM(rol))
    WHEN 'admin'   THEN 'admin'
    WHEN 'user'    THEN 'usuario'
    WHEN 'usuario' THEN 'usuario'
    ELSE LOWER(TRIM(rol))
END
WHERE rol IS NOT NULL;
