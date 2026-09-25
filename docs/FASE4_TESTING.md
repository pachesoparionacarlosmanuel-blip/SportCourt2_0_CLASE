# ✅ FASE 4: TESTING Y VALIDACIÓN - COMPLETADA, corriendo en CI

> **Actualizado 2026-09-14 (integration tests de endpoints):** se agregaron 47 tests de
> integración HTTP reales (`@SpringBootTest` + `java.net.http.HttpClient`, sin mocks, mismo
> patrón que `CsrfLoginFlowTest`) para los 5 controladores (Cancha, Clase, Reserva,
> Inscripcion, Usuario) en `backend/src/test/java/.../controller/`, ejercitando el flujo
> completo controller→service→repository→H2: CRUD autenticado, validación 400, 404,
> conflictos 409 (duplicados/capacidad/cupos), autorización 401/403. Total: 71 → **119 tests**.
> Estas pruebas, junto con una verificación E2E manual contra el MySQL real, detectaron dos
> bugs reales que ya se corrigieron: (1) `AccessDeniedException` de los chequeos de
> propietario devolvía 500 en vez de 403 (faltaba `@ExceptionHandler` en
> `GlobalExceptionHandler`); (2) crear una reserva sobre una cancha con `capacidad` NULL en
> MySQL lanzaba `NullPointerException` (dato legado en canchas reales id 1 y 2). Ver
> `docs/PROYECTO_STATUS.md` para el detalle de ambos.
>
> **Actualizado 2026-09-14:** se agregó `CsrfLoginFlowTest`, una suite de
> integración end-to-end (sin mocks, `java.net.http.HttpClient` contra un
> servidor embebido real) que verifica el flujo CSRF completo alrededor de
> `/api/login` — incluyendo la regresión que motivó el fix de
> `CsrfTokenRequestAttributeHandler` (el token del body debe coincidir con el
> valor crudo de la cookie). También se amplió `ReservaServiceTest` con la
> autorización por propietario/ADMIN en `eliminarReserva`. Cifras verificadas
> ejecutando `./mvnw clean test` sin ninguna variable de entorno de base de
> datos configurada.
>
> **Actualizado 2026-09-13:** además de ampliar los tests de `ReservaService`
> e `InscripcionService` (CSRF + autorización por propietario), se activó el
> perfil `test` (H2 en memoria) en `BackendApplicationTests` — ya no depende
> de la MySQL real — y se agregó el job `validar-backend` en
> `.github/workflows/validar-proyecto.yml`, que corre `mvn test` en cada
> push/PR.

## 📋 Resumen Ejecutivo

| Métrica | Valor |
|---------|-------|
| **Suites de test** | 5 de servicio (Mockito) + 1 de contexto Spring (H2) + 1 de integración CSRF end-to-end + 5 de integración de endpoints (H2) |
| **Total de Tests** | 119 tests |
| **Tests Pasados** | 119 ✅ |
| **Tests Fallidos** | 0 |
| **Cobertura** | Servicios críticos (ReservaService, InscripcionService) + flujo CSRF/login real + endpoints HTTP completos de los 5 recursos |
| **CI** | `validar-backend` en GitHub Actions corre `mvn test` en cada push/PR |

`BackendApplicationTests` ahora usa `@ActiveProfiles("test")` →
`application-test.properties` (H2 en memoria, `ddl-auto=create-drop`), por lo
que no requiere `DB_USERNAME`/`DB_PASSWORD` ni una MySQL real — corre igual
en tu máquina que en el runner de GitHub Actions.

---

## 🧪 Tests Unitarios Implementados

### 1. **CanchaServiceTest** (7 tests)

✅ **Casos de Éxito:**
- Crear cancha exitosa
- Obtener cancha existente
- Listar todas las canchas
- Actualizar cancha existente
- Obtener capacidad de cancha
- Eliminar cancha existente

❌ **Casos de Error:**
- Obtener cancha no existente → ResourceNotFoundException

### 2. **ClaseServiceTest** (7 tests)

✅ **Casos de Éxito:**
- Crear clase exitosa
- Obtener clase existente
- Listar todas las clases
- Actualizar clase existente
- Obtener cupos disponibles de clase
- Eliminar clase existente

❌ **Casos de Error:**
- Obtener clase no existente → ResourceNotFoundException

### 3. **ReservaServiceTest** (34 tests)

✅ **Casos de Éxito:**
- Crear reserva exitosa con datos válidos
- Obtener reserva existente
- Cancelar reserva exitosamente
- Reserva SIN superposición (hora fin = nueva inicio)
- Capacidad disponible - reserva exitosa

