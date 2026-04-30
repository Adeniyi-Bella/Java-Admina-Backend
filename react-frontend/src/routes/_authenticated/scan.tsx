import ScanPage from '@/pages/ScanPage/ScanPage'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/scan')({
  component: ScanPage,
})


