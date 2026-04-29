import { useIsAuthenticated, useMsal } from "@azure/msal-react";
import { InteractionStatus } from "@azure/msal-browser";

export const useAuth = () => {
  const { instance, inProgress } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const isLoading = inProgress !== InteractionStatus.None;
  const account = instance.getActiveAccount();

  return { isAuthenticated, isLoading, account, instance };
};

export type AuthContext = ReturnType<typeof useAuth>;