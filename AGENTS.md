# AGENTS.md — SportCourt 2.0

## 1. Objetivo del proyecto

SportCourt 2.0 es un sistema web para la gestión de canchas deportivas, clases, reservas e inscripciones.

La arquitectura del proyecto es:

Frontend (HTML/CSS/JavaScript/Tailwind)
        ↓
REST API
        ↓
Backend (Spring Boot / Java)
        ↓
MySQL

Regla principal:

> El Frontend presenta, el Backend decide y MySQL almacena.

---

## 2. Reglas obligatorias de trabajo

- NO modificar código, archivos, configuraciones o base de datos sin autorización explícita.
- Antes de modificar cualquier archivo, analizar primero el problema y explicar qué se cambiará.
- No realizar cambios destructivos sin autorización.
- No crear otra base de datos.
- La base de datos existente `sportcourt` es la fuente de verdad para los datos del sistema.
- No utilizar localStorage como fuente de verdad para datos de negocio cuando estos deben provenir de MySQL.
- Mantener la arquitectura existente salvo que se autorice expresamente cambiarla.
- No eliminar funcionalidades existentes sin autorización.
- No reemplazar código funcional por una solución diferente sin justificarlo.
- Mantener cambios pequeños, controlados y fáciles de revertir.

---

## 3. Metodología de trabajo

El proyecto se desarrolla en 7 fases:

1. Análisis
2. Diseño
3. Frontend
4. Backend
5. Integración
6. Testing y Seguridad
7. Deployment y Documentación

No avanzar a una fase siguiente hasta comprobar y cerrar correctamente la fase actual.

Proceso obligatorio:

OBJETIVO
→ TAREAS
→ IMPLEMENTACIÓN AUTORIZADA
→ PRUEBA
→ CORRECCIÓN
→ VALIDACIÓN
→ CIERRE DE FASE
→ SIGUIENTE FASE

---

## 4. Base de datos

Base de datos existente:

`sportcourt`

No crear una segunda base de datos ni cambiar el nombre de la base de datos.

Tablas principales:

- usuarios
- perfil
- cancha
- clase
- reserva
- inscripcion

MySQL debe ser la fuente principal de persistencia de los datos de negocio.

Las operaciones de creación, consulta, modificación y eliminación deben realizarse mediante el Backend cuando corresponda.

---

## 5. Backend

Tecnologías principales:

- Java
- Spring Boot
- Spring Data JPA
- MySQL
- REST API

Endpoints principales:

- `/api/login`
- `/api/usuarios`
- `/api/canchas`
- `/api/clases`
- `/api/reservas`
- `/api/inscripciones`

El Backend debe encargarse de:

- Validaciones de negocio.
- Autenticación.
- Autorización.
- Reglas de reservas.
- Prevención de reservas duplicadas o superpuestas.
- Capacidad de clases.
- Prevención de inscripciones duplicadas.
- Persistencia en MySQL.
- Manejo correcto de errores HTTP.

---

## 6. Frontend

Tecnologías principales:

- HTML
- CSS
- JavaScript
- Tailwind CSS

El Frontend debe encargarse principalmente de:

- Presentar información.
- Capturar datos del usuario.
- Validaciones básicas de interfaz.
- Consumir la API REST.
- Mostrar respuestas y errores del Backend.

No debe tomar decisiones de negocio que correspondan al Backend.

---

## 7. Seguridad

Priorizar:

- Contraseñas protegidas.
- Autenticación correcta.
- Autorización según rol.
- Validación de entradas.
- Prevención de XSS.
- CORS restringido.
- No exponer contraseñas en respuestas de la API.
- No colocar credenciales sensibles directamente en el código.
- Configuración mediante variables de entorno cuando corresponda.

---

## 8. Roles del sistema

### Visitante

Puede:

- Ver información pública.
- Ver canchas.
- Ver clases.
- Acceder al login.

No puede realizar operaciones que requieran autenticación.

### Usuario

Puede:

- Iniciar sesión.
- Consultar canchas.
- Realizar reservas.
- Cancelar sus reservas según las reglas del sistema.
- Consultar sus reservas.
- Consultar clases.
- Inscribirse en clases.
- Consultar sus inscripciones.
- Consultar su perfil.

### Administrador

Puede:

- Gestionar canchas.
- Gestionar clases.
- Gestionar reservas.
- Realizar operaciones administrativas autorizadas.

---

## 9. Testing

Antes de considerar una funcionalidad terminada:

1. Analizar el código.
2. Ejecutar o realizar la prueba correspondiente.
3. Verificar la respuesta del Backend.
4. Verificar los datos en MySQL cuando corresponda.
5. Verificar la interfaz.
6. Confirmar que no se introdujeron regresiones.

No declarar una funcionalidad como terminada solamente porque la interfaz parece funcionar.

---

## 10. Forma de trabajar con Codex

Cuando se solicite ayuda:

1. Primero analizar.
2. Identificar archivos afectados.
3. Explicar el problema.
4. Proponer la solución.
5. Esperar autorización antes de modificar archivos.
6. Realizar solamente los cambios autorizados.
7. Mostrar qué se modificó.
8. Ejecutar las pruebas correspondientes.
9. Informar claramente el resultado.

Si existe incertidumbre sobre un cambio, preguntar antes de modificar.

---

## 11. Regla de prioridad

Las instrucciones explícitas del propietario del proyecto tienen prioridad sobre cualquier cambio automático.

Nunca asumir autorización para:

- Modificar archivos.
- Eliminar código.
- Eliminar registros.
- Cambiar la base de datos.
- Crear otra base de datos.
- Cambiar arquitectura.
- Cambiar tecnologías.
- Hacer migraciones destructivas.

---

## 12. Estado actual

El proyecto se encuentra en un proceso de revisión y mejora por fases.

La prioridad es:

- Mantener la funcionalidad existente.
- Corregir problemas de forma controlada.
- Mantener MySQL como fuente de verdad.
- Probar cada cambio.
- Documentar los resultados.
- Avanzar únicamente después de validar cada fase.