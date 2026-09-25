# SportCourt 2.0 - Estado del Proyecto

**Última Actualización:** 2026-09-14  
**Compilación Actual:** BUILD SUCCESS ✅

---

## 📊 Estado por Fases

```
┌────────────────────────────────────────────────────────────────┐
│ FASE 1: SEGURIDAD                                              │
│ ✅ COMPLETADA                                                  │
│                                                                │
│ - ✅ BCrypt password encoder (strength 10)                     │
│ - ✅ Credenciales en variables de entorno                      │
│ - ✅ CORS configurado (localhost only)                         │
│ - ✅ Logging SQL deshabilitado                                 │
│ - ✅ AuthService para verificación segura                      │
│ - ✅ LoginResponse sin contraseña                              │
│                                                                │
│ Compilación: 18.671 segundos                                   │
│ Archivos modificados: 9                                        │
│ Líneas añadidas: 267                                           │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 2: ARQUITECTURA                                           │
│ ✅ COMPLETADA                                                  │
│                                                                │
│ - ✅ 6 DTOs con validación (CanchaDTO, ClaseDTO, etc)         │
│ - ✅ GlobalExceptionHandler centralizado                       │
│ - ✅ ErrorResponse estandarizado                               │
│ - ✅ Todos los controladores con @Valid                        │
│ - ✅ UsuarioDTO sin exponer contraseña                         │
│ - ✅ Códigos HTTP correctos (201, 204, 400, 404, 409)         │
│                                                                │
│ Compilación: 6.441 segundos                                    │
│ Archivos creados: 7                                            │
│ Archivos modificados: 5                                        │
│ Líneas añadidas: 421                                           │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 3: LÓGICA DE NEGOCIO (Service Layer)                     │
│ ✅ COMPLETADA                                                  │
│                                                                │
│ Servicios Creados:                                             │
│ - ✅ UsuarioService (búsqueda y validación)                    │
│ - ✅ CanchaService (CRUD + capacidad)                          │
│ - ✅ ClaseService (CRUD + cupos)                               │
│ - ✅ ReservaService ⭐ (5 validaciones críticas)               │
│ - ✅ InscripcionService ⭐ (4 validaciones críticas)           │
│                                                                │
│ Controladores Actualizados:                                    │
│ - ✅ CanchaController (usa CanchaService)                      │
│ - ✅ ClaseController (usa ClaseService)                        │
│ - ✅ ReservaController (usa ReservaService)                    │
│ - ✅ InscripcionController (usa InscripcionService)            │
│ - ✅ UsuarioController (usa UsuarioService)                    │
│                                                                │
│ Compilación: 6.080 segundos                                    │
│ Archivos creados: 5                                            │
│ Archivos modificados: 5                                        │
│ Líneas añadidas: 583                                           │
│ Errores corregidos: 3 (tipos LocalTime)                        │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 4: TESTING Y VALIDACIÓN                                  │
│ ✅ COMPLETADA — 2026-09-14 (unit + integración de endpoints)   │
│                                                                │
│ - ✅ 129 tests (JUnit 5 + Mockito + 1 @SpringBootTest + 1       │
│   integración CSRF end-to-end + 47 integration tests de        │
│   endpoints), 0 fallos                                         │
│ - ✅ Pruebas de duplicados y superposición de horarios         │
│ - ✅ Pruebas de capacidad/cupos                                │
│ - ✅ Pruebas de autorización por propietario (Reserva)         │
│ - ✅ Integration tests HTTP reales (controller→service→        │
│   repository→H2, sin mocks) para los 5 recursos (Cancha,       │
│   Clase, Reserva, Inscripcion, Usuario): CRUD autenticado,      │
│   validación 400, 404, conflictos 409, autorización 401/403,   │
│   vía java.net.http.HttpClient + flujo CSRF+sesión real (igual │
│   patrón que CsrfLoginFlowTest, sin spring-security-test) —    │
│   ver backend/src/test/java/.../controller/                    │
│ - ✅ BackendApplicationTests corre con H2 en memoria (perfil   │
│   "test"), sin depender de la MySQL real — apto para CI       │
│ - ✅ GitHub Actions ejecuta "mvn test" en cada push/PR         │
│   (job "validar-backend" en validar-proyecto.yml)              │
│                                                                │
│ Ver detalle en FASE4_TESTING.md                                │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 1B: ENDURECIMIENTO DE SEGURIDAD (post-Fase 1)             │
│ ✅ COMPLETADA — 2026-09-13                                     │
│                                                                │
│ - ✅ Protección CSRF con cookie token (`/api/csrf`)            │
│ - ✅ Autorización por propietario en /api/reservas/{id}        │
│   (un usuario solo ve/cancela sus propias reservas; ADMIN      │
│   puede ver todas)                                             │
│ - ✅ Autorización por rol reforzada en SecurityConfig          │
│   (ADMIN vs autenticado, por método HTTP)                      │
│ - ✅ UsuarioService.obtenerUsuarioPorEmail para resolver al     │
│   usuario autenticado desde el contexto de seguridad           │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 1C: CORS/CSRF Y XSS — ÚLTIMOS DETALLES                    │
│ ✅ COMPLETADA — 2026-09-13                                     │
│                                                                │
│ - ✅ Fix crítico de preflight: CorsConfig pasa de               │
│   WebMvcConfigurer a un bean CorsConfigurationSource            │
│   conectado en SecurityConfig vía .cors(...). Sin esa           │
│   conexión, cada preflight (OPTIONS) se trataba como petición   │
│   anónima: se le creaba una JSESSIONID nueva que pisaba la      │
│   cookie de sesión autenticada justo antes de la petición       │
│   real, tumbando con 403 cualquier llamada con headers no       │
│   "simples" (p. ej. X-XSRF-TOKEN)                               │
│ - ✅ CsrfTokenRequestAttributeHandler (sin XOR) en vez del       │
│   handler por defecto, para que el valor de la cookie           │
│   XSRF-TOKEN coincida con el que el backend espera en el        │
│   header (patrón double-submit-cookie usado por el frontend)    │
│ - ✅ admin.html ahora exige ROLE_ADMIN en SecurityConfig (antes  │
│   era público y solo el frontend lo ocultaba por rol)           │
│ - ✅ ReservaService.eliminarReserva ahora valida propietario o   │
│   ADMIN antes de borrar (antes cualquier usuario autenticado    │
│   podía eliminar la reserva de otro solo conociendo su ID)      │
│ - ✅ Frontend: helper escapeHtml() aplicado a todo el contenido  │
│   dinámico insertado vía innerHTML (nombres de cancha/clase/    │
│   usuario, descripciones, fechas) para prevenir XSS almacenado  │
│ - ✅ Frontend: getCsrfTokenAsync() pide GET /api/csrf si la      │
│   cookie XSRF-TOKEN aún no existe; todas las mutaciones         │
│   (reservar, cancelar, inscribirse, CRUD del panel admin)       │
│   envían X-XSRF-TOKEN                                           │
│ - ✅ CsrfLoginFlowTest: 4 tests de integración end-to-end        │
│   (java.net.http.HttpClient real, sin mocks) que verifican el   │
│   flujo CSRF completo contra el servidor embebido               │
│ - ✅ CI: job validar-backend separado de validar-frontend en     │
│   validar-proyecto.yml, cada uno corriendo en su propio job     │
│ - ✅ Limpieza: eliminados CreateUsuarioDTO y los métodos         │
│   encryptPassword/verifyPassword de AuthService (código muerto, │
│   el registro de usuarios no está expuesto por la API)          │
│ - ✅ assets/css/*.css movido fuera de assets/js/ a assets/css/   │
│ - ✅ Reorganización de carpetas: todo el frontend (los 7 .html,  │
│   assets/css/ y assets/js/) se movió de la raíz a frontend/,     │
│   separando claramente cliente estático y backend Spring Boot   │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 1D: MAPEO DE ROL — BD/API/FRONTEND VS. SPRING SECURITY    │
│ ✅ COMPLETADA — 2026-09-14                                     │
│                                                                │
│ Contrato del proyecto: la tabla `usuarios` (columna `rol`), la │
│ API (DTOs) y el frontend usan SIEMPRE el valor de negocio en   │
│ español y minúsculas — `"admin"` / `"usuario"` — nunca la      │
│ representación interna de Spring Security. `LoginController`  │
│ convierte ese valor a la authority `ROLE_<ROL EN MAYÚSCULAS>`  │
│ (`"admin"` → `ROLE_ADMIN`) solo en memoria, al autenticar; ese │
│ prefijo `ROLE_` nunca se guarda en la base de datos. Auditado  │
│ el flujo completo (LoginController, SecurityConfig, DTOs,      │
│ frontend, tests) sin encontrar ningún doble prefijo ni un      │
│ `hasRole`/`hasAuthority` mal formado — el diseño ya estaba bien│
│ implementado en la capa de seguridad. Se corrigieron 3 huecos: │
│                                                                │
│ - ✅ Fix: el fallback de `LoginController` para un usuario con │
│   `rol` nulo/vacío pasa de `"USER"` (inglés) a `"usuario"`,    │
│   consistente con el contrato — antes generaba ROLE_USER en    │
│   vez de ROLE_USUARIO para esos casos (sin impacto funcional   │
│   hoy porque ningún endpoint exige `hasRole("USUARIO")`, pero  │
│   era una trampa latente)                                      │
│ - ✅ Nuevo: `backend/src/main/resources/sql/                   │
│   normalize_rol_usuarios.sql` — script versionado (no había    │
│   ninguno en el repo; no hay Flyway/Liquibase ni data.sql)     │
│   que normaliza valores históricos de `rol` inconsistentes     │
│   (`'ADMIN'`, `'User'`, etc.) a `'admin'` / `'usuario'`         │
│ - ✅ Fix en tests de integración: los fixtures de               │
│   `crearUsuario(...)` en CanchaControllerTest,                 │
│   ReservaControllerTest, ClaseControllerTest,                  │
│   InscripcionControllerTest y UsuarioControllerTest guardaban  │
│   `"ADMIN"`/`"USER"` (inglés) como valor del campo `rol` en    │
│   BD — no coincidía con el contrato real que usa el frontend   │
│   (`usuario.rol === 'admin'`). Corregido a `"admin"`/          │
│   `"usuario"`; 47 tests re-verificados en verde                │
│ - ℹ️ Sin cambios (a propósito): los tests unitarios de          │
│   ReservaService e InscripcionService (`autenticarUsuario`)    │
│   construyen la `Authentication` directamente con              │
│   `"ROLE_" + rol` — ahí `"ADMIN"`/`"USER"` sí representa la    │
│   authority de Spring Security, no el valor de BD, así que     │
│   cambiarlos habría mezclado rol de negocio con authority      │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 5: FRONTEND                                               │
│ ✅ COMPLETADA — 2026-09-14 (código + verificación E2E contra    │
│    MySQL real vía backend levantado localmente)                 │
│                                                                │
│ - ✅ Eliminados todos los console.log de assets/js/app.js       │
│ - ✅ Helper parseApiError: muestra el message real del         │
│   ErrorResponse del backend (400/404/409) en vez del código    │
│   HTTP genérico, en reservas, cancelaciones, inscripciones y   │
│   CRUD del panel admin                                         │
│ - ✅ Helper asArray: valida que las respuestas de la API sean   │
│   arrays antes de usarlas en .map/.find/.some (canchas,        │
│   clases, reservas, inscripciones, usuarios)                   │
│ - ✅ Revisados los 6 DTOs del backend (Cancha, Clase, Reserva,  │
│   Inscripcion, Usuario, LoginResponse) contra su uso en el     │
│   frontend                                                      │
│ - ✅ Bug corregido: al guardar/eliminar una clase en el panel   │
│   admin, el código leía/escribía campos en español             │
│   (nombre/nivel/horario/precio/cupos) que no existen en        │
│   ClaseDTO (name/level/schedule/price/slots) → mostraba        │
│   nombre y precio en blanco o NaN. Unificado en                │
│   mapClaseDesdeAPI()                                           │
│ - ✅ Bug corregido: el formulario de reserva mostraba "Reserva  │
│   confirmada" sin esperar la respuesta del backend, incluso    │
│   si fallaba (p. ej. 409 por horario duplicado)                │
│ - ✅ localStorage ya no se usa como fuente de verdad de datos   │
│   de negocio: eliminados DEFAULT_COURTS, SEED_COURTS,          │
│   SEED_CLASSES, loadData/saveData; canchas y clases del panel  │
│   admin ahora solo vienen de MySQL vía la API                  │
│ - ✅ Verificación E2E contra MySQL real (contenedor Docker      │
│   local `mysql-sportcourt`, datos reales existentes): backend  │
│   levantado con las credenciales reales, GET /api/canchas y    │
│   /api/clases devuelven los datos reales correctamente         │
│   mapeados, login real + CSRF, creación real de una reserva y  │
│   una inscripción de prueba (con usuario y datos temporales,   │
│   eliminados al finalizar sin dejar rastro — conteos de tablas │
│   verificados idénticos antes/después)                         │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│ FASE 6: DOCUMENTACIÓN FINAL                                    │
│ ✅ COMPLETADA — 2026-09-13                                     │
│                                                                │
│ - ✅ OpenAPI/Swagger: springdoc-openapi 3.1.1 agregado,         │
│   /v3/api-docs y /swagger-ui/index.html habilitados y          │
│   permitidos en SecurityConfig; verificado en vivo (200 OK)    │
│   con perfil test (H2), 7 controladores con @Tag               │
│ - ✅ README.md (raíz): arquitectura, stack, setup, cómo correr  │
│   backend/frontend, tests, link a Swagger, roles del sistema   │
│ - ✅ docs/DEPLOYMENT.md: esquema de referencia de MySQL, vars   │
│   de entorno de producción, build/ejecución, checklist de      │
│   seguridad, ajustes localhost→dominio real pendientes de      │
│   autorización                                                  │
│ - ✅ docs/MANUAL_USUARIO.md: guía funcional por rol (Visitante, │
│   Usuario, Administrador) y mensajes de error comunes           │
└────────────────────────────────────────────────────────────────┘
```

