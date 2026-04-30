import { createFileRoute } from "@tanstack/react-router";
import DocumentPage from "@/pages/DocumentPage/DocumentPage";

export const Route = createFileRoute("/_authenticated/document/$docId")({
  component: DocumentPage,
});

