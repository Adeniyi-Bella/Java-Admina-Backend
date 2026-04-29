import { useMemo } from "react";
import DashboardOverviewView from "./DashboardOverview";
import { LoadingSpinner } from "@/components/common/loading-spinner";
import { Card } from "@/components/common/Card/Card";
import { brandStyles } from "@/lib/design/styles";
import { useAuthenticateUser } from "@/hooks/api/user/useGetUser";

function isDateWithinDays(date: Date, days: number) {
  const now = new Date();
  const future = new Date();
  future.setDate(now.getDate() + days);
  return date >= now && date <= future;
}

export default function DashboardPage() {
  const { data: authResult, isLoading } = useAuthenticateUser();
  const dashboardData = authResult?.data;

  const derived = useMemo(() => {
    const documents = dashboardData?.documents ?? [];

    let pendingActions = 0;
    let upcomingDeadlines = 0;
    let completedTasks = 0;

    documents.forEach((doc) => {
      doc.actionPlanTasks?.forEach((action) => {
        if (action.completed) {
          completedTasks++;
          return;
        }

        pendingActions++;
        if (action.dueDate && isDateWithinDays(new Date(action.dueDate), 30)) {
          upcomingDeadlines++;
        }
      });
    });

    const recentDocuments = documents
      .slice()
      .sort(
        (a, b) =>
          new Date(b.receivedDate ?? 0).getTime() -
          new Date(a.receivedDate ?? 0).getTime(),
      )
      .slice(0, 3)
      .map((doc) => {
        const totalTasks = doc.actionPlanTasks?.length ?? 0;
        const doneTasks =
          doc.actionPlanTasks?.filter((item) => item.completed).length ?? 0;
        return {
          docId: doc.id,
          title: doc.title,
          sender: doc.sender,
          actionPlans: doc.actionPlanTasks,
          progress: totalTasks > 0 ? (doneTasks / totalTasks) * 100 : 0,
        };
      });

    const upcomingTasks = documents
      .flatMap((doc) =>
        (doc.actionPlanTasks ?? [])
          .filter((ap) => !ap.completed && ap.dueDate)
          .map((ap) => {
            const dueDate = new Date(ap.dueDate as string);
            return {
              task: ap.title ?? "Untitled task",
              date: dueDate,
              daysLeft: Math.ceil(
                (dueDate.getTime() - new Date().getTime()) /
                  (1000 * 60 * 60 * 24),
              ),
              location: ap.location ?? undefined,
            };
          }),
      )
      .filter((item) => item.daysLeft >= 0)
      .sort((a, b) => a.date.getTime() - b.date.getTime())
      .slice(0, 3);

    return {
      documentsProcessed: documents.length,
      pendingActions,
      upcomingDeadlines,
      completedTasks,
      recentDocuments,
      upcomingTasks,
    };
  }, [dashboardData]);

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {authResult?.message && (
        <Card className={`${brandStyles.brandCard} border-emerald-200 bg-emerald-50 px-4 py-3 text-emerald-800`}>
          <p className="text-sm font-medium">{authResult.message}</p>
        </Card>
      )}
      <DashboardOverviewView {...derived} />
    </div>
  );
}
