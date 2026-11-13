package com.turnos.controlador;

import com.turnos.cliente.ClienteRed;
import com.turnos.cliente.EstadoPublico;
import com.turnos.cliente.ObservadorServidor;
import com.turnos.cliente.TurnoRemoto;
import com.turnos.modelo.TurnoLlamado;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.util.Animaciones;
import com.turnos.util.TareaAsincrona;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla pública tipo sala de espera.
 * Se actualiza sola: es observadora de ClienteRed. Cuando una ventanilla llama un turno, el
 * servidor envía EVENTO_TURNO_LLAMADO y el hilo lector del socket llama a alRecibirEvento.
 * Si se pierde la conexión, un hilo reintenta cada 3 segundos.
 * <p>
 * Además de la tabla de llamados tiene un panel de anuncios que rota solo, un aviso grande
 * que aparece encima al llamar un turno y una cinta de mensajes en el pie.
 */
public class PantallaPublicaController implements ObservadorServidor {

    @FXML private Label lblHora;
    @FXML private Label lblAviso;
    @FXML private ListView<TurnoLlamado> lstHistorial;

    @FXML private VBox diapositiva;
    @FXML private ImageView imgDiapositiva;
    @FXML private Label lblDiapoTitulo;
    @FXML private Label lblDiapoTexto;
    @FXML private HBox boxPuntos;

    @FXML private VBox avisoLlamado;
    @FXML private Label lblAvisoTurno;
    @FXML private Label lblAvisoVentanilla;

    @FXML private Label lblFecha;
    @FXML private Label lblHoraPie;
    @FXML private Pane cinta;
    @FXML private Label lblCinta;

