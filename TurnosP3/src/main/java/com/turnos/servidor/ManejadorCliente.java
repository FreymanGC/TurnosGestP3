package com.turnos.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Atiende a UN cliente en su propio hilo. Protocolo de líneas de texto. Por ahora solo responde
 * PING → PONG y SALIR → ADIOS para poder verificar la conexión y el manejo de varios clientes.
 * PENDIENTE (segundo avance): comandos de generar turno, llamar siguiente y difusión a pantallas,
 * delegando en los servicios existentes (nunca SQL aquí).
 */
class ManejadorCliente implements Runnable {

    private static final Logger LOG = Logger.getLogger(ManejadorCliente.class.getName());

    private final Socket socket;

    ManejadorCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if ("SALIR".equalsIgnoreCase(linea.trim())) {
                    salida.println("ADIOS");
                    return;
                }
                salida.println(responder(linea.trim()));
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Conexión con el cliente interrumpida", e);
        }
    }

    private String responder(String comando) {
        if ("PING".equalsIgnoreCase(comando)) {
            return "PONG";
        }
        return "ERROR Comando no soportado todavía";
    }
}
