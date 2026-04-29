import { createRootRouteWithContext, Outlet } from "@tanstack/react-router";
import { Suspense } from "react";
import { NotFound } from "@/pages/NotFound /NotFound";
import { DashboardLoader } from "@/components/common/Loader/Loader";
import type { RouterContext } from "@/lib/router";

export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootComponent,
  notFoundComponent: NotFound,
});

function RootComponent() {
  return (
    <>
      <Suspense fallback={<DashboardLoader />}>
        <Outlet />
      </Suspense>
    </>
  );
}
