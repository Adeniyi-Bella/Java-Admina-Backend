"use client";

import { useEffect, useRef, useState } from "react";
import { MessageCircle, Send } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { brandStyles } from "@/lib/design/styles";
import { chatbotTranslations } from "./chatBotTranslations";
import type { GetDocumentResponseDto } from "@/api/dto/responseDto";
import { useChatbotAssistant } from "@/hooks/api/chatbot/useChatbotAssistant";
import type { AppError } from "@/api/error/customeError";

type Props = {
  documentData: GetDocumentResponseDto;
};

type ChatUiMessage = {
  id: string;
  type: "user" | "bot";
  message: string;
  timestamp: Date;
};

export default function ChatBotAssistant({ documentData }: Props) {
  const content =
    chatbotTranslations[
      (documentData.targetLanguage ?? "en") as keyof typeof chatbotTranslations
    ] ?? chatbotTranslations.en;

  const [pendingBotMessageId, setPendingBotMessageId] = useState<string | null>(
    null,
  );
  const { submitPrompt, isProcessing } = useChatbotAssistant({
    docId: documentData.id,
    onSuccess: (responseText) => {
      if (!pendingBotMessageId) {
        return;
      }

      setChatMessages((prev) =>
        prev.map((message) =>
          message.id === pendingBotMessageId
            ? {
                ...message,
                message: responseText || content.failedFetch,
              }
            : message,
        ),
      );
      setPendingBotMessageId(null);
    },
    onError: (error: AppError) => {
      setError(error.userMessage ?? content.error);
      if (!pendingBotMessageId) {
        return;
      }

      setChatMessages((prev) =>
        prev.filter((message) => message.id !== pendingBotMessageId),
      );
      setPendingBotMessageId(null);
    },
  });
  const chatContainerRef = useRef<HTMLDivElement | null>(null);
  const [chatInput, setChatInput] = useState("");
  const [chatMessages, setChatMessages] = useState<ChatUiMessage[]>(
    () =>
      documentData.chatMessagesHistory?.length
        ? documentData.chatMessagesHistory.map((message) => ({
            id: message.id,
            type: message.role === "USER" ? "user" : "bot",
            message: message.content,
            timestamp: new Date(message.createdAt),
          }))
        : [
            {
              id: "greeting",
              type: "bot",
              message: content.greeting,
              timestamp: new Date(),
            },
          ],
  );
  const [error, setError] = useState<string | null>(null);

  const chatbotRemaining = documentData.chatbotCreditRemaining ?? 0;
  const hasChatbotCredits = chatbotRemaining > 0;

  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop =
        chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  useEffect(() => {
    setChatMessages(
      documentData.chatMessagesHistory?.length
        ? documentData.chatMessagesHistory.map((message) => ({
            id: message.id,
            type: message.role === "USER" ? "user" : "bot",
            message: message.content,
            timestamp: new Date(message.createdAt),
          }))
        : [
            {
              id: "greeting",
              type: "bot",
              message: content.greeting,
              timestamp: new Date(),
            },
          ],
    );
  }, [content.greeting, documentData.chatMessagesHistory]);

  const handleSendMessage = () => {
    if (!chatInput.trim() || isProcessing || !hasChatbotCredits) {
      return;
    }

    const prompt = chatInput.trim();
    setError(null);
    setChatInput("");

    const userMessageId = crypto.randomUUID();
    const botMessageId = crypto.randomUUID();

    setChatMessages((prev) => [
      ...prev,
      {
        id: userMessageId,
        type: "user",
        message: prompt,
        timestamp: new Date(),
      },
      {
        id: botMessageId,
        type: "bot",
        message: "",
        timestamp: new Date(),
      },
    ]);
    setPendingBotMessageId(botMessageId);

    submitPrompt(prompt);
  };

  return (
    <Card className={`border-none lg:border h-[40%] ${brandStyles.brandCard}`}>
      <CardHeader>
        <CardTitle className={brandStyles.brandText}>
          <MessageCircle className="mr-2 inline h-5 w-5" />
          {content.title}
        </CardTitle>
        <CardDescription>{content.description}</CardDescription>
      </CardHeader>

      <CardContent>
        <div
          ref={chatContainerRef}
          className="max-h-[80vh] space-y-4 overflow-y-auto"
        >
          {chatMessages.map((message) => (
            <div
              key={message.id}
              className={`flex ${message.type === "user" ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`max-w-[85%] rounded-lg p-3 ${
                  message.type === "user"
                    ? `text-white ${brandStyles.brandBg}`
                    : "bg-gray-100 text-gray-800"
                }`}
              >
                <p className="whitespace-pre-wrap text-sm">{message.message}</p>
                <p className="mt-1 text-xs opacity-70">
                  {message.timestamp.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </p>
              </div>
            </div>
          ))}

          {isProcessing && (
            <div className="flex justify-start">
              <div className="rounded-lg bg-gray-100 p-3">
                <div className="flex space-x-1">
                  <div className="h-2 w-2 animate-bounce rounded-full bg-gray-400" />
                  <div
                    className="h-2 w-2 animate-bounce rounded-full bg-gray-400"
                    style={{ animationDelay: "0.1s" }}
                  />
                  <div
                    className="h-2 w-2 animate-bounce rounded-full bg-gray-400"
                    style={{ animationDelay: "0.2s" }}
                  />
                </div>
              </div>
            </div>
          )}
        </div>

        <div
          className={`mt-6 rounded-lg border p-1 ${
            chatbotRemaining <= 2
              ? `${brandStyles.dangerSoftBg} ${brandStyles.dangerSoft2Border}`
              : `${brandStyles.warningSoftBg} ${brandStyles.warningSoftBorder}`
          }`}
        >
          <div className="flex items-center space-x-2">
            <div
              className={`h-2 w-2 rounded-full ${
                chatbotRemaining <= 2 ? "bg-red-400" : "bg-yellow-400"
              }`}
            />
            <p
              className={`text-xs font-medium ${
                chatbotRemaining <= 2 ? "text-red-700" : "text-yellow-700"
              }`}
            >
              {chatbotRemaining <= 0
                ? "No prompts remaining for this document"
                : `${chatbotRemaining} prompt${
                    chatbotRemaining !== 1 ? "s" : ""
                  } remaining for this document`}
            </p>
          </div>
        </div>

        <div className="mb-2 mt-2 flex flex-col gap-2">
          <div className="mb-2 mt-1 flex space-x-2">
            <textarea
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onInput={(e) => {
                const t = e.target as HTMLTextAreaElement;
                t.style.height = "10px";
                t.style.height = `${t.scrollHeight}px`;
                t.style.maxHeight = "150px";
                t.style.overflowY = t.scrollHeight > 150 ? "auto" : "hidden";
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  void handleSendMessage();
                }
              }}
              placeholder={content.placeholder}
              rows={1}
              className={`flex-1 resize-none overflow-hidden rounded-md border p-2 text-sm ${brandStyles.brandBorder}`}
              disabled={!hasChatbotCredits || isProcessing}
            />
            <Button
              onClick={() => void handleSendMessage()}
              disabled={!chatInput.trim() || isProcessing || !hasChatbotCredits}
              className={`${brandStyles.brandButton} p-[5px]`}
              size="sm"
            >
              <Send />
            </Button>
          </div>
        </div>

        {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
      </CardContent>
    </Card>
  );
}
