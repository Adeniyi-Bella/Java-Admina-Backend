import { config } from "./config";

export const msalConfig = {
    auth: {
        clientId: config.NEXT_PUBLIC_AZURE_CLIENT_ID,
        authority: config.NEXT_PUBLIC_AZURE_AUTHORITY,
        redirectUri: config.NEXT_PUBLIC_AZURE_REDIRECT_URI,
        postLogoutRedirectUri: config.NEXT_PUBLIC_AZURE_LOGOUT_REDIRECT_URI,
        navigateToLoginRequestUrl: true,
      },
    cache: {
        cacheLocation: 'sessionStorage', // Configures cache location. "sessionStorage" is more secure, but "localStorage" gives you SSO between tabs.
        storeAuthStateInCookie: false, // Set this to "true" if you are having issues on IE11 or Edge
    },
    // system: {
    //     loggerOptions: {
    //         loggerCallback: (level: LogLevel, message: string, containsPii: boolean) => {
    //             if (containsPii) {
    //                 return;
    //             }
    //             switch (level) {
    //                 case LogLevel.Error:
    //                     console.error(message);
    //                     return;
    //                 case LogLevel.Info:
    //                     console.info(message);
    //                     return;
    //                 case LogLevel.Verbose:
    //                     console.debug(message);
    //                     return;
    //                 case LogLevel.Warning:
    //                     console.warn(message);
    //                     return;
    //                 default:
    //                     return;
    //             }
    //         },
    //     },
    // },
};


export const loginRequest = {
  scopes: [ `api://${config.NEXT_PUBLIC_ADMINA_API_CLIENT_ID}/api.authorize`],
};
