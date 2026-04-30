import {
  type AccountInfo,
  type IPublicClientApplication,
} from "@azure/msal-browser";
import { buildBearerHeaders } from "./auth/authHeaders";
import { requireApiData } from "./response";
import type { CustomApiResponse } from "./interfaces/customApiResponse.interface";
import { apiClient } from "./clients/axiosClient";
import type { UserWithDocumentsResponseDto } from "./dto/responseDto";

export class UserApi {
  static async authenticate(
    instance: IPublicClientApplication,
    account: AccountInfo,
  ): Promise<{ data: UserWithDocumentsResponseDto }> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.post<
      CustomApiResponse<UserWithDocumentsResponseDto>
    >("/users/authenticate", undefined, {
      headers,
    });

    const data = requireApiData(response.data, {
      message: "Authenticate response missing data",
      code: "EMPTY_AUTHENTICATE_RESPONSE",
      statusCode: response.status,
    });

    return {
      data,
    };
  }

  static async deleteUser(
    instance: IPublicClientApplication,
    account: AccountInfo,
  ): Promise<void> {
    const headers = await buildBearerHeaders(instance, account);

    await apiClient.delete("/users", {
      headers,
    });
  }
}
