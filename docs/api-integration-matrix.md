# CampusFix — API Integration Matrix & Contract Inventory

This document provides a comprehensive inventory and verification record of all API endpoints across the **CampusFix** Spring Boot backend (`com.anurag.campusfix`) and the React.js frontend (`frontend/`).

---

## 1. Authentication & User Management Endpoints (`/api/auth`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/auth/register` | `POST` | Public | None | `RegisterRequest`: `{ name, email, password, phone }` | `201/200`: `UserResponse` (`id`, `name`, `email`, `role: STUDENT`, `phone`, `isActive`, `createdAt`) | `authService.register` / `RegisterPage.jsx` | Connected & Verified | Tested live with MySQL: creates user with encrypted password and default `STUDENT` role. |
| `/api/auth/login` | `POST` | Public | None | `LoginRequest`: `{ email, password }` | `200`: `AuthResponse` (`accessToken`, `expiresIn`, `refreshToken`, `tokenType: 'Bearer'`, `user: {...}`) | `authService.login` / `LoginPage.jsx` | Connected & Verified | Tested live: returns valid JWT tokens and user payload. |
| `/api/auth/me` | `GET` | Authenticated | Any active user | Bearer token in `Authorization` header | `200`: `UserResponse` (`id`, `name`, `email`, `role`, `isActive`) | `authService.getCurrentUser` / `AuthContext.jsx` | Connected & Verified | Tested live: returns authenticated profile. Session restoration on page refresh verified. |
| `/api/auth/refresh` | `POST` | Authenticated / Public | Any active user | `RefreshTokenRequest`: `{ refreshToken }` | `200`: `AuthResponse` with refreshed access token | `authService.refreshToken` / `axios.js` | Connected & Verified | Covered by unit tests and integration tests. |
| `/api/auth/logout` | `POST` | Authenticated | Any active user | Bearer token in header | `200`: `{ success: true, message }` | `authService.logout` / `Navbar.jsx` | Connected & Verified | Clears tokens from storage and resets state. |

---

## 2. Complaint Management Endpoints (`/api/complaints`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/complaints` | `POST` | Authenticated | `STUDENT` | Multipart: `@RequestPart("complaint")` JSON + `@RequestPart("images")` files (max 5) | `201`: `ComplaintResponse` | `complaintService.createComplaint` / `SubmitComplaintPage.jsx` | Connected & Verified | Tested live with multipart upload and sample PNG image. Stored in MySQL and file system. |
| `/api/complaints/mine` | `GET` | Authenticated | `STUDENT` | Query: `search`, `category`, `priority`, `status`, `page`, `size`, `sortBy`, `direction` | `200`: `PagedResponse<ComplaintResponse>` | `complaintService.getMyComplaints` / `MyComplaintsPage.jsx` | Connected & Verified | Tested live: fetches student personal complaints with pagination and sorting. |
| `/api/complaints/{id}` | `GET` | Authenticated | Owner or Staff or Admin | Path: `id` (Long) | `200`: `ComplaintResponse` | `complaintService.getComplaintById` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: fetches complaint details by numeric ID. Ownership verified. |
| `/api/complaints/ref/{ref}` | `GET` | Authenticated | Owner or Staff or Admin | Path: `referenceNumber` (String) | `200`: `ComplaintResponse` | `complaintService.getComplaintByRef` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: fetches complaint by reference number (e.g. `CMP-20261013-QCB8DB`). |
| `/api/complaints/{id}/history` | `GET` | Authenticated | Owner or Staff or Admin | Path: `id` (Long) | `200`: `List<ComplaintHistoryResponse>` | `complaintService.getComplaintHistory` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: returns chronological transition audit trail with actor IDs and names. |
| `/api/complaints/{id}/images` | `POST` | Authenticated | `STUDENT` (Owner) | Multipart: `@RequestParam("images") MultipartFile[]` | `200`: `ComplaintResponse` | `complaintService.addComplaintImages` | Connected & Verified | Tested live: rejects closed complaints; successfully appends images to active complaints. |
| `/api/complaints/{id}/images/{imageId}` | `GET` | Authenticated | Owner or Staff or Admin | Path: `id`, `imageId` | `200`: Resource (`image/png`, `image/jpeg`, etc.) | `resolveImageUrl` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: returns HTTP 200 with inline image headers and binary data. |

---

