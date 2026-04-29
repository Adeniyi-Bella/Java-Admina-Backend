import { Link } from "@tanstack/react-router";
import { useMsalLoginLogoutSignup } from "@/hooks/auth/useMsalLoginLogoutSignup";
import { brandStyles } from "@/lib/design/styles";

type FooterLink = {
  href: string;
  label: string;
};

const links: FooterLink[] = [
  { href: "/blog", label: "Blog" },
  { href: "/privacy", label: "Privacy Policy" },
  { href: "/terms", label: "Terms of Service" },
  { href: "/impressum", label: "Impressum" },
];

export default function Footer() {
  const { signUp } = useMsalLoginLogoutSignup();
  return (
    <>
      {/* Footer */}
      <footer
        className={`border-t py-16 ${brandStyles.surface} ${brandStyles.brandStrongBorder}`}
      >
        <div className="container mx-auto px-4">
          <div className="grid md:grid-cols-4 gap-8">
            {/* Brand Section */}
            <div className="md:col-span-2">
              <div className="flex items-center space-x-3 mb-6">
                <img
                  src="/images/admina-logo.png"
                  alt="Admina Logo"
                  width={120}
                  height={32}
                  className="h-8"
                />
              </div>
              <p className="text-gray-600 mb-6 italic text-lg">
                Letter In. Clarity Out.
              </p>
              <p className="text-gray-500 leading-relaxed">
                Making government bureaucracy accessible to everyone through AI
                powered document analysis.
              </p>
            </div>

            {/* Navigation Links */}
            <div>
              <h4 className="font-semibold text-gray-900 mb-6 text-lg">
                Product
              </h4>
              <ul className="space-y-3">
                <li>
                  <button
                    onClick={signUp}
                    className="text-gray-600 hover:text-gray-900 transition-colors focus:outline-none"
                  >
                    Get Started
                  </button>
                </li>
              </ul>
            </div>

            {/* Support Links */}
            <div>
              <h4 className="font-semibold text-gray-900 mb-6 text-lg">
                Support
              </h4>
              <ul className="space-y-3">
                {links.map((link, idx) => (
                  <li key={`${link.href}-${idx}`}>
                    <Link
                      to={link.href}
                      className="text-gray-600 hover:text-gray-900 transition-colors"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Bottom Section */}
          <div
            className={`border-t mt-12 pt-8 flex flex-col md:flex-row justify-between items-center ${brandStyles.brandStrongBorder}`}
          >
            <p className="text-gray-500">© 2025 Admina. All rights reserved.</p>

            {/* Social Media Icons */}
            <div className="flex items-center space-x-6 mt-4 md:mt-0">
              <a
                href="https://www.instagram.com/adminaapp/"
                className="text-gray-400 hover:text-gray-600 transition-colors"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Follow us on Instagram"
              >
                {/* Instagram SVG */}
                <svg
                  className="h-8 w-8"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path d="M7.75 2C5.12665 2 3 4.12665 3 6.75V17.25C3 19.8734 5.12665 22 7.75 22H16.25C18.8734 22 21 19.8734 21 17.25V6.75C21 4.12665 18.8734 2 16.25 2H7.75ZM5 6.75C5 5.23122 6.23122 4 7.75 4H16.25C17.7688 4 19 5.23122 19 6.75V17.25C19 18.7688 17.7688 20 16.25 20H7.75C6.23122 20 5 18.7688 5 17.25V6.75ZM12 7C9.51472 7 7.5 9.01472 7.5 11.5C7.5 13.9853 9.51472 16 12 16C14.4853 16 16.5 13.9853 16.5 11.5C16.5 9.01472 14.4853 7 12 7ZM9.5 11.5C9.5 10.1193 10.6193 9 12 9C13.3807 9 14.5 10.1193 14.5 11.5C14.5 12.8807 13.3807 14 12 14C10.6193 14 9.5 12.8807 9.5 11.5ZM17 6.75C16.3096 6.75 15.75 7.30964 15.75 8C15.75 8.69036 16.3096 9.25 17 9.25C17.6904 9.25 18.25 8.69036 18.25 8C18.25 7.30964 17.6904 6.75 17 6.75Z" />
                </svg>
              </a>
              <a
                href="https://www.linkedin.com/company/adminaapp/"
                className="text-gray-400 hover:text-gray-600 transition-colors"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Follow us on Linkedin"
              >
                {/* LinkedIn SVG */}
                <svg
                  className="h-6 w-6"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
                </svg>
              </a>
            </div>
          </div>
        </div>
      </footer>
    </>
  );
}
