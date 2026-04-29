import { useEffect, useRef } from "react";
import { useMsal } from "@azure/msal-react";
import { tanstackQueryClient } from "@/api/clients/tanstackQueryClient";
import { AUTH_ERROR_EVENT } from "@/types/constants";

export function AuthErrorListener() {
  const { instance } = useMsal();
  const handledRef = useRef(false);

  useEffect(() => {
    const handleAuthError = () => {
      if (handledRef.current) return;
      handledRef.current = true;

      tanstackQueryClient.clear();

      instance
        .logoutRedirect({
          postLogoutRedirectUri: "/",
        })
        .catch(() => {
          window.location.assign("/");
        });
    };

    window.addEventListener(AUTH_ERROR_EVENT, handleAuthError);
    return () => {
      window.removeEventListener(AUTH_ERROR_EVENT, handleAuthError);
    };
  }, [instance]);

  return null;
}
