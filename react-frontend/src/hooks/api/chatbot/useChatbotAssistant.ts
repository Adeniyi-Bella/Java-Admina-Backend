import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/hooks/auth/useAuth";
import { DocumentApi } from "@/api/document.api";
import {
  type AppError,
  ApiError,
  MsalNoAccountError,
} from "@/api/error/customeError";
import { queryKey } from "@/types/constants";
import type {
  ChatJobResponseDto,
  ChatJobStatusResponseDto,
} from "@/api/dto/responseDto";

type UseChatbotAssistantOptions = {
  docId: string;
  onSuccess: (response: string) => void;
  onError: (error: AppError) => void;
};

const CHAT_POLL_INTERVAL_MS = 2000;

const isFinalStatus = (status: string) =>
  status === "COMPLETED" || status === "ERROR";

const clearActiveJob = (setActiveChatJobId: (value: string | null) => void) => {
  queueMicrotask(() => setActiveChatJobId(null));
};

export function useChatbotAssistant({
  docId,
  onSuccess,
  onError,
}: UseChatbotAssistantOptions) {
  const { instance, account } = useAuth();
  const queryClient = useQueryClient();
  const [activeChatJobId, setActiveChatJobId] = useState<string | null>(null);
  const completedJobRef = useRef<string | null>(null);
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  const createMutation = useMutation<ChatJobResponseDto, AppError, string>({
    mutationFn: async (prompt) => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      completedJobRef.current = null;

      return DocumentApi.sendChatMessage(instance, account, docId, { prompt });
    },
    onSuccess: (response) => {
      setActiveChatJobId(response.chatbotPollingId);
    },
    onError,
  });

  const statusQuery = useQuery<ChatJobStatusResponseDto, AppError>({
    queryKey: queryKey.getChatbotStatus(docId, activeChatJobId ?? undefined),
    queryFn: async () => {
      if (!activeChatJobId) {
        throw new MsalNoAccountError();
      }
      if (!account) {
        throw new MsalNoAccountError();
      }

      return DocumentApi.getChatJobStatus(
        instance,
        account,
        docId,
        activeChatJobId,
      );
    },
    enabled: !!activeChatJobId && !!account,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (!status || isFinalStatus(status)) {
        return false;
      }

      return CHAT_POLL_INTERVAL_MS;
    },
    refetchIntervalInBackground: true,
  });

  useEffect(() => {
    const statusData = statusQuery.data;
    const status = statusData?.status;
    if (!statusData || !status || !activeChatJobId) {
      return;
    }

    if (status === "COMPLETED" && completedJobRef.current !== activeChatJobId) {
      completedJobRef.current = activeChatJobId;
      void queryClient.invalidateQueries({
        queryKey: queryKey.getDocument(docId),
      });
      void queryClient.invalidateQueries({
        queryKey: queryKey.getDocumentChatbotCredit(docId),
      });
      void queryClient.invalidateQueries({
        queryKey: queryKey.authenticateUser(accountId),
      });
      onSuccess(statusData.response ?? "");
      clearActiveJob(setActiveChatJobId);
      return;
    }

    if (status === "ERROR") {
      onError(
        new ApiError(
          statusData.errorMessage ?? "Chat job failed",
          "CHAT_JOB_FAILED",
          400,
        ),
      );
      clearActiveJob(setActiveChatJobId);
    }
  }, [
    accountId,
    activeChatJobId,
    docId,
    onError,
    onSuccess,
    queryClient,
    statusQuery.data,
  ]);

  const isProcessing = useMemo(() => {
    const status = statusQuery.data?.status;
    return (
      createMutation.isPending ||
      statusQuery.isFetching ||
      (!!activeChatJobId && !status ? true : !!status && !isFinalStatus(status))
    );
  }, [
    activeChatJobId,
    createMutation.isPending,
    statusQuery.data?.status,
    statusQuery.isFetching,
  ]);

  const submitPrompt = (prompt: string) => {
    createMutation.mutate(prompt);
  };

  return {
    submitPrompt,
    isProcessing,
  };
}
