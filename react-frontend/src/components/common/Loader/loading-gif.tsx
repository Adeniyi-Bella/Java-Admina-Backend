;

import { LOADING_TEXT } from "./loading-copy";
import { brandStyles } from "@/lib/design/styles";

interface UnifiedLoadingProps {
  text?: string;
}

export default function UnifiedLoading({
  text = LOADING_TEXT.generic,
}: UnifiedLoadingProps) {
  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div
        className={`rounded-lg border-2 border-gray-200 dark:border-gray-700 shadow-2xl w-80 h-80 flex flex-col items-center justify-center space-y-6 ${brandStyles.surface}`}
      >
        {/* Company Logo */}
        <div className="flex items-center justify-center">
          <span className={`text-3xl font-bold ${brandStyles.brandText}`}>
            Admina
          </span>
        </div>

        {/* Loading Animation */}
        <div className="relative">
          <div
            className={`w-16 h-16 border-4 rounded-full animate-spin ${brandStyles.brandStrongBorder}`}
          >
            <div
              className={`absolute top-1 left-1 w-3 h-3 rounded-full ${brandStyles.brandBg}`}
            ></div>
          </div>
        </div>

        {/* Loading Text */}
        <div className="text-center px-6">
          <p className="text-lg font-medium text-gray-900 dark:text-white">
            {text}
          </p>
        </div>
      </div>
    </div>
  );
}
