const API_URL = 'http://localhost:8080/api';

// Escapa texto antes de interpolarlo en innerHTML (tanto en contenido como
// en atributos value="..."): sin esto, un nombre de cancha/clase/usuario
// guardado en MySQL con HTML dentro se inyecta tal cual en la página.
const HTML_ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, function (ch) { return HTML_ESCAPES[ch]; });
}

// Lee el cuerpo de error del backend ({message: ...}) y devuelve un mensaje
// legible para el usuario; si no se puede leer, usa el mensaje por defecto.
async function parseApiError(respuesta, mensajePorDefecto) {
  try {
    const data = await respuesta.json();
    return (data && data.message) ? data.message : mensajePorDefecto;
  } catch (error) {
    return mensajePorDefecto;
  }
}

// Confirma que la respuesta de la API tenga la forma esperada (array) antes
// de usarla en .map/.find/.some; evita romper la página si el backend
// devuelve un error inesperado o cambia de forma.
function asArray(valor, contexto) {
  if (Array.isArray(valor)) return valor;
  console.error('Respuesta de API con forma inesperada (se esperaba un array): ' + contexto, valor);
  return [];
}

// Adapta una ClaseDTO del backend (id, name, icon, level, schedule,
// professor, price, slots) al formato que usa el frontend.
function mapClaseDesdeAPI(c) {
  return {
    id: String(c.id),
    name: c.name || '',
    icon: c.icon || '⚽',
    level: c.level || '',
    schedule: c.schedule || '',
    professor: c.professor || '',
    price: Number(c.price ?? 0),
    slots: Number(c.slots ?? 0)
  };
}

async function cargarCanchasDesdeAPI() {
  try {
    const respuesta = await fetch(API_URL + '/canchas', {
      credentials: 'include'
    });

    if (!respuesta.ok) {
      throw new Error('Error HTTP: ' + respuesta.status);
    }

    const canchas = await respuesta.json();

    return asArray(canchas, 'GET /canchas');

  } catch (error) {
    console.error('Error al conectar con el backend:', error);
    return [];
  }
}
// =================================================
// ROLES: helpers compartidos por todas las páginas
// =================================================
function getRole() {
  return localStorage.getItem('sportcourt_role') || 'invitado';
}
function getUserName() {
  return localStorage.getItem('sportcourt_user_name') || 'Invitado';
}

function getCsrfToken() {
  const cookie = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='));

  return cookie ? decodeURIComponent(cookie.split('=')[1]) : null;
}

// La cookie XSRF-TOKEN solo se planta cuando algo resuelve el CsrfToken en el
// backend (carga diferida de Spring Security). Antes de la primera petición
// que modifica datos hay que pedir GET /csrf para obtenerla.
async function getCsrfTokenAsync() {
  const token = getCsrfToken();
  if (token) return token;

  try {
    await fetch(API_URL + '/csrf', { credentials: 'include' });
  } catch (error) {
    console.error('No se pudo obtener el token CSRF:', error);
  }

  return getCsrfToken();
}

// ---------------------------------------------
// Login: asigna rol según el correo y redirige
// admin... -> Administrador | cualquier otro -> Usuario
// ---------------------------------------------
const loginForm = document.getElementById('login-form');
if (loginForm) {
  loginForm.addEventListener('submit', async function (e) {
    e.preventDefault();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    try {
      const respuesta = await fetch(API_URL + '/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': await getCsrfTokenAsync()
        },
        body: JSON.stringify({ email: email, password: password })
      });

      const usuario = await respuesta.json();

      if (!respuesta.ok) {
        alert(usuario.message || 'Correo o contraseña incorrectos');
        return;
      }

      localStorage.setItem('sportcourt_user', usuario.email);
      localStorage.setItem('sportcourt_user_id', usuario.id);
      localStorage.setItem('sportcourt_role', usuario.rol);
      localStorage.setItem('sportcourt_user_name', usuario.nombre);

      window.location.href =
        usuario.rol === 'admin' ? 'admin.html' : 'index.html';

    } catch (error) {
      console.error('Error al conectar con el backend:', error);
      alert('No se pudo conectar con el servidor.');
    }
  });
}

// Continuar como invitado (Login)
const guestLink = document.getElementById('guest-link');
if (guestLink) {
  guestLink.addEventListener('click', function (e) {
    e.preventDefault();
    localStorage.removeItem('sportcourt_user');
    localStorage.setItem('sportcourt_role', 'invitado');
    localStorage.setItem('sportcourt_user_name', 'Invitado');
    window.location.href = 'index.html';
  });
}

// ---------------------------------------------
// Navbar: se adapta según el rol activo
// ---------------------------------------------
const navbar = document.querySelector('.navbar');
if (navbar) {
  const role = getRole();
  const userChip = navbar.querySelector('.user-chip');
  const adminLink = navbar.querySelector('.admin-link');
  const reservasLink = navbar.querySelector('a[href="reservas.html"]');
  const perfilLink = navbar.querySelector('a[href="perfil.html"]');

  if (adminLink) adminLink.style.display = role === 'admin' ? 'inline-block' : 'none';

  if (role === 'invitado') {
    if (userChip) {
      userChip.setAttribute('href', 'login.html');
      userChip.classList.add('guest-chip');
      userChip.innerHTML = 'Iniciar sesión';
    }
    // Mis Reservas y Perfil quedan bloqueados para invitados
    [reservasLink, perfilLink].forEach(function (link) {
      if (link) link.classList.add('locked-link');
    });
  } else {
    if (userChip) {
      const avatar = userChip.querySelector('.avatar');
      const nameSpan = userChip.querySelector('span:last-child');
      const name = getUserName();
      if (avatar) avatar.textContent = name.charAt(0).toUpperCase();
      if (nameSpan) {
        nameSpan.textContent = name;
        if (role === 'admin') {
          nameSpan.innerHTML = escapeHtml(name) + '<span class="role-tag">Admin</span>';
        }
      }
    }
  }
}

// Enlaces bloqueados (Mis Reservas / Perfil para invitados) -> a login
document.querySelectorAll('.locked-link').forEach(function (link) {
  link.addEventListener('click', function (e) {
    e.preventDefault();
    window.location.href = 'login.html';
  });
});

// Páginas exclusivas de cuenta: si un invitado entra directo por URL, se le pide iniciar sesión
if (document.body.dataset.page === 'reservas' || document.body.dataset.page === 'perfil') {
  if (getRole() === 'invitado') window.location.href = 'login.html';
}

// Panel Admin: solo accesible para administradores
if (document.body.dataset.page === 'admin' && getRole() !== 'admin') {
  window.location.href = 'index.html';
}

