import { createFileRoute } from "@tanstack/react-router";
import { ServerErrorPage } from "@/pages/ServerErrorPage/ServerErrorPage";

export const Route = createFileRoute("/server-error")({
  validateSearch: (search: Record<string, unknown>) => ({
    message: typeof search.message === "string" ? search.message : undefined,
    returnUrl: typeof search.returnUrl === "string" ? search.returnUrl : "/",
  }),
  component: ServerErrorRouteComponent,
});

function ServerErrorRouteComponent() {
  const { message, returnUrl } = Route.useSearch();
  return <ServerErrorPage message={message} returnUrl={returnUrl} />;
}
