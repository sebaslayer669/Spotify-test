import { requireAuth } from '../_lib/auth.js';
import { readJson, sendJson } from '../_lib/http.js';
import { getStore } from '../_lib/store.js';

export default async function handler(req, res) {
  if (!requireAuth(req, res)) {
    return;
  }
  const store = getStore();

  if (req.method === 'GET') {
    sendJson(res, 200, Array.from(store.values()));
    return;
  }

  if (req.method === 'POST') {
    try {
      const body = await readJson(req);
      const nombre = body?.nombre;
      if (nombre == null || String(nombre).trim() === '') {
        sendJson(res, 400, {
          status: 400,
          error: 'Bad Request',
          message: 'El nombre de la lista no es válido'
        });
        return;
      }
      const key = String(nombre).trim();
      if (store.has(key)) {
        sendJson(res, 400, {
          status: 400,
          error: 'Bad Request',
          message: 'Ya existe una lista con ese nombre'
        });
        return;
      }
      const lista = {
        nombre: key,
        descripcion: body.descripcion || '',
        canciones: Array.isArray(body.canciones) ? body.canciones : []
      };
      store.set(key, lista);
      res.setHeader('Location', `/lists/${encodeURIComponent(key)}`);
      sendJson(res, 201, lista);
    } catch {
      sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
    }
    return;
  }

  sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Método no permitido' });
}
