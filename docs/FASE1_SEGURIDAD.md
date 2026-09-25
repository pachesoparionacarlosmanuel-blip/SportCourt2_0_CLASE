# FASE 1: SEGURIDAD — SportCourt 2.0

**Estado:** ✅ **COMPLETADA**
**Fecha:** 2026-09-09
**Compilación:** BUILD SUCCESS

> ❗ **Nota sobre este documento:** existió un `FASE1_SEGURIDAD.md` (y un `RESUMEN_FASE1.md`)
> en la raíz del repositorio desde el commit original de Fase 1, pero se eliminaron el
> 2026-09-10 (`security: remove hardcoded credentials and obsolete docs`) porque el
> documento mostraba en texto plano la contraseña de MySQL que en ese momento estaba
> hardcodeada en `application.properties` — exactamente el problema que esa misma fase
> venía a corregir. Este documento se reconstruye el 2026-09-14 a partir del historial de
> git y del estado actual del código, sin reproducir esa credencial, y siguiendo el mismo
> formato que `FASE2_ARQUITECTURA.md`, `FASE3_LOGICA_NEGOCIO.md` y `FASE4_TESTING.md`.
>
> **Actualización posterior (Fase 1B/1C, 2026-09-13):** Fase 1 cubrió la base
> (credenciales, BCrypt, CORS, logs). El endurecimiento posterior —CSRF con cookie token,
> autorización por propietario en reservas/inscripciones, `admin.html` restringido a
> `ROLE_ADMIN` a nivel de servidor, CSP, y `escapeHtml()` contra XSS almacenado en el
> frontend— se documenta como Fase 1B/1C en `docs/PROYECTO_STATUS.md`, no aquí.
>
> **Actualización posterior (Fase 1D, 2026-09-14):** ajuste del mapeo entre el valor de
> negocio del rol (`admin`/`usuario`, guardado en la columna `rol` de MySQL) y la authority
> interna de Spring Security (`ROLE_ADMIN`/`ROLE_USUARIO`, construida en `LoginController` y
> usada solo en memoria vía `hasRole(...)`) — ver Fase 1D en `docs/PROYECTO_STATUS.md`.

---

## 🎯 Objetivo

Corregir los riesgos de seguridad más críticos detectados en la base del proyecto antes de
seguir construyendo funcionalidad sobre ella:

- Credenciales de base de datos hardcodeadas en el código fuente.
- Contraseñas de usuario almacenadas y comparadas en texto plano.
- CORS abierto a cualquier origen (`origins = "*"`).
- Logging de SQL activado (expone la estructura y los datos de las queries).

---

## 📋 Resumen Ejecutivo

| Cambio | Antes | Después |
|--------|-------|---------|
| Credenciales de BD | Hardcodeadas en `application.properties` | Variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), sin valor por defecto para la contraseña |
| Contraseñas de usuario | Texto plano, comparación directa | BCrypt (`BCryptPasswordEncoder`, strength 10) |
| CORS | `@CrossOrigin(origins = "*")` en cada controller | Configuración centralizada en `CorsConfig.java`, whitelist de orígenes locales |
| Logging SQL | `spring.jpa.show-sql=true` | `spring.jpa.show-sql=false` |
| Respuesta de login | Podía incluir datos sensibles del usuario | `LoginResponse` dedicado, sin contraseña |

---

## 🔐 1. Credenciales de Base de Datos Protegidas

**Archivo:** `backend/src/main/resources/application.properties`

**Antes:** usuario y contraseña de MySQL escritos directamente como texto plano en el
archivo, versionado en git.

**Después:**
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/sportcourt}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

`DB_USERNAME` y `DB_PASSWORD` son obligatorias (sin valor por defecto): si no están
definidas como variables de entorno, el backend no arranca — evita que alguien despliegue
por accidente con credenciales de ejemplo. `application-dev.properties` sí trae un default
de `DB_USERNAME` (`sportcourt`) para desarrollo local, pero nunca de la contraseña.

**Referencia:** `backend/.env.example` documenta las variables esperadas sin exponer
valores reales.

---

## 🔒 2. Autenticación con BCrypt

**Dependencia agregada:** `spring-boot-starter-security`

**Archivos:**
- `config/SecurityConfig.java` — expone el bean `PasswordEncoder` (`BCryptPasswordEncoder`,
  strength 10) y la configuración de la cadena de seguridad.
- `service/AuthService.java` — autentica comparando la contraseña recibida contra el hash
  guardado, vía `passwordEncoder.matches(...)`; nunca compara texto plano.
- `dto/LoginRequest.java` / `dto/LoginResponse.java` — separan lo que el cliente envía de lo
  que el backend devuelve.

```java
public Usuario authenticate(String email, String password) {
    return usuarioRepository
        .findByEmail(email)
        .filter(usuario -> passwordEncoder.matches(password, usuario.getPassword()))
        .orElse(null);
}
```

