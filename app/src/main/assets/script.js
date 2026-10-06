// State management
let accessToken = localStorage.getItem('access_token');
let refreshToken = localStorage.getItem('refresh_token');
let currentDate = new Date();
let cachedPlanning = {};
let userInfo = null;

// DOM Elements
const elLoading = document.getElementById('loading');
const elLoginScreen = document.getElementById('login-screen');
const elAppScreen = document.getElementById('app-screen');
const elUserInfo = document.getElementById('user-info');
const elCurrentWeekLabel = document.getElementById('current-week-label');
const elAgendaList = document.getElementById('agenda-list');

const btnLogin = document.getElementById('btn-login');
const btnLogout = document.getElementById('btn-logout');
const btnPrevWeek = document.getElementById('btn-prev-week');
const btnNextWeek = document.getElementById('btn-next-week');

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
  initListeners();

  const urlParams = new URLSearchParams(window.location.search);
  const webAuth = urlParams.get('web_auth');
  if (webAuth) {
    window.history.replaceState({}, document.title, window.location.pathname);
    localStorage.setItem('web_authenticated', '1');
    checkAuth();
  } else {
    checkAuth();
  }

  lucide.createIcons();
});

function initListeners() {
  btnLogin.addEventListener('click', handleOAuthLogin);
  btnLogout.addEventListener('click', handleLogout);
  btnPrevWeek.addEventListener('click', () => changeWeek(-7));
  btnNextWeek.addEventListener('click', () => changeWeek(7));
}

async function checkAuth() {
  showLoading(true);
  const isWebAuth = localStorage.getItem('web_authenticated') === '1';
  if (isWebAuth || accessToken) {
    try {
      await fetchUserProfile();
      await loadCurrentWeekAgenda();
      showScreen('app');
    } catch (err) {
      console.error("Auth check failed", err);
      if (!isWebAuth && accessToken) {
        const refreshed = await handleTokenRefresh();
        if (refreshed) {
          await checkAuth();
          return;
        }
      }
      handleLogout();
    }
  } else {
    showScreen('login');
  }
  showLoading(false);
}

// ─── XSRF-TOKEN pre-fetch ─────────────────────────────────────────────────────
// Hit the api-mobile root to seed the XSRF-TOKEN into CookieManager BEFORE login.
// postSecure() will then forward it automatically alongside the session cookie.
async function fetchXsrfToken() {
  try {
    if (window.Android && window.Android.getSecure) {
      const responseStr = window.Android.getSecure('https://www.myefrei.fr/api-mobile/');
      const res = JSON.parse(responseStr);
      console.error("XSRF pre-fetch status: " + res.status);
    } else {
      await fetch('https://www.myefrei.fr/api-mobile/', { credentials: 'include' });
    }
    console.error("XSRF pre-fetch complete — XSRF-TOKEN seeded in CookieManager");
  } catch(e) {
    console.error("XSRF pre-fetch error (non-fatal): " + e.message);
  }
}

// ─── PKCE helpers ─────────────────────────────────────────────────────────────
function sha256(ascii) {
  function rightRotate(value, amount) { return (value >>> amount) | (value << (32 - amount)); }
  const mathPow = Math.pow, maxWord = mathPow(2, 32), lengthProperty = 'length';
  let i, j;
  const words = [], asciiLength = ascii[lengthProperty] * 8;
  const hash = [0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19];
  const k = [
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbEF9a3f7,0xc67178f2
  ];
  let asciiWithPadding = ascii + '\x80';
  while (asciiWithPadding[lengthProperty] % 64 - 56) asciiWithPadding += '\x00';
  for (i = 0; i < asciiWithPadding[lengthProperty]; i++) {
    j = asciiWithPadding.charCodeAt(i);
    words[i >> 2] |= j << (24 - (i % 4) * 8);
  }
  words[words[lengthProperty]] = ((asciiLength / maxWord) | 0);
  words[words[lengthProperty]] = (asciiLength | 0);
  for (j = 0; j < words[lengthProperty];) {
    const w = words.slice(j, j += 16), oldHash = hash.slice(0);
    for (i = 0; i < 64; i++) {
      const w16=w[i-16],w15=w[i-15],w7=w[i-7],w2=w[i-2];
      const s0=rightRotate(w15,7)^rightRotate(w15,18)^(w15>>>3);
      const s1=rightRotate(w2,17)^rightRotate(w2,19)^(w2>>>10);
      const temp1=w[i]=i<16?(w[i]||0):(w16+s0+w7+s1)|0;
      const s3=rightRotate(oldHash[0],2)^rightRotate(oldHash[0],13)^rightRotate(oldHash[0],22);
      const t1=(oldHash[7]+(rightRotate(oldHash[4],6)^rightRotate(oldHash[4],11)^rightRotate(oldHash[4],25))+((oldHash[4]&oldHash[5])^(~oldHash[4]&oldHash[6]))+k[i]+temp1)|0;
      const t2=(s3+((oldHash[0]&oldHash[1])^(oldHash[0]&oldHash[2])^(oldHash[1]&oldHash[2])))|0;
      oldHash.pop(); oldHash.unshift((t1+t2)|0); oldHash[4]=(oldHash[4]+t1)|0;
    }
    for (i = 0; i < 8; i++) hash[i] = (hash[i] + oldHash[i]) | 0;
  }
  const byteArray = new Uint8Array(32);
  for (i = 0; i < 8; i++) {
    const word = hash[i];
    byteArray[i*4]=(word>>>24)&0xff; byteArray[i*4+1]=(word>>>16)&0xff;
    byteArray[i*4+2]=(word>>>8)&0xff; byteArray[i*4+3]=word&0xff;
  }
  return byteArray;
}

