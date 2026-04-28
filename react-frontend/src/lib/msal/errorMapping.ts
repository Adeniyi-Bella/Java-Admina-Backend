import { logger } from "@/lib/logger";
import type { BrowserAuthError } from "@azure/msal-browser";

export type ToastFn = (payload: {
  title?: string;
  description?: string;
  variant?: "success" | "destructive";
  duration?: number;
}) => void;

export function handleMsalError(error: BrowserAuthError, toast: ToastFn) {
  if (error?.errorMessage?.includes("AADSTS50057")) {
    logger.error(error, "Account is scheduled for deletion");

    toast({
      title: "Account Deletion Scheduled",
      description:
        "This account is scheduled for deletion. You cannot register or login for 30 days.",
      variant: "destructive",
      duration: 5000,
    });
    return;
  }

  logger.error(error);

  toast({
    variant: "destructive",
    description:
      "We apologise for the inconvenience. Please check your internet connection or contact us",
    duration: 5000,
  });
}
