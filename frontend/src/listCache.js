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

/**
 * Une respuesta API + caché local.
 * La API es la fuente de verdad de qué listas existen;
 * la caché solo puede enriquecer canciones de listas que sí están en la API
 * (p. ej. en Vercel/multi-lambda con pérdida parcial de canciones).
 */
export function mergeLists(apiLists) {
  const cache = readListCache();
  const byName = {};
  const alive = new Set();

  for (const item of apiLists || []) {
    if (!item?.nombre) continue;
    alive.add(item.nombre);
    const cached = cache[item.nombre];
    const cachedSongs = cached?.canciones?.length || 0;
    const currentSongs = item?.canciones?.length || 0;
    byName[item.nombre] = cached && cachedSongs > currentSongs ? cached : item;
  }

  // Purga fantasmas: listas solo en caché (p. ej. tras reiniciar H2)
  const pruned = {};
  for (const nombre of alive) {
    if (byName[nombre]) pruned[nombre] = byName[nombre];
  }
  writeListCache(pruned);

  return Object.values(byName);
}

export function resolveList(apiList, nombre) {
  const cached = readListCache()[nombre];
  if (!apiList) {
    // No inventar lista fantasma si el servidor ya no la tiene
    if (cached) removeCachedList(nombre);
    return null;
  }
  const apiSongs = apiList.canciones?.length || 0;
  const cachedSongs = cached?.canciones?.length || 0;
  if (cached && cachedSongs > apiSongs) {
    return cached;
  }
  return apiList;
}