---

## 📈 Métricas Generales

| Métrica | Valor |
|---------|-------|
| **Archivos Java (main)** | 38 |
| **Archivos Java (test)** | 13 |
| **Tests (unit + contexto Spring + integración CSRF + integración de endpoints)** | 129 (0 fallos), corren en CI |
| **Servicios implementados** | 5 |
| **Validaciones críticas** | 9 |
| **Controladores actualizados** | 5 |
| **DTOs creados** | 6 (Fase 2; hoy 5 vigentes de ese lote — `CreateUsuarioDTO` eliminado en Fase 1C, ver abajo) |
| **DTOs vigentes hoy** | 7 (5 de Fase 2 + LoginRequest + LoginResponse) |
| **Excepciones personalizadas** | 2 |

---

## 🏗️ Arquitectura Actual

```
┌─────────────────────────────────────────────────────────┐
│                   CLIENTE HTTP                          │
│              (Frontend HTML/JS/Tailwind)                │
└─────────────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────────────┐
│                   REST API GATEWAY                      │
│              (Spring Boot 4.1.1, Java 21)               │
└─────────────────────────────────────────────────────────┘
              ↓                 ↓               ↓
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │  Controllers │  │  Validación  │  │ Exception    │
    │  (5)         │  │  de DTOs     │  │ Handler      │
    │              │  │  (@Valid)    │  │              │
    └──────────────┘  └──────────────┘  └──────────────┘
              ↓
    ┌────────────────────────────────┐
    │     SERVICE LAYER              │
    │  (Lógica de Negocio)           │
    │                                │
    │  UsuarioService ⌈─────┐        │
    │  CanchaService  │     ├─→ GlobalExceptionHandler
    │  ClaseService   │     │  ├─ 400 BAD_REQUEST
    │  ReservaService ⭐ 9  │  ├─ 404 NOT_FOUND  
    │  InscripcionService ⭐ │  └─ 409 CONFLICT
    │                    ⌞─────┘   
    │                                │
    └────────────────────────────────┘
              ↓
    ┌────────────────────────────────┐
    │   REPOSITORY LAYER             │
    │   (Spring Data JPA)            │
    └────────────────────────────────┘
              ↓
    ┌────────────────────────────────┐
    │      MYSQL (sportcourt)        │
    │                                │
    │ usuarios | canchas | clases    │
    │ reservas | inscripciones       │
    └────────────────────────────────┘
```

