export interface PlanLimits {
  current: number;
  min: number;
}

/**
 * Checks if user has exceeded their document processing limit
 */
export const isOverDocumentLimit = (
  planName: string | undefined,
  limits: PlanLimits | undefined
): boolean => {
  if (!limits) return false;
  if (planName === "premium" || planName === "standard") return false;
  return limits.current <= limits.min;
};

/**
 * Validates document based on user plan.
 * Currently only enforces limits for 'free' users.
 */
export const validateDocument = (
  file: { pageCount?: number; size: number } | null,
  planName: string | undefined
): { isValid: boolean; reason: "pages" | "size" | null } => {
  if (!file) return { isValid: true, reason: null };
  
  // Only apply restrictions to free users
  if (planName === "free") {
    // 1. Check Page Count (Limit: 2)
    if ((file.pageCount ?? 0) > 2) {
      return { isValid: false, reason: "pages" };
    }
    
    // 2. Check File Size (Limit: 2MB)
    if (file.size > 2 * 1024 * 1024) {
      return { isValid: false, reason: "size" };
    }
  }

  return { isValid: true, reason: null };
};