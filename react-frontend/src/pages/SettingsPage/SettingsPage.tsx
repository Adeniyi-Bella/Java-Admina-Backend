import { useState } from "react";
import { AlertTriangle, Trash2, User } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { useDeleteUserAccount, useGetUser } from "@/hooks/api/user/useUser";
import { AppError } from "@/api/error/customeError";
import { logger } from "@/lib/logger";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/common/Card/Card";
import { Input } from "@/components/common/input";
import { Label } from "@/components/common/label";
import { Button } from "@/components/common/Button/Button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/common/dialog";
import { brandStyles } from "@/lib/design/styles";
import { DashboardLoader } from "@/components/common/Loader/Loader";
import { useMsal } from "@azure/msal-react";
import { AUTH_SESSION_INVALIDATE_EVENT } from "@/types/constants";

export default function SettingsPage() {
  const { instance } = useMsal();
  const activeAccount = instance.getActiveAccount();
  const { toast } = useToast();
  const { data: userDetails, isLoading } = useGetUser();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState("");

  const user = userDetails?.data.user;
  const displayName = activeAccount?.name ?? user?.username ?? "";
  const displayEmail = user?.email ?? activeAccount?.username ?? "";
  const displayPlan = user?.plan
    ? user.plan.charAt(0).toUpperCase() + user.plan.slice(1).toLowerCase()
    : "";

  const { deleteUser, isDeleting } = useDeleteUserAccount({
    onError: (error: AppError) => {
      toast({
        title: "Action Failed",
        description: error.userMessage,
        variant: "destructive",
        duration: 20000,
      });
      logger.error(error, "Delete Account Error");
    },
    onSuccess: () => {
      window.dispatchEvent(new CustomEvent(AUTH_SESSION_INVALIDATE_EVENT));
    },
  });

  const handleDeleteAccount = () => {
    if (deleteConfirmation !== "delete") {
      return;
    }
    deleteUser();
  };

  const handleCloseDeleteDialog = () => {
    setShowDeleteDialog(false);
    setDeleteConfirmation("");
  };

  const handleOpenDeleteDialog = () => {
    setDeleteConfirmation("");
    setShowDeleteDialog(true);
  };

  if (isLoading) {
    return <DashboardLoader />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-600">
          Manage your account settings and preferences
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card className={brandStyles.brandCard}>
            <CardHeader>
              <CardTitle
                className={`flex items-center space-x-2 ${brandStyles.brandText}`}
              >
                <User className="h-5 w-5" />
                <span>Profile Information</span>
              </CardTitle>
              <CardDescription>
                Profile details are managed via your login provider.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-4">
                <h4 className="font-semibold text-gray-800">
                  Personal Information
                </h4>
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="name">Full Name</Label>
                    <Input
                      disabled
                      id="name"
                      value={displayName}
                      className={`${brandStyles.brandBorder} ${brandStyles.page}`}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input
                      disabled
                      id="email"
                      value={displayEmail}
                      className={`${brandStyles.brandBorder} ${brandStyles.page}`}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="plan">Subscription Plan</Label>
                    <Input
                      disabled
                      id="plan"
                      value={displayPlan}
                      className={`font-medium ${brandStyles.orangeText} ${brandStyles.orangeSoftBg} ${brandStyles.brandBorder}`}
                    />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className={brandStyles.brandCard}>
            <CardHeader>
              <CardTitle
                className={`flex items-center space-x-2 ${brandStyles.brandText}`}
              >
                <AlertTriangle className={`h-5 w-5 ${brandStyles.brandText}`} />
                <span>Danger Zone</span>
              </CardTitle>
              <CardDescription>
                Irreversible actions that will permanently affect your account
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <h4 className="mb-2 font-semibold text-gray-800">
                    Delete Account
                  </h4>
                  <p className="mb-4 text-sm text-gray-700">
                    Once you delete your account, there is no going back. Please
                    be certain. All your documents, settings, and data will be
                    permanently removed. You can re-register with the same email
                    after 30 days if you change your mind.
                  </p>
                  <Button
                    onClick={handleOpenDeleteDialog}
                    className={brandStyles.dangerButton}
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    Delete Account
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Dialog
        open={showDeleteDialog}
        onOpenChange={(open) => !open && handleCloseDeleteDialog()}
      >
        <DialogContent className={`sm:max-w-md ${brandStyles.brandCard}`}>
          <DialogHeader>
            <DialogTitle
              className={`flex items-center space-x-2 ${brandStyles.dangerText}`}
            >
              <AlertTriangle className="h-5 w-5" />
              <span>Delete Account</span>
            </DialogTitle>
            <DialogDescription>
              <strong>This action cannot be undone.</strong> This will
              permanently delete your account and remove all your data from our
              servers.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 text-sm text-muted-foreground">
            <p>
              All your processed documents, settings, and account information
              will be lost forever.
            </p>
            <p>
              To confirm deletion, please type <strong>delete</strong> in the
              field below:
            </p>
          </div>

          <div className="space-y-4">
            <Input
              placeholder="Type delete to confirm"
              value={deleteConfirmation}
              onChange={(e) => setDeleteConfirmation(e.target.value)}
              className={brandStyles.dangerBorder}
              disabled={isDeleting}
            />
          </div>

          <DialogDescription className="sr-only">
            Type delete to confirm account deletion
          </DialogDescription>

          <div className="flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={handleCloseDeleteDialog}
              disabled={isDeleting}
              className={brandStyles.brandOutlineButton}
            >
              Cancel
            </Button>
            <Button
              onClick={handleDeleteAccount}
              disabled={deleteConfirmation !== "delete" || isDeleting}
              className={brandStyles.dangerButton}
            >
              <Trash2 className="mr-2 h-4 w-4" />
              {isDeleting ? "Deleting..." : "Delete Account"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