// Protección real del panel Admin mediante Spring Security
if (document.body.dataset.page === 'admin') {
  fetch(API_URL + '/usuarios', {
    method: 'GET',
    credentials: 'include'
  })
    .then(function (respuesta) {
      if (respuesta.status === 403 || respuesta.status === 401) {
        window.location.href = 'index.html';
        return null;
      }

      if (!respuesta.ok) {
        throw new Error('No se pudo verificar el acceso de administrador');
      }

      return respuesta.json();
    })
    .catch(function (error) {
      console.error('Error verificando permisos de administrador:', error);
      window.location.href = 'index.html';
    });
}

// ---------------------------------------------
// Botones "Ver canchas" (Inicio) -> van a Canchas
// ---------------------------------------------
document.querySelectorAll('.cta-btn, .offer-btn').forEach(function (btn) {
  btn.addEventListener('click', function () {
    window.location.href = 'canchas.html';
  });
});

// ---------------------------------------------
// Canchas: filtro por deporte + búsqueda por nombre
// ---------------------------------------------
let canchasAPI = null;

const courtsGrid = document.getElementById('courts-grid');
if (courtsGrid) {
  const filterPills = document.querySelectorAll('.filter-pill');
  const searchInput = document.getElementById('court-search');
  const resultsCount = document.getElementById('results-count');
  let activeFilter = 'todos';

  function renderPublicCourts() {
    const courts = canchasAPI || getCourts();
    courtsGrid.innerHTML = courts.map(function (c) {
      const available = c.status === 'disponible';
      return '<article class=\"court-card\" data-court-id=\"' + c.id + '\" data-sport=\"' + escapeHtml(c.sport) + '\" data-name=\"' + escapeHtml(c.name) + '\">' +
        '<div class=\"court-media' + (available ? '' : ' is-unavailable') + '\" style=\"background-image:url(\'' + escapeHtml(c.image || '') + '\')\">' +
        '<span class=\"court-tag\">' + (c.sport === 'fulbito' ? '⚽ Fulbito' : c.sport === 'futbol' ? '🏟️ Fútbol' : c.sport === 'tenis' ? '🎾 Tenis' : '🏊 Piscina') + '</span>' +
        '<span class=\"court-status ' + (available ? 'available' : 'unavailable') + '\">' + (available ? 'Disponible' : 'No disponible') + '</span>' +
        '</div>' +
        '<div class=\"court-body\">' +
        '<h3>' + escapeHtml(c.name) + '</h3>' +
        '<p>' + escapeHtml(c.desc || 'Cancha deportiva disponible para reservas.') + '</p>' +
        '<div class=\"court-footer\"><div class=\"court-price\">S/ ' + Number(c.price || 0) + ' <span>/ hora</span></div>' +
        '<div class=\"court-capacity\">👥 hasta ' + Number(c.capacity || 0) + '</div></div>' +
        (available ? '<button class=\"reserve-btn\" type=\"button\">Reservar cancha</button>' : '<button class=\"reserve-btn\" type=\"button\" disabled>No disponible</button>') +
        '</div></article>';
    }).join('');

    courtsGrid.querySelectorAll('.reserve-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        const card = btn.closest('.court-card');
        if (card) openReservation(card.dataset.courtId);
      });
    });
  }

  cargarCanchasDesdeAPI().then(function (canchas) {

    if (Array.isArray(canchas) && canchas.length > 0) {

      canchasAPI = canchas.map(function (c) {
        return {
          id: String(c.id),
          sport: c.sport,
          name: c.name,
          desc: c.description || '',
          price: Number(c.price),
          capacity: Number(c.capacity),
          image: c.image || '',
          status: c.status
        };
      });

      renderPublicCourts();
      applyFilters();

    } else {
      renderPublicCourts();
    }

  });

  function applyFilters() {
    const query = (searchInput ? searchInput.value : '').trim().toLowerCase();
    const cards = document.querySelectorAll('.court-card');
    let visible = 0;

    cards.forEach(function (card) {
      const matchesSport = activeFilter === 'todos' || card.dataset.sport === activeFilter;
      const matchesQuery = card.dataset.name.toLowerCase().includes(query);
      const show = matchesSport && matchesQuery;
      card.hidden = !show;
      if (show) visible++;
    });

    if (resultsCount) resultsCount.textContent = visible;
  }

  filterPills.forEach(function (pill) {
    pill.addEventListener('click', function () {
      filterPills.forEach(function (p) { p.classList.remove('active'); });
      pill.classList.add('active');
      activeFilter = pill.dataset.filter;
      applyFilters();
    });
  });

  if (searchInput) {
    searchInput.addEventListener('input', applyFilters);
  }
}

// ---------------------------------------------
// Clases: renderizado dinámico desde la API (clases.html)
// ---------------------------------------------
let clasesPublicAPI = null;

const classesGrid = document.getElementById('classes-grid');
if (classesGrid) {

  function renderPublicClasses() {
    const clases = clasesPublicAPI;
    if (!Array.isArray(clases) || clases.length === 0) return;

    classesGrid.innerHTML = clases.map(function (k) {
      return '<article class=\"class-card\">' +
        '<div class=\"class-top\">' +
        '<div class=\"class-icon\">' + k.icon + '</div>' +
        '<span class=\"class-slots ' + (k.slots <= 3 ? 'low' : 'ok') + '\">' + k.slots + ' cupos</span>' +
        '</div>' +
        '<h3>' + escapeHtml(k.name) + '</h3>' +
        '<p class=\"class-level\">' + escapeHtml(k.level) + '</p>' +
        '<div class=\"class-meta\">' +
        '<div class=\"class-meta-row\"><span class=\"meta-icon\">🕒</span> ' + escapeHtml(k.schedule) + '</div>' +
        '<div class=\"class-meta-row\"><span class=\"meta-icon\">👨‍🏫</span> Prof. ' + escapeHtml(k.professor) + '</div>' +
        '</div>' +
        '<div class=\"class-footer\">' +
        '<div class=\"class-price\">S/ ' + k.price + ' <span>/mes</span></div>' +
        '<button class=\"enroll-btn\" data-id=\"' + k.id + '\">Inscribirse</button>' +
        '</div>' +
        '</article>';
    }).join('');

    wireEnrollButtons();
    cargarEstadoInscripciones();
  }

  cargarClasesDesdeAPI().then(function (clases) {
    if (Array.isArray(clases) && clases.length > 0) {
      clasesPublicAPI = clases.map(mapClaseDesdeAPI);
      renderPublicClasses();
    }
  });
}

