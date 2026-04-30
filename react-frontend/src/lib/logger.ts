import * as Sentry from "@sentry/react";
import { isProduction } from "./config/config";

type LogExtra = Record<string, unknown>;

export const logger = {
  // In Dev: We bind directly to the console. This preserves the original line number.
  // In Prod: We use a custom function to send data to Sentry.

  log: !isProduction
    ? (message: string, extra?: LogExtra) => {
        console.log(
          "%c[LOG]",
          "color: var(--teal); font-weight: bold;",
          message,
          extra,
        );
      }
    : (message: string, extra?: LogExtra) => {
        Sentry.captureMessage(message, { level: "info", extra });
      },

  warn: !isProduction
    ? console.warn.bind(
        console,
        "%c[WARN]",
        "color: var(--warning); font-weight: bold;",
      )
    : (message: string, extra?: LogExtra) => {
        Sentry.captureMessage(message, { level: "warning", extra });
      },

  error: (error: unknown, context?: string, extra?: LogExtra) => {
    if (!isProduction) {
      // We use a group so the developer sees the context clearly
      console.group(
        `%c[ERROR] Context: ${context || "General"}`,
        "color: white; background: var(--danger); padding: 2px 4px;",
      );

      // By logging the error object directly, you can expand it
      // in the console to see the full stack trace and the exact file/line.
      console.error(error);

      if (extra) console.table(extra);
      console.groupEnd();
      // return;
    }

    Sentry.captureException(error, {
      extra: {
        ...extra,
      },
    });

    Sentry.flush(2000);
  },
};
