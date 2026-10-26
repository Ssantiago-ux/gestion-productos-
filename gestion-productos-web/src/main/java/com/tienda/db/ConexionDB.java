package com.tienda.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Gestiona la conexion JDBC con SQLite y crea la tabla si no existe. */
public class ConexionDB {

    // Ruta del archivo: variable de entorno DB_PATH o "productos.db" por defecto
    private static final String URL =
            "jdbc:sqlite:" + System.getenv().getOrDefault("DB_PATH", "productos.db");

    private static final String CREAR_TABLA = """
            CREATE TABLE IF NOT EXISTS productos (
                codigo      TEXT PRIMARY KEY,
                nombre      TEXT NOT NULL,
                descripcion TEXT,
                precio      REAL NOT NULL CHECK (precio >= 0),
                cantidad    INTEGER NOT NULL CHECK (cantidad >= 0)
            )
            """;

    /** Devuelve una nueva conexion. Quien la use debe cerrarla (try-with-resources). */
    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void inicializar() throws SQLException {
        try (Connection con = getConexion();
             Statement st = con.createStatement()) {
            st.execute(CREAR_TABLA);
        }
    }
}
