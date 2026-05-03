import { useEffect, useMemo } from "react";
import { useSearch } from "@tanstack/react-router";
import { useAuthenticateUser } from "@/hooks/api/user/useUser";
import { useToast } from "@/hooks/use-toast";
import DashboardOverviewView from "./DashboardOverview";

export default function DashboardPage() {
  const { toast } = useToast();
  const search = useSearch({ from: "/_authenticated/dashboard" }) as Record<
    string,
    string | undefined
  >;
  const { data: authResult } = useAuthenticateUser();
  const dashboardData = authResult?.data;

  // Show upgrade success toast when redirected from pricing after checkout.
  // Cleans the URL param afterwards so refreshing doesn't re-show it.
  useEffect(() => {
    if (search.toast !== "upgraded") return;

    // const planName = search.plan
    //   ? `${search.plan.charAt(0).toUpperCase()}${search.plan.slice(1).toLowerCase()}`
    //   : "a new";

    toast({
      title: "Subscription upgraded! 🎉",
      description: `You have been successfully upgraded!!!.`,
      variant: "success",
    });

    window.history.replaceState({}, "", "/dashboard");
  }, [search.toast, search.plan, toast]);

  // Derive recent documents from the latest 3 documents sorted by the API.
  // Progress is calculated as the ratio of completed to total tasks (0–100).
  const recentDocuments = useMemo(() => {
    const documents = dashboardData?.documents ?? [];
    if (!documents.length) return [];

    return documents.map((doc) => {
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
  }, [dashboardData?.documents]);

  // Derive upcoming tasks — filter out past tasks and limit to the next 3.
  // daysLeft is calculated from now so it stays accurate to the current render.
  const upcomingTasks = useMemo(() => {
    const tasks = dashboardData?.upcomingTasks ?? [];
    if (!tasks.length) return [];

    return tasks
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
      .filter((item) => item.daysLeft >= 0)
      .slice(0, 3);
  }, [dashboardData?.upcomingTasks]);

  return (
    <div className="space-y-4">
      <DashboardOverviewView
        documentsProcessed={dashboardData?.user.totalDocuments ?? 0}
        pendingActions={dashboardData?.user.pendingActions ?? 0}
        upcomingDeadlines={dashboardData?.user.upcomingDeadlines ?? 0}
        completedTasks={dashboardData?.user.completedTasks ?? 0}
        recentDocuments={recentDocuments}
        upcomingTasks={upcomingTasks}
      />
    </div>
  );
}
