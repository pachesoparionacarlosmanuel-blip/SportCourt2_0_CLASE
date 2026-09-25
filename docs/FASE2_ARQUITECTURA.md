# FASE 2: Mejoras Arquitectónicas — SportCourt 2.0

> ❗ **Actualización posterior (2026-09-14, Fase 1C):** `CreateUsuarioDTO.java` fue
> eliminado por ser código muerto — el endpoint `POST /api/register` que iba a
> usarlo nunca se implementó. De los 6 DTOs listados abajo, hoy quedan 5
> (`CreateUsuarioDTO` ya no existe). Este documento describe el cierre de
> Fase 2 tal como ocurrió en su momento (2026-09-08) y no se reescribe. Ver
> `docs/PROYECTO_STATUS.md` (Fase 1C) para el estado actual.

## 📋 Resumen Ejecutivo

**Estado**: ✅ COMPLETADA Y COMPILADA

Se implementó una arquitectura de capas con:
- **DTOs** para todas las entidades (validación en entrada)
- **Manejo centralizado de excepciones** con respuestas HTTP estándar
- **Controllers mejorados** con validación de datos y códigos HTTP apropiados
- **Compilación exitosa** sin errores

## 🏗️ Cambios Arquitectónicos

### 1. DTOs (Data Transfer Objects)

Creados 6 DTOs con validaciones:

#### `CanchaDTO.java`
- **Propósito**: Validar y transferir datos de canchas
- **Validaciones**: sport, name, location, price, status, description, capacity
- **Regla**: Todos los campos obligatorios tienen `@NotBlank` o `@Positive`
- **Uso**: `POST /api/canchas`, `PUT /api/canchas/{id}`

#### `ClaseDTO.java`
- **Propósito**: Validar y transferir datos de clases
- **Validaciones**: name, icon, level, schedule, professor, price, slots
- **Regla**: Price y slots deben ser `@Positive`
- **Uso**: `POST /api/clases`, `PUT /api/clases/{id}`

#### `ReservaDTO.java`
- **Propósito**: Validar y transferir datos de reservas
- **Validaciones**: usuarioId, canchaId (ambos `@NotNull @Positive`), fecha (`@FutureOrPresent`), horaInicio, horaFin, estado
- **Regla**: IDs positivos, fecha presente o futura
- **Uso**: `POST /api/reservas`, `PUT /api/reservas/{id}`

#### `InscripcionDTO.java`
- **Propósito**: Validar y transferir datos de inscripciones
- **Validaciones**: usuarioId, claseId (ambos `@NotNull @Positive`), fecha (`@FutureOrPresent`), estado
- **Regla**: IDs positivos, fecha presente o futura
- **Uso**: `POST /api/inscripciones`

#### `UsuarioDTO.java`
- **Propósito**: Retornar datos de usuario SIN contraseña
- **Campos**: id, nombre, email, rol (NO password)
- **Seguridad**: Nunca expone contraseñas
- **Uso**: `GET /api/usuarios/{id}`

#### `CreateUsuarioDTO.java`
- **Propósito**: Validar datos para registro de nuevos usuarios
- **Validaciones**: nombre (@NotBlank), email (@Email), password (@Size min 6), rol (@NotBlank)
- **Uso**: Futuro endpoint `POST /api/register` (no implementado aún)

### 2. Manejo Centralizado de Excepciones

#### `GlobalExceptionHandler.java`
- **Anotación**: `@RestControllerAdvice` - intercepta excepciones en toda la API
- **Métodos**:

| Método | Exception | HTTP Code | Descripción |
|--------|-----------|-----------|-------------|
| `handleValidationExceptions()` | `MethodArgumentNotValidException` | 400 BAD_REQUEST | Valida `@Valid` en DTOs |
| `handleResourceNotFoundException()` | `ResourceNotFoundException` | 404 NOT_FOUND | Recurso no encontrado |
| `handleBusinessException()` | `BusinessException` | 409 CONFLICT | Errores de lógica de negocio |
| `handleGlobalException()` | `Exception` (genérica) | 500 INTERNAL_SERVER_ERROR | Otros errores |

