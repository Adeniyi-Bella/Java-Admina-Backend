import {
  type AccountInfo,
  type IPublicClientApplication,
} from "@azure/msal-browser";
import { buildBearerHeaders } from "./auth/authHeaders";
import { apiClient } from "./clients/axiosClient";
import { requireApiData } from "./response";
import type { SubscriptionCheckoutRequestDto } from "./dto/requestDto";
import type {
  CustomApiResponse,
} from "./dto/responseDto";

export class BillingApi {
  static async createCheckoutSession(
    instance: IPublicClientApplication,
    account: AccountInfo,
    request: SubscriptionCheckoutRequestDto,
    idempotencyKey: string,
  ): Promise<string> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.post<CustomApiResponse<string>>(
      "/billing/checkout",
      request,
      {
        headers: {
          ...headers,
          "Idempotency-Key": idempotencyKey,
        },
      },
    );        

    return requireApiData(response.data, {
      message: "Checkout session response missing data",
      code: "EMPTY_CHECKOUT_SESSION_RESPONSE",
      statusCode: response.status,
    });
  }
}
