package com.turnos.controlador;

import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;
import com.turnos.servicio.AutenticacionService;
import com.turnos.util.TareaAsincrona;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/** Panel de login reutilizable (se incluye con fx:include en Operador y Administrador). */
public class LoginController {

    @FXML private Label lblSubtitulo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    private final AutenticacionService autenticacionService = new AutenticacionService();
    private Rol rolRequerido;
    private Consumer<Usuario> alAutenticar;

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
