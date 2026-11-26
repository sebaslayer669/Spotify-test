import { computed, reactive, ref } from 'vue';
import {
  addSong,
  createList,
  deleteList,
  getList,
  getRecommendations,
  listLists
} from '../api';
import {
  mergeLists,
  removeCachedList,
  resolveList,
  upsertCachedList
} from '../listCache';

export function usePlaylists() {
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

  const songForm = reactive({
    titulo: '',
    artista: '',
    album: '',
    anno: '',
    genero: ''
  });

  const songCount = computed(() => selected.value?.canciones?.length || 0);

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
    songForm.titulo = '';
    songForm.artista = '';
    songForm.album = '';
    songForm.anno = '';
    songForm.genero = '';
  }

  function clearSelection() {
    lists.value = [];
    selectedName.value = '';
    selected.value = null;
    recommendations.value = null;
    error.value = '';
  }

  async function refreshLists() {
    const apiLists = await listLists();
    lists.value = mergeLists(apiLists);
    for (const item of lists.value) {
      upsertCachedList(item);
    }
  }

  async function selectList(name) {
    error.value = '';
    recommendations.value = null;
    selectedName.value = name;
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
      await deleteList(name);
      removeCachedList(name);
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

  async function addSongToSelected() {
    if (!selectedName.value) {
      error.value = 'Selecciona una lista primero';
      return;
    }
    error.value = '';
    if (!songForm.titulo.trim() && !songForm.artista.trim()) {
      error.value = 'Indica al menos título o artista';
      return;
    }
    loading.value = true;
    try {
      const updated = await addSong(selectedName.value, {
        titulo: songForm.titulo.trim(),
        artista: songForm.artista.trim(),
        album: songForm.album.trim(),
        anno: songForm.anno.trim(),
        genero: songForm.genero.trim()
      });
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
    clearSelection,
    refreshLists,
    selectList,
    create,
    remove,
    recommend,
    addSongToSelected
  };
}
