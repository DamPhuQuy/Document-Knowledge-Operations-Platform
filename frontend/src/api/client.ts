import { config } from '@/config';
import { getCookie } from '@/utils/cookie';
import type { ErrorResponse } from '@/types';

export class ApiError extends Error {
  readonly status: number;
  readonly errors?: Record<string, string>;

  constructor(message: string, status: number, errors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errors = errors;
  }
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  token?: string | null;
}

export async function apiClient<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { body, token, headers: customHeaders, ...customOptions } = options;

  const url = endpoint.startsWith('http')
    ? endpoint
    : `${config.apiBaseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const headers = new Headers(customHeaders);

  // Authenticate via token if passed or read from cookie or localStorage
  const activeToken = token ?? getCookie('access_token') ?? localStorage.getItem('access_token');
  if (activeToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${activeToken}`);
  }

  let finalBody: BodyInit | undefined;

  if (body instanceof FormData) {
    // FormData: let browser set Content-Type with multipart boundary
    finalBody = body;
  } else if (body !== undefined && body !== null) {
    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }
    finalBody = JSON.stringify(body);
  }

  const response = await fetch(url, {
    ...customOptions,
    headers,
    body: finalBody,
  });

  if (!response.ok) {
    let errorMessage = `HTTP error ${response.status}: ${response.statusText}`;
    let fieldErrors: Record<string, string> | undefined;

    try {
      const errorJson = (await response.json()) as ErrorResponse;
      if (errorJson.message) {
        errorMessage = errorJson.message;
      }
      if (errorJson.errors) {
        fieldErrors = errorJson.errors;
      }
    } catch {
      // Body was not JSON or empty
    }

    throw new ApiError(errorMessage, response.status, fieldErrors);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return undefined as unknown as T;
  }

  return (await response.json()) as T;
}
