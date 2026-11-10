package com.turnos.dao;

import com.turnos.modelo.Ventanilla;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    public int insertar(Ventanilla ventanilla) {
        String sql = "INSERT INTO ventanilla (numero, nombre, disponible) VALUES (?, ?, ?)";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ventanilla.numero());
                ps.setString(2, ventanilla.nombre());
                ps.setBoolean(3, ventanilla.disponible());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    claves.next();
                    return claves.getInt(1);
                }
            }
        });
    }

    public void actualizar(Ventanilla ventanilla) {
        String sql = "UPDATE ventanilla SET numero = ?, nombre = ?, disponible = ? WHERE id_ventanilla = ?";
        EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, ventanilla.numero());
                ps.setString(2, ventanilla.nombre());
                ps.setBoolean(3, ventanilla.disponible());
                ps.setInt(4, ventanilla.id());
                return ps.executeUpdate();
            }
        });
    }

    public boolean existeNumero(int numero, int idExcluir) {
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT 1 FROM ventanilla WHERE numero = ? AND id_ventanilla <> ?")) {
                ps.setInt(1, numero);
                ps.setInt(2, idExcluir);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Ids de los servicios que puede atender la ventanilla (tabla ventanilla_servicio). */
    public List<Integer> listarIdsServicios(int idVentanilla) {
        return EjecutorSQL.consultar(conexion -> {
            List<Integer> ids = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT id_servicio FROM ventanilla_servicio WHERE id_ventanilla = ? ORDER BY id_servicio")) {
                ps.setInt(1, idVentanilla);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }
            }
            return ids;
        });
    }

    /** Reemplaza en una sola transacción los servicios que atiende la ventanilla. */
    public void reemplazarServicios(int idVentanilla, List<Integer> idsServicios) {
        EjecutorSQL.enTransaccion(conexion -> {
            try (PreparedStatement borrar = conexion.prepareStatement("DELETE FROM ventanilla_servicio WHERE id_ventanilla = ?");
                 PreparedStatement insertar = conexion.prepareStatement(
                         "INSERT INTO ventanilla_servicio (id_ventanilla, id_servicio) VALUES (?, ?)")) {
                borrar.setInt(1, idVentanilla);
                borrar.executeUpdate();
                for (Integer idServicio : idsServicios) {
                    insertar.setInt(1, idVentanilla);
                    insertar.setInt(2, idServicio);
                    insertar.addBatch();
                }
                insertar.executeBatch();
            }
            return null;
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