---

## 🔐 Seguridad Implementada

| Aspecto | Estado | Detalle |
|--------|--------|---------|
| **Contraseñas** | ✅ BCrypt | Strength 10, nunca en respuestas |
| **Credenciales BD** | ✅ Vars.Env | Sin default, obligatorias (`DB_USERNAME`/`DB_PASSWORD`) |
| **CORS** | ✅ Restringido + conectado a Security | `CorsConfigurationSource` (localhost:5500 / 127.0.0.1:5500) enlazado vía `.cors(...)`, resuelve preflight antes de la sesión |
| **CSRF** | ✅ Cookie token (double-submit) | Endpoint `/api/csrf`, `CookieCsrfTokenRepository` + `CsrfTokenRequestAttributeHandler` (sin XOR) |
| **Autorización por rol** | ✅ SecurityConfig | ADMIN vs autenticado, por ruta y método HTTP; `admin.html` exige `ROLE_ADMIN` |
| **Autorización por propietario** | ✅ ReservaService | Usuario solo ve/cancela/elimina sus propias reservas (ADMIN puede todas) |
| **XSS almacenado** | ✅ escapeHtml() | Todo el contenido dinámico (nombres, descripciones, fechas) se escapa antes de insertarse en innerHTML |
| **Input Validation** | ✅ DTOs | @NotNull, @Email, @Size, etc. |
| **SQL Logging** | ✅ Deshabilitado | No expone queries |
| **Error Handling** | ✅ Genérico | No expone detalles técnicos |
| **API Responses** | ✅ Seguras | Nunca passwords en responses |

