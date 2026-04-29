import { type ReactNode } from "react";
import {
  Card,
  CardHeader,
  CardContent,
  CardTitle,
} from "@/components/common/Card/Card";
import { brandStyles } from "@/lib/design/styles";

interface StatCardProps {
  title: string;
  value: number | string;
  icon: ReactNode;
  textClassName?: string;
  iconClassName?: string;
  borderClassName?: string;
  description?: string;
  descriptionClassName?: string;
  // NEW: Prop to control blurring
  isBlurred?: boolean;
}

export function StatCard({
  title,
  value,
  icon,
  textClassName = brandStyles.brandText,
  iconClassName = brandStyles.brandText,
  borderClassName = brandStyles.brandBorder,
  description,
  descriptionClassName = brandStyles.brandSoftText,
  isBlurred = false,
}: StatCardProps) {
  return (
    <Card className={`${brandStyles.brandCard} ${borderClassName}`}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <div className={iconClassName}>{icon}</div>
      </CardHeader>
      <CardContent>
        <div
          className={`${textClassName} text-2xl font-bold ${isBlurred ? "blur-sm select-none" : ""}`}
        >
          {isBlurred && title !== "Documents Processed" ? "000" : value}
        </div>
        {description && (
          <p
            className={`${descriptionClassName} text-xs ${isBlurred ? "blur-sm select-none" : ""}`}
          >
            {isBlurred && title !== "Documents Processed"
              ? "Locked Feature"
              : description}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
