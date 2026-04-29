import "./styles/globals.css";
import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { SentryLogger } from "@lib/logger/sentry";
import { router } from "./lib/router";
import { PublicClientApplication } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalConfig } from "./lib/config/msalConfig";

const msalInstance = new PublicClientApplication(msalConfig);

SentryLogger.init(router);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <MsalProvider instance={msalInstance}>
      <App />
    </MsalProvider>
  </React.StrictMode>,
);
