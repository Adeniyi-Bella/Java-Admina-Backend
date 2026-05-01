import { useMemo, useState } from "react";
import { useLocation, useNavigate } from "@tanstack/react-router";
import { useGetDocuments } from "@/hooks/api/document/useGetDocuments";
import {
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  Clock,
  Eye,
  FileText,
  Search,
} from "lucide-react";
import { Badge } from "@/components/common/badge";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Input } from "@/components/common/input";
import { Progress } from "@/components/common/progress";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/common/select";
import { brandStyles } from "@/lib/design/styles";
import { Link } from "@tanstack/react-router";
import type { HistoryDocument } from "@/types/pages/types";

// Number of documents to show per page
const PAGE_SIZE = 5;

/**
 * Determines whether a document is "completed" or "active".
 * A document is completed if it has no tasks at all, or all tasks are done.
 */
const mapStatus = (
  completedTasksCount: number,
  uncompletedTasksCount: number,
): HistoryDocument["status"] =>
  completedTasksCount + uncompletedTasksCount === 0 ||
  uncompletedTasksCount === 0
    ? "completed"
    : "active";

/**
 * Returns the appropriate icon for a document's status.
 * - active   → orange clock
 * - completed → brand-colored check
 * - unknown  → grey file icon
 */
const getStatusIcon = (status: string) => {
  switch (status) {
    case "active":
      return <Clock className={`h-4 w-4 ${brandStyles.orangeText}`} />;
    case "completed":
      return <CheckCircle className={`h-4 w-4 ${brandStyles.brandText}`} />;
    default:
      return <FileText className="h-4 w-4 text-gray-600" />;
  }
};

