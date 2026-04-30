

import { AlertTriangle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/common/alert";
import { brandStyles } from "@/lib/design/styles";

type Props = {
  isOverLimit: boolean;
  showValidationAlert: boolean;
  validationMessage: string;
};

export default function ScanValidationState({
  isOverLimit,
  showValidationAlert,
  validationMessage,
}: Props) {
  return (
    <>
      {isOverLimit && (
        <Alert className={`mb-6 ${brandStyles.dangerSoftBg} ${brandStyles.dangerText}`}>
          <AlertTriangle className={`h-4 w-4 ${brandStyles.dangerText}`} />
          <AlertDescription className={brandStyles.dangerText}>
            <strong>Monthly limit reached!</strong> You&apos;ve processed the
            maximum documents allowed.
          </AlertDescription>
        </Alert>
      )}

      {showValidationAlert && (
        <Alert className={`mb-4 mt-6 ${brandStyles.dangerSoftBg} ${brandStyles.dangerText}`}>
          <AlertTriangle className={`h-4 w-4 ${brandStyles.dangerText}`} />
          <AlertDescription className={brandStyles.dangerText}>
            {validationMessage}
          </AlertDescription>
        </Alert>
      )}
    </>
  );
}
