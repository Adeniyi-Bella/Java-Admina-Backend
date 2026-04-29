import { useMutation } from "@tanstack/react-query";
import { useMsal } from "@azure/msal-react";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import type { UseDeleteUserOptions } from "@/api/types";
import { UserApi } from "@/api/user.api";

export function useDeleteUserAccount({
  onError,
  onSuccess,
}: UseDeleteUserOptions) {
  const { instance } = useMsal();
  const account = instance.getActiveAccount();

  const mutation = useMutation<void, AppError, void>({
    mutationFn: async (): Promise<void> => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      await UserApi.deleteUser(instance, account);
    },
    onSuccess: () => {
      sessionStorage.clear();
      instance.clearCache();
      onSuccess?.();
    },
    onError: (error: Error) => {
      onError(error as AppError);
    },
  });

  return {
    deleteUser: mutation.mutate,
    deleteAsync: mutation.mutateAsync,
    isDeleting: mutation.isPending,
  };
}
