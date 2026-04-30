import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/common/select";
import { Label } from "@/components/common/label";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/common/tabs";
import { Alert, AlertDescription } from "@/components/common/alert";
import { AlertTriangle } from "lucide-react";
import { type FileWithPages } from "@/utils/pdfUtils";
import { brandStyles } from "@/lib/design/styles";
import { DashboardLoader } from "@/components/common/Loader/Loader";
import ScanSubmitState from "@/components/ScanPage/ScanSubmitState";
import ScanUploadArea from "@/components/ScanPage/ScanUploadArea";
import ScanValidationState from "@/components/ScanPage/ScanValidationState";

type LanguageOption = {
  code: string;
  name: string;
};

type ScanTab = {
  value: string;
  label: string;
};

type Props = {
  languages: LanguageOption[];
  remainingDocuments?: number;
  file: FileWithPages | null;
  sourceLanguage: string;
  targetLanguage: string;
  activeTab: string;
  clearSignal: boolean;
  isOverLimit: boolean;
  canShowForm: boolean;
  showValidationAlert: boolean;
  validationMessage: string;
  submitLabel: string;
  isSubmitDisabled: boolean;
  isProcessing: boolean;
  currentStatus: string;
  onTabChange: (value: string) => void;
  onSourceLanguageChange: (value: string) => void;
  onTargetLanguageChange: (value: string) => void;
  onSetFile: (file: File | null) => void | Promise<void>;
  onClearFile: () => void;
  onSubmit: () => void;
};

const scanTabs: ScanTab[] = [{ value: "upload", label: "Upload File" }];

export default function ScanPageView({
  languages,
  remainingDocuments,
  file,
  sourceLanguage,
  targetLanguage,
  activeTab,
  clearSignal,
  isOverLimit,
  canShowForm,
  showValidationAlert,
  validationMessage,
  submitLabel,
  isSubmitDisabled,
  isProcessing,
  currentStatus,
  onTabChange,
  onSourceLanguageChange,
  onTargetLanguageChange,
  onSetFile,
  onClearFile,
  onSubmit,
}: Props) {
  return (
    <div className="space-y-6">
      <div>
        <p className="text-gray-600">
          Upload, capture, or paste your document content for analysis
        </p>
      </div>

      {!isProcessing ? (
        <div className="max-w-2xl">
          {isOverLimit && (
            <Alert
              className={`mb-6 ${brandStyles.dangerSoftBg} ${brandStyles.dangerText}`}
            >
              <AlertTriangle className={`h-4 w-4 ${brandStyles.dangerText}`} />
              <AlertDescription className={brandStyles.dangerText}>
                <strong>Monthly limit reached!</strong> You&apos;ve processed
                the maximum documents allowed.
              </AlertDescription>
            </Alert>
          )}

          <Tabs
            value={activeTab}
            onValueChange={onTabChange}
            className="space-y-0"
          >
            <TabsList className="grid w-full grid-cols-1">
              {scanTabs.map((tab) => {
                const isActive = activeTab === tab.value;
                return (
                  <TabsTrigger
                    key={tab.value}
                    value={tab.value}
                    className={
                      isActive
                        ? `text-white ${brandStyles.brandBg}`
                        : "text-gray-700"
                    }
                  >
                    {tab.label}
                  </TabsTrigger>
                );
              })}
            </TabsList>

            <TabsContent value="upload">
              <ScanUploadArea
                file={file}
                remainingDocuments={remainingDocuments}
                clearSignal={clearSignal}
                onSetFile={onSetFile}
                onClearFile={onClearFile}
              />
            </TabsContent>

            {/* <TabsContent value="text">
              <ScanTextInputArea
                textInput={textInput}
                setTextInput={onTextInputChange}
              />
            </TabsContent> */}
          </Tabs>

          {canShowForm && (
            <>
              <ScanValidationState
                isOverLimit={isOverLimit}
                showValidationAlert={showValidationAlert}
                validationMessage={validationMessage}
              />

              <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
                <Card className={brandStyles.brandCard}>
                  <CardHeader>
                    <CardTitle className={brandStyles.brandText}>
                      Source Language
                    </CardTitle>
                    <CardDescription>
                      What language is your document in?
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2 pt-5">
                      <Label htmlFor="source-language">Document Language</Label>
                      <Select
                        value={sourceLanguage}
                        onValueChange={onSourceLanguageChange}
                      >
                        <SelectTrigger className={brandStyles.brandBorder}>
                          <SelectValue placeholder="Select document language" />
                        </SelectTrigger>
                        <SelectContent>
                          {languages.map((lang) => (
                            <SelectItem key={lang.code} value={lang.code}>
                              {lang.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </CardContent>
                </Card>

                <Card className={brandStyles.brandCard}>
                  <CardHeader>
                    <CardTitle className={brandStyles.brandText}>
                      Target Language
                    </CardTitle>
                    <CardDescription>
                      What language do you want it translated to?
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <Label htmlFor="target-language">
                        Translation Language
                      </Label>
                      <Select
                        value={targetLanguage}
                        onValueChange={onTargetLanguageChange}
                      >
                        <SelectTrigger className={brandStyles.brandBorder}>
                          <SelectValue placeholder="Select target language" />
                        </SelectTrigger>
                        <SelectContent>
                          {languages.map((lang) => (
                            <SelectItem key={lang.code} value={lang.code}>
                              {lang.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </CardContent>
                </Card>
              </div>

              <ScanSubmitState
                submitLabel={submitLabel}
                isSubmitDisabled={isSubmitDisabled}
                onSubmit={onSubmit}
              />
            </>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          <DashboardLoader text={currentStatus} />
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            Processing is in progress. Do not reload or leave this page until
            the document finishes processing, to avoid inconsistent behaviour.
          </div>
        </div>
      )}
    </div>
  );
}