---

## 📋 Validaciones Críticas Implementadas

### ReservaService

```
✅ 1. Usuario existe → ResourceNotFoundException (404)
✅ 2. Cancha existe → ResourceNotFoundException (404)  
✅ 3. Horarios válidos → BusinessException (409)
✅ 4. NO hay duplicados → BusinessException (409)
✅ 5. Capacidad disponible → BusinessException (409)
```

**Lógica de Duplicado:** Usuario intenta 2da reserva en MISMA cancha, MISMA fecha, con horas QUE SE SUPERPONEN

### InscripcionService

```
✅ 1. Usuario existe → ResourceNotFoundException (404)
✅ 2. Clase existe → ResourceNotFoundException (404)
✅ 3. NO hay duplicada → BusinessException (409)
✅ 4. Hay cupos → BusinessException (409)
```

---

## 🐛 Issues Detectados y Resueltos

| Issue | Fase | Solución | Status |
|-------|------|----------|--------|
| Credenciales hardcodeadas | 1 | Vars. ambiente | ✅ FIXED |
| Passwords en plain text | 1 | BCrypt encoder | ✅ FIXED |
| CORS abierto a cualquiera | 1 | Whitelist localhost | ✅ FIXED |
| Sin validación de inputs | 2 | DTOs + @Valid | ✅ FIXED |
| Passwords en respuestas API | 2 | LoginResponse, UsuarioDTO sin password | ✅ FIXED |
| Errores inconsistentes | 2 | GlobalExceptionHandler | ✅ FIXED |
| Sin lógica de negocio | 3 | Service Layer | ✅ FIXED |
| Sin validación de duplicados | 3 | ReservaService + InscripcionService | ✅ FIXED |
| Sin validación de capacidad | 3 | ReservaService + InscripcionService | ✅ FIXED |
| Tipos de datos inconsistentes | 3 | LocalTime methods | ✅ FIXED |
| Reserva confirmaba éxito sin esperar al backend | 5 | async/await real + parseApiError | ✅ FIXED |
| Mismatch de campos ClaseDTO en panel admin (nombre/nivel vs name/level) | 5 | mapClaseDesdeAPI() unificado | ✅ FIXED |
| localStorage como fuente de verdad de canchas/clases (admin) | 5 | Eliminados SEED_*/loadData/saveData | ✅ FIXED |
| Respuestas de API sin validar forma antes de .map/.find | 5 | Helper asArray() | ✅ FIXED |
| Preflight (OPTIONS) creaba sesión nueva y tumbaba el login con 403 | 1C | CorsConfigurationSource conectado vía `.cors(...)` en SecurityConfig | ✅ FIXED |
| Token CSRF del body no coincidía con la cookie (handler XOR) | 1C | CsrfTokenRequestAttributeHandler | ✅ FIXED |
| admin.html accesible sin rol ADMIN a nivel de servidor | 1C | `.requestMatchers("/admin.html").hasRole("ADMIN")` | ✅ FIXED |
| Cualquier usuario autenticado podía eliminar la reserva de otro por ID | 1C | Chequeo de propietario/ADMIN en `eliminarReserva` | ✅ FIXED |
| XSS almacenado: nombres/descripciones se insertaban sin escapar en innerHTML | 1C | Helper escapeHtml() en todas las vistas dinámicas | ✅ FIXED |
| `AccessDeniedException` lanzada manualmente en ReservaController/ReservaService e InscripcionController/InscripcionService (chequeo de propietario) no tiene `@ExceptionHandler` específico en `GlobalExceptionHandler` → cae al handler genérico y respondía **500** en vez de 403/404 cuando un usuario consulta/cancela la reserva o inscripción de otro | 4 (integration tests) | `@ExceptionHandler(AccessDeniedException.class)` → 403 en GlobalExceptionHandler | ✅ FIXED — 2026-09-14 |
| `NullPointerException` al crear una reserva sobre una cancha con `capacidad` NULL en MySQL (p. ej. canchas reales id 1 y 2): el service hacía unboxing de `capacidadTotal` sin verificar null → 500 en vez de manejarlo como dato faltante | 5 (E2E contra MySQL real) | `ReservaService.validarCapacidadDisponible`: capacidad NULL se trata como 1 (uso exclusivo, comportamiento legado) | ✅ FIXED — 2026-09-14 |
| Fallback de rol vacío/nulo en `LoginController` usaba `"USER"` (inglés) en vez de `"usuario"`, inconsistente con el contrato de negocio BD/API/frontend | 1D | `rol = "usuario"` en `LoginController` | ✅ FIXED — 2026-09-14 |
| No existía script versionado para normalizar valores históricos inconsistentes de `rol` en MySQL (`'ADMIN'`, `'User'`, etc.) — la limpieza solo se había hecho manualmente contra la BD, sin quedar en el repo | 1D | `backend/src/main/resources/sql/normalize_rol_usuarios.sql` | ✅ FIXED — 2026-09-14 |
| Fixtures de 5 suites de integration tests (`crearUsuario(...)`) guardaban `"ADMIN"`/`"USER"` (inglés) como valor del campo `rol`, sin reflejar el contrato real (`"admin"`/`"usuario"`) que usa el frontend | 1D | Fixtures actualizados a `"admin"`/`"usuario"` en CanchaControllerTest, ReservaControllerTest, ClaseControllerTest, InscripcionControllerTest, UsuarioControllerTest | ✅ FIXED — 2026-09-14 |

