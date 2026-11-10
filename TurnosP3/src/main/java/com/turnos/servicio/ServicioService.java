package com.turnos.servicio;

import com.turnos.dao.ServicioDAO;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.Servicio;

import java.util.List;

public class ServicioService {

    private final ServicioDAO servicioDAO;

    public ServicioService() {
        this(new ServicioDAO());
    }

    public ServicioService(ServicioDAO servicioDAO) {
        this.servicioDAO = servicioDAO;
    }

    public List<Servicio> listarActivos() {
        List<Servicio> servicios = servicioDAO.listarActivos();
        if (servicios.isEmpty()) {
            throw new ReglaNegocioException("No hay servicios activos configurados.");
        }
        return servicios;
    }

    public List<Servicio> listarTodos() {
        return servicioDAO.listarTodos();
    }

    /** Crea (id = 0) o actualiza un servicio. */
    public Servicio guardar(Servicio servicio) {
        String prefijo = servicio.prefijo() == null ? "" : servicio.prefijo().trim().toUpperCase();
        String nombre = servicio.nombre() == null ? "" : servicio.nombre().trim();
        if (!prefijo.matches("[A-Z]")) {
            throw new ReglaNegocioException("El prefijo debe ser una sola letra (A-Z).");
        }
        if (nombre.isEmpty() || nombre.length() > 60) {
            throw new ReglaNegocioException("Digite el nombre del servicio (máximo 60 caracteres).");
        }
        if (servicio.prioridad() < 0 || servicio.prioridad() > 9) {
            throw new ReglaNegocioException("La prioridad debe estar entre 0 (normal) y 9.");
        }
        if (servicioDAO.existe("prefijo", prefijo, servicio.id())) {
            throw new ReglaNegocioException("Ya existe un servicio con el prefijo " + prefijo + ".");
        }
        if (servicioDAO.existe("nombre", nombre, servicio.id())) {
            throw new ReglaNegocioException("Ya existe un servicio llamado \"" + nombre + "\".");
        }
        Servicio limpio = new Servicio(servicio.id(), prefijo, nombre, servicio.prioridad(), servicio.activo());
        if (servicio.id() == 0) {
            return new Servicio(servicioDAO.insertar(limpio), prefijo, nombre, servicio.prioridad(), servicio.activo());
        }
        servicioDAO.actualizar(limpio);
        return limpio;
    }
}
