package com.turnos.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Único punto donde se abren conexiones a MySQL (solo lo usa el servidor central).
 * Los datos de conexión se leen de src/main/resources/db.properties, que NO se sube a
 * GitHub (.gitignore). Para configurarlo: copiar db.properties.example como db.properties.
 */
public final class ConexionBD {

    private static final String ARCHIVO = "db.properties";
    private static final String PARAMETROS_POR_DEFECTO =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Costa_Rica"
            + "&connectTimeout=8000&socketTimeout=20000";

    private static Properties configuracion;

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        Properties props = configuracion();
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.usuario", ""),
                props.getProperty("db.clave", ""));
    }

    /** Lee db.properties una sola vez (synchronized: varios hilos del servidor lo usan). */
    private static synchronized Properties configuracion() throws SQLException {
        if (configuracion != null) {
            return configuracion;
        }
        Properties props = new Properties();
        try (InputStream in = ConexionBD.class.getClassLoader().getResourceAsStream(ARCHIVO)) {
            if (in == null) {
                throw new SQLException("No se encontró " + ARCHIVO + " en src/main/resources. "
                        + "Copie db.properties.example como db.properties y complete sus datos.", "08001");
            }
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + ARCHIVO, "08001", e);
        }
        String url = props.getProperty("db.url", "").trim();
        if (url.isEmpty()) {
            throw new SQLException("Falta la propiedad db.url en " + ARCHIVO, "08001");
        }
        if (!url.contains("?")) {
            url = url + PARAMETROS_POR_DEFECTO;
        }
        props.setProperty("db.url", url);
        configuracion = props;
        return configuracion;
    }

    /** Prueba rápida: Run Maven → "6 - Probar conexión a la base de datos". */
    public static void main(String[] args) {
        try (Connection conexion = obtenerConexion()) {
            System.out.println("Conexion a MySQL exitosa: " + conexion.getCatalog());
        } catch (SQLException e) {
            System.out.println("No se pudo conectar a la base de datos: " + e.getMessage());
        }
    }
}