---

## 📝 Archivos Clave del Proyecto

### Backend Java
- `backend/src/main/java/com/sportcourt/backend/`
  - `config/` - SecurityConfig, CorsConfig
  - `controller/` - 7 controladores (Cancha, Clase, Csrf, Inscripcion, Login, Reserva, Usuario)
  - `service/` - 5 servicios con lógica de negocio ⭐ + AuthService
  - `model/` - 5 entities JPA
  - `repository/` - 5 repositories
  - `dto/` - 7 DTOs con validación
  - `exception/` - ErrorResponse, ResourceNotFoundException, BusinessException, GlobalExceptionHandler
  - `BackendApplication.java` - Main app
- `backend/src/test/java/com/sportcourt/backend/` - `BackendApplicationTests` (contexto Spring, perfil
  "test" con H2) + `CsrfLoginFlowTest` (4 tests de integración end-to-end del flujo CSRF, sin mocks)
  + `service/` con 5 suites de unit tests + `controller/` con 6 suites de integración HTTP de
  endpoints (129 tests en total)

### Base de Datos
- `sportcourt` (MySQL)
  - usuarios, canchas, clases, reservas, inscripciones
- `backend/src/main/resources/sql/normalize_rol_usuarios.sql` — script manual (no hay
  Flyway/Liquibase) para normalizar valores históricos de `usuarios.rol` a `admin`/`usuario`

