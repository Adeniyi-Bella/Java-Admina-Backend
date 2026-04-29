import type { CustomApiResponse } from "@/api/interfaces/customApiResponse.interface";
import type { UserWithDocumentsResponseDto } from "@/api/interfaces/user.interface";

export type AuthenticateUserResult = {
  httpStatus: number;
  created: boolean;
  message: string;
  response: CustomApiResponse<UserWithDocumentsResponseDto>;
  data: UserWithDocumentsResponseDto;
};
