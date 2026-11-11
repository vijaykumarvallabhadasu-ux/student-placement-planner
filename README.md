# Student Placement Planner — Modern Web Application

A full-stack campus placement readiness and recruitment management platform built using **Java Servlets (`javax.servlet`), JSP, JDBC, Oracle Database 10g Express Edition, HTML5, CSS3, and JavaScript**, running on **Eclipse IDE and Apache Tomcat 9.0.120** on **Java 21**.

---

## 1. Project Overview & Features

- **Modern & Colorful UI**: Built with a curated navy, indigo, purple, and teal palette, card elevation, subtle animations, and accessible contrast.
- **Light & Dark Theme**: Header toggle with instant zero-flash pre-rendering and `localStorage` persistence.
- **Dynamic Student Dashboard (`/dashboard`)**:
  - Live placement metrics: Available Jobs, Active Applications, Preparation Milestones, Skills Tracked.
  - Visual placement readiness progress bar.
  - Preparation milestones with one-click completion and target date tracking.
  - Recommended campus recruitment drives with applied status tracking.
  - Graceful degradation: in case of transient database issues, an informative banner is displayed without an unhandled HTTP 500 error page.
- **Skills Portfolio (`/skills`)**:
  - Add and delete technical skills with confirmation dialogs.
  - Quick-fill suggested tags (Core Java, Oracle SQL, Spring Boot, DSA, Python, Web Dev, React, Git).
  - Multi-level proficiency selector (Beginner, Intermediate, Advanced, Expert).
- **Preparation Tasks & Milestones (`/tasks`)**:
  - Create milestone tasks with calendar due dates.
  - Inline editing of task titles and target dates.
  - One-click status toggle between Pending and Completed.
  - Overdue milestone badge indicators and live filtering by All, Pending, Completed, and Overdue.
- **Job Opportunities & Applications (`/jobs` & `/applications`)**:
  - Live client-side keyword search and location filter.
  - Eligibility and minimum CGPA tags.
  - Single-query `LEFT JOIN` application status tracker preventing duplicate applications and N+1 database queries.
- **Student Academic Profile Management (`/profile`)**:
  - View and update academic details: Department/Branch (CSE, IT, ECE, EEE, Mech, Civil, AI & DS), Year of Study, and CGPA.
  - Real-time placement eligibility tier indicator (Product, Dream, and Core drive thresholds).
  - Synchronized across the header badge, dashboard hero banner, and the database.
- **Administrator Recruitment Portal (`/admin/jobs`)**:
  - Restricted to `ADMIN` role via server-side authorization filter (`AuthFilter`).
  - Publish new campus recruitment drives.
  - Complete **Applicant Review Pipeline**: view applicants across drives and update candidate statuses (Under Review, Shortlisted, Interview, Offered, Rejected).
- **Enterprise-Grade Reliability & Security**:
  - Built-in connection pool with dynamic proxy wrapping (`max=5`) tailored for Oracle 10g XE `PROCESSES` limits.
  - Exponential backoff retry handler for transient listener errors (`ORA-12516`, `ORA-12519`).
  - Session fixation protection: pre-existing sessions are invalidated before new session generation.
  - SHA-256 password hashing with fallback compatibility for legacy plaintext records.
  - Proper `web.xml` configuration with custom 404/500 error pages and `http-only` session cookie protection.

---

## 2. Root Cause Analysis: ORA-12516

### The Symptom
When loading `/dashboard`, the application threw an HTTP 500 error:
```
Could not load dashboard data from Oracle database
ORA-12516: Cannot connect to database. Listener at host 127.0.0.1 port 1521 does not have a protocol handler for TCP ready or registered for service XE.
```

### The Root Cause
1. **Unpooled Connection Burst**: `DashboardServlet.doGet()` was calling `DBUtil.getConnection()` sequentially **10 separate times** within a single HTTP request to compute student statistics, fetch tasks, fetch skills, and load jobs.
2. **Oracle 10g XE Process Limit**: Windows Oracle 10g Express Edition defaults to `PROCESSES = 40`. When 10 dedicated server processes were created and torn down in less than 50 milliseconds, Oracle's background Process Monitor (PMON) temporarily flagged the service handler as `BLOCKED`. Subsequent incoming socket handshakes were rejected by the listener with `TNS-12516`.
3. **N+1 Connection Leak**: `ApplicationDAO.findByStudent()` opened a secondary connection (`jobDAO.findById()`) inside the `while (rs.next())` loop of the primary connection.
4. **Missing Driver in Deployment Assembly**: `ojdbc14_g.jar` was present in the Eclipse Java Build Path but was missing from `src/main/webapp/WEB-INF/lib`, leading to runtime driver resolution discrepancies inside Tomcat.

### The Fix
1. **Lightweight Connection Pool**: Implemented a thread-safe connection pool in `DBUtil` with a default cap of 5 connections. Connections are wrapped with a dynamic proxy so that `conn.close()` returns the connection to the pool rather than terminating the Oracle server process.
2. **Listener Error Retry**: Added an automatic retry loop (up to 3 attempts with exponential backoff) specifically intercepting `ORA-12516` and `ORA-12519` transient listener spikes.
3. **Query Optimization**: Eliminated 4 redundant queries in `DashboardServlet` by deriving counts in-memory from the fetched collections, and refactored `ApplicationDAO.findByStudent()` into a single `LEFT JOIN jobs` query.
4. **Deployment Assembly Fix**: Placed `ojdbc11-23.6.0.24.10.jar` into `src/main/webapp/WEB-INF/lib/` to guarantee that Eclipse WTP deploys the JDBC driver directly into Tomcat.
5. **Zero Database Modifications**: All changes were made strictly at the application, DAO, and connection management layer. The existing database schema and records were preserved untouched.

