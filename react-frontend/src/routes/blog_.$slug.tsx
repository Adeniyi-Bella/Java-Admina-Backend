import BlogPostPage from '@/pages/BlogPage/[slug]/BlogPostPage'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/blog_/$slug')({
  component: BlogPostPage,
})