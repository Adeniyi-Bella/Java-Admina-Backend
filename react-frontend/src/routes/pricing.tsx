import PricingPage from '@/pages/PricingPage/PricingPage'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/pricing')({
  component: PricingPage,
})
