# Smart Order Allocation System

A full-stack application built for the **Software Engineer Intern Technical Assessment**. It automatically determines the most suitable branch to fulfill customer orders based on stock availability, branch workload, and geographic proximity, while integrating a real-time AI/ML customer message classifier.

---

## 🛠️ Technologies Used

| Layer | Technology | Description |
|-------|-----------|-------------|
| **Backend** | Java 21, Spring Boot 3.4.1 | Core REST API, Spring Data JPA, Hibernate, Lombok |
| **Security** | Spring Security 6, JWT (`jjwt 0.11.5`) | BCrypt password hashing, stateless JWT authentication |
| **Frontend** | React 18, Vite, React Router 6, Axios | Modern glassmorphism UI, React Context API |
| **Database** | PostgreSQL (Supabase) | Persistent SQL database |
| **AI / ML** | Python 3.12, FastAPI, Scikit-learn | TF-IDF + LinearSVC with probability calibration |

---

## 🚀 Setup Instructions

### Prerequisites
- **Java 21+**
- **Node.js 18+**
- **Python 3.10+**
- **PostgreSQL Database** (or cloud host like Supabase)

### 1. Backend Service (Spring Boot)

```bash
cd smart-order-backend

# Set Environment Variables (or use defaults in application.yaml / .env)
export DB_URL=jdbc:postgresql://your-host:5432/your-db
export DB_USERNAME=postgres
export DB_PASSWORD=your-password
export JWT_SECRET=your-64-char-hex-secret-key-here

# Run backend
./mvnw spring-boot:run
```

The Spring Boot backend will start on `http://localhost:8080`.  
On initial run, it automatically seeds initial demo data via `DataSeeder.java`:
- **System Admin**: `admin@smartorder.com` / `Admin@123`
- **Customer**: `john@example.com` / `Customer@123`
- **3 Initial Branches**: Colombo Main Branch, Kandy City Branch, Galle Fort Branch
- **10 Sample Products**: Across Electronics, Clothing, and Accessories

### 2. Frontend Application (React + Vite)

```bash
cd smart-order-frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will run on `http://localhost:5173`.

### 3. AI/ML Classification Service (Python + FastAPI)

```bash
cd ml-service

# Install dependencies
pip install -r requirements.txt

# Train model on Customer_Message_Dataset.csv
python train.py

# Start FastAPI service
python app.py
```

FastAPI server runs on `http://localhost:8000`.

---

## 🏗️ Brief Explanation of System Architecture

```
┌─────────────────┐       ┌────────────────────────┐       ┌─────────────────┐
│                 │       │                        │       │                 │
│  React App UI   │──────▶│ Spring Boot REST API   │──────▶│ PostgreSQL DB   │
│  (Vite :5173)   │       │ (:8080)                │       │ (Supabase)      │
│                 │       │                        │       │                 │
└─────────────────┘       └───────────┬────────────┘       └─────────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │ Python FastAPI ML      │
                          │ Microservice (:8000)   │
                          └────────────────────────┘
```

1. **Frontend Layer**: React SPA built with Vite. Manages user authentication, product catalog browsing, persistent cart synchronization, order placement, and customer-admin messaging.
2. **Backend API Layer**: Spring Boot REST backend handling business logic, user authentication, role-based authorization, order lifecycle state transitions, database access, and branch allocation calculations.
3. **Database Layer**: PostgreSQL instance storing Users, Branches, Products, Branch Inventory, Orders, Order Items, Cart Items, and Order Messages.
4. **AI Microservice**: Lightweight FastAPI service running a Scikit-Learn ML pipeline to classify customer notes and messages in real-time.

---

## 🧠 Branch Allocation Logic & Why Selected

### Weighted Multi-Factor Algorithm

When an order is submitted, `AllocationService.java` executes the following logic:

1. **Stock Qualification Filter**:
   - Identifies active branches that possess **sufficient stock for ALL requested items**.
   - If no branch has complete inventory, the order is automatically flagged as `REJECTED` with an explicit failure reason.

