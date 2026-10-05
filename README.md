# HireFlow – Smart Placement & Hiring Platform

HireFlow is a production-grade, full-stack placement management platform designed to streamline and automate campus recruitment coordination. It replaces manual, fragmented communication (such as WhatsApp/email threads) with deterministic eligibility enforcement, real-time application tracking, automated notification pipelines, and AI-assisted candidate evaluation.

---

## 🏗️ Architecture & Tech Stack

```
                                  ┌───────────────────────┐
                                  │   React 18 Frontend   │
                                  │  (Vite + TS + Tailwind│
                                  │   TanStack Query +    │
                                  │    Lucide Icons)      │
                                  └───────────┬───────────┘
                                              │  HTTPS / REST / JWT
                                              ▼
                                  ┌───────────────────────┐
                                  │  Spring Boot Backend  │
                                  │   (Java 17 / Spring   │
                                  │   Security 6 + JWT)   │
                                  └───┬───────────────┬───┘
                                      │               │
                     Deterministic    │               │ Advisory Match
                     Eligibility      ▼               ▼ / JD Analysis
                   ┌────────────────────┐   ┌────────────────────┐
                   │  Aiven Cloud MySQL │   │ AI Advisory Engine │
                   │  (Authoritative DB │   │ (OpenAI/Gemini/    │
                   │   with SSL/TLS)    │   │  Rule-based Fallbk)│
                   └────────────────────┘   └────────────────────┘
```

- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, TanStack Query (v5), React Router (v6), React Hook Form + Zod, Recharts, Lucide React, react-hot-toast.
- **Backend**: Java 17, Spring Boot 3.3.4, Spring Security 6 (Stateless JWT + Refresh Token Rotation), Spring Data JPA (Hibernate 6), Bean Validation, Flyway DB Migrations, Lombok, Springdoc OpenAPI (Swagger UI).
- **Database**: **Aiven Cloud MySQL 8** (Authoritative production database connected via SSL/TLS).
- **AI Advisory**: OpenAI / Gemini compatible API with structured JSON output and graceful rule-based fallback. *Deterministic eligibility remains authoritative; AI scores are advisory only.*
- **Containerization**: Multi-stage Dockerfiles for Spring Boot (Alpine JRE) and React (Nginx Alpine) + Docker Compose orchestration.

---

## 🚀 Key Features

### 👨‍🎓 Student Features
- **Authentication & Security**: Secure registration, login, and silent JWT token refresh with automatic token rotation and reuse detection.
- **Dynamic Profile Management**: Profile details (branch, CGPA, graduation year, active backlogs, multi-tag skills) and secure PDF resume upload (size-checked & MIME-validated).
- **Profile Completeness Barrier**: Real-time completeness meter; students cannot apply to drives until required fields and resume are complete.
- **Drive Discovery & Instant Diagnosis**: Search drives with filters (branch, package, CGPA, deadline, eligibility filter). View clear reasons for ineligibility (e.g., *"CGPA 7.1 is below required 7.5"*).
- **Application Lifecycle**: 1-click apply, round-by-round progress timeline stepper, and option to withdraw prior to Round 1 start.
- **Notification Inbox**: Real-time notifications for drive publication, round status transitions, deadline alerts, and broadcast messages.

### 🛡️ Placement Coordinator / Admin Features
- **Placement Dashboard**: Live KPI cards (total students, active drives, total applications, selected candidates, selection rate) and visual recruitment funnel chart.
- **Drive Management**: Create, edit, and soft-delete placement drives with multi-round configuration, allowed branches, and required skill tags.
- **AI Job Description Assistant**: Auto-fill drive criteria by pasting company job descriptions directly into the creation modal.
- **Applicant Management & State Machine**: Filter applicants by status/drive, view student resumes, perform single/bulk round promotions, and record transition audit trails.
- **AI Candidate Match Advisor**: Advisory match scores and explanations for eligible candidates, ranking candidates while maintaining deterministic eligibility authority.
- **Audience-Targeted Notifications**: Preview recipient count and broadcast announcements to all students, drive-eligible candidates, or drive applicants.
- **Student Directory & CSV Export**: Search students by branch/skills and export clean CSV reports for college records.

---

## ⚙️ Environment Configuration

Copy `.env.example` to `.env` in the project root:

```bash
# Aiven Cloud MySQL Credentials
DB_URL=jdbc:mysql://<aiven-host>:<port>/<dbname>?sslMode=REQUIRED
DB_USERNAME=<aiven-username>
DB_PASSWORD=<aiven-password>

# JWT Secret & Expirations
JWT_SECRET=your-256-bit-secret-key-must-be-very-secure-and-long-enough-32-chars
JWT_EXPIRATION_MS=900000        # 15 minutes
REFRESH_EXPIRATION_MS=604800000 # 7 days

# CORS & File Uploads
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:80,http://localhost
UPLOAD_DIR=/app/uploads/resumes
MAX_RESUME_SIZE_MB=5

# Admin Bootstrap (Auto-created on initial startup if not present)
ADMIN_BOOTSTRAP_EMAIL=admin@hireflow.edu
ADMIN_BOOTSTRAP_PASSWORD=AdminSecurePassword123!

# AI Module Configuration (Optional - graceful rule-based fallback if disabled)
AI_ENABLED=true
AI_PROVIDER=openai
AI_API_KEY=your-api-key
AI_MODEL=gpt-4o-mini
AI_BASE_URL=https://api.openai.com/v1

# Frontend API URL
VITE_API_BASE_URL=/api
```

---

## 🛠️ Local Development Setup

### Prerequisites
- Java 17+
- Node.js 18+ and npm
- Aiven Cloud MySQL account / credentials

### 1. Run Backend
```bash
cd backend
# Run Flyway migrations and start Spring Boot
./mvnw spring-boot:run
```
Backend runs at `http://localhost:8080`.
- Swagger API Docs: `http://localhost:8080/swagger-ui.html`

### 2. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs at `http://localhost:5173`.

---

## 🐳 Docker Deployment

To build and run the entire application stack containerized:

```bash
# Build and run backend + frontend containers
docker-compose up --build -d

# Check running containers
docker-compose ps
```

The frontend will be served at `http://localhost:80`, routing `/api/*` calls to the containerized Spring Boot backend connected to Aiven Cloud MySQL.

---

## 🔒 Security & Deterministic Eligibility Principle

1. **Deterministic Eligibility Is Authoritative**: Eligibility for drives is evaluated strictly via deterministic business rules:
   - Profile completeness check
   - Branch eligibility check
   - CGPA cutoff check
   - Backlog threshold check
   - Drive status & deadline check
   - Single active application per drive
2. **AI Is Advisory Only**: AI analysis assists coordinators by parsing unstructured JDs and computing candidate match scores with explanations. AI never directly promotes, rejects, or selects candidates.
3. **Defense in Depth**: Password hashing with BCrypt (strength 12), token rotation with SHA-256 hash storage and reuse detection, and role-based route guards at both Spring Security (`@PreAuthorize`) and React Router layers.
