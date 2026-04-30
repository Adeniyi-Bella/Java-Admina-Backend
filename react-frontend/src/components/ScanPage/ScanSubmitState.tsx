import { Button } from "@/components/common/Button/Button";
import { brandStyles } from "@/lib/design/styles";

type Props = {
  submitLabel: string;
  isSubmitDisabled: boolean;
  onSubmit: () => void;
};

export default function ScanSubmitState({
  submitLabel,
  isSubmitDisabled,
  onSubmit,
}: Props) {
  return (
    <Button
      onClick={onSubmit}
      disabled={isSubmitDisabled}
      className={`mt-4 w-full ${
        isSubmitDisabled ? "bg-gray-300 text-gray-500 cursor-not-allowed" : brandStyles.brandButton
      }`}
      size="lg"
    >
      {submitLabel}
    </Button>
  );
}
