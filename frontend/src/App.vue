<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  clearToken,
  createList,
  deleteList,
  getList,
  getRecommendations,
  getToken,
  listLists,
  login as apiLogin,
  setToken
} from './api';

const token = ref(getToken());
const username = ref('demo');
const lists = ref([]);
const selectedName = ref('');
const selected = ref(null);
const recommendations = ref(null);
const loading = ref(false);
const error = ref('');

const form = reactive({
  nombre: '',
  descripcion: '',
  titulo: '',
  artista: '',
  album: '',
  anno: '',
  genero: ''
});

const isLoggedIn = computed(() => Boolean(token.value));
const songCount = computed(() => selected.value?.canciones?.length || 0);

async function handleLogin() {
  error.value = '';
  loading.value = true;
  try {
    const data = await apiLogin(username.value.trim() || 'demo');
    setToken(data.accessToken);
    token.value = data.accessToken;
    await refreshLists();
  } catch (e) {
    error.value = typeof e?.message === 'string' ? e.message : 'No se pudo iniciar sesión';
  } finally {
    loading.value = false;
  }
}

function logout() {
  clearToken();
  token.value = null;
  lists.value = [];
  selected.value = null;
  selectedName.value = '';
  recommendations.value = null;
  error.value = '';
}

async function refreshLists() {
  lists.value = await listLists();
}

async function selectList(name) {
  error.value = '';
  recommendations.value = null;
  selectedName.value = name;
  loading.value = true;
  try {
    selected.value = await getList(name);
  } catch (e) {
    error.value = e.message;
    selected.value = null;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.nombre = '';
  form.descripcion = '';
  form.titulo = '';
  form.artista = '';
  form.album = '';
  form.anno = '';
  form.genero = '';
}

async function handleCreate() {
  error.value = '';
  if (!form.nombre.trim()) {
    error.value = 'Indica un nombre para la lista';
    return;
  }
  loading.value = true;
  try {
    const canciones = [];
    if (form.titulo.trim() || form.artista.trim()) {
      canciones.push({
        titulo: form.titulo.trim(),
        artista: form.artista.trim(),
        album: form.album.trim(),
        anno: form.anno.trim(),
        genero: form.genero.trim()
      });
    }
    const created = await createList({
      nombre: form.nombre.trim(),
      descripcion: form.descripcion.trim(),
      canciones
    });
    resetForm();
    await refreshLists();
    await selectList(created.nombre);
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function handleDelete(name) {
  error.value = '';
  if (!confirm(`¿Eliminar “${name}”?`)) return;
  loading.value = true;
  try {
    await deleteList(name);
    if (selectedName.value === name) {
      selectedName.value = '';
      selected.value = null;
      recommendations.value = null;
    }
    await refreshLists();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

async function handleRecommendations() {
  if (!selectedName.value) return;
  error.value = '';
  loading.value = true;
  try {
    recommendations.value = await getRecommendations(selectedName.value);
  } catch (e) {
    recommendations.value = null;
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  if (!token.value) return;
  try {
    await refreshLists();
  } catch (e) {
    if (e.status === 401) logout();
    else error.value = e.message;
  }
});
</script>

<template>
  <div v-if="!isLoggedIn" class="login-wrap">
    <section class="login-card">
      <h1>Quipux <span>Lists</span></h1>
      <label>
        Usuario
        <input v-model="username" autocomplete="username" @keyup.enter="handleLogin" />
      </label>
      <button :disabled="loading" @click="handleLogin">Entrar</button>
      <p v-if="error" class="error-banner" style="margin: 0">{{ error }}</p>
    </section>
  </div>

  <div v-else class="app">
    <header class="topbar">
      <h1 class="logo">Quipux <span>Lists</span></h1>
      <button class="secondary" @click="logout">Salir</button>
    </header>

    <p v-if="error" class="error-banner">{{ error }}</p>

    <div class="workspace">
      <aside class="column">
        <div class="column-head">
          <h2>Listas</h2>
          <button class="ghost" :disabled="loading" @click="refreshLists">↻</button>
        </div>
        <ul class="playlist-list">
          <li
            v-for="item in lists"
            :key="item.nombre"
            class="playlist-item"
            :class="{ active: item.nombre === selectedName }"
          >
            <button class="select" @click="selectList(item.nombre)">{{ item.nombre }}</button>
            <button class="danger-ghost" :disabled="loading" @click="handleDelete(item.nombre)">✕</button>
          </li>
        </ul>
        <p v-if="!lists.length" class="empty">Sin listas</p>
      </aside>

      <main class="column detail">
        <template v-if="selected">
          <div class="detail-header">
            <h1>{{ selected.nombre }}</h1>
            <p v-if="selected.descripcion">{{ selected.descripcion }}</p>
          </div>
          <div class="meta-row">
            <span class="badge">{{ songCount }} canciones</span>
            <button :disabled="loading" @click="handleRecommendations">Recomendar</button>
          </div>

          <div class="songs">
            <div v-for="(song, i) in selected.canciones || []" :key="i" class="song">
              <strong>{{ song.titulo || '—' }}</strong>
              <span class="sub">
                {{ [song.artista, song.album, song.genero, song.anno].filter(Boolean).join(' · ') }}
              </span>
            </div>
            <p v-if="!songCount" class="empty">Lista vacía</p>
          </div>

          <template v-if="recommendations">
            <div class="divider"></div>
            <div class="meta-row">
              <h2 class="section-label" style="margin-right: auto">Recomendaciones</h2>
              <span
                class="badge"
                :class="recommendations.source === 'FALLBACK' ? 'fallback' : 'ai'"
              >
                {{ recommendations.source === 'FALLBACK' ? 'Fallback' : 'IA' }}
              </span>
            </div>
            <div class="songs">
              <div
                v-for="(rec, i) in recommendations.recommendations || []"
                :key="i"
                class="song"
              >
                <strong>{{ rec.titulo }}</strong>
                <span class="sub">{{ [rec.artista, rec.genero].filter(Boolean).join(' · ') }}</span>
                <span v-if="rec.motivo" class="motivo">{{ rec.motivo }}</span>
              </div>
            </div>
          </template>
        </template>

        <div v-else class="placeholder-detail">
          <p>Selecciona una lista</p>
        </div>
      </main>

      <aside class="column">
        <div class="column-head">
          <h2>Nueva lista</h2>
        </div>
        <div class="form-stack">
          <label>
            Nombre
            <input v-model="form.nombre" />
          </label>
          <label>
            Descripción
            <textarea v-model="form.descripcion" rows="2" />
          </label>
          <div class="divider"></div>
          <p class="section-label">Canción</p>
          <div class="fields-2">
            <label>Título <input v-model="form.titulo" /></label>
            <label>Artista <input v-model="form.artista" /></label>
            <label>Álbum <input v-model="form.album" /></label>
            <label>Año <input v-model="form.anno" /></label>
            <label>Género <input v-model="form.genero" /></label>
          </div>
          <div class="actions">
            <button :disabled="loading" @click="handleCreate">Crear</button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
