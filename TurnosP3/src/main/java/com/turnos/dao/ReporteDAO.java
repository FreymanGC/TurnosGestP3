package com.turnos.dao;

import com.turnos.modelo.ConteoReporte;
import com.turnos.modelo.EstadoTurno;
import com.turnos.modelo.TurnoDetallado;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAO {

    /** Turnos que llegaron a ser atendidos (tienen hora de inicio) entre dos fechas. */
    public List<TurnoDetallado> listarAtendidos(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT t.codigo, s.nombre AS servicio, t.fecha_operacion, t.id_estado_turno, "
                + "t.fecha_hora_generacion, t.fecha_hora_llamado, t.fecha_hora_inicio_atencion, "
                + "t.fecha_hora_finalizacion, v.numero AS numero_ventanilla, u.nombre_completo AS funcionario "
                + "FROM turno t JOIN servicio s ON s.id_servicio = t.id_servicio "
                + "LEFT JOIN ventanilla v ON v.id_ventanilla = t.id_ventanilla "
                + "LEFT JOIN usuario u ON u.id_usuario = t.id_usuario "
                + "WHERE t.fecha_operacion BETWEEN ? AND ? AND t.fecha_hora_inicio_atencion IS NOT NULL "
                + "ORDER BY t.fecha_hora_generacion, t.id_turno";
        return EjecutorSQL.consultar(conexion -> {
            List<TurnoDetallado> turnos = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setObject(1, desde);
                ps.setObject(2, hasta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int ventanilla = rs.getInt("numero_ventanilla");
                        turnos.add(new TurnoDetallado(
                                rs.getString("codigo"),
                                rs.getString("servicio"),
                                rs.getObject("fecha_operacion", LocalDate.class),
                                EstadoTurno.desdeId(rs.getInt("id_estado_turno")),
                                rs.getObject("fecha_hora_generacion", LocalDateTime.class),
                                rs.getObject("fecha_hora_llamado", LocalDateTime.class),
                                rs.getObject("fecha_hora_inicio_atencion", LocalDateTime.class),
                                rs.getObject("fecha_hora_finalizacion", LocalDateTime.class),
                                rs.wasNull() ? null : ventanilla,
                                rs.getString("funcionario")));
                    }
                }
            }
            return turnos;
        });
    }

    /** Turnos FINALIZADOS por cada funcionario entre dos fechas. */
    public List<ConteoReporte> contarAtendidosPorFuncionario(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT u.nombre_completo AS nombre, COUNT(t.id_turno) AS cantidad "
                + "FROM turno t JOIN usuario u ON u.id_usuario = t.id_usuario "
                + "WHERE t.id_estado_turno = ? AND t.fecha_operacion BETWEEN ? AND ? "
                + "GROUP BY u.id_usuario, u.nombre_completo ORDER BY cantidad DESC, nombre";
        return contar(sql, desde, hasta);
    }

    /** Turnos generados por cada servicio entre dos fechas (incluye servicios con 0). */
    public List<ConteoReporte> contarPorServicio(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT CONCAT(s.prefijo, ' - ', s.nombre) AS nombre, COUNT(t.id_turno) AS cantidad "
                + "FROM servicio s LEFT JOIN turno t ON t.id_servicio = s.id_servicio "
                + "AND t.id_estado_turno <> ? AND t.fecha_operacion BETWEEN ? AND ? "
                + "GROUP BY s.id_servicio, s.prefijo, s.nombre ORDER BY s.prefijo";
        return contar(sql, desde, hasta);
    }

    /**
     * El primer parámetro es un estado: FINALIZADO para el conteo por funcionario; en el conteo
     * por servicio se usa como "distinto de" con un id que no existe (0) para contar todos.
     */
    private List<ConteoReporte> contar(String sql, LocalDate desde, LocalDate hasta) {
        int estado = sql.contains("<> ?") ? 0 : EstadoTurno.FINALIZADO.getId();
        return EjecutorSQL.consultar(conexion -> {
            List<ConteoReporte> filas = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, estado);
                ps.setObject(2, desde);
                ps.setObject(3, hasta);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        filas.add(new ConteoReporte(rs.getString("nombre"), rs.getLong("cantidad")));
                    }
                }
            }
            return filas;
        });
    }
}
