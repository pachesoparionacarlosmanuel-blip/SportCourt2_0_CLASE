# FASE 3: LÓGICA DE NEGOCIO - SERVICIOS DE VALIDACIÓN

**Estado:** ✅ **COMPLETADA Y COMPILADA**  
**Compilación:** BUILD SUCCESS (6.080 segundos)  
**Fecha:** Fase 3 Completa

> ❗ **Actualización posterior (2026-09-14):** la Validación 5 (capacidad disponible) se
> encontró que lanzaba `NullPointerException` al reservar una cancha real con `capacidad`
> NULL en MySQL (dato legado, p. ej. canchas id 1 y 2). Se corrigió tratando `capacidad`
> NULL como 1 (uso exclusivo). Ver `docs/PROYECTO_STATUS.md` para el detalle.

---

## 1. Objetivo de la Fase

Implementar la capa de servicios (`@Service`) que encapsula la lógica de negocio crítica:
- Validaciones complejas de duplicados
- Validaciones de capacidad y cupos
- Integración entre entidades relacionadas

---

## 2. Arquitectura de Servicios

La arquitectura implementada sigue el patrón de **Service Layer** con inyección de dependencias:

```
┌─────────────────────────────────────────────────────────┐
│              Controladores (HTTP REST)                   │
│  CanchaController, ClaseController, ReservaController    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│          CAPA DE SERVICIOS (Business Logic)              │
│                                                          │
│ ┌──────────────────────────────────────────────────┐   │
│ │  UsuarioService (Base)                           │   │
│ │  - verificarUsuarioExiste(id)                    │   │
│ │  - obtenerUsuario(id)                            │   │
│ │  - obtenerUsuarioDTO(id)                         │   │
│ └──────────────────────────────────────────────────┘   │
│        ↑              ↑                   ↑              │
│        │              │                   │              │
│ ┌──────────────────────────────────────────────────┐   │
│ │  CanchaService (Simple)                          │   │
│ │  - obtenerCancha(id)                             │   │
│ │  - listarCanchas()                               │   │
│ │  - crearCancha(DTO)                              │   │
│ │  - obtenerCapacidadCancha(id)                    │   │
│ └──────────────────────────────────────────────────┘   │
│        ↑                                                 │
│        │                                                 │
│ ┌──────────────────────────────────────────────────┐   │
│ │  ClaseService (Simple)                           │   │
│ │  - obtenerClase(id)                              │   │
│ │  - listarClases()                                │   │
│ │  - crearClase(DTO)                               │   │
│ │  - obtenerCuposDisponibles(id)                   │   │
│ └──────────────────────────────────────────────────┘   │
│        ↑                                                 │
│        │                                                 │
│ ┌──────────────────────────────────────────────────┐   │
│ │  ReservaService ⭐ (Crítico - 5 validaciones)    │   │
│ │  - crearReserva(DTO) con validaciones:           │   │
│ │    1. Usuario existe                             │   │
│ │    2. Cancha existe                              │   │
│ │    3. Horarios válidos (inicio < fin)            │   │
│ │    4. NO hay duplicados (horas superpuestas)     │   │
│ │    5. Capacidad disponible                       │   │
│ │  - actualizarReserva(id, DTO)                    │   │
│ │  - cancelarReserva(id)                           │   │
│ └──────────────────────────────────────────────────┘   │
│        ↑                                                 │
│        │                                                 │
│ ┌──────────────────────────────────────────────────┐   │
│ │  InscripcionService ⭐ (Crítico - 4 validaciones) │  │
│ │  - crearInscripcion(DTO) con validaciones:       │   │
│ │    1. Usuario existe                             │   │
│ │    2. Clase existe                               │   │
│ │    3. NO hay inscripción duplicada                │   │
│ │    4. Hay cupos disponibles                      │   │
│ │  - cancelarInscripcion(id)                       │   │
│ └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│          Repositorios (Spring Data JPA)                  │
│  CanchaRepository, ClaseRepository, ReservaRepository    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│          Base de Datos MySQL (sportcourt)                │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Servicios Implementados

### 3.1 UsuarioService (Base)

**Archivo:** `backend/src/main/java/com/sportcourt/backend/service/UsuarioService.java`

**Métodos:**

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `obtenerUsuario(id)` | Usuario | Obtiene usuario por ID o lanza ResourceNotFoundException |
| `listarUsuarios()` | List<Usuario> | Obtiene todos los usuarios |
| `obtenerUsuarioDTO(id)` | UsuarioDTO | Obtiene usuario sin exponer contraseña |
| `verificarUsuarioExiste(usuarioId)` | void | Lanza excepción si usuario NO existe (usado por otros servicios) |

**Dependencias:** UsuarioRepository

---

### 3.2 CanchaService (Simple CRUD)

**Archivo:** `backend/src/main/java/com/sportcourt/backend/service/CanchaService.java`

**Métodos:**

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `obtenerCancha(id)` | Cancha | Obtiene cancha o lanza ResourceNotFoundException |
| `listarCanchas()` | List<Cancha> | Lista todas las canchas |
| `crearCancha(DTO)` | Cancha | Crea cancha desde DTO (mapeo automático) |
| `actualizarCancha(id, DTO)` | Cancha | Actualiza cancha existente |
| `eliminarCancha(id)` | void | Elimina cancha |
| `obtenerCapacidadCancha(id)` | Integer | Obtiene capacidad (usado por ReservaService) |

**Dependencias:** CanchaRepository

---

### 3.3 ClaseService (Simple CRUD)

**Archivo:** `backend/src/main/java/com/sportcourt/backend/service/ClaseService.java`

**Métodos:**

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `obtenerClase(id)` | Clase | Obtiene clase o lanza ResourceNotFoundException |
| `listarClases()` | List<Clase> | Lista todas las clases |
| `crearClase(DTO)` | Clase | Crea clase desde DTO |
| `actualizarClase(id, DTO)` | Clase | Actualiza clase existente |
| `eliminarClase(id)` | void | Elimina clase |
| `obtenerCuposDisponibles(id)` | Integer | Obtiene cupos (usado por InscripcionService) |

**Dependencias:** ClaseRepository

---

### 3.4 ReservaService ⭐ (Crítico)

**Archivo:** `backend/src/main/java/com/sportcourt/backend/service/ReservaService.java`

#### Validaciones en `crearReserva(ReservaDTO)`

**Validación 1: Usuario existe**
```java
usuarioService.verificarUsuarioExiste(reservaDTO.getUsuarioId());
// Lanza: ResourceNotFoundException si usuario NO existe
```

**Validación 2: Cancha existe**
```java
canchaService.obtenerCancha(reservaDTO.getCanchaId());
// Lanza: ResourceNotFoundException si cancha NO existe
```

**Validación 3: Horarios válidos**
```java
// horaInicio < horaFin (no pueden ser iguales)
// Lanza: BusinessException("Hora inicio debe ser menor a hora fin")
```

**Validación 4: NO hay reserva duplicada** (MÁS IMPORTANTE)
```java
// Criterios de duplicado:
// - Mismo usuarioId
// - Misma canchaId
// - Misma fecha
// - Horas superpuestas: inicio1 < fin2 AND fin1 > inicio2
// - Estado NO cancelada

