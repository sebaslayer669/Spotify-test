import crypto from 'crypto';

const DEFAULT_SECRET = 'change-me-local-dev-only-not-for-prod';

function secretBytes() {
  const secret = process.env.JWT_SECRET || DEFAULT_SECRET;
  const buf = Buffer.from(secret, 'utf8');
  if (buf.length >= 32) {
    return buf;
  }
  const padded = Buffer.alloc(32);
  buf.copy(padded);
  return padded;
}

function b64url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function b64urlJson(obj) {
  return b64url(JSON.stringify(obj));
}

export function signToken(username) {
  const header = b64urlJson({ alg: 'HS256', typ: 'JWT' });
  const now = Math.floor(Date.now() / 1000);
  const expMs = Number(process.env.JWT_EXPIRATION_MS || 3600000);
  const payload = b64urlJson({
    sub: username || 'demo',
    iat: now,
    exp: now + Math.floor(expMs / 1000)
  });
  const data = `${header}.${payload}`;
  const sig = crypto.createHmac('sha256', secretBytes()).update(data).digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `${data}.${sig}`;
}

export function verifyToken(token) {
  if (!token || typeof token !== 'string') {
    return null;
  }
  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }
  const [header, payload, signature] = parts;
  const data = `${header}.${payload}`;
  const expected = crypto.createHmac('sha256', secretBytes()).update(data).digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
    return null;
  }
  try {
    const json = JSON.parse(Buffer.from(payload, 'base64').toString('utf8'));
    if (!json.exp || json.exp * 1000 < Date.now()) {
      return null;
    }
    return json;
  } catch {
    return null;
  }
}
