import { useEffect, useRef } from "react";
import { useMsal } from "@azure/msal-react";
import { tanstackQueryClient } from "@/api/clients/tanstackQueryClient";
import { AUTH_SESSION_INVALIDATE_EVENT } from "@/types/constants";

export function AuthSessionInvalidationListener() {
  const { instance } = useMsal();
  const handledRef = useRef(false);

  useEffect(() => {
    const handleAuthSession = () => {
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

    window.addEventListener(AUTH_SESSION_INVALIDATE_EVENT, handleAuthSession);
    return () => {
      window.removeEventListener(AUTH_SESSION_INVALIDATE_EVENT, handleAuthSession);
    };
  }, [instance]);

  return null;
}
