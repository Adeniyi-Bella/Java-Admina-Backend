import DashboardOverviewView from "./DashboardOverview";
import { useAuthenticateUser } from "@/hooks/api/user/useGetUser";

export default function DashboardPage() {
  const query = useAuthenticateUser();
  const { data: authResult } = query;

  const dashboardData = authResult?.data;

  const recentDocuments = (dashboardData?.documents ?? []).map((doc) => {
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

  const upcomingTasks = (dashboardData?.upcomingTasks ?? [])
    .filter((task) => task.dueDate)
    .map((task) => {
      const dueDate = new Date(task.dueDate as string);
      return {
        task: task.title ?? "Untitled task",
        date: dueDate,
        daysLeft: Math.ceil(
          (dueDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24),
        ),
        location: task.location ?? undefined,
      };
    })
    .filter((item) => item.daysLeft >= 0);

  return (
    <div className="space-y-4">
      <DashboardOverviewView
        documentsProcessed={dashboardData?.user.totalDocuments ?? 0}
        pendingActions={dashboardData?.user.pendingActions ?? 0}
        upcomingDeadlines={dashboardData?.user.upcomingDeadlines ?? 0}
        completedTasks={dashboardData?.user.completedTasks ?? 0}
        recentDocuments={recentDocuments}
        upcomingTasks={upcomingTasks.slice(0, 3)}
      />
    </div>
  );
}
