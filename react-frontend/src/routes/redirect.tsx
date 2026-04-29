import { createFileRoute } from "@tanstack/react-router";
import { useEffect } from "react";
import { useMsal } from "@azure/msal-react";
import { useNavigate } from "@tanstack/react-router";
import { AuthLoader } from "@/components/common/Loader/Loader";

export const Route = createFileRoute("/redirect")({
  component: RedirectPage,
});

function RedirectPage() {
  const { instance } = useMsal();
  const navigate = useNavigate();

  useEffect(() => {
    instance
      .handleRedirectPromise()
      .then((result) => {
        if (result?.account) {
          instance.setActiveAccount(result.account);
        }
        navigate({ to: "/dashboard" });
      })
      .catch(() => {
        navigate({ to: "/" });
      });
  }, [instance, navigate]);

  return <AuthLoader />;
}
