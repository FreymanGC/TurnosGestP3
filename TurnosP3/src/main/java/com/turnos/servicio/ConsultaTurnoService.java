package com.turnos.servicio;

import com.turnos.dao.ServicioDAO;
import com.turnos.dao.TurnoDAO;
import com.turnos.dao.VentanillaDAO;
import com.turnos.modelo.ConsultaTurno;
import com.turnos.modelo.EstadoTurno;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.modelo.Ventanilla;

import java.util.Optional;


public class ConsultaTurnoService {

    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();
    private final VentanillaDAO ventanillaDAO = new VentanillaDAO();

    public Optional<ConsultaTurno> consultar(long idTurno, String codigo) {
        Optional<Turno> encontrado = turnoDAO.buscarPorId(idTurno);
        if (encontrado.isEmpty() || !encontrado.get().codigo().equalsIgnoreCase(codigo)) {
            return Optional.empty();
        }
        Turno turno = encontrado.get();
        String servicio = servicioDAO.buscarPorId(turno.idServicio()).map(Servicio::nombre).orElse("");
        Integer ventanilla = turno.idVentanilla() == null ? null
                : ventanillaDAO.buscarPorId(turno.idVentanilla()).map(Ventanilla::numero).orElse(null);
        int antes = turno.estado() == EstadoTurno.EN_ESPERA ? turnoDAO.contarEnEsperaAntes(turno) : 0;
        return Optional.of(new ConsultaTurno(turno.codigo(), servicio, turno.estado(), ventanilla, antes,
                turno.fechaHoraGeneracion(), turno.fechaHoraLlamado()));
    }
}