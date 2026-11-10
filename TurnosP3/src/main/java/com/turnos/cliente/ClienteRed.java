package com.turnos.cliente;

import com.turnos.excepcion.ConexionServidorException;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.util.ConfiguracionRed;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Conexión de una aplicación cliente con el servidor central (Singleton: una por aplicación).
 *
 * Al conectarse crea un "hilo lector" que escucha el socket todo el tiempo y recibe:
 *  1. Respuestas → despierta al hilo que esperaba esa respuesta (mismo id).
 *  2. Eventos    → avisa a los observadores (por eso la pantalla pública se actualiza sola).
 * Si la conexión se cayó, la siguiente solicitud intenta reconectar automáticamente.
 */
public final class ClienteRed {

    private static final int SEGUNDOS_ESPERA_RESPUESTA = 15;
    private static final int MILISEGUNDOS_CONEXION = 3000;
    private static final ClienteRed INSTANCIA = new ClienteRed();

    private final AtomicInteger contador = new AtomicInteger();
    private final Map<Integer, CompletableFuture<Mensaje>> pendientes = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ObservadorServidor> observadores = new CopyOnWriteArrayList<>();

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private volatile boolean conectado;

    private ClienteRed() {
    }

    public static ClienteRed getInstancia() {
        return INSTANCIA;
    }

    public boolean estaConectado() {
        return conectado;
    }

    public synchronized void conectar() {
        if (conectado) {
            return;
        }
        ConfiguracionRed config = ConfiguracionRed.getInstancia();
        try {
            Socket nuevo = new Socket();
            nuevo.connect(new InetSocketAddress(config.getHost(), config.getPuerto()), MILISEGUNDOS_CONEXION);
            ObjectOutputStream out = new ObjectOutputStream(nuevo.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(nuevo.getInputStream());
            socket = nuevo;
            salida = out;
            entrada = in;
            conectado = true;
            Thread lector = new Thread(this::escucharServidor, "hilo-lector-servidor");
            lector.setDaemon(true);
            lector.start();
        } catch (IOException e) {
            throw new ConexionServidorException("No se pudo conectar con el servidor de turnos ("
                    + config.getHost() + ":" + config.getPuerto() + "). Verifique que esté iniciado.", e);
        }
    }

    /**
     * Envía una solicitud y espera la respuesta.
     * @throws ReglaNegocioException     si el servidor respondió con un error (mensaje para el usuario)
     * @throws ConexionServidorException si no hay comunicación con el servidor
     */
    @SuppressWarnings("unchecked")
    public <T> T enviar(TipoMensaje tipo, Serializable datos) {
        conectar();
        int id = contador.incrementAndGet();
        CompletableFuture<Mensaje> futura = new CompletableFuture<>();
        pendientes.put(id, futura);
        try {
            escribir(Mensaje.solicitud(id, tipo, datos));
            Mensaje respuesta = futura.get(SEGUNDOS_ESPERA_RESPUESTA, TimeUnit.SECONDS);
            if (!respuesta.isExito()) {
                throw new ReglaNegocioException(respuesta.getTexto());
            }
            return (T) respuesta.getDatos();
        } catch (TimeoutException e) {
            throw new ConexionServidorException("El servidor no respondió a tiempo. Intente de nuevo.", e);
        } catch (ExecutionException e) {
            throw new ConexionServidorException("Se perdió la conexión con el servidor.", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConexionServidorException("La operación fue interrumpida.", e);
        } finally {
            pendientes.remove(id);
        }
    }

    /** Solo un hilo a la vez escribe en el socket. */
    private synchronized void escribir(Mensaje mensaje) {
        if (!conectado) {
            throw new ConexionServidorException("No hay conexión con el servidor.");
        }
        try {
            salida.writeObject(mensaje);
            salida.flush();
            salida.reset();
        } catch (IOException e) {
            perderConexion();
            throw new ConexionServidorException("Se perdió la conexión con el servidor.", e);
        }
    }

    /** Código del hilo lector. */
    private void escucharServidor() {
        try {
            while (conectado) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                if (mensaje.getTipo() == TipoMensaje.RESPUESTA) {
                    CompletableFuture<Mensaje> futura = pendientes.get(mensaje.getId());
                    if (futura != null) {
                        futura.complete(mensaje);
                    }
                } else if (mensaje.getTipo().esEvento()) {
                    for (ObservadorServidor observador : observadores) {
                        try {
                            observador.alRecibirEvento(mensaje);
                        } catch (RuntimeException e) {
                            System.err.println("Error en un observador: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            perderConexion();
        }
    }

    private void perderConexion() {
        boolean estaba;
        synchronized (this) {
            estaba = conectado;
            conectado = false;
            cerrarSocket();
        }
        ConexionServidorException error = new ConexionServidorException("Se perdió la conexión con el servidor.");
        pendientes.values().forEach(f -> f.completeExceptionally(error));
        if (estaba) {
            observadores.forEach(ObservadorServidor::alPerderConexion);
        }
    }

    public synchronized void desconectar() {
        conectado = false;
        cerrarSocket();
    }

    private void cerrarSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // ya estaba cerrado
        }
    }

    public void agregarObservador(ObservadorServidor observador) {
        observadores.addIfAbsent(observador);
    }

    public void quitarObservador(ObservadorServidor observador) {
        observadores.remove(observador);
    }
}
