import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { useIsAuthenticated, useMsal } from '@azure/msal-react'
import { InteractionStatus } from '@azure/msal-browser'
import { useEffect } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { DashboardLoader } from '@/components/common/Loader/Loader'
import SidebarLayout from '@/pages/Sidebar/Sidebar'
import type { RouterContext } from '@/lib/router'
import { useAuthenticateUser } from '@/hooks/api/user/useGetUser'

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: ({ context }) => {
    const auth = (context as RouterContext).auth
    if (!auth.isLoading && !auth.isAuthenticated) {
      throw redirect({ to: '/' })
    }
  },
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  const { inProgress } = useMsal()
  const isAuthenticated = useIsAuthenticated()
  const isLoading = inProgress !== InteractionStatus.None
  const { isLoading: isUserLoading } = useAuthenticateUser()
  const navigate = useNavigate()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      navigate({ to: '/' })
    }
  }, [isLoading, isAuthenticated, navigate])

  if (isLoading || isUserLoading) {
    return <DashboardLoader />
  }

  return (
    <SidebarLayout>
      <Outlet />
    </SidebarLayout>
  )
}
