-- ==============================================================================
-- Student Placement Planner - Oracle Database Seed Data Script
-- Safe to run against existing tables without duplicating records
-- ==============================================================================

-- 1. Insert Demo Administrator if not exists (password: admin123)
MERGE INTO students s
USING (SELECT 'admin@placement.edu' AS email FROM dual) d
ON (s.email = d.email)
WHEN NOT MATCHED THEN
INSERT (name, email, password_hash, role, department, year_of_study, cgpa)
VALUES (
    'Placement Officer',
    'admin@placement.edu',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'ADMIN',
    'Administration',
    0,
    10.0
);

-- 2. Insert Demo Student if not exists (password: student123)
MERGE INTO students s
USING (SELECT 'student@placement.edu' AS email FROM dual) d
ON (s.email = d.email)
WHEN NOT MATCHED THEN
INSERT (name, email, password_hash, role, department, year_of_study, cgpa)
VALUES (
    'Vijay Kumar',
    'student@placement.edu',
    '3612d33b49195d52ad5ef3c5be1fdb7efc464bf27b9c9f430bfb9c4709d78906',
    'STUDENT',
    'Computer Science & Engineering',
    4,
    8.2
);

-- 3. Insert 8 Realistic Placement Job Listings (checking titles to avoid duplicates)
MERGE INTO jobs j
USING (SELECT 'Java Developer Intern' AS title, 'TechNova Solutions' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Java Developer Intern',
    'TechNova Solutions',
    'Hyderabad',
    'Design and develop scalable backend modules using Core Java, JDBC, Servlets, and Oracle Database. Participate in code reviews, SQL query optimization, and REST API testing.',
    'B.Tech / B.E. (CSE, IT, AI-ML)',
    6.5,
    SYSDATE + 30
);

MERGE INTO jobs j
USING (SELECT 'Software Engineer Trainee' AS title, 'CloudPeak Systems' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Software Engineer Trainee',
    'CloudPeak Systems',
    'Bengaluru',
    'Full-stack software engineering trainee program. Work on cloud microservices, relational database modeling, and interactive frontend interfaces.',
    'B.Tech / M.Tech / MCA (Circuit Branches)',
    7.0,
    SYSDATE + 45
);

MERGE INTO jobs j
USING (SELECT 'Python Developer Intern' AS title, 'DataBridge Labs' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Python Developer Intern',
    'DataBridge Labs',
    'Pune',
    'Build automated data extraction pipelines, microservice endpoints, and backend tools using Python, SQL, and Git. Fast-paced product engineering team.',
    'B.Tech (CSE, ECE, Data Science)',
    6.0,
    SYSDATE + 25
);

MERGE INTO jobs j
USING (SELECT 'Graduate Engineer Trainee' AS title, 'NexaCore Technologies' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Graduate Engineer Trainee',
    'NexaCore Technologies',
    'Chennai',
    'Comprehensive graduate engineering trainee curriculum covering system design, relational database administration, unit testing, and agile sprints.',
    'B.Tech (All Engineering Branches)',
    6.5,
    SYSDATE + 40
);

MERGE INTO jobs j
USING (SELECT 'Web Developer Intern' AS title, 'Apex Software Solutions' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Web Developer Intern',
    'Apex Software Solutions',
    'Remote / Noida',
    'Create engaging, accessible web applications using HTML5, modern CSS, JavaScript, and Java web technologies with strong focus on responsive mobile-first design.',
    'B.Tech / BCA / MCA',
    6.0,
    SYSDATE + 35
);

MERGE INTO jobs j
USING (SELECT 'Data Analyst Intern' AS title, 'InsightSphere Analytics' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Data Analyst Intern',
    'InsightSphere Analytics',
    'Mumbai',
    'Extract and clean placement analytics datasets using Oracle SQL, build business intelligence reports, and deliver data-driven executive summaries.',
    'B.Tech / B.Sc / MCA (Any Branch)',
    7.0,
    SYSDATE + 20
);

MERGE INTO jobs j
USING (SELECT 'QA Engineer Trainee' AS title, 'QualityFirst Solutions' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'QA Engineer Trainee',
    'QualityFirst Solutions',
    'Hyderabad',
    'Execute manual and automated test suites, record defects in Jira, write test plans, and verify backend database records using SQL queries.',
    'B.Tech / B.E. (CSE, ECE, EEE)',
    6.2,
    SYSDATE + 50
);

MERGE INTO jobs j
USING (SELECT 'Technical Support Engineer' AS title, 'GlobalNet Services' AS company FROM dual) d
ON (j.title = d.title AND j.company = d.company)
WHEN NOT MATCHED THEN
INSERT (title, company, location, description, eligibility, min_cgpa, deadline)
VALUES (
    'Technical Support Engineer',
    'GlobalNet Services',
    'Gurugram',
    'Diagnose enterprise client applications, analyze server network connectivity, review database logs, and provide technical troubleshooting solutions.',
    'Any Engineering Graduate',
    6.0,
    SYSDATE + 60
);

COMMIT;
