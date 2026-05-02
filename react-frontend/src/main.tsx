import "./styles/globals.css";
import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { SentryLogger } from "@lib/logger/sentry";
import { router } from "./lib/router";
import { EventType, PublicClientApplication } from "@azure/msal-browser";
import type { AuthenticationResult } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalConfig } from "./lib/config/msalConfig";
import { Toaster } from "./components/common/toaster";

const msalInstance = new PublicClientApplication(msalConfig);

(async () => {
  await msalInstance.initialize();

  // Set active account from cached session (returning users)
  const cachedAccounts = msalInstance.getAllAccounts();
  if (cachedAccounts.length > 0) {
    msalInstance.setActiveAccount(cachedAccounts[0]);
  }

  // Set active account on fresh login
  msalInstance.addEventCallback((event) => {
    if (event.eventType === EventType.LOGIN_SUCCESS && event.payload) {
      const account = (event.payload as AuthenticationResult).account;
      msalInstance.setActiveAccount(account);
    }
  });

  SentryLogger.init(router);

  ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
      <MsalProvider instance={msalInstance}>
        <App />
      </MsalProvider>
      <Toaster />
    </React.StrictMode>,
  );
})();