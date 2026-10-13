# CampusFix — Spring Boot Backend

The enterprise backend service for **CampusFix: Smart Campus Complaint & Maintenance Management System**, built with Java 17, Spring Boot 4.x, Spring Security 6, Spring Data JPA, Hibernate, JWT authentication, and MySQL.

---

## 1. Project Overview & Architecture

- **Base Package**: `com.anurag.campusfix`
- **Application Entry Point**: `com.anurag.campusfix.CampusFixBackendApplication`
- **Default Port**: `8080` (Configurable via `PORT`)
- **Database**: MySQL 8.x (`campusfix`)
- **Authentication**: Stateless Bearer JWT + Refresh Token Rotation
- **Authorization**: Role-Based (`STUDENT`, `ADMIN`, `MAINTENANCE_STAFF`)
- **API Base**: `/api`

---

## 2. Prerequisites

- **Java JDK**: Version 17 or higher (`java -version`)
- **MySQL Database**: Version 8.0+ running on port `3306`
- **Maven**: Maven 3.9+ (or use the included Maven Wrapper `mvnw` / `mvnw.cmd`)
- **IDE**: Spring Tool Suite (STS 4) or IntelliJ IDEA

---

## 3. Database Setup (MySQL)

1. Ensure MySQL is running on `localhost:3306`:
   ```sql
   CREATE DATABASE IF NOT EXISTS campusfix CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. The application utilizes Hibernate automatic schema management (`spring.jpa.hibernate.ddl-auto=update`), generating and updating required tables (`users`, `complaints`, `complaint_history`, `complaint_images`, `complaint_verifications`, `feedback`, `notifications`, `refresh_tokens`) on startup.

---

## 4. Configuration & Environment Variables

The backend reads configuration settings from environment variables or falls back to standard local defaults specified in `src/main/resources/application.properties`.

Create a `.env` file in `backend/` or set environment variables in your system / IDE run configuration:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | HTTP port for the Spring Boot server |
| `DB_URL` | `jdbc:mysql://localhost:3306/campusfix` | JDBC connection URL for MySQL |
| `DB_USERNAME` | `root` | MySQL database username |
| `DB_PASSWORD` | `123@123` | MySQL database password |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | 256-bit cryptographic signing secret |
| `JWT_EXPIRATION_MS` | `900000` (15 mins) | Access token validity period in milliseconds |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 days) | Refresh token validity period in milliseconds |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Whitelisted frontend origins |
| `COMPLAINT_UPLOAD_DIR` | `uploads/complaints` | Directory for complaint image attachments |

---

## 5. Importing into Spring Tool Suite (STS)

Follow these exact steps to import and run the backend independently in STS:

1. **Launch Spring Tool Suite (STS)**.
2. In the top menu bar, select **`File`** → **`Import...`**.
3. In the import dialog, expand **`Maven`** and choose **`Existing Maven Projects`**. Click **`Next >`**.
4. Click **`Browse...`** and navigate to your `CampusFix/backend/` folder (the directory containing `pom.xml`).
5. Verify that `/pom.xml com.anurag.campusfix:CampusFix-backend` is checked in the Projects list.
6. Click **`Finish`**.
7. **Wait for Maven dependency resolution** and indexing to complete in the background (check the progress bar in the bottom-right corner of STS).
8. **Run the Application**:
   - Right-click the imported `CampusFix-backend` project in Project Explorer.
   - Select **`Run As`** → **`Spring Boot App`** (or open `src/main/java/com/anurag/campusfix/CampusFixBackendApplication.java` and click the green Run button).
9. Check the **Console** tab to verify startup:
   ```text
   Started CampusFixBackendApplication in X.XXX seconds
   Tomcat started on port 8080 (http) with context path '/'
   ```

---

## 6. Running via Command Line (Maven Wrapper)

You can also run the backend directly from terminal without opening STS:

```bash
# Navigate to backend folder
cd backend

# On Windows PowerShell / Command Prompt:
.\mvnw.cmd spring-boot:run

# On Linux / macOS:
./mvnw spring-boot:run
```

To run the automated test suite (all 126 unit & integration tests):
```bash
.\mvnw.cmd test
```

---

## 7. Pre-Configured Test Accounts

| Role | Email | Password |
| :--- | :--- | :--- |
| **Admin** | `admin@campusfix.edu` | `Admin@123` |
| **Admin** | `24eg107b13@anurag.edu.in` | *(Your registered password)* |
| **Maintenance Staff** | `staff_dev@campusfix.edu` | `Admin@123` |
| **Student** | Register at `http://localhost:5173/register` | *(Self-registration)* |

---

## 8. REST API Endpoints Overview

- **Authentication**: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/refresh`, `POST /api/auth/logout`
- **Complaints (Student / All)**: `GET /api/complaints`, `POST /api/complaints`, `GET /api/complaints/{id}`, `GET /api/complaints/mine`, `POST /api/complaints/{id}/images`
- **Staff Work Orders**: `GET /api/staff/complaints`, `PATCH /api/staff/complaints/{id}/accept`, `PATCH /api/staff/complaints/{id}/start`, `PATCH /api/staff/complaints/{id}/resolve`
- **Verification Cycle**: `POST /api/complaints/{id}/resolution`, `POST /api/complaints/{id}/verification`, `GET /api/complaints/{id}/verification/cycles`
- **Admin Management & SLA**: `GET /api/admin/complaints`, `PATCH /api/admin/complaints/{id}/assign`, `GET /api/admin/analytics/summary`, `GET /api/admin/analytics/monthly`, `GET /api/admin/analytics/categories`, `GET /api/admin/analytics/resolution-performance`, `GET /api/admin/analytics/locations`, `GET /api/admin/analytics/staff-performance`
