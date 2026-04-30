

import { useState, useRef } from "react";
import { Card, CardHeader, CardTitle } from "@/components/common/Card/Card";
import { Button } from "@/components/common/Button/Button";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { brandStyles } from "@/lib/design/styles";

interface DocumentTextSectionProps {
  structuredTranslatedText?: Record<string, string>;
}

export default function DocumentTextSection({
  structuredTranslatedText,
}: DocumentTextSectionProps) {
  const [currentPage, setCurrentPage] = useState(0);
  const translationRef = useRef<HTMLDivElement>(null);

  if (
    !structuredTranslatedText ||
    Object.keys(structuredTranslatedText).length === 0
  ) {
    return (
      <Card className={brandStyles.brandCard}>
        <CardHeader>
          <CardTitle className={brandStyles.brandText}>
            No translated text available.
          </CardTitle>
        </CardHeader>
      </Card>
    );
  }

  const pages = Object.values(structuredTranslatedText);
  const totalPages = pages.length;

  const goPrev = () => setCurrentPage((prev) => Math.max(prev - 1, 0));
  const goNext = () =>
    setCurrentPage((prev) => Math.min(prev + 1, totalPages - 1));

  return (
    <Card className={brandStyles.brandCard}>
      <CardHeader>
        <div className="flex justify-between items-center w-full">
          <CardTitle className={brandStyles.brandText}>
            Translation (Page {currentPage + 1} of {totalPages})
          </CardTitle>
          <div className="flex space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={goPrev}
              disabled={currentPage === 0}
              className={brandStyles.brandOutlineButton}
            >
              <ArrowLeft className="h-4 w-4 mr-1" />
              Prev
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={goNext}
              disabled={currentPage === totalPages - 1}
              className={brandStyles.brandOutlineButton}
            >
              Next
              <ArrowRight className="h-4 w-4 ml-1" />
            </Button>
          </div>
        </div>
      </CardHeader>

      <div ref={translationRef}>
        <div className="p-6">
          <div
            className={`bg-gray-50 p-6 rounded-lg border text-sm text-gray-800 font-sans leading-relaxed prose ${brandStyles.brandBorder}`}
            dangerouslySetInnerHTML={{ __html: pages[currentPage] }}
          />
        </div>

        <div
          style={{
            position: "absolute",
            left: "-9999px",
            width: "100%",
          }}
        >
          {pages.map((htmlContent, index) => (
            <div
              key={index}
              className="pdf-page-content"
              style={{ padding: "50px" }}
              dangerouslySetInnerHTML={{ __html: htmlContent }}
            />
          ))}
        </div>
      </div>
    </Card>
  );
}