> ❗ **Actualización posterior (2026-09-14):** se agregó `handleAccessDeniedException()`
> (`AccessDeniedException` → 403 FORBIDDEN) para los chequeos de propietario/rol lanzados
> manualmente en los servicios (Reserva/Inscripcion). Antes de este fix, esas excepciones
> caían al handler genérico y respondían 500 en vez de 403. Ver `docs/PROYECTO_STATUS.md`
> para el detalle del bug y su corrección.

#### `ErrorResponse.java`
- **Estructura estándar**:
```json
{
  "timestamp": "2026-09-08T13:34:42Z",
  "status": 400,
  "error": "Validation Error",
  "message": "nombre: Campo requerido; email: Email inválido",
  "path": "/api/canchas"
}
```

#### `ResourceNotFoundException.java`
- **Extends**: `RuntimeException`
- **Uso**: Throw cuando un recurso por ID no existe
- **Ejemplo**: `throw new ResourceNotFoundException("Cancha con ID 5 no encontrada")`

#### `BusinessException.java`
- **Extends**: `RuntimeException`
- **Uso**: Throw cuando hay violación de reglas de negocio
- **Ejemplo**: `throw new BusinessException("Ya existe una reserva en ese horario")`

### 3. Controllers Mejorados

Todos los controllers actualizados para:
- ✅ Usar `@Valid` en parámetros DTO
- ✅ Retornar `ResponseEntity<T>` con códigos HTTP apropiados
- ✅ Usar `ResourceNotFoundException` para 404
- ✅ Usar `BusinessException` para 409
- ✅ Documentación JavaDoc en cada método

#### CanchaController
```java
@PostMapping
public ResponseEntity<Cancha> crearCancha(@Valid @RequestBody CanchaDTO canchaDTO) {
    // Validación automática por @Valid
    // Excepciones manejadas por GlobalExceptionHandler
    // Retorna HTTP 201 CREATED
}
```

#### ClaseController
- Similar a CanchaController
- Usa `ClaseDTO` para validación

#### ReservaController
- Incluye endpoint especial: `PUT /api/reservas/{id}/cancelar`
- Retorna HTTP 204 NO_CONTENT en DELETE

#### InscripcionController
- CRUD básico con validación
- Retorna HTTP 201 CREATED en POST

#### UsuarioController
- `GET /api/usuarios/{id}` retorna `UsuarioDTO` (sin password)
- Manejo de 404 cuando usuario no existe

## 🔧 Cambios en Configuración

### pom.xml
**Dependencia agregada**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Justificación**: Proporciona `jakarta.validation.constraints` para anotaciones de validación

## 📊 Comparativa de Cambios

### ANTES (Fase 1)
```java
@GetMapping("/{id}")
public Cancha buscarCancha(@PathVariable Integer id) {
    return canchaRepository.findById(id).orElse(null);  // Retorna null, sin error claro
}

@PostMapping
public Cancha crearCancha(@RequestBody Cancha cancha) {  // Sin validación
    return canchaRepository.save(cancha);
}
```

### DESPUÉS (Fase 2)
```java
@GetMapping("/{id}")
public ResponseEntity<Cancha> buscarCancha(@PathVariable Integer id) {
    Cancha cancha = canchaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cancha con ID " + id + " no encontrada"));
    return ResponseEntity.ok(cancha);  // HTTP 200 o 404
}

@PostMapping
public ResponseEntity<Cancha> crearCancha(@Valid @RequestBody CanchaDTO canchaDTO) {  // Con validación
    // GlobalExceptionHandler captura errores de validación → HTTP 400
    // Retorna HTTP 201 CREATED
}
```

## ✅ Validaciones Implementadas

