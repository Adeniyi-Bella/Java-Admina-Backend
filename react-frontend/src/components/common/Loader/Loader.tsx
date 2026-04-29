"use client"

import UnifiedLoading from "./loading-gif"
import { LOADING_TEXT } from "./loading-copy"
import { brandStyles } from "@/lib/design/styles";

export function DocumentProcessingLoader({ text = LOADING_TEXT.processingDocument }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function DashboardLoader({ text = LOADING_TEXT.dashboard }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function FormSubmissionLoader({ text = LOADING_TEXT.submitting }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function PageLoader({ text = LOADING_TEXT.page }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function ApiLoader({ text = LOADING_TEXT.fetchingData }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function UploadLoader({ text = LOADING_TEXT.uploadingDocument }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function DownloadLoader({ text = LOADING_TEXT.downloadingPdf }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function AuthLoader({ text = LOADING_TEXT.auth }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function GenericLoader({ text = LOADING_TEXT.generic }: { text?: string }) {
  return <UnifiedLoading text={text} />
}

export function InlineLoader({ text = LOADING_TEXT.generic }: { text?: string }) {
  return (
    <div className="flex items-center justify-center py-6">
      <div className="text-center">
        <div
          className={`w-6 h-6 border-4 border-t-transparent rounded-full animate-spin mx-auto mb-2 ${brandStyles.brandStrongBorder}`}
        />
        <p className="text-sm text-muted-foreground">{text}</p>
      </div>
    </div>
  );
}