function generateVerifier() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let v = '';
  for (let i = 0; i < 64; i++) v += chars.charAt(Math.floor(Math.random() * chars.length));
  return v;
}

function generatePkce() {
  const verifier = generateVerifier();
  const rawHash = sha256(verifier);
  let binary = '';
  for (let i = 0; i < rawHash.byteLength; i++) binary += String.fromCharCode(rawHash[i]);
  const challenge = btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return { verifier, challenge };
}

// ─── Login flow ───────────────────────────────────────────────────────────────
async function handleOAuthLogin() {
  console.error("handleOAuthLogin clicked: redirecting to Efrei Web SSO");
  showLoading(true);
  try {
    // Web SSO gateway on myefrei.fr sets myefrei.sid without App Check / Play Integrity
    const webAuthUrl = "https://www.myefrei.fr/auth/efrei?redirectPath=" + encodeURIComponent("home");
    console.error("Redirecting to Web SSO: " + webAuthUrl);
    window.location.href = webAuthUrl;
  } catch (err) {
    console.error("handleOAuthLogin error: " + err.message);
    showLoading(false);
  }
}

// ─── Native bridge helpers ────────────────────────────────────────────────────
async function nativeGet(url) {
  if (window.Android && window.Android.getSecure) {
    const responseStr = window.Android.getSecure(url);
    const res = JSON.parse(responseStr);
    return { ok: res.status >= 200 && res.status < 300, status: res.status,
             json: async () => JSON.parse(res.body), text: async () => res.body };
  }
  return fetch(url, { credentials: 'include' });
}

async function nativePost(url, dataStr, contentType) {
  if (window.Android && window.Android.postSecure) {
    console.error("Native POST to: " + url);
    const responseStr = window.Android.postSecure(url, contentType, dataStr);
    const res = JSON.parse(responseStr);
    console.error("Native POST status: " + res.status);
    return { ok: res.status >= 200 && res.status < 300, status: res.status,
             json: async () => JSON.parse(res.body), text: async () => res.body };
  }
  return fetch(url, { method: 'POST', headers: { 'Content-Type': contentType }, body: dataStr });
}

// ─── Auth code exchange ───────────────────────────────────────────────────────
window.handleAuthCode = function(code) {
  console.error("handleAuthCode called with code: " + code);
  showLoading(true);

  const verifier = localStorage.getItem('code_verifier');
  const payload = new URLSearchParams();
  payload.append('grant_type', 'authorization_code');
  payload.append('code', code);
  payload.append('redirect_uri', 'fr.myefrei.app://');
  payload.append('client_id', 'mobile-prod');
  if (verifier) payload.append('code_verifier', verifier);

  console.error("Token payload: " + payload.toString());

  nativePost(
    'https://www.myefrei.fr/api-mobile/rest/public/token',
    payload.toString(),
    'application/x-www-form-urlencoded;charset=utf-8'
  )
  .then(async res => {
    console.error("Token response status: " + res.status);
    if (!res.ok) {
      const errorText = await res.text();
      console.error("Token error: " + errorText);
      throw new Error("Token exchange failed: " + res.status + " - " + errorText);
    }
    return res.json();
  })
  .then(data => {
    if (data.access_token) {
      localStorage.setItem('access_token', data.access_token);
      if (data.refresh_token) localStorage.setItem('refresh_token', data.refresh_token);
      accessToken = data.access_token;
      refreshToken = data.refresh_token;
      localStorage.removeItem('code_verifier');
      checkAuth();
    } else {
      console.error("No access_token: " + JSON.stringify(data));
      alert("Erreur de connexion : " + JSON.stringify(data));
      showScreen('login');
    }
  })
  .catch(err => {
    console.error("handleAuthCode error: " + err.message);
    alert("Erreur de connexion : " + err.message);
    showScreen('login');
  })
  .finally(() => showLoading(false));
};

