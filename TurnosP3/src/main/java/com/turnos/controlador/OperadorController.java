package com.turnos.controlador;

import com.turnos.modelo.EstadoTurno;
import com.turnos.modelo.Rol;
import com.turnos.modelo.Turno;
import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;
import com.turnos.servicio.TurnoService;
import com.turnos.servicio.VentanillaService;
import com.turnos.util.Paneles;
import com.turnos.util.TareaAsincrona;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

public class OperadorController {

    private static final int LIMITE_HISTORIAL = 15;
    private static final String SIN_TURNO = "---";
    private static final String CLASE_ERROR = "mensaje-error";
    private static final String CLASE_EXITO = "mensaje-exito";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Node login;
    @FXML private LoginController loginController;

    @FXML private VBox panelVentanilla;
    @FXML private Label lblBienvenida;
    @FXML private ComboBox<Ventanilla> cmbVentanilla;
    @FXML private Label lblErrorVentanilla;
    @FXML private Button btnContinuar;

    @FXML private BorderPane panelOperador;
    @FXML private Label lblFuncionario;
    @FXML private Label lblVentanilla;
    @FXML private Label lblEstadoVentanilla;
    @FXML private Label lblTurnoActual;
    @FXML private Label lblEstadoTurno;
    @FXML private Label lblMensaje;
    @FXML private HBox boxAcciones;
    @FXML private Button btnSiguiente;
    @FXML private Button btnVolverALlamar;
    @FXML private Button btnIniciar;
    @FXML private Button btnFinalizar;
    @FXML private Button btnDisponibilidad;
    @FXML private ListView<String> lstHistorial;

    private final TurnoService turnoService = new TurnoService();
    private final VentanillaService ventanillaService = new VentanillaService();
    private final ObservableList<String> historial = FXCollections.observableArrayList();

    private Usuario operador;
    private Ventanilla ventanilla;
    private Turno turnoActual;

    private record VistaInicial(Ventanilla ventanilla, Turno turnoActivo, List<Turno> historial) {
    }

    private record ResultadoAccion(Turno turno, List<Turno> historial) {
    }

    @FXML
    private void initialize() {
        loginController.configurar(Rol.OPERADOR, "Acceso para operadores", this::alAutenticar);
        lstHistorial.setItems(historial);
        mostrarLogin();
    }

    private void alAutenticar(Usuario autenticado) {
        operador = autenticado;
        lblBienvenida.setText("Bienvenido(a), " + autenticado.nombreCompleto());
        lblErrorVentanilla.setText("");
        Paneles.mostrarSolo(panelVentanilla, login, panelVentanilla, panelOperador);
        TareaAsincrona.ejecutar(ventanillaService::listar, this::mostrarVentanillas,
                lblErrorVentanilla::setText, btnContinuar);
    }

    private void mostrarVentanillas(List<Ventanilla> ventanillas) {
        cmbVentanilla.getItems().setAll(ventanillas);
        cmbVentanilla.getSelectionModel().selectFirst();
    }

    @FXML
    private void confirmarVentanilla() {
        Ventanilla seleccionada = cmbVentanilla.getValue();
        if (seleccionada == null) {
            lblErrorVentanilla.setText("Seleccione una ventanilla.");
            return;
        }
        lblErrorVentanilla.setText("");
        TareaAsincrona.ejecutar(() -> cargarVistaInicial(seleccionada), this::entrarAlPanel,
                lblErrorVentanilla::setText, btnContinuar);
    }

    private VistaInicial cargarVistaInicial(Ventanilla seleccionada) {
        Ventanilla actual = ventanillaService.refrescar(seleccionada);
        Turno activo = turnoService.obtenerTurnoActivo(actual).orElse(null);
        return new VistaInicial(actual, activo, turnoService.historialDeVentanilla(actual, LIMITE_HISTORIAL));
    }

    private void entrarAlPanel(VistaInicial vista) {
        ventanilla = vista.ventanilla();
        turnoActual = vista.turnoActivo();
        lblFuncionario.setText("Funcionario: " + operador.nombreCompleto());
        mostrarHistorial(vista.historial());
        limpiarMensaje();
        refrescarPantalla();
        Paneles.mostrarSolo(panelOperador, login, panelVentanilla, panelOperador);
    }

    @FXML
    private void solicitarSiguienteTurno() {
        ejecutarAccion(() -> turnoService.llamarSiguiente(ventanilla, operador), "Turno llamado.");
    }

