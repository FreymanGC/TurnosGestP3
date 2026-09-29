package com.turnos.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String HOST = "mysql.us.cloudlogin.co";
    private static final String PUERTO = "3306";
    private static final String BASE_DATOS = "gamabasis_p3g2";
    private static final String USUARIO = "gamabasis_p3g2";
    private static final String CLAVE = "JiV7Qle35%";

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE_DATOS
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Costa_Rica";

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    public static void main(String[] args) {
        try (Connection conexion = obtenerConexion()) {
            System.out.println(
                "Conexion a MySQL exitosa: " + conexion.getCatalog()
            );
        } catch (SQLException e) {
            System.out.println("No se pudo conectar a la base de datos:");
            e.printStackTrace();
        }
    }
}