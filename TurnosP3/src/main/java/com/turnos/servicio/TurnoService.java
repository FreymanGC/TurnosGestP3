package com.turnos.servicio;

import com.turnos.dao.ServicioDAO;
import com.turnos.dao.TurnoDAO;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.modelo.TurnoLlamado;
import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Flujo del turno: EN_ESPERA → LLAMADO → EN_ATENCION → FINALIZADO (o LLAMADO → CANCELADO).
 *
 * Concurrencia: en el servidor cada cliente se atiende en un hilo distinto, así que varios hilos
 * pueden llamar a generarTurno o llamarSiguiente al mismo tiempo. Se protege en dos niveles:
 *  1. Java: ReentrantLock justo (fair) → un hilo a la vez en la sección crítica.
 *  2. BD: transacción con FOR UPDATE / SKIP LOCKED en TurnoDAO.
 */
public class TurnoService {

    private final ReentrantLock candadoGeneracion = new ReentrantLock(true);
    private final ReentrantLock candadoAsignacion = new ReentrantLock(true);

    private final TurnoDAO turnoDAO;
    private final ServicioDAO servicioDAO;
    private final NotificadorLlamados notificador;

    public TurnoService(TurnoDAO turnoDAO, ServicioDAO servicioDAO, NotificadorLlamados notificador) {
        this.turnoDAO = turnoDAO;
        this.servicioDAO = servicioDAO;
        this.notificador = notificador;
    }

    public Turno generarTurno(Servicio servicio) {
        if (servicio == null) {
            throw new ReglaNegocioException("Debe seleccionar un servicio.");
        }
        Servicio vigente = servicioDAO.buscarPorId(servicio.id())
                .orElseThrow(() -> new ReglaNegocioException("El servicio seleccionado no existe."));
        if (!vigente.activo()) {
            throw new ReglaNegocioException("El servicio \"" + vigente.nombre() + "\" no está disponible en este momento.");
        }
        candadoGeneracion.lock();
        try {
            return turnoDAO.generar(vigente, LocalDate.now(), LocalDateTime.now());
        } finally {
            candadoGeneracion.unlock();
        }
    }

    public Turno llamarSiguiente(Ventanilla ventanilla, Usuario operador) {
        Turno turno;
        candadoAsignacion.lock();
        try {
            turno = turnoDAO.llamarSiguiente(ventanilla.id(), operador.id(), LocalDate.now(), LocalDateTime.now())
                    .orElseThrow(() -> new ReglaNegocioException("No hay turnos en espera para esta ventanilla."));
        } finally {
            candadoAsignacion.unlock();
        }
        notificar(turno, ventanilla);
        return turno;
    }

    public Turno volverALlamar(Turno turno, Ventanilla ventanilla) {
        Turno actualizado = turnoDAO.registrarNuevoLlamado(turno.id(), LocalDateTime.now())
                .orElseThrow(() -> new ReglaNegocioException("Solo se puede volver a llamar un turno en estado Llamado."));
        notificar(actualizado, ventanilla);
        return actualizado;
    }

    public Turno iniciarAtencion(Turno turno) {
        return turnoDAO.iniciarAtencion(turno.id(), LocalDateTime.now())
                .orElseThrow(() -> new ReglaNegocioException("Solo se puede iniciar la atención de un turno en estado Llamado."));
    }

    public Turno finalizarAtencion(Turno turno) {
        return turnoDAO.finalizarAtencion(turno.id(), LocalDateTime.now())
                .orElseThrow(() -> new ReglaNegocioException("Solo se puede finalizar un turno que está en atención."));
    }

    public Turno marcarAusente(Turno turno) {
        return turnoDAO.marcarAusente(turno.id(), LocalDateTime.now())
                .orElseThrow(() -> new ReglaNegocioException("Solo se puede marcar ausente un turno en estado Llamado."));
    }

    public Optional<Turno> obtenerTurnoActivo(Ventanilla ventanilla) {
        return turnoDAO.buscarActivoDeVentanilla(ventanilla.id(), LocalDate.now());
    }

    public List<Turno> historialDeVentanilla(Ventanilla ventanilla, int limite) {
        return turnoDAO.listarPorVentanilla(ventanilla.id(), LocalDate.now(), limite);
    }

    public List<TurnoLlamado> ultimosLlamados(int limite) {
        return turnoDAO.listarUltimosLlamados(LocalDate.now(), limite);
    }

    private void notificar(Turno turno, Ventanilla ventanilla) {
        notificador.turnoLlamado(new TurnoLlamado(turno.codigo(), ventanilla.numero(), turno.fechaHoraLlamado()));
    }
}