export default function HistoryPage() {
  // Local UI state for search input and status filter dropdown
  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState("all");

  // Current page index (0-based) for server-side pagination
  const [page, setPage] = useState(0);

  // TanStack Router hooks — navigate() for programmatic navigation,
  // pathname for the current URL path (used as the "back" destination)
  const navigate = useNavigate();
  const { pathname } = useLocation();

  // Fetch the current page of documents from the API.
  // isPlaceholderData is true while a new page is loading,
  // allowing us to show the previous page's data with reduced opacity.
  const { data, isError, isPlaceholderData } = useGetDocuments(page, PAGE_SIZE);
  const totalItems = data?.totalItems ?? 0;
  const totalPages = data?.totalPages ?? 0;

  /**
   * Transform raw API documents into HistoryDocument shape,
   * then apply client-side search and status filtering.
   * Only re-runs when the data, search term, or filter changes.
   */
  const processedDocuments = useMemo<HistoryDocument[]>(() => {
    const documents = data?.items ?? [];

    if (!documents.length) return [];

    return (
      documents
        .map((document) => {
          const totalTasks =
            document.completedTasksCount + document.uncompletedTasksCount;
          const completedTasks = document.completedTasksCount;

          // Progress is a percentage (0–100), defaulting to 0 if there are no tasks
          const progress =
            totalTasks > 0 ? (completedTasks / totalTasks) * 100 : 0;

          return {
            docId: document.id,
            title: document.title ?? undefined,
            sender: document.sender ?? undefined,
            receivedDate: document.receivedDate
              ? new Date(document.receivedDate)
              : undefined,
            status: mapStatus(
              document.completedTasksCount,
              document.uncompletedTasksCount,
            ),
            completedTasks,
            totalTasks,
            progress,
          };
        })

        // A document matches the search if its title or sender contains the term
        .filter((document) => {
          const search = searchTerm.trim().toLowerCase();
          const matchesSearch =
            !search ||
            [document.title, document.sender]
              .filter(Boolean)
              .some((value) => value!.toLowerCase().includes(search));

          // "all" means no status filter is applied
          const matchesStatus =
            filterStatus === "all" || document.status === filterStatus;

          return matchesSearch && matchesStatus;
        })
    );
  }, [data?.items, filterStatus, searchTerm]);

  /**
   * Navigates to the document detail page.
   * Passes the current pathname as a `from` query param so the
   * document page knows where to send the user when they click "Go back".
   *
   * Result URL example: /document/abc-123?from=/dashboard/history
   */
  const handleViewDocument = (docId: string) => {
    navigate({
      to: "/document/$docId",
      params: { docId },
      search: { from: pathname },
    });
  };

  // Show a simple error message if the API call failed
  if (isError) {
    return (
      <div className="p-6 text-center text-red-500">
        Failed to load documents. Please try again later.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-gray-600">
          Track all your processed documents and their current status
        </p>
        <p className="mt-1 text-sm text-gray-500">
          Showing page {page + 1} of {Math.max(1, totalPages)} with {totalItems}{" "}
          total document{totalItems === 1 ? "" : "s"}.
        </p>
      </div>

      <Card className={brandStyles.brandCard}>
        <CardHeader>
          <CardTitle className={brandStyles.brandText}>
            Filter Documents
          </CardTitle>
          <CardDescription>
            Search and filter your document history
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 md:flex-row">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search documents (current page)..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className={`pl-10 ${brandStyles.brandBorder} ${brandStyles.page}`}
                />
              </div>
            </div>

            {/* Status filter dropdown — all / active / completed */}
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger
                className={`w-full md:w-48 ${brandStyles.brandBorder} ${brandStyles.page}`}
              >
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="active">Active</SelectItem>
                <SelectItem value="completed">Completed</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Document list */}
      <div className="space-y-4">
        {processedDocuments.length === 0 ? (
          // Empty state — shown when no documents match filters or none exist yet
          <Card className={`${brandStyles.brandCard} ${brandStyles.page}`}>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <FileText className="mb-4 h-12 w-12 text-gray-400" />
              <h3 className="mb-2 text-lg font-semibold text-gray-900">
                No documents found
              </h3>
              <p className="mb-4 text-center text-gray-600">
                {searchTerm || filterStatus !== "all"
                  ? "Try adjusting your search or filters"
                  : "You haven't processed any documents yet"}
              </p>
              <Button asChild className={brandStyles.brandButton}>
                <Link to="/scan">Scan Your First Document</Link>
              </Button>
            </CardContent>
          </Card>
        ) : (
          processedDocuments.map((doc) => (
            // Render one card per document
            <Card
              key={doc.docId}
              // Dim the list while a new page is loading in the background
              className={`border transition-all hover:shadow-md ${
                isPlaceholderData ? "opacity-50" : ""
              }`}
            >
              <CardContent className="p-6">
                <div className="mb-4 flex items-start justify-between">
                  <div className="flex flex-1 items-start space-x-4">
                    <div className="flex items-center space-x-2">
                      {getStatusIcon(doc.status)}
                    </div>
                    <div className="flex-1">
                      <h3 className="mb-1 text-lg font-semibold text-gray-900">
                        {doc.title}
                      </h3>
                      <div className="flex items-center space-x-4 text-sm text-gray-500">
                        <span>{doc.sender}</span>
                        <span>•</span>
                        <span>
                          {doc.receivedDate
                            ? doc.receivedDate.toLocaleDateString()
                            : "N/A"}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className={`${brandStyles.brandOutlineButton} hover:bg-opacity-10`}
                      onClick={() => handleViewDocument(doc.docId)}
                    >
                      <Eye className="mr-1 h-4 w-4" />
                      View
                    </Button>
                  </div>
                </div>

                <div className="space-y-3">
                  {doc.totalTasks > 0 && (
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium text-gray-700">
                        Progress: {doc.completedTasks}/{doc.totalTasks} tasks
                        completed
                      </span>
                      <Badge
                        variant={
                          doc.status === "completed" ? "secondary" : "default"
                        }
                        className={
                          doc.status === "completed"
                            ? `text-white ${brandStyles.brandBg}`
                            : "bg-orange-100 text-orange-800"
                        }
                      >
                        {doc.status}
                      </Badge>
                    </div>
                  )}
                  <Progress value={doc.progress} className="h-2" />
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
      {/* Pagination controls */}
      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          onClick={() => {
            if (page - 1 >= 0) setPage((current) => current - 1);
          }}
          disabled={page - 1 < 0}
          className={brandStyles.brandOutlineButton}
        >
          <ChevronLeft className="mr-1 h-4 w-4" />
          Previous
        </Button>
        <Button
          variant="outline"
          onClick={() => {
            // Prevent navigating past the last page or triggering
            // a new fetch while the previous one is still loading
            if (!isPlaceholderData && page + 1 < totalPages)
              setPage((current) => current + 1);
          }}
          disabled={isPlaceholderData || page + 1 >= totalPages}
          className={brandStyles.brandOutlineButton}
        >
          Next
          <ChevronRight className="ml-1 h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
