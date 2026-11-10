package com.turnos.controlador;

import com.turnos.cliente.ClienteRed;
import com.turnos.cliente.ObservadorServidor;
import com.turnos.cliente.ServicioRemoto;
import com.turnos.cliente.TurnoRemoto;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.util.GeneradorQR;
import com.turnos.util.TareaAsincrona;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.text.Normalizer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Terminal (kiosko) donde la persona elige el servicio y retira su turno.
 * Cada servicio tiene su propio color e ícono; el tiquete incluye un QR para seguir el turno
 * desde el celular.
 */
public class GeneradoraTurnosController implements ObservadorServidor {

    private static final int COLUMNAS = 2;
    private static final int TAMANO_QR = 380;
    private static final String SIN_TURNO = "—";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_RELOJ = DateTimeFormatter.ofPattern("hh:mm a");

    /** Colores (inicio y fin del degradado) que se usan si el servicio no es de un tipo conocido. */
    private static final String[][] COLORES_EXTRA = {
        {"#EC4899", "#BE185D"}, {"#14B8A6", "#0F766E"}, {"#0EA5E9", "#0369A1"},
        {"#EF4444", "#B91C1C"}, {"#84CC16", "#4D7C0F"}, {"#F59E0B", "#B45309"}
    };

    @FXML private GridPane gridServicios;
    @FXML private VBox tiquete;
    @FXML private Label lblMensaje;
    @FXML private Label lblTurnoGenerado;
    @FXML private Label lblDetalleTurno;
    @FXML private Label lblHora;
    @FXML private Label lblQR;
    @FXML private ImageView imgQR;

    private final ServicioRemoto servicioService = new ServicioRemoto();
    private final TurnoRemoto turnoService = new TurnoRemoto();
    /** Dirección base de la página del QR; se pide al servidor una sola vez. */
    private volatile String urlConsulta;

    @FXML
    private void initialize() {
        ClienteRed.getInstancia().agregarObservador(this);
        iniciarReloj();
        cargarServicios();
    }

    private void cargarServicios() {
        TareaAsincrona.ejecutar(servicioService::listarActivos, this::construirBotones, lblMensaje::setText);
    }

    @Override
    public void alRecibirEvento(Mensaje evento) {
        if (evento.getTipo() == TipoMensaje.EVENTO_SERVICIOS_ACTUALIZADOS) {
            Platform.runLater(this::cargarServicios);
        }
    }

    @Override
    public void alPerderConexion() {
        Platform.runLater(() -> lblMensaje.setText(
                "Se perdió la conexión con el servidor. Se reconectará al seleccionar un servicio."));
    }

    // ------------------------------------------------------------------ botones de servicio

    private void construirBotones(List<Servicio> servicios) {
        gridServicios.getChildren().clear();
        for (int i = 0; i < servicios.size(); i++) {
            Servicio servicio = servicios.get(i);
            Apariencia apariencia = aparienciaDe(servicio, i);

            Button boton = new Button(servicio.nombre());
            boton.setGraphic(contenido(servicio, apariencia));
            boton.getStyleClass().add("tarjeta-servicio");
            boton.setStyle("-fx-background-color: linear-gradient(to bottom right, " + apariencia.inicio()
                    + ", " + apariencia.fin() + "); -fx-effect: dropshadow(gaussian, " + apariencia.inicio()
                    + "AA, 22, 0, 0, 8);");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setWrapText(true);
            boton.setOnAction(evento -> generarTurno(servicio));
            gridServicios.add(boton, i % COLUMNAS, i / COLUMNAS);
        }
    }

