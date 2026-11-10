package com.turnos.servidor;

import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.excepcion.TurnosException;
import com.turnos.modelo.EstadoVentanilla;
import com.turnos.modelo.Rol;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;
import com.turnos.protocolo.DatosLogin;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.ServiciosDeVentanilla;
import com.turnos.protocolo.SolicitudReporte;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.protocolo.UsuarioConClave;
import com.turnos.servicio.AutenticacionService;
import com.turnos.servicio.ReporteService;
import com.turnos.servicio.ServicioService;
import com.turnos.servicio.TurnoService;
import com.turnos.servicio.UsuarioService;
import com.turnos.servicio.VentanillaService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class ProcesadorSolicitudes {

    private static final Logger LOG = Logger.getLogger(ProcesadorSolicitudes.class.getName());

    private final TurnoService turnoService;
    private final ServicioService servicioService;
    private final VentanillaService ventanillaService;
    private final AutenticacionService autenticacionService;
    private final UsuarioService usuarioService = new UsuarioService();
    private final ReporteService reporteService = new ReporteService();
    private final CentroNotificaciones notificaciones;
    private final RegistroVentanillas registroVentanillas;

    ProcesadorSolicitudes(CentroNotificaciones notificaciones, RegistroVentanillas registroVentanillas) {
        this.notificaciones = notificaciones;
        this.registroVentanillas = registroVentanillas;
        this.turnoService = new TurnoService(new com.turnos.dao.TurnoDAO(), new com.turnos.dao.ServicioDAO(),
                new NotificadorRed(notificaciones));
        this.servicioService = new ServicioService();
        this.ventanillaService = new VentanillaService();
        this.autenticacionService = new AutenticacionService();
    }

    /** Siempre devuelve una respuesta: de éxito o con el mensaje de error para el usuario. */
    Mensaje procesar(Mensaje solicitud, SesionCliente sesion) {
        try {
            return Mensaje.respuestaOk(solicitud.getId(), atender(solicitud, sesion));
        } catch (TurnosException e) {
            return Mensaje.respuestaError(solicitud.getId(), e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Error inesperado procesando " + solicitud.getTipo(), e);
            return Mensaje.respuestaError(solicitud.getId(), "Ocurrió un error inesperado en el servidor. Intente de nuevo.");
        }
    }

    private Serializable atender(Mensaje m, SesionCliente sesion) {
        switch (m.getTipo()) {
            case PING:
                return "PONG";
            case LISTAR_SERVICIOS_ACTIVOS:
                return new ArrayList<>(servicioService.listarActivos());
            case GENERAR_TURNO: {
                Turno turno = turnoService.generarTurno((Servicio) m.getDatos());
                registrar("Generadora: nuevo turno " + turno.codigo());
                return turno;
            }
            case URL_CONSULTA_TURNO:
                return com.turnos.util.ConfiguracionRed.getInstancia().getUrlConsultaTurno();
            case ULTIMOS_LLAMADOS:
                return new ArrayList<>(turnoService.ultimosLlamados((Integer) m.getDatos()));

            case LOGIN: {
                DatosLogin datos = (DatosLogin) m.getDatos();
                Usuario usuario = autenticacionService.autenticar(datos.nombreUsuario(), datos.clave(), datos.rolRequerido());
                cerrarSesion(sesion);
                sesion.iniciar(usuario.sinClave());
                registrar("Inició sesión " + sesion.descripcion());
                return usuario.sinClave();
            }
            case LOGOUT:
                cerrarSesion(sesion);
                return null;

            case LISTAR_VENTANILLAS:
                exigirSesion(sesion);
                return new ArrayList<>(ventanillaService.listar());
            case SELECCIONAR_VENTANILLA:
                return seleccionarVentanilla((Ventanilla) m.getDatos(), sesion);
            case TURNO_ACTIVO:
                return turnoService.obtenerTurnoActivo(exigirVentanilla(sesion)).orElse(null);
            case HISTORIAL_VENTANILLA:
                return new ArrayList<>(turnoService.historialDeVentanilla(exigirVentanilla(sesion), (Integer) m.getDatos()));
            case LLAMAR_SIGUIENTE: {
                Turno turno = turnoService.llamarSiguiente(exigirVentanilla(sesion), sesion.getUsuario());
                registrar(sesion.descripcion() + " llamó el turno " + turno.codigo());
                return turno;
            }
            case VOLVER_A_LLAMAR: {
                Ventanilla ventanilla = exigirVentanilla(sesion);
                Turno turno = turnoService.volverALlamar(turnoEnCurso(ventanilla), ventanilla);
                registrar(sesion.descripcion() + " volvió a llamar " + turno.codigo());
                return turno;
            }
            case INICIAR_ATENCION: {
                Turno turno = turnoService.iniciarAtencion(turnoEnCurso(exigirVentanilla(sesion)));
                registrar(sesion.descripcion() + " inició la atención de " + turno.codigo());
                return turno;
            }
            case FINALIZAR_ATENCION: {
                Turno turno = turnoService.finalizarAtencion(turnoEnCurso(exigirVentanilla(sesion)));
                registrar(sesion.descripcion() + " finalizó la atención de " + turno.codigo());
                return turno;
            }
            case MARCAR_AUSENTE: {
                Turno turno = turnoService.marcarAusente(turnoEnCurso(exigirVentanilla(sesion)));
                registrar(sesion.descripcion() + " marcó ausente " + turno.codigo());
                return turno;
            }
            case CAMBIAR_DISPONIBILIDAD: {
                Ventanilla actualizada = ventanillaService.cambiarDisponibilidad(exigirVentanilla(sesion), (Boolean) m.getDatos());
                sesion.asignarVentanilla(actualizada);
                registrar(sesion.descripcion() + (actualizada.disponible() ? " abrió" : " cerró") + " la ventanilla");
                notificaciones.publicar(TipoMensaje.EVENTO_VENTANILLAS_ACTUALIZADAS, null);
                return actualizada;
            }
            // ------------------------------------------------------------ administrador
            case USUARIOS_LISTAR:
                exigirAdministrador(sesion);
                return new ArrayList<>(usuarioService.listar());
            case USUARIO_GUARDAR: {
                exigirAdministrador(sesion);
                UsuarioConClave datos = (UsuarioConClave) m.getDatos();
                Usuario guardado = usuarioService.guardar(datos.usuario(), datos.clave(), sesion.getUsuario());
                registrar(sesion.descripcion() + " guardó el usuario " + guardado.nombreUsuario());
                return guardado;
            }
            case SERVICIOS_LISTAR_TODOS:
                exigirAdministrador(sesion);
                return new ArrayList<>(servicioService.listarTodos());
            case SERVICIO_GUARDAR: {
                exigirAdministrador(sesion);
                Servicio guardado = servicioService.guardar((Servicio) m.getDatos());
                registrar(sesion.descripcion() + " guardó el servicio " + guardado.prefijo());
                notificaciones.publicar(TipoMensaje.EVENTO_SERVICIOS_ACTUALIZADOS, null);
                return guardado;
            }
            case VENTANILLAS_ESTADO: {
                exigirAdministrador(sesion);
                ArrayList<EstadoVentanilla> estados = new ArrayList<>();
                for (Ventanilla v : ventanillaService.listar()) {
                    estados.add(new EstadoVentanilla(v, registroVentanillas.operadorEn(v.id()).orElse("—"),
                            ventanillaService.descripcionTurnoEnCurso(v)));
                }
                return estados;
            }
            case VENTANILLA_GUARDAR: {
                exigirAdministrador(sesion);
                Ventanilla guardada = ventanillaService.guardar((Ventanilla) m.getDatos());
                registrar(sesion.descripcion() + " guardó la " + guardada.etiqueta());
                notificaciones.publicar(TipoMensaje.EVENTO_VENTANILLAS_ACTUALIZADAS, null);
                return guardada;
            }
            case VENTANILLA_SERVICIOS:
                exigirAdministrador(sesion);
                return new ArrayList<>(ventanillaService.serviciosDe((Integer) m.getDatos()));
            case VENTANILLA_SERVICIOS_GUARDAR: {
                exigirAdministrador(sesion);
                ServiciosDeVentanilla datos = (ServiciosDeVentanilla) m.getDatos();
                ventanillaService.asignarServicios(datos.idVentanilla(), datos.idsServicios());
                registrar(sesion.descripcion() + " cambió los servicios de la ventanilla " + datos.idVentanilla());
                return null;
            }
            case REPORTE: {
                exigirAdministrador(sesion);
                SolicitudReporte solicitud = (SolicitudReporte) m.getDatos();
                return reporteService.generar(solicitud.numeroConsulta(), solicitud.desde(), solicitud.hasta());
            }
            default:
                throw new ReglaNegocioException("Solicitud no soportada: " + m.getTipo());
        }
    }

    private Ventanilla seleccionarVentanilla(Ventanilla elegida, SesionCliente sesion) {
        Usuario usuario = exigirSesion(sesion);
        if (usuario.rol() != Rol.OPERADOR) {
            throw new ReglaNegocioException("Solo los operadores pueden atender en una ventanilla.");
        }
        Ventanilla ventanilla = ventanillaService.refrescar(elegida);
        Ventanilla anterior = sesion.getVentanilla();
        if (anterior != null && anterior.id() != ventanilla.id()) {
            registroVentanillas.liberar(anterior.id(), sesion);
        }
        if (!registroVentanillas.ocupar(ventanilla.id(), sesion)) {
            String quien = registroVentanillas.operadorEn(ventanilla.id()).orElse("otro operador");
            throw new ReglaNegocioException("La " + ventanilla.etiqueta() + " ya está siendo usada por " + quien + ".");
        }
        sesion.asignarVentanilla(ventanilla);
        registrar(sesion.descripcion() + " ocupó la ventanilla");
        notificaciones.publicar(TipoMensaje.EVENTO_VENTANILLAS_ACTUALIZADAS, null);
        return ventanilla;
    }

    /** Cierra la sesión y libera la ventanilla. También se usa cuando el cliente se desconecta. */
    void cerrarSesion(SesionCliente sesion) {
        if (sesion.getUsuario() == null) {
            return;
        }
        String descripcion = sesion.descripcion();
        Ventanilla ventanilla = sesion.getVentanilla();
        if (ventanilla != null) {
            registroVentanillas.liberar(ventanilla.id(), sesion);
        }
        sesion.cerrar();
        registrar("Cerró sesión " + descripcion);
        if (ventanilla != null) {
            notificaciones.publicar(TipoMensaje.EVENTO_VENTANILLAS_ACTUALIZADAS, null);
        }
    }

    private Usuario exigirSesion(SesionCliente sesion) {
        if (sesion.getUsuario() == null) {
            throw new ReglaNegocioException("Debe iniciar sesión para realizar esta operación.");
        }
        return sesion.getUsuario();
    }

    private void exigirAdministrador(SesionCliente sesion) {
        if (exigirSesion(sesion).rol() != Rol.ADMIN) {
            throw new ReglaNegocioException("Solo el administrador puede realizar esta operación.");
        }
    }

    private Ventanilla exigirVentanilla(SesionCliente sesion) {
        exigirSesion(sesion);
        if (sesion.getVentanilla() == null) {
            throw new ReglaNegocioException("Seleccione su ventanilla para realizar esta operación.");
        }
        return sesion.getVentanilla();
    }

    private Turno turnoEnCurso(Ventanilla ventanilla) {
        return turnoService.obtenerTurnoActivo(ventanilla)
                .orElseThrow(() -> new ReglaNegocioException("La ventanilla no tiene un turno en curso."));
    }

    private void registrar(String texto) {
        LOG.info("[" + Thread.currentThread().getName() + "] " + texto);
    }
}