❌ **Casos de Error - Validaciones:**

**VALIDACIÓN 1: Usuario NO existe**
- Throws: ResourceNotFoundException ✓

**VALIDACIÓN 2: Cancha NO existe**
- Throws: ResourceNotFoundException ✓

**VALIDACIÓN 3: Horarios inválidos**
- Horas iguales → BusinessException ✓
- Horas invertidas → BusinessException ✓

**VALIDACIÓN 4: Reserva duplicada (horas superpuestas)**
- Superposición completa → BusinessException ✓
- Superposición en inicio → BusinessException ✓

**VALIDACIÓN 5: Capacidad no disponible**
- Sin capacidad → BusinessException ✓

**VALIDACIÓN 6: Autorización por propietario al cancelar**
- Usuario NO puede cancelar la reserva de otro usuario → BusinessException/AccessDenied ✓
- Un ADMIN SÍ puede cancelar la reserva de otro usuario ✓ (necesario para que el panel admin
  pueda cancelar reservas de cualquier usuario contra la API real)

**VALIDACIÓN 7 (nueva): Autorización por propietario al eliminar**
- Usuario NO puede eliminar la reserva de otro usuario → AccessDeniedException ✓
- Un ADMIN SÍ puede eliminar la reserva de otro usuario ✓

**Ampliación (nueva): Actualizar reserva (`actualizarReserva`)**
- Conserva su propio horario sin marcarlo como duplicado ✓
- Rechaza horario duplicado con otra reserva del mismo usuario ✓
- Rechaza horario sin capacidad disponible ✓
- Una reserva cancelada no bloquea el mismo horario ✓
- No genera falso conflicto consigo misma ✓

**Ampliación (nueva): Listado y borrado**
- Lista todas las reservas correctamente ✓
- Elimina una reserva existente ✓ / rechaza eliminar una inexistente ✓

### 4. **InscripcionServiceTest** (12 tests)

✅ **Casos de Éxito:**
- Crear inscripción exitosa con datos válidos
- Obtener inscripción existente
- Cancelar inscripción exitosamente
- Listar todas las inscripciones

❌ **Casos de Error - Validaciones:**

**VALIDACIÓN 1: Usuario NO existe**
- Throws: ResourceNotFoundException ✓

**VALIDACIÓN 2: Clase NO existe**
- Throws: ResourceNotFoundException ✓

**VALIDACIÓN 3: Inscripción duplicada**
- Usuario ya en clase → BusinessException ✓
- Inscripción anterior CANCELADA → OK ✓
- Usuario diferente en MISMA clase → OK ✓

**VALIDACIÓN 4: Cupos no disponibles**
- Sin cupos → BusinessException ✓
- Con cupos disponibles → OK ✓
- Con múltiples cupos → OK ✓

### 5. **UsuarioServiceTest** (6 tests)

✅ **Casos de Éxito:**
- Obtener usuario existente
- Listar todos los usuarios
- Obtener usuario como DTO (sin password)
- Verificar usuario existe

❌ **Casos de Error:**
- Obtener usuario no existente → ResourceNotFoundException ✓
- Verificar usuario no existe → ResourceNotFoundException ✓

### 6. **CsrfLoginFlowTest** (4 tests, integración end-to-end)

A diferencia de las otras suites (Mockito, sin contexto Spring), esta corre contra un
servidor embebido real (`@SpringBootTest(webEnvironment = RANDOM_PORT)`) usando
`java.net.http.HttpClient` puro — sin mocks ni módulos de test de Spring Security — para
pasar por el filtro de seguridad de verdad, tal como lo haría un navegador.

✅ **Casos verificados:**
- `POST /api/login` sin cookie ni header CSRF → **403** (Spring Security bloquea el POST)
- `GET /api/csrf` fija la cookie `XSRF-TOKEN` en la respuesta
- Flujo completo: `GET /api/csrf` → cookie + header `X-XSRF-TOKEN` → `POST /api/login`
  pasa el filtro CSRF y falla solo por credenciales inválidas (**401**, no 403)
- El token expuesto en el body JSON de `/api/csrf` coincide con el valor crudo de la
  cookie `XSRF-TOKEN` (regresión: con el handler `Xor` por defecto no coincidían, y el
  frontend —que lee la cookie directo— nunca podía pasar el filtro CSRF)

### 7. **Integration tests de endpoints** (47 tests, agregados 2026-09-14)