// Ejemplo: Si usuario ya tiene reserva 10:00-11:00
// Intentar 10:30-11:30 → RECHAZADO (se superpone)
// Intentar 11:00-12:00 → ACEPTADO (no se superpone)
```

**Validación 5: Capacidad disponible**
```java
// capacidadDisponible = capacidadTotal - reservasActivas
// Lanza: BusinessException si NO hay cupo disponible
```

#### Otros métodos

| Método | Descripción |
|--------|-------------|
| `obtenerReserva(id)` | Obtiene reserva o lanza ResourceNotFoundException |
| `listarReservas()` | Lista todas las reservas |
| `obtenerReservasDeUsuario(usuarioId)` | Lista reservas de un usuario |
| `actualizarReserva(id, DTO)` | Actualiza con validaciones similares |
| `cancelarReserva(id)` | Cambia estado a "cancelada" |
| `eliminarReserva(id)` | Elimina físicamente |

**Dependencias:** ReservaRepository, UsuarioService, CanchaService

---

### 3.5 InscripcionService ⭐ (Crítico)

**Archivo:** `backend/src/main/java/com/sportcourt/backend/service/InscripcionService.java`

#### Validaciones en `crearInscripcion(InscripcionDTO)`

**Validación 1: Usuario existe**
```java
usuarioService.verificarUsuarioExiste(inscripcionDTO.getUsuarioId());
// Lanza: ResourceNotFoundException si usuario NO existe
```

**Validación 2: Clase existe**
```java
claseService.obtenerClase(inscripcionDTO.getClaseId());
// Lanza: ResourceNotFoundException si clase NO existe
```

**Validación 3: NO hay inscripción duplicada** (IMPORTANTE)
```java
// Criterios de duplicado:
// - Mismo usuarioId
// - Misma claseId
// - Estado NO cancelada

