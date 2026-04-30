import { type AccountInfo, type IPublicClientApplication } from "@azure/msal-browser";
import { buildBearerHeaders } from "./auth/authHeaders";
import { apiClient } from "./clients/axiosClient";
import { requireApiData } from "./response";
import type { CustomApiResponse } from "./interfaces/customApiResponse.interface";
import type {
  CreateDocumentRequestDto,
} from "./dto/requestDto";
import type {
  DocumentJobResponseDto,
  DocumentStatusResponseDto,
} from "./dto/documentDto";

export class DocumentApi {
  static async createDocument(
    instance: IPublicClientApplication,
    account: AccountInfo,
    file: File,
    request: CreateDocumentRequestDto,
  ): Promise<DocumentJobResponseDto> {
    const headers = await buildBearerHeaders(instance, account);
    const formData = new FormData();

    formData.append("file", file);
    formData.append("docLanguage", request.docLanguage);
    formData.append("targetLanguage", request.targetLanguage);

    const response = await apiClient.post<CustomApiResponse<DocumentJobResponseDto>>(
      "/documents",
      formData,
      {
        headers,
      },
    );

    return requireApiData(response.data, {
      message: "Create document response missing data",
      code: "EMPTY_CREATE_DOCUMENT_RESPONSE",
      statusCode: response.status,
    });
  }

  static async getDocumentStatus(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
  ): Promise<DocumentStatusResponseDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.get<
      CustomApiResponse<DocumentStatusResponseDto>
    >(`/documents/status/${docId}`, {
      headers,
    });

    return requireApiData(response.data, {
      message: "Document status response missing data",
      code: "EMPTY_DOCUMENT_STATUS_RESPONSE",
      statusCode: response.status,
    });
  }
}
