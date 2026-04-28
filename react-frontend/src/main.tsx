import './styles/globals.css';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { SentryLogger } from '@lib/logger/sentry';
import { router } from './lib/router';

SentryLogger.init(router);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);