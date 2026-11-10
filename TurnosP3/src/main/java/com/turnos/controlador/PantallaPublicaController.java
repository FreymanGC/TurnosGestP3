package com.turnos.controlador;

import com.turnos.cliente.ClienteRed;
import com.turnos.cliente.EstadoPublico;
import com.turnos.cliente.ObservadorServidor;
import com.turnos.cliente.TurnoRemoto;
import com.turnos.modelo.TurnoLlamado;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.util.TareaAsincrona;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PantallaPublicaController implements ObservadorServidor {

    @FXML private Label lblHora;
    @FXML private Label lblTurnoActual;
    @FXML private Label lblVentanilla;
    @FXML private Label lblAviso;
    @FXML private ListView<String> lstHistorial;

    private static final int LIMITE_HISTORIAL = 10;
    private static final long MILISEGUNDOS_ENTRE_REINTENTOS = 3000;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ClienteRed cliente = ClienteRed.getInstancia();
    private final TurnoRemoto turnoService = new TurnoRemoto();
    private volatile boolean reconectando;

    @FXML
    private void initialize() {
        iniciarReloj();

        EstadoPublico estado = EstadoPublico.getInstancia();
        lblTurnoActual.textProperty().bind(estado.turnoLlamadoProperty());
        lblVentanilla.textProperty().bind(Bindings.concat("Ventanilla: ", estado.ventanillaLlamadaProperty()));
        lstHistorial.setItems(estado.getHistorial());

        cliente.agregarObservador(this);
        precargarHistorial(estado);
    }

    private void precargarHistorial(EstadoPublico estado) {
        TareaAsincrona.ejecutar(
                () -> turnoService.ultimosLlamados(LIMITE_HISTORIAL),
                llamados -> {
                    estado.cargarHistorial(llamados);
                    lblAviso.setText("");
                },
                mensaje -> {
                    lblAviso.setText(mensaje + " Reintentando...");
                    reconectarEnSegundoPlano();
                });
    }

    @Override
    public void alRecibirEvento(Mensaje evento) {
        if (evento.getTipo() == TipoMensaje.EVENTO_TURNO_LLAMADO) {
            TurnoLlamado llamado = (TurnoLlamado) evento.getDatos();
            EstadoPublico.getInstancia().publicar(llamado);
            Platform.runLater(this::avisar);
        }
    }

    @Override
    public void alPerderConexion() {
        Platform.runLater(() -> {
            lblAviso.setText("Sin conexión con el servidor. Reintentando...");
            reconectarEnSegundoPlano();
        });
    }

    /** Innovación: aviso visual (parpadeo) y sonoro cuando se llama un turno. */
    private void avisar() {
        FadeTransition parpadeo = new FadeTransition(Duration.millis(350), lblTurnoActual);
        parpadeo.setFromValue(1.0);
        parpadeo.setToValue(0.2);
        parpadeo.setCycleCount(6);
        parpadeo.setAutoReverse(true);
        parpadeo.play();
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable e) {
            // equipo sin sonido: queda solo el parpadeo
        }
    }

    private void reconectarEnSegundoPlano() {
        if (reconectando) {
            return;
        }
        reconectando = true;
        Thread hilo = new Thread(() -> {
            while (!cliente.estaConectado()) {
                try {
                    Thread.sleep(MILISEGUNDOS_ENTRE_REINTENTOS);
                    cliente.conectar();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (RuntimeException e) {
                    // el servidor sigue sin responder
                }
            }
            reconectando = false;
            Platform.runLater(() -> precargarHistorial(EstadoPublico.getInstancia()));
        }, "hilo-reconexion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void iniciarReloj() {
        Timeline reloj = new Timeline(
                new KeyFrame(Duration.ZERO, e -> lblHora.setText(LocalTime.now().format(FORMATO_HORA))),
                new KeyFrame(Duration.seconds(1))
        );
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }
}
