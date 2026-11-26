const TOKEN_KEY = 'quipux_jwt';

/**
 * Local: vacío → Vite proxy a Spring :8080
 * Vercel: vacío → rewrites a /api/* (adaptador serverless del mismo repo)
 * Opcional: VITE_API_BASE_URL si apuntas a Spring publicado aparte
 */
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

async function request(path, options = {}) {
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
        : 'No se pudo conectar con la API'
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
    const message = toErrorMessage(
      data?.message ?? data?.error,
      `HTTP ${response.status}`
    );
    const error = new Error(message);
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
