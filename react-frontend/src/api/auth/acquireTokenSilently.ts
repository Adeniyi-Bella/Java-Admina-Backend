import {
  type IPublicClientApplication,
  type AccountInfo,
} from "@azure/msal-browser";
import { UnauthorizedError } from "@/api/error/customeError";
import { loginRequest } from "@/lib/config/msalConfig";
import { logger } from "@/lib/logger";

export const acquireTokenSilently = async (
  instance: IPublicClientApplication,
  account: AccountInfo,
): Promise<{ idToken: string; accessToken: string }> => {
  if (!account) throw new Error("No active account");

  try {
    const response = await instance.acquireTokenSilent({
      ...loginRequest,
      account,
      forceRefresh: true,
    });

    const { idToken, accessToken } = response;
    logger.log(accessToken);
    return { idToken, accessToken };
  } catch (error: unknown) {
    logger.error(error, "MSAL_SILENT_TOKEN_ACQUISITION", {
      message: "Silent token acquisition failed",
    });

    // THROW YOUR CUSTOM ERROR:
    // This allows TanStack Query's global handler to see it and run window.location.href = "/"
    throw new UnauthorizedError(
      "Your session has expired. Please log in again.",
      "MSAL_AUTH_FAILURE",
      401,
    );
  }
};
