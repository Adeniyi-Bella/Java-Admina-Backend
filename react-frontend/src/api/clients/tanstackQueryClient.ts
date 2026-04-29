import { QueryClient, MutationCache, QueryCache } from "@tanstack/react-query";
import {
  ApiError,
  AppError,
  NetworkError,
  ReregistrationBlockedError,
  ServerDownError,
  UnauthorizedError,
} from "../error/customeError";
import { logger } from "@/lib/logger";

const AUTH_ERROR_EVENT = "admina:auth-error";
const SERVER_ERROR_EVENT = "admina:server-error";

const dispatchAuthError = () => {
  window.dispatchEvent(new CustomEvent(AUTH_ERROR_EVENT));
};

const dispatchServerError = (message: string, returnUrl: string) => {
  window.dispatchEvent(
    new CustomEvent(SERVER_ERROR_EVENT, {
      detail: { message, returnUrl },
    }),
  );
};

const handleQueryError = (error: Error) => {
  if (typeof window === "undefined") return;

  logger.error(error, "API Error");

  if (error instanceof UnauthorizedError) {
    dispatchAuthError();
    return;
  }

  if (window.location.pathname === "/server-error") return;

  if (
    error instanceof ReregistrationBlockedError ||
    error instanceof ServerDownError ||
    error instanceof NetworkError ||
    error instanceof ApiError
  ) {
    const appError = error as AppError;
    const message = appError.userMessage || "An unexpected error occurred";
    const returnUrl = window.location.pathname + window.location.search;

    dispatchServerError(message, returnUrl);
  }
};

const handleMutationError = (error: Error) => {
  if (error instanceof UnauthorizedError) {
    if (typeof window !== "undefined") {
      dispatchAuthError();
    }
    return;
  }
};

export const reactQueryEvents = {
  AUTH_ERROR_EVENT,
  SERVER_ERROR_EVENT,
};

export const tanstackQueryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (failureCount >= 2) return false;
        if (error instanceof AppError && error.retryable) return true;
        return false;
      },
      retryDelay: () => 4000 + Math.random() * 2000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
  queryCache: new QueryCache({
    onError: handleQueryError,
  }),
  mutationCache: new MutationCache({
    onError: handleMutationError,
  }),
});
