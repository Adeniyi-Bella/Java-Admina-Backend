import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/hooks/auth/useAuth";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import { DocumentApi } from "@/api/document.api";
import { queryKey } from "@/types/constants";
import type { GetDocumentResponseDto } from "@/api/dto/responseDto";

export const useDocument = (docId: string) => {
  const { instance, account, isAuthenticated } = useAuth();

  return useQuery<GetDocumentResponseDto, AppError>({
    queryKey: queryKey.getDocument(docId),
    queryFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      return DocumentApi.getDocument(instance, account, docId);
    },
    enabled: isAuthenticated && !!account && !!docId,
    staleTime: Infinity,
    gcTime: 1000 * 60 * 60,
  });
};
