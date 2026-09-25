# Manual de Usuario — SportCourt 2.0

Guía de uso de la plataforma según el rol de la persona que ingresa. No requiere
conocimientos técnicos.

---

## 1. Visitante (sin iniciar sesión)

Al entrar a la página principal (`index.html`) sin haber iniciado sesión, puedes:

- Ver la información pública del sitio.
- Ir a **Canchas** y ver el listado completo, con filtro por deporte (Fulbito, Fútbol,
  Tenis, Piscina) y buscador por nombre.
- Ir a **Clases** y ver el listado de clases disponibles.
- Ir a **Iniciar sesión** desde el botón superior derecho.

Si intentas **reservar una cancha**, **inscribirte a una clase**, o entrar directamente
a "Mis Reservas" o "Perfil" sin haber iniciado sesión, la aplicación te redirige
automáticamente a la pantalla de login.

## 2. Usuario (con cuenta)

### Iniciar sesión

1. Entra a **Login** e ingresa tu correo y contraseña.
2. Si los datos son correctos, entras a la página principal ya identificado (tu nombre
   aparece arriba a la derecha).
3. Si el correo o la contraseña son incorrectos, verás el mensaje de error debajo del
   formulario.

También puedes entrar como **invitado** desde el link correspondiente en la pantalla
de login, sin necesidad de cuenta (mismos permisos que un Visitante).

### Reservar una cancha

1. Ve a **Canchas** y elige una cancha marcada como **Disponible**.
2. Haz clic en **Reservar cancha**.
3. Elige una fecha (no puede ser anterior a hoy) y un horario libre — los horarios ya
   ocupados aparecen deshabilitados.
4. Confirma la reserva. Si el horario fue tomado por otra persona justo antes de
   confirmar, o si hay algún problema, verás el motivo exacto debajo del formulario
   (por ejemplo, "ya existe una reserva en ese horario").
5. Si todo sale bien, verás un mensaje de confirmación con la cancha, fecha y hora.

### Ver y cancelar mis reservas

1. Ve a **Mis Reservas**.
2. Puedes filtrar por pestañas: **Todas**, **Confirmada**, **Pendiente**, **Cancelada**.
3. Cada reserva muestra la cancha, fecha, horario y precio.
4. Para cancelar una reserva activa, haz clic en **Cancelar**. Se te pedirá confirmar.
5. Puedes descargar un comprobante de texto de cualquiera de tus reservas con
   **Descargar comprobante**.
6. Arriba de la lista se muestra tu total de reservas activas y el monto gastado.

### Inscribirse a una clase

1. Ve a **Clases** y elige una clase.
2. Haz clic en **Inscribirme**. Si ya estabas inscrito, o si la clase ya no tiene
   cupos, verás el motivo exacto en pantalla.
3. Si la inscripción se realiza con éxito, el botón cambia a **Inscrito ✓** y queda
   deshabilitado.

### Ver mi perfil

En **Perfil** puedes ver tu nombre, correo, cantidad de reservas realizadas, cantidad
de clases en las que estás inscrito y el monto total gastado.

### Cerrar sesión

Desde el botón **Cerrar sesión** del menú, tu sesión se cierra y vuelves a la pantalla
de login.

## 3. Administrador

Un administrador tiene, además de todo lo del Usuario, acceso al **Panel Admin**
(visible solo para su rol; cualquier otra persona que intente entrar es redirigida).

El panel tiene tres pestañas: **Canchas**, **Clases** y **Reservas**.

### Gestionar canchas

- **Agregar cancha:** botón "Agregar cancha", completa nombre, deporte, descripción,
  precio por hora, capacidad, imagen (URL) y estado (Disponible / No disponible).
- **Editar cancha:** botón "Editar" sobre la tarjeta de la cancha, modifica los campos
  y guarda.
- **Eliminar cancha:** botón "Eliminar", pide confirmación antes de borrar.

### Gestionar clases

- **Agregar clase:** botón "Agregar clase", completa nombre, ícono, nivel/edades,
  horario, profesor, precio mensual y cupos disponibles.
- **Editar / Eliminar clase:** igual que canchas, con confirmación antes de eliminar.

### Gestionar reservas

- La tabla muestra todas las reservas del sistema: usuario, cancha, fecha/hora,
  precio y estado.
- **Cancelar reserva:** disponible para reservas activas, pide confirmación.
- **Eliminar reserva:** borra el registro por completo, pide confirmación.

En todos los casos, si una operación falla (por ejemplo, datos inválidos o un
conflicto de negocio), aparece un mensaje explicando el motivo en vez de un error
genérico.

## 4. Mensajes que puedes encontrar

| Situación | Qué significa |
|---|---|
| "Ya tienes una reserva en ese horario" / similar | Ya existe una reserva tuya que se cruza con el horario elegido. |
| "No hay cupos disponibles" | La clase ya alcanzó su capacidad máxima. |
| "No se pudo conectar con el servidor" | El backend no respondió; intenta de nuevo en unos segundos. |
| Redirección a Login al hacer clic en "Mis Reservas" o "Perfil" | No has iniciado sesión (o tu sesión expiró). |