## 3. Maintenance Staff Work Orders (`/api/staff/complaints`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/staff/complaints` | `GET` | Authenticated | `MAINTENANCE_STAFF` | Query: `search`, `category`, `priority`, `status`, `page`, `size`, `sortBy`, `direction` | `200`: `PagedResponse<ComplaintResponse>` | `staffService.getAssignedWorkOrders` / `StaffWorkOrdersPage.jsx` | Connected & Verified | Tested live: lists assigned work orders for logged-in maintenance staff member. |
| `/api/staff/complaints/{id}/accept` | `PATCH` | Authenticated | `MAINTENANCE_STAFF` | Path: `id` | `200`: `ComplaintResponse` (status $\rightarrow$ `ACCEPTED`) | `staffService.acceptWorkOrder` / `StaffWorkOrdersPage.jsx` | Connected & Verified | Tested live: transitions status from `ASSIGNED` to `ACCEPTED`. |
| `/api/staff/complaints/{id}/start` | `PATCH` | Authenticated | `MAINTENANCE_STAFF` | Path: `id` | `200`: `ComplaintResponse` (status $\rightarrow$ `IN_PROGRESS`) | `staffService.startWorkOrder` / `StaffWorkOrdersPage.jsx` | Connected & Verified | Tested live: transitions status from `ACCEPTED` / `REOPENED` to `IN_PROGRESS`. |
| `/api/staff/complaints/{id}/resolve` | `PATCH` | Authenticated | `MAINTENANCE_STAFF` | Path: `id`, Body: `{ resolutionNotes }` | `200`: `ComplaintResponse` (status $\rightarrow$ `RESOLVED`, `verificationStatus: PENDING`) | `staffService.resolveWorkOrder` / `StaffWorkOrdersPage.jsx` | Connected & Verified | Tested live: sets resolution notes and marks complaint resolved awaiting verification. |

---

## 4. Resolution Verification System (`/api/complaints/{id}`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/complaints/{id}/resolution` | `POST` | Authenticated | Assigned Staff / Admin | Multipart or JSON: `resolutionNotes`, optional `afterImage` file | `200`: `ResolutionResponse` | `verificationService.submitResolution` | Connected & Verified | Tested live with multipart upload and after-repair photo. Stored in DB and file system. |
| `/api/complaints/{id}/verification` | `POST` | Authenticated | `STUDENT` (Owner) | JSON: `VerificationRequest` (`decision`: `VERIFIED` \| `REJECTED`, `comment`) | `200`: `VerificationResponse` | `verificationService.verifyResolution` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: `VERIFIED` marks complaint `CLOSED`. `REJECTED` transitions complaint to `REOPENED`. |
| `/api/complaints/{id}/verification` | `GET` | Authenticated | Owner, Staff, Admin | Path: `complaintId` | `200`: `VerificationResponse` | `verificationService.getCurrentVerification` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: returns current active verification cycle state. |
| `/api/complaints/{id}/verification-history` | `GET` | Authenticated | Owner, Staff, Admin | Path: `complaintId` | `200`: `List<VerificationHistoryResponse>` | `verificationService.getVerificationHistory` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: returns multi-cycle verification history (Cycle 1, Cycle 2). |
| `/api/complaints/{id}/verification/after-image` | `GET` | Authenticated | Owner, Staff, Admin | Path: `complaintId` | `200`: Resource (`image/png`, etc.) | `resolveImageUrl` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: streams after-repair photo inline with HTTP 200. |
| `/api/complaints/{id}/reopen` | `POST` / `PATCH` | Authenticated | Owner, Admin | JSON: `ComplaintReopenRequest` (`reason`) | `200`: `ComplaintResponse` (status $\rightarrow$ `REOPENED`) | `verificationService.reopenComplaint` / `ComplaintDetailPage.jsx` | Connected & Verified | Tested live: reopens complaint and resets verification status to `REJECTED`. |

---

## 5. Admin Complaint Management (`/api/admin/complaints`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/admin/complaints` | `GET` | Authenticated | `ADMIN` | Query: `search`, `category`, `priority`, `status`, `building`, `floor`, `room`, `reporterId`, `staffId`, `page`, `size`, `sortBy`, `direction` | `200`: `PagedResponse<ComplaintResponse>` | `adminService.getAllComplaints` / `AdminComplaintsPage.jsx` | Connected & Verified | Tested live: fetches campus-wide complaints with multi-criteria filtering. |
| `/api/admin/complaints/{id}/assignment` | `PATCH` | Authenticated | `ADMIN` | Path: `id`, Body: `{ staffId }` | `200`: `ComplaintResponse` | `adminService.assignStaff` / `AdminComplaintsPage.jsx` | Connected & Verified | Tested live: assigns complaint to maintenance staff member. Generates notification and history log. |
| `/api/admin/complaints/{id}/priority` | `PATCH` | Authenticated | `ADMIN` | Path: `id`, Body: `{ priority }` | `200`: `ComplaintResponse` | `adminService.updatePriority` / `AdminComplaintsPage.jsx` | Connected & Verified | Tested: updates priority and logs audit history. |
| `/api/admin/complaints/{id}/close` | `PATCH` | Authenticated | `ADMIN` | Path: `id` | `200`: `ComplaintResponse` | `adminService.closeComplaint` / `AdminComplaintsPage.jsx` | Connected & Verified | Tested: closes complaint administratively. |
| `/api/admin/complaints/{id}/reopen` | `PATCH` | Authenticated | `ADMIN` | Path: `id`, Body: `{ reason }` | `200`: `ComplaintResponse` | `adminService.reopenComplaint` / `AdminComplaintsPage.jsx` | Connected & Verified | Tested: reopens complaint administratively with audit reason. |
| `/api/admin/complaints/{id}/verification/override` | `POST` | Authenticated | `ADMIN` | Path: `id`, Body: `{ verificationStatus, adminOverrideReason }` | `200`: `VerificationResponse` | `adminService.overrideVerification` / `AdminComplaintsPage.jsx` | Connected & Verified | Tested via backend automated integration suite (`ResolutionVerificationIntegrationTest`). |

