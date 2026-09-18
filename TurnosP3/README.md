# Sistema de Gestión de Turnos — Avance 1

Proyecto de EIF206 Programación III. Java 21 + JavaFX 21.0.10 + FXML + Maven +
MySQL (base de datos gamabasis_p3g2, compartida por el profesor en phpMyAdmin).
El código fuente no lleva comentarios.

## Cómo abrirlo en NetBeans
1. NetBeans → File → Open Project → seleccionar esta carpeta (NetBeans detecta
   automáticamente que es un proyecto Maven por el pom.xml).
2. Clic derecho sobre el proyecto → Clean and Build.
3. Antes de correrlo, editar `src/main/java/com/turnos/util/ConexionBD.java`
   con la clave real (y confirmar el host de conexión — ver el comentario
   dentro de la clase, puede que no sea el mismo dominio del panel de phpMyAdmin).
4. Correr con: clic derecho → Run, o desde terminal con `mvn javafx:run`.

## Qué incluye este avance
- Estructura de paquetes por capas (modelo, dao, servicio, controlador,
  servidor, cliente, util).
- 4 pantallas FXML (Pantalla Pública, Generadora de Turnos, Operador —que
  incluye el login—, Administrador), cada una con su controlador. Login y
  Operador se fusionaron en una sola pantalla/ventana a pedido del usuario:
  primero se ve el formulario de login y, al iniciar sesión, la misma
  ventana cambia al panel de atención de turnos.
- Un puente en memoria (EstadoPublico, patrón Singleton + propiedades
  observables de JavaFX) que conecta el Operador con la Pantalla Pública
  mientras no existe comunicación real entre aplicaciones separadas: al
  llamar un turno desde Operador, la Pantalla Pública se actualiza sola.
  Esto es temporal y se reemplaza por sockets en un avance posterior.
- Un "Menú de pruebas" (MenuPrincipal.fxml) para abrir cada pantalla por
  separado durante el desarrollo — no es una pantalla del sistema final.
- Conexión centralizada a MySQL (ConexionBD.java), lista para
  configurar con tus datos reales.
- CSS externo compartido (estilos.css).

## Qué NO incluye todavía (a propósito)
- Sockets / comunicación cliente-servidor real.
- Hilos y control de concurrencia.
- Lógica real de generación/asignación de turnos.
- Autenticación real contra la base de datos.
- Reportes y consultas.
- Implementación de patrones de diseño (se proponen, no se codifican aún).
