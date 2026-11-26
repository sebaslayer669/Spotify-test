import { requireAuth } from '../_lib/auth.js';
import { sendJson } from '../_lib/http.js';

export default async function handler(req, res) {
  if (!requireAuth(req, res)) {
    return;
  }
  if (req.method !== 'GET') {
    sendJson(res, 405, { status: 405, error: 'Method Not Allowed', message: 'Usa GET' });
    return;
  }
  sendJson(res, 200, { genres: [] });
}
