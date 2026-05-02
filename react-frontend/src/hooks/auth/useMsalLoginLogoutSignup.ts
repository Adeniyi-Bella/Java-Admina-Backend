import { useMsal } from "@azure/msal-react";
import { loginRequest } from "@/lib/config/msalConfig";
import { useToast } from "../use-toast";
import { InteractionStatus } from "@azure/msal-browser";
import { handleMsalError } from "@/lib/msal/errorMapping";

export const useMsalLoginLogoutSignup = () => {
  const { instance, inProgress } = useMsal();
  const { toast } = useToast();

  const login = () => {
    instance
      .loginRedirect({
        ...loginRequest,
        prompt: "select_account",
        redirectStartPage: "/dashboard",
      })
      .catch((error) => {
        handleMsalError(error, toast);
      });
  };

  const loginWithRedirect = (redirectStartPage: string) => {
    instance
      .loginRedirect({
        ...loginRequest,
        prompt: "select_account",
        redirectStartPage,
      })
      .catch((error) => {
        handleMsalError(error, toast);
      });
  };

  const signUp = () => {
    instance
      .loginRedirect({
        ...loginRequest,
        prompt: "create",
        redirectStartPage: "/dashboard",
      })
      .catch((error) => {
        handleMsalError(error, toast);
      });
  };

  const signUpWithRedirect = (redirectStartPage: string) => {
    instance
      .loginRedirect({
        ...loginRequest,
        prompt: "create",
        redirectStartPage,
      })
      .catch((error) => {
        handleMsalError(error, toast);
      });
  };

  const logout = async () => {
    if (inProgress !== InteractionStatus.None) return;

    instance
      .logoutRedirect({
        postLogoutRedirectUri: "/",
      })
      .catch((error) => {
        handleMsalError(error, toast);
      });
  };

  return { login, loginWithRedirect, signUp, signUpWithRedirect, logout };
};