### Frontend (HTML/CSS/JS) — `frontend/`
- `frontend/index.html` - Home
- `frontend/login.html` - Autenticación
- `frontend/canchas.html` - Listado canchas
- `frontend/clases.html` - Listado clases
- `frontend/reservas.html` - Mis reservas (incluye inscripciones a clases)
- `frontend/perfil.html` - Perfil usuario
- `frontend/admin.html` - Panel admin
- `frontend/assets/css/`, `frontend/assets/js/app.js` - Tailwind y lógica de consumo de la API

### Configuración
- `pom.xml` - Dependencias Maven (incluye springdoc-openapi 3.1.1)
- `application.properties` - Config BD, logging
- `AGENTS.md` - Reglas del proyecto

### Documentación
- `README.md` - Setup, cómo correr el proyecto, stack, roles
- `docs/DEPLOYMENT.md` - Guía de despliegue a producción
- `docs/MANUAL_USUARIO.md` - Manual funcional por rol
- `http://localhost:8080/swagger-ui/index.html` - API interactiva (backend corriendo)

---

## ⚙️ Configuración de Build

```
Maven Version: wrapper (mvnw)
Java Version: 21
Spring Boot: 4.1.1
Validación: Jakarta Validation
Persistencia: Spring Data JPA
Base de Datos: MySQL
Build Tool: Maven
```