// ─── Token refresh ────────────────────────────────────────────────────────────
async function handleTokenRefresh() {
  if (!refreshToken) return false;
  const payload = new URLSearchParams();
  payload.append('grant_type', 'refresh_token');
  payload.append('refresh_token', refreshToken);
  payload.append('client_id', 'mobile-prod');
  try {
    const res = await nativePost(
      'https://www.myefrei.fr/api-mobile/rest/public/refresh-token',
      payload.toString(),
      'application/x-www-form-urlencoded;charset=utf-8'
    );
    if (!res.ok) return false;
    const data = await res.json();
    if (data.access_token) {
      localStorage.setItem('access_token', data.access_token);
      accessToken = data.access_token;
      if (data.refresh_token) { localStorage.setItem('refresh_token', data.refresh_token); refreshToken = data.refresh_token; }
      return true;
    }
  } catch (err) { console.error("Token refresh failed", err); }
  return false;
}

// ─── User profile ─────────────────────────────────────────────────────────────
async function fetchUserProfile() {
  try {
    let res = await nativeGet('https://www.myefrei.fr/api/rest/student/user/info');
    if (!res.ok) {
      res = await nativeGet('https://www.myefrei.fr/rest/student/user/info');
    }
    if (res.ok) {
      userInfo = await res.json();
      const displayName = userInfo.firstName
        ? `${userInfo.firstName} ${userInfo.lastName || ''}`.trim()
        : (userInfo.name || userInfo.username || 'Étudiant');
      elUserInfo.innerText = displayName;
      return;
    }
  } catch(e) {}
  elUserInfo.innerText = 'Étudiant';
}

// ─── Planning fetch ───────────────────────────────────────────────────────────
async function fetchWeekPlanning(startDate, endDate) {
  const startStr = startDate.toISOString();
  const endStr = endDate.toISOString();
  const cacheKey = `${startStr}_${endStr}`;
  if (cachedPlanning[cacheKey]) return cachedPlanning[cacheKey];

  const url = `https://www.myefrei.fr/api/rest/student/planning?startDate=${encodeURIComponent(startStr)}&endDate=${encodeURIComponent(endStr)}`;
  console.error("Fetching planning from: " + url);
  const res = await nativeGet(url);

  if (res.status === 401 || res.status === 403) throw new Error("Unauthorized (" + res.status + ")");
  if (!res.ok) throw new Error("Failed to fetch planning: " + res.status);
  const data = await res.json();
  const events = Array.isArray(data) ? data : [];
  cachedPlanning[cacheKey] = events;
  return events;
}

// ─── Agenda rendering ─────────────────────────────────────────────────────────
async function loadCurrentWeekAgenda() {
  showLoading(true);
  try {
    const weekDays = getWeekDays(currentDate);
    const startOfWeek = new Date(weekDays[0]);
    startOfWeek.setHours(0, 0, 0, 0);
    const endOfWeek = new Date(weekDays[6]);
    endOfWeek.setHours(23, 59, 59, 999);

    const options = { day: 'numeric', month: 'long', year: 'numeric' };
    elCurrentWeekLabel.innerText = `Semaine du ${weekDays[0].toLocaleDateString('fr-FR', options)}`;

    const weekCourses = await fetchWeekPlanning(startOfWeek, endOfWeek);
    renderAgenda(weekDays, weekCourses);
  } catch (err) {
    console.error("Failed to load agenda", err);
    if (err.message && err.message.includes("Unauthorized")) {
      handleLogout();
    } else {
      elAgendaList.innerHTML = `<div class="empty-state"><i data-lucide="alert-circle"></i><p>Impossible de charger l'agenda.</p><button onclick="loadCurrentWeekAgenda()">Réessayer</button></div>`;
      lucide.createIcons();
    }
  } finally {
    showLoading(false);
  }
}

