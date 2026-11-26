/** Estado en memoria del adaptador Vercel (demo). La API real está en api-lists/ (Spring). */
export function getStore() {
  if (!globalThis.__quipuxListsStore) {
    globalThis.__quipuxListsStore = new Map();
  }
  return globalThis.__quipuxListsStore;
}
