import {
  type AccountInfo,
  type IPublicClientApplication,
} from "@azure/msal-browser";
import { buildBearerHeaders } from "./auth/authHeaders";
import {
  buildAuthenticateUserMessage,
  requireApiData,
} from "./response";
import type { CustomApiResponse } from "./interfaces/customApiResponse.interface";
import type { UserWithDocumentsResponseDto } from "./interfaces/user.interface";
import { apiClient } from "./clients/axiosClient";
import type { AuthenticateUserResult } from "./types/authentication.types";

export class UserApi {
  static async authenticate(
    instance: IPublicClientApplication,
    account: AccountInfo,
  ): Promise<AuthenticateUserResult> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.post<
      CustomApiResponse<UserWithDocumentsResponseDto>
    >(
      "/users/authenticate",
      undefined,
      {
        headers,
      },
    );

    const data = requireApiData(response.data, {
      message: "Authenticate response missing data",
      code: "EMPTY_AUTHENTICATE_RESPONSE",
      statusCode: response.status,
    });

    const created = response.status === 201;

    return {
      httpStatus: response.status,
      created,
      message: buildAuthenticateUserMessage(data.user.username, created),
      response: response.data,
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