---

## 🎯 Próximas Acciones (Orden Prioritario)

### Fase 4: Testing
1. [x] Crear unit tests para UsuarioService
2. [x] Crear unit tests para CanchaService
3. [x] Crear unit tests para ClaseService
4. [x] Crear unit tests para ReservaService (validaciones)
5. [x] Crear unit tests para InscripcionService (validaciones)
6. [x] Integration tests para endpoints (con H2, flujo HTTP real) — 2026-09-14

### Fase 5: Frontend
1. [x] Eliminar console.log statements
2. [x] Actualizar llamadas API para nuevos DTOs (y corregir mismatches encontrados)
3. [x] Manejar errores 409 (duplicados, capacidad)
4. [x] Manejar errores 404 (recurso no encontrado)
5. [x] Validación de respuestas
6. [x] Verificación E2E con backend + MySQL real corriendo — 2026-09-14

### Fase 6: Documentación
1. [x] Generar OpenAPI/Swagger
2. [x] Actualizar README
3. [x] Guía de deployment
4. [x] Manual de usuario

### Bugs detectados y corregidos (Fase 4/5) — 2026-09-14
1. [x] `GlobalExceptionHandler`: agregado `@ExceptionHandler(AccessDeniedException.class)` → 403 (antes 500 en chequeos de propietario de Reserva/Inscripcion)
2. [x] `ReservaService`: `capacidad` NULL en `cancha` se trata como 1 en vez de lanzar NPE (antes 500 al reservar canchas reales sin capacidad definida, p. ej. id 1 y 2)

Ambos corregidos, con tests de regresión agregados (unit + integration) y reverificados en vivo
contra el MySQL real (mismo flujo de usuario de prueba con limpieza, sin dejar rastro).

---

## 📞 Soporte de Reglas de Negocio

Según AGENTS.md:
- ✅ MySQL es fuente de verdad
- ✅ Backend decide (validaciones en servicios)
- ✅ Frontend presenta
- ✅ Cambios autorizados y controlados
- ✅ Sin cambios destructivos
- ✅ Funcionalidades preservadas

---

## 🚀 Status General

```
SEGURIDAD    ████████████████████ ✅ 100%
ARQUITECTURA ████████████████████ ✅ 100%
LÓGICA NEG.  ████████████████████ ✅ 100%
TESTING      ████████████████████ ✅ 100% (unit ✅ / integración CSRF ✅ / integración endpoints ✅)
FRONTEND     ████████████████████ ✅ 100% (código ✅ / verificación E2E ✅)
DOCUMENTAC.  ████████████████████ ✅ 100%

Progreso General: ████████████████████ 100%
```

