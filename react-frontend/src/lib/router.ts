import { createRouter } from "@tanstack/react-router";
import { QueryClient } from "@tanstack/react-query";
import { routeTree } from "../routeTree.gen";
import type { AuthContext } from "@/hooks/auth/useAuth";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      gcTime: 10 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export type RouterContext = {
  queryClient: QueryClient;
  auth: AuthContext;
};

export const router = createRouter({
  routeTree,
  context: {
    queryClient,
    auth: undefined, // This will be set in the RouterProvider context in App.tsx
  } as unknown as RouterContext,
  defaultPreload: "intent",
  defaultPreloadStaleTime: 0,
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}