**Cambio en `LoginController`:**
- Ya no retorna la contraseña del usuario en la respuesta — devuelve `LoginResponse`
  (id, nombre, email, rol).
- Valida que email y password vengan presentes → 400 si faltan.
- Retorna 401 si las credenciales no coinciden (sin distinguir "usuario no existe" de
  "contraseña incorrecta", para no filtrar qué emails están registrados).

**Migración de contraseñas existentes:** las contraseñas de los usuarios ya en MySQL
estaban en texto plano al momento de introducir BCrypt y se migraron a hashes BCrypt como
parte del cierre de esta fase — hoy todo usuario en la tabla `usuarios` tiene su contraseña
almacenada como hash BCrypt, verificado directamente contra la base real.

---

## 🛡️ 3. CORS Restringido

**Archivo:** `config/CorsConfig.java`

**Antes:** cada controller (`CanchaController`, `ClaseController`, `ReservaController`,
`InscripcionController`, `UsuarioController`) llevaba `@CrossOrigin(origins = "*")` —
cualquier sitio web podía llamar a la API autenticado con las cookies del usuario.

**Después:** configuración centralizada, solo para `/api/**`:
```java
configuration.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "http://localhost:5500",
        "http://127.0.0.1:5500"
));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowCredentials(true);
```

Se removió `@CrossOrigin` de los 5 controllers. En producción, estos orígenes deben
cambiarse al dominio real (ver `docs/DEPLOYMENT.md`).

---

## 🔇 4. Logging de SQL Desactivado

**Archivo:** `backend/src/main/resources/application.properties`

```properties
spring.jpa.show-sql=false
```

Antes estaba en `true`, lo que volcaba cada query (con sus parámetros) a la consola —
información que no debe quedar en logs de un entorno compartido.

---

## 📁 Archivos Nuevos

```
backend/src/main/java/com/sportcourt/backend/
├── config/
│   ├── CorsConfig.java ....................... Configuración CORS centralizada
│   └── SecurityConfig.java ................... PasswordEncoder BCrypt + cadena de seguridad
├── service/
│   └── AuthService.java ....................... Autenticación vía BCrypt
└── dto/
    ├── LoginRequest.java ...................... DTO de solicitud de login
    └── LoginResponse.java ..................... DTO de respuesta (sin password)

backend/src/main/resources/
├── application.properties ..................... Variables de entorno, logging SQL off
├── application-dev.properties ................. Perfil de desarrollo (sin credenciales reales)
└── .env.example ................................ Plantilla de variables de entorno
```

## 📝 Archivos Modificados

- `pom.xml` — dependencia `spring-boot-starter-security`
- `LoginController.java` — BCrypt, sin exponer contraseña, códigos HTTP 400/401
- `CanchaController.java`, `ClaseController.java`, `ReservaController.java`,
  `InscripcionController.java`, `UsuarioController.java` — removido `@CrossOrigin("*")`

---

## 🧪 Verificación

```bash
cd backend/
./mvnw clean compile
# BUILD SUCCESS
```

Verificación funcional: login con credenciales válidas devuelve `LoginResponse` (200, sin
password); login con credenciales inválidas devuelve 401; petición sin `DB_USERNAME`/
`DB_PASSWORD` en el entorno falla al arrancar en vez de conectar con un valor por defecto.

---

## 📈 Impacto de Seguridad

| Riesgo | Antes | Después |
|--------|-------|---------|
| Credenciales de BD expuestas en el código | 🔴 Crítico | 🟢 Mitigado (variables de entorno obligatorias) |
| Contraseñas de usuario en texto plano | 🔴 Crítico | 🟢 Mitigado (BCrypt, strength 10) |
| CORS abierto a cualquier origen | 🔴 Crítico | 🟢 Mitigado (whitelist centralizada) |
| Logs con datos de queries SQL | 🟠 Medio | 🟢 Mitigado (`show-sql=false`) |
| Contraseña expuesta en la respuesta de login | 🟠 Medio | 🟢 Mitigado (`LoginResponse` sin password) |

---

## 🚀 Estado y Continuidad

Fase 1 se dio por completada el 2026-09-09. El endurecimiento adicional (CSRF, CSP,
autorización por propietario, restricción de `admin.html`, XSS) llegó después como Fase 1B
y 1C — ver `docs/PROYECTO_STATUS.md` para el detalle completo y el estado de seguridad
actual del proyecto.

```
✅ FASE 1 COMPLETADA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Credenciales de BD protegidas (variables de entorno)
✅ Contraseñas con BCrypt (strength 10)
✅ CORS restringido y centralizado
✅ Logging de SQL desactivado
✅ Login sin exponer contraseña

Siguiente: Fase 2 — Arquitectura (DTOs + manejo centralizado de excepciones)
```
