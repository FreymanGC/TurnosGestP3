package com.turnos.dao;

import com.turnos.excepcion.PersistenciaException;
import com.turnos.util.ConexionBD;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Único lugar donde los DAO abren conexiones y manejan transacciones. Traduce las SQLException
 * a PersistenciaException con mensajes aptos para el usuario (el detalle técnico va al log).
 */
final class EjecutorSQL {

    private static final Logger LOG = Logger.getLogger(EjecutorSQL.class.getName());

    private static final int ERROR_LOCK_WAIT_TIMEOUT = 1205;
    private static final int ERROR_DEADLOCK = 1213;

    @FunctionalInterface
    interface Trabajo<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private EjecutorSQL() {
    }

    static <T> T consultar(Trabajo<T> trabajo) {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return trabajo.ejecutar(conexion);
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    /**
     * READ COMMITTED evita bloqueos de intervalo (gap locks) entre operadores y generadoras que
     * trabajan sobre la misma tabla turno; los bloqueos de fila se piden explícitamente en cada DAO.
     */
    static <T> T enTransaccion(Trabajo<T> trabajo) {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            conexion.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                T resultado = trabajo.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                revertir(conexion);
                throw e;
            }
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    private static void revertir(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "No se pudo revertir la transacción", e);
        }
    }

    private static PersistenciaException traducir(SQLException e) {
        LOG.log(Level.SEVERE, "Error de BD (SQLState=" + e.getSQLState() + ", código=" + e.getErrorCode() + ")", e);
        String estado = e.getSQLState();
        if (e instanceof SQLTransientConnectionException || e instanceof SQLNonTransientConnectionException
                || (estado != null && estado.startsWith("08"))) {
            return new PersistenciaException(
                    "No se pudo comunicar con la base de datos. Verifique su conexión e intente de nuevo.", e);
        }
        if (e instanceof SQLTimeoutException) {
            return new PersistenciaException("La base de datos tardó demasiado en responder. Intente de nuevo.", e);
        }
        if (e.getErrorCode() == ERROR_LOCK_WAIT_TIMEOUT || e.getErrorCode() == ERROR_DEADLOCK) {
            return new PersistenciaException("El sistema está ocupado en este momento. Intente de nuevo.", e);
        }
        return new PersistenciaException("Ocurrió un error al acceder a los datos. Intente de nuevo.", e);
    }
}
