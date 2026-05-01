import { useEffect, useMemo, useRef, useState } from "react";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useAuth } from "@/hooks/auth/useAuth";
import { DocumentApi } from "@/api/document.api";
import {
  type AppError,
  ApiError,
  MsalNoAccountError,
} from "@/api/error/customeError";
import {
  DOCUMENT_POLL_INTERVAL_MS,
  documentStatusMessages,
  queryKey,
} from "@/types/constants";
import type {
  DocumentJobResponseDto,
  DocumentProcessStatus,
  DocumentStatusResponseDto,
  GetDocumentResponseDto,
  GetDocumentsPageDto,
} from "@/api/dto/responseDto";
import type { CreateDocumentPayload } from "@/api/dto/requestDto";

type UseCreateDocumentOptions = {
  onError: (error: AppError) => void;
  onSuccess: (docId: string) => void;
};

// A document job has reached a terminal state — polling should stop
const isFinalStatus = (status: DocumentProcessStatus) =>
  status === "COMPLETED" || status === "ERROR" || status === "CANCELLED";

/**
 * Manages the full document creation and processing flow:
 *
 * 1. Uploads the file to the backend via a mutation
 * 2. Receives a job ID and starts polling the status endpoint
 * 3. Stops polling once the job reaches a terminal state
 * 4. Calls onSuccess or onError depending on the outcome
 * 5. Invalidates the document list and user cache so the UI stays in sync
 */
export function useCreateDocument({
  onError,
  onSuccess,
}: UseCreateDocumentOptions) {
  const { instance, account } = useAuth();
  const queryClient = useQueryClient();

  // ID of the active processing job — null when idle
  const [activeDocId, setActiveDocId] = useState<string | null>(null);

  // Tracks which docId has already triggered onSuccess to prevent
  // the callback firing twice if the component re-renders after completion
  const completedDocRef = useRef<string | null>(null);

  // Scopes query invalidations to the current user's cache entries
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  // ── Step 1: Upload the document
  const createMutation = useMutation<
    DocumentJobResponseDto,
    AppError,
    CreateDocumentPayload
  >({
    mutationFn: async ({ file, docLanguage, targetLanguage }) => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      // Reset completed ref so re-uploading the same document works correctly
      completedDocRef.current = null;

      return DocumentApi.createDocument(instance, account, file, {
        docLanguage,
        targetLanguage,
      });
    },
    onSuccess: (response) => {
      // Activates the status polling query below once the document is successfully uploaded and we have a job ID to track
      setActiveDocId(response.docId);
    },
    onError,
  });

  // ── Step 2: Poll the status endpoint using the job ID from the mutation response
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
    // Only poll when a job is active and the user is logged in
    enabled: !!activeDocId && !!account,

    // polling logic: if we have a status and it's not a final state, poll every DOCUMENT_POLL_INTERVAL_MS milliseconds. Otherwise, stop polling.
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      // Stop polling once the job has reached a terminal state (COMPLETED, ERROR, or CANCELLED)
      if (!status || isFinalStatus(status)) {
        return false;
      }
      return DOCUMENT_POLL_INTERVAL_MS;
    },
    // Keep polling even if the user switches to another browser tab or minimizes the window, so they get real-time updates on their document status without needing to manually refresh
    refetchIntervalInBackground: true,
  });

  // ── Step 3: React to status changes
  useEffect(() => {
    const statusData = statusQuery.data;
    const status = statusData?.status;
    // Nothing to act on — job hasn't started or no data yet
    if (!statusData || !status || !activeDocId) {
      return;
    }

    if (status === "COMPLETED" && completedDocRef.current !== activeDocId) {
      // Mark as handled before calling onSuccess to prevent duplicate calls if the component re-renders after completion
      completedDocRef.current = activeDocId;

      // Refresh the document list and user data so the UI reflects
      // the newly created document and updated plan limits
      void queryClient.invalidateQueries({
        queryKey: queryKey.getDocuments(accountId),
      });
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

  /**
   * Derives the status message from mutation and query state.
   * Uses useMemo instead of useState to avoid an extra render cycle.
   */
  const currentStatus = useMemo(() => {
    /**
     * Derives the status message from mutation and query state.
     * Uses useMemo instead of useState to avoid an extra render cycle.
     */
    if (createMutation.isPending) {
      return documentStatusMessages.PENDING;
    }

    // Status poll has data — show the message for the latest status
    const status = statusQuery.data?.status;
    if (status) {
      return documentStatusMessages[status] ?? "Processing document...";
    }

    // Job created but first poll hasn't returned yet
    if (activeDocId) {
      return "Processing document...";
    }
    // Idle — nothing is happening
    return "Preparing document...";
  }, [activeDocId, createMutation.isPending, statusQuery.data?.status]);

  /**
   * True while anything is in-flight:
   * - The upload mutation is pending
   * - The status query is actively fetching
   * - A job is active and hasn't reached a terminal state yet
   */
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

/**
 * Fetches a single document by ID.
 *
 * Cached indefinitely (staleTime: Infinity) since document content doesn't
 * change after processing. Only refreshed on explicit query invalidation
 * (e.g. after a task is updated or the document is deleted).
 */
export const useGetDocument = (docId: string) => {
  const { instance, account, isAuthenticated } = useAuth();

  return useQuery<GetDocumentResponseDto, AppError>({
    queryKey: queryKey.getDocument(docId),
    queryFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      return DocumentApi.getDocument(instance, account, docId);
    },
    // Don't fetch if the user isn't logged in or no docId is provided
    enabled: isAuthenticated && !!account && !!docId,
    staleTime: Infinity,
    gcTime: 1000 * 60 * 60,
  });
};

/**
 * Fetches a paginated list of documents for the current user.
 *
 * Uses placeholderData to keep the previous page visible while the next
 * page loads — prevents the list from flickering or going blank between
 * page changes.
 */
export function useGetAllDocuments(page: number, size: number) {
  const { instance, account, isAuthenticated } = useAuth();
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  return useQuery<GetDocumentsPageDto, AppError>({
    // page and size are included in the key so each combination has
    // its own cache entry — navigating back to a previous page is instant
    queryKey: [...queryKey.getDocuments(accountId), page, size] as const,
    queryFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      return DocumentApi.getDocuments(instance, account, page, size);
    },
    enabled: isAuthenticated && !!account,
    // Show the previous page's data while the next page is loading
    placeholderData: keepPreviousData,
  });
}
