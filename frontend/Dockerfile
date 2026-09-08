# syntax=docker/dockerfile:1

# Base: Node 22 with cached npm dependencies
FROM node:22-alpine AS base
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Dev: Vite dev server with HMR
FROM base AS dev
COPY . ./
EXPOSE 5173
ENV NODE_ENV=development
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]

# Builder: Build static SPA bundle
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
RUN npm run build:prod

# Prod: Nginx reverse proxy serving static assets
FROM nginx:alpine AS prod
WORKDIR /usr/share/nginx/html
RUN apk add --no-cache curl && rm -rf ./* /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist ./

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
