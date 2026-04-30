import { AlertTriangle, ArrowLeft, RefreshCw } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { brandStyles } from "@/lib/design/styles";

type ServerErrorPageProps = {
  message?: string;
  returnUrl?: string;
};

export function ServerErrorPage({
  message,
  returnUrl = "/",
}: ServerErrorPageProps) {
  const handleRetry = () => {
    window.location.assign(returnUrl || "/");
  };

  const handleGoHome = () => {
    window.location.assign("/");
  };

  return (
    <div className="min-h-screen bg-(--page-bg) px-4 py-12">
      <div className="mx-auto flex w-full max-w-3xl items-center justify-center">
        <Card
          className={`${brandStyles.brandCard} w-full border-(--danger-soft) shadow-xl`}
        >
          <CardHeader className="space-y-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-(--danger-soft) text-(--danger)">
              <AlertTriangle className="h-7 w-7" />
            </div>
            <div>
              <CardTitle className="text-3xl font-bold text-(--neutral-darker)">
                The service is temporarily unavailable
              </CardTitle>
              <CardDescription className="mt-2 text-base">
                We could not reach the backend right now. Please try again in a
                moment.
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            {message ? (
              <div className="rounded-lg border border-(--danger-soft) bg-(--danger-soft)/50 p-4 text-sm text-(--neutral-darker)">
                <p className="font-medium text-(--danger-dark)">Details</p>
                <p className="mt-1">{message}</p>
              </div>
            ) : null}

            <div className="flex flex-col gap-3 sm:flex-row">
              <Button
                className={brandStyles.dangerButton}
                onClick={handleRetry}
              >
                <RefreshCw className="h-4 w-4" />
                Retry
              </Button>
              <Button
                variant="outline"
                className={brandStyles.brandOutlineButton}
                onClick={handleGoHome}
              >
                <ArrowLeft className="h-4 w-4" />
                Go home
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
