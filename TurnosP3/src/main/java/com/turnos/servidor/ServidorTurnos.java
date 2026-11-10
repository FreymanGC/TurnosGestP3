package com.turnos.servidor;

import com.turnos.util.ConfiguracionRed;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServidorTurnos implements AutoCloseable {

    static {
        // Formato de una línea para la bitácora: [hora] mensaje (debe ir antes de crear el Logger)
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] %5$s%6$s%n");
    }

    public static final int PUERTO_POR_DEFECTO = 5050;
    private static final int MAXIMO_CLIENTES_SIMULTANEOS = 50;
    private static final Logger LOG = Logger.getLogger(ServidorTurnos.class.getName());

    private final int puerto;
    private final AtomicInteger numeroHilo = new AtomicInteger();
    private final ExecutorService atencionClientes = Executors.newFixedThreadPool(MAXIMO_CLIENTES_SIMULTANEOS, tarea -> {
        Thread hilo = new Thread(tarea, "cliente-" + numeroHilo.incrementAndGet());
        hilo.setDaemon(true);
        return hilo;
    });
    private final CentroNotificaciones notificaciones = new CentroNotificaciones();
    private final RegistroVentanillas registroVentanillas = new RegistroVentanillas();
    private final Set<ManejadorCliente> clientes = ConcurrentHashMap.newKeySet();
    private volatile ServerSocket socketServidor;
    private volatile boolean activo;

    public ServidorTurnos(int puerto) {
        this.puerto = puerto;
    }

    /** Bloquea el hilo que lo invoca hasta que se llame a detener(). */
    public void iniciar() throws IOException {
        ProcesadorSolicitudes procesador = new ProcesadorSolicitudes(notificaciones, registroVentanillas);
        socketServidor = new ServerSocket(puerto);
        activo = true;
        LOG.info("Servidor de turnos escuchando en el puerto " + puerto);
        while (activo) {
            try {
                Socket socket = socketServidor.accept();
                ManejadorCliente[] referencia = new ManejadorCliente[1];
                referencia[0] = new ManejadorCliente(socket, procesador, notificaciones, () -> quitar(referencia[0]));
                clientes.add(referencia[0]);
                atencionClientes.execute(referencia[0]);
                LOG.info("Clientes conectados: " + clientes.size());
            } catch (SocketException e) {
                if (activo) {
                    throw e;
                }
            }
        }
    }

    private void quitar(ManejadorCliente cliente) {
        if (clientes.remove(cliente)) {
            LOG.info("Clientes conectados: " + clientes.size());
        }
    }

    public void detener() {
        activo = false;
        try {
            if (socketServidor != null) {
                socketServidor.close();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error al cerrar el socket del servidor", e);
        }
        clientes.forEach(ManejadorCliente::cerrar);
        atencionClientes.shutdown();
        try {
            if (!atencionClientes.awaitTermination(3, TimeUnit.SECONDS)) {
                atencionClientes.shutdownNow();
            }
        } catch (InterruptedException e) {
            atencionClientes.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        detener();
    }

    /** Run Maven → "1 - Servidor central". La bitácora se ve en la ventana Output de NetBeans. */
    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : ConfiguracionRed.getInstancia().getPuerto();
        ConfiguracionRed config = ConfiguracionRed.getInstancia();
        ServidorWebTurnos web = new ServidorWebTurnos(config.getPuertoWeb());
        try {
            web.iniciar();
            LOG.info("Consulta de turnos por QR disponible en " + config.getUrlConsultaTurno()
                    + "  (si el celular no la abre: misma red Wi-Fi, permitir Java en el firewall,"
                    + " o poner web.host=<IP de esta computadora> en red.properties)");
        } catch (IOException e) {
            LOG.warning("No se pudo iniciar la página web del QR (puerto " + config.getPuertoWeb()
                    + " ocupado): " + e.getMessage());
        }
        try (ServidorTurnos servidor = new ServidorTurnos(puerto)) {
            servidor.iniciar();
        } finally {
            web.detener();
        }
    }
}