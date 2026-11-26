import { requireAuth } from '../../_lib/auth.js';
import { sendJson } from '../../_lib/http.js';
import { getStore } from '../../_lib/store.js';

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
  if (!requireAuth(req, res)) {
    return;
  }
  if (req.method !== 'GET') {
    sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa GET' });
    return;
  }

  const listName = req.query.listName;
  const lista = getStore().get(listName);
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
}
