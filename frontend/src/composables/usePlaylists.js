import { computed, reactive, ref } from 'vue';
import {
  addSong,
  createList,
  deleteList,
  getList,
  getRecommendations,
  listLists,
  updateSong
} from '../api';
import {
  mergeLists,
  removeCachedList,
  resolveList,
  upsertCachedList
} from '../listCache';

function blankSong() {
  return {
    titulo: '',
    artista: '',
    album: '',
    anno: '',
    genero: ''
  };
}

export function usePlaylists() {
  const lists = ref([]);
  const selectedName = ref('');
  const selected = ref(null);
  const recommendations = ref(null);
  const loading = ref(false);
  const error = ref('');
  const editingIndex = ref(null);

  const form = reactive({
    nombre: '',
    descripcion: '',
    titulo: '',
    artista: '',
    album: '',
    anno: '',
    genero: ''
  });

  const songForm = reactive(blankSong());

  const songCount = computed(() => selected.value?.canciones?.length || 0);
  const isEditingSong = computed(() => editingIndex.value !== null);

  function resetForm() {
    form.nombre = '';
    form.descripcion = '';
    form.titulo = '';
    form.artista = '';
    form.album = '';
    form.anno = '';
    form.genero = '';
  }

  function resetSongForm() {
    Object.assign(songForm, blankSong());
    editingIndex.value = null;
  }

  function clearSelection() {
    lists.value = [];
    selectedName.value = '';
    selected.value = null;
    recommendations.value = null;
    error.value = '';
    resetSongForm();
  }

  async function refreshLists() {
    const apiLists = await listLists();
    lists.value = mergeLists(apiLists);
    for (const item of lists.value) {
      upsertCachedList(item);
    }
    if (selectedName.value && !lists.value.some((l) => l.nombre === selectedName.value)) {
      selectedName.value = '';
      selected.value = null;
      recommendations.value = null;
      resetSongForm();
    }
  }

  async function selectList(name) {
    error.value = '';
    recommendations.value = null;
    selectedName.value = name;
    resetSongForm();
    loading.value = true;
    try {
      let apiList = null;
      try {
        apiList = await getList(name);
      } catch (e) {
        if (e.status !== 404) throw e;
      }
      const resolved = resolveList(apiList, name);
      if (!resolved) {
        error.value = `Lista no encontrada: ${name}`;
        selected.value = null;
        return;
      }
      selected.value = resolved;
      upsertCachedList(resolved);
    } catch (e) {
      error.value = e.message;
      selected.value = null;
    } finally {
      loading.value = false;
    }
  }

  async function create() {
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
      upsertCachedList(created);
      resetForm();
      await refreshLists();
      selectedName.value = created.nombre;
      selected.value = created;
      recommendations.value = null;
      resetSongForm();
    } catch (e) {
      error.value = e.message;
    } finally {
      loading.value = false;
    }
  }

  async function remove(name) {
    error.value = '';
    if (!confirm(`¿Eliminar “${name}”?`)) return;
    loading.value = true;
    try {
      try {
        await deleteList(name);
      } catch (e) {
        // 404: ya no está en el servidor (caché fantasma tras reinicio H2) → limpiar UI igual
        if (e.status !== 404) throw e;
      }
      removeCachedList(name);
      if (selectedName.value === name) {
        selectedName.value = '';
        selected.value = null;
        recommendations.value = null;
        resetSongForm();
      }
      error.value = '';
      await refreshLists();
    } catch (e) {
      error.value = e.message;
    } finally {
      loading.value = false;
    }
  }

  async function recommend() {
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

  function startEditSong(index) {
    const song = selected.value?.canciones?.[index];
    if (!song) return;
    editingIndex.value = index;
    Object.assign(songForm, {
      titulo: song.titulo || '',
      artista: song.artista || '',
      album: song.album || '',
      anno: song.anno || '',
      genero: song.genero || ''
    });
    error.value = '';
  }

  function cancelEditSong() {
    resetSongForm();
  }

  function songPayload() {
    return {
      titulo: songForm.titulo.trim(),
      artista: songForm.artista.trim(),
      album: songForm.album.trim(),
      anno: songForm.anno.trim(),
      genero: songForm.genero.trim()
    };
  }

  async function addSongToSelected() {
    if (!selectedName.value) {
      error.value = 'Selecciona una lista primero';
      return;
    }
    error.value = '';
    const payload = songPayload();
    if (!payload.titulo && !payload.artista) {
      error.value = 'Indica al menos título o artista';
      return;
    }
    loading.value = true;
    try {
      const updated = await addSong(selectedName.value, payload);
      upsertCachedList(updated);
      selected.value = updated;
      recommendations.value = null;
      resetSongForm();
      await refreshLists();
    } catch (e) {
      error.value = e.message;
    } finally {
      loading.value = false;
    }
  }

  async function saveEditedSong() {
    if (!selectedName.value || editingIndex.value === null) {
      error.value = 'Selecciona una canción para editar';
      return;
    }
    error.value = '';
    const payload = songPayload();
    if (!payload.titulo && !payload.artista) {
      error.value = 'Indica al menos título o artista';
      return;
    }
    loading.value = true;
    try {
      const updated = await updateSong(selectedName.value, editingIndex.value, payload);
      upsertCachedList(updated);
      selected.value = updated;
      recommendations.value = null;
      resetSongForm();
      await refreshLists();
    } catch (e) {
      error.value = e.message;
    } finally {
      loading.value = false;
    }
  }

  return {
    lists,
    selectedName,
    selected,
    recommendations,
    loading,
    error,
    form,
    songForm,
    songCount,
    editingIndex,
    isEditingSong,
    clearSelection,
    refreshLists,
    selectList,
    create,
    remove,
    recommend,
    startEditSong,
    cancelEditSong,
    addSongToSelected,
    saveEditedSong
  };
}
