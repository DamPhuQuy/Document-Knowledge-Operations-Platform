# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Stage 1: Base Stage (Node 22 on Alpine & NPM Dependencies Caching)
# ------------------------------------------------------------------------------
FROM node:22-alpine AS base

WORKDIR /app

# Copy package manifests first for optimal Docker layer caching
COPY package.json package-lock.json ./

# Install exact dependencies
RUN npm ci

# ------------------------------------------------------------------------------
# Target: Development (docker build --target dev -t frontend:dev .)
# ------------------------------------------------------------------------------
FROM base AS dev

# Copy remaining source files (can also be mounted via docker-compose volume)
COPY . ./

EXPOSE 5173

ENV NODE_ENV=development

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]

# ------------------------------------------------------------------------------
# Stage: Builder (Compiles TypeScript & Vite Static Bundle)
# ------------------------------------------------------------------------------
FROM base AS builder

ARG VITE_API_BASE_URL
ARG VITE_APP_TITLE
ARG VITE_APP_ENV=production
ARG VITE_ENABLE_DEBUG=false
ARG VITE_APP_VERSION=1.0.0

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_APP_TITLE=${VITE_APP_TITLE} \
    VITE_APP_ENV=${VITE_APP_ENV} \
    VITE_ENABLE_DEBUG=${VITE_ENABLE_DEBUG} \
    VITE_APP_VERSION=${VITE_APP_VERSION} \
    NODE_ENV=production

COPY . ./

# Build production distribution
RUN npm run build:prod

# ------------------------------------------------------------------------------
# Target: Production (Default: docker build --target prod -t frontend:prod .)
# ------------------------------------------------------------------------------
FROM nginx:alpine AS prod

WORKDIR /usr/share/nginx/html

# Install curl/wget for container health checking
RUN apk add --no-cache curl

# Remove default nginx static assets and configuration
RUN rm -rf ./* /etc/nginx/conf.d/default.conf

# Copy custom Nginx configuration with SPA routing and gzip
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets from builder stage
COPY --from=builder /app/dist ./

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
