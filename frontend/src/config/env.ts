/**
 * Application Environment Configuration
 * 
 * Centralized, type-safe configuration module that parses and validates
 * Vite environment variables (`import.meta.env`) with safe defaults.
 */

export type AppEnvironment = 'development' | 'production' | 'staging' | 'test';

export interface AppConfig {
  readonly apiBaseUrl: string;
  readonly appTitle: string;
  readonly appEnv: AppEnvironment;
  readonly appVersion: string;
  readonly enableDebug: boolean;
  readonly isDev: boolean;
  readonly isProd: boolean;
  readonly mode: string;
}

const rawEnv = import.meta.env;

const rawAppEnv = rawEnv.VITE_APP_ENV || (rawEnv.PROD ? 'production' : 'development');
const appEnv: AppEnvironment = (['development', 'production', 'staging', 'test'].includes(rawAppEnv)
  ? rawAppEnv
  : 'development') as AppEnvironment;

const isDev = rawEnv.DEV || appEnv === 'development';
const isProd = rawEnv.PROD || appEnv === 'production';

const defaultApiUrl = isProd ? '/api/v1' : 'http://localhost:8080/api/v1';

export const config: AppConfig = Object.freeze({
  apiBaseUrl: rawEnv.VITE_API_BASE_URL?.trim() || defaultApiUrl,
  appTitle: rawEnv.VITE_APP_TITLE?.trim() || 'Document & Knowledge Operations Platform',
  appEnv,
  appVersion: rawEnv.VITE_APP_VERSION?.trim() || '1.0.0',
  enableDebug: rawEnv.VITE_ENABLE_DEBUG === 'true' || (isDev && rawEnv.VITE_ENABLE_DEBUG !== 'false'),
  isDev,
  isProd,
  mode: rawEnv.MODE || (isProd ? 'production' : 'development'),
});

/**
 * Environment-aware logger that suppresses debug output in production
 * unless explicitly enabled via VITE_ENABLE_DEBUG=true.
 */
export const logger = {
  debug: (...args: unknown[]): void => {
    if (config.enableDebug) {
      console.debug('[DEBUG]', ...args);
    }
  },
  info: (...args: unknown[]): void => {
    console.info('[INFO]', ...args);
  },
  warn: (...args: unknown[]): void => {
    console.warn('[WARN]', ...args);
  },
  error: (...args: unknown[]): void => {
    console.error('[ERROR]', ...args);
  },
};

// Initial boot debug log
logger.debug(`Loaded environment config in [${config.mode}] mode:`, {
  apiBaseUrl: config.apiBaseUrl,
  appEnv: config.appEnv,
  isProd: config.isProd,
});

export default config;
