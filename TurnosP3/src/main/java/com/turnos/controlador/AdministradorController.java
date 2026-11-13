package com.turnos.controlador;

import com.turnos.cliente.AdministracionRemota;
import com.turnos.cliente.AutenticacionRemota;
import com.turnos.cliente.ClienteRed;
import com.turnos.cliente.ObservadorServidor;
import com.turnos.modelo.EstadoVentanilla;
import com.turnos.modelo.Rol;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.TablaReporte;
import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.util.Animaciones;
import com.turnos.util.Paneles;
import com.turnos.util.TareaAsincrona;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel del administrador: usuarios/funcionarios, servicios (con prioridad), ventanillas (con
 * los servicios que atiende cada una y su estado en tiempo real) y las 5 consultas.
 */
public class AdministradorController implements ObservadorServidor {

    private static final String[] CONSULTAS = {
        "1. Turnos atendidos entre dos fechas",
        "2. Personas atendidas por funcionario",
        "3. Turnos por servicio",
        "4. Tiempo de espera (generación → inicio)",
        "5. Tiempo de atención (inicio → fin)"
    };
    private static final String CLASE_ERROR = "mensaje-error";
    private static final String CLASE_EXITO = "mensaje-exito";

    @FXML private Node login;
    @FXML private LoginController loginController;
    @FXML private BorderPane panelAdmin;
    @FXML private Label lblBienvenida;
    @FXML private Label lblMensaje;
    @FXML private javafx.scene.image.ImageView imgBanner;

    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuUsuario;
    @FXML private TableColumn<Usuario, String> colUsuNombre;
    @FXML private TableColumn<Usuario, String> colUsuRol;
    @FXML private TableColumn<Usuario, String> colUsuActivo;
    @FXML private Label lblUsuFormulario;
    @FXML private TextField txtUsuUsuario;
    @FXML private TextField txtUsuNombre;
    @FXML private ComboBox<Rol> cmbUsuRol;
    @FXML private CheckBox chkUsuActivo;
    @FXML private PasswordField txtUsuClave;

    @FXML private TableView<Servicio> tblServicios;
    @FXML private TableColumn<Servicio, String> colSerPrefijo;
    @FXML private TableColumn<Servicio, String> colSerNombre;
    @FXML private TableColumn<Servicio, String> colSerPrioridad;
    @FXML private TableColumn<Servicio, String> colSerActivo;
    @FXML private Label lblSerFormulario;
    @FXML private TextField txtSerPrefijo;
    @FXML private TextField txtSerNombre;
    @FXML private Spinner<Integer> spnSerPrioridad;
    @FXML private CheckBox chkSerActivo;

    @FXML private TableView<EstadoVentanilla> tblVentanillas;
    @FXML private TableColumn<EstadoVentanilla, String> colVenNumero;
    @FXML private TableColumn<EstadoVentanilla, String> colVenNombre;
    @FXML private TableColumn<EstadoVentanilla, String> colVenDisponible;
    @FXML private TableColumn<EstadoVentanilla, String> colVenOperador;
    @FXML private TableColumn<EstadoVentanilla, String> colVenTurno;
    @FXML private Label lblVenFormulario;
    @FXML private TextField txtVenNumero;
    @FXML private TextField txtVenNombre;
    @FXML private CheckBox chkVenDisponible;
    @FXML private VBox boxVenServicios;

    @FXML private ComboBox<String> cmbConsulta;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblTituloReporte;
    @FXML private TableView<List<String>> tblReporte;
    @FXML private Label lblResumenReporte;

    private final AdministracionRemota administracion = new AdministracionRemota();
    private Usuario administrador;
    private Usuario usuarioEditado;
    private Servicio servicioEditado;
    private Ventanilla ventanillaEditada;
    private List<Servicio> servicios = new ArrayList<>();

