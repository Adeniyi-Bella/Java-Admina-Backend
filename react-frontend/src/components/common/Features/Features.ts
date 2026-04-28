import {
  FileText,
  Globe,
  Clock,
  CheckCircle,
  Upload,
  Bot,
  type LucideIcon,
} from "lucide-react";

export interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

export const features: Feature[] = [
  {
    icon: Upload,
    title: "User uploads a Document",
    description:
      "Users uploads official letters, notices, or bureaucratic documents in PDF or image format for processing.",
  },
  {
    icon: FileText,
    title: "Text Extraction",
    description:
      "We extract readable and accurate text from scanned or photographed documents.",
  },
  {
    icon: Globe,
    title: "Smart Translation",
    description: "Extracted texts are translated to your native tongue. ",
  },
  {
    icon: Clock,
    title: "Deadline Extraction",
    description:
      "Critical dates and deadlines are automatically identified and clearly displayed to ensure timely responses.",
  },
  {
    icon: CheckCircle,
    title: "Document Summary",
    description:
      "We summarize the purpose and required actions from the letter, making it simple to understand what's next.",
  },
  {
    icon: Bot,
    title: "Chat Bot",
    description:
      "If anything is still unclear, you can ask our chatbot how to respond or get further clarification.",
  },
];
