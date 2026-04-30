import {
  type AccountInfo,
  type IPublicClientApplication,
} from "@azure/msal-browser";
import { acquireTokenSilently } from "./acquireTokenSilently";

type HeaderMap = Record<string, string>;

export async function buildBearerAuthHeader(
  instance: IPublicClientApplication,
  account: AccountInfo,
): Promise<string> {
  const accessToken = await acquireTokenSilently(instance, account);
  return `Bearer ${accessToken}`;
}

export async function buildBearerHeaders(
  instance: IPublicClientApplication,
  account: AccountInfo,
): Promise<HeaderMap> {
  const authHeader = await buildBearerAuthHeader(instance, account);

  return {
    Authorization: authHeader,
  };
}
