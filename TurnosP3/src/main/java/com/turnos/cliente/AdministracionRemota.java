package com.turnos.cliente;

import com.turnos.modelo.EstadoVentanilla;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.TablaReporte;
import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;
import com.turnos.protocolo.ServiciosDeVentanilla;
import com.turnos.protocolo.SolicitudReporte;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.protocolo.UsuarioConClave;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Proxy de las operaciones del administrador (usuarios, servicios, ventanillas y reportes). */
public class AdministracionRemota {

    private final ClienteRed cliente = ClienteRed.getInstancia();

    public List<Usuario> listarUsuarios() {
        return cliente.enviar(TipoMensaje.USUARIOS_LISTAR, null);
    }

    public Usuario guardarUsuario(Usuario usuario, String clave) {
        return cliente.enviar(TipoMensaje.USUARIO_GUARDAR, new UsuarioConClave(usuario, clave));
    }

    public List<Servicio> listarServicios() {
        return cliente.enviar(TipoMensaje.SERVICIOS_LISTAR_TODOS, null);
    }

    public Servicio guardarServicio(Servicio servicio) {
        return cliente.enviar(TipoMensaje.SERVICIO_GUARDAR, servicio);
    }

    public List<EstadoVentanilla> estadoVentanillas() {
        return cliente.enviar(TipoMensaje.VENTANILLAS_ESTADO, null);
    }

    public List<Integer> serviciosDeVentanilla(int idVentanilla) {
        return cliente.enviar(TipoMensaje.VENTANILLA_SERVICIOS, idVentanilla);
    }

    /** Guarda la ventanilla y luego los servicios que atiende. */
    public Ventanilla guardarVentanilla(Ventanilla ventanilla, List<Integer> idsServicios) {
        Ventanilla guardada = cliente.enviar(TipoMensaje.VENTANILLA_GUARDAR, ventanilla);
        cliente.enviar(TipoMensaje.VENTANILLA_SERVICIOS_GUARDAR,
                new ServiciosDeVentanilla(guardada.id(), new ArrayList<>(idsServicios)));
        return guardada;
    }

    public TablaReporte consultar(int numeroConsulta, LocalDate desde, LocalDate hasta) {
        return cliente.enviar(TipoMensaje.REPORTE, new SolicitudReporte(numeroConsulta, desde, hasta));
    }
}