// ---------------------------------------------
// Reservas: calendario + horarios + persistencia local
// ---------------------------------------------
const reservationModal = document.getElementById('reservation-modal');
const reservationForm = document.getElementById('reservation-form');
const reservationDate = document.getElementById('reservation-date');
const reservationTime = document.getElementById('reservation-time');
const timeSlots = document.getElementById('time-slots');
const reservationFeedback = document.getElementById('reservation-feedback');
const reservationCourtName = document.getElementById('reservation-court-name');
const reservationPrice = document.getElementById('reservation-price');
const reservationClose = document.getElementById('reservation-close');

function getCourts() {
  return Array.isArray(canchasAPI) ? canchasAPI : [];
}
// =============================================
// API BACKEND - CANCHAS
// =============================================
async function cargarClasesDesdeAPI() {
  try {
    const respuesta = await fetch(API_URL + '/clases', {
      credentials: 'include'
    });

    if (!respuesta.ok) {
      throw new Error('Error HTTP clases: ' + respuesta.status);
    }

    const clases = await respuesta.json();

    return asArray(clases, 'GET /clases');

  } catch (error) {
    console.error('Error al conectar clases con el backend:', error);
    return [];
  }
}

let clasesDesdeAPI = [];

cargarClasesDesdeAPI().then(function (clases) {
  clasesDesdeAPI = clases.map(mapClaseDesdeAPI);

  classes = clasesDesdeAPI;

  if (typeof renderClasses === 'function') {
    renderClasses();
  }
});

function getCourt(courtId) {
  return getCourts().find(function (c) { return c.id === courtId; }) || null;
}
const HOURS = ['06:00', '07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00'];

function localDateKey(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return y + '-' + m + '-' + d;
}
function displayDate(dateKey) {
  const parts = dateKey.split('-');
  const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' });
}
let reservasAPI = [];

function getReservations() {
  return reservasAPI;
}
async function cargarReservasDesdeAPI() {
  try {
    // Obtener reservas desde el backend
    const respuestaReservas = await fetch(API_URL + '/reservas', {
      credentials: 'include'
    });
    if (!respuestaReservas.ok) {
      throw new Error('Error HTTP reservas: ' + respuestaReservas.status);
    }
    const reservas = asArray(await respuestaReservas.json(), 'GET /reservas');
    // Obtener canchas desde el backend
    const respuestaCanchas = await fetch(API_URL + '/canchas', {
      credentials: 'include'
    });
    if (!respuestaCanchas.ok) {
      throw new Error('Error HTTP canchas: ' + respuestaCanchas.status);
    }
    const canchas = asArray(await respuestaCanchas.json(), 'GET /canchas');
    // Adaptar las reservas al formato que usa el frontend
    reservasAPI = reservas.map(function (r) {
      const cancha = canchas.find(function (c) {
        return String(c.id) === String(r.canchaId);
      });
      return {
        id: String(r.id),
        usuarioId: String(r.usuarioId),
        user: '',
        userName: '',
        courtId: String(r.canchaId),
        item: cancha ? cancha.name : 'Cancha',
        dateKey: r.fecha,
        date: r.fecha,
        time: String(r.horaInicio).slice(0, 5) +
          ' — ' +
          String(r.horaFin).slice(0, 5),
        price: cancha ? Number(cancha.price) : 0,
        status: r.estado,
        createdAt: ''
      };
    });
    return reservasAPI;
  } catch (error) {
    console.error('Error al conectar reservas con el backend:', error);
    return [];
  }
}

function reservationDateKey(r) {
  if (r.dateKey) return r.dateKey;
  const match = String(r.date || '').match(/(\d{1,2})\s+([A-Za-zÁÉÍÓÚáéíóú]+)\s+(\d{4})/i);
  if (!match) return '';
  const months = { enero: 1, febrero: 2, marzo: 3, abril: 4, mayo: 5, junio: 6, julio: 7, agosto: 8, septiembre: 9, octubre: 10, noviembre: 11, diciembre: 12 };
  const month = months[match[2].toLowerCase()];
  return month ? match[3] + '-' + String(month).padStart(2, '0') + '-' + String(match[1]).padStart(2, '0') : '';
}
function reservationHour(r) { return String(r.time || '').split(' ')[0].split('—')[0].trim(); }
function isOccupied(courtId, dateKey, hour) {
  return getReservations().some(function (r) {
    const court = getCourt(courtId);
    const sameCourt = (r.courtId === courtId) || (!r.courtId && court && r.item === court.name);
    const active = r.status !== 'cancelada';
    return sameCourt && active && reservationDateKey(r) === dateKey && reservationHour(r) === hour;
  });
}
function renderTimeSlots() {
  if (!timeSlots || !reservationDate || !reservationTime) return;
  const dateKey = reservationDate.value;
  const courtId = reservationForm.dataset.courtId;
  reservationTime.value = '';
  timeSlots.innerHTML = '';
  if (!dateKey || !courtId) return;
  HOURS.forEach(function (hour) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'time-slot' + (isOccupied(courtId, dateKey, hour) ? ' occupied' : '');
    btn.textContent = hour + ' — ' + String(Number(hour.slice(0, 2)) + 1).padStart(2, '0') + ':00';
    btn.dataset.time = hour;
    btn.disabled = isOccupied(courtId, dateKey, hour);
    btn.addEventListener('click', function () {
      timeSlots.querySelectorAll('.time-slot').forEach(function (b) { b.classList.remove('selected'); });
      btn.classList.add('selected');
      reservationTime.value = hour;
      reservationFeedback.textContent = '';
    });
    timeSlots.appendChild(btn);
  });
}
function openReservation(courtId) {
  if (getRole() === 'invitado') { window.location.href = 'login.html'; return; }
  const court = getCourt(courtId);
  if (!court || court.status !== 'disponible') { alert('Esta cancha no está disponible.'); return; }
  reservationForm.dataset.courtId = courtId;
  reservationCourtName.textContent = court.name;
  reservationPrice.textContent = 'S/ ' + court.price;
  reservationFeedback.textContent = '';
  const today = new Date();
  reservationDate.min = localDateKey(today);
  if (!reservationDate.value || reservationDate.value < reservationDate.min) reservationDate.value = reservationDate.min;
  renderTimeSlots();
  reservationModal.classList.add('open');
  reservationModal.setAttribute('aria-hidden', 'false');
}
function closeReservation() {
  if (!reservationModal) return;
  reservationModal.classList.remove('open');
  reservationModal.setAttribute('aria-hidden', 'true');
}

