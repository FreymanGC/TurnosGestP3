package com.turnos.util;

import javafx.scene.Node;

public final class Paneles {

    private Paneles() {
    }

    public static void mostrarSolo(Node visible, Node... paneles) {
        for (Node panel : paneles) {
            boolean mostrar = panel == visible;
            panel.setVisible(mostrar);
            panel.setManaged(mostrar);
        }
    }
}
