import { memo } from "react";
import { useNavigate } from "@tanstack/react-router";
import { Button } from "@components/common/Button/Button";
import { brandStyles } from "@/lib/design/styles";

export const NotFound = memo(() => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className={`${brandStyles.brandText} text-center`}>
        {/* 404 Graphic */}
        <div className="mb-8">
          <h1 className="text-9xl font-bold text-primary-600">404</h1>
          <div className="text-6xl mt-4">🔍</div>
        </div>

        {/* Error Message */}
        <h2 className={`${brandStyles.brandText} text-3xl font-bold mb-4`}>
          Page Not Found
        </h2>
        <p className="text-gray-600 mb-8 max-w-md mx-auto">
          Sorry, we couldn't find the page you're looking for. The page might
          have been moved, deleted, or never existed.
        </p>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Button size="default" className={brandStyles.brandButton} onClick={() => navigate({ to: "/" })}>
            Go Home
          </Button>
          <Button variant="outline"  onClick={() => window.history.back()}>
            Go Back
          </Button>
        </div>
      </div>
    </div>
  );
});

NotFound.displayName = "NotFound";
