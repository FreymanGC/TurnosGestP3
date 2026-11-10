package com.turnos.cliente;

import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.modelo.TurnoLlamado;
import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;
import com.turnos.protocolo.TipoMensaje;
import java.util.List;
import java.util.Optional;


public class TurnoRemoto {

    private final ClienteRed cliente = ClienteRed.getInstancia();

    public Turno generarTurno(Servicio servicio) {
        if (servicio == null) {
            throw new ReglaNegocioException("Debe seleccionar un servicio.");
        }
        return cliente.enviar(TipoMensaje.GENERAR_TURNO, servicio);
    }

    public Turno llamarSiguiente(Ventanilla ventanilla, Usuario operador) {
        return cliente.enviar(TipoMensaje.LLAMAR_SIGUIENTE, null);
    }

    public Turno volverALlamar(Turno turno, Ventanilla ventanilla) {
        return cliente.enviar(TipoMensaje.VOLVER_A_LLAMAR, null);
    }

    public Turno iniciarAtencion(Turno turno) {
        return cliente.enviar(TipoMensaje.INICIAR_ATENCION, null);
    }

    public Turno finalizarAtencion(Turno turno) {
        return cliente.enviar(TipoMensaje.FINALIZAR_ATENCION, null);
    }

    public Turno marcarAusente(Turno turno) {
        return cliente.enviar(TipoMensaje.MARCAR_AUSENTE, null);
    }

    public Optional<Turno> obtenerTurnoActivo(Ventanilla ventanilla) {
        Turno turno = cliente.enviar(TipoMensaje.TURNO_ACTIVO, null);
        return Optional.ofNullable(turno);
    }

    public List<Turno> historialDeVentanilla(Ventanilla ventanilla, int limite) {
        return cliente.enviar(TipoMensaje.HISTORIAL_VENTANILLA, limite);
    }

    /** Dirección base de la página del QR (la da el servidor, que conoce su IP en la red). */
    public String urlConsultaTurno() {
        return cliente.enviar(TipoMensaje.URL_CONSULTA_TURNO, null);
    }

    public List<TurnoLlamado> ultimosLlamados(int limite) {
        return cliente.enviar(TipoMensaje.ULTIMOS_LLAMADOS, limite);
    }
}