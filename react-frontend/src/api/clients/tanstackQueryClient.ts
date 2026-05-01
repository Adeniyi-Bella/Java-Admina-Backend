import { QueryClient, MutationCache, QueryCache } from "@tanstack/react-query";
import {
  AppError,
  NetworkError,
  ServerDownError,
  TimeoutError,
  UnauthorizedError,
} from "../error/customeError";
import { AUTH_SESSION_INVALIDATE_EVENT } from "@/types/constants";
import { SentryLogger } from "@lib/logger/sentry";

/**
 * Dispatches a custom DOM event to signal an authentication error.
 * Decoupled from the router so any listener (e.g. MSAL handler, auth context)
 * can respond without this module needing a direct reference to them.
 */
const dispatchAuthError = () => {
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_INVALIDATE_EVENT));
};

const captureQueryError = (error: Error, context: Record<string, unknown>) => {
  SentryLogger.captureException(error, context);
};

/**
 * Redirects the user to the server error page, passing a human-readable
 * message and the current URL so the user can be sent back after recovery.
 *
 * @param message  - User-facing error description shown on the error page
 * @param returnUrl - The URL to return to after the error is acknowledged
 */
const redirectToServerErrorPage = (message: string, returnUrl: string) => {
  const search = new URLSearchParams({
    message,
    returnUrl,
  });
  window.location.assign(`/server-error?${search.toString()}`);
};

/**
 * Central error handler for all React Query **queries** (useQuery, useSuspenseQuery).
 *
 * Called automatically by QueryCache whenever any query settles with an error.
 * Handles three categories:
 *
 * 1. UnauthorizedError (401) → dispatches auth-error event for MSAL to handle
 * 2. Already on /server-error → no redirect loop, bail out early
 * 3. Known AppErrors (network, server down, API) → redirect to
 *    the server error page with context so the user understands what happened
 *
 * @param error - The error thrown by the query function
 */
const handleQueryError = (error: Error) => {
  // SSR guard — window is not available during server-side rendering
  if (typeof window === "undefined") return;

  // 401 — token expired or invalid; trigger re-authentication
  if (error instanceof UnauthorizedError) {
    captureQueryError(error, {
      source: "query",
      type: "unauthorized",
      pathname: window.location.pathname,
      search: window.location.search,
    });
    dispatchAuthError();
    return;
  }

  // Prevent redirect loop if we're already on the error page
  if (window.location.pathname === "/server-error") return;

  const isServerFailure =
    error instanceof ServerDownError ||
    error instanceof NetworkError ||
    error instanceof TimeoutError ||
    (error instanceof AppError &&
      typeof error.statusCode === "number" &&
      error.statusCode >= 500);

  if (!isServerFailure) return;

  const appError = error as AppError;
  const message = appError.userMessage || "An unexpected error occurred";
  const returnUrl = window.location.pathname + window.location.search;

  captureQueryError(error, {
    source: "query",
    type: "server_error",
    code: appError.code,
    statusCode: appError.statusCode,
    pathname: window.location.pathname,
    search: window.location.search,
    returnUrl,
  });

  redirectToServerErrorPage(message, returnUrl);
};

/**
 * Central error handler for all React Query **mutations** (useMutation).
 *
 * Mutations are user-initiated actions (form submits, button clicks) so we
 * handle errors more selectively — only auth errors are handled globally here.
 * All other mutation errors should be handled locally in the calling component
 * (e.g. showing inline validation messages or toast notifications).
 *
 * @param error - The error thrown by the mutation function
 */

const handleMutationError = (error: Error) => {
  if (error instanceof UnauthorizedError) {
    if (typeof window !== "undefined") {
      captureQueryError(error, {
        source: "mutation",
        type: "unauthorized",
        pathname: window.location.pathname,
        search: window.location.search,
      });
      dispatchAuthError();
    }
    return;
  }
};

/**
 * The singleton TanStack Query client for the entire application.
 *
 * Configuration decisions:
 *
 * - `retry` — AppErrors expose a `retryable` flag set at the error class level.
 *   Only retryable errors (e.g. transient network blips) are retried, capped at
 *   3 attempts. Non-retryable errors (401, 403, validation) fail immediately.
 *   Unknown non-AppErrors are also capped at 3 attempts.
 *
 * - `retryDelay: 4–6s` — jitter (random 0–2s) spreads retry attempts to avoid
 *   thundering herd on the API when multiple queries fail simultaneously.
 *
 * - `refetchOnWindowFocus: false` — prevents silent background refetches when
 *   the user switches tabs, which could overwrite in-progress form state.
 *
 * - `mutations.retry: false` — mutations are user-initiated actions; retrying
 *   silently (e.g. a form submit) risks duplicate writes and confuses the user.
 *
 * - `QueryCache.onError` — global handler for all query errors (see above).
 * - `MutationCache.onError` — global handler for mutation auth errors only.
 */
export const tanstackQueryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof AppError) {
          // Respect the retryable flag defined on each AppError subclass
          return error.retryable && failureCount < 3;
        }
        // Unknown errors: cap at 3 attempts
        if (failureCount >= 3) return false;
        return false;
      },
      // Jittered 4–6s delay reduces API pressure during retry bursts
      retryDelay: () => 4000 + Math.random() * 2000,
      // No background refetch on tab focus — avoids disrupting active UI state
      refetchOnWindowFocus: false,
    },
    mutations: {
      // Never silently retry mutations — let components handle failure explicitly
      retry: false,
    },
  },
  // Routes all query errors through the centralized handleQueryError function
  queryCache: new QueryCache({
    onError: handleQueryError,
  }),
  // Routes mutation errors through handleMutationError (auth errors only)
  mutationCache: new MutationCache({
    onError: handleMutationError,
  }),
});
