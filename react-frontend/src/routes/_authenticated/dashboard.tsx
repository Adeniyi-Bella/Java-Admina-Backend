import { createFileRoute } from '@tanstack/react-router'
import DashboardPage from '@/pages/DashboardPage/DashboardPage'
import SidebarLayout from '@/pages/Sidebar/Sidebar'

export const Route = createFileRoute('/_authenticated/dashboard')({
  component: DashboardRoute,
})

function DashboardRoute() {
  return (
    <SidebarLayout>
      <DashboardPage />
    </SidebarLayout>
  )
}