// Lanza: BusinessException("El usuario ya está inscrito en esta clase")
```

**Validación 4: Hay cupos disponibles**
```java
// cuposDisponibles = cuposTotales - inscripcionesActivas
// Lanza: BusinessException si NO hay cupos
```

#### Otros métodos

| Método | Descripción |
|--------|-------------|
| `obtenerInscripcion(id)` | Obtiene inscripción o lanza ResourceNotFoundException |
| `listarInscripciones()` | Lista todas las inscripciones |
| `obtenerInscripcionesDeUsuario(usuarioId)` | Lista inscripciones de un usuario |
| `cancelarInscripcion(id)` | Cambia estado a "cancelada" |
| `eliminarInscripcion(id)` | Elimina físicamente |

**Dependencias:** InscripcionRepository, UsuarioService, ClaseService

---

## 4. Controladores Actualizados

Los controladores han sido actualizados para **inyectar y usar los servicios** en lugar de acceder directamente a repositorios:

### CanchaController
```java
@RestController
@RequestMapping("/api/canchas")
public class CanchaController {
    private final CanchaService canchaService;
    
    public CanchaController(CanchaService canchaService) {
        this.canchaService = canchaService;
    }
    
    @PostMapping
    public ResponseEntity<Cancha> crearCancha(@Valid @RequestBody CanchaDTO canchaDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(canchaService.crearCancha(canchaDTO));
    }
}
```

### ReservaController (Crítico)
```java
@PostMapping
public ResponseEntity<Reserva> crearReserva(@Valid @RequestBody ReservaDTO reservaDTO) {
    // ReservaService ejecuta 5 validaciones automáticamente
    // Si alguna falla, lanza BusinessException → GlobalExceptionHandler → 409
    Reserva reservaSaved = reservaService.crearReserva(reservaDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(reservaSaved);
}
```

### InscripcionController (Crítico)
```java
@PostMapping
public ResponseEntity<Inscripcion> crearInscripcion(@Valid @RequestBody InscripcionDTO inscripcionDTO) {
    // InscripcionService ejecuta 4 validaciones automáticamente
    Inscripcion inscripcionSaved = inscripcionService.crearInscripcion(inscripcionDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(inscripcionSaved);
}
```

---

## 5. Flujo de Errores - GlobalExceptionHandler

Todos los errores de validación se manejan centralmente:

```
Controller
    ↓
@Valid Validation Error → MethodArgumentNotValidException
                                    ↓
                        GlobalExceptionHandler
                                    ↓
                        → 400 BAD_REQUEST
                
ResourceNotFoundException
                                    ↓
                        GlobalExceptionHandler
                                    ↓
                        → 404 NOT_FOUND
                
BusinessException (duplicados, capacidad)
                                    ↓
                        GlobalExceptionHandler
                                    ↓
                        → 409 CONFLICT
```

**Ejemplo de respuesta 409 (BusinessException):**
```json
{
  "timestamp": "2026-09-08T13:40:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Ya existe una reserva en ese horario. Cancha: 1, Fecha: 2026-09-10, Horas: 10:00-11:00",
  "path": "/api/reservas"
}
```

---

## 6. Transaccionalidad

Todos los servicios están anotados con `@Transactional`:

```java
@Service
@Transactional
public class ReservaService {
    // Automáticamente:
    // - Todas las operaciones dentro de una transacción
    // - Si ocurre error → ROLLBACK automático
    // - Si éxito → COMMIT automático
}
```

---

## 7. Archivos Creados/Modificados

### Archivos Creados (Fase 3)

| Archivo | Líneas | Descripción |
|---------|--------|-------------|
| `service/UsuarioService.java` | 57 | Validación y consulta de usuarios |
| `service/CanchaService.java` | 90 | CRUD de canchas con capacidad |
| `service/ClaseService.java` | 90 | CRUD de clases con cupos |
| `service/ReservaService.java` | 189 | ⭐ 5 validaciones críticas |
| `service/InscripcionService.java` | 157 | ⭐ 4 validaciones críticas |

**Total:** 583 líneas de código nuevas

### Archivos Modificados

| Archivo | Cambios |
|---------|---------|
| `controller/CanchaController.java` | Inyecta CanchaService |
| `controller/ClaseController.java` | Inyecta ClaseService |
| `controller/ReservaController.java` | Inyecta ReservaService |
| `controller/InscripcionController.java` | Inyecta InscripcionService |
| `controller/UsuarioController.java` | Inyecta UsuarioService |

---

## 8. Compilación

```
BUILD SUCCESS
Total time: 6.080 s
Archivos compilados: 37

✅ Todos los servicios compilando correctamente
✅ Inyección de dependencias configurada
✅ Validaciones integradas
```

---

## 9. Casos de Prueba Esperados

### Caso 1: Reserva exitosa
```bash
POST /api/reservas
{
  "usuarioId": 1,
  "canchaId": 1,
  "fecha": "2026-09-10",
  "horaInicio": "10:00",
  "horaFin": "11:00",
  "estado": "activa"
}

Respuesta: 201 CREATED
{
  "id": 1,
  "usuarioId": 1,
  ...
}
```

### Caso 2: Usuario NO existe
```bash
POST /api/reservas
{
  "usuarioId": 999,
  ...
}

Respuesta: 404 NOT_FOUND
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "message": "Usuario con ID 999 no encontrado"
}
```

### Caso 3: Horarios inválidos
```bash
POST /api/reservas
{
  "usuarioId": 1,
  "canchaId": 1,
  "fecha": "2026-09-10",
  "horaInicio": "11:00",  // MAYOR que fin
  "horaFin": "10:00",
  "estado": "activa"
}

Respuesta: 409 CONFLICT
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "Hora inicio debe ser menor a hora fin"
}
```

### Caso 4: Reserva duplicada (horas superpuestas)
```
Usuario 1 ya tiene: 10:00-11:00 en cancha 1 el 2026-09-10
Intenta agregar: 10:30-11:30

