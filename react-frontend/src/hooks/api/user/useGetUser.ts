import { useAuth } from "@/hooks/auth/useAuth";
import { useQuery } from "@tanstack/react-query";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import { UserApi } from "@/api/user.api";
import { queryKey } from "@/types/constants";
import type { UserWithDocumentsResponseDto } from "@/api/dto/responseDto";

export const useAuthenticateUser = () => {
  const { instance, account, isAuthenticated } = useAuth();
  const accountId = account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  return useQuery<{ data: UserWithDocumentsResponseDto }, AppError>({
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
