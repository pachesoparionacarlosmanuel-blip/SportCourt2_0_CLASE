# SportCourt 2.0

Sistema web para la gestión de canchas deportivas, clases, reservas e inscripciones.

## Arquitectura

```
Frontend (HTML/CSS/JavaScript/Tailwind)
        ↓
REST API
        ↓
Backend (Spring Boot / Java)
        ↓
MySQL
```

> El Frontend presenta, el Backend decide y MySQL almacena.

Las reglas de trabajo y la metodología del proyecto están definidas en [AGENTS.md](AGENTS.md).

## Stack tecnológico

- **Backend:** Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security, MySQL
- **Frontend:** HTML, CSS, JavaScript (sin frameworks), Tailwind CSS
- **Testing:** JUnit 5 + Mockito (backend), H2 en memoria para tests de integración
- **CI:** GitHub Actions (`.github/workflows/validar-proyecto.yml`)

## Estructura del repositorio

```
├── backend/                    # API REST (Spring Boot)
│   └── src/main/java/com/sportcourt/backend/
│       ├── controller/          # Endpoints REST
│       ├── service/             # Lógica de negocio
│       ├── repository/          # Spring Data JPA
│       ├── model/               # Entidades JPA
│       ├── dto/                 # DTOs con validación (@Valid)
│       ├── config/               # Seguridad, CORS
│       └── exception/           # Manejo centralizado de errores
├── frontend/                   # Páginas estáticas + assets del cliente
│   ├── assets/
│   │   ├── css/                  # Tailwind (app.css fuente, app.min.css generado)
│   │   └── js/                   # app.js (consumo de la API REST)
│   ├── index.html, login.html, canchas.html, clases.html,
│   │   reservas.html, perfil.html, admin.html
├── docs/                        # Documentación del proyecto por fase
├── .github/workflows/           # CI (GitHub Actions)
├── package.json                 # Build de Tailwind CSS (raíz del repo)
└── AGENTS.md                    # Reglas y metodología del proyecto
```

## Requisitos previos

- Java 21
- Node.js 20+ (solo para compilar Tailwind CSS)
- MySQL 8+, con la base de datos `sportcourt` ya creada (ver [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) para el esquema de tablas)

## Configuración

El backend requiere variables de entorno para conectarse a MySQL. Copia el ejemplo y completa tus credenciales:

```bash
cd backend
cp .env.example .env   # o exporta las variables directamente en tu shell
```

Variables usadas (ver [backend/src/main/resources/application.properties](backend/src/main/resources/application.properties)):

| Variable | Requerida | Descripción |
|---|---|---|
| `DB_URL` | No (default `jdbc:mysql://localhost:3306/sportcourt`) | URL JDBC de MySQL |
| `DB_USERNAME` | Sí | Usuario de MySQL |
| `DB_PASSWORD` | Sí | Contraseña de MySQL |
| `SERVER_PORT` | No (default `8080`) | Puerto del backend |

Nunca se ponen credenciales en el código ni en el repositorio.

## Cómo correr el backend

```bash
cd backend
./mvnw spring-boot:run
```

El backend queda disponible en `http://localhost:8080`.

## Cómo correr el frontend

El frontend son páginas estáticas que consumen la API REST, ubicadas en [frontend/](frontend/). Cualquier servidor estático sirve (por ejemplo, la extensión "Live Server" de VS Code abriendo `frontend/index.html` en el puerto `5500`, que ya está permitido en la configuración CORS del backend).

Para compilar el CSS de Tailwind:

```bash
npm install
npm run dev        # compila con watch mode, para desarrollo
npm run build:css  # compila minificado, para producción
```

## Tests

```bash
# Backend (130 tests: unitarios + contexto Spring con H2 en memoria + integración CSRF
# + integración HTTP de endpoints, todo con H2 en memoria)
cd backend
./mvnw test

# Frontend (chequeo de sintaxis de frontend/assets/js/app.js)
npm test
```

Ambos corren automáticamente en cada push/PR vía GitHub Actions.

## Documentación de la API

Con el backend corriendo, la documentación interactiva (OpenAPI/Swagger) está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

## Roles del sistema

- **Visitante:** ve información pública, canchas y clases disponibles; puede acceder al login.
- **Usuario:** inicia sesión, reserva/cancela canchas, se inscribe a clases, consulta su perfil.

> **Regla de reservas:** una reserva ocupa la cancha completa. Si ya existe una reserva
> activa que se superpone en la misma cancha y fecha, la nueva se rechaza con 409. El campo
> `capacidad` de la cancha es solo informativo (personas que caben) y puede quedar NULL.
- **Administrador:** gestiona canchas, clases y reservas desde el panel admin.

Detalle completo de permisos en [AGENTS.md](AGENTS.md#8-roles-del-sistema).

## Entrega (ZIP limpio)

Para entregar solo el código fuente (sin `node_modules`, `backend/target` ni `.git`),
genera el ZIP a partir del último commit; `git archive` incluye únicamente los archivos
versionados:

```bash
git archive --format=zip -o SportCourt2.0.zip HEAD
```

Tras descomprimirlo: `npm install` recrea `node_modules` (solo hace falta para recompilar
el CSS, `app.min.css` ya viene versionado) y `./mvnw package` recrea `backend/target`.

## Documentación adicional

- [docs/PROYECTO_STATUS.md](docs/PROYECTO_STATUS.md) — estado y progreso por fase
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — guía de despliegue
- [docs/MANUAL_USUARIO.md](docs/MANUAL_USUARIO.md) — manual de usuario por rol
