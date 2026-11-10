/**
 * Servidor central: acepta conexiones (ServerSocket), atiende cada cliente en un hilo del pool,
 * procesa las solicitudes con la capa de servicio y avisa a todos los clientes (Observer).
 */
package com.turnos.servidor;