if (reservationDate) reservationDate.addEventListener('change', renderTimeSlots);
if (reservationClose) reservationClose.addEventListener('click', closeReservation);
if (reservationModal) reservationModal.addEventListener('click', function (e) { if (e.target === reservationModal) closeReservation(); });
document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeReservation(); });
if (reservationForm) {
  reservationForm.addEventListener('submit', async function (e) {
    e.preventDefault();
    const courtId = reservationForm.dataset.courtId;
    const court = getCourt(courtId);
    const dateKey = reservationDate.value;
    const hour = reservationTime.value;
    if (!court || !dateKey || !hour) { reservationFeedback.textContent = 'Selecciona una fecha y un horario.'; return; }
    if (dateKey < reservationDate.min) { reservationFeedback.textContent = 'La fecha no puede ser anterior a hoy.'; return; }
    if (isOccupied(courtId, dateKey, hour)) { reservationFeedback.textContent = 'Ese horario acaba de ser ocupado. Elige otro.'; renderTimeSlots(); return; }

    try {
      const respuesta = await fetch(API_URL + '/reservas', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': await getCsrfTokenAsync()
        },
        body: JSON.stringify({
          usuarioId: Number(localStorage.getItem('sportcourt_user_id')),
          canchaId: Number(courtId),
          fecha: dateKey,
          horaInicio: hour + ':00',
          horaFin: String(Number(hour.split(':')[0]) + 1).padStart(2, '0') + ':00',
          estado: 'confirmada'
        })
      });

      if (!respuesta.ok) {
        reservationFeedback.textContent = await parseApiError(respuesta, 'No se pudo guardar la reserva.');
        renderTimeSlots();
        return;
      }

      await respuesta.json();
      await cargarReservasDesdeAPI();

      closeReservation();
      alert('Reserva confirmada para ' + court.name + ' el ' + displayDate(dateKey) + ' a las ' + hour + '.');

    } catch (error) {
      console.error('Error al guardar la reserva en el backend:', error);
      reservationFeedback.textContent = 'No se pudo conectar con el servidor.';
    }
  });
}

// ---------------------------------------------
// Mis Reservas: renderiza las reservas reales del usuario
// ---------------------------------------------
const reservasList = document.getElementById('reservas-list');
if (reservasList) {
  function renderUserReservations(filter) {
    const currentUserId = localStorage.getItem('sportcourt_user_id');

    const all = getReservations().filter(function (r) {
      return String(r.usuarioId) === String(currentUserId);
    });
    const filtered = filter === 'todas' ? all : all.filter(function (r) { return r.status === filter; });
    reservasList.innerHTML = filtered.length ? filtered.map(function (r) {
      const statusLabel = r.status === 'confirmada' ? 'Confirmada' : r.status === 'pendiente' ? 'Pendiente' : 'Cancelada';
      const canCancel = r.status !== 'cancelada';
      return '<article class="reserva-card" data-status="' + r.status + '">' +
        '<div class="reserva-info">' +
        '<div class="reserva-tags"><span class="reserva-sport-tag">🏟️ Cancha</span><span class="reserva-status ' + r.status + '">' + statusLabel + '</span></div>' +
        '<h3>' + escapeHtml(r.item) + '</h3>' +
        '<div class="reserva-meta"><span>📅 ' + escapeHtml(r.date) + '</span><span>🕒 ' + escapeHtml(r.time) + '</span></div>' +
        '<div class="reserva-actions">' +
        '<button class="ver-btn" type="button">Ver cancha</button>' +
        (canCancel ? '<button class="cancel-btn" data-id="' + r.id + '" type="button">Cancelar</button>' : '') +
        '<button class="comprobante-btn" data-id="' + r.id + '" type="button">Descargar comprobante</button>' +
        '</div></div><div class="reserva-price"><div class="amount">S/ ' + r.price + '</div><div class="duration">1h</div></div></article>';
    }).join('') : '<div class="empty-state"><h3>No tienes reservas en este filtro.</h3><p>Ve a Canchas para seleccionar un día y horario disponible.</p></div>';
    const active = all.filter(function (r) { return r.status === 'confirmada' || r.status === 'pendiente'; }).length;
    const spent = all.filter(function (r) { return r.status !== 'cancelada'; }).reduce(function (sum, r) { return sum + Number(r.price || 0); }, 0);
    const ac = document.getElementById('activas-count'); if (ac) ac.textContent = active;
    const tg = document.getElementById('total-gastado'); if (tg) tg.textContent = 'S/ ' + spent;
  }
  let currentFilter = 'todas';
  const tabs = document.querySelectorAll('.reservas-tab');
  tabs.forEach(function (tab) { tab.addEventListener('click', function () { tabs.forEach(function (t) { t.classList.remove('active'); }); tab.classList.add('active'); currentFilter = tab.dataset.tab; renderUserReservations(currentFilter); }); });
  reservasList.addEventListener('click', async function (e) {
    const cancel = e.target.closest('.cancel-btn');
    if (cancel) {
      const id = cancel.dataset.id;
      try {
        const response = await fetch(API_URL + '/reservas/' + id + '/cancelar', {
          method: 'PUT',
          credentials: 'include',
          headers: {
            'X-XSRF-TOKEN': await getCsrfTokenAsync()
          }
        });

        if (!response.ok) {
          alert(await parseApiError(response, 'No se pudo cancelar la reserva.'));
          return;
        }

        await response.json();
        reservasAPI = reservasAPI.map(function (r) {
          return String(r.id) === String(id)
            ? Object.assign({}, r, { status: 'cancelada' })
            : r;
        });
        renderUserReservations(currentFilter);
      } catch (error) {
        console.error('Error al cancelar la reserva:', error);
        alert('No se pudo conectar con el servidor.');
      }
      return;
    }
    const receipt = e.target.closest('.comprobante-btn');
    if (receipt) { const r = getReservations().find(function (x) { return x.id === receipt.dataset.id; }); if (r) { const blob = new Blob(['SportCourt Perú\nComprobante de reserva\n\nUsuario: ' + r.user + '\nCancha: ' + r.item + '\nFecha: ' + r.date + '\nHorario: ' + r.time + '\nMonto: S/ ' + r.price + '\nEstado: ' + r.status], { type: 'text/plain;charset=utf-8' }); const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'comprobante-' + r.id + '.txt'; a.click(); URL.revokeObjectURL(a.href); } }
  });
  cargarReservasDesdeAPI()
    .then(function () {
      renderUserReservations(currentFilter);
    })
    .catch(function (error) {
      console.error('Error al cargar datos de reservas:', error);
      renderUserReservations(currentFilter);
    });
}
// ---------------------------------------------
// Clases: inscripción y recuperación del estado
// ---------------------------------------------

