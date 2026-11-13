package com.turnos.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;
import java.util.List;

/**
 *
 * @author gomez
 */

public final class Animaciones {

    private Animaciones() {
    }

    /** El nodo sube y baja suavemente sin parar (ideal para ilustraciones). */
    public static void flotar(Node nodo, double pixeles, double segundos) {
        TranslateTransition flote = new TranslateTransition(Duration.seconds(segundos), nodo);
        flote.setFromY(0);
        flote.setToY(-pixeles);
        flote.setAutoReverse(true);
        flote.setCycleCount(TranslateTransition.INDEFINITE);
        flote.setInterpolator(Interpolator.EASE_BOTH);
        flote.play();
    }

    /** Aparece desvaneciéndose y creciendo un poco (para tarjetas y paneles). */
    public static void aparecer(Node nodo) {
        nodo.setOpacity(0);
        FadeTransition fundido = new FadeTransition(Duration.millis(450), nodo);
        fundido.setToValue(1);
        ScaleTransition escala = new ScaleTransition(Duration.millis(450), nodo);
        escala.setFromX(0.96);
        escala.setFromY(0.96);
        escala.setToX(1);
        escala.setToY(1);
        escala.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fundido, escala).play();
    }

    /** Los nodos entran uno tras otro desde abajo (efecto "cascada"). */
    public static void entradaEscalonada(List<? extends Node> nodos, double retrasoMs) {
        for (int i = 0; i < nodos.size(); i++) {
            Node nodo = nodos.get(i);
            nodo.setOpacity(0);
            nodo.setTranslateY(40);

            FadeTransition fundido = new FadeTransition(Duration.millis(420), nodo);
            fundido.setToValue(1);
            TranslateTransition subida = new TranslateTransition(Duration.millis(420), nodo);
            subida.setToY(0);
            subida.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition entrada = new ParallelTransition(fundido, subida);
            entrada.setDelay(Duration.millis(120 + i * retrasoMs));
            entrada.play();
        }
    }

    /** Un "salto" rápido: crece y vuelve a su tamaño (para llamar la atención sobre un dato nuevo). */
    public static void rebote(Node nodo) {
        ScaleTransition crece = new ScaleTransition(Duration.millis(160), nodo);
        crece.setToX(1.18);
        crece.setToY(1.18);
        ScaleTransition vuelve = new ScaleTransition(Duration.millis(260), nodo);
        vuelve.setToX(1);
        vuelve.setToY(1);
        vuelve.setInterpolator(Interpolator.EASE_OUT);
        new SequentialTransition(crece, vuelve).play();
    }

    /** Late suavemente sin parar (para botones o avisos importantes). */
    public static void latir(Node nodo) {
        ScaleTransition latido = new ScaleTransition(Duration.millis(900), nodo);
        latido.setFromX(1);
        latido.setFromY(1);
        latido.setToX(1.04);
        latido.setToY(1.04);
        latido.setAutoReverse(true);
        latido.setCycleCount(ScaleTransition.INDEFINITE);
        latido.setInterpolator(Interpolator.EASE_BOTH);
        latido.play();
    }
}