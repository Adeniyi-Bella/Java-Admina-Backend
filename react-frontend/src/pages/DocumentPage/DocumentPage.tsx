import { useEffect } from "react";
import { useNavigate, useParams } from "@tanstack/react-router";
import { useGetDocument } from "@/hooks/api/document/useDocument";
import { PageLoader } from "@/components/common/Loader/Loader";
import DocumentPageView from "@/components/DocumentPage/DocumentPageView";
import { AppError } from "@/api/error/customeError";

export default function DocumentPage() {
  const navigate = useNavigate();
  const { docId } = useParams({ from: "/_authenticated/document/$docId" });

  const {
    data: documentData,
    error,
    isLoading,
    isError,
  } = useGetDocument(docId);

  useEffect(() => {
    if (!isError || !error) {
      return;
    }

    if (error instanceof AppError && error.statusCode === 401) {
      return;
    }

    if (error instanceof AppError && typeof error.statusCode === "number") {
      if (error.statusCode < 500) {
        navigate({ to: "/dashboard", replace: true });
      }
      return;
    }

    navigate({ to: "/dashboard", replace: true });
  }, [error, isError, navigate]);

  if (isLoading) return <PageLoader />;

  if (isError || !documentData) {
    return <PageLoader />;
  }

  return (
    <DocumentPageView
      documentData={documentData}
      onBack={() => navigate({ to: "/dashboard" })}
    />
  );
}