async function cargarEstadoInscripciones() {
  const currentUserId = Number(localStorage.getItem('sportcourt_user_id'));

  if (getRole() === 'admin') {
    document.querySelectorAll('.enroll-btn').forEach(function (btn) {
      btn.textContent = 'No disponible para admin';
      btn.disabled = true;
    });
    return;
  }

  if (!currentUserId || getRole() === 'invitado') {
    return;
  }

  try {
    const respuesta = await fetch(API_URL + '/inscripciones', {
      credentials: 'include'
    });

    if (!respuesta.ok) {
      throw new Error('Error HTTP inscripciones: ' + respuesta.status);
    }

    const inscripciones = asArray(await respuesta.json(), 'GET /inscripciones');

    const misInscripciones = inscripciones.filter(function (i) {
      return Number(i.usuarioId) === currentUserId &&
        i.estado === 'inscrita';
    });

    document.querySelectorAll('.enroll-btn').forEach(function (btn) {
      const claseId = Number(btn.dataset.id);

      const yaInscrito = misInscripciones.some(function (i) {
        return Number(i.claseId) === claseId;
      });

      if (yaInscrito) {
        btn.textContent = 'Inscrito ✓';
        btn.classList.add('enrolled');
        btn.disabled = true;
      }
    });

  } catch (error) {
    console.error('Error al cargar las inscripciones:', error);
  }
}

function wireEnrollButtons() {
  document.querySelectorAll('.enroll-btn').forEach(function (btn) {
    btn.addEventListener('click', async function () {

      if (getRole() === 'invitado') {
        window.location.href = 'login.html';
        return;
      }

      if (getRole() === 'admin') {
        alert('Los administradores no pueden inscribirse a clases.');
        return;
      }

      const claseId = Number(btn.dataset.id);

      const inscripcion = {
        usuarioId: Number(localStorage.getItem('sportcourt_user_id')),
        claseId: claseId,
        fecha: new Date().toISOString().split('T')[0],
        estado: 'inscrita'
      };

      try {
        const respuesta = await fetch(API_URL + '/inscripciones', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': await getCsrfTokenAsync()
          },
          body: JSON.stringify(inscripcion)
        });

        if (!respuesta.ok) {
          alert(await parseApiError(respuesta, 'No se pudo guardar la inscripción.'));
          return;
        }

        await respuesta.json();

        btn.textContent = 'Inscrito ✓';
        btn.classList.add('enrolled');
        btn.disabled = true;

        alert('¡Inscripción realizada correctamente!');

      } catch (error) {
        console.error('Error al guardar la inscripción:', error);
        alert('No se pudo guardar la inscripción.');
      }
    });
  });
}

// Recuperar el estado guardado en MySQL al cargar la página
if (document.querySelectorAll('.enroll-btn').length > 0) {
  wireEnrollButtons();
  cargarEstadoInscripciones();
}
if (document.getElementById('inscripciones-list')) {
  cargarMisInscripciones();
}
// ---------------------------------------------
// Mis Inscripciones: muestra las inscripciones del usuario
// ---------------------------------------------
async function cargarMisInscripciones() {
  const lista = document.getElementById('inscripciones-list');

  if (!lista) {
    return;
  }

  const currentUserId = Number(localStorage.getItem('sportcourt_user_id'));

  if (!currentUserId) {
    lista.innerHTML = '<p class="text-gray-500">No hay un usuario identificado.</p>';
    return;
  }

  try {
    const respuesta = await fetch(API_URL + '/inscripciones', {
      credentials: 'include'
    });

    if (!respuesta.ok) {
      throw new Error('Error HTTP: ' + respuesta.status);
    }

    const inscripciones = asArray(await respuesta.json(), 'GET /inscripciones');

    const misInscripciones = inscripciones
      .filter(function (i) {
        return Number(i.usuarioId) === currentUserId &&
          i.estado === 'inscrita';
      })
      .filter(function (i, index, self) {
        return index === self.findIndex(function (j) {
          return Number(j.claseId) === Number(i.claseId);
        });
      });

    if (misInscripciones.length === 0) {
      lista.innerHTML = '<p class="text-gray-500">No tienes inscripciones.</p>';
      return;
    }

    lista.innerHTML = misInscripciones.map(function (i) {
      return '<div class="mb-3 p-4 bg-white rounded-lg border">' +
        '<p class="font-semibold">Clase #' + i.claseId + '</p>' +
        '<p class="text-sm text-gray-500">Fecha de inscripción: ' + i.fecha + '</p>' +
        '<p class="text-sm text-green-600">Inscrita</p>' +
        '</div>';
    }).join('');

  } catch (error) {
    console.error('Error al cargar mis inscripciones:', error);
    lista.innerHTML = '<p class="text-red-500">No se pudieron cargar las inscripciones.</p>';
  }
}
// ---------------------------------------------
// Perfil: muestra los datos de la sesión actual
// ---------------------------------------------
if (document.body.dataset.page === 'perfil') {
  cargarReservasDesdeAPI().then(function () {
    const profileName = document.getElementById('perfil-name');
    const profileEmail = document.getElementById('perfil-email');
    const profileAvatar = document.getElementById('perfil-avatar');
    const profileReservations = document.getElementById('perfil-reservas-count');
    const profileSpent = document.getElementById('perfil-total-gastado');
    const currentEmail = localStorage.getItem('sportcourt_user') || '';
    const currentName = getUserName();
    if (profileName) profileName.textContent = currentName;
    if (profileEmail) profileEmail.textContent = currentEmail || 'Sin correo';
    if (profileAvatar) profileAvatar.textContent = (currentName || 'U').charAt(0).toUpperCase();
    const currentUserId = localStorage.getItem('sportcourt_user_id');

    const userReservations = getReservations().filter(function (r) {
      return String(r.usuarioId) === String(currentUserId);
    });

    const totalSpent = userReservations
      .filter(function (r) { return r.status !== 'cancelada'; })
      .reduce(function (sum, r) { return sum + Number(r.price || 0); }, 0);
    if (profileReservations) profileReservations.textContent = userReservations.length;
    if (profileSpent) profileSpent.textContent = 'S/ ' + totalSpent;
    const profileClasses = document.querySelector('#perfil-clases-count');

    fetch(API_URL + '/inscripciones', {
      credentials: 'include'
    })
      .then(function (respuesta) {
        if (!respuesta.ok) {
          throw new Error('Error HTTP: ' + respuesta.status);
        }
        return respuesta.json();
      })
      .then(function (inscripciones) {
        const misInscripciones = inscripciones.filter(function (i) {
          return String(i.usuarioId) === String(currentUserId)
            && i.estado === 'inscrita';
        });

        if (profileClasses) {
          profileClasses.textContent = misInscripciones.length;
        }
      })
      .catch(function (error) {
        console.error('Error al cargar las clases inscritas:', error);
      });
  });
}


// ---------------------------------------------
// Cerrar sesión -> vuelve a Login
// ---------------------------------------------
const logoutBtn = document.getElementById('logout-btn');
if (logoutBtn) {
  logoutBtn.addEventListener('click', function (e) {
    e.preventDefault();
    localStorage.removeItem('sportcourt_user');
    localStorage.removeItem('sportcourt_user_id');
    localStorage.removeItem('sportcourt_role');
    localStorage.removeItem('sportcourt_user_name');
    window.location.replace('login.html');
  });
}