**Último BUILD:** 2026-09-25 - BUILD SUCCESS ✅ (129/129 tests, 0 fallos, corre en CI)  
**Correcciones (2026-09-25):** rotación del ID de sesión en `/api/login` (anti fijación de
sesión); errores de login con el `ErrorResponse` estándar; el estado inicial de reservas
(`confirmada`) e inscripciones (`inscrita`) lo asigna el backend y la edición conserva el
estado; un ADMIN puede editar reservas ajenas conservando al dueño; bloqueo pesimista
(`SELECT ... FOR UPDATE`) de la cancha/clase para no superar capacidad/cupos con peticiones
simultáneas; `InscripcionService` consulta en BD en vez de `findAll()`; clase con cupos NULL
responde 409 en vez de 500.  
**Seguridad (Fase 1C):** fix de preflight CORS/CSRF que tumbaba el login con 403, admin.html
restringido a ROLE_ADMIN en el servidor, autorización por propietario también en
`eliminarReserva`, y escapeHtml() contra XSS almacenado en todas las vistas dinámicas del
frontend.  
**Frontend:** console.log eliminados, errores 404/409 mostrados con el mensaje real del backend,
respuestas de API validadas antes de usarse, mismatches de DTO en Clase corregidos, localStorage
ya no es fuente de verdad de negocio.  
**Documentación:** OpenAPI/Swagger habilitado y verificado en vivo, README.md, guía de
deployment y manual de usuario creados.  
**Testing (2026-09-14):** 47 integration tests HTTP nuevos (controller→service→repository→H2,
sin mocks) para Cancha, Clase, Reserva, Inscripcion y Usuario, sumando 118/118 tests en CI.  
**Verificación E2E (2026-09-14):** backend levantado contra el MySQL real (`sportcourt`, vía
contenedor Docker local `mysql-sportcourt`) — lecturas (`/api/canchas`, `/api/clases`, `/api/csrf`,
autorización 403 sin sesión) y un flujo de escritura completo (usuario de prueba → login real →
crear reserva → crear inscripción → verificar → eliminar todo) confirmados correctos, sin dejar
datos residuales (conteos de tablas verificados idénticos antes/después).  
**Bugs detectados y corregidos (2026-09-14):**
1. `AccessDeniedException` de los chequeos de propietario (Reserva/Inscripcion) no tenía handler
   específico → respondía 500 en vez de 403. Fix: `@ExceptionHandler(AccessDeniedException.class)`
   en `GlobalExceptionHandler`.
2. Crear una reserva sobre una cancha con `capacidad` NULL en MySQL (canchas reales id 1 y 2)
   lanzaba `NullPointerException` → 500. Fix: `ReservaService` trata `capacidad` NULL como 1.
   Ambos con tests de regresión (unit + integration) y reverificados en vivo contra el MySQL real.  
**Mapeo de roles (Fase 1D, 2026-09-14):** auditoría completa del contrato de dos capas
BD/API/frontend (`"admin"`/`"usuario"`, minúsculas y en español) vs. authority interna de
Spring Security (`ROLE_ADMIN`/`ROLE_USUARIO`, construida solo en memoria en `LoginController`).
La capa de seguridad ya estaba bien implementada (sin doble prefijo, sin `hasRole` mal
formado); se corrigieron 3 huecos menores: el fallback de rol vacío en `LoginController`
(`"USER"` → `"usuario"`), un script SQL versionado nuevo para normalizar valores históricos de
`rol` (`backend/src/main/resources/sql/normalize_rol_usuarios.sql`, no existía ninguno en el
repo), y los fixtures de 5 suites de integration tests que usaban `"ADMIN"`/`"USER"` como valor
de BD en vez de `"admin"`/`"usuario"`. No se tocaron los tests unitarios de ReservaService/
InscripcionService que usan `"ROLE_" + rol` para construir la `Authentication` directamente:
ahí `"ADMIN"`/`"USER"` sí es la authority de Spring Security, no el valor de negocio. 47 tests
re-verificados en verde tras el cambio.  
**Próximo paso:** ninguno bloqueante — proyecto al 100% en las 7 fases, sin issues abiertos
conocidos.
