"use client";

import { useState, useMemo, memo, useCallback } from "react";
import { Link, useRouterState } from "@tanstack/react-router";
import { Button } from "@/components/common/Button/Button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/common/dropdown-menu";
import {
  LayoutDashboard,
  FileText,
  History,
  Menu,
  X,
  LogOut,
  User,
  Settings,
} from "lucide-react";
import { brandStyles } from "@/lib/design/styles";
import { useMsalLoginLogoutSignup } from "@/hooks/auth/useMsalLoginLogoutSignup";

const NAV_ITEMS = [
  { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { name: "Scan Document", href: "/scan", icon: FileText },
  { name: "Document History", href: "/dashboard/history", icon: History },
  { name: "Settings", href: "/dashboard/settings", icon: Settings },
];

// ─── Desktop Sidebar ────────────────────────────────────────────────────────

const DesktopSidebar = memo(
  ({ pathname, onLogout }: { pathname: string; onLogout: () => void }) => {
    return (
      <div className="hidden md:flex md:w-64 md:flex-col">
        <div
          className={`flex flex-col grow pt-5 overflow-y-auto border-r border-gray-200 dark:border-gray-700 dark:bg-gray-800 ${brandStyles.surface}`}
        >
          {/* Logo */}
          <div className={`p-4 ${brandStyles.surface}`}>
            <div className="flex items-center">
              <img
                src="/images/admina-logo.png"
                alt="Admina Logo"
                className="h-10 w-auto object-contain"
              />
            </div>
          </div>

          {/* Nav */}
          <div className="flex-1 px-3 mt-4">
            <p
              className={`px-2 mb-2 text-xs font-semibold uppercase tracking-wider ${brandStyles.brandText}`}
            >
              Navigation
            </p>
            <nav className="space-y-1">
              {NAV_ITEMS.map((item) => {
                const isActive = pathname === item.href;
                const Icon = item.icon;
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    className={`group flex items-center px-2 py-2 text-sm font-medium rounded-md transition-colors ${
                      isActive
                        ? `text-white ${brandStyles.brandBg}`
                        : "text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white"
                    }`}
                  >
                    <Icon className="mr-3 h-5 w-5 shrink-0" />
                    {item.name}
                  </Link>
                );
              })}
            </nav>
          </div>

          {/* Footer */}
          <div
            className={`shrink-0 border-t border-gray-200 dark:border-gray-700 p-4 ${brandStyles.brandBorder}`}
          >
            <Button
              variant="outline"
              onClick={onLogout}
              className={`${brandStyles.brandOutlineButton} w-full justify-start hover:bg-opacity-10`}
            >
              <LogOut className="h-4 w-4 mr-2" />
              Sign Out
            </Button>
          </div>
        </div>
      </div>
    );
  },
);

DesktopSidebar.displayName = "DesktopSidebar";

// ─── Main Layout ─────────────────────────────────────────────────────────────

interface SidebarLayoutProps {
  children: React.ReactNode;
}

export default function SidebarLayout({ children }: SidebarLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { logout } = useMsalLoginLogoutSignup();
  //   const navigate = useNavigate();

  // TanStack Router — derive current pathname
  const routerState = useRouterState();
  const pathname = routerState.location.pathname;

  const handleLogout = useCallback(() => {
    logout();
  }, [logout]);

  const activeNavItemName = useMemo(() => {
    return (
      NAV_ITEMS.find((item) => item.href === pathname)?.name ?? "Dashboard"
    );
  }, [pathname]);

  return (
    <div className="flex h-screen bg-white dark:bg-gray-900">
      {/* ── Desktop sidebar ── */}
      <DesktopSidebar pathname={pathname} onLogout={handleLogout} />

      {/* ── Mobile sidebar overlay ── */}
      {sidebarOpen && (
        <div className="fixed inset-0 flex z-40 md:hidden">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-gray-600 bg-opacity-75"
            onClick={() => setSidebarOpen(false)}
          />

          {/* Drawer */}
          <div
            className={`relative flex-1 flex flex-col max-w-xs w-full dark:bg-gray-800 ${brandStyles.surface}`}
          >
            {/* Close button */}
            <div className="absolute top-0 right-0 pt-2 pr-2">
              <Button
                variant="ghost"
                size="icon"
                className="text-gray-600 dark:text-gray-300"
                onClick={() => setSidebarOpen(false)}
              >
                <X className="h-6 w-6" />
              </Button>
            </div>

            <div className="flex-1 h-0 pt-5 pb-4 overflow-y-auto">
              {/* Logo */}
              <div className="shrink-0 flex items-center px-4">
                <Link to="/dashboard" onClick={() => setSidebarOpen(false)}>
                  <img
                    src="/images/admina-logo.png"
                    alt="Admina Logo"
                    className="h-8 w-auto object-contain"
                  />
                </Link>
              </div>

              {/* Nav */}
              <nav className="mt-5 px-2 space-y-1">
                {NAV_ITEMS.map((item) => {
                  const isActive = pathname === item.href;
                  const Icon = item.icon;
                  return (
                    <Link
                      key={item.name}
                      to={item.href}
                      className={`group flex items-center px-2 py-2 text-sm font-medium rounded-md transition-colors ${
                        isActive
                          ? `text-white ${brandStyles.brandBg}`
                          : "text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white"
                      }`}
                      onClick={() => setSidebarOpen(false)}
                    >
                      <Icon className="mr-3 h-5 w-5 shrink-0" />
                      {item.name}
                    </Link>
                  );
                })}
              </nav>
            </div>

            {/* Footer */}
            <div
              className={`shrink-0 border-t border-gray-200 dark:border-gray-700 p-4 ${brandStyles.brandBorder}`}
            >
              <Button
                variant="outline"
                onClick={handleLogout}
                className={`${brandStyles.brandOutlineButton} w-full justify-start hover:bg-opacity-10`}
              >
                <LogOut className="h-4 w-4 mr-2" />
                Sign Out
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* ── Right column: header + main ── */}
      <div className="flex flex-col w-0 flex-1 overflow-hidden">
        {/* Mobile hamburger */}
        <div className="md:hidden pl-1 pt-1 sm:pl-3 sm:pt-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="h-6 w-6" />
          </Button>
        </div>

        {/* Header */}
        <header className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between px-4 py-4 sm:px-6">
            <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">
              {activeNavItemName}
            </h1>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="icon">
                  <User className="h-5 w-5" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>My Account</DropdownMenuLabel>
                <DropdownMenuSeparator />
                {/* <DropdownMenuItem
                  onClick={() => navigate({ to: "/dashboard/settings" })}
                > */}
                <Settings className="mr-2 h-4 w-4" />
                Settings
                {/* </DropdownMenuItem> */}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout}>
                  <LogOut className="mr-2 h-4 w-4" />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 relative overflow-y-auto focus:outline-none">
          <div className="py-6">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 md:px-8">
              {children}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
