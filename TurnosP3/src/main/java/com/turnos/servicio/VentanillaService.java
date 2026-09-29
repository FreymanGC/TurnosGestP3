package com.turnos.servicio;

import com.turnos.dao.TurnoDAO;
import com.turnos.dao.VentanillaDAO;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.Ventanilla;

import java.time.LocalDate;
import java.util.List;

public class VentanillaService {

    private final VentanillaDAO ventanillaDAO;
    private final TurnoDAO turnoDAO;

    public VentanillaService() {
        this(new VentanillaDAO(), new TurnoDAO());
    }

    public VentanillaService(VentanillaDAO ventanillaDAO, TurnoDAO turnoDAO) {
        this.ventanillaDAO = ventanillaDAO;
        this.turnoDAO = turnoDAO;
    }

    public List<Ventanilla> listar() {
        List<Ventanilla> ventanillas = ventanillaDAO.listar();
        if (ventanillas.isEmpty()) {
            throw new ReglaNegocioException("No hay ventanillas configuradas.");
        }
        return ventanillas;
    }

    public Ventanilla refrescar(Ventanilla ventanilla) {
        return ventanillaDAO.buscarPorId(ventanilla.id())
                .orElseThrow(() -> new ReglaNegocioException("La ventanilla ya no existe."));
    }

    public Ventanilla cambiarDisponibilidad(Ventanilla ventanilla, boolean disponible) {
        if (!disponible && turnoDAO.buscarActivoDeVentanilla(ventanilla.id(), LocalDate.now()).isPresent()) {
            throw new ReglaNegocioException("No puede marcar la ventanilla como no disponible con un turno en curso.");
        }
        ventanillaDAO.actualizarDisponibilidad(ventanilla.id(), disponible);
        return refrescar(ventanilla);
    }
}
