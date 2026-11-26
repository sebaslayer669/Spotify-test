const TOKEN_KEY = 'quipux_jwt';

/** En Vercel: VITE_API_BASE_URL = URL pública de Spring (NO la URL de Vercel). */
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY);
}

function assertApiConfigured() {
  if (!import.meta.env.PROD) {
    return;
  }
  if (!API_BASE) {
    throw new Error(
      'Falta VITE_API_BASE_URL en Vercel (URL de la API Spring, no del front). Redeploy tras guardarla.'
    );
  }
  try {
    const apiHost = new URL(API_BASE).host;
    if (typeof window !== 'undefined' && apiHost === window.location.host) {
      throw new Error(
        'VITE_API_BASE_URL apunta al mismo host del front. Debe ser la URL del backend Spring.'
      );
    }
  } catch (e) {
    if (e instanceof Error && e.message.includes('VITE_API_BASE_URL')) {
      throw e;
    }
    throw new Error('VITE_API_BASE_URL no es una URL válida');
  }
}

function toErrorMessage(value, fallback) {
  if (value == null || value === '') {
    return fallback;
  }
  if (typeof value === 'string') {
    return value;
  }
  if (typeof value === 'object') {
    if (typeof value.message === 'string') {
      return value.message;
    }
    try {
      return JSON.stringify(value);
    } catch {
      return fallback;
    }
  }
  return String(value);
}

function statusMessage(status, data) {
  if (status === 405) {
    return (
      'HTTP 405: el POST no llegó a Spring. En Vercel configura VITE_API_BASE_URL ' +
      'con la URL de la API (ej. https://tu-api.onrender.com) y vuelve a desplegar.'
    );
  }
  if (status === 404) {
    return (
      'HTTP 404: ruta no encontrada en el host configurado. Revisa VITE_API_BASE_URL.'
    );
  }
  return toErrorMessage(data?.message ?? data?.error, `HTTP ${status}`);
}

async function request(path, options = {}) {
  assertApiConfigured();

  const headers = {
    Accept: 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...options.headers
  };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const url = `${API_BASE}${path}`;
  let response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch {
    throw new Error(
      API_BASE
        ? `No se pudo conectar con la API (${API_BASE})`
        : 'API no configurada: define VITE_API_BASE_URL en Vercel'
    );
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text.slice(0, 200) };
    }
  }

  if (!response.ok) {
    const error = new Error(statusMessage(response.status, data));
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

export function login(username) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: username || 'demo' })
  });
}

export function listLists() {
  return request('/lists');
}

export function getList(listName) {
  return request(`/lists/${encodeURIComponent(listName)}`);
}

export function createList(payload) {
  return request('/lists', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function deleteList(listName) {
  return request(`/lists/${encodeURIComponent(listName)}`, { method: 'DELETE' });
}

export function getRecommendations(listName) {
  return request(`/lists/${encodeURIComponent(listName)}/recommendations`);
}

export function getSpotifyGenres() {
  return request('/spotify/genres');
}
