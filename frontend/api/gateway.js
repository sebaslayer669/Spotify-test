import { getBearer, readJson, sendEmpty, sendJson } from './_lib/http.js';
import { signToken, verifyToken } from './_lib/jwt.js';
import { getStore, persistStore } from './_lib/store.js';

function requireAuth(req, res) {
  const claims = verifyToken(getBearer(req));
  if (!claims) {
    sendJson(res, 401, {
      status: 401,
      error: 'Unauthorized',
      message: 'JWT requerido'
    });
    return null;
  }
  return claims;
}

function suggestionsFor(genero) {
  const g = (genero || 'pop').toLowerCase();
  if (['rock', 'grunge', 'metal'].includes(g)) {
    return [
      { titulo: 'Everlong', artista: 'Foo Fighters', genero: g, motivo: 'Misma energía de guitarra' },
      { titulo: 'Black Hole Sun', artista: 'Soundgarden', genero: g, motivo: 'Atmósfera cercana a tu lista' },
      { titulo: 'Karma Police', artista: 'Radiohead', genero: 'rock', motivo: 'Melodía y textura compatibles' }
    ];
  }
  if (g === 'pop') {
    return [
      { titulo: 'Blinding Lights', artista: 'The Weeknd', genero: 'pop', motivo: 'Ritmo y producción actuales' },
      { titulo: 'Levitating', artista: 'Dua Lipa', genero: 'pop', motivo: 'Melodía pegajosa afín' },
      { titulo: 'As It Was', artista: 'Harry Styles', genero: 'pop', motivo: 'Pop contemporáneo' }
    ];
  }
  return [
    { titulo: 'Dreams', artista: 'Fleetwood Mac', genero: g, motivo: 'Melodía accesible cercana a tu lista' },
    { titulo: 'Mr. Brightside', artista: 'The Killers', genero: g, motivo: 'Ritmo y gancho compatibles' },
    { titulo: 'Yellow', artista: 'Coldplay', genero: g, motivo: 'Textura y atmósfera similares' }
  ];
}

