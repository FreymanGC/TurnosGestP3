package com.turnos.servicio;

import com.turnos.dao.ServicioDAO;
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

    /** Crea (id = 0) o actualiza una ventanilla. */
    public Ventanilla guardar(Ventanilla ventanilla) {
        if (ventanilla.numero() < 1 || ventanilla.numero() > 999) {
            throw new ReglaNegocioException("El número de ventanilla debe estar entre 1 y 999.");
        }
        String nombre = (ventanilla.nombre() == null || ventanilla.nombre().isBlank())
                ? "Ventanilla " + ventanilla.numero() : ventanilla.nombre().trim();
        if (nombre.length() > 40) {
            throw new ReglaNegocioException("El nombre de la ventanilla no puede superar 40 caracteres.");
        }
        if (ventanillaDAO.existeNumero(ventanilla.numero(), ventanilla.id())) {
            throw new ReglaNegocioException("Ya existe la ventanilla número " + ventanilla.numero() + ".");
        }
        Ventanilla limpia = new Ventanilla(ventanilla.id(), ventanilla.numero(), nombre, ventanilla.disponible());
        if (ventanilla.id() == 0) {
            return new Ventanilla(ventanillaDAO.insertar(limpia), limpia.numero(), nombre, limpia.disponible());
        }
        ventanillaDAO.actualizar(limpia);
        return limpia;
    }

    public List<Integer> serviciosDe(int idVentanilla) {
        return ventanillaDAO.listarIdsServicios(idVentanilla);
    }

    /** Configura qué servicios puede atender la ventanilla (al menos uno). */
    public void asignarServicios(int idVentanilla, List<Integer> idsServicios) {
        if (idsServicios == null || idsServicios.isEmpty()) {
            throw new ReglaNegocioException("La ventanilla debe atender al menos un servicio.");
        }
        ServicioDAO servicioDAO = new ServicioDAO();
        for (Integer idServicio : idsServicios) {
            if (servicioDAO.buscarPorId(idServicio).isEmpty()) {
                throw new ReglaNegocioException("Uno de los servicios seleccionados no existe.");
            }
        }
        ventanillaDAO.reemplazarServicios(idVentanilla, idsServicios);
    }

    /** Código y estado del turno en curso de la ventanilla, para el panel del administrador. */
    public String descripcionTurnoEnCurso(Ventanilla ventanilla) {
        return turnoDAO.buscarActivoDeVentanilla(ventanilla.id(), LocalDate.now())
                .map(t -> t.codigo() + " (" + t.estado().getNombre() + ")")
                .orElse("—");
    }
}
