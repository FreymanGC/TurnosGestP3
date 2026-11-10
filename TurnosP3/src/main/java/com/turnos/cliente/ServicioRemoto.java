package com.turnos.cliente;

import com.turnos.modelo.Servicio;
import com.turnos.protocolo.TipoMensaje;
import java.util.List;
public class ServicioRemoto {

    private final ClienteRed cliente = ClienteRed.getInstancia();

    public List<Servicio> listarActivos() {
        return cliente.enviar(TipoMensaje.LISTAR_SERVICIOS_ACTIVOS, null);
    }
}