// Revalida las páginas protegidas al volver con el botón Atrás.
if (document.body.dataset.page === 'reservas' || document.body.dataset.page === 'perfil' || document.body.dataset.page === 'admin') {
  window.addEventListener('pageshow', function () {
    const role = getRole();
    const page = document.body.dataset.page;
    if ((page === 'admin' && role !== 'admin') || ((page === 'reservas' || page === 'perfil') && role === 'invitado')) {
      window.location.replace(page === 'admin' ? 'index.html' : 'login.html');
    }
  });
}

// =================================================
// PANEL ADMIN: datos + CRUD de canchas, clases y reservas
// =================================================
if (document.body.dataset.page === 'admin' && getRole() === 'admin') {

  // =============================================
  // API BACKEND - CANCHAS
  // =============================================

  let courts = [];

  // Cargar canchas desde MySQL
  cargarCanchasDesdeAPI().then(function (canchas) {
    if (canchas.length > 0) {

      courts = canchas.map(function (c) {
        return {
          id: String(c.id),
          sport: c.sport,
          name: c.name,
          desc: c.description || '',
          location: c.location || '',
          price: Number(c.price),
          capacity: Number(c.capacity),
          image: c.image || '',
          status: c.status
        };
      });

      renderCourts();
    }
  });
  let classes = [];

  // Cargar clases desde MySQL
  cargarClasesDesdeAPI().then(function (clases) {
    if (clases.length > 0) {
      classes = clases.map(mapClaseDesdeAPI);
      renderClasses();
    }
  });

  let reservations = [];

  // Cargar reservas desde MySQL (todas, porque el panel corre como ADMIN)
  async function cargarReservasAdminDesdeAPI() {
    try {
      const [respuestaReservas, respuestaCanchas, respuestaUsuarios] = await Promise.all([
        fetch(API_URL + '/reservas', { credentials: 'include' }),
        fetch(API_URL + '/canchas', { credentials: 'include' }),
        fetch(API_URL + '/usuarios', { credentials: 'include' })
      ]);

      if (!respuestaReservas.ok || !respuestaCanchas.ok || !respuestaUsuarios.ok) {
        throw new Error('Error HTTP al cargar reservas del panel admin');
      }

      const [reservasRaw, canchasRaw, usuariosRaw] = await Promise.all([
        respuestaReservas.json(),
        respuestaCanchas.json(),
        respuestaUsuarios.json()
      ]);

      const reservas = asArray(reservasRaw, 'GET /reservas (admin)');
      const canchas = asArray(canchasRaw, 'GET /canchas (admin)');
      const usuarios = asArray(usuariosRaw, 'GET /usuarios (admin)');

      return reservas.map(function (r) {
        const cancha = canchas.find(function (c) { return String(c.id) === String(r.canchaId); });
        const usuario = usuarios.find(function (u) { return String(u.id) === String(r.usuarioId); });

        return {
          id: String(r.id),
          user: usuario ? (usuario.nombre + ' (' + usuario.email + ')') : ('Usuario #' + r.usuarioId),
          item: cancha ? cancha.name : ('Cancha #' + r.canchaId),
          date: r.fecha,
          time: String(r.horaInicio).slice(0, 5) + ' — ' + String(r.horaFin).slice(0, 5),
          price: cancha ? Number(cancha.price) : 0,
          status: r.estado
        };
      });

    } catch (error) {
      console.error('Error al cargar reservas (panel admin):', error);
      return [];
    }
  }

  cargarReservasAdminDesdeAPI().then(function (data) {
    reservations = data;
    renderReservations();
  });

  const sportLabels = { fulbito: '⚽ Fulbito', futbol: '🏟️ Fútbol', tenis: '🎾 Tenis', piscina: '🏊 Piscina' };

  // ---- Tabs ----
  const adminTabs = document.querySelectorAll('.admin-tab');
  const adminPanels = document.querySelectorAll('.admin-panel');
  adminTabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      adminTabs.forEach(function (t) { t.classList.remove('active'); });
      adminPanels.forEach(function (p) { p.classList.remove('active'); });
      tab.classList.add('active');
      document.getElementById('panel-' + tab.dataset.tab).classList.add('active');
    });
  });

  // ---- Render: Canchas ----
  const courtsAdminGrid = document.getElementById('admin-courts-grid');
  function renderCourts() {
    courtsAdminGrid.innerHTML = courts.map(function (c) {
      return '<div class="admin-item-card" data-id="' + c.id + '">' +
        '<div class="admin-item-media" style="background-image:url(\'' + escapeHtml(c.image) + '\')">' +
        '<span class="item-tag">' + escapeHtml(sportLabels[c.sport] || c.sport) + '</span>' +
        '</div>' +
        '<div class="admin-item-body">' +
        '<h3>' + escapeHtml(c.name) + '</h3>' +
        '<p class="item-sub">' + escapeHtml(c.status.replace('_', ' ')) + ' · hasta ' + c.capacity + ' personas</p>' +
        '<div class="item-price">S/ ' + c.price + ' /hora</div>' +
        '<div class="admin-item-actions">' +
        '<button class="edit-item-btn" data-type="court" data-id="' + c.id + '">Editar</button>' +
        '<button class="delete-item-btn" data-type="court" data-id="' + c.id + '">Eliminar</button>' +
        '</div>' +
        '</div>' +
        '</div>';
    }).join('');
  }

  // ---- Render: Clases ----
  const classesAdminGrid = document.getElementById('admin-classes-grid');
  function renderClasses() {
    classesAdminGrid.innerHTML = classes.map(function (k) {
      return '<div class="admin-item-card" data-id="' + k.id + '">' +
        '<div class="admin-item-icon">' + escapeHtml(k.icon) + '</div>' +
        '<div class="admin-item-body">' +
        '<h3>' + escapeHtml(k.name) + '</h3>' +
        '<p class="item-sub">' + escapeHtml(k.level) + ' · ' + escapeHtml(k.schedule) + ' · ' + k.slots + ' cupos</p>' +
        '<div class="item-price">S/ ' + k.price + ' /mes</div>' +
        '<div class="admin-item-actions">' +
        '<button class="edit-item-btn" data-type="class" data-id="' + k.id + '">Editar</button>' +
        '<button class="delete-item-btn" data-type="class" data-id="' + k.id + '">Eliminar</button>' +
        '</div>' +
        '</div>' +
        '</div>';
    }).join('');
  }

  // ---- Render: Reservas ----
  const reservationsTableBody = document.getElementById('admin-reservations-body');
  function renderReservations() {
    reservationsTableBody.innerHTML = reservations.map(function (r) {
      const cancelada = r.status === 'cancelada';
      return '<tr data-id="' + r.id + '">' +
        '<td>' + escapeHtml(r.user) + '</td>' +
        '<td>' + escapeHtml(r.item) + '</td>' +
        '<td>' + escapeHtml(r.date) + '<br><span style="color:var(--muted);font-size:0.8rem;">' + escapeHtml(r.time) + '</span></td>' +
        '<td>S/ ' + r.price + '</td>' +
        '<td>' + escapeHtml(r.status) + '</td>' +
        '<td>' +
        (cancelada ? '' : '<button class="cancel-item-btn" data-type="reservation" data-id="' + r.id + '">Cancelar</button> ') +
        '<button class="delete-item-btn" data-type="reservation" data-id="' + r.id + '">Eliminar</button>' +
        '</td>' +
        '</tr>';
    }).join('');
  }

  function renderAll() {
    renderCourts();
    renderClasses();
    renderReservations();
  }
  renderAll();

  // ---- Formulario (agregar / editar) ----
  const formPanel = document.getElementById('form-panel');
  const formPanelInner = document.getElementById('form-panel-inner');

  function openCourtForm(existing) {
    const c = existing || { sport: 'fulbito', name: '', desc: '', location: '', price: '', capacity: '', image: '', status: 'disponible' };
    formPanelInner.innerHTML =
      '<h2>' + (existing ? 'Editar cancha' : 'Agregar cancha') + '</h2>' +
      '<form id="item-form">' +
      '<div class="field"><label>Nombre</label><input type="text" id="f-name" value="' + escapeHtml(c.name) + '" required></div>' +
      '<div class="field"><label>Deporte</label><select id="f-sport">' +
      ['fulbito', 'futbol', 'tenis', 'piscina'].map(function (s) {
        return '<option value="' + s + '"' + (c.sport === s ? ' selected' : '') + '>' + (sportLabels[s] || s) + '</option>';
      }).join('') +
      '</select></div>' +
      '<div class="field"><label>Ubicación</label><input type="text" id="f-location" value="' + escapeHtml(c.location || '') + '" required></div>' +
      '<div class="field"><label>Descripción</label><input type="text" id="f-desc" value="' + escapeHtml(c.desc || '') + '"></div>' +
      '<div class="field"><label>Precio por hora (S/)</label><input type="number" id="f-price" value="' + c.price + '" required></div>' +
      '<div class="field"><label>Capacidad</label><input type="number" id="f-capacity" value="' + c.capacity + '" required></div>' +
      '<div class="field"><label>URL de imagen</label><input type="text" id="f-image" value="' + escapeHtml(c.image || '') + '"></div>' +
      '<div class="field"><label>Estado</label><select id="f-status">' +
      '<option value="disponible"' + (c.status === 'disponible' ? ' selected' : '') + '>Disponible</option>' +
      '<option value="no_disponible"' + (c.status === 'no_disponible' ? ' selected' : '') + '>No disponible</option>' +
      '</select></div>' +
      '<div class="form-panel-actions">' +
      '<button type="button" class="cancel-form-btn">Cancelar</button>' +
      '<button type="submit" class="save-btn">Guardar</button>' +
      '</div>' +
      '</form>';

    formPanel.classList.add('open');
    document.querySelector('.cancel-form-btn').addEventListener('click', closeForm);
    document.getElementById('item-form').addEventListener('submit', async function (e) {
      e.preventDefault();
      const data = {
        id: existing ? existing.id : 'c' + Date.now(),
        name: document.getElementById('f-name').value,
        sport: document.getElementById('f-sport').value,
        location: document.getElementById('f-location').value,
        desc: document.getElementById('f-desc').value,
        price: Number(document.getElementById('f-price').value),
        capacity: Number(document.getElementById('f-capacity').value),
        image: document.getElementById('f-image').value || 'https://images.unsplash.com/photo-1551958219-acbc608c6377?w=400&q=80',
        status: document.getElementById('f-status').value
      };
      try {
        const metodo = existing ? 'PUT' : 'POST';
        const url = existing
          ? API_URL + '/canchas/' + data.id
          : API_URL + '/canchas';

        const respuesta = await fetch(url, {
          method: metodo,
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': await getCsrfTokenAsync()
          },
          body: JSON.stringify({
            sport: data.sport,
            name: data.name,
            location: data.location,
            description: data.desc,
            price: data.price,
            capacity: data.capacity,
            image: data.image,
            status: data.status
          })
        });

        if (!respuesta.ok) {
          alert(await parseApiError(respuesta, 'No se pudo guardar la cancha.'));
          return;
        }

        await respuesta.json();

        const canchas = await cargarCanchasDesdeAPI();

        courts = canchas.map(function (c) {
          return {
            id: String(c.id),
            sport: c.sport,
            name: c.name,
            desc: c.description || '',
            location: c.location || '',
            price: Number(c.price),
            capacity: Number(c.capacity),
            image: c.image || '',
            status: c.status
          };
        });

        renderCourts();
        closeForm();

        alert(
          existing
            ? 'Cancha actualizada correctamente en MySQL.'
            : 'Cancha guardada correctamente en MySQL.'
        );

      } catch (error) {
        console.error('Error al guardar la cancha:', error);
        alert('No se pudo guardar la cancha en MySQL.');
      }
    });
  }

  function openClassForm(existing) {
    const k = existing || { icon: '⚽', name: '', level: '', schedule: '', professor: '', price: '', slots: '' };
    formPanelInner.innerHTML =
      '<h2>' + (existing ? 'Editar clase' : 'Agregar clase') + '</h2>' +
      '<form id="item-form">' +
      '<div class="field"><label>Nombre</label><input type="text" id="f-name" value="' + escapeHtml(k.name) + '" required></div>' +
      '<div class="field"><label>Emoji / ícono</label><input type="text" id="f-icon" value="' + escapeHtml(k.icon) + '"></div>' +
      '<div class="field"><label>Nivel / edades</label><input type="text" id="f-level" value="' + escapeHtml(k.level) + '"></div>' +
      '<div class="field"><label>Horario</label><input type="text" id="f-schedule" value="' + escapeHtml(k.schedule) + '"></div>' +
      '<div class="field"><label>Profesor</label><input type="text" id="f-professor" value="' + escapeHtml(k.professor) + '"></div>' +
      '<div class="field"><label>Precio mensual (S/)</label><input type="number" id="f-price" value="' + k.price + '" required></div>' +
      '<div class="field"><label>Cupos disponibles</label><input type="number" id="f-slots" value="' + k.slots + '" required></div>' +
      '<div class="form-panel-actions">' +
      '<button type="button" class="cancel-form-btn">Cancelar</button>' +
      '<button type="submit" class="save-btn">Guardar</button>' +
      '</div>' +
      '</form>';

    formPanel.classList.add('open');
    document.querySelector('.cancel-form-btn').addEventListener('click', closeForm);
    document.getElementById('item-form').addEventListener('submit', async function (e) {
      e.preventDefault();
      const data = {
        id: existing ? existing.id : 'k' + Date.now(),
        name: document.getElementById('f-name').value,
        icon: document.getElementById('f-icon').value || '⚽',
        level: document.getElementById('f-level').value,
        schedule: document.getElementById('f-schedule').value,
        professor: document.getElementById('f-professor').value,
        price: Number(document.getElementById('f-price').value),
        slots: Number(document.getElementById('f-slots').value)
      };
      try {
        const url = existing
          ? API_URL + '/clases/' + existing.id
          : API_URL + '/clases';

        const method = existing ? 'PUT' : 'POST';

        if (!existing) {
          delete data.id;
        }

        const respuesta = await fetch(url, {
          method: method,
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': await getCsrfTokenAsync()
          },
          body: JSON.stringify(data)
        });

        if (!respuesta.ok) {
          alert(await parseApiError(respuesta, 'No se pudo guardar la clase.'));
          return;
        }

        await respuesta.json();

        const clasesActualizadas = await cargarClasesDesdeAPI();

        clasesDesdeAPI = clasesActualizadas.map(mapClaseDesdeAPI);

        classes = clasesDesdeAPI;

        renderClasses();
        closeForm();

        alert(existing
          ? 'Clase actualizada correctamente'
          : 'Clase agregada correctamente'
        );

      } catch (error) {
        console.error('Error al guardar clase:', error);
        alert('No se pudo guardar la clase en MySQL');
      }
    });
  }

  function closeForm() {
    formPanel.classList.remove('open');
    formPanelInner.innerHTML = '';
  }

  // Botón "Agregar" según la pestaña activa
  document.getElementById('add-court-btn').addEventListener('click', function () { openCourtForm(null); });
  document.getElementById('add-class-btn').addEventListener('click', function () { openClassForm(null); });

  // Delegación de eventos: editar / eliminar (se re-renderiza el DOM cada vez)
  document.addEventListener('click', async function (e) {
    const editBtn = e.target.closest('.edit-item-btn');
    const delBtn = e.target.closest('.delete-item-btn');
    const cancelBtn = e.target.closest('.cancel-item-btn');

    if (cancelBtn && cancelBtn.dataset.type === 'reservation') {
      const id = cancelBtn.dataset.id;

      if (!confirm('¿Seguro que deseas cancelar esta reserva?')) {
        return;
      }

      try {
        const respuesta = await fetch(API_URL + '/reservas/' + id + '/cancelar', {
          method: 'PUT',
          credentials: 'include',
          headers: {
            'X-XSRF-TOKEN': await getCsrfTokenAsync()
          }
        });

        if (!respuesta.ok) {
          alert(await parseApiError(respuesta, 'No se pudo cancelar la reserva.'));
          return;
        }

        reservations = await cargarReservasAdminDesdeAPI();
        renderReservations();

        alert('Reserva cancelada correctamente.');

      } catch (error) {
        console.error('Error al cancelar la reserva:', error);
        alert('No se pudo cancelar la reserva.');
      }
    }

    if (editBtn) {
      const id = editBtn.dataset.id;
      if (editBtn.dataset.type === 'court') {
        openCourtForm(courts.find(function (c) { return c.id === id; }));
      } else if (editBtn.dataset.type === 'class') {
        const clase = clasesDesdeAPI.find(function (k) {
          return String(k.id) === String(id);
        });

        openClassForm(clase);
      }
    }

    if (delBtn) {
      const id = delBtn.dataset.id;

      // =========================
      // ELIMINAR CANCHA
      // =========================
      if (delBtn.dataset.type === 'court') {

        if (!confirm('¿Seguro que deseas eliminar esta cancha?')) {
          return;
        }

        try {
          const respuesta = await fetch(API_URL + '/canchas/' + id, {
            method: 'DELETE',
            credentials: 'include',
            headers: {
              'X-XSRF-TOKEN': await getCsrfTokenAsync()
            }
          });

          if (!respuesta.ok) {
            alert(await parseApiError(respuesta, 'No se pudo eliminar la cancha.'));
            return;
          }

          courts = courts.filter(function (c) {
            return String(c.id) !== String(id);
          });

          renderCourts();

          alert('Cancha eliminada correctamente de MySQL.');

        } catch (error) {
          console.error('Error al eliminar la cancha:', error);
          alert('No se pudo eliminar la cancha de MySQL.');
        }

        // =========================
        // ELIMINAR CLASE
        // =========================
      } else if (delBtn.dataset.type === 'class') {

        if (!confirm('¿Seguro que deseas eliminar esta clase?')) {
          return;
        }

        try {
          const respuesta = await fetch(API_URL + '/clases/' + id, {
            method: 'DELETE',
            credentials: 'include',
            headers: {
              'X-XSRF-TOKEN': await getCsrfTokenAsync()
            }
          });

          if (!respuesta.ok) {
            alert(await parseApiError(respuesta, 'No se pudo eliminar la clase.'));
            return;
          }

          const clasesActualizadas = await cargarClasesDesdeAPI();

          clasesDesdeAPI = clasesActualizadas.map(mapClaseDesdeAPI);

          classes = clasesDesdeAPI;

          renderClasses();

          alert('Clase eliminada correctamente de MySQL.');

        } catch (error) {
          console.error('Error al eliminar la clase:', error);
          alert('No se pudo eliminar la clase de MySQL.');
        }

        // =========================
        // ELIMINAR RESERVA
        // =========================
      } else if (delBtn.dataset.type === 'reservation') {

        if (!confirm('¿Seguro que deseas eliminar esta reserva?')) {
          return;
        }

        try {
          const respuesta = await fetch(API_URL + '/reservas/' + id, {
            method: 'DELETE',
            credentials: 'include',
            headers: {
              'X-XSRF-TOKEN': await getCsrfTokenAsync()
            }
          });

          if (!respuesta.ok) {
            alert(await parseApiError(respuesta, 'No se pudo eliminar la reserva.'));
            return;
          }

          reservations = await cargarReservasAdminDesdeAPI();
          renderReservations();

          alert('Reserva eliminada correctamente de MySQL.');

        } catch (error) {
          console.error('Error al eliminar la reserva:', error);
          alert('No se pudo eliminar la reserva de MySQL.');
        }
      }
    }
  });

  // Cerrar el formulario haciendo clic fuera de él
  formPanel.addEventListener('click', function (e) {
    if (e.target === formPanel) closeForm();
  });
}
