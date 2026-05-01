import { useAuth } from "@/hooks/auth/useAuth";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import { UserApi } from "@/api/user.api";
import { queryKey } from "@/types/constants";
import type { UserWithDocumentsResponseDto } from "@/api/dto/responseDto";
import type { UseDeleteUserOptions } from "@/api/types";

/**
 * Authenticates the current MSAL user against the backend and returns
 * their full profile in the db including documents and plan limits.
 *
 * Cached indefinitely (staleTime: Infinity) since user data is stable
 * and only needs to be refreshed on explicit invalidation (e.g. after
 * a document is processed or the account is updated).
 *
 * Only runs when the user is authenticated and an MSAL account is present.
 */
export const useAuthenticateUser = () => {
  const { instance, account, isAuthenticated } = useAuth();

  // accountId is used as part of the query key so each user gets their own
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  return useQuery<{ data: UserWithDocumentsResponseDto }, AppError>({
    // Unique query key per user to cache their profile data separately
    queryKey: queryKey.authenticateUser(accountId),
    // The query function calls the UserApi to authenticate and fetch the user's profile
    queryFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }
      return UserApi.authenticate(instance, account);
    },
    // Only enable this query if the user is authenticated and we have an account
    enabled: isAuthenticated && !!account,
    // Cache the user data indefinitely since it doesn't change often and can be manually invalidated when needed
    staleTime: Infinity,
    // Keep in cache for 1 hour after unmounting to allow for quick remounts without refetching
    gcTime: 1000 * 60 * 60,
  });
};
export const useGetUser = useAuthenticateUser;

/**
 * Permanently deletes the current user's account from the backend,
 * then clears all local MSAL session data so the user is fully logged out.
 *
 * Accepts optional onSuccess and onError callbacks so the calling component
 * can handle navigation or show toast notifications after the operation.
 */
export function useDeleteUserAccount({
  onError,
  onSuccess,
}: UseDeleteUserOptions) {
  // Get the MSAL instance and current account from the auth context
  const { instance, account } = useAuth();
  // Set up a mutation using React Query to handle the delete user operation
  // the mutation accepts no variables (void) and returns void on success, but can throw an AppError on failure
  const mutation = useMutation<void, AppError, void>({
    mutationFn: async (): Promise<void> => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      await UserApi.deleteUser(instance, account);
    },
    onSuccess: () => {
      // Clear session storage and MSAL cache so no stale tokens or user
      // data remain in the browser after the account is deleted
      sessionStorage.clear();
      instance.clearCache();
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError(error as AppError);
    },
  });

  return {
    // Trigger deletion — fire and forget (no need to await)
    deleteUser: mutation.mutate,
    // Trigger deletion and await the result — useful if the caller
    // needs to chain logic after the mutation completes
    deleteAsync: mutation.mutateAsync,
    isDeleting: mutation.isPending,
  };
}
