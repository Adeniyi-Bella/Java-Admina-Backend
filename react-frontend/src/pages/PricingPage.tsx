import { useState } from "react";
import { Check, Zap, Crown, ArrowRight } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Badge } from "@/components/common/Badge/Badge";
import { Switch } from "@/components/common/Switch/Switch";
import { Label } from "@/components/common/Label/Label";
import { useEffect } from "react";
import Header from "@/components/common/Header/Header";
import Footer from "@/components/common/Footer/Footer";
import { useMsalLoginLogoutSignup } from "@/hooks/auth/useMsalLoginLogoutSignup";
import { brandStyles } from "@/lib/design/styles";
import { Button } from "@/components/common/Button/Button";

export default function PricingPage() {
  const [isAnnual, setIsAnnual] = useState(false);

  const { signUp } = useMsalLoginLogoutSignup();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  const plans = [
    {
      name: "Free",
      description: "Perfect for getting started",
      price: { monthly: 0, annual: 0 },
      icon: Zap,
      colorClass: brandStyles.brandSoftBg,
      features: [
        "2 documents per month",
        "2 pages max per document",
        "Multi-language translation",
        "Easy translation from the camera of your phone",
        "5 chatbot questions per document",
        "Document processing not guaranteed during high demand",
        "Not suitable for complex structured documents",
        "2MB max file size per document",
        "Document summary, keypoints extraction, action plans, and translation included",
      ],
      limitations: ["Limited to 2 documents/month"],
      cta: "Get Started Free",
      ctaLink: "/register",
      popular: false,
    },
    {
      name: "Standard",
      // description: "For individuals and small teams",
      price: { monthly: 5.99, annual: 59.99 },
      icon: Crown,
      colorClass: brandStyles.brandBg,
      features: [
        "All Free plan features, plus:",
        "Up to 5 documents",
        "Up to five pages per document",
        "15 Chatbot questions per document",
        "Priority document processing",
        "Automated Email notifications (coming soon)",
        "Automated response generation per document (coming soon)",
        "Suitable for complex structured documents",
        "5MB max file size per document",
        "Maximum data security",
        "And more features coming soon!",
      ],
      limitations: [],
      cta: "Start Standard Trial",
      ctaLink: "/payment?plan=Standard",
    },
  ];

  return (
    <div className={`min-h-screen ${brandStyles.page}`}>
      {/* Header */}
      <Header text="Back To Home" backRoute="/" />

      <div className="container mx-auto px-4 py-16">
        <div className="max-w-6xl mx-auto">
          {/* Page Header */}
          <div className="text-center mb-16">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Choose Your Plan
            </h1>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto mb-8">
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

            {/* Billing Toggle */}
            <div className="flex items-center justify-center space-x-4 mb-8">
              <Label
                htmlFor="billing-toggle"
                className={isAnnual ? "text-gray-500" : "font-semibold"}
              >
                Monthly
              </Label>
              <Switch
                id="billing-toggle"
                checked={isAnnual}
                onCheckedChange={setIsAnnual}
                className={brandStyles.brandToggle}
              />
              <Label
                htmlFor="billing-toggle"
                className={isAnnual ? "font-semibold" : "text-gray-500"}
              >
                Annual
              </Label>
              <Badge className={`${brandStyles.successBg} ml-2 text-white`}>
                Save 17%
              </Badge>
            </div>
          </div>

          {/* Pricing Cards */}
          <div className="grid lg:grid-cols-2 gap-8 mb-16">
            {plans.map((plan, index) => (
              <Card
                key={index}
                className={`relative flex flex-col transition-all duration-300 hover:shadow-lg ${brandStyles.brandCard}`}
              >
                <CardHeader className="text-center pb-8">
                  <div className="flex justify-center mb-4">
                    <div
                      className={`h-16 w-16 rounded-full flex items-center justify-center ${plan.colorClass}`}
                    >
                      <plan.icon className="h-8 w-8 text-white" />
                    </div>
                  </div>
                  <CardTitle className="text-2xl mb-2">{plan.name}</CardTitle>
                  <CardDescription className="text-base">
                    {plan.description}
                  </CardDescription>
                  <div className="mt-4">
                    {typeof plan.price.monthly === "number" ? (
                      <>
                        <div
                          className={`text-4xl font-bold ${brandStyles.brandText}`}
                        >
                          €{isAnnual ? plan.price.annual : plan.price.monthly}
                        </div>
                        <p className="text-gray-600">
                          {plan.price.monthly === 0
                            ? "Forever free"
                            : isAnnual
                              ? "per year"
                              : "per month"}
                        </p>
                        {isAnnual && plan.price.monthly > 0 && (
                          <p className="text-sm text-gray-500">
                            €{((plan.price.annual as number) / 12).toFixed(2)}{" "}
                            per month
                          </p>
                        )}
                      </>
                    ) : (
                      <>
                        <div
                          className={`text-4xl font-bold ${brandStyles.brandText}`}
                        >
                          {plan.price.monthly}
                        </div>
                        <p className="text-gray-600">Contact for pricing</p>
                      </>
                    )}
                  </div>
                </CardHeader>

                <CardContent className="flex flex-col justify-between grow">
                  {/* Features & limitations */}
                  <div className="space-y-6">
                    <div>
                      <h4 className="font-semibold mb-3">What's included:</h4>
                      <ul className="space-y-3">
                        {plan.features.map((feature, featureIndex) => (
                          <li
                            key={featureIndex}
                            className="flex items-start space-x-3"
                          >
                            <Check
                              className={`h-5 w-5 mt-0.5 shrink-0 ${brandStyles.successText}`}
                            />
                            <span className="text-gray-600">{feature}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    {plan.limitations.length > 0 && (
                      <div>
                        <h4 className="font-semibold mb-3 text-gray-500">
                          Limitations:
                        </h4>
                        <ul className="space-y-2">
                          {plan.limitations.map((limitation, limitIndex) => (
                            <li
                              key={limitIndex}
                              className="text-sm text-gray-500"
                            >
                              • {limitation}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>

                  {/* Footer Button */}
                  {plan.name === "Free" && (
                    <div className="mt-8 flex justify-center">
                      <Button
                        size="lg"
                        variant="outline"
                        className={`text-xl px-12 py-6 rounded-full border-2 transition-all duration-500 bg-transparent transform hover:scale-110 hover:-translate-y-1 ${brandStyles.brandOutlineButton}`}
                        onClick={signUp}
                      >
                        Get Started
                        <ArrowRight className="ml-3 h-6 w-6" />
                      </Button>
                    </div>
                  )}

                  {plan.name === "Standard" && (
                    <div
                      className={`border-2 rounded-lg p-4 mt-6 italic text-sm ${brandStyles.brandStrongBorder}`}
                    >
                      The Standard version is not officially live yet. Contact
                      us directly at{" "}
                      <a
                        href="mailto:support@admina-app.com"
                        className="text-blue-600 underline"
                      >
                        support@admina-app.com
                      </a>{" "}
                      for the standard version.
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>

          {/* Feature Comparison */}
          <div className="mb-16">
            <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
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
                        <th className="text-left p-6 font-semibold">
                          Features
                        </th>
                        <th className="text-center p-6 font-semibold">Free</th>
                        <th
                          className={`text-center p-6 font-semibold ${brandStyles.brandText}`}
                        >
                          Standard
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {[
                        {
                          feature: "Documents per month",
                          free: "2",
                          Standard: "5",
                        },
                        {
                          feature: "Document types",
                          free: "pdf, jpg",
                          Standard: "pdf, jpg",
                        },
                        {
                          feature: "Multi-language translation",
                          free: "✓",
                          Standard: "✓",
                        },
                        {
                          feature: "Email notifications",
                          free: "✗",
                          Standard: "✓",
                        },
                        {
                          feature: "Chatbot support",
                          free: "✓",
                          Standard: "✓",
                        },
                      ].map((row, index) => (
                        <tr key={index} className="border-b border-gray-200">
                          <td className="p-6 font-medium">{row.feature}</td>
                          <td className="p-6 text-center text-gray-600">
                            {row.free}
                          </td>
                          <td
                            className={`p-6 text-center ${brandStyles.brandText}`}
                          >
                            {row.Standard}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}
