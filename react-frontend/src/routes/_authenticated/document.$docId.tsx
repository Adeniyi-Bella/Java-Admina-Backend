import { createFileRoute } from "@tanstack/react-router";
import SidebarLayout from "@/pages/Sidebar/Sidebar";

export const Route = createFileRoute("/_authenticated/document/$docId")({
  component: DocumentDetailRoute,
});

function DocumentDetailRoute() {
  const { docId } = Route.useParams();

  return (
    <SidebarLayout>
      <div className="space-y-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Document Details</h1>
          <p className="text-gray-600">
            Document {docId} is ready for the next UI pass.
          </p>
        </div>
      </div>
    </SidebarLayout>
  );
}
