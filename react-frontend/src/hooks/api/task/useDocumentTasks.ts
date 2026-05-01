import { useMutation, useQueryClient } from "@tanstack/react-query";
import { DocumentApi } from "@/api/document.api";
import type { AddTaskToDocumentRequestDto, UpdateExistingTaskRequestDto } from "@/api/dto/requestDto";
import type { ActionPlanTaskDto } from "@/api/dto/responseDto";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import { useAuth } from "@/hooks/auth/useAuth";
import { queryKey } from "@/types/constants";

type UseDocumentTasksOptions = {
  documentId: string;
  onError: (error: AppError) => void;
  onAddSuccess?: (task: ActionPlanTaskDto) => void;
  onUpdateSuccess?: (task: ActionPlanTaskDto) => void;
  onDeleteSuccess?: () => void;
};

export function useDocumentTasks({
  documentId,
  onError,
  onAddSuccess,
  onUpdateSuccess,
  onDeleteSuccess,
}: UseDocumentTasksOptions) {
  const { instance, account } = useAuth();
  const queryClient = useQueryClient();
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  const invalidateDocumentData = async () => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: queryKey.getDocument(documentId),
      }),
      queryClient.invalidateQueries({
        queryKey: queryKey.getDocuments(accountId),
      }),
      queryClient.invalidateQueries({
        queryKey: ["authenticateUser"],
      }),
      accountId
        ? queryClient.invalidateQueries({
            queryKey: queryKey.authenticateUser(accountId),
          })
        : Promise.resolve(),
    ]);
  };

  const getAccount = () => {
    if (!account) {
      throw new MsalNoAccountError();
    }

    return account;
  };

  const addTaskMutation = useMutation<
    ActionPlanTaskDto,
    AppError,
    AddTaskToDocumentRequestDto
  >({
    mutationFn: async (payload) => {
      return DocumentApi.addTask(instance, getAccount(), documentId, payload);
    },
    onSuccess: async (task) => {
      await invalidateDocumentData();
      onAddSuccess?.(task);
    },
    onError,
  });

  const updateTaskMutation = useMutation<
    ActionPlanTaskDto,
    AppError,
    { taskId: string; payload: UpdateExistingTaskRequestDto }
  >({
    mutationFn: async ({ taskId, payload }) => {
      return DocumentApi.updateTask(
        instance,
        getAccount(),
        documentId,
        taskId,
        payload,
      );
    },
    onSuccess: async (task) => {
      await invalidateDocumentData();
      onUpdateSuccess?.(task);
    },
    onError,
  });

  const deleteTaskMutation = useMutation<void, AppError, string>({
    mutationFn: async (taskId) => {
      await DocumentApi.deleteTask(instance, getAccount(), documentId, taskId);
    },
    onSuccess: async () => {
      await invalidateDocumentData();
      onDeleteSuccess?.();
    },
    onError,
  });

  return {
    addTask: addTaskMutation.mutate,
    updateTask: updateTaskMutation.mutate,
    deleteTask: deleteTaskMutation.mutate,
    isAddingTask: addTaskMutation.isPending,
    isUpdatingTask: updateTaskMutation.isPending,
    isDeletingTask: deleteTaskMutation.isPending,
  };
}
