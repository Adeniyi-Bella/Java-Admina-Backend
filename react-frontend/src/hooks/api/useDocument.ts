import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/hooks/auth/useAuth";
import { DocumentApi } from "@/api/document.api";
import {
  type AppError,
  ApiError,
  MsalNoAccountError,
} from "@/api/error/customeError";
import type { FileWithPages } from "@/utils/pdfUtils";
import { DOCUMENT_POLL_INTERVAL_MS, queryKey } from "@/types/constants";
import type { CreateDocumentRequestDto } from "@/api/dto/requestDto";
import type {
  DocumentJobResponseDto,
  DocumentProcessStatus,
  DocumentStatusResponseDto,
} from "@/api/dto/responseDto";

type CreateDocumentPayload = CreateDocumentRequestDto & {
  file: FileWithPages;
};

type UseCreateDocumentOptions = {
  onError: (error: AppError) => void;
  onSuccess: (docId: string) => void;
};

const documentStatusMessages: Record<DocumentProcessStatus, string> = {
  PENDING: "Uploading document...",
  QUEUE: "Queued for processing...",
  TRANSLATE: "Translating document...",
  SUMMARIZE: "Summarizing document...",
  SAVING: "Saving document...",
  COMPLETED: "Document processed successfully",
  ERROR: "Document processing failed",
  CANCELLED: "Document processing cancelled",
};

const isFinalStatus = (status: DocumentProcessStatus) =>
  status === "COMPLETED" || status === "ERROR" || status === "CANCELLED";

export function useCreateDocument({
  onError,
  onSuccess,
}: UseCreateDocumentOptions) {
  const { instance, account } = useAuth();
  const queryClient = useQueryClient();
  const [activeDocId, setActiveDocId] = useState<string | null>(null);
  const completedDocRef = useRef<string | null>(null);
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  const createMutation = useMutation<
    DocumentJobResponseDto,
    AppError,
    CreateDocumentPayload
  >({
    mutationFn: async ({ file, docLanguage, targetLanguage }) => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      completedDocRef.current = null;

      return DocumentApi.createDocument(instance, account, file, {
        docLanguage,
        targetLanguage,
      });
    },
    onSuccess: (response) => {
      setActiveDocId(response.docId);
    },
    onError,
  });

  const statusQuery = useQuery<DocumentStatusResponseDto, AppError>({
    queryKey: queryKey.getDocumentStatus(activeDocId ?? ""),
    queryFn: async () => {
      if (!activeDocId) {
        throw new MsalNoAccountError();
      }
      if (!account) {
        throw new MsalNoAccountError();
      }

      return DocumentApi.getDocumentStatus(instance, account, activeDocId);
    },
    enabled: !!activeDocId && !!account,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (!status || isFinalStatus(status)) {
        return false;
      }
      return DOCUMENT_POLL_INTERVAL_MS;
    },
    refetchIntervalInBackground: true,
  });

  useEffect(() => {
    const statusData = statusQuery.data;
    const status = statusData?.status;
    if (!statusData || !status || !activeDocId) {
      return;
    }

    if (status === "COMPLETED" && completedDocRef.current !== activeDocId) {
      completedDocRef.current = activeDocId;
      void queryClient.invalidateQueries({
        queryKey: queryKey.authenticateUser(accountId),
      });
      onSuccess(activeDocId);
      return;
    }

    if (status === "ERROR" || status === "CANCELLED") {
      onError(
        new ApiError(
          statusData.errorMessage ?? "Document processing failed",
          "DOCUMENT_PROCESSING_FAILED",
          400,
        ),
      );
    }
  }, [
    accountId,
    activeDocId,
    onError,
    onSuccess,
    queryClient,
    statusQuery.data,
  ]);

  const currentStatus = useMemo(() => {
    if (createMutation.isPending) {
      return documentStatusMessages.PENDING;
    }

    const status = statusQuery.data?.status;
    if (status) {
      return documentStatusMessages[status] ?? "Processing document...";
    }

    if (activeDocId) {
      return "Processing document...";
    }

    return "Preparing document...";
  }, [activeDocId, createMutation.isPending, statusQuery.data?.status]);

  const isProcessing = useMemo(() => {
    const status = statusQuery.data?.status;
    return (
      createMutation.isPending ||
      statusQuery.isFetching ||
      (!!activeDocId && !status ? true : !!status && !isFinalStatus(status))
    );
  }, [
    activeDocId,
    createMutation.isPending,
    statusQuery.data?.status,
    statusQuery.isFetching,
  ]);

  const submitDocument = (payload: CreateDocumentPayload) => {
    createMutation.mutate(payload);
  };

  return {
    submitDocument,
    isProcessing,
    currentStatus,
  };
}