function renderAgenda(weekDays, courses) {
  elAgendaList.innerHTML = '';
  courses.sort((a, b) => (parseCourseDate(a.start || a.timeFrom) || new Date(0)) - (parseCourseDate(b.start || b.timeFrom) || new Date(0)));
  weekDays.forEach(day => {
    const dayCourses = courses.filter(c => {
      const cd = parseCourseDate(c.start || c.timeFrom);
      return cd && cd.getDate() === day.getDate() && cd.getMonth() === day.getMonth() && cd.getFullYear() === day.getFullYear();
    });
    const dayName = day.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
    const isToday = new Date().toDateString() === day.toDateString();
    const daySection = document.createElement('div');
    daySection.className = "day-section";
    daySection.innerHTML = `<h3 class="day-title ${isToday ? 'today' : ''}">${isToday ? '<span class="today-dot"></span>' : ''}${dayName} ${isToday ? "(Aujourd'hui)" : ''}</h3>`;
    const cardList = document.createElement('div');
    cardList.className = "day-courses";
    if (dayCourses.length === 0) {
      cardList.innerHTML = `<div class="empty-day-card"><i data-lucide="smile"></i> Aucun cours prévu</div>`;
    } else {
      dayCourses.forEach(c => {
        const start = parseCourseDate(c.start || c.timeFrom), end = parseCourseDate(c.end || c.timeTo);
        const nameLower = (c.name || '').toLowerCase(), modalityLower = (c.modality || '').toLowerCase();
        let styleClass = "";
        if (c.sessionType === 'exam' || nameLower.includes('exam') || nameLower.includes('partiel') || nameLower.includes('qcm') || nameLower.includes('ds') || nameLower.includes('contrôle')) styleClass = "exam";
        else if (c.modality === 'online' || modalityLower.includes('distanciel') || modalityLower.includes('distant') || modalityLower.includes('virtuel')) styleClass = "distanciel";
        else if (c.courseActivity === 'TP' || nameLower.includes('tp') || nameLower.includes('travaux pratiques')) styleClass = "tp";

        // Extract room / place
        let locationStr = 'Lieu non spécifié';
        if (Array.isArray(c.locations) && c.locations.length > 0) {
          const loc = c.locations[0];
          locationStr = [loc.building || loc.bat, loc.room ? `Salle ${loc.room}` : ''].filter(Boolean).join(' - ') || loc.campus || locationStr;
        } else if (c.place) {
          locationStr = c.place;
        }

        // Extract teachers
        let teacherStr = '';
        if (Array.isArray(c.teachers) && c.teachers.length > 0) {
          teacherStr = c.teachers.join(', ');
        } else if (c.teacher) {
          teacherStr = c.teacher;
        }

        // Activity type label
        const activityBadge = c.courseActivityName || (c.courseActivity ? c.courseActivity : '');

        const card = document.createElement('div');
        card.className = `course-card ${styleClass}`.trim();
        card.innerHTML = `
          <div class="course-time">
            <span class="time-start">${start ? formatTime(start) : (c.startTime || '')}</span>
            <span class="time-end">${end ? formatTime(end) : (c.endTime || '')}</span>
          </div>
          <div class="course-content">
            <h4 class="course-name">${c.name || 'Cours sans nom'}</h4>
            <div class="course-details">
              <span class="detail-item"><i data-lucide="map-pin"></i> ${locationStr}</span>
              ${teacherStr ? `<span class="detail-item"><i data-lucide="user"></i> ${teacherStr}</span>` : ''}
              ${activityBadge ? `<span class="detail-item"><i data-lucide="tag"></i> ${activityBadge}</span>` : ''}
              ${c.modality && c.modality !== 'in_person' ? `<span class="detail-item"><i data-lucide="info"></i> ${c.modality}</span>` : ''}
            </div>
          </div>`;
        cardList.appendChild(card);
      });
    }
    daySection.appendChild(cardList);
    elAgendaList.appendChild(daySection);
  });
  lucide.createIcons();
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function showLoading(show) {
  elLoading.style.display = show ? 'flex' : 'none';
  if (show) { elLoading.classList.remove('opacity-0','pointer-events-none'); }
  else { elLoading.classList.add('opacity-0','pointer-events-none'); setTimeout(() => { elLoading.style.display = 'none'; }, 300); }
}

function showScreen(screen) {
  if (screen === 'login') { elLoginScreen.classList.remove('hidden'); elAppScreen.classList.add('hidden'); }
  else { elLoginScreen.classList.add('hidden'); elAppScreen.classList.remove('hidden'); }
}

function handleLogout() {
  localStorage.removeItem('access_token'); localStorage.removeItem('refresh_token');
  accessToken = null; refreshToken = null;
  cachedPlanning = {}; userInfo = null;
  showScreen('login');
}

function changeWeek(daysOffset) { currentDate.setDate(currentDate.getDate() + daysOffset); loadCurrentWeekAgenda(); }

function parseCourseDate(dateVal) {
  if (!dateVal) return null;
  if (typeof dateVal === 'number' || !isNaN(dateVal)) return new Date(Number(dateVal));
  return new Date(dateVal);
}

function getWeekDays(date) {
  const current = new Date(date), day = current.getDay();
  const diff = current.getDate() - day + (day === 0 ? -6 : 1);
  const startOfWeek = new Date(current.setDate(diff));
  const days = [];
  for (let i = 0; i < 7; i++) { const d = new Date(startOfWeek); d.setDate(startOfWeek.getDate() + i); days.push(d); }
  return days;
}

function formatTime(date) { return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
