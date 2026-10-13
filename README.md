# CampusFix — Smart Campus Complaint & Maintenance Management System

**CampusFix** is a full-stack, enterprise-grade campus maintenance and complaint management platform organized into two completely separate projects:

- **Frontend Portal** (`frontend/`): React 19 single-page application built with Vite, React Router, Axios, and modern Purple & Lavender SaaS design tokens.
- **Backend Service** (`backend/`): Spring Boot REST API built with Java 17, Spring Security, JWT authentication, Hibernate JPA, and MySQL.

---

## 1. Project Directory Structure

```text
CampusFix/
├── backend/                  # Independent Spring Boot Maven project (importable into STS)
│   ├── pom.xml               # Maven configuration & dependencies
│   ├── mvnw & mvnw.cmd       # Maven Wrapper executables
│   ├── .mvn/                 # Maven Wrapper configuration
│   ├── .gitignore            # Backend-specific ignore rules
│   ├── .env.example          # Backend environment variable template
│   ├── README.md             # Backend setup & STS import instructions
│   ├── uploads/              # Local uploaded complaint attachments
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/anurag/campusfix/  # Controllers, Services, Entities, DTOs, Security, JWT
│       │   └── resources/
│       │       └── application.properties # Spring Boot configuration
│       └── test/             # JUnit 5 & MockMvc integration tests (126 tests)
│
├── frontend/                 # Independent React single-page application
│   ├── package.json          # Node dependencies & build scripts
│   ├── package-lock.json     # Locked dependency graph
│   ├── index.html            # Single page entry HTML
│   ├── vite.config.js        # Vite 8 bundling & dev server configuration
│   ├── .env.example          # Frontend environment variable template
│   ├── README.md             # Frontend setup & development instructions
│   ├── public/               # Static assets & icons
│   └── src/
│       ├── api/              # Axios instance & service clients (configured via VITE_API_BASE_URL)
│       ├── context/          # AuthContext, ThemeContext, ToastContext
│       ├── components/       # UI components & responsive SaaS layout
│       ├── pages/            # Role-tailored views (Student, Admin, Staff dashboards)
│       └── routes/           # Protected routes & role-based route dispatching
│
├── docs/                     # Additional architectural & API documentation
├── .gitignore                # Workspace git ignore
└── README.md                 # Root workspace documentation
```

---

## 2. Communication Between Frontend and Backend

The frontend and backend run as independent processes and communicate exclusively via HTTP REST APIs with JSON payloads:

- **Local Backend URL**: `http://localhost:8080`
- **Local Frontend URL**: `http://localhost:5173`
- **Configurable Endpoint Base**: The frontend communicates with the backend via the configurable environment variable `VITE_API_BASE_URL` defined in `frontend/.env`.
  ```bash
  VITE_API_BASE_URL=http://localhost:8080
  ```
- **CORS Protection**: The backend (`backend/src/main/resources/application.properties`) whitelists the frontend origins:
  ```properties
  app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}
  ```
- **Security & Session Tokens**:
  - Stateless Bearer JWT tokens (`Authorization: Bearer <token>`) are injected by Axios for all authenticated requests.
  - Refresh tokens are handled via both secure cookies and response bodies for seamless token rotation.

---

## 3. Quick Start Guide

### Step 1: Start MySQL Database
Ensure MySQL is running on `localhost:3306`:
```sql
CREATE DATABASE IF NOT EXISTS campusfix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 2: Run the Backend (Spring Boot)

#### Option A: Running from Spring Tool Suite (STS)
1. Open **Spring Tool Suite (STS)**.
2. Select **File** → **Import...**.
3. Select **Maven** → **Existing Maven Projects** and click **Next**.
4. Browse to the **`CampusFix/backend/`** folder (containing `pom.xml`).
5. Select the project and click **Finish**.
6. Wait for Maven dependency resolution to complete.
7. Right-click the project → **Run As** → **Spring Boot App** (or run `CampusFixBackendApplication.java`).

#### Option B: Running from Terminal
```bash
cd backend
.\mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run         # Linux / macOS
```
Backend will be live at `http://localhost:8080`.

### Step 3: Run the Frontend (React / Vite)
Open a new terminal window:
```bash
cd frontend
npm install
npm run dev
```
Frontend will be live at `http://localhost:5173`.

---

## 4. Default Test Credentials

| Role | Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@campusfix.edu` | `Admin@123` | Full administrative control, technician allocation, SLA intelligence & analytics |
| **Administrator** | `24eg107b13@anurag.edu.in` | *(Your registered password)* | Full administrator rights |
| **Maintenance Staff** | `staff_dev@campusfix.edu` | `Admin@123` | Assigned work order queue, status transitions, repair photo verification |
| **Student** | Register at `http://localhost:5173/register` | *(Self-registration)* | Issue submission, personal tracking, resolution verification/reopen |

---

## 5. Verification & Testing

Both projects can be tested independently:

```bash
# 1. Run all backend tests (126 tests)
cd backend
.\mvnw.cmd test

# 2. Run all frontend tests (51 tests)
cd frontend
npm test

# 3. Test production build
cd frontend
npm run build
```

---

## 6. Production Docker Deployment (One-Command Launch)

The entire full-stack application (MySQL database, Spring Boot API, and Nginx React frontend) can be deployed in a single command using Docker Compose:

### 1. Launch All Services
```bash
# From the CampusFix root directory:
docker compose up --build -d
```

### 2. Verify Running Containers
```bash
docker compose ps
```
Services started:
- **`campusfix-frontend`**: Live on port `80` (`http://localhost`) with Nginx serving static assets and proxying `/api/` to the backend.
- **`campusfix-backend`**: Live on port `8080` (`http://localhost:8080/api/health`).
- **`campusfix-mysql`**: Live on port `3306` with persistent volume `mysql_data`.

### 3. View Logs & Stop
```bash
# Stream live logs across all containers:
docker compose logs -f

# Stop and gracefully shut down all services:
docker compose down
```

