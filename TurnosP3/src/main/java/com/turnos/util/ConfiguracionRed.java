package com.turnos.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Host y puerto del servidor central, leídos de red.properties (por defecto localhost:5050). */
public final class ConfiguracionRed {

    private static final String ARCHIVO = "red.properties";
    private static final ConfiguracionRed INSTANCIA = new ConfiguracionRed();

    private final String host;
    private final int puerto;

    private ConfiguracionRed() {
        Properties props = new Properties();
        try (InputStream in = ConfiguracionRed.class.getClassLoader().getResourceAsStream(ARCHIVO)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // se usan los valores por defecto
        }
        host = props.getProperty("servidor.host", "localhost").trim();
        int valor;
        try {
            valor = Integer.parseInt(props.getProperty("servidor.puerto", "5050").trim());
        } catch (NumberFormatException e) {
            valor = 5050;
        }
        puerto = valor;
    }

    public static ConfiguracionRed getInstancia() {
        return INSTANCIA;
    }

    public String getHost() {
        return host;
    }

    public int getPuerto() {
        return puerto;
    }
}