    @FXML
    private void initialize() {
        loginController.configurar(Rol.ADMIN, "Acceso para administradores", this::alAutenticar);
        Paneles.mostrarSolo(login, login, panelAdmin);
        ClienteRed.getInstancia().agregarObservador(this);
        Animaciones.flotar(imgBanner, 6, 2.6);

        colUsuUsuario.setCellValueFactory(c -> texto(c.getValue().nombreUsuario()));
        colUsuNombre.setCellValueFactory(c -> texto(c.getValue().nombreCompleto()));
        colUsuRol.setCellValueFactory(c -> texto(c.getValue().rol().getNombre()));
        colUsuActivo.setCellValueFactory(c -> texto(c.getValue().activo() ? "Sí" : "No"));
        colSerPrefijo.setCellValueFactory(c -> texto(c.getValue().prefijo()));
        colSerNombre.setCellValueFactory(c -> texto(c.getValue().nombre()));
        colSerPrioridad.setCellValueFactory(c -> texto(c.getValue().prioridad() == 0
                ? "0 (normal)" : c.getValue().prioridad() + " (preferencial)"));
        colSerActivo.setCellValueFactory(c -> texto(c.getValue().activo() ? "Sí" : "No"));
        colVenNumero.setCellValueFactory(c -> texto(String.valueOf(c.getValue().ventanilla().numero())));
        colVenNombre.setCellValueFactory(c -> texto(c.getValue().ventanilla().etiqueta()));
        colVenDisponible.setCellValueFactory(c -> texto(c.getValue().ventanilla().disponible() ? "Sí" : "No"));
        colVenOperador.setCellValueFactory(c -> texto(c.getValue().operadorConectado()));
        colVenTurno.setCellValueFactory(c -> texto(c.getValue().turnoActual()));

        cmbUsuRol.getItems().setAll(Rol.values());
        cmbUsuRol.setValue(Rol.OPERADOR);
        spnSerPrioridad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        cmbConsulta.getItems().setAll(CONSULTAS);
        cmbConsulta.getSelectionModel().selectFirst();
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((o, a, u) -> { if (u != null) editarUsuario(u); });
        tblServicios.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> { if (s != null) editarServicio(s); });
        tblVentanillas.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> { if (v != null) editarVentanilla(v.ventanilla()); });
    }

    private void alAutenticar(Usuario autenticado) {
        administrador = autenticado;
        lblBienvenida.setText("Bienvenido(a), " + autenticado.nombreCompleto());
        Paneles.mostrarSolo(panelAdmin, login, panelAdmin);
        cargarUsuarios();
        cargarServicios();
        cargarVentanillas();
    }

    @FXML
    private void cerrarSesion() {
        TareaAsincrona.ejecutar(() -> {
            new AutenticacionRemota().cerrarSesion();
            return true;
        }, ok -> { }, mensaje -> { });
        volverAlLogin();
    }

    private void volverAlLogin() {
        administrador = null;
        loginController.limpiar();
        Paneles.mostrarSolo(login, login, panelAdmin);
    }

    // ================================================================== usuarios

    private void cargarUsuarios() {
        TareaAsincrona.ejecutar(administracion::listarUsuarios, tblUsuarios.getItems()::setAll, this::mostrarError);
    }

    @FXML
    private void nuevoUsuario() {
        usuarioEditado = null;
        tblUsuarios.getSelectionModel().clearSelection();
        lblUsuFormulario.setText("Nuevo usuario");
        txtUsuUsuario.clear();
        txtUsuNombre.clear();
        cmbUsuRol.setValue(Rol.OPERADOR);
        chkUsuActivo.setSelected(true);
        txtUsuClave.clear();
    }

    private void editarUsuario(Usuario u) {
        usuarioEditado = u;
        lblUsuFormulario.setText("Editando: " + u.nombreUsuario());
        txtUsuUsuario.setText(u.nombreUsuario());
        txtUsuNombre.setText(u.nombreCompleto());
        cmbUsuRol.setValue(u.rol());
        chkUsuActivo.setSelected(u.activo());
        txtUsuClave.clear();
    }

    @FXML
    private void guardarUsuario() {
        Usuario u = new Usuario(usuarioEditado == null ? 0 : usuarioEditado.id(), txtUsuUsuario.getText(), null,
                txtUsuNombre.getText(), cmbUsuRol.getValue(), chkUsuActivo.isSelected());
        String clave = txtUsuClave.getText();
        TareaAsincrona.ejecutar(() -> administracion.guardarUsuario(u, clave), guardado -> {
            mostrarExito("Usuario \"" + guardado.nombreUsuario() + "\" guardado.");
            nuevoUsuario();
            cargarUsuarios();
        }, this::mostrarError);
    }

    // ================================================================== servicios

    private void cargarServicios() {
        TareaAsincrona.ejecutar(administracion::listarServicios, lista -> {
            servicios = lista;
            tblServicios.getItems().setAll(lista);
            dibujarCasillas(List.of());
        }, this::mostrarError);
    }

    @FXML
    private void nuevoServicio() {
        servicioEditado = null;
        tblServicios.getSelectionModel().clearSelection();
        lblSerFormulario.setText("Nuevo servicio");
        txtSerPrefijo.clear();
        txtSerNombre.clear();
        spnSerPrioridad.getValueFactory().setValue(0);
        chkSerActivo.setSelected(true);
    }

    private void editarServicio(Servicio s) {
        servicioEditado = s;
        lblSerFormulario.setText("Editando: " + s.prefijo() + " - " + s.nombre());
        txtSerPrefijo.setText(s.prefijo());
        txtSerNombre.setText(s.nombre());
        spnSerPrioridad.getValueFactory().setValue(s.prioridad());
        chkSerActivo.setSelected(s.activo());
    }

    @FXML
    private void guardarServicio() {
        Integer prioridad = spnSerPrioridad.getValue();
        Servicio s = new Servicio(servicioEditado == null ? 0 : servicioEditado.id(), txtSerPrefijo.getText(),
                txtSerNombre.getText(), prioridad == null ? 0 : prioridad, chkSerActivo.isSelected());
        TareaAsincrona.ejecutar(() -> administracion.guardarServicio(s), guardado -> {
            mostrarExito("Servicio " + guardado.prefijo() + " guardado. Las generadoras se actualizaron.");
            nuevoServicio();
            cargarServicios();
        }, this::mostrarError);
    }

    // ================================================================== ventanillas

    @FXML
    private void cargarVentanillas() {
        TareaAsincrona.ejecutar(administracion::estadoVentanillas, tblVentanillas.getItems()::setAll, this::mostrarError);
    }

    @FXML
    private void nuevaVentanilla() {
        ventanillaEditada = null;
        tblVentanillas.getSelectionModel().clearSelection();
        lblVenFormulario.setText("Nueva ventanilla");
        txtVenNumero.clear();
        txtVenNombre.clear();
        chkVenDisponible.setSelected(true);
        dibujarCasillas(List.of());
    }

    private void editarVentanilla(Ventanilla v) {
        ventanillaEditada = v;
        lblVenFormulario.setText("Editando: " + v.etiqueta());
        txtVenNumero.setText(String.valueOf(v.numero()));
        txtVenNombre.setText(v.nombre());
        chkVenDisponible.setSelected(v.disponible());
        TareaAsincrona.ejecutar(() -> administracion.serviciosDeVentanilla(v.id()), this::dibujarCasillas, this::mostrarError);
    }

    private void dibujarCasillas(List<Integer> marcados) {
        boxVenServicios.getChildren().clear();
        for (Servicio s : servicios) {
            CheckBox casilla = new CheckBox(s.prefijo() + " - " + s.nombre());
            casilla.setUserData(s.id());
            casilla.setSelected(marcados.contains(s.id()));
            boxVenServicios.getChildren().add(casilla);
        }
    }

    @FXML
    private void guardarVentanilla() {
        int numero;
        try {
            numero = Integer.parseInt(txtVenNumero.getText().trim());
        } catch (NumberFormatException e) {
            mostrarError("El número de ventanilla debe ser un número entero.");
            return;
        }
        List<Integer> marcados = new ArrayList<>();
        boxVenServicios.getChildren().forEach(n -> {
            CheckBox c = (CheckBox) n;
            if (c.isSelected()) {
                marcados.add((Integer) c.getUserData());
            }
        });
        if (marcados.isEmpty()) {
            mostrarError("Marque al menos un servicio que pueda atender la ventanilla.");
            return;
        }
        Ventanilla v = new Ventanilla(ventanillaEditada == null ? 0 : ventanillaEditada.id(), numero,
                txtVenNombre.getText(), chkVenDisponible.isSelected());
        TareaAsincrona.ejecutar(() -> administracion.guardarVentanilla(v, marcados), guardada -> {
            mostrarExito(guardada.etiqueta() + " guardada.");
            nuevaVentanilla();
            cargarVentanillas();
        }, this::mostrarError);
    }

    // ================================================================== consultas

    @FXML
    private void consultar() {
        int numero = cmbConsulta.getSelectionModel().getSelectedIndex() + 1;
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        TareaAsincrona.ejecutar(() -> administracion.consultar(numero, desde, hasta), this::mostrarReporte, this::mostrarError);
    }

    /** Crea las columnas según el reporte recibido: una sola tabla sirve para las 5 consultas. */
    private void mostrarReporte(TablaReporte reporte) {
        tblReporte.getColumns().clear();
        for (int i = 0; i < reporte.columnas().size(); i++) {
            final int indice = i;
            TableColumn<List<String>, String> columna = new TableColumn<>(reporte.columnas().get(i));
            columna.setCellValueFactory(c -> texto(c.getValue().get(indice)));
            columna.setPrefWidth(i == 1 ? 180 : 115);
            tblReporte.getColumns().add(columna);
        }
        tblReporte.getItems().setAll(reporte.filas());
        lblTituloReporte.setText(reporte.titulo());
        lblResumenReporte.setText(reporte.resumen());
        mostrarExito("Consulta generada.");
    }

    // ================================================================== general

    @Override
    public void alRecibirEvento(Mensaje evento) {
        if (administrador != null && (evento.getTipo() == TipoMensaje.EVENTO_VENTANILLAS_ACTUALIZADAS
                || evento.getTipo() == TipoMensaje.EVENTO_TURNO_LLAMADO
                || evento.getTipo() == TipoMensaje.EVENTO_TURNO_RETIRADO)) {
            Platform.runLater(this::cargarVentanillas);
        }
    }

    @Override
    public void alPerderConexion() {
        Platform.runLater(() -> {
            if (administrador != null) {
                volverAlLogin();
                loginController.mostrarError("Se perdió la conexión con el servidor. Inicie sesión de nuevo.");
            }
        });
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor == null ? "" : valor);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.getStyleClass().removeAll(CLASE_ERROR, CLASE_EXITO);
        lblMensaje.getStyleClass().add(CLASE_ERROR);
        lblMensaje.setText(mensaje);
    }

    private void mostrarExito(String mensaje) {
        lblMensaje.getStyleClass().removeAll(CLASE_ERROR, CLASE_EXITO);
        lblMensaje.getStyleClass().add(CLASE_EXITO);
        lblMensaje.setText(mensaje);
    }
}