    private static final int LIMITE_HISTORIAL = 10;
    private static final long MILISEGUNDOS_ENTRE_REINTENTOS = 3000;
    private static final Duration DURACION_DIAPOSITIVA = Duration.seconds(8);
    private static final Duration DURACION_AVISO = Duration.seconds(7);
    private static final double PIXELES_POR_SEGUNDO_CINTA = 90;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_HORA_CORTA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.forLanguageTag("es-CR"));

    /** Un color por ventanilla (se repiten si hay más ventanillas que colores). */
    private static final String[] COLORES_VENTANILLA = {
            "#2563EB", "#059669", "#EA580C", "#9333EA", "#DB2777", "#0891B2", "#CA8A04"};

    private static final String MENSAJE_CINTA =
            "Bienvenidos  •  Esté atento a la pantalla: al escuchar el aviso diríjase a la ventanilla indicada"
            + "  •  Escanee el código QR de su tiquete para seguir su turno desde el celular"
            + "  •  Adultos mayores, embarazadas y personas con discapacidad tienen atención preferencial  •  ";

    /** Anuncios que rotan en el panel derecho: imagen, título y texto. */
    private record Anuncio(String imagen, String titulo, String texto) {
    }

    private static final List<Anuncio> ANUNCIOS = List.of(
            new Anuncio("/img/ilustracion-turno.png", "Siga su turno desde el celular",
                    "Escanee el código QR de su tiquete y vea cuántas personas faltan y en qué ventanilla lo atienden."),
            new Anuncio("/img/ilustracion-atencion.png", "¡Bienvenidos!",
                    "Al escuchar el aviso, diríjase a la ventanilla que indica la pantalla."),
            new Anuncio("/img/ilustracion-ventanilla.png", "Atención preferencial",
                    "Adultos mayores, embarazadas y personas con discapacidad son atendidos con prioridad."),
            new Anuncio("/img/ilustracion-espera.png", "Gracias por su paciencia",
                    "Si no se presenta cuando lo llaman, su turno se marca como ausente."));

    private final ClienteRed cliente = ClienteRed.getInstancia();
    private final TurnoRemoto turnoService = new TurnoRemoto();
    private volatile boolean reconectando;
    private int anuncioActual;
    private PauseTransition ocultarAviso;

    @FXML
    private void initialize() {
        iniciarReloj();
        configurarTabla();
        iniciarAnuncios();
        iniciarCinta();
        Animaciones.flotar(imgDiapositiva, 10, 2.5);
        Animaciones.latir(lblAvisoVentanilla);

        cliente.agregarObservador(this);
        precargarHistorial(EstadoPublico.getInstancia());
    }

    // ------------------------------------------------------------------ tabla de llamados

    private void configurarTabla() {
        lstHistorial.setItems(EstadoPublico.getInstancia().getHistorial());
        lstHistorial.setCellFactory(lista -> new FilaTurno());
    }

    /** Celda de la tabla: [ventanilla de color] [código] [hora]. La primera fila va en rojo. */
    private static final class FilaTurno extends ListCell<TurnoLlamado> {
        private final Label ventanilla = new Label();
        private final Label codigo = new Label();
        private final Label hora = new Label();
        private final HBox fila = new HBox(10, ventanilla, codigo, hora);

        FilaTurno() {
            ventanilla.getStyleClass().add("celda-ventanilla");
            codigo.getStyleClass().add("celda-turno");
            hora.getStyleClass().add("celda-hora");
            codigo.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(codigo, Priority.ALWAYS);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("fila-turno");
        }

        @Override
        protected void updateItem(TurnoLlamado llamado, boolean vacio) {
            super.updateItem(llamado, vacio);
            if (vacio || llamado == null) {
                setGraphic(null);
                return;
            }
            int numero = llamado.numeroVentanilla();
            ventanilla.setText(String.valueOf(numero));
            ventanilla.setStyle("-fx-background-color: " + colorDe(numero) + ";");
            codigo.setText(llamado.codigo());
            hora.setText(llamado.fechaHoraLlamado().format(FORMATO_HORA_CORTA));

            fila.getStyleClass().remove("fila-actual");
            if (getIndex() == 0) {
                fila.getStyleClass().add("fila-actual");
                ventanilla.setStyle(null); // en la fila roja el número va sobre fondo translúcido
            }
            setGraphic(fila);
        }
    }

    private static String colorDe(int numeroVentanilla) {
        return COLORES_VENTANILLA[Math.floorMod(numeroVentanilla - 1, COLORES_VENTANILLA.length)];
    }

    // ------------------------------------------------------------------ red

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
            Platform.runLater(() -> avisar(llamado));
        } else if (evento.getTipo() == TipoMensaje.EVENTO_TURNO_RETIRADO) {
            EstadoPublico.getInstancia().retirar((String) evento.getDatos());
        }
    }

    @Override
    public void alPerderConexion() {
        Platform.runLater(() -> {
            lblAviso.setText("Sin conexión con el servidor. Reintentando...");
            reconectarEnSegundoPlano();
        });
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

    // ------------------------------------------------------------------ aviso de llamado

    /**
     * Innovación: al llamar un turno aparece en grande encima de los anuncios (con parpadeo
     * y un pitido) durante unos segundos; luego vuelven los anuncios.
     */
    private void avisar(TurnoLlamado llamado) {
        lblAvisoTurno.setText(llamado.codigo());
        lblAvisoVentanilla.setText("Ventanilla " + llamado.numeroVentanilla());
        avisoLlamado.setOpacity(1);
        avisoLlamado.setVisible(true);

        ScaleTransition entrada = new ScaleTransition(Duration.millis(380), avisoLlamado);
        entrada.setFromX(0.6);
        entrada.setFromY(0.6);
        entrada.setToX(1);
        entrada.setToY(1);
        entrada.setInterpolator(Interpolator.EASE_OUT);
        entrada.play();

        FadeTransition parpadeo = new FadeTransition(Duration.millis(350), lblAvisoTurno);
        parpadeo.setFromValue(1.0);
        parpadeo.setToValue(0.25);
        parpadeo.setCycleCount(6);
        parpadeo.setAutoReverse(true);
        parpadeo.play();

        if (ocultarAviso != null) {
            ocultarAviso.stop(); // si llaman otro turno seguido, se reinicia el tiempo
        }
        ocultarAviso = new PauseTransition(DURACION_AVISO);
        ocultarAviso.setOnFinished(e -> {
            FadeTransition salida = new FadeTransition(Duration.millis(500), avisoLlamado);
            salida.setToValue(0);
            salida.setOnFinished(f -> avisoLlamado.setVisible(false));
            salida.play();
        });
        ocultarAviso.play();

        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable e) {
            // equipo sin sonido: queda solo el aviso visual
        }
    }

    // ------------------------------------------------------------------ anuncios que rotan

    private void iniciarAnuncios() {
        for (int i = 0; i < ANUNCIOS.size(); i++) {
            Region punto = new Region();
            punto.getStyleClass().add("punto");
            boxPuntos.getChildren().add(punto);
        }
        mostrarAnuncio(0);
        Timeline rotacion = new Timeline(new KeyFrame(DURACION_DIAPOSITIVA, e -> cambiarAnuncio()));
        rotacion.setCycleCount(Timeline.INDEFINITE);
        rotacion.play();
    }

    private void cambiarAnuncio() {
        FadeTransition salida = new FadeTransition(Duration.millis(400), diapositiva);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            mostrarAnuncio((anuncioActual + 1) % ANUNCIOS.size());
            FadeTransition entrada = new FadeTransition(Duration.millis(500), diapositiva);
            entrada.setToValue(1);
            entrada.play();
        });
        salida.play();
    }

    private void mostrarAnuncio(int indice) {
        anuncioActual = indice;
        Anuncio anuncio = ANUNCIOS.get(indice);
        imgDiapositiva.setImage(new Image(getClass().getResourceAsStream(anuncio.imagen())));
        lblDiapoTitulo.setText(anuncio.titulo());
        lblDiapoTexto.setText(anuncio.texto());
        for (int i = 0; i < boxPuntos.getChildren().size(); i++) {
            var clases = boxPuntos.getChildren().get(i).getStyleClass();
            clases.remove("punto-activo");
            if (i == indice) {
                clases.add("punto-activo");
            }
        }
    }

    // ------------------------------------------------------------------ cinta de mensajes

    /** El texto entra por la derecha y sale por la izquierda, una y otra vez. */
    private void iniciarCinta() {
        lblCinta.setText(MENSAJE_CINTA);
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(cinta.widthProperty());
        recorte.heightProperty().bind(cinta.heightProperty());
        cinta.setClip(recorte);

        // se arranca cuando JavaFX ya calculó los anchos (antes valen 0)
        cinta.widthProperty().addListener((obs, antes, ancho) -> {
            if (antes.doubleValue() == 0 && ancho.doubleValue() > 0) {
                Platform.runLater(this::animarCinta);
            }
        });
    }

    private void animarCinta() {
        double anchoCinta = cinta.getWidth();
        double anchoTexto = lblCinta.getWidth();
        TranslateTransition movimiento = new TranslateTransition(
                Duration.seconds((anchoCinta + anchoTexto) / PIXELES_POR_SEGUNDO_CINTA), lblCinta);
        movimiento.setFromX(anchoCinta);
        movimiento.setToX(-anchoTexto);
        movimiento.setInterpolator(Interpolator.LINEAR);
        movimiento.setCycleCount(TranslateTransition.INDEFINITE);
        movimiento.play();
    }

    // ------------------------------------------------------------------ reloj y fecha

    private void iniciarReloj() {
        Timeline reloj = new Timeline(
                new KeyFrame(Duration.ZERO, e -> actualizarReloj()),
                new KeyFrame(Duration.seconds(1))
        );
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    private void actualizarReloj() {
        LocalTime ahora = LocalTime.now();
        lblHora.setText(ahora.format(FORMATO_HORA));
        lblHoraPie.setText(ahora.format(FORMATO_HORA_CORTA));
        String fecha = LocalDate.now().format(FORMATO_FECHA);
        lblFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
    }
}