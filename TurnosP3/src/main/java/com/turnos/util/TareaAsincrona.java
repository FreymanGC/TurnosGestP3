package com.turnos.util;

import com.turnos.excepcion.TurnosException;
import javafx.concurrent.Task;
import javafx.scene.Node;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ejecuta el trabajo (normalmente una llamada a un servicio con acceso a BD) fuera del hilo de
 * JavaFX para que la ventana no se congele con una BD remota, y devuelve el resultado o el
 * mensaje de error al hilo de JavaFX. Los nodos indicados se bloquean mientras dura la tarea
 * para evitar dobles clics (por ejemplo, dos turnos generados con un solo toque).
 */
public final class TareaAsincrona {

    private static final Logger LOG = Logger.getLogger(TareaAsincrona.class.getName());
    private static final String MENSAJE_INESPERADO = "Ocurrió un error inesperado. Intente de nuevo.";

    private static final ExecutorService EJECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread hilo = new Thread(runnable, "turnos-tarea-bd");
        hilo.setDaemon(true);
        return hilo;
    });

    private TareaAsincrona() {
    }

    public static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alExito,
                                    Consumer<String> alError, Node... bloquear) {
        boolean[] estabanDeshabilitados = bloquearNodos(bloquear);

        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(evento -> {
            restaurarNodos(bloquear, estabanDeshabilitados);
            alExito.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            restaurarNodos(bloquear, estabanDeshabilitados);
            alError.accept(mensajeParaUsuario(tarea.getException()));
        });
        EJECUTOR.execute(tarea);
    }

    private static String mensajeParaUsuario(Throwable error) {
        if (error instanceof TurnosException) {
            return error.getMessage();
        }
        LOG.log(Level.SEVERE, "Error inesperado en tarea asíncrona", error);
        return MENSAJE_INESPERADO;
    }

    private static boolean[] bloquearNodos(Node[] nodos) {
        boolean[] previos = new boolean[nodos.length];
        for (int i = 0; i < nodos.length; i++) {
            previos[i] = nodos[i].isDisable();
            nodos[i].setDisable(true);
        }
        return previos;
    }

    private static void restaurarNodos(Node[] nodos, boolean[] previos) {
        for (int i = 0; i < nodos.length; i++) {
            nodos[i].setDisable(previos[i]);
        }
    }
}
