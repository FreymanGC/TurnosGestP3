package com.turnos.dao;

import com.turnos.modelo.Ventanilla;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VentanillaDAO {

    private static final String COLUMNAS = "id_ventanilla, numero, nombre, disponible";

    public List<Ventanilla> listar() {
        String sql = "SELECT " + COLUMNAS + " FROM ventanilla ORDER BY numero";
        return EjecutorSQL.consultar(conexion -> {
            List<Ventanilla> ventanillas = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventanillas.add(mapear(rs));
                }
            }
            return ventanillas;
        });
    }

    public Optional<Ventanilla> buscarPorId(int idVentanilla) {
        String sql = "SELECT " + COLUMNAS + " FROM ventanilla WHERE id_ventanilla = ?";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idVentanilla);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean actualizarDisponibilidad(int idVentanilla, boolean disponible) {
        String sql = "UPDATE ventanilla SET disponible = ? WHERE id_ventanilla = ?";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setBoolean(1, disponible);
                ps.setInt(2, idVentanilla);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private Ventanilla mapear(ResultSet rs) throws SQLException {
        return new Ventanilla(
                rs.getInt("id_ventanilla"),
                rs.getInt("numero"),
                rs.getString("nombre"),
                rs.getBoolean("disponible"));
    }
}