export default async function handler(req, res) {
  const op = req.query.op;

  if (op === 'login') {
    if (req.method !== 'POST') {
      sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa POST' });
      return;
    }
    try {
      const body = await readJson(req);
      const username = (body.username || 'demo').toString();
      sendJson(res, 200, { accessToken: signToken(username), tokenType: 'Bearer' });
    } catch {
      sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
    }
    return;
  }

  if (!requireAuth(req, res)) {
    return;
  }

  const store = getStore();

  if (op === 'lists') {
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
        persistStore();
        res.setHeader('Location', `/lists/${encodeURIComponent(key)}`);
        sendJson(res, 201, lista);
      } catch {
        sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
      }
      return;
    }
    sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Método no permitido' });
    return;
  }

  if (op === 'list') {
    const listName = req.query.listName;
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
    if (req.method === 'PUT') {
      if (!lista) {
        sendJson(res, 404, {
          status: 404,
          error: 'Not Found',
          message: `Lista no encontrada: ${listName}`
        });
        return;
      }
      try {
        const body = await readJson(req);
        const newNombre = (body?.nombre != null ? String(body.nombre) : listName).trim();
        if (!newNombre) {
          sendJson(res, 400, {
            status: 400,
            error: 'Bad Request',
            message: 'El nombre de la lista no es válido'
          });
          return;
        }
        const newDescripcion =
          body?.descripcion != null ? String(body.descripcion) : lista.descripcion || '';
        if (newNombre !== listName) {
          if (store.has(newNombre)) {
            sendJson(res, 400, {
              status: 400,
              error: 'Bad Request',
              message: 'Ya existe una lista con ese nombre'
            });
            return;
          }
          const renamed = {
            nombre: newNombre,
            descripcion: newDescripcion,
            canciones: Array.isArray(lista.canciones) ? [...lista.canciones] : []
          };
          store.delete(listName);
          store.set(newNombre, renamed);
          persistStore();
          sendJson(res, 200, renamed);
          return;
        }
        const updated = { ...lista, descripcion: newDescripcion };
        store.set(listName, updated);
        persistStore();
        sendJson(res, 200, updated);
      } catch {
        sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
      }
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
      persistStore();
      sendEmpty(res, 204);
      return;
    }
    sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Método no permitido' });
    return;
  }

  if (op === 'updateCancion') {
    const listName = req.query.listName;
    const index = Number.parseInt(String(req.query.index ?? ''), 10);
    const lista = store.get(listName);
    if (!lista) {
      sendJson(res, 404, {
        status: 404,
        error: 'Not Found',
        message: `Lista no encontrada: ${listName}`
      });
      return;
    }
    if (!Number.isInteger(index) || index < 0) {
      sendJson(res, 400, {
        status: 400,
        error: 'Bad Request',
        message: 'Índice de canción inválido'
      });
      return;
    }
    const canciones = Array.isArray(lista.canciones) ? [...lista.canciones] : [];
    if (index >= canciones.length) {
      sendJson(res, 404, {
        status: 404,
        error: 'Not Found',
        message: 'Canción no encontrada en la lista'
      });
      return;
    }

    if (req.method === 'DELETE') {
      canciones.splice(index, 1);
      const updated = { ...lista, canciones };
      store.set(listName, updated);
      persistStore();
      sendJson(res, 200, updated);
      return;
    }

    if (req.method !== 'PUT') {
      sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa PUT o DELETE' });
      return;
    }
    try {
      const body = await readJson(req);
      const empty =
        !(body?.titulo || '').toString().trim() &&
        !(body?.artista || '').toString().trim() &&
        !(body?.album || '').toString().trim() &&
        !(body?.anno || '').toString().trim() &&
        !(body?.genero || '').toString().trim();
      if (empty) {
        sendJson(res, 400, {
          status: 400,
          error: 'Bad Request',
          message: 'Indica al menos título o artista'
        });
        return;
      }
      canciones[index] = {
        titulo: (body.titulo || '').toString(),
        artista: (body.artista || '').toString(),
        album: (body.album || '').toString(),
        anno: (body.anno || '').toString(),
        genero: (body.genero || '').toString()
      };
      const updated = { ...lista, canciones };
      store.set(listName, updated);
      persistStore();
      sendJson(res, 200, updated);
    } catch {
      sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
    }
    return;
  }

  if (op === 'addCancion') {
    if (req.method !== 'POST') {
      sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa POST' });
      return;
    }
    const listName = req.query.listName;
    const lista = store.get(listName);
    if (!lista) {
      sendJson(res, 404, {
        status: 404,
        error: 'Not Found',
        message: `Lista no encontrada: ${listName}`
      });
      return;
    }
    try {
      const body = await readJson(req);
      const empty =
        !(body?.titulo || '').toString().trim() &&
        !(body?.artista || '').toString().trim() &&
        !(body?.album || '').toString().trim() &&
        !(body?.anno || '').toString().trim() &&
        !(body?.genero || '').toString().trim();
      if (empty) {
        sendJson(res, 400, {
          status: 400,
          error: 'Bad Request',
          message: 'Indica al menos título o artista'
        });
        return;
      }
      const song = {
        titulo: (body.titulo || '').toString(),
        artista: (body.artista || '').toString(),
        album: (body.album || '').toString(),
        anno: (body.anno || '').toString(),
        genero: (body.genero || '').toString()
      };
      const canciones = Array.isArray(lista.canciones) ? [...lista.canciones, song] : [song];
      const updated = { ...lista, canciones };
      store.set(listName, updated);
      persistStore();
      sendJson(res, 200, updated);
    } catch {
      sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
    }
    return;
  }

  if (op === 'recommendations') {
    if (req.method !== 'GET') {
      sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa GET' });
      return;
    }
    const listName = req.query.listName;
    const lista = store.get(listName);
    if (!lista) {
      sendJson(res, 404, {
        status: 404,
        error: 'Not Found',
        message: `Lista no encontrada: ${listName}`
      });
      return;
    }
    const genero = (lista.canciones || [])
      .map((c) => c.genero)
      .find((g) => g && String(g).trim());
    sendJson(res, 200, {
      listName,
      source: 'AI',
      message: 'Sugerencias listas',
      recommendations: suggestionsFor(genero)
    });
    return;
  }

  if (op === 'genres') {
    sendJson(res, 200, { genres: [] });
    return;
  }

  sendJson(res, 404, { status: 404, error: 'Not Found', message: 'Ruta no encontrada' });
}