    @FXML
    private void volverALlamar() {
        ejecutarAccion(() -> turnoService.volverALlamar(turnoActual, ventanilla), "Turno llamado de nuevo.");
    }

    @FXML
    private void iniciarAtencion() {
        ejecutarAccion(() -> turnoService.iniciarAtencion(turnoActual), "Atención iniciada.");
    }

    @FXML
    private void finalizarAtencion() {
        ejecutarAccion(() -> turnoService.finalizarAtencion(turnoActual), "Atención finalizada.");
    }

    @FXML
    private void alternarDisponibilidad() {
        boolean nuevoEstado = !ventanilla.disponible();
        TareaAsincrona.ejecutar(
                () -> ventanillaService.cambiarDisponibilidad(ventanilla, nuevoEstado),
                actualizada -> {
                    ventanilla = actualizada;
                    mostrarExito(actualizada.disponible() ? "Ventanilla disponible." : "Ventanilla no disponible.");
                    refrescarPantalla();
                },
                this::mostrarError,
                boxAcciones);
    }

    @FXML
    private void cerrarSesion() {
        operador = null;
        ventanilla = null;
        turnoActual = null;
        historial.clear();
        loginController.limpiar();
        mostrarLogin();
    }

    /** Ejecuta una transición del turno y luego recarga el historial de la ventanilla. */
    private void ejecutarAccion(Callable<Turno> accion, String mensajeExito) {
        TareaAsincrona.ejecutar(
                () -> {
                    Turno resultado = accion.call();
                    return new ResultadoAccion(resultado, turnoService.historialDeVentanilla(ventanilla, LIMITE_HISTORIAL));
                },
                resultado -> alTerminarAccion(resultado, mensajeExito),
                this::mostrarError,
                boxAcciones);
    }

    private void alTerminarAccion(ResultadoAccion resultado, String mensajeExito) {
        Turno turno = resultado.turno();
        turnoActual = turno.estado().esFinal() ? null : turno;
        mostrarHistorial(resultado.historial());
        mostrarExito(mensajeExito + " " + turno.codigo());
        refrescarPantalla();
    }

    private void refrescarPantalla() {
        lblVentanilla.setText("Ventanilla: " + ventanilla.etiqueta());
        lblEstadoVentanilla.setText("Estado: " + (ventanilla.disponible() ? "Disponible" : "No disponible"));
        btnDisponibilidad.setText(ventanilla.disponible() ? "Marcar no disponible" : "Marcar disponible");
        lblTurnoActual.setText(turnoActual == null ? SIN_TURNO : turnoActual.codigo());
        lblEstadoTurno.setText(turnoActual == null ? "Sin turno asignado" : turnoActual.estado().getNombre());

        EstadoTurno estado = turnoActual == null ? null : turnoActual.estado();
        btnSiguiente.setDisable(estado != null || !ventanilla.disponible());
        btnVolverALlamar.setDisable(estado != EstadoTurno.LLAMADO);
        btnIniciar.setDisable(estado != EstadoTurno.LLAMADO);
        btnFinalizar.setDisable(estado != EstadoTurno.EN_ATENCION);
        btnDisponibilidad.setDisable(estado != null);
    }

    private void mostrarHistorial(List<Turno> turnos) {
        historial.setAll(turnos.stream().map(this::describir).toList());
    }

    private String describir(Turno turno) {
        String hora = turno.fechaHoraLlamado() == null ? "" : "  ·  llamado " + turno.fechaHoraLlamado().format(FORMATO_HORA);
        return turno.codigo() + "  ·  " + turno.estado().getNombre() + hora;
    }

    private void mostrarLogin() {
        Paneles.mostrarSolo(login, login, panelVentanilla, panelOperador);
    }

    private void mostrarError(String mensaje) {
        establecerMensaje(mensaje, CLASE_ERROR);
    }

    private void mostrarExito(String mensaje) {
        establecerMensaje(mensaje, CLASE_EXITO);
    }

    private void limpiarMensaje() {
        lblMensaje.setText("");
    }

    private void establecerMensaje(String mensaje, String clase) {
        lblMensaje.getStyleClass().removeAll(CLASE_ERROR, CLASE_EXITO);
        lblMensaje.getStyleClass().add(clase);
        lblMensaje.setText(mensaje);
    }
}
