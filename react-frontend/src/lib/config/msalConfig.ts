import { config } from "./config";

export const msalConfig = {
  auth: {
    clientId: config.VITE_AZURE_CLIENT_ID,
    authority: config.VITE_AZURE_AUTHORITY,
    redirectUri: config.VITE_AZURE_REDIRECT_URI,
    postLogoutRedirectUri: config.VITE_AZURE_LOGOUT_REDIRECT_URI,
    navigateToLoginRequestUrl: true,
  },
  cache: {
    cacheLocation: "sessionStorage", // Configures cache location. "sessionStorage" is more secure, but "localStorage" gives you SSO between tabs.
    storeAuthStateInCookie: false, // Set this to "true" if you are having issues on IE11 or Edge
  },
};

export const loginRequest = {
  scopes: [`api://${config.VITE_ADMINA_API_CLIENT_ID}/api.authorize`],
};
