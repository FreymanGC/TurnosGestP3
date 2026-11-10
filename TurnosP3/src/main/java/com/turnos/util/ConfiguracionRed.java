package com.turnos.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Properties;

public final class ConfiguracionRed {

    private static final String ARCHIVO = "red.properties";
    private static final ConfiguracionRed INSTANCIA = new ConfiguracionRed();

    private final String host;
    private final int puerto;
    private final int puertoWeb;
    private final String hostWeb;

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
        puerto = entero(props.getProperty("servidor.puerto"), 5050);
        puertoWeb = entero(props.getProperty("web.puerto"), 8080);
        hostWeb = props.getProperty("web.host", "").trim();
    }

    private static int entero(String valor, int porDefecto) {
        try {
            return valor == null ? porDefecto : Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
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

    public int getPuertoWeb() {
        return puertoWeb;
    }

    /**
     * Dirección base de la página de consulta, por ejemplo http://192.168.1.20:8080/turno/.
     * Debe ser la IP de la red (no "localhost") para que el celular pueda llegar.
     */
    public String getUrlConsultaTurno() {
        String ip = hostWeb.isEmpty() ? detectarIpLocal() : hostWeb;
        return "http://" + ip + ":" + puertoWeb + "/turno/";
    }

    /**
     * IPv4 de la red local (Wi-Fi o cable). Se saltan los adaptadores virtuales (VirtualBox,
     * VMware, Hyper-V, WSL, Bluetooth), que el celular no puede alcanzar.
     */
    private static String detectarIpLocal() {
        String respaldo = null;
        try {
            for (NetworkInterface red : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!red.isUp() || red.isLoopback() || red.isVirtual()) {
                    continue;
                }
                String nombre = (red.getName() + " " + red.getDisplayName()).toLowerCase();
                boolean virtual = nombre.matches(".*(virtual|vmware|vbox|hyper-v|vethernet|wsl|bluetooth|docker).*");
                for (InetAddress direccion : Collections.list(red.getInetAddresses())) {
                    if (direccion instanceof Inet4Address && direccion.isSiteLocalAddress()) {
                        if (!virtual) {
                            return direccion.getHostAddress();
                        }
                        if (respaldo == null) {
                            respaldo = direccion.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            // se usa el respaldo o localhost
        }
        return respaldo != null ? respaldo : "localhost";
    }
}