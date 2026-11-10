package com.turnos.dao;

import com.turnos.modelo.Servicio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServicioDAO {

    private static final String COLUMNAS = "id_servicio, prefijo, nombre, prioridad, activo";

    public List<Servicio> listarActivos() {
        String sql = "SELECT " + COLUMNAS + " FROM servicio WHERE activo = 1 ORDER BY prefijo";
        return EjecutorSQL.consultar(conexion -> {
            List<Servicio> servicios = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    servicios.add(mapear(rs));
                }
            }
            return servicios;
        });
    }

    public List<Servicio> listarTodos() {
        String sql = "SELECT " + COLUMNAS + " FROM servicio ORDER BY prefijo";
        return EjecutorSQL.consultar(conexion -> {
            List<Servicio> servicios = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    servicios.add(mapear(rs));
                }
            }
            return servicios;
        });
    }

    public int insertar(Servicio servicio) {
        String sql = "INSERT INTO servicio (prefijo, nombre, prioridad, activo) VALUES (?, ?, ?, ?)";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, servicio.prefijo());
                ps.setString(2, servicio.nombre());
                ps.setInt(3, servicio.prioridad());
                ps.setBoolean(4, servicio.activo());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    claves.next();
                    return claves.getInt(1);
                }
            }
        });
    }

    public void actualizar(Servicio servicio) {
        String sql = "UPDATE servicio SET prefijo = ?, nombre = ?, prioridad = ?, activo = ? WHERE id_servicio = ?";
        EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, servicio.prefijo());
                ps.setString(2, servicio.nombre());
                ps.setInt(3, servicio.prioridad());
                ps.setBoolean(4, servicio.activo());
                ps.setInt(5, servicio.id());
                return ps.executeUpdate();
            }
        });
    }

    /** ¿Otro servicio (distinto de idExcluir) ya usa ese valor en la columna indicada? */
    public boolean existe(String columna, String valor, int idExcluir) {
        if (!columna.equals("prefijo") && !columna.equals("nombre")) {
            throw new IllegalArgumentException("Columna no permitida: " + columna);
        }
        String sql = "SELECT 1 FROM servicio WHERE " + columna + " = ? AND id_servicio <> ?";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, valor);
                ps.setInt(2, idExcluir);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public Optional<Servicio> buscarPorId(int idServicio) {
        String sql = "SELECT " + COLUMNAS + " FROM servicio WHERE id_servicio = ?";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idServicio);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    private Servicio mapear(ResultSet rs) throws SQLException {
        return new Servicio(
                rs.getInt("id_servicio"),
                rs.getString("prefijo"),
                rs.getString("nombre"),
                rs.getInt("prioridad"),
                rs.getBoolean("activo"));
    }
}
