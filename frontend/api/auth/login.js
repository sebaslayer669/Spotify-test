import { readJson, sendJson } from '../_lib/http.js';
import { signToken } from '../_lib/jwt.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa POST' });
    return;
  }
  try {
    const body = await readJson(req);
    const username = (body.username || 'demo').toString();
    const accessToken = signToken(username);
    sendJson(res, 200, { accessToken, tokenType: 'Bearer' });
  } catch {
    sendJson(res, 400, { status: 400, error: 'Bad Request', message: 'JSON inválido' });
  }
}