---

## 6. Admin Analytics Dashboard (`/api/admin/analytics`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/admin/analytics/summary` | `GET` | Authenticated | `ADMIN` | Query: `from`, `to` (ISO dates) | `200`: `SummaryAnalyticsResponse` | `adminService.getSummary` / `AnalyticsDashboardPage.jsx` | Connected & Verified | Tested live: returns live count of submitted, assigned, in-progress, resolved, closed, and overdue complaints. |
| `/api/admin/analytics/monthly` | `GET` | Authenticated | `ADMIN` | Query: `from`, `to` | `200`: `MonthlyTrendsResponse` | `adminService.getMonthlyTrends` / `AnalyticsDashboardPage.jsx` | Connected & Verified | Tested live: returns monthly breakdown array for trend charts. |
| `/api/admin/analytics/categories` | `GET` | Authenticated | `ADMIN` | Query: `from`, `to` | `200`: `CategoryPriorityAnalyticsResponse` | `adminService.getCategoriesAndPriorities` / `AnalyticsDashboardPage.jsx` | Connected & Verified | Tested live: returns category and priority distribution derived from database. |
| `/api/admin/analytics/locations` | `GET` | Authenticated | `ADMIN` | Query: `from`, `to`, `building`, `category`, `priority`, `page`, `size`, `sort` | `200`: `LocationAnalyticsResponse` | `adminService.getLocationAnalytics` / `AnalyticsDashboardPage.jsx` | Connected & Verified | Tested live: hotspot and building breakdown. |
| `/api/admin/analytics/resolution-performance` | `GET` | Authenticated | `ADMIN` | Query: `from`, `to` | `200`: `ResolutionPerformanceResponse` | `adminService.getResolutionPerformance` / `AnalyticsDashboardPage.jsx` | Connected & Verified | Tested live: resolution durations, verification turnaround, and SLA metrics. |
| `/api/admin/analytics/staff-performance` | `GET` | Authenticated | `ADMIN` | Query: `from`, `to`, `staffId`, `page`, `size`, `sort` | `200`: `StaffPerformanceResponse` | `adminService.getStaffPerformance` / `AnalyticsDashboardPage.jsx` | Connected & Verified | Tested live: staff workload, completed count, and resolution speed. |

---

## 7. System Health Endpoint (`/api/health`)

| Endpoint | Method | Auth | Role(s) | Request Body / Parameters | Response Body & Status | Frontend Caller | Integration State | Verified Test Evidence |
|---|---|---|---|---|---|---|---|---|
| `/api/health` | `GET` | Public | None | None | `200` (or `503` degraded): `HealthResponse` (`success`, `status`, `database`, `service`, `timestamp`) | System monitoring / integration check | Connected & Verified | Tested live: returns HTTP 200 with `{ success: true, status: "healthy", database: "connected" }`. |

---

## 8. Backend Architectural Gaps & Non-Implemented Surfaces

1. **In-App Notification Feed (`/api/notifications`)**:
   - The backend includes an asynchronous email/event notification service (`DefaultNotificationService`) that logs notification dispatches (e.g., `STAFF_ASSIGNED`, `VERIFICATION_PENDING`, `VERIFICATION_CONFIRMED`). However, there is no in-app notification database entity or REST endpoint (`/api/notifications`). The frontend UI gracefully displays transactional toast notices and does not invoke non-existent notification endpoints.
2. **Post-Closure Feedback & Star Ratings (`/api/feedback`)**:
   - In `StaffPerformanceResponse`, the field `feedbackStatus` is populated with `"UNAVAILABLE"` to explicitly reflect that feedback collection is planned for future extension and not yet modeled in the JPA schema. The frontend respects this contract without fabricating rating numbers.
