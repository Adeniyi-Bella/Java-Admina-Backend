import { AlertTriangle, CheckCircle, Clock, FileText } from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Button } from "@/components/common/Button/Button";
import { Progress } from "@/components/common/progress";
import { StatCard } from "@/components/common/stats-card";
import { brandStyles } from "@/lib/design/styles";
import { Link } from "@tanstack/react-router";

export type DashboardOverviewProps = {
  documentsProcessed: number;
  pendingActions: number;
  upcomingDeadlines: number;
  completedTasks: number;
  recentDocuments: Array<{
    docId: string;
    title?: string;
    sender?: string;
    actionPlans?: Array<{ completed: boolean }>;
    progress: number;
  }>;
  upcomingTasks: Array<{
    task: string;
    date: Date;
    daysLeft: number;
    location?: string;
  }>;
};

export default function DashboardOverview({
  documentsProcessed,
  pendingActions,
  upcomingDeadlines,
  completedTasks,
  recentDocuments,
  upcomingTasks,
}: DashboardOverviewProps) {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          {/* <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1> */}
          <p className="text-gray-600">
            Welcome back! Here's what you need to do.
          </p>
        </div>
        <Button asChild className={brandStyles.brandButton}>
          {/* <Link to="/dashboard/scan">Scan New Document</Link> */}
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Documents Processed"
          value={documentsProcessed}
          icon={<FileText className="h-4 w-4" />}
        />
        <StatCard
          title="Pending Actions"
          value={pendingActions}
          icon={<Clock className="h-4 w-4" />}
          textClassName={brandStyles.orangeText}
          iconClassName={brandStyles.orangeText}
          description="Requires attention"
          descriptionClassName={brandStyles.orangeText}
        />
        <StatCard
          title="Upcoming Deadlines"
          value={upcomingDeadlines}
          icon={<AlertTriangle className="h-4 w-4" />}
          textClassName={brandStyles.dangerText}
          iconClassName={brandStyles.dangerText}
          description="Next 30 days"
          descriptionClassName={brandStyles.dangerText}
        />
        <StatCard
          title="Completed Tasks"
          value={completedTasks}
          icon={<CheckCircle className="h-4 w-4" />}
        />
      </div>

      <div className="grid gap-8 lg:grid-cols-2">
        <Card className={brandStyles.brandCard}>
          <CardHeader>
            <CardTitle className={brandStyles.brandText}>
              Recent Documents
            </CardTitle>
            <CardDescription>Your recently processed documents</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentDocuments.length === 0 && (
                <p className="text-center text-gray-500">
                  No recent documents available.
                </p>
              )}
              {recentDocuments.map((doc) => (
                <div
                  key={doc.docId}
                  className={`flex cursor-pointer items-center justify-between gap-4 rounded-lg border p-4 transition-colors hover:bg-opacity-10 ${brandStyles.brandCard}`}
                >
                  <div className="flex items-center space-x-3">
                    <FileText className={`h-5 w-5 ${brandStyles.brandText}`} />
                    <div className="flex-1">
                      <p className="font-medium text-gray-900">{doc.title}</p>
                      <p className="text-sm text-gray-500">{doc.sender}</p>
                      <div className="mt-2">
                        {doc.actionPlans?.length === 0 ? (
                          <p className="text-sm italic text-gray-500">
                            No action plan for this document.
                          </p>
                        ) : (
                          <>
                            <div className="mb-1 flex items-center justify-between text-xs text-gray-500">
                              <span>Progress</span>
                              <span>{`${Math.round(doc.progress)}%`}</span>
                            </div>
                            <Progress value={doc.progress} className="h-2" />
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="flex flex-col items-end space-y-2">
                    <Button
                      size="sm"
                      asChild
                      className={brandStyles.brandButton}
                    >
                      <Link to="/document/$docId" params={{ docId: doc.docId }}>
                        View Document
                      </Link>
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className={brandStyles.brandCard}>
          <CardHeader>
            <CardTitle className={brandStyles.brandText}>
              Upcoming Tasks
            </CardTitle>
            <CardDescription>
              Action items that need your attention
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {upcomingTasks.length === 0 && (
                <p className="text-center text-gray-500">
                  No upcoming tasks to show.
                </p>
              )}
              {upcomingTasks.map((task) => (
                <div
                  key={`${task.task}-${task.date.toISOString()}`}
                  className={`rounded-lg border p-4 ${brandStyles.brandCard}`}
                >
                  <h4 className="font-medium text-gray-900">{task.task}</h4>
                  <p className="text-sm text-gray-500">{task.location}</p>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-sm text-gray-500">
                      {task.date.toLocaleDateString()}
                    </span>
                    <span className="text-sm font-medium text-orange-600">
                      {task.daysLeft} days left
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