POST /api/reservas → 409 CONFLICT
"Ya existe una reserva en ese horario"
```

### Caso 5: Sin cupos disponibles
```
Cancha con capacidad 1, ya tiene 1 reserva activa
Intenta agregar otra

POST /api/reservas → 409 CONFLICT
"No hay capacidad disponible"
```

---

## 10. Próximas Fases

✅ **Fase 1:** Seguridad (BCrypt, CORS) - COMPLETADA  
✅ **Fase 2:** Arquitectura (DTOs, Exception Handler) - COMPLETADA  
✅ **Fase 3:** Lógica de Negocio (Servicios) - **COMPLETADA**  

📋 **Fase 4:** Testing y Validación (Unit Tests, Integration Tests)  
📋 **Fase 5:** Frontend (Actualizar JS para nuevos errores 409)  
📋 **Fase 6:** Documentación Final (OpenAPI/Swagger)  

---

## 11. Resumen

| Aspecto | Detalle |
|--------|---------|
| **Servicios creados** | 5 (UsuarioService, CanchaService, ClaseService, ReservaService, InscripcionService) |
| **Validaciones críticas** | 9 (5 en Reserva, 4 en Inscripción) |
| **Controladores actualizados** | 5 |
| **Líneas de código** | 583 nuevas líneas en servicios |
| **Compilación** | ✅ BUILD SUCCESS |
| **Errores detectados y corregidos** | 3 (tipos LocalTime vs String) |
| **Patrones aplicados** | Dependency Injection, Service Layer, @Transactional, Exception Handling |

**Fase 3 completada y lista para testing en Fase 4.**
