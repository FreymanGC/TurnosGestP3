/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.turnos.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
 
/**
 *
 * @author gomez
 */
public record ConsultaTurno(
        String codigo,
        String servicio,
        EstadoTurno estado,
        Integer numeroVentanilla,
        int personasAntes,
        LocalDateTime fechaHoraGeneracion,
        LocalDateTime fechaHoraLlamado) implements Serializable {
}
 