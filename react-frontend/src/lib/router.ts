import { createRouter } from "@tanstack/react-router";
import { QueryClient } from "@tanstack/react-query";
import { routeTree } from "../routeTree.gen";
import type { AuthContext } from "@/hooks/auth/useAuth";
import { tanstackQueryClient } from "@/api/clients/tanstackQueryClient";

export type RouterContext = {
  queryClient: QueryClient;
  auth: AuthContext;
};

export const router = createRouter({
  routeTree,
  context: {
    tanstackQueryClient,
    auth: undefined,
  } as unknown as RouterContext,
  defaultPreload: "intent",
  defaultPreloadStaleTime: 0,
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}
