import HistoryPage from '@/pages/DocumentHistoryPage/HistoryPage'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/history')({
  component: HistoryPage,
})
