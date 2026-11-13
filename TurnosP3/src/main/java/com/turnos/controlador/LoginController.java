package com.turnos.controlador;

import com.turnos.cliente.AutenticacionRemota;
import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;
import com.turnos.util.Animaciones;
import com.turnos.util.TareaAsincrona;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.util.function.Consumer;

public class LoginController {

    @FXML private Label lblSubtitulo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;
    @FXML private ImageView imgIlustracion;

    private final AutenticacionRemota autenticacionService = new AutenticacionRemota();
    private Rol rolRequerido;
    private Consumer<Usuario> alAutenticar;

    @FXML
    private void initialize() {
        Animaciones.flotar(imgIlustracion, 12, 2.4);
    }

    public void configurar(Rol rolRequerido, String subtitulo, Consumer<Usuario> alAutenticar) {
        this.rolRequerido = rolRequerido;
        this.alAutenticar = alAutenticar;
        lblSubtitulo.setText(subtitulo);
    }

    public void limpiar() {
        txtUsuario.clear();
        txtContrasena.clear();
        lblError.setText("");
    }

    public void mostrarError(String mensaje) {
        lblError.setText(mensaje);
    }

    @FXML
    private void iniciarSesion() {
        lblError.setText("");
        String usuario = txtUsuario.getText();
        String contrasena = txtContrasena.getText();
        TareaAsincrona.ejecutar(
                () -> autenticacionService.autenticar(usuario, contrasena, rolRequerido),
                autenticado -> {
                    txtContrasena.clear();
                    alAutenticar.accept(autenticado);
                },
                lblError::setText,
                btnIngresar, txtUsuario, txtContrasena);
    }
}