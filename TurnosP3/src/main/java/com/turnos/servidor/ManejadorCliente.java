package com.turnos.servidor;

import com.turnos.protocolo.Mensaje;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Atiende a UN cliente en su propio hilo del pool. Recibe objetos Mensaje, los entrega al
 * ProcesadorSolicitudes y devuelve la respuesta. También es observador del CentroNotificaciones:
 * reenvía los eventos (turno llamado, etc.) por este mismo socket.
 */
class ManejadorCliente implements Runnable, ObservadorEventos {

    private static final Logger LOG = Logger.getLogger(ManejadorCliente.class.getName());

    private final Socket socket;
    private final ProcesadorSolicitudes procesador;
    private final CentroNotificaciones notificaciones;
    private final Runnable alDesconectar;
    private final SesionCliente sesion = new SesionCliente();

    private ObjectOutputStream salida;
    private volatile boolean activo = true;

    ManejadorCliente(Socket socket, ProcesadorSolicitudes procesador, CentroNotificaciones notificaciones,
                     Runnable alDesconectar) {
        this.socket = socket;
        this.procesador = procesador;
        this.notificaciones = notificaciones;
        this.alDesconectar = alDesconectar;
    }

    @Override
    public void run() {
        String origen = String.valueOf(socket.getRemoteSocketAddress());
        try {
            // Primero la salida (con flush) y después la entrada; si ambos lados abren primero la
            // entrada, se quedan esperando el encabezado del otro para siempre.
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            notificaciones.suscribir(this);
            LOG.info("[" + Thread.currentThread().getName() + "] Cliente conectado desde " + origen);

            while (activo) {
                Mensaje solicitud = (Mensaje) entrada.readObject();
                enviar(procesador.procesar(solicitud, sesion));
            }
        } catch (EOFException | SocketException e) {
            // el cliente cerró la aplicación o se perdió la red
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.WARNING, "Conexión con el cliente interrumpida", e);
        } finally {
            cerrar();
            LOG.info("[" + Thread.currentThread().getName() + "] Cliente desconectado: " + origen);
        }
    }

    @Override
    public void notificar(Mensaje evento) {
        enviar(evento);
    }

    /**
     * synchronized: la respuesta (hilo de este cliente) y los eventos (hilos de OTROS clientes)
     * pueden escribir a la vez en el mismo socket; sin esto los bytes se mezclarían.
     */
    private synchronized void enviar(Mensaje mensaje) {
        if (!activo || salida == null) {
            return;
        }
        try {
            salida.writeObject(mensaje);
            salida.flush();
            salida.reset();
        } catch (IOException e) {
            activo = false;
            cerrarSocket();
        }
    }

    void cerrar() {
        activo = false;
        notificaciones.desuscribir(this);
        procesador.cerrarSesion(sesion);
        cerrarSocket();
        alDesconectar.run();
    }

    private void cerrarSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            // ya estaba cerrado
        }
    }
}
