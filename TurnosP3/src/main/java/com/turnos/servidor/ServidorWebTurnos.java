package com.turnos.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.turnos.excepcion.TurnosException;
import com.turnos.modelo.ConsultaTurno;
import com.turnos.servicio.ConsultaTurnoService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServidorWebTurnos {

    private static final Logger LOG = Logger.getLogger(ServidorWebTurnos.class.getName());
    private static final String RUTA_TURNO = "/turno/";

    private final int puerto;
    private final ConsultaTurnoService consultaService = new ConsultaTurnoService();
    private HttpServer servidor;
    private ExecutorService hilos;

    public ServidorWebTurnos(int puerto) {
        this.puerto = puerto;
    }

    public void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        AtomicInteger numero = new AtomicInteger();
        hilos = Executors.newFixedThreadPool(4, tarea -> {
            Thread hilo = new Thread(tarea, "web-" + numero.incrementAndGet());
            hilo.setDaemon(true);
            return hilo;
        });
        servidor.setExecutor(hilos);
        servidor.createContext(RUTA_TURNO, this::atenderTurno);
        servidor.createContext("/", intercambio -> {
            registrarVisita(intercambio);
            responder(intercambio, 200, PaginaTurno.inicio());
        });
        servidor.start();
    }

    /**
     * Escribe en la salida del servidor cada visita a la página: si al escanear el QR no aparece
     * esta línea, el celular nunca llegó a la computadora (firewall o red distinta).
     */
    private static void registrarVisita(HttpExchange intercambio) {
        LOG.info(() -> "[web] " + intercambio.getRequestMethod() + " " + intercambio.getRequestURI()
                + " desde " + intercambio.getRemoteAddress().getAddress().getHostAddress());
    }

    public void detener() {
        if (servidor != null) {
            servidor.stop(0);
            hilos.shutdownNow();
        }
    }

    /** /turno/125-A038 → página con el estado del turno 125 (solo si el código coincide). */
    private void atenderTurno(HttpExchange intercambio) throws IOException {
        registrarVisita(intercambio);
        String clave = intercambio.getRequestURI().getPath().substring(RUTA_TURNO.length());
        int guion = clave.indexOf('-');
        Optional<ConsultaTurno> consulta = Optional.empty();
        try {
            if (guion > 0) {
                long id = Long.parseLong(clave.substring(0, guion));
                consulta = consultaService.consultar(id, clave.substring(guion + 1));
            }
        } catch (NumberFormatException e) {
            // dirección mal formada → "no encontrado"
        } catch (TurnosException e) {
            responder(intercambio, 503, PaginaTurno.error(e.getMessage()));
            return;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Error en la consulta web del turno", e);
            responder(intercambio, 500, PaginaTurno.error("No se pudo consultar el turno. Intente de nuevo."));
            return;
        }
        if (consulta.isPresent()) {
            responder(intercambio, 200, PaginaTurno.de(consulta.get()));
        } else {
            responder(intercambio, 404, PaginaTurno.error("No encontramos ese turno. Revise el código QR de su tiquete."));
        }
    }

    private static void responder(HttpExchange intercambio, int estado, String html) throws IOException {
        byte[] cuerpo = html.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.sendResponseHeaders(estado, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }
}