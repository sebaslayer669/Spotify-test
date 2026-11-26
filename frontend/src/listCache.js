const KEY = 'spotify_lists_cache';

export function readListCache() {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

export function writeListCache(map) {
  sessionStorage.setItem(KEY, JSON.stringify(map || {}));
}

export function upsertCachedList(lista) {
  if (!lista?.nombre) return;
  const map = readListCache();
  map[lista.nombre] = lista;
  writeListCache(map);
}

export function removeCachedList(nombre) {
  const map = readListCache();
  delete map[nombre];
  writeListCache(map);
}

export function clearListCache() {
  sessionStorage.removeItem(KEY);
}

/** Une respuesta API + caché local (la caché gana si trae más canciones). */
export function mergeLists(apiLists) {
  const cache = readListCache();
  const byName = {};
  for (const item of apiLists || []) {
    byName[item.nombre] = item;
  }
  for (const [nombre, cached] of Object.entries(cache)) {
    const current = byName[nombre];
    const cachedSongs = cached?.canciones?.length || 0;
    const currentSongs = current?.canciones?.length || 0;
    if (!current || cachedSongs > currentSongs) {
      byName[nombre] = cached;
    }
  }
  return Object.values(byName);
}

export function resolveList(apiList, nombre) {
  const cached = readListCache()[nombre];
  if (!apiList) return cached || null;
  const apiSongs = apiList.canciones?.length || 0;
  const cachedSongs = cached?.canciones?.length || 0;
  if (cached && cachedSongs > apiSongs) {
    return cached;
  }
  return apiList;
}
