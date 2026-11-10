package com.turnos.cliente;

import com.turnos.modelo.Ventanilla;
import com.turnos.protocolo.TipoMensaje;
import java.util.List;

public class VentanillaRemota {

    private final ClienteRed cliente = ClienteRed.getInstancia();

    public List<Ventanilla> listar() {
        return cliente.enviar(TipoMensaje.LISTAR_VENTANILLAS, null);
    }

    /** Reserva la ventanilla para este operador; falla si otro operador ya la está usando. */
    public Ventanilla ocupar(Ventanilla ventanilla) {
        return cliente.enviar(TipoMensaje.SELECCIONAR_VENTANILLA, ventanilla);
    }

    public Ventanilla cambiarDisponibilidad(Ventanilla ventanilla, boolean disponible) {
        return cliente.enviar(TipoMensaje.CAMBIAR_DISPONIBILIDAD, disponible);
    }
}
