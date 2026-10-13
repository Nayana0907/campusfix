# CampusFix — React Frontend Portal Foundation

Smart Campus Complaint & Maintenance Management System frontend built with React 19, Vite, React Router 7, Axios, and Lucide React.

---

## 1. Architecture & Tech Stack

- **Framework**: React 19 (Functional components, custom hooks, React Context)
- **Build Tool**: Vite 8 (Ultra-fast HMR and optimized production bundling)
- **Routing**: React Router 7 (`BrowserRouter`, nested layouts, role-guarded routes)
- **Styling**: Vanilla CSS Design Tokens (Custom properties, dark mode support via `[data-theme="dark"]`, zero bloated frameworks)
- **Icons**: Lucide React
- **API Client**: Centralized Axios with Bearer token injection, error normalization, and 401 session expiration handling
- **Testing**: Vitest + JSDOM + `@testing-library/react` (19 automated unit tests covering API services, interceptors, UI components, and route guards)
- **Linting**: Oxlint

---

## 2. Design Tokens & Visual Hierarchy

Located in `src/index.css`:

| Token | Hex / Value | Description |
|---|---|---|
| `--color-primary` | `#7C3AED` | Primary Purple (brand, buttons, active states) |
| `--color-primary-light` | `#EDE9FE` | Light Lavender (highlights, active nav, badges) |
| `--color-primary-hover-bg` | `#F5F3FF` | Soft Lavender Hover Surface |
| `--color-secondary` | `#A78BFA` | Accent Purple |
| `--color-bg-main` | `#F9FAFB` | Clean Main Page Background |
| `--color-bg-card` | `#FFFFFF` | Card & Form Surface |
| `--color-text-primary` | `#1F2937` | Dark Charcoal Primary Text |
| `--color-text-secondary` | `#6B7280` | Supporting Slate Secondary Text |
| `--color-border` | `#E5E7EB` | Subtle Structural Border |
| `--color-success` | `#059669` | Emerald / Resolution Success State |
| `--color-warning` | `#D97706` | Amber / Warning & In-Progress State |
| `--color-danger` | `#DC2626` | Crimson / Urgent & SLA Alert State |
| `--font-family` | `'Inter', sans-serif` | Clean Modern SaaS Typography |

---

## 3. Directory Structure

```
frontend/
├── public/                 # Static assets (favicons, manifest)
├── src/
│   ├── api/
│   │   ├── axios.js        # Centralized Axios instance with JWT interceptor & 401 handler
│   │   └── authService.js  # Real backend auth integrations (/login, /register, /me, /logout)
│   ├── context/
│   │   ├── AuthContext.jsx # Auth session, role mapping, token persistence, mount validation
│   │   ├── ThemeContext.jsx# Light/dark mode toggle and system preference persistence
│   │   └── ToastContext.jsx# Accessible toast notification system
│   ├── components/
│   │   ├── common/         # Button, Input, Card, StatusBadge, Modal, Table, Loader, Skeleton, EmptyState
│   │   └── layout/         # AppLayout, Sidebar (collapsible & drawer), TopNav (branding & controls)
│   ├── pages/
│   │   ├── auth/           # LoginPage, RegisterPage
│   │   ├── dashboard/      # Role-tailored DashboardPage
│   │   ├── complaints/     # ComplaintsPage (submission & list foundation)
│   │   ├── staff/          # AssignedWorkPage (maintenance staff orders)
│   │   ├── admin/          # AnalyticsPage (admin metrics foundation)
│   │   ├── profile/        # ProfilePage (user details & security guidelines)
│   │   └── errors/         # UnauthorizedPage (403), NotFoundPage (404)
│   ├── routes/
│   │   ├── ProtectedRoute.jsx # Role-aware route guard & PublicOnly guard
│   │   └── AppRoutes.jsx      # Route configuration
│   ├── __tests__/          # Vitest automated test suites
│   ├── App.jsx             # Provider root
│   ├── main.jsx            # Application entry point
│   └── index.css           # Design tokens, responsive reset, layout rules
├── .env                    # Active local environment variables
├── .env.example            # Environment template
└── vite.config.js          # Vite & Vitest configuration
```

---

## 4. Backend Authentication Integration

The frontend connects directly to the Spring Boot REST backend:

| Action | Endpoint | HTTP Method | Request Body / Headers | Response |
|---|---|---|---|---|
| **Login** | `/api/auth/login` | `POST` | `{ "email": "...", "password": "..." }` | `{ "accessToken": "...", "user": { ... } }` |
| **Register** | `/api/auth/register` | `POST` | `{ "name": "...", "email": "...", "password": "...", "phone": "..." }` | User entity with default `STUDENT` role |
| **Profile** | `/api/auth/me` | `GET` | `Authorization: Bearer <token>` | User profile details |
| **Refresh** | `/api/auth/refresh` | `POST` | `{ "refreshToken": "..." }` (or cookie) | Refreshed JWT token |
| **Logout** | `/api/auth/logout` | `POST` | Bearer token / credentials | Session invalidation confirmation |

### Supported Roles
1. **`STUDENT`**: Self-registers via `/register`. Reports complaints, views personal submissions, verifies repairs.
2. **`MAINTENANCE_STAFF`**: Provisioned by Admin. Views assigned work orders, updates status to `IN_PROGRESS`, uploads proof of completion.
3. **`ADMIN`**: Facility supervisor. Assigns complaints, manages staff allocation, views SLA & performance analytics.

---

## 5. Setup & Running Instructions

### Prerequisites
- Node.js 18+ (tested on Node 20+)
- Java 17+ and Maven (for the Spring Boot backend)

### Step 1: Configure Environment
Copy `.env.example` to `.env`:
```bash
VITE_API_BASE_URL=http://localhost:8080
```

### Step 2: Install Dependencies
```bash
cd frontend
npm install
```

### Step 3: Run the Development Server
```bash
npm run dev
```
The application will launch at `http://localhost:5173`.

### Step 4: Run Automated Tests
```bash
npm run test
```

### Step 5: Run Production Build & Linting
```bash
npm run build
npm run lint
```

---

## 6. Troubleshooting & CORS Notes

- **CORS Configuration**: The Spring Boot backend (`UserConfig.java`) is preconfigured to permit origins `http://localhost:5173` and `http://localhost:3000` with `allowCredentials(true)`.
- **Backend Offline**: If the backend is not running, the login screen displays a clear notification, and the dashboard status badge indicates `Offline` with recovery suggestions.
