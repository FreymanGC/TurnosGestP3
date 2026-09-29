package com.turnos.dao;

import com.turnos.modelo.Servicio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServicioDAO {

    private static final String COLUMNAS = "id_servicio, prefijo, nombre, activo";

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
                rs.getBoolean("activo"));
    }
}
