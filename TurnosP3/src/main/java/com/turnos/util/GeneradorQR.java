/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
 
/**
 *
 * @author gomez
 */
public final class GeneradorQR {
 
    private static final Color OSCURO = Color.web("#1E1B4B");
 
    private GeneradorQR() {
    }
 
    public static Image crear(String texto, int tamano) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            WritableImage imagen = new WritableImage(matriz.getWidth(), matriz.getHeight());
            PixelWriter pixeles = imagen.getPixelWriter();
            for (int y = 0; y < matriz.getHeight(); y++) {
                for (int x = 0; x < matriz.getWidth(); x++) {
                    pixeles.setColor(x, y, matriz.get(x, y) ? OSCURO : Color.WHITE);
                }
            }
            return imagen;
        } catch (WriterException e) {
            throw new IllegalArgumentException("No se pudo generar el código QR", e);
        }
    }
}
