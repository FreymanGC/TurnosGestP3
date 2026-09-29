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
}
