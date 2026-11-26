<script setup>
import { onMounted, watch } from 'vue';
import { useAuth } from './composables/useAuth';
import { usePlaylists } from './composables/usePlaylists';

const auth = useAuth();
const playlists = usePlaylists();

watch(
  () => auth.isLoggedIn.value,
  (logged) => {
    if (!logged) playlists.clearSelection();
  }
);

async function handleLogin() {
  const ok = await auth.login();
  if (ok) {
    try {
      await playlists.refreshLists();
    } catch (e) {
      if (e.status === 401) auth.logout();
      else playlists.error.value = e.message;
    }
  }
}

onMounted(async () => {
  if (!auth.isLoggedIn.value) return;
  try {
    await playlists.refreshLists();
  } catch (e) {
    if (e.status === 401) auth.logout();
    else playlists.error.value = e.message;
  }
});
</script>

<template>
  <div v-if="!auth.isLoggedIn.value" class="login-wrap">
    <section class="login-card">
      <h1>Spotify <span>List</span></h1>
      <label>
        Usuario
        <input v-model="auth.username.value" autocomplete="username" @keyup.enter="handleLogin" />
      </label>
      <button :disabled="auth.loading.value" @click="handleLogin">Entrar</button>
      <p v-if="auth.error.value" class="error-banner" style="margin: 0">{{ auth.error.value }}</p>
    </section>
  </div>

  <div v-else class="app">
    <header class="topbar">
      <h1 class="logo">Spotify <span>List</span></h1>
      <button class="secondary" @click="auth.logout()">Salir</button>
    </header>

    <p v-if="playlists.error.value" class="error-banner">{{ playlists.error.value }}</p>

    <div class="workspace">
      <aside class="column">
        <div class="column-head">
          <h2>Listas</h2>
          <button class="ghost" :disabled="playlists.loading.value" @click="playlists.refreshLists()">↻</button>
        </div>
        <ul class="playlist-list">
          <li
            v-for="item in playlists.lists.value"
            :key="item.nombre"
            class="playlist-item"
            :class="{ active: item.nombre === playlists.selectedName.value }"
          >
            <button class="select" @click="playlists.selectList(item.nombre)">{{ item.nombre }}</button>
            <button class="danger-ghost" :disabled="playlists.loading.value" @click="playlists.remove(item.nombre)">✕</button>
          </li>
        </ul>
        <p v-if="!playlists.lists.value.length" class="empty">Sin listas</p>
      </aside>

      <main class="column detail">
        <template v-if="playlists.selected.value">
          <div class="detail-header">
            <h1>{{ playlists.selected.value.nombre }}</h1>
            <p v-if="playlists.selected.value.descripcion">{{ playlists.selected.value.descripcion }}</p>
          </div>
          <div class="meta-row">
            <span class="badge">{{ playlists.songCount.value }} canciones</span>
            <button :disabled="playlists.loading.value" @click="playlists.recommend()">Recomendar</button>
          </div>

          <div class="songs">
            <div
              v-for="(song, i) in playlists.selected.value.canciones || []"
              :key="i"
              class="song"
              :class="{ editing: playlists.editingIndex.value === i }"
            >
              <div class="song-main">
                <strong>{{ song.titulo || '—' }}</strong>
                <span class="sub">
                  {{ [song.artista, song.album, song.genero, song.anno].filter(Boolean).join(' · ') }}
                </span>
              </div>
              <button
                class="ghost song-edit"
                :disabled="playlists.loading.value"
                @click="playlists.startEditSong(i)"
              >
                Editar
              </button>
            </div>
            <p v-if="!playlists.songCount.value" class="empty">Lista vacía</p>
          </div>

          <div class="divider"></div>
          <p class="section-label">
            {{ playlists.isEditingSong.value ? 'Editar canción' : 'Agregar canción' }}
          </p>
          <div class="form-stack" style="padding: 0">
            <div class="fields-2">
              <label>Título <input v-model="playlists.songForm.titulo" /></label>
              <label>Artista <input v-model="playlists.songForm.artista" /></label>
              <label>Álbum <input v-model="playlists.songForm.album" /></label>
              <label>Año <input v-model="playlists.songForm.anno" /></label>
              <label>Género <input v-model="playlists.songForm.genero" /></label>
            </div>
            <div class="actions">
              <template v-if="playlists.isEditingSong.value">
                <button :disabled="playlists.loading.value" @click="playlists.saveEditedSong()">
                  Guardar cambios
                </button>
                <button
                  class="secondary"
                  :disabled="playlists.loading.value"
                  @click="playlists.cancelEditSong()"
                >
                  Cancelar
                </button>
              </template>
              <button
                v-else
                :disabled="playlists.loading.value"
                @click="playlists.addSongToSelected()"
              >
                Agregar a la lista
              </button>
            </div>
          </div>

          <template v-if="playlists.recommendations.value">
            <div class="divider"></div>
            <div class="meta-row">
              <h2 class="section-label" style="margin-right: auto">Recomendaciones</h2>
              <span
                class="badge"
                :class="playlists.recommendations.value.source === 'FALLBACK' ? 'fallback' : 'ai'"
              >
                {{ playlists.recommendations.value.source === 'FALLBACK' ? 'Fallback' : 'IA' }}
              </span>
            </div>
            <div class="songs">
              <div
                v-for="(rec, i) in playlists.recommendations.value.recommendations || []"
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
            <input v-model="playlists.form.nombre" />
          </label>
          <label>
            Descripción
            <textarea v-model="playlists.form.descripcion" rows="2" />
          </label>
          <div class="divider"></div>
          <p class="section-label">Canción</p>
          <div class="fields-2">
            <label>Título <input v-model="playlists.form.titulo" /></label>
            <label>Artista <input v-model="playlists.form.artista" /></label>
            <label>Álbum <input v-model="playlists.form.album" /></label>
            <label>Año <input v-model="playlists.form.anno" /></label>
            <label>Género <input v-model="playlists.form.genero" /></label>
          </div>
          <div class="actions">
            <button :disabled="playlists.loading.value" @click="playlists.create()">Crear</button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
