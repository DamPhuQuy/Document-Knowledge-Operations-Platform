# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Build Stage using Node.js Alpine
# ------------------------------------------------------------------------------
FROM node:22-alpine AS builder
WORKDIR /app

# Copy package manifests and install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source files and build production bundle
COPY . ./
RUN npm run build

# ------------------------------------------------------------------------------
# Production Serving Stage using Nginx Alpine
# ------------------------------------------------------------------------------
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy build output from builder stage
COPY --from=builder /app/dist ./

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
