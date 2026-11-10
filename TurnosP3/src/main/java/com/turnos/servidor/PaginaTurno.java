/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.turnos.servidor;
import com.turnos.modelo.ConsultaTurno;
 
import java.time.format.DateTimeFormatter;
/**
 *
 * @author gomez
 */
final class PaginaTurno {
 
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SEGUNDOS_REFRESCO = 5;
 
    private PaginaTurno() {
    }
 
    static String de(ConsultaTurno c) {
        String color;
        String icono;
        String titulo;
        String detalle;
        switch (c.estado()) {
            case LLAMADO -> {
                color = "#10B981";
                icono = "🔔";
                titulo = "¡Es su turno!";
                detalle = "Diríjase a la <b class='grande'>VENTANILLA " + c.numeroVentanilla() + "</b>";
            }
            case EN_ATENCION -> {
                color = "#4F46E5";
                icono = "🙋";
                titulo = "En atención";
                detalle = "Está siendo atendido en la <b>Ventanilla " + c.numeroVentanilla() + "</b>";
            }
            case FINALIZADO -> {
                color = "#64748B";
                icono = "✅";
                titulo = "Atención finalizada";
                detalle = "¡Gracias por su visita!";
            }
            case CANCELADO -> {
                color = "#EF4444";
                icono = "⚠️";
                titulo = "Turno cancelado";
                detalle = "El turno fue llamado y no se presentó. Solicite uno nuevo en la generadora.";
            }
            default -> {
                color = "#F59E0B";
                icono = "⏳";
                titulo = "Está en la fila";
                detalle = c.personasAntes() == 0
                        ? "<b>¡Usted es el siguiente!</b> Esté atento a la pantalla."
                        : "Personas antes que usted: <b class='grande'>" + c.personasAntes() + "</b>";
            }
        }
        boolean refrescar = !c.estado().esFinal();
        String llamado = c.fechaHoraLlamado() == null ? "" : " · Llamado " + c.fechaHoraLlamado().format(HORA);
        return plantilla(refrescar,
                "<div class='etiqueta'>SU TURNO</div>"
                + "<div class='codigo'>" + escapar(c.codigo()) + "</div>"
                + "<div class='servicio'>" + escapar(c.servicio()) + "</div>"
                + "<div class='estado' style='background:" + color + "'>"
                + "<div class='icono'>" + icono + "</div>"
                + "<div class='titulo'>" + titulo + "</div>"
                + "<div class='detalle'>" + detalle + "</div></div>"
                + "<div class='pie'>Generado " + c.fechaHoraGeneracion().format(HORA) + llamado
                + (refrescar ? "<br>Esta página se actualiza sola cada " + SEGUNDOS_REFRESCO + " segundos" : "")
                + "</div>");
    }
 
    static String error(String mensaje) {
        return plantilla(false, "<div class='icono'>🔍</div><div class='titulo' style='color:#0F172A'>"
                + escapar(mensaje) + "</div>");
    }
 
    static String inicio() {
        return plantilla(false, "<div class='icono'>🎟️</div><div class='titulo' style='color:#0F172A'>"
                + "Sistema de Gestión de Turnos</div><div class='pie'>Escanee el código QR de su tiquete.</div>");
    }
 
    private static String plantilla(boolean refrescar, String contenido) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + (refrescar ? "<meta http-equiv='refresh' content='" + SEGUNDOS_REFRESCO + "'>" : "")
                + "<title>Mi turno</title><style>"
                + "body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
                + "font-family:'Segoe UI',Roboto,Arial,sans-serif;background:linear-gradient(135deg,#4F46E5,#C026D3);}"
                + ".tarjeta{background:#fff;border-radius:28px;padding:28px 24px;width:min(88vw,420px);text-align:center;"
                + "box-shadow:0 20px 50px rgba(15,23,42,.35);}"
                + ".etiqueta{color:#94A3B8;font-weight:700;letter-spacing:2px;font-size:13px}"
                + ".codigo{font-size:76px;font-weight:800;color:#4F46E5;line-height:1.1}"
                + ".servicio{color:#64748B;font-size:17px;margin-bottom:18px}"
                + ".estado{color:#fff;border-radius:20px;padding:20px 16px}"
                + ".icono{font-size:44px}.titulo{font-size:26px;font-weight:800;margin:6px 0}"
                + ".detalle{font-size:18px}.grande{font-size:30px;display:block;margin-top:6px}"
                + ".pie{color:#94A3B8;font-size:13px;margin-top:18px;line-height:1.6}"
                + "</style></head><body><div class='tarjeta'>" + contenido + "</div></body></html>";
    }
 
    /** Evita que un nombre con < o > se interprete como HTML. */
    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("'", "&#39;").replace("\"", "&quot;");
    }
}
 