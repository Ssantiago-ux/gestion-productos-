-- Script de creacion de la base de datos (SQLite)
CREATE TABLE IF NOT EXISTS productos (
    codigo      TEXT PRIMARY KEY,
    nombre      TEXT NOT NULL,
    descripcion TEXT,
    precio      REAL NOT NULL CHECK (precio >= 0),
    cantidad    INTEGER NOT NULL CHECK (cantidad >= 0)
);
