import { useEffect } from "react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/hooks/auth/useAuth";
import { useGetUser } from "@/hooks/api/user/useUser";
import { queryKey } from "@/types/constants";
import { planRank } from "@/pages/PricingPage/constants";

/**
 * Reads and acts on URL parameters injected by two external flows:
 *
 *  1. Stripe return  — ?session_id=…&upgraded=true  or  ?canceled=true
 *     Stripe redirects back here after the user completes (or abandons) checkout.
 *     We show a toast, invalidate the user cache, then navigate to /dashboard on success.
 *
 *  2. Post-login resume — ?pendingPlan=standard&billing=monthly
 *     When an unauthenticated user clicks "Upgrade", we redirect them to sign up and
 *     append these params so we can resume checkout automatically once they return.
 *
 * Design notes
 * ─────────────
 * • We use TanStack Router's `useSearch` instead of `window.location.search` so the
 *   router's internal state stays in sync.
 * • We clear params with `navigate` (not `window.history.replaceState`) for the same reason.
 * • Removing the param from the URL before the effect finishes means React will never
 *   see the same params twice — no `useRef` guard needed.
 */
export function usePricingURLActions(
  startCheckout: (plan: string) => void,
  pendingPlanType: string | null,
) {
  const navigate = useNavigate();
  const search = useSearch({ strict: false }) as Record<
    string,
    string | undefined
  >;
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const { isAuthenticated, account } = useAuth();
  const { data: userResult, isLoading: isUserLoading } = useGetUser();

  const currentPlan = userResult?.data.user.plan?.toLowerCase() ?? null;
  const currentPlanRank = currentPlan ? (planRank[currentPlan] ?? -1) : -1;

  // Prefer the most stable identifier available on the MSAL account object.
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  // 1. Handle Stripe checkout return
  useEffect(() => {
    //Destructures the URL search params — session_id, upgraded, and canceled are the params Stripe adds when redirecting back.
    const { session_id: sessionId, upgraded, canceled } = search;

    // If none of the Stripe params are present — normal page visit, do nothing and exit early.
    if (!sessionId && upgraded !== "true" && canceled !== "true") return;

    // Clears the Stripe params from the URL immediately — so if the user refreshes, the effect doesn't re-run. replace: true means it doesn't add a new browser history entry.
    navigate({ to: "/pricing", replace: true });

    if (canceled === "true") {
      toast({
        title: "Checkout canceled",
        description: "You can upgrade anytime from the pricing page.",
      });
      return;
    }

    // Invalidate cached user data so the new plan is reflected in the UI right away,
    // then redirect to the dashboard.
    const handleSuccessfulUpgrade = async () => {
      if (accountId) {
        await queryClient.invalidateQueries({
          queryKey: queryKey.authenticateUser(accountId),
        });
      }

      navigate({
        to: "/dashboard",
        search: { toast: "upgraded", plan: pendingPlanType ?? "" },
        replace: true,
      });
    };

    void handleSuccessfulUpgrade();
  }, [search, accountId, navigate, queryClient, toast, pendingPlanType]);

  // 2. Resume checkout after post-login redirect
  useEffect(() => {
    const pendingPlan = search.pendingPlan?.toLowerCase();

    // Nothing pending — bail early.
    if (!pendingPlan) return;

    // Wait until authentication and user data are both ready.
    if (!isAuthenticated || isUserLoading || !currentPlan) return;

    const targetRank = planRank[pendingPlan];

    // Unknown plan key or user already on this plan (or higher) — just clean up.
    const shouldSkipCheckout =
      targetRank === undefined || currentPlanRank >= targetRank;

    // Navigate to /pricing with no search string to clear all pending plan params.
    navigate({ to: "/pricing", replace: true });

    if (!shouldSkipCheckout) {
      startCheckout(pendingPlan);
    }
  }, [
    search,
    isAuthenticated,
    isUserLoading,
    currentPlan,
    currentPlanRank,
    navigate,
    startCheckout,
  ]);
}
