import { useMutation } from "@tanstack/react-query";
import { BillingApi } from "@/api/billing.api";
import { type AppError, MsalNoAccountError } from "@/api/error/customeError";
import { useAuth } from "@/hooks/auth/useAuth";

type UseCreatePortalSessionOptions = {
  onError: (error: AppError) => void;
  onSuccess?: (portalUrl: string) => void;
};

export function useCreatePortalSession({
  onError,
  onSuccess,
}: UseCreatePortalSessionOptions) {
  const { instance, account } = useAuth();

  const mutation = useMutation<string, AppError, void>({
    mutationFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      return BillingApi.createPortalSession(instance, account);
    },
    onSuccess,
    onError,
  });

  return {
    startPortalSession: mutation.mutate,
    isStartingPortalSession: mutation.isPending,
  };
}
