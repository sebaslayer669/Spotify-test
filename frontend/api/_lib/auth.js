import { getBearer, sendJson } from './http.js';
import { verifyToken } from './jwt.js';

export function requireAuth(req, res) {
  const token = getBearer(req);
  const claims = verifyToken(token);
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
