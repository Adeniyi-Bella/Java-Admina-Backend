import BlogPage from '@/pages/BlogPage/BlogPage'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/blog')({
  component: BlogPage,
})

