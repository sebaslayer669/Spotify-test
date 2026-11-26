import { computed, ref } from 'vue';
import { clearToken, getToken, login as apiLogin, setToken } from '../api';
import { clearListCache } from '../listCache';

const token = ref(getToken());

export function useAuth() {
  const loading = ref(false);
  const error = ref('');
  const username = ref('demo');
  const isLoggedIn = computed(() => Boolean(token.value));

  async function login() {
    error.value = '';
    loading.value = true;
    try {
      const data = await apiLogin(username.value.trim() || 'demo');
      setToken(data.accessToken);
      token.value = data.accessToken;
      return true;
    } catch (e) {
      error.value = typeof e?.message === 'string' ? e.message : 'No se pudo iniciar sesión';
      return false;
    } finally {
      loading.value = false;
    }
  }

  function logout() {
    clearToken();
    clearListCache();
    token.value = null;
    error.value = '';
  }

  return {
    token,
    username,
    loading,
    error,
    isLoggedIn,
    login,
    logout
  };
}