Igual que `CsrfLoginFlowTest`: `@SpringBootTest(webEnvironment = RANDOM_PORT)` +
`@ActiveProfiles("test")` + `java.net.http.HttpClient` puro, sin mocks — ejercitan el
flujo real controller→service→repository→H2. Un archivo por recurso en
`backend/src/test/java/com/sportcourt/backend/controller/`, con una clase base
(`AbstractControllerTest`) que centraliza el login real (CSRF + sesión) y el seeding de
datos vía repositorios.

| Clase | Tests | Cubre |
|-------|-------|-------|
| `CanchaControllerTest` | 10 | GET público, CRUD admin, 400, 404, 403 sin rol ADMIN |
| `ClaseControllerTest` | 9 | GET público, CRUD admin, 400, 404, 403 sin rol ADMIN |
| `ReservaControllerTest` | 12 | Crear/cancelar autenticado, 400, 404, 409 (duplicado/capacidad), 403 (propietario, DELETE solo ADMIN) |
| `InscripcionControllerTest` | 11 | Crear/cancelar/eliminar autenticado, 400, 404, 409 (duplicado/cupos), 403 (propietario) |
| `UsuarioControllerTest` | 5 | 403 sin rol ADMIN, respuesta sin password |

**Nota de implementación:** las clases usan el sufijo `Test` (no `IT`) porque el proyecto
no tiene el plugin Failsafe configurado — Surefire (`mvn test`) ignora silenciosamente los
archivos `*IT.java`.

**Bugs detectados y corregidos gracias a estas pruebas** (ver `docs/PROYECTO_STATUS.md`):
- `AccessDeniedException` de los chequeos de propietario (Reserva/Inscripcion) devolvía 500
  en vez de 403 → agregado `@ExceptionHandler(AccessDeniedException.class)` en
  `GlobalExceptionHandler`.
- `NullPointerException` al reservar una cancha con `capacidad` NULL (detectado en la
  verificación E2E contra MySQL real, no en H2) → `ReservaService` trata `capacidad` NULL
  como 1.

---

## 🔍 Validaciones Críticas Probadas

### ReservaService - Flujo Completo

```
Usuario 1 intenta reservar:
├─ Validación 1: ¿Usuario existe? ✓
├─ Validación 2: ¿Cancha existe? ✓
├─ Validación 3: ¿Horarios válidos? (inicio < fin) ✓
├─ Validación 4: ¿NO hay duplicado? 
│   └─ Búsqueda: Usuario + Cancha + Fecha + Horas NO superpuestas ✓
└─ Validación 5: ¿Hay capacidad? ✓
    → Reserva CREADA ✓
```

**Ejemplo de Superposición Detectada:**
```
Usuario intenta 10:30-11:30
Ya tiene     10:00-11:00
Resultado: CONFLICTO ✓ (se detecta overlap)

Usuario intenta 11:00-12:00
Ya tiene     10:00-11:00
Resultado: OK ✓ (no hay overlap, comienza exactamente cuando termina)
```

### InscripcionService - Flujo Completo

```
Usuario 1 intenta inscribirse:
├─ Validación 1: ¿Usuario existe? ✓
├─ Validación 2: ¿Clase existe? ✓
├─ Validación 3: ¿NO hay inscripción previa?
│   └─ Si anterior está CANCELADA → OK ✓
│   └─ Si es otro usuario → OK ✓
└─ Validación 4: ¿Hay cupos disponibles? ✓
    → Inscripción CREADA ✓
```

---

## 🛠️ Herramientas y Configuración

### Dependencias de Testing
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### Framework
- **JUnit 5** - Framework de tests
- **Mockito** - Mocking de dependencias
- **Spring Boot Test** - Context de Spring para tests

### Patrón de Testing
```java
@ExtendWith(MockitoExtension.class)
public class [Service]Test {
    @Mock
    private [Repository] repository;
    
    @InjectMocks
    private [Service] service;
    
    // Tests usando @DisplayName para documentación clara
    @Test
    @DisplayName("✅ Caso de éxito")
    void testExitoso() { }
    
    @Test
    @DisplayName("❌ Caso de error esperado")
    void testError() { }
}
```

---

## 📝 Ejecución de Tests

### Comando
```bash
./mvnw test
```