2. **Multi-Factor Scoring**:
   For every eligible branch, a normalized score ($0.0 \rightarrow 1.0$) is calculated:

   $$\text{Score} = (0.40 \times \text{StockScore}) + (0.35 \times \text{WorkloadScore}) + (0.25 \times \text{ProximityScore})$$

   - **Stock Score (40% Weight)**: Ratio of available stock vs requested quantity. Higher available stock reduces future stockout risks.
   - **Workload Score (35% Weight)**: Calculated as $1 - \left(\frac{\text{Current Active Orders}}{\text{Max Workload Capacity}}\right)$. Prevents bottlenecking overloaded branches.
   - **Proximity Score (25% Weight)**: Calculated using the **Haversine formula** to measure straight-line distance between the customer location and branch coordinates.

### Why This Approach Was Selected

- **Inventory-First Priority (40%)**: Fulfilment is impossible without stock.
- **Workload Protection (35%)**: Allocating to an overloaded nearby branch leads to shipping delays. A slightly farther branch with zero backlog delivers faster.
- **Distance Factor (25%)**: Ensures proximity plays a decisive role when stock levels and workloads are comparable.
- **Deterministic & Fast**: Avoids heavy constraint solvers while producing balanced allocations in $\mathcal{O}(N)$ time.

---

## 🔒 Authentication & Security Approach

| Security Component | Implementation | Purpose |
|--------------------|----------------|---------|
| **Password Hashing** | Spring Security `BCryptPasswordEncoder` | Secure one-way hashing with salt |
| **Token Authentication** | Stateless JWT (`jjwt 0.11.5`) | 24-hour expiration token passed via `Authorization: Bearer` header |
| **Token Refresh** | Refresh Tokens with Database Rotation | 7-day refresh tokens; revokes old token upon rotation |
| **Role-Based Access Control (RBAC)** | `ROLE_ADMIN` & `ROLE_CUSTOMER` | `@PreAuthorize("hasRole('ADMIN')")` secures `/api/admin/**` endpoints |
| **Rate Limiting** | Sliding Window Filter (`RateLimitingFilter.java`) | 30 requests/min on auth, 100 requests/min general |
| **Input Validation** | Jakarta Bean Validation (`@Valid`, `@NotNull`) | Prevents invalid payload processing |
| **CORS** | Strict Origin Whitelisting | Allows requests only from configured frontend domains |

---

## 🤖 AI/ML Approach

### Text Classification Pipeline
- **Dataset**: Trained on `Customer_Message_Dataset.csv` (~426 labeled customer samples).
- **Text Preprocessing**: Lowercasing, whitespace stripping, and regex noise removal.
- **Feature Extraction**: TF-IDF vectorization with unigram and bigram (`ngram_range=(1,2)`).
- **Classification Model**: `LinearSVC` wrapped in `CalibratedClassifierCV` for calibrated probability estimates.
- **Confidence Threshold**: Set to $60\%$. Predictions below this threshold return `Uncertain`.

### Target Categories
- `Payment Issue`
- `Delivery Issue`
- `Refund/Cancellation`
- `Product Inquiry`
- `General Inquiry`

### Admin vs Customer Display Rules
- Customer messages sent at order creation or via post-order support chat are classified in real-time.
- The resulting **AI Category badge** (e.g., `AI Category: Delivery Issue (86% confidence)`) is displayed **exclusively to Admins** on order detail and dashboard tables.

---

## ⚠️ Assumptions & Limitations

1. **Coordinates**: Customer coordinates are mapped from selected Sri Lankan District centroids or browser GPS.
2. **Stock Reservation**: Stock is deducted optimistically upon order allocation.
3. **Workload Counter**: Workload is tracked by active non-delivered order count.
4. **Single-Node Rate Limiting**: In-memory rate limiting is designed for single-instance deployments.
