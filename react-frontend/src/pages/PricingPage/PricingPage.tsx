import { Check, ArrowRight } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import Header from "@/components/common/Header/Header";
import Footer from "@/components/common/Footer/Footer";
import { brandStyles } from "@/lib/design/styles";
import { Button } from "@/components/common/Button/Button";
import { useMsalLoginLogoutSignup } from "@/hooks/auth/useMsalLoginLogoutSignup";
import { useAuthenticateUser } from "@/hooks/api/user/useUser";
import { useAuth } from "@/hooks/auth/useAuth";
import { useCreateCheckoutSession } from "@/hooks/api/payment/useCreateCheckoutSession";
import { useToast } from "@/hooks/use-toast";
import { COMPARISON_ROWS, planRank, PLANS } from "./constants";
import { DashboardLoader } from "@/components/common/Loader/Loader";
import type { AppError } from "@/api/error/customeError";
import { usePricingURLActions } from "@/hooks/api/payment/usePricingURLActions";
import { useState } from "react";

/**
 * Builds the post-login redirect URL so an unauthenticated user who clicks
 * "Upgrade" is sent back here (with their chosen plan preserved) after signing up.
 */
const buildPricingRedirect = (plan: string, billing: string) =>
  `/pricing?pendingPlan=${encodeURIComponent(plan)}&billing=${encodeURIComponent(billing)}`;

