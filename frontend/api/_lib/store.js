import fs from 'fs';
import path from 'path';
import os from 'os';

const FILE = path.join(os.tmpdir(), 'spotify-lists-store.json');

function readFileStore() {
  try {
    if (!fs.existsSync(FILE)) {
      return {};
    }
    return JSON.parse(fs.readFileSync(FILE, 'utf8'));
  } catch {
    return {};
  }
}

function writeFileStore(data) {
  try {
    fs.writeFileSync(FILE, JSON.stringify(data), 'utf8');
  } catch {
    // ignore — demo adapter
  }
}

/** Persistencia en /tmp para que POST y GET compartan datos en el adaptador Vercel. */
export function getStore() {
  if (!globalThis.__spotifyListsStore) {
    globalThis.__spotifyListsStore = new Map(Object.entries(readFileStore()));
  }
  return globalThis.__spotifyListsStore;
}

export function persistStore() {
  const store = getStore();
  const obj = Object.fromEntries(store.entries());
  writeFileStore(obj);
}
