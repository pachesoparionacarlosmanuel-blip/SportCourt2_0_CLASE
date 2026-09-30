-- =========================================================
-- SportCourt 2.0 - Esquema físico de base de datos
-- MySQL
-- =========================================================

CREATE DATABASE IF NOT EXISTS sportcourt_clase
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sportcourt_clase;

-- =========================================================
-- TABLA: usuarios
-- =========================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

-- =========================================================
-- TABLA: cancha
-- =========================================================

CREATE TABLE IF NOT EXISTS cancha (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150) NOT NULL,
    precio DOUBLE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    capacidad INT NULL,
    imagen VARCHAR(500) NULL
);

-- =========================================================
-- TABLA: clase
-- =========================================================

CREATE TABLE IF NOT EXISTS clase (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    icono VARCHAR(100) NOT NULL,
    nivel VARCHAR(50) NOT NULL,
    horario VARCHAR(100) NOT NULL,
    profesor VARCHAR(100) NOT NULL,
    precio DOUBLE NOT NULL,
    cupos INT NOT NULL
);

-- =========================================================
-- TABLA: reserva
-- =========================================================

CREATE TABLE IF NOT EXISTS reserva (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    cancha_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) NOT NULL,

    CONSTRAINT fk_reserva_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id),

    CONSTRAINT fk_reserva_cancha
        FOREIGN KEY (cancha_id)
        REFERENCES cancha(id)
);

-- =========================================================
-- TABLA: inscripcion
-- =========================================================

CREATE TABLE IF NOT EXISTS inscripcion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    clase_id INT NOT NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,

    CONSTRAINT fk_inscripcion_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id),

    CONSTRAINT fk_inscripcion_clase
        FOREIGN KEY (clase_id)
        REFERENCES clase(id)
);

-- =========================================================
-- Fin del esquema
-- =========================================================