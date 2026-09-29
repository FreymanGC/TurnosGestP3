package com.turnos.dao;

import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.EstadoTurno;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.modelo.TurnoLlamado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla turno. Las dos operaciones que compiten entre clientes (generar y llamar
 * siguiente) se hacen en una transacción con bloqueo de fila, de modo que:
 *  - dos generadoras del mismo servicio nunca reciben el mismo número;
 *  - dos operadores que piden turno a la vez nunca reciben el mismo turno.
 */
public class TurnoDAO {

    private static final String FORMATO_NUMERO = "%03d";

    private static final String COLUMNAS =
            "t.id_turno, t.id_servicio, t.fecha_operacion, t.numero, t.codigo, t.id_estado_turno, "
            + "t.fecha_hora_generacion, t.fecha_hora_llamado, t.fecha_hora_inicio_atencion, "
            + "t.fecha_hora_finalizacion, t.id_ventanilla, t.id_usuario";

    public Turno generar(Servicio servicio, LocalDate fechaOperacion, LocalDateTime ahora) {
        return EjecutorSQL.enTransaccion(conexion -> {
            bloquearServicio(conexion, servicio.id());
            int numero = siguienteNumero(conexion, servicio.id(), fechaOperacion);
            String codigo = servicio.prefijo() + String.format(FORMATO_NUMERO, numero);
            long idTurno = insertar(conexion, servicio.id(), fechaOperacion, numero, codigo, ahora);
            return leer(conexion, idTurno).orElseThrow(() -> new SQLException("Turno recién insertado no encontrado"));
        });
    }

    /**
     * Toma el turno EN_ESPERA más antiguo de los servicios que atiende la ventanilla y lo marca
     * LLAMADO, todo en una transacción. Bloqueos:
     *  1. Fila de la ventanilla (FOR UPDATE): serializa las validaciones por ventanilla (disponible,
     *     sin turno en curso), por eso esas reglas viven aquí y no en el servicio.
     *  2. Fila del turno (FOR UPDATE SKIP LOCKED): si otro operador ya reservó el primer turno, este
     *     salta al siguiente en vez de esperar o duplicar el llamado.
     */
    public Optional<Turno> llamarSiguiente(int idVentanilla, int idUsuario, LocalDate fechaOperacion, LocalDateTime ahora) {
        return EjecutorSQL.enTransaccion(conexion -> {
            validarVentanillaLibre(conexion, idVentanilla, fechaOperacion);
            Optional<Long> idTurno = reservarSiguienteEnEspera(conexion, idVentanilla, fechaOperacion);
            if (idTurno.isEmpty()) {
                return Optional.<Turno>empty();
            }
            marcarLlamado(conexion, idTurno.get(), idVentanilla, idUsuario, ahora);
            return leer(conexion, idTurno.get());
        });
    }

    public Optional<Turno> iniciarAtencion(long idTurno, LocalDateTime ahora) {
        String sql = "UPDATE turno SET id_estado_turno = ?, fecha_hora_inicio_atencion = ? "
                + "WHERE id_turno = ? AND id_estado_turno = ?";
        return transicionar(sql, idTurno, EstadoTurno.EN_ATENCION.getId(), ahora, idTurno, EstadoTurno.LLAMADO.getId());
    }

    public Optional<Turno> finalizarAtencion(long idTurno, LocalDateTime ahora) {
        String sql = "UPDATE turno SET id_estado_turno = ?, fecha_hora_finalizacion = ? "
                + "WHERE id_turno = ? AND id_estado_turno = ?";
        return transicionar(sql, idTurno, EstadoTurno.FINALIZADO.getId(), ahora, idTurno, EstadoTurno.EN_ATENCION.getId());
    }

    public Optional<Turno> registrarNuevoLlamado(long idTurno, LocalDateTime ahora) {
        String sql = "UPDATE turno SET fecha_hora_llamado = ? WHERE id_turno = ? AND id_estado_turno = ?";
        return transicionar(sql, idTurno, ahora, idTurno, EstadoTurno.LLAMADO.getId());
    }

