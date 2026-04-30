

import React, { useEffect, useRef, useState } from "react";
import { Upload } from "lucide-react";
import { Button } from "@/components/common/Button/Button";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/common/Card/Card";
import { brandStyles } from "@/lib/design/styles";

interface DocumentUploadProps {
  setFile: (file: File | null) => void;
  userMetadata: number;
  clearSignal?: boolean;
}

function DocumentUpload({
  setFile,
  userMetadata,
  clearSignal,
}: DocumentUploadProps) {
  const [dragActive, setDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  }, [clearSignal]);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(true);
  };

  const handleDragLeave = () => setDragActive(false);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.match(/(pdf|image\/jpeg|image\/png)/)) {
      setFile(file);
    } else {
      alert("Only PDF, JPG, and PNG files are supported.");
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  return (
    <div className="space-y-2">
      <Card className={brandStyles.brandCard}>
        <CardHeader>
          <CardTitle className={brandStyles.brandText}>Select Document</CardTitle>
          <CardDescription>
            Upload one PDF, JPG, or PNG file of your document
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div
            className={`border-2 border-dashed rounded-lg p-8 text-center transition-all ${
              dragActive
                ? "bg-orange-50 border-orange-400"
                : "hover:border-opacity-80"
            } ${brandStyles.brandBorder}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <Upload className={`h-12 w-12 mx-auto mb-4 ${brandStyles.brandSoftText}`} />
            <p className="text-lg font-medium text-gray-900 mb-2">
              Drop a file here or click to browse
            </p>
            <p className="text-sm text-gray-500 mb-4">
              Supports PDF, JPG, PNG up to 10MB (one file only)
            </p>
            {userMetadata > 0 ? (
              <CardDescription>
                Only {userMetadata} uploads left for this month
              </CardDescription>
            ) : (
              <CardDescription>
                No more uploads left for the month. Wait for the upgrade to be
                available, or contact support directly or wait until next month.
                Contact us directly at{" "}
                <a
                  href="mailto:support@admina-app.com"
                  className="text-blue-600 underline"
                >
                  support@admina-app.com
                </a>{" "}
                for assistance.
              </CardDescription>
            )}

            {userMetadata > 0 && (
              <>
                <input
                  ref={inputRef}
                  type="file"
                  accept=".pdf,.jpg,.jpeg,.png"
                  onChange={handleFileChange}
                  className="hidden"
                  id="file-upload"
                />

                <Button
                  disabled={userMetadata <= 0}
                  className={`mt-2.5 ${brandStyles.brandButton}`}
                >
                  <label htmlFor="file-upload" className="cursor-pointer">
                    Choose File
                  </label>
                </Button>
              </>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default React.memo(DocumentUpload);