---

## 3. Database Configuration

Database configuration is centralized in `com.placementplanner.util.DBUtil`.

### Default Settings (Local Development)
- **URL**: `jdbc:oracle:thin:@//127.0.0.1:1521/XE`
- **Default Username**: `system`
- **Default Password**: `system`
- **Connection Pool Size**: `5`

### External Configuration (Optional)
Credentials and parameters can be overridden without modifying code using environment variables or JVM system properties:

| Property / Env Var | Default | Description |
| :--- | :--- | :--- |
| `db.url` / `DB_URL` | `jdbc:oracle:thin:@//127.0.0.1:1521/XE` | Oracle JDBC connection URL |
| `db.user` / `DB_USER` | `system` | Database username |
| `db.password` / `DB_PASSWORD` | `system` | Database password |
| `db.pool.max` / `DB_POOL_MAX` | `5` | Maximum active pooled connections |
| `db.pool` / `DB_POOL` | `true` | Enable or disable connection pooling |

> [!NOTE]
> Passwords are never logged in application logs or exposed to frontend views.

---

## 4. How to Run in Eclipse IDE with Apache Tomcat 9

### Step 1: Start Oracle Database 10g Express Edition
Ensure your Oracle Database XE service and listener are running:
1. Open Windows **Services** (`services.msc`).
2. Verify that `OracleServiceXE` and `OracleXETNSListener` are in the **Running** state.
3. Alternatively, in a command prompt or PowerShell:
   ```cmd
   lsnrctl status
   ```
   Ensure `XE` is reported with status `READY`.

### Step 2: Open Eclipse IDE
1. Launch **Eclipse IDE for Enterprise Java and Web Developers**.
2. If the project is already imported, right-click `StudentPlacementPlanner` in the Project Explorer:
   - Select **Maven → Update Project...**
   - Check **Force Update of Snapshots/Releases** and click **OK**.
3. Verify that `src/main/webapp/WEB-INF/lib/ojdbc11-23.6.0.24.10.jar` is present.

### Step 3: Configure Apache Tomcat 9 Server
1. Go to the **Servers** tab at the bottom of Eclipse (or **Window → Show View → Servers**).
2. If your Tomcat 9 server is already configured:
   - Double-click the server to open its configuration.
   - Verify HTTP port is set to **8087** (or your preferred port).
   - Ensure the server runtime JRE points to your installed Java runtime (Java 17 or Java 21).
3. Right-click the Tomcat 9 server → **Clean...** → click **OK**.
4. Right-click the Tomcat 9 server → **Add and Remove...**:
   - Ensure `StudentPlacementPlanner` is listed under **Configured**.
   - Click **Finish**.

### Step 4: Start Server & Access Application
1. Right-click the Tomcat 9 server → **Start** (or **Debug**).
2. Open your web browser and navigate to:
   ```
   http://localhost:8087/StudentPlacementPlanner/
   ```
3. Log in with your existing student or administrator credentials.

---

## 5. Troubleshooting Common Issues

### 1. `ORA-12516` or `ORA-12519`
- **Cause**: Oracle 10g XE process or session limit reached, or PMON temporarily marking the listener as blocked.
- **Solution**: The application's built-in connection pool (`max=5`) and retry mechanism prevent connection saturation. If running external tools simultaneously (like SQL Developer), check Oracle's process count in SQL*Plus:
  ```sql
  SELECT COUNT(*) FROM v$session;
  SELECT * FROM v$resource_limit WHERE resource_name IN ('processes', 'sessions');
  ```

### 2. `ClassNotFoundException: oracle.jdbc.OracleDriver`
- **Cause**: Eclipse Tomcat runtime did not publish `WEB-INF/lib`.
- **Solution**: In Eclipse, right-click the project → **Properties** → **Deployment Assembly** → Ensure `src/main/webapp/WEB-INF/lib` (or Maven Dependencies) is mapped to `WEB-INF/lib`. We have also included `ojdbc11-23.6.0.24.10.jar` directly in `src/main/webapp/WEB-INF/lib/` to prevent this issue.

### 3. Port Conflict on 8087
- **Cause**: Another service or a previous Tomcat instance is holding port 8087.
- **Solution**: Run `netstat -ano | findstr :8087` in PowerShell/CMD and terminate the conflicting process via Task Manager or `taskkill /PID <PID> /F`.

---

## 6. Production-Readiness & Oracle 10g Limitations

> [!WARNING]
> **Oracle 10g Express Edition Legacy Notice**
> Oracle 10g Release 2 (10.2.0.1.0) was released in 2005 and has been end-of-life for over a decade. When planning for production deployment:
> 1. **Process Limit**: Oracle 10g XE has a hardcoded database process limit of 40 (`PROCESSES=40`). The application's connection pool is configured to `5` to stay safely within limits.
> 2. **Authentication Protocol**: Oracle 10g uses legacy 10g password hashes (DES-based). Modern drivers (such as `ojdbc11`) connect in backward compatibility mode.
> 3. **SQL Dialect**: Features like `OFFSET ... FETCH NEXT` (introduced in Oracle 12c) and identity columns are not available in 10g. The DAO layer uses `ROWNUM` and sequence / `NVL(MAX(id), 0) + 1` queries to maintain compatibility with 10g.
> 4. **TLS/SSL**: Oracle 10g XE does not natively support modern TLS 1.3 encrypted listener endpoints without advanced Oracle Advanced Security network configurations. For production environments, migrate to Oracle Database 19c or 23ai LTS.
