package com.turnos.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PREPARADO, NO COMPLETO: infraestructura del servidor central. Acepta conexiones y atiende cada
 * cliente en un hilo del pool; el protocolo de turnos (generar, llamar, notificar a pantallas)
 * todavía NO existe. Ver ManejadorCliente.
 *
 * ServerSocket + ExecutorService de tamaño fijo: un hilo bloqueado por cliente es simple y
 * suficiente para pocas aplicaciones conectadas (generadoras, operadores, pantallas), y el
 * límite del pool evita crear hilos sin control, a diferencia de un hilo nuevo por conexión.
 */
public class ServidorTurnos implements AutoCloseable {

    public static final int PUERTO_POR_DEFECTO = 5050;
    private static final int MAXIMO_CLIENTES_SIMULTANEOS = 20;
    private static final Logger LOG = Logger.getLogger(ServidorTurnos.class.getName());

    private final int puerto;
    private final ExecutorService atencionClientes = Executors.newFixedThreadPool(MAXIMO_CLIENTES_SIMULTANEOS);
    private volatile ServerSocket socketServidor;
    private volatile boolean activo;

    public ServidorTurnos(int puerto) {
        this.puerto = puerto;
    }

    /** Bloquea el hilo que lo invoca hasta que se llame a detener(). */
    public void iniciar() throws IOException {
        socketServidor = new ServerSocket(puerto);
        activo = true;
        LOG.info("Servidor de turnos escuchando en el puerto " + puerto);
        while (activo) {
            try {
                Socket cliente = socketServidor.accept();
                atencionClientes.execute(new ManejadorCliente(cliente));
            } catch (SocketException e) {
                if (activo) {
                    throw e;
                }
            }
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

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO_POR_DEFECTO;
        try (ServidorTurnos servidor = new ServidorTurnos(puerto)) {
            servidor.iniciar();
        }
    }
}
