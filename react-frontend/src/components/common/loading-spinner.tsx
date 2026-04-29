interface LoadingSpinnerProps {
  message?: string;
  size?: "sm" | "md" | "lg";
}

export function LoadingSpinner({
  message = "Loading...",
  size = "md",
}: LoadingSpinnerProps) {
  const containerSizeClasses = {
    sm: "w-24 h-24",
    md: "w-32 h-32",
    lg: "w-40 h-40",
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-amber-50 dark:bg-amber-950">
      <div className={`relative ${containerSizeClasses[size]} mb-6`}>
        {/* Outer rotating ring */}
        <div className="absolute inset-0 border-4 border-amber-200 dark:border-amber-800 rounded-full animate-spin border-t-amber-600 dark:border-t-amber-400"></div>

        {/* Inner pulsing circle */}
        {/* <div className="absolute inset-2 bg-amber-100 dark:bg-amber-900/50 rounded-full animate-pulse flex items-center justify-center"> */}
        {/* Cooking utensils icon */}
        {/* <UtensilsCrossed className={`${sizeClasses[size]} text-amber-600 dark:text-amber-400 animate-bounce`} />
        </div> */}

        {/* Floating dots animation */}
        <div className="absolute -top-2 left-1/2 transform -translate-x-1/2">
          <div className="flex space-x-1">
            <div
              className="w-2 h-2 bg-amber-500 rounded-full animate-bounce"
              style={{ animationDelay: "0ms" }}
            ></div>
            <div
              className="w-2 h-2 bg-amber-500 rounded-full animate-bounce"
              style={{ animationDelay: "150ms" }}
            ></div>
            <div
              className="w-2 h-2 bg-amber-500 rounded-full animate-bounce"
              style={{ animationDelay: "300ms" }}
            ></div>
          </div>
        </div>
      </div>

      {/* Loading message */}
      <div className="text-center">
        <h2 className="text-xl font-semibold text-amber-900 dark:text-amber-100 mb-2">
          {message}
        </h2>
        <p className="text-amber-700 dark:text-amber-300 animate-pulse">
          Preparing your Data...
        </p>
      </div>

      {/* Additional cooking-themed animation */}
      <div className="mt-8 flex space-x-4">
        <div
          className="w-3 h-3 bg-orange-400 rounded-full animate-ping"
          style={{ animationDelay: "0ms" }}
        ></div>
        <div
          className="w-3 h-3 bg-amber-400 rounded-full animate-ping"
          style={{ animationDelay: "200ms" }}
        ></div>
        <div
          className="w-3 h-3 bg-yellow-400 rounded-full animate-ping"
          style={{ animationDelay: "400ms" }}
        ></div>
        <div
          className="w-3 h-3 bg-orange-400 rounded-full animate-ping"
          style={{ animationDelay: "600ms" }}
        ></div>
      </div>
    </div>
  );
}
