import { ArrowLeft } from "lucide-react";
import { brandStyles } from "@/lib/design/styles";
import { Button } from "../Button/Button";
import { Link } from "@tanstack/react-router";

export default function Header({
  text,
  backRoute,
}: {
  text: string;
  backRoute: string;
}) {
  return (
    <header className={`border-b ${brandStyles.surface}`}>
      <div className="container mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Link to="/" aria-label="Home">
            <img
              src="/images/admina-logo.png"
              alt="Admina Logo"
              width={120}
              height={40}
              className="h-8 cursor-pointer"
            />
          </Link>
          <nav className="hidden sm:flex items-center space-x-4">
            <Link to="/" className="text-gray-600 hover:text-gray-900">
              Home
            </Link>
            <Link to="/pricing" className="text-gray-600 hover:text-gray-900">
              Pricing
            </Link>
            {/* <Link to="/blog" className="text-gray-600 hover:text-gray-900">
              Blog
            </Link> */}
          </nav>
        </div>
        <Button
          variant="outline"
          asChild
          className={brandStyles.brandOutlineButton}
        >
          <Link to={backRoute}>
            <ArrowLeft className="mr-2 h-4 w-4" />
            {text}
          </Link>
        </Button>
      </div>
    </header>
  );
}
