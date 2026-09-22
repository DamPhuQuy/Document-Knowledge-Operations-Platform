export interface CookieOptions {
  days?: number;
  path?: string;
  sameSite?: 'Strict' | 'Lax' | 'None';
  secure?: boolean;
}

export function setCookie(name: string, value: string, options: CookieOptions = {}): void {
  const {
    days = 7,
    path = '/',
    sameSite = 'Strict',
    secure = typeof window !== 'undefined' && window.location.protocol === 'https:',
  } = options;

  let expires = '';
  if (days) {
    const date = new Date();
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000);
    expires = `; expires=${date.toUTCString()}`;
  }

  const secureFlag = secure ? '; Secure' : '';
  const sameSiteFlag = `; SameSite=${sameSite}`;
  const pathFlag = `; Path=${path}`;

  document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}${expires}${pathFlag}${sameSiteFlag}${secureFlag}`;
}

export function getCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }

  const nameEQ = `${encodeURIComponent(name)}=`;
  const ca = document.cookie.split(';');

  for (let i = 0; i < ca.length; i += 1) {
    let c = ca[i];
    if (!c) continue;
    while (c.charAt(0) === ' ') {
      c = c.substring(1, c.length);
    }
    if (c.indexOf(nameEQ) === 0) {
      return decodeURIComponent(c.substring(nameEQ.length, c.length));
    }
  }

  return null;
}

export function deleteCookie(name: string, path = '/'): void {
  document.cookie = `${encodeURIComponent(name)}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=${path}; SameSite=Strict`;
}
