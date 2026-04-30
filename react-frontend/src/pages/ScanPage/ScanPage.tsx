import { useCallback, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useToast } from "@/hooks/use-toast";
import { logger } from "@/lib/logger";
import { type FileWithPages, processFileForPages } from "@/utils/pdfUtils";
import { useCreateDocument } from "@/hooks/api/document/useCreateDocument";
import { AppError } from "@/api/error/customeError";
import { languages } from "@/types/constants";
import { useAuthenticateUser } from "@/hooks/api/user/useGetUser";
import ScanPageView from "./ScanPageView";

export default function ScanPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { data: authResult } = useAuthenticateUser();
  const [file, setFile] = useState<FileWithPages | null>(null);
  const [sourceLanguage, setSourceLanguage] = useState("");
  const [targetLanguage, setTargetLanguage] = useState("");
  const [activeTab, setActiveTab] = useState("upload");
  const [clearSignal, setClearSignal] = useState(false);

  const user = authResult?.data.user;
  const uploadsLeft = authResult?.data.user.documentsUsed ?? 0;
  const canShowForm = uploadsLeft > 0;

  const { submitDocument, isProcessing, currentStatus } = useCreateDocument({
    onError: (error: AppError) => {
      toast({
        title: "Action Failed",
        description: error.userMessage,
        variant: "destructive",
        duration: 20000,
      });

      logger.error(error, "Document Submission Error", {
        userEmail: user?.email,
      });
    },
    onSuccess: (docId: string) => {
      navigate({ to: "/document/$docId", params: { docId } });
    },
  });

  const isValidInput = activeTab === "upload" && !!file;

  const isSubmitDisabled =
    !isValidInput || !sourceLanguage || !targetLanguage || !canShowForm;

  const submitLabel = "Process Document";

  const handleSetFile = useCallback(
    async (newFile: File | null) => {
      if (!newFile) {
        setFile(null);
        return;
      }

      if (
        !(newFile instanceof File) ||
        !newFile.name ||
        typeof newFile.size !== "number"
      ) {
        toast({
          variant: "destructive",
          description:
            "Invalid file selected. Please upload a valid PDF, JPG, or PNG file.",
        });
        setFile(null);
        return;
      }

      const processedFile = await processFileForPages(newFile);
      setFile(processedFile);
    },
    [toast],
  );

  const handleSubmit = useCallback(async () => {
    if (isSubmitDisabled || !file || activeTab !== "upload") {
      return;
    }

    const latestUploadsLeft = authResult?.data.user.documentsUsed ?? 0;
    if (latestUploadsLeft <= 0) {
      toast({
        title: "Document Cannot be processed",
        description: "You have no remaining document uploads.",
        variant: "destructive",
        duration: 20000,
      });
      return;
    }

    submitDocument({ file, docLanguage: sourceLanguage, targetLanguage });
  }, [
    activeTab,
    authResult,
    file,
    isSubmitDisabled,
    sourceLanguage,
    submitDocument,
    targetLanguage,
    toast,
  ]);

  const handleTabChange = useCallback((tab: string) => {
    setActiveTab(tab);
    setFile(null);
    setClearSignal((prev) => !prev);
  }, []);

  return (
    <ScanPageView
      languages={languages}
      remainingDocuments={uploadsLeft}
      file={file}
      sourceLanguage={sourceLanguage}
      targetLanguage={targetLanguage}
      activeTab={activeTab}
      clearSignal={clearSignal}
      isOverLimit={!canShowForm}
      canShowForm={canShowForm}
      showValidationAlert={false}
      validationMessage=""
      submitLabel={submitLabel}
      isSubmitDisabled={isSubmitDisabled}
      isProcessing={isProcessing}
      currentStatus={currentStatus}
      onTabChange={handleTabChange}
      onSourceLanguageChange={setSourceLanguage}
      onTargetLanguageChange={setTargetLanguage}
      onSetFile={handleSetFile}
      onClearFile={() => {
        setFile(null);
        setClearSignal((prev) => !prev);
      }}
      onSubmit={handleSubmit}
    />
  );
}