export default function PricingPage() {
  const { toast } = useToast();
  const { isAuthenticated } = useAuth();
  const { data: userResult, isLoading: isUserLoading } = useAuthenticateUser();
  const { signUp, signUpWithRedirect } = useMsalLoginLogoutSignup();
  const [isPreparing, setIsPreparing] = useState(false);
  const [pendingPlanType, setPendingPlanType] = useState<string | null>(null);

  const currentPlan = userResult?.data.user.plan?.toLowerCase() ?? null;
  const currentPlanRank = currentPlan ? (planRank[currentPlan] ?? -1) : -1;

  const { startCheckout, isStartingCheckout } = useCreateCheckoutSession({
    onError: (error: AppError) => {
      setIsPreparing(false);
      toast({
        title: "Could not start checkout",
        description: error.userMessage ?? "Please try again.",
        variant: "destructive",
      });
    },
    onSuccess: (checkoutUrl) => {
      // Hand off to Stripe — full page navigation intentional.
      window.location.assign(checkoutUrl);
    },
  });

  const showLoader = isPreparing || isStartingCheckout;

  // Handles all URL-driven side effects:
  //   • Stripe return (success / canceled)
  //   • Resuming checkout after post-login redirect
  usePricingURLActions(startCheckout, pendingPlanType);

  // /**
  //  * Returns "Current Plan" when the user already holds this tier (or higher),
  //  * otherwise falls back to the plan's default CTA label.
  //  */
  // const getPlanActionLabel = (
  //   planType: string,
  //   fallbackLabel: string,
  // ): string => {
  //   if (!isAuthenticated) return fallbackLabel;
  //   const rank = planRank[planType] ?? -1;
  //   return currentPlanRank >= rank && rank >= 0
  //     ? "Current Plan"
  //     : fallbackLabel;
  // };

  /**
   * Determines whether a plan's CTA button should be disabled.
   *
   * Rules:
   *   • Always disabled while a checkout is in flight (avoids duplicate sessions).
   *   • Unauthenticated users can always click (they'll be sent to sign up).
   *   • Disabled while user data is loading for paid plans (plan rank is unknown).
   *   • Disabled when the user is already on this exact plan.
   */
  const isButtonDisabled = (planType: string): boolean => {
    if (isStartingCheckout) return true;
    if (!isAuthenticated) return false;
    if (isUserLoading && planType !== "free") return true;

    const rank = planRank[planType] ?? -1;
    return currentPlanRank >= rank && rank >= 0;
  };

  const handlePlanClick = (planType: string): void => {
    // new-user/free plan → Sign Up flow (no payment checkout needed)
    if (planType === "free") {
      signUp();
      return;
    }
    // new-user/paid plan →Sign up flow with automatic payment checkout
    if (!isAuthenticated) {
      signUpWithRedirect(buildPricingRedirect(planType, "monthly"));
      return;
    }

    if (isUserLoading) return;

    // used to prevent upgrading/downgrading during a monthly billing cycle until the next cycle starts. This is to avoid confusion around proration charges and billing dates.
    const targetRank = planRank[planType] ?? -1;
    if (currentPlanRank >= targetRank) return;
    setIsPreparing(true);
    setPendingPlanType(planType.toUpperCase());
    startCheckout(planType.toUpperCase());
  };

  return (
    <div className={`min-h-screen ${brandStyles.page}`}>
      {/* Full-screen overlay while Stripe session is being created */}
      {showLoader && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-white/70 backdrop-blur-sm">
          <DashboardLoader text="Redirecting to payment platform" />
        </div>
      )}

      <Header text="Back To Home" backRoute="/" />

      <div className="container mx-auto px-4 py-16">
        <div className="mx-auto max-w-6xl">
          {/* Page header */}
          <div className="mb-16 text-center">
            <h1 className="mb-4 text-4xl font-bold text-gray-900">
              Choose Your Plan
            </h1>
            <p className="mx-auto mb-8 max-w-2xl text-xl text-gray-600">
              Start with our free plan. The Standard version is not officially
              live yet. Contact us directly at{" "}
              <a
                href="mailto:support@admina-app.com"
                className="text-blue-600 underline"
              >
                support@admina-app.com
              </a>{" "}
              for the standard version.
            </p>
          </div>

          {/* Pricing cards */}
          <div className="mb-16 grid gap-8 lg:grid-cols-2">
            {PLANS.map((plan) => {
              const isFree = plan.price === 0;
              const actionLabel = plan.cta;

              return (
                <Card
                  key={plan.name}
                  className={`relative flex flex-col transition-all duration-300 hover:shadow-lg ${brandStyles.brandCard}`}
                >
                  <CardHeader className="pb-8 text-center">
                    <div className="mb-4 flex justify-center">
                      <div
                        className={`flex h-16 w-16 items-center justify-center rounded-full ${plan.colorClass}`}
                      >
                        <plan.icon className="h-8 w-8 text-white" />
                      </div>
                    </div>

                    <CardTitle className="mb-2 text-2xl">{plan.name}</CardTitle>
                    <CardDescription className="text-base">
                      {plan.description}
                    </CardDescription>

                    <div className="mt-4">
                      <div
                        className={`text-4xl font-bold ${brandStyles.brandText}`}
                      >
                        €{plan.price}
                      </div>
                      <p className="text-gray-600">
                        {isFree ? "Forever free" : "per month"}
                      </p>
                    </div>
                  </CardHeader>

                  <CardContent className="grow">
                    <div className="space-y-6">
                      {/* Included features */}
                      <div>
                        <h4 className="mb-3 font-semibold">What's included:</h4>
                        <ul className="space-y-3">
                          {plan.features.map((feature) => (
                            <li
                              key={feature}
                              className="flex items-start space-x-3"
                            >
                              <Check
                                className={`mt-0.5 h-5 w-5 shrink-0 ${brandStyles.successText}`}
                              />
                              <span className="text-gray-600">{feature}</span>
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* Limitations — omitted when empty */}
                      {plan.limitations.length > 0 && (
                        <div>
                          <h4 className="mb-3 font-semibold text-gray-500">
                            Limitations:
                          </h4>
                          <ul className="space-y-2">
                            {plan.limitations.map((limitation) => (
                              <li
                                key={limitation}
                                className="text-sm text-gray-500"
                              >
                                • {limitation}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>

                    <div className="mt-8 flex justify-center">
                      <Button
                        size="lg"
                        variant="outline"
                        className={`rounded-full border-2 bg-transparent px-12 py-6 text-xl transition-all duration-500 transform hover:-translate-y-1 hover:scale-110 ${brandStyles.brandOutlineButton}`}
                        onClick={() => handlePlanClick(plan.type)}
                        disabled={isButtonDisabled(plan.type)}
                      >
                        {actionLabel}
                        <ArrowRight className="ml-3 h-6 w-6" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>

          {/* Feature comparison table */}
          <div className="mb-16">
            <h2 className="mb-12 text-center text-3xl font-bold text-gray-900">
              Feature Comparison
            </h2>
            <Card className={brandStyles.brandCard}>
              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr
                        className={`border-b ${brandStyles.brandStrongBorder}`}
                      >
                        <th className="p-6 text-left font-semibold">
                          Features
                        </th>
                        <th className="p-6 text-center font-semibold">Free</th>
                        <th
                          className={`p-6 text-center font-semibold ${brandStyles.brandText}`}
                        >
                          Standard
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {COMPARISON_ROWS.map((row, index) => (
                        <tr
                          key={row.feature}
                          className={
                            index < COMPARISON_ROWS.length - 1
                              ? `border-b ${brandStyles.brandStrongBorder}`
                              : ""
                          }
                        >
                          <td className="p-6">{row.feature}</td>
                          <td className="p-6 text-center">{row.free}</td>
                          <td
                            className={`p-6 text-center ${brandStyles.brandText}`}
                          >
                            {row.standard}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Support CTA */}
          <div className="text-center">
            <div className={`rounded-xl p-8 ${brandStyles.brandCard}`}>
              <h3 className="mb-4 text-2xl font-bold text-gray-900">
                Need More Help?
              </h3>
              <p className="mx-auto mb-6 max-w-2xl text-gray-600">
                Contact our support team for custom plans, enterprise features,
                or any questions about our pricing.
              </p>
              <Button className={brandStyles.brandButton} asChild>
                <a href="mailto:support@admina-app.com">Contact Support</a>
              </Button>
            </div>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}
