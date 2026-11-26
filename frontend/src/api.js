const TOKEN_KEY = 'quipux_jwt';

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY);
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

  const response = await fetch(path, { ...options, headers });
  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const message = data?.message || data?.error || `HTTP ${response.status}`;
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
