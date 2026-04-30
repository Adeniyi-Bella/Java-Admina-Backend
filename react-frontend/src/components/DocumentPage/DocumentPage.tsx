import { ArrowLeft, AlertTriangle } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Badge } from "@/components/common/badge";
import { Alert, AlertTitle } from "@/components/common/alert";
import { brandStyles } from "@/lib/design/styles";
import DocumentTextSection from "./DocumentTextSection";
import type { GetDocumentResponseDto } from "@/api/dto/responseDto";
import ActionItemsSection from "./tasks/ActionItemsSection";

type Props = {
  documentData: GetDocumentResponseDto;
  onBack: () => void;
};

export default function DocumentPageView({ documentData, onBack }: Props) {
  const documentTitle = documentData.title ?? "Untitled document";
  const documentSender = documentData.sender ?? "Unknown sender";

  const summaryParagraphs: string[] = [];
  const sentences =
    documentData.summary
      ?.split(/\. /)
      .map((sentence) =>
        sentence.endsWith(".") ? sentence.trim() : `${sentence.trim()}.`,
      )
      .filter(Boolean) ?? [];

  for (let i = 0; i < sentences.length; i += 3) {
    summaryParagraphs.push(sentences.slice(i, i + 3).join(" "));
  }

  return (
    <div className="mb-30 space-y-6 bg-white">
      <Alert className="border-amber-200 bg-amber-50">
        <AlertTriangle className="h-4 w-4 text-amber-600" />
        <AlertTitle className="text-amber-800">
          Please verify all information carefully. Admina can make mistakes.
        </AlertTitle>
      </Alert>

      <Button
        variant="outline"
        onClick={onBack}
        className={`${brandStyles.brandOutlineButton} hover:bg-opacity-10`}
      >
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back
      </Button>

      <div>
        <div className="mb-4 flex flex-col gap-4">
          <p className="sm:mt-0 sm:text-3xl font-bold text-gray-900">
            {documentTitle}
          </p>
          <div className="flex items-center space-x-4">
            <Badge variant="secondary" className={brandStyles.brandButton}>
              {documentSender}
            </Badge>
          </div>
        </div>

        <Card className={`${brandStyles.brandCard} p-[15px]`}>
          <CardHeader className="flex flex-row justify-between gap-5 align-middle">
            <CardTitle className={`${brandStyles.brandText} self-center`}>
              Document Summary
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {summaryParagraphs.map((paragraph, idx) => (
                <p key={idx} className="text-gray-700 leading-relaxed">
                  {paragraph}
                </p>
              ))}

              <div className="border-t pt-4">
                <h4 className="mb-3 font-semibold">Key Points:</h4>
                <ul className="space-y-4">
                  {(documentData.actionPlan ?? []).map((point, index) => (
                    <li key={index}>
                      <div className="font-medium">
                        {index + 1}. {point.title}
                      </div>
                      <div className="ml-5 text-sm text-gray-600">
                        {point.reason}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <ActionItemsSection document={documentData} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-10">
        <div className="lg:col-span-6">
          <DocumentTextSection
            structuredTranslatedText={
              documentData.structuredTranslatedText ?? undefined
            }
          />
        </div>
      </div>
    </div>
  );
}
