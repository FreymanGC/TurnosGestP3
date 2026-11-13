package com.turnos.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Map;

 public final class GeneradorQR {

    private static final int ZONA_SILENCIO = 4;

    private GeneradorQR() {
    }

    /** @param tamanoMaximo tamaño aproximado en píxeles; la imagen final mide igual o un poco menos. */
    public static Image crear(String texto, int tamanoMaximo) {
        try {
            // tamaño 0 = ZXing devuelve la matriz mínima: 1 celda por módulo
            BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                           EncodeHintType.MARGIN, ZONA_SILENCIO));
            int modulos = matriz.getWidth();
            int escala = Math.max(1, tamanoMaximo / modulos);   // píxeles por módulo (entero)
            int lado = modulos * escala;

            WritableImage imagen = new WritableImage(lado, lado);
            PixelWriter pixeles = imagen.getPixelWriter();
            for (int y = 0; y < lado; y++) {
                for (int x = 0; x < lado; x++) {
                    pixeles.setColor(x, y, matriz.get(x / escala, y / escala) ? Color.BLACK : Color.WHITE);
                }
            }
            return imagen;
        } catch (WriterException e) {
            throw new IllegalArgumentException("No se pudo generar el codigo QR", e);
        }
    }
}