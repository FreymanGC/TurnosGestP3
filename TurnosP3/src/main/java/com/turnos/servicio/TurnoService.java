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

/** Flujo del turno: EN_ESPERA → LLAMADO → EN_ATENCION → FINALIZADO. */
public class TurnoService {

    private final TurnoDAO turnoDAO;
    private final ServicioDAO servicioDAO;
    private final NotificadorLlamados notificador;

    public TurnoService() {
        this(new TurnoDAO(), new ServicioDAO(), new NotificadorLocal());
    }

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
        return turnoDAO.generar(vigente, LocalDate.now(), LocalDateTime.now());
    }

    public Turno llamarSiguiente(Ventanilla ventanilla, Usuario operador) {
        Turno turno = turnoDAO.llamarSiguiente(ventanilla.id(), operador.id(), LocalDate.now(), LocalDateTime.now())
                .orElseThrow(() -> new ReglaNegocioException("No hay turnos en espera para esta ventanilla."));
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
