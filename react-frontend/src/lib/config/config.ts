function requireEnv(key: string, validator?: (v: string) => boolean): string {
  const value = import.meta.env[key];
  if (!value || value.trim() === "") {
    throw new Error(`Missing env var: ${key}`);
  }
  if (validator && !validator(value)) {
    throw new Error(`Invalid env var: ${key}`);
  }
  return value;
}

export const config = {
  VITE_API_URL: requireEnv("VITE_API_URL", v => v.startsWith("http")),
  VITE_AZURE_CLIENT_ID: requireEnv("VITE_AZURE_CLIENT_ID"),
  VITE_AZURE_AUTHORITY: requireEnv("VITE_AZURE_AUTHORITY", v => v.startsWith("http")),
  VITE_AZURE_REDIRECT_URI: requireEnv("VITE_AZURE_REDIRECT_URI"),
  VITE_AZURE_LOGOUT_REDIRECT_URI: requireEnv("VITE_AZURE_LOGOUT_REDIRECT_URI"),
  VITE_ADMINA_API_CLIENT_ID: requireEnv("VITE_ADMINA_API_CLIENT_ID"),
  VITE_SENTRY_DSN: import.meta.env.VITE_SENTRY_DSN,
  VITE_ENV: requireEnv("VITE_ENV", v => ["development", "production"].includes(v)),
} as const;

export const isDevelopment = config.VITE_ENV === "development";
export const isProduction = config.VITE_ENV === "production";