    /** Ícono grande en un círculo, la letra del servicio y, si aplica, la etiqueta de preferencial. */
    private VBox contenido(Servicio servicio, Apariencia apariencia) {
        SVGPath icono = new SVGPath();
        icono.setContent(apariencia.icono());
        icono.setFillRule(FillRule.EVEN_ODD);
        icono.getStyleClass().add("icono-servicio");
        icono.setScaleX(1.9);
        icono.setScaleY(1.9);
        StackPane circulo = new StackPane(icono);
        circulo.getStyleClass().add("circulo-icono");

        Label letra = new Label(servicio.prefijo());
        letra.getStyleClass().add("chip-letra");
        HBox chips = new HBox(6, letra);
        chips.setAlignment(Pos.CENTER);
        if (servicio.esPreferencial()) {
            Label preferencial = new Label("★ PREFERENCIAL");
            preferencial.getStyleClass().add("chip-letra");
            chips.getChildren().add(preferencial);
        }
        VBox caja = new VBox(10, circulo, chips);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    /** Elige color e ícono según el tipo de servicio (por palabras de su nombre). */
    private static Apariencia aparienciaDe(Servicio servicio, int posicion) {
        String nombre = Normalizer.normalize(servicio.nombre().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        if (servicio.esPreferencial() || nombre.contains("preferencial")) {
            return new Apariencia("#A855F7", "#DB2777", Iconos.ESTRELLA);
        }
        if (nombre.contains("inform") || nombre.contains("consulta")) {
            return new Apariencia("#3B82F6", "#1D4ED8", Iconos.INFORMACION);
        }
        if (nombre.contains("tramite") || nombre.contains("document") || nombre.contains("solicitud")) {
            return new Apariencia("#10B981", "#047857", Iconos.DOCUMENTO);
        }
        if (nombre.contains("pago") || nombre.contains("caja") || nombre.contains("cobro")) {
            return new Apariencia("#F97316", "#C2410C", Iconos.TARJETA);
        }
        String[] colores = COLORES_EXTRA[posicion % COLORES_EXTRA.length];
        return new Apariencia(colores[0], colores[1], Iconos.TIQUETE);
    }

    private record Apariencia(String inicio, String fin, String icono) {
    }

    /** Íconos dibujados con SVGPath (no necesitan archivos de imagen). */
    private static final class Iconos {
        static final String INFORMACION = "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM11 10h2v7h-2zM11 6.5h2v2h-2z";
        static final String DOCUMENTO = "M6 2h8l5 5v15H6zM8.5 11h7v1.6h-7zM8.5 14.5h7v1.6h-7zM8.5 18h5v1.6h-5z";
        static final String TARJETA = "M2 5h20v14H2zM2 8h20v3H2zM4.5 14h6v2h-6z";
        static final String ESTRELLA = "M12 2l2.9 6.3 6.9.7-5.2 4.6 1.5 6.8L12 17l-6.1 3.4 1.5-6.8L2.2 9l6.9-.7z";
        static final String TIQUETE = "M2 6h20v4a2 2 0 0 0 0 4v4H2v-4a2 2 0 0 0 0-4z";

        private Iconos() {
        }
    }

    // ------------------------------------------------------------------ turno y QR

    private void generarTurno(Servicio servicio) {
        lblMensaje.setText("");
        TareaAsincrona.ejecutar(
                () -> {
                    Turno turno = turnoService.generarTurno(servicio);
                    if (urlConsulta == null) {
                        urlConsulta = turnoService.urlConsultaTurno();
                    }
                    return turno;
                },
                turno -> mostrarTurno(turno, servicio),
                mensaje -> {
                    lblTurnoGenerado.setText(SIN_TURNO);
                    lblDetalleTurno.setText("");
                    mostrarQR(null);
                    lblMensaje.setText(mensaje);
                },
                gridServicios);
    }

    private void mostrarTurno(Turno turno, Servicio servicio) {
        lblTurnoGenerado.setText(turno.codigo());
        lblDetalleTurno.setText(servicio.nombre() + "  ·  " + turno.fechaHoraGeneracion().format(FORMATO_HORA));
        mostrarQR(urlConsulta == null ? null : urlConsulta + turno.id() + "-" + turno.codigo());

        ScaleTransition salto = new ScaleTransition(Duration.millis(180), tiquete);
        salto.setFromX(0.94);
        salto.setFromY(0.94);
        salto.setToX(1);
        salto.setToY(1);
        salto.play();
    }

    private void mostrarQR(String direccion) {
        boolean hayQR = direccion != null;
        imgQR.setImage(hayQR ? GeneradorQR.crear(direccion, TAMANO_QR) : null);
        lblQR.setVisible(hayQR);
        lblQR.setManaged(hayQR);
    }

    private void iniciarReloj() {
        Timeline reloj = new Timeline(
                new KeyFrame(Duration.ZERO, e -> lblHora.setText(LocalTime.now().format(FORMATO_RELOJ))),
                new KeyFrame(Duration.seconds(1)));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }
}