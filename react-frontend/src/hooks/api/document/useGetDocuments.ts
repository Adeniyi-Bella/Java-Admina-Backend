import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { DocumentApi } from "@/api/document.api";
import { AppError, MsalNoAccountError } from "@/api/error/customeError";
import { useAuth } from "@/hooks/auth/useAuth";
import { queryKey } from "@/types/constants";
import type { GetDocumentsPageDto } from "@/api/dto/responseDto";

export function useGetDocuments(page: number, size: number) {
  const { instance, account, isAuthenticated } = useAuth();
  const accountId =
    account?.homeAccountId ?? account?.localAccountId ?? account?.username;

  return useQuery<GetDocumentsPageDto, AppError>({
    queryKey: [...queryKey.getAllDocuments, page, size, accountId] as const,
    queryFn: async () => {
      if (!account) {
        throw new MsalNoAccountError();
      }

      return DocumentApi.getDocuments(instance, account, page, size);
    },
    enabled: isAuthenticated && !!account,
    placeholderData: keepPreviousData,
  });
}
