# Frontend Web Client

Web interface for the AI Document & Knowledge Operations Platform, built with **React 19**, **TypeScript**, and **Vite**.

---

## Tech Stack

- **Framework**: React 19
- **Build Tool**: Vite
- **Language**: TypeScript
- **Code Quality**: ESLint

---

## Project Structure

```text
frontend/
├── public/                 # Static assets
├── src/
│   ├── assets/             # Images and SVG icons
│   ├── App.tsx             # Main application component
│   ├── App.css             # Application styles
│   ├── index.css           # Global stylesheets
│   └── main.tsx            # Application entry point
├── .env.example            # Local / default environment template
├── .env.development.example# Development environment template
├── .env.production.example # Production environment template
├── index.html              # HTML template
├── package.json            # Dependencies and scripts
├── tsconfig.json           # TypeScript configuration
└── vite.config.ts          # Vite configuration
```

---

## Environment Configuration

Copy the example environment file for local development:

```bash
cp .env.example .env
```

---

## Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Run Development Server
```bash
npm run dev
```

The development server starts at `http://localhost:5173`.

### 3. Build for Production
```bash
npm run build
```

The compiled static assets will be output to the `dist/` directory.

### 4. Preview Production Build
```bash
npm run preview
```

### 5. Linting
```bash
npm run lint
```


