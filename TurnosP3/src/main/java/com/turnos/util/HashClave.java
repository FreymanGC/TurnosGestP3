package com.turnos.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


public final class HashClave {

    private HashClave() {
    }

    public static String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", e);
        }
    }

    public static boolean coincide(String claveIngresada, String hashAlmacenado) {
        byte[] calculado = sha256Hex(claveIngresada).getBytes(StandardCharsets.UTF_8);
        byte[] almacenado = hashAlmacenado.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(calculado, almacenado);
    }
}