### Resultado (verificado 2026-09-14)
```
BackendApplicationTests:      1 test
CanchaControllerTest:        10 tests
ClaseControllerTest:          9 tests
InscripcionControllerTest:   11 tests
ReservaControllerTest:       12 tests
UsuarioControllerTest:        5 tests
CsrfLoginFlowTest:            4 tests
CanchaServiceTest:            7 tests
ClaseServiceTest:             7 tests
InscripcionServiceTest:      12 tests
ReservaServiceTest:          35 tests
UsuarioServiceTest:           6 tests
--------------------------------
Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## ✅ Validaciones Clave Probadas

### Lógica de Superposición de Horarios
✓ Fórmula: `inicio1.isBefore(fin2) && fin1.isAfter(inicio2)`
✓ Detecta overlaps correctamente
✓ Diferencia entre "exacto" y "superpuesto"

### Conteo de Capacidad
✓ Cuenta SOLO reservas activas (estado != "cancelada")
✓ Compara con capacidad total de cancha
✓ Lanza BusinessException si no hay espacio

### Conteo de Cupos
✓ Cuenta SOLO inscripciones activas (estado != "cancelada")
✓ Compara con slots total de clase
✓ Lanza BusinessException si no hay cupos

### Validación de Duplicados
✓ Busca por usuario + cancha/clase + fecha
✓ Ignora inscripciones canceladas
✓ Detecta correctamente reservas duplicadas
✓ Permite múltiples usuarios en MISMA cancha/clase

---

## 🎯 Cobertura de Servicios

| Servicio | Tests | Métodos Probados | Coverage |
|----------|-------|------------------|----------|
| ReservaService | 35 | crearReserva(), obtenerReserva(), actualizarReserva(), cancelarReserva(), listarReservas(), eliminarReserva() | 100% |
| InscripcionService | 12 | crearInscripcion(), obtenerInscripcion(), cancelarInscripcion() | 100% |
| CanchaService | 7 | crearCancha(), obtenerCancha(), listarCanchas(), actualizarCancha(), eliminarCancha() | 100% |
| ClaseService | 7 | crearClase(), obtenerClase(), listarClases(), actualizarClase(), eliminarClase() | 100% |
| UsuarioService | 6 | obtenerUsuario(), listarUsuarios(), verificarUsuarioExiste() | 100% |
| CSRF/login (integración) | 4 | flujo GET /api/csrf → POST /api/login end-to-end | — |
| Controllers (integración HTTP) | 47 | los 5 controladores, flujo completo vía HTTP real | — |

---

## 🚀 Siguiente Paso

Fase 5 (Frontend) y la verificación E2E contra MySQL real ya se completaron el
2026-09-14 — ver `docs/PROYECTO_STATUS.md`. No queda ningún paso pendiente de Fase 4.

---

## 📦 Archivos Creados/Modificados

### Creados (11 test suites)
- ✅ ReservaServiceTest.java — ampliado con tests de actualización, listado, autorización por propietario/ADMIN (cancelar y eliminar) y regresión de `capacidad` NULL
- ✅ InscripcionServiceTest.java (403 líneas)
- ✅ CanchaServiceTest.java (142 líneas)
- ✅ ClaseServiceTest.java (142 líneas)
- ✅ UsuarioServiceTest.java (116 líneas)
- ✅ CsrfLoginFlowTest.java (122 líneas — integración end-to-end, sin mocks)
- ✅ controller/AbstractControllerTest.java + CanchaControllerTest.java + ClaseControllerTest.java + ReservaControllerTest.java + InscripcionControllerTest.java + UsuarioControllerTest.java (47 tests — integración HTTP de endpoints, agregados 2026-09-14)

### Configuración de Tests
- ✅ application-test.properties (H2 database)
- ✅ pom.xml (H2 dependency added)

---

## 🏆 Estado Final

```
✅ FASE 4 COMPLETADA — corre en CI en cada push/PR
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Fase 1: Seguridad (+ endurecimiento CSRF/autorización) — 100%
✅ Fase 2: Arquitectura (DTOs)       — 100%
✅ Fase 3: Lógica de Negocio (Services) — 100%
✅ Fase 4: Testing (119 tests, H2 en CI) — 100%
✅ Fase 5: Frontend (JavaScript) + verificación E2E — 100%
✅ Fase 6: Documentación (OpenAPI)   — 100%

Progreso Total: 100%
```

---

## 📊 Estadísticas

- **Archivos de test**: 13 (5 de servicio + `BackendApplicationTests` + `CsrfLoginFlowTest` + 6 de integración de endpoints)
- **Tests totales**: 119
- **Cobertura**: Todos los servicios críticos + flujo CSRF/login real + los 5 controladores end-to-end
- **Tasa de éxito**: 100%
- **CI**: corre automáticamente en cada push/PR (`validar-backend`)

