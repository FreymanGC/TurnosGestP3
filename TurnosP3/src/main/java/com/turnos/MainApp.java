package com.turnos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage stagePrincipal) throws IOException {
        Parent raiz = FXMLLoader.load(getClass().getResource("/fxml/MenuPrincipal.fxml"));
        Scene escena = new Scene(raiz);
        escena.getStylesheets().add(getClass().getResource("/css/estilos.css").toExternalForm());

        stagePrincipal.setTitle("Sistema de Gestión de Turnos - Menú de pruebas");
        stagePrincipal.setScene(escena);
        stagePrincipal.show();
    }

    public static void abrirVentana(String rutaFxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(rutaFxml));
            Parent raiz = loader.load();
            Scene escena = new Scene(raiz);
            escena.getStylesheets().add(MainApp.class.getResource("/css/estilos.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(escena);
            stage.show();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "No se pudo abrir la ventana " + rutaFxml, e);
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir la ventana \"" + titulo + "\".").show();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