    public Optional<Turno> buscarPorId(long idTurno) {
        return EjecutorSQL.consultar(conexion -> leer(conexion, idTurno));
    }

    public Optional<Turno> buscarActivoDeVentanilla(int idVentanilla, LocalDate fechaOperacion) {
        String sql = "SELECT " + COLUMNAS + " FROM turno t "
                + "WHERE t.id_ventanilla = ? AND t.fecha_operacion = ? AND t.id_estado_turno IN (?, ?) "
                + "ORDER BY t.fecha_hora_llamado DESC, t.id_turno DESC LIMIT 1";
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idVentanilla);
                ps.setObject(2, fechaOperacion);
                ps.setInt(3, EstadoTurno.LLAMADO.getId());
                ps.setInt(4, EstadoTurno.EN_ATENCION.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public List<Turno> listarPorVentanilla(int idVentanilla, LocalDate fechaOperacion, int limite) {
        String sql = "SELECT " + COLUMNAS + " FROM turno t "
                + "WHERE t.id_ventanilla = ? AND t.fecha_operacion = ? "
                + "ORDER BY t.fecha_hora_llamado DESC, t.id_turno DESC LIMIT ?";
        return EjecutorSQL.consultar(conexion -> {
            List<Turno> turnos = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idVentanilla);
                ps.setObject(2, fechaOperacion);
                ps.setInt(3, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        turnos.add(mapear(rs));
                    }
                }
            }
            return turnos;
        });
    }

    public List<TurnoLlamado> listarUltimosLlamados(LocalDate fechaOperacion, int limite) {
        String sql = "SELECT t.codigo, v.numero, t.fecha_hora_llamado FROM turno t "
                + "JOIN ventanilla v ON v.id_ventanilla = t.id_ventanilla "
                + "WHERE t.fecha_operacion = ? AND t.fecha_hora_llamado IS NOT NULL "
                + "ORDER BY t.fecha_hora_llamado DESC, t.id_turno DESC LIMIT ?";
        return EjecutorSQL.consultar(conexion -> {
            List<TurnoLlamado> llamados = new ArrayList<>();
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setObject(1, fechaOperacion);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        llamados.add(new TurnoLlamado(
                                rs.getString("codigo"),
                                rs.getInt("numero"),
                                rs.getObject("fecha_hora_llamado", LocalDateTime.class)));
                    }
                }
            }
            return llamados;
        });
    }

    private void bloquearServicio(Connection conexion, int idServicio) throws SQLException {
        String sql = "SELECT id_servicio FROM servicio WHERE id_servicio = ? FOR UPDATE";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idServicio);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ReglaNegocioException("El servicio seleccionado no existe.");
                }
            }
        }
    }

    private int siguienteNumero(Connection conexion, int idServicio, LocalDate fechaOperacion) throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 FROM turno WHERE id_servicio = ? AND fecha_operacion = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idServicio);
            ps.setObject(2, fechaOperacion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private long insertar(Connection conexion, int idServicio, LocalDate fechaOperacion, int numero,
                          String codigo, LocalDateTime ahora) throws SQLException {
        String sql = "INSERT INTO turno (id_servicio, fecha_operacion, numero, codigo, id_estado_turno, fecha_hora_generacion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idServicio);
            ps.setObject(2, fechaOperacion);
            ps.setInt(3, numero);
            ps.setString(4, codigo);
            ps.setInt(5, EstadoTurno.EN_ESPERA.getId());
            ps.setObject(6, ahora);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                return claves.getLong(1);
            }
        }
    }

    private void validarVentanillaLibre(Connection conexion, int idVentanilla, LocalDate fechaOperacion) throws SQLException {
        String sqlVentanilla = "SELECT disponible FROM ventanilla WHERE id_ventanilla = ? FOR UPDATE";
        try (PreparedStatement ps = conexion.prepareStatement(sqlVentanilla)) {
            ps.setInt(1, idVentanilla);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ReglaNegocioException("La ventanilla seleccionada no existe.");
                }
                if (!rs.getBoolean("disponible")) {
                    throw new ReglaNegocioException(
                            "La ventanilla no está disponible. Márquela como disponible para llamar turnos.");
                }
            }
        }
        String sqlEnCurso = "SELECT COUNT(*) FROM turno WHERE id_ventanilla = ? AND fecha_operacion = ? "
                + "AND id_estado_turno IN (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sqlEnCurso)) {
            ps.setInt(1, idVentanilla);
            ps.setObject(2, fechaOperacion);
            ps.setInt(3, EstadoTurno.LLAMADO.getId());
            ps.setInt(4, EstadoTurno.EN_ATENCION.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new ReglaNegocioException(
                            "Ya tiene un turno en curso. Finalícelo antes de llamar al siguiente.");
                }
            }
        }
    }

    private Optional<Long> reservarSiguienteEnEspera(Connection conexion, int idVentanilla, LocalDate fechaOperacion)
            throws SQLException {
        String sql = "SELECT t.id_turno FROM turno t "
                + "JOIN ventanilla_servicio vs ON vs.id_servicio = t.id_servicio AND vs.id_ventanilla = ? "
                + "JOIN servicio s ON s.id_servicio = t.id_servicio AND s.activo = 1 "
                + "WHERE t.id_estado_turno = ? AND t.fecha_operacion = ? "
                + "ORDER BY t.fecha_hora_generacion, t.id_turno "
                + "LIMIT 1 FOR UPDATE OF t SKIP LOCKED";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idVentanilla);
            ps.setInt(2, EstadoTurno.EN_ESPERA.getId());
            ps.setObject(3, fechaOperacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }
        }
    }

    private void marcarLlamado(Connection conexion, long idTurno, int idVentanilla, int idUsuario,
                               LocalDateTime ahora) throws SQLException {
        String sql = "UPDATE turno SET id_estado_turno = ?, fecha_hora_llamado = ?, id_ventanilla = ?, id_usuario = ? "
                + "WHERE id_turno = ? AND id_estado_turno = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, EstadoTurno.LLAMADO.getId());
            ps.setObject(2, ahora);
            ps.setInt(3, idVentanilla);
            ps.setInt(4, idUsuario);
            ps.setLong(5, idTurno);
            ps.setInt(6, EstadoTurno.EN_ESPERA.getId());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("El turno reservado cambió de estado durante la transacción");
            }
        }
    }

    private Optional<Turno> transicionar(String sqlUpdate, long idTurno, Object... parametros) {
        return EjecutorSQL.consultar(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(sqlUpdate)) {
                for (int i = 0; i < parametros.length; i++) {
                    ps.setObject(i + 1, parametros[i]);
                }
                if (ps.executeUpdate() == 0) {
                    return Optional.<Turno>empty();
                }
            }
            return leer(conexion, idTurno);
        });
    }

    private Optional<Turno> leer(Connection conexion, long idTurno) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM turno t WHERE t.id_turno = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setLong(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private Turno mapear(ResultSet rs) throws SQLException {
        return new Turno(
                rs.getLong("id_turno"),
                rs.getInt("id_servicio"),
                rs.getObject("fecha_operacion", LocalDate.class),
                rs.getInt("numero"),
                rs.getString("codigo"),
                EstadoTurno.desdeId(rs.getInt("id_estado_turno")),
                rs.getObject("fecha_hora_generacion", LocalDateTime.class),
                rs.getObject("fecha_hora_llamado", LocalDateTime.class),
                rs.getObject("fecha_hora_inicio_atencion", LocalDateTime.class),
                rs.getObject("fecha_hora_finalizacion", LocalDateTime.class),
                enteroONulo(rs, "id_ventanilla"),
                enteroONulo(rs, "id_usuario"));
    }

    private Integer enteroONulo(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }
}
