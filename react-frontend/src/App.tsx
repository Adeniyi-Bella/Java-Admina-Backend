import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "@tanstack/react-router";
import { ErrorBoundary } from "@components/common/ErrorBoundary/ErrorBoundary";
import { queryClient, router } from "./lib/router";
import { useAuth } from "./hooks/auth/useAuth";

const AdminaUI = () => {
  const auth = useAuth()
  return <RouterProvider router={router} context={{ auth }} />
}

export const App = () => {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AdminaUI />
      </QueryClientProvider>
    </ErrorBoundary>
  );
};