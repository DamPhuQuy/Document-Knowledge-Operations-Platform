# Frontend Web Client (React + Vite)

Web interface for the **AI Document & Knowledge Operations Platform**, built with **React 19**, **TypeScript**, **Vite**, and **Nginx**.

---

## 1. Project Structure

```text
frontend/
├── .env.example                               # Local / baseline environment template
├── .env.dev.example / .env.development.example# Development environment template
├── .env.prod.example / .env.production.example# Production environment template
├── .env                                       # Active local environment configuration
├── Dockerfile                                 # Multi-stage Dockerfile (dev & prod targets)
├── Dockerfile.dev                             # Dedicated development Dockerfile (hot reload)
├── Dockerfile.prod                            # Dedicated production Dockerfile (Nginx + static build)
├── docker-compose.yaml                        # Standalone frontend container
├── docker-compose.dev.yaml                    # Full stack dev compose (source mounting + hot reload)
├── docker-compose.prod.yaml                   # Production compose stack (hardened Nginx)
├── nginx.conf                                 # Production Nginx reverse proxy & SPA routing config
├── package.json                               # Dependencies and multi-environment scripts
├── tsconfig.json                              # TypeScript project configuration
├── tsconfig.app.json                          # App TypeScript options
├── tsconfig.node.json                         # Node/Vite build config options
├── vite.config.ts                             # Vite configuration with proxy, ports, & modes
├── public/                                    # Static assets (favicons, icons)
└── src/
    ├── assets/                                # Images and SVG illustrations
    ├── config/                                # Centralized, type-safe environment configuration
    │   ├── env.ts                             # Environment parsing, defaults, & debug logger
    │   └── index.ts                           # Config barrel export
    ├── vite-env.d.ts                          # TypeScript interface declarations for import.meta.env
    ├── App.tsx                                # Main application root component
    ├── App.css                                # Component styles
    ├── index.css                              # Global styles & reset
    └── main.tsx                               # Application entry point
```

---

## 2. Multi-Environment Architecture

The frontend follows a 3-tier configuration model aligned with the platform backend:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Environment Files (.env / .env.development / .env.production)       │
│    -> Injected at build/runtime via Vite's `import.meta.env`           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 2. Type-Safe Config Module (`src/config/env.ts`)                       │
│    -> Strong types, fallback validation, environment-aware logger      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ 3. Containerization Strategy (Dockerfile / Docker Compose)             │
│    -> Dev: Node 22 Alpine + Vite HMR | Prod: Nginx Alpine + SPA routing│
└────────────────────────────────────────────────────────────────────────┘
```

### Environment Comparison Matrix

| Configuration Variable | Local (`.env.example`) | Dev (`.env.development.example`) | Prod (`.env.production.example`) | Purpose & Security |
| :--- | :--- | :--- | :--- | :--- |
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | `https://dev-api.docknowledge.company.com/api/v1` | `https://api.docknowledge.company.com/api/v1` | Backend API gateway / REST endpoint |
| `VITE_APP_TITLE` | `Document & Knowledge Operations Platform` | `Document & Knowledge Operations Platform (Dev)` | `Document & Knowledge Operations Platform` | Application title rendered in header/tab |
| `VITE_APP_ENV` | `development` | `development` | `production` | Active runtime tier badge (`dev`, `prod`, `test`) |
| `VITE_ENABLE_DEBUG` | `true` | `true` | `false` | Controls client-side verbose debug logging |
| `VITE_DEV_PORT` | `5173` | `5173` | N/A | Local Vite development server port |
| `VITE_PREVIEW_PORT` | `3000` | `3000` | N/A | Local Vite preview server port |

---

## 3. Local Development

### Prerequisites
- **Node.js 20+** (v22 recommended)
- **npm** (v10+)

### Setup Environment
```bash
# 1. Copy the appropriate environment template
cp .env.example .env

# Or for staging / dev cloud backend:
# cp .env.development.example .env.development
```

### Install Dependencies
```bash
npm install
```

### Development Commands

| Command | Action |
| :--- | :--- |
| `npm run dev` | Starts Vite dev server in **development** mode (`http://localhost:5173`) |
| `npm run dev:prod` | Runs Vite dev server simulating **production** mode |
| `npm run build` | Compiles TypeScript and builds default production bundle into `dist/` |
| `npm run build:dev` | Compiles with development mode settings (includes source maps) |
| `npm run build:prod` | Compiles optimized, minified production distribution |
| `npm run type-check` | Runs TypeScript compiler verification (`tsc -b --noEmit`) |
| `npm run lint` | Runs ESLint analysis across TypeScript & TSX source files |
| `npm run preview` | Serves the compiled `dist/` directory on `http://localhost:3000` |
| `npm run preview:dev` | Previews development bundle |
| `npm run preview:prod` | Previews production bundle |

---

## 4. Docker & Containerization

### A. Development Mode (Hot Reloading)

Runs the Node.js development server with file watching and HMR:

```bash
# Run standalone frontend dev container
docker compose -f docker-compose.dev.yaml up --build

# Or build via multi-stage target:
docker build --target dev -t frontend:dev .
docker run -p 5173:5173 -v $(pwd):/app -v /app/node_modules frontend:dev
```

### B. Production Mode (Hardened Nginx)

Builds the static bundle and serves it via an optimized, secure Nginx container with SPA routing (`try_files $uri $uri/ /index.html;`), gzip compression, and caching headers:

```bash
# Run production container stack
docker compose -f docker-compose.prod.yaml up --build -d

# Or build via multi-stage target with build arguments:
docker build \
  --target prod \
  --build-arg VITE_API_BASE_URL=https://api.docknowledge.company.com/api/v1 \
  --build-arg VITE_APP_TITLE="Document & Knowledge Operations Platform" \
  -t frontend:prod .

# Run production container (exposing on port 3000)
docker run -p 3000:80 frontend:prod
```

### Production Nginx Features
- **SPA Routing**: Fallback to `index.html` for client-side routing.
- **Cache Policy**: `no-cache` for HTML; `max-age=1y, immutable` for hashed `/assets/` bundles.
- **Security Headers**: `X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`, `Referrer-Policy`.
- **Health Probes**: Built-in `/health` endpoint returning `HTTP 200 OK`.
