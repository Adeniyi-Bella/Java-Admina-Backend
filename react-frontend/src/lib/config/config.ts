import { z } from "zod";

const configSchema = z.object({
  VITE_API_URL: z.string().url(),
  VITE_AZURE_CLIENT_ID: z.string().min(1),
  VITE_AZURE_AUTHORITY: z.string().url(),
  VITE_AZURE_REDIRECT_URI: z.string().min(1),
  VITE_AZURE_LOGOUT_REDIRECT_URI: z.string().min(1),
  VITE_ADMINA_API_CLIENT_ID: z.string().min(1),
  VITE_SENTRY_DSN: z.string().min(1).optional(),
});

const rawConfig = {
  VITE_API_URL: import.meta.env.VITE_API_URL,
  VITE_AZURE_CLIENT_ID: import.meta.env.VITE_AZURE_CLIENT_ID,
  VITE_AZURE_AUTHORITY: import.meta.env.VITE_AZURE_AUTHORITY,
  VITE_AZURE_REDIRECT_URI: import.meta.env.VITE_AZURE_REDIRECT_URI,
  VITE_AZURE_LOGOUT_REDIRECT_URI: import.meta.env.VITE_AZURE_LOGOUT_REDIRECT_URI,
  VITE_ADMINA_API_CLIENT_ID: import.meta.env.VITE_ADMINA_API_CLIENT_ID,
  VITE_SENTRY_DSN: import.meta.env.VITE_SENTRY_DSN,
};

const parsedConfig = configSchema.safeParse(rawConfig);

if (!parsedConfig.success) {
  const issues = parsedConfig.error.issues
    .map((issue) => `${issue.path.join(".") || "config"}: ${issue.message}`)
    .join(", ");

  throw new Error(`Invalid configuration: ${issues}`);
}

export const config = parsedConfig.data;

export const isDevelopment = import.meta.env.MODE === "development";
export const isProduction = import.meta.env.MODE === "production";