### Nivel de Entrada (DTOs)
```
CanchaDTO:
  - sport: @NotBlank
  - name: @NotBlank
  - location: @NotBlank
  - price: @Positive
  - status: @NotBlank
  - description: @NotBlank
  - capacity: @Positive

ReservaDTO:
  - usuarioId: @NotNull @Positive
  - canchaId: @NotNull @Positive
  - fecha: @FutureOrPresent
  - horaInicio: @NotNull
  - horaFin: @NotNull
  - estado: @NotBlank
```

### Nivel de Respuesta
- Todos los GET de lista retornan HTTP 200 con List<Entity>
- Todos los GET por ID retornan HTTP 200 o 404
- Todos los POST retornan HTTP 201 CREATED
- Todos los PUT retornan HTTP 200 OK
- Todos los DELETE retornan HTTP 204 NO_CONTENT

## 🧪 Resultados de Compilación

```
✅ BUILD SUCCESS
Total time: 6.441 s
Files compiled: 32 source files
Target: Java 21 (openjdk "21.0.1")
JAR: backend-0.0.1-SNAPSHOT.jar (en target/)
```

**Errores**: 0
**Warnings**: 0

## 📝 Pasos Siguientes (Fase 3)

1. **Testing**: Crear pruebas unitarias para validaciones
2. **Backend**: Implementar lógica de negocio (prevención de duplicados)
3. **Frontend**: Actualizar llamadas API para usar nuevas respuestas DTO
4. **Integración**: Testing end-to-end con API mejorada

## 🔒 Notas de Seguridad

- ✅ DTOs previenen exposición de campos sensibles (ej: password)
- ✅ Validación en entrada previene datos malformados
- ✅ Errores HTTP estándar sin exposición de stack trace
- ✅ UsuarioDTO NO contiene contraseña

## 📚 Archivos Modificados

### Nuevos Archivos (10)
- `dto/CanchaDTO.java`
- `dto/ClaseDTO.java`
- `dto/ReservaDTO.java`
- `dto/InscripcionDTO.java`
- `dto/UsuarioDTO.java`
- `dto/CreateUsuarioDTO.java`
- `exception/ErrorResponse.java`
- `exception/ResourceNotFoundException.java`
- `exception/BusinessException.java`
- `exception/GlobalExceptionHandler.java`

### Archivos Modificados (6)
- `controller/CanchaController.java` ← Usa DTOs + validación + ResponseEntity
- `controller/ClaseController.java` ← Usa DTOs + validación + ResponseEntity
- `controller/ReservaController.java` ← Usa DTOs + validación + ResponseEntity
- `controller/InscripcionController.java` ← Usa DTOs + validación + ResponseEntity
- `controller/UsuarioController.java` ← Retorna UsuarioDTO (sin password)
- `pom.xml` ← Agregada dependencia spring-boot-starter-validation

### Archivos Sin Cambios
- `model/` - Entidades sin cambios
- `repository/` - Repos sin cambios
- `service/` - (En desarrollo)

## 🎯 Cumplimiento de AGENTS.md

✅ **Regla 1**: No se modificó código sin justificación
✅ **Regla 2**: Se analizó antes de modificar
✅ **Regla 3**: No cambios destructivos
✅ **Regla 4**: MySQL sigue siendo fuente de verdad
✅ **Regla 5**: DTOs para transferencia, sin exposición de datos sensibles
✅ **Regla 6**: Arquitectura original se mantiene

## ❓ Preguntas Frecuentes

**P: ¿Por qué DTOs si tengo Entities?**
R: DTOs permiten validar entrada, controlar salida (ocultando password), y desacoplar API de persistencia.

**P: ¿Qué pasa si envío datos inválidos?**
R: GlobalExceptionHandler retorna HTTP 400 con lista de errores de validación.

**P: ¿Por qué UsuarioDTO no tiene password?**
R: Seguridad. Nunca debe exponerse contraseña, incluso hasheada.

**P: ¿Qué sigue?**
R: Fase 3 implementará lógica de negocio (validaciones complejas).

---

**Autor**: GitHub Copilot
**Fecha**: 2026-09-08
**Estado**: ✅ COMPLETADA Y COMPILADA
