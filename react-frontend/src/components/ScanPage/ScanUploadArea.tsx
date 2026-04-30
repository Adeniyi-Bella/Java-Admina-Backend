
import { FileText, X } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import { type FileWithPages } from "@/utils/pdfUtils";
import { brandStyles } from "@/lib/design/styles";
import DocumentUpload from "./upload/DocumentUpload";

type Props = {
  file: FileWithPages | null;
  remainingDocuments?: number;
  clearSignal: boolean;
  onSetFile: (file: File | null) => void | Promise<void>;
  onClearFile: () => void;
};

export default function ScanUploadArea({
  file,
  remainingDocuments,
  clearSignal,
  onSetFile,
  onClearFile,
}: Props) {
  return (
    <div className="mb-4">
      {remainingDocuments !== undefined && (
        <DocumentUpload
          setFile={onSetFile}
          userMetadata={remainingDocuments}
          clearSignal={clearSignal}
        />
      )}

      {file && (
        <div className="mt-4">
        <div
            className={`flex items-center justify-between rounded-lg border p-3 ${
              (file.pageCount ?? 0) > 2
                ? `${brandStyles.dangerSoftBg} ${brandStyles.dangerSoft3Border}`
                : `${brandStyles.page} ${brandStyles.brandBorder}`
            }`}
          >
            <div className="flex items-center space-x-3">
              <FileText className={`h-5 w-5 ${brandStyles.brandText}`} />
              <div className="flex flex-col">
                <span className="text-sm font-medium">
                  {file.name || "Unknown File"}
                </span>
                <div className="flex items-center space-x-2 text-xs text-gray-500">
                  <span>
                    (
                    {typeof file.size === "number"
                      ? (file.size / 1024 / 1024).toFixed(1)
                      : "0"}{" "}
                    MB)
                  </span>
                  <span>•</span>
                  <span
                    className={`${
                      (file.pageCount ?? 0) > 2 ? "font-medium text-red-600" : ""
                    }`}
                  >
                    {file.pageCount ?? 1} page
                    {(file.pageCount ?? 1) !== 1 && "s"}
                  </span>
                </div>
              </div>
            </div>
            <Button
              className={`mt-2.5 ${brandStyles.brandButton}`}
              onClick={onClearFile}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
