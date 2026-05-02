import { useMutation } from "@tanstack/react-query";
import { BillingApi } from "@/api/billing.api";
import { type AppError, MsalNoAccountError } from "@/api/error/customeError";
import { useAuth } from "@/hooks/auth/useAuth";

type UseCreateCheckoutSessionOptions = {
  onError: (error: AppError) => void;
  onSuccess?: (sessionUrl: string) => void;
};

export function useCreateCheckoutSession({
  onError,
  onSuccess,
}: UseCreateCheckoutSessionOptions) {
  const { instance, account } = useAuth();

  const mutation = useMutation<string, AppError, string>({
    mutationFn: async (plan) => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      const idempotencyKey = crypto.randomUUID();

      const checkoutUrl = await BillingApi.createCheckoutSession(
        instance,
        account,
        { plan },
        idempotencyKey,
      );

      return checkoutUrl;
    },
    onSuccess,
    onError,
  });

  return {
    startCheckout: mutation.mutate,
    isStartingCheckout: mutation.isPending,
  };
}
