import { requireAuth } from '../../_lib/auth.js';
import { sendEmpty, sendJson } from '../../_lib/http.js';
import { getStore } from '../../_lib/store.js';

export default async function handler(req, res) {
  if (!requireAuth(req, res)) {
    return;
  }
  const listName = req.query.listName;
  const store = getStore();
  const lista = store.get(listName);

  if (req.method === 'GET') {
    if (!lista) {
      sendJson(res, 404, {
        status: 404,
        error: 'Not Found',
        message: `Lista no encontrada: ${listName}`
      });
      return;
    }
    sendJson(res, 200, lista);
    return;
  }

  if (req.method === 'DELETE') {
    if (!lista) {
      sendJson(res, 404, {
        status: 404,
        error: 'Not Found',
        message: `Lista no encontrada: ${listName}`
      });
      return;
    }
    store.delete(listName);
    sendEmpty(res, 204);
    return;
  }

  sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Método no permitido' });
}
