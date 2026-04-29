# Admina Frontend

The Admina frontend is a React single-page application that serves both the public marketing pages and the authenticated dashboard.

## What It Contains

- public home page
- pricing page
- blog pages
- authenticated dashboard
- Microsoft Entra login and logout flow
- API integration for user authentication and dashboard data
- toast, loading, and error UI

## Tech Stack

- React 19
- TypeScript
- Vite
- TanStack Router
- TanStack Query
- Axios
- MSAL Browser
- MSAL React
- React Hook Form
- Zod
- Zustand
- Radix UI components
- Tailwind CSS
- Motion
- Lucide React
- Sentry React

## Key Libraries And Why They Are Used

- `@tanstack/react-router`: file-based routing and route guards
- `@tanstack/react-query`: cached server state, loading and error states
- `axios`: HTTP client for the backend API
- `@azure/msal-browser` and `@azure/msal-react`: Microsoft Entra authentication
- `zod`: runtime validation for config and form schemas
- `react-hook-form`: form state management
- `zustand`: lightweight client state
- `@radix-ui/*`: accessible UI primitives for buttons, cards, switches, progress, toast, and labels
- `motion`: animation helpers
- `lucide-react`: icons
- `@sentry/react`: frontend monitoring

## Folder Overview

- `src/api/`: API client, auth header helpers, response types, and error mapping
- `src/hooks/`: React hooks for auth and API queries
- `src/pages/`: page-level screens such as home, pricing, blog, and dashboard
- `src/components/`: shared UI components
- `src/lib/`: config, router setup, logging, styling helpers, and utility functions
- `src/routes/`: TanStack Router route definitions
- `src/styles/`: global styles

## API Shape

The frontend mirrors the backend response pattern around `CustomApiResponse`:

- success responses carry `success`, `data`, and `timestamp`
- error responses carry `success: false`, `data: null`, and `error` with `status` and `message`

The dashboard currently uses `/api/v1/users/authenticate` and displays the returned auth result there.

## Development

```bash
pnpm install
pnpm dev
```

## Scripts

- `pnpm dev`: run the Vite dev server
- `pnpm build`: type-check and build for production
- `pnpm preview`: preview the production build
- `pnpm lint`: run ESLint
- `pnpm test`: run Vitest
- `pnpm test:e2e`: open Cypress
- `pnpm test:e2e:headless`: run Cypress headless

## Environment Variables

The app expects the following Vite variables:

- `VITE_API_URL`
- `VITE_AZURE_CLIENT_ID`
- `VITE_AZURE_AUTHORITY`
- `VITE_AZURE_REDIRECT_URI`
- `VITE_AZURE_LOGOUT_REDIRECT_URI`
- `VITE_ADMINA_API_CLIENT_ID`
- `VITE_SENTRY_DSN` optional

## Docker

The frontend is designed to be built and served as a static SPA inside Docker.
Its Docker image reads the Vite environment variables from `react-frontend/.env` at build time.
From the repository root you can run the entire stack with:

```bash
docker compose up --build
```
