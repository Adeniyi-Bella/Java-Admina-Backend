import { useIsAuthenticated, useMsal } from "@azure/msal-react";
import { useQuery } from "@tanstack/react-query";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import { UserApi } from "@/api/user.api";
import { queryKey } from "@/types/constants";
import type { AuthenticateUserResult } from "@/api/types/authentication.types";

export const useAuthenticateUser = () => {
  const { instance } = useMsal();
  const account = instance.getActiveAccount();
  const isAuthenticated = useIsAuthenticated();
  const accountId = account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  return useQuery<AuthenticateUserResult, AppError>({
    queryKey: queryKey.authenticateUser(accountId),
    queryFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }
      return UserApi.authenticate(instance, account);
    },
    enabled: isAuthenticated && !!account,
    staleTime: Infinity,
    gcTime: 1000 * 60 * 60,
  });
};

export const useGetUser = useAuthenticateUser;
