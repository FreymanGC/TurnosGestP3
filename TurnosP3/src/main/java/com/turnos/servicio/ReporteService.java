package com.turnos.servicio;

import com.turnos.dao.ReporteDAO;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.ConteoReporte;
import com.turnos.modelo.EstadoTurno;
import com.turnos.modelo.TablaReporte;
import com.turnos.modelo.TurnoDetallado;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Las 5 consultas del enunciado. Cada una devuelve una TablaReporte lista para mostrarse. */
public class ReporteService {

    public static final String[] CONSULTAS = {
        "1. Turnos atendidos entre dos fechas",
        "2. Personas atendidas por funcionario",
        "3. Turnos por servicio",
        "4. Tiempo de espera (generación → inicio)",
        "5. Tiempo de atención (inicio → fin)"
    };

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ReporteDAO reporteDAO;

    public ReporteService() {
        this(new ReporteDAO());
    }

    public ReporteService(ReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    public TablaReporte generar(int numeroConsulta, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("Seleccione la fecha inicial y la fecha final.");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        return switch (numeroConsulta) {
            case 1 -> turnosAtendidos(desde, hasta);
            case 2 -> conteo("Personas atendidas por funcionario", "Funcionario", "Personas atendidas",
                    reporteDAO.contarAtendidosPorFuncionario(desde, hasta), desde, hasta);
            case 3 -> conteo("Turnos por servicio", "Servicio", "Turnos",
                    reporteDAO.contarPorServicio(desde, hasta), desde, hasta);
            case 4 -> tiempoEspera(desde, hasta);
            case 5 -> tiempoAtencion(desde, hasta);
            default -> throw new ReglaNegocioException("Seleccione una consulta válida.");
        };
    }

    private TablaReporte turnosAtendidos(LocalDate desde, LocalDate hasta) {
        List<List<String>> filas = new ArrayList<>();
        for (TurnoDetallado t : reporteDAO.listarAtendidos(desde, hasta)) {
            if (t.estado() == EstadoTurno.FINALIZADO) {
                filas.add(List.of(t.codigo(), t.servicio(), FECHA.format(t.fechaOperacion()),
                        hora(t.fechaHoraGeneracion()), hora(t.fechaHoraLlamado()), hora(t.fechaHoraInicioAtencion()),
                        hora(t.fechaHoraFinalizacion()), ventanilla(t), texto(t.funcionario())));
            }
        }
        return new TablaReporte("Turnos atendidos del " + rango(desde, hasta),
                List.of("Turno", "Servicio", "Fecha", "Generado", "Llamado", "Inicio", "Fin", "Ventanilla", "Funcionario"),
                filas, "Total de turnos atendidos: " + filas.size());
    }

    private TablaReporte tiempoEspera(LocalDate desde, LocalDate hasta) {
        List<List<String>> filas = new ArrayList<>();
        long total = 0;
        for (TurnoDetallado t : reporteDAO.listarAtendidos(desde, hasta)) {
            Duration espera = Duration.between(t.fechaHoraGeneracion(), t.fechaHoraInicioAtencion());
            total += espera.getSeconds();
            filas.add(List.of(t.codigo(), t.servicio(), FECHA.format(t.fechaOperacion()),
                    hora(t.fechaHoraGeneracion()), hora(t.fechaHoraInicioAtencion()), duracion(espera)));
        }
        return new TablaReporte("Tiempo de espera del " + rango(desde, hasta),
                List.of("Turno", "Servicio", "Fecha", "Generado", "Inicio atención", "Espera"),
                filas, promedio("Espera promedio", total, filas.size()));
    }

    private TablaReporte tiempoAtencion(LocalDate desde, LocalDate hasta) {
        List<List<String>> filas = new ArrayList<>();
        long total = 0;
        for (TurnoDetallado t : reporteDAO.listarAtendidos(desde, hasta)) {
            if (t.estado() != EstadoTurno.FINALIZADO || t.fechaHoraFinalizacion() == null) {
                continue;
            }
            Duration atencion = Duration.between(t.fechaHoraInicioAtencion(), t.fechaHoraFinalizacion());
            total += atencion.getSeconds();
            filas.add(List.of(t.codigo(), t.servicio(), texto(t.funcionario()), ventanilla(t),
                    hora(t.fechaHoraInicioAtencion()), hora(t.fechaHoraFinalizacion()), duracion(atencion)));
        }
        return new TablaReporte("Tiempo de atención del " + rango(desde, hasta),
                List.of("Turno", "Servicio", "Funcionario", "Ventanilla", "Inicio", "Fin", "Duración"),
                filas, promedio("Atención promedio", total, filas.size()));
    }

    private TablaReporte conteo(String titulo, String columnaNombre, String columnaCantidad,
                                List<ConteoReporte> conteos, LocalDate desde, LocalDate hasta) {
        List<List<String>> filas = new ArrayList<>();
        long total = 0;
        for (ConteoReporte c : conteos) {
            filas.add(List.of(c.nombre(), String.valueOf(c.cantidad())));
            total += c.cantidad();
        }
        return new TablaReporte(titulo + " del " + rango(desde, hasta), List.of(columnaNombre, columnaCantidad),
                filas, "Total: " + total);
    }

    private static String rango(LocalDate desde, LocalDate hasta) {
        return FECHA.format(desde) + " al " + FECHA.format(hasta);
    }

    private static String hora(LocalDateTime fecha) {
        return fecha == null ? "—" : HORA.format(fecha);
    }

    private static String texto(String valor) {
        return valor == null ? "—" : valor;
    }

    private static String ventanilla(TurnoDetallado t) {
        return t.numeroVentanilla() == null ? "—" : String.valueOf(t.numeroVentanilla());
    }

    /** 754 segundos → "12 min 34 s". */
    private static String duracion(Duration d) {
        long s = Math.max(0, d.getSeconds());
        return (s / 60) + " min " + String.format("%02d", s % 60) + " s";
    }

    private static String promedio(String etiqueta, long totalSegundos, int cantidad) {
        if (cantidad == 0) {
            return "No hay atenciones en el rango seleccionado.";
        }
        return etiqueta + ": " + duracion(Duration.ofSeconds(totalSegundos / cantidad)) + "  (" + cantidad + " atenciones)";
    }
}
