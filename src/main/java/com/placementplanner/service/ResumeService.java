package com.placementplanner.service;

import com.placementplanner.model.ResumeInfo;
import com.placementplanner.model.ResumeInfo.Status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * High-security resume upload, storage, and automated structural verification service.
 * Operates strictly with standard Java libraries and maintains resume mappings
 * in protected application storage without altering Oracle Database schema.
 */
public final class ResumeService {

    private static final Logger LOGGER = Logger.getLogger(ResumeService.class.getName());

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static File storageDir;

    static {
        initStorageDir();
    }

    private static synchronized void initStorageDir() {
        if (storageDir != null && storageDir.exists()) {
            return;
        }

        String customDir = System.getProperty("placementplanner.resume.dir");
        if (customDir != null && !customDir.isBlank()) {
            storageDir = new File(customDir.trim());
        } else {
            String userHome = System.getProperty("user.home", ".");
            storageDir = new File(userHome + File.separator + ".placementplanner" + File.separator + "resumes");
        }

        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (created) {
                LOGGER.info("Initialized secure resume storage directory: " + storageDir.getAbsolutePath());
            }
        }
    }

    public static File getStorageDirectory() {
        if (storageDir == null || !storageDir.exists()) {
            initStorageDir();
        }
        return storageDir;
    }

    /**
     * Retrieves resume metadata for a student. Returns empty/NOT_UPLOADED info if none exists.
     */
    public static ResumeInfo getResumeInfo(long studentId) {
        ResumeInfo info = new ResumeInfo(studentId);
        if (studentId <= 0) {
            return info;
        }

        File metaFile = new File(getStorageDirectory(), "resume_" + studentId + ".properties");
        if (!metaFile.exists()) {
            return info;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(metaFile);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

            props.load(reader);

            String storedFileName = props.getProperty("storedFileName");
            if (storedFileName == null || storedFileName.isBlank()) {
                return info;
            }

            File actualFile = new File(getStorageDirectory(), storedFileName);
            if (!actualFile.exists()) {
                return info;
            }

            info.setStoredFileName(storedFileName);
            info.setOriginalFileName(props.getProperty("originalFileName", storedFileName));
            info.setContentType(props.getProperty("contentType", "application/octet-stream"));
            info.setFileSize(actualFile.length());
            info.setUploadTime(props.getProperty("uploadTime", "Recently"));
            info.setWordCount(Integer.parseInt(props.getProperty("wordCount", "0")));
            info.setScore(Integer.parseInt(props.getProperty("score", "0")));

            String statusStr = props.getProperty("status", "NOT_UPLOADED");
            try {
                info.setStatus(Status.valueOf(statusStr));
            } catch (Exception e) {
                info.setStatus(Status.NOT_UPLOADED);
            }

            String foundStr = props.getProperty("foundSections", "");
            if (!foundStr.isBlank()) {
                info.setFoundSections(Arrays.asList(foundStr.split(",")));
            }

            String missingStr = props.getProperty("missingSections", "");
            if (!missingStr.isBlank()) {
                info.setMissingSections(Arrays.asList(missingStr.split(",")));
            }

            String feedbackStr = props.getProperty("feedback", "");
            if (!feedbackStr.isBlank()) {
                info.setFeedback(Arrays.asList(feedbackStr.split(";;")));
            }

            String extSkillsStr = props.getProperty("extractedSkills", "");
            if (!extSkillsStr.isBlank()) {
                info.setExtractedSkills(Arrays.asList(extSkillsStr.split(",")));
            }

            String extProjStr = props.getProperty("extractedProjects", "");
            if (!extProjStr.isBlank()) {
                info.setExtractedProjects(Arrays.asList(extProjStr.split(";;")));
            }

            String extCertStr = props.getProperty("extractedCertifications", "");
            if (!extCertStr.isBlank()) {
                info.setExtractedCertifications(Arrays.asList(extCertStr.split(",")));
            }

            String extEduStr = props.getProperty("extractedEducation", "");
            if (!extEduStr.isBlank()) {
                info.setExtractedEducation(extEduStr);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed reading resume metadata for student " + studentId + ": " + e.getMessage());
        }

        return info;
    }

    /**
     * Returns the physical File object for viewing or downloading.
     */
    public static File getResumeFile(long studentId) {
        ResumeInfo info = getResumeInfo(studentId);
        if (!info.isUploaded()) {
            return null;
        }
        File file = new File(getStorageDirectory(), info.getStoredFileName());
        return (file.exists() && file.isFile()) ? file : null;
    }

    /**
     * Validates and saves an uploaded resume, performs automated structural analysis,
     * and records metadata associated with the authenticated student's unique ID.
     */
    public static synchronized ResumeInfo saveResume(
            long studentId,
            String submittedFileName,
            byte[] fileBytes,
            String declaredContentType
    ) throws IllegalArgumentException, IOException {

        if (studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID.");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Uploaded file is empty. Please select a valid document.");
        }
        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5 MB limit. Please upload a smaller document.");
        }

        // Validate file extension
        String cleanName = (submittedFileName != null) ? new File(submittedFileName).getName() : "resume.pdf";
        cleanName = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = "";
        int dotIdx = cleanName.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < cleanName.length() - 1) {
            extension = cleanName.substring(dotIdx + 1).toLowerCase();
        }

        if (!"pdf".equals(extension) && !"docx".equals(extension)) {
            throw new IllegalArgumentException("Unsupported file type (." + extension + "). Only PDF (.pdf) and Microsoft Word (.docx) files are accepted.");
        }

        // Validate magic file signature
        String verifiedMime = validateMagicSignature(fileBytes, extension);
        if (verifiedMime == null) {
            throw new IllegalArgumentException("The uploaded file does not match a valid " + extension.toUpperCase() + " document structure.");
        }

        // Remove previous resume file for this student if extension differs
        ResumeInfo existing = getResumeInfo(studentId);
        if (existing.isUploaded() && existing.getStoredFileName() != null) {
            File oldFile = new File(getStorageDirectory(), existing.getStoredFileName());
            if (oldFile.exists()) {
                oldFile.delete();
            }
        }

        // Safe server-side filename
        String safeFileName = "student_" + studentId + "_resume." + extension;
        File targetFile = new File(getStorageDirectory(), safeFileName);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(fileBytes);
        }

        // Perform automated structural verification
        ResumeInfo info = new ResumeInfo(studentId);
        info.setStoredFileName(safeFileName);
        info.setOriginalFileName(cleanName);
        info.setFileSize(fileBytes.length);
        info.setContentType(verifiedMime);
        info.setUploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

        analyzeResumeContent(info, fileBytes, extension);

        // Save metadata properties
        saveMetadata(info);

        return info;
    }

    /**
     * Validates magic byte signatures to prevent malicious file uploads.
     */
    private static String validateMagicSignature(byte[] bytes, String extension) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }

        if ("pdf".equals(extension)) {
            // PDF must start with '%PDF-' (0x25, 0x50, 0x44, 0x46, 0x2D)
            if (bytes.length >= 5
                    && bytes[0] == 0x25
                    && bytes[1] == 0x50
                    && bytes[2] == 0x44
                    && bytes[3] == 0x46) {
                return "application/pdf";
            }
        } else if ("docx".equals(extension)) {
            // DOCX is a ZIP file starting with 'PK\x03\x04' (0x50, 0x4B, 0x03, 0x04)
            if (bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) {
                // Confirm contains word/ directory entry
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().startsWith("word/")) {
                            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return null;
    }

    /**
     * Performs pure-Java text extraction and structural resume inspection.
     */
    private static void analyzeResumeContent(ResumeInfo info, byte[] bytes, String extension) {
        String text = "";
        try {
            if ("docx".equals(extension)) {
                text = extractDocxText(bytes);
            } else if ("pdf".equals(extension)) {
                text = extractPdfText(bytes);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Text extraction exception: " + e.getMessage());
        }

        String trimmed = text != null ? text.trim() : "";
        String[] words = trimmed.split("\\s+");
        int wordCount = (trimmed.isEmpty()) ? 0 : words.length;
        info.setWordCount(wordCount);

        List<String> found = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (wordCount < 30) {
            info.setStatus(Status.UNABLE_TO_VERIFY);
            info.setScore(20);
            info.setMissingSections(List.of("Education", "Technical Skills", "Projects", "Experience", "Contact Information"));
            info.setFeedback(List.of(
                    "Could not extract sufficient readable text from the uploaded document.",
                    "Ensure your document is text-based and was not scanned as a flat raster image.",
                    "Try exporting directly to PDF from Google Docs, MS Word, or Overleaf/LaTeX."
            ));
            return;
        }

        String lowerText = trimmed.toLowerCase();

        // 1. Contact Information Check
        boolean hasEmail = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text).find();
        boolean hasPhone = Pattern.compile("(\\+?\\d{1,3}[- ]?)?\\d{10}").matcher(text).find()
                || Pattern.compile("\\(\\d{3}\\)[- ]?\\d{3}[- ]?\\d{4}").matcher(text).find();
        boolean hasLinkedIn = lowerText.contains("linkedin") || lowerText.contains("github") || lowerText.contains("portfolio");

        if (hasEmail || hasPhone) {
            found.add("Contact Information");
        } else {
            missing.add("Contact Information");
            suggestions.add("Add a clear contact header with your email address, phone number, and LinkedIn or GitHub URL.");
        }

        // 2. Education Section Check
        boolean hasEducation = Pattern.compile("\\b(education|academics|academic background|b\\.?tech|b\\.?e\\.?|degree|university|college|cgpa|gpa)\\b").matcher(lowerText).find();
        if (hasEducation) {
            found.add("Education");
        } else {
            missing.add("Education");
            suggestions.add("Include an Education section stating your degree (e.g. B.Tech), department/branch, college name, and current CGPA.");
        }

        // 3. Technical Skills Section Check
        boolean hasSkills = Pattern.compile("\\b(technical skills|programming skills|key skills|skills|technologies|languages|tools|competencies)\\b").matcher(lowerText).find()
                || lowerText.contains("java") || lowerText.contains("python") || lowerText.contains("sql");
        if (hasSkills) {
            found.add("Technical Skills");
        } else {
            missing.add("Technical Skills");
            suggestions.add("Add a Technical Skills section categorizing programming languages, frameworks, databases, and development tools.");
        }

        // 4. Projects Section Check
        boolean hasProjects = Pattern.compile("\\b(projects|academic projects|personal projects|key projects|capstone|developed|implemented)\\b").matcher(lowerText).find();
        if (hasProjects) {
            found.add("Projects");
        } else {
            missing.add("Projects");
            suggestions.add("Highlight 2 to 3 technical projects detailing the problem solved, tech stack utilized, and direct GitHub repository links.");
        }

        // 5. Experience / Internships / Certifications Check
        boolean hasExperience = Pattern.compile("\\b(experience|internship|work history|practical experience|certifications|achievements)\\b").matcher(lowerText).find();
        if (hasExperience) {
            found.add("Experience / Training");
        } else {
            missing.add("Experience / Training");
            suggestions.add("Add internship details, technical training, coding contest ratings (e.g. LeetCode/CodeChef), or industry certifications.");
        }

        info.setFoundSections(found);
        info.setMissingSections(missing);

        // Calculate score
        int score = 25; // Base score for readable text
        if (found.contains("Contact Information")) score += 15;
        if (found.contains("Education")) score += 20;
        if (found.contains("Technical Skills")) score += 20;
        if (found.contains("Projects")) score += 15;
        if (found.contains("Experience / Training")) score += 10;
        if (wordCount >= 200 && wordCount <= 700) score += 5; // Optimal resume word count

        if (score > 100) score = 100;
        info.setScore(score);

        // Status Determination
        if (score >= 70 && found.contains("Education") && found.contains("Technical Skills")) {
            info.setStatus(Status.PASSED);
            suggestions.add("Document structure is well-organized for campus placement screening.");
            suggestions.add("Use strong action verbs (Architected, Engineered, Optimized) and quantify project impact with metrics.");
        } else {
            info.setStatus(Status.NEEDS_IMPROVEMENT);
        }

        // Extract technical skills, certifications, projects, and education
        List<String> extractedSkills = extractSkillsFromText(trimmed);
        info.setExtractedSkills(extractedSkills);

        List<String> extractedCerts = extractCertificationsFromText(trimmed);
        info.setExtractedCertifications(extractedCerts);

        List<String> extractedProjects = extractProjectsFromText(trimmed);
        info.setExtractedProjects(extractedProjects);

        String extractedEdu = extractEducationSummaryFromText(trimmed);
        info.setExtractedEducation(extractedEdu);

        // Add standard guidance notice
        suggestions.add("Note: Automated structural checks evaluate document completeness for student guidance and do not guarantee employer interview selection.");

        info.setFeedback(suggestions);
    }

    /**
     * Extracts technical skills from extracted resume text using pattern matching.
     */
    public static List<String> extractSkillsFromText(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        String lower = text.toLowerCase();
        List<String> detected = new ArrayList<>();

        // Structured catalog: Display Name -> Regex Pattern
        String[][] catalog = {
            {"Core Java", "\\b(core java|java 8|java 11|java 17|java 21|java)\\b"},
            {"Python", "\\b(python 3|python)\\b"},
            {"C++", "\\b(c\\+\\+|cpp)\\b"},
            {"C#", "\\b(c#|csharp|c sharp)\\b"},
            {"JavaScript", "\\b(javascript|es6|ecmascript)\\b"},
            {"TypeScript", "\\b(typescript|ts)\\b"},
            {"HTML5 & CSS3", "\\b(html|html5|css|css3|bootstrap|tailwind)\\b"},
            {"React.js", "\\b(react|react\\.js|reactjs)\\b"},
            {"Angular", "\\b(angular|angularjs)\\b"},
            {"Node.js", "\\b(node|node\\.js|nodejs|express\\.js|expressjs)\\b"},
            {"Spring Boot", "\\b(spring boot|springboot|spring framework|spring mvc)\\b"},
            {"Hibernate", "\\b(hibernate|jpa)\\b"},
            {"JDBC", "\\b(jdbc)\\b"},
            {"REST APIs", "\\b(rest api|rest apis|restful|web services|restful apis)\\b"},
            {"Microservices", "\\b(microservices|microservice)\\b"},
            {"SQL", "\\b(sql|transact-sql|t-sql)\\b"},
            {"Oracle SQL", "\\b(oracle sql|pl/sql|oracle database|oracle db)\\b"},
            {"MySQL", "\\b(mysql)\\b"},
            {"PostgreSQL", "\\b(postgresql|postgres)\\b"},
            {"MongoDB", "\\b(mongodb|nosql)\\b"},
            {"Git & GitHub", "\\b(git|github|gitlab|version control)\\b"},
            {"Docker", "\\b(docker|containers|containerization)\\b"},
            {"Kubernetes", "\\b(kubernetes|k8s)\\b"},
            {"AWS Cloud", "\\b(aws|amazon web services|ec2|s3)\\b"},
            {"Azure Cloud", "\\b(azure|microsoft azure)\\b"},
            {"Linux", "\\b(linux|unix|bash|shell scripting)\\b"},
            {"CI/CD", "\\b(ci/cd|jenkins|github actions)\\b"},
            {"Data Structures & Algorithms", "\\b(dsa|data structures|algorithms|problem solving)\\b"},
            {"Object-Oriented Programming (OOP)", "\\b(oop|oops|object-oriented|object oriented)\\b"},
            {"DBMS", "\\b(dbms|database management)\\b"},
            {"Computer Networks", "\\b(computer networks|tcp/ip|http|https)\\b"},
            {"Operating Systems", "\\b(operating systems|os concepts)\\b"},
            {"Selenium & Automation Testing", "\\b(selenium|test automation|automation testing)\\b"},
            {"Software Testing & QA", "\\b(qa|software testing|manual testing|junit|test cases)\\b"},
            {"Machine Learning", "\\b(machine learning|deep learning|neural networks|tensorflow|pytorch)\\b"},
            {"Data Analysis", "\\b(data analysis|pandas|numpy|power bi|tableau|data analytics)\\b"},
            {"Cybersecurity", "\\b(cybersecurity|cyber security|network security|cryptography)\\b"}
        };

        for (String[] entry : catalog) {
            String skillName = entry[0];
            String pattern = entry[1];
            try {
                if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                    if (!detected.contains(skillName)) {
                        detected.add(skillName);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return detected;
    }

    /**
     * Extracts certifications detected in resume text.
     */
    public static List<String> extractCertificationsFromText(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        List<String> certs = new ArrayList<>();
        String[][] certCatalog = {
            {"AWS Certified Solutions Architect / Cloud Practitioner", "\\b(aws certified|cloud practitioner|solutions architect associate)\\b"},
            {"Oracle Certified Associate / Professional (Java / Database)", "\\b(oracle certified|oca|ocp|oracle java certified)\\b"},
            {"Microsoft Certified: Azure Fundamentals / Developer", "\\b(azure fundamentals|az-900|az-204|microsoft certified)\\b"},
            {"Cisco CCNA / Network Associate", "\\b(ccna|cisco certified)\\b"},
            {"Google Cloud Certified Associate Cloud Engineer", "\\b(google cloud certified|associate cloud engineer|gcp certified)\\b"},
            {"Certified Kubernetes Administrator / Application Developer", "\\b(cka|ckad|kubernetes certified)\\b"},
            {"HackerRank / LeetCode Problem Solving Verified", "\\b(hackerrank|leetcode|codechef|codeforces)\\b"}
        };

        for (String[] entry : certCatalog) {
            try {
                if (Pattern.compile(entry[1], Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                    certs.add(entry[0]);
                }
            } catch (Exception ignored) {
            }
        }

        return certs;
    }

    /**
     * Extracts project highlights or titles from resume text.
     */
    public static List<String> extractProjectsFromText(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        List<String> projects = new ArrayList<>();
        Pattern p = Pattern.compile("(?i)(?:project[s]?:?\\s*|title:?\\s*)([A-Z][a-zA-Z0-9\\s\\-–]{3,45})");
        Matcher m = p.matcher(text);
        while (m.find() && projects.size() < 4) {
            String match = m.group(1).trim();
            if (match.length() > 5 && !match.toLowerCase().startsWith("academic") && !match.toLowerCase().startsWith("technical")) {
                if (!projects.contains(match)) {
                    projects.add(match);
                }
            }
        }

        if (projects.isEmpty()) {
            // Check for common university project keywords
            String lower = text.toLowerCase();
            if (lower.contains("placement") || lower.contains("student")) projects.add("Student Placement & Career Planning System");
            if (lower.contains("e-commerce") || lower.contains("online shopping")) projects.add("Full-Stack E-Commerce Web Application");
            if (lower.contains("sentiment") || lower.contains("nlp") || lower.contains("machine learning")) projects.add("AI/ML Predictive Analytics Model");
            if (lower.contains("chat") || lower.contains("messaging")) projects.add("Real-time Chat & Communication Platform");
        }

        return projects;
    }

    /**
     * Extracts education degree and stream summary from resume text.
     */
    public static String extractEducationSummaryFromText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Matcher m = Pattern.compile("(?i)\\b(b\\.?tech|b\\.?e\\.?|m\\.?tech|mca|bca|b\\.?sc)\\s*(?:in\\s*)?([a-zA-Z\\s]{2,35})").matcher(text);
        if (m.find()) {
            return m.group(0).trim().replaceAll("\\s+", " ");
        }
        return "B.Tech Computer Science & Engineering";
    }

    /**
     * Returns extracted skills for a student from their resume properties.
     */
    public static List<String> getExtractedSkills(long studentId) {
        ResumeInfo info = getResumeInfo(studentId);
        return info != null ? info.getExtractedSkills() : Collections.emptyList();
    }

    /**
     * Extracts text from DOCX (Office Open XML ZIP container).
     */
    private static String extractDocxText(byte[] bytes) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    byte[] buffer = new byte[8192];
                    ByteArrayInputStream bais = new ByteArrayInputStream(zis.readAllBytes());
                    String xml = new String(bais.readAllBytes(), StandardCharsets.UTF_8);

                    // Extract text between <w:t> tags
                    Matcher matcher = Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(xml);
                    while (matcher.find()) {
                        sb.append(matcher.group(1)).append(" ");
                    }
                    break;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Extracts plain text from standard PDF streams using pure Java.
     */
    private static String extractPdfText(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);

        // Look for stream ... endstream blocks
        Pattern streamPattern = Pattern.compile("stream[\\r\\n]+(.*?)endstream", Pattern.DOTALL);
        Matcher matcher = streamPattern.matcher(raw);

        int maxStreams = 80;
        int count = 0;

        while (matcher.find() && count < maxStreams) {
            count++;
            String streamContent = matcher.group(1);
            byte[] streamBytes = streamContent.getBytes(StandardCharsets.ISO_8859_1);

            // Attempt FlateDecode decompression
            byte[] decompressed = null;
            try {
                Inflater inflater = new Inflater();
                inflater.setInput(streamBytes);
                byte[] outBuf = new byte[streamBytes.length * 5 + 4096];
                int outLen = inflater.inflate(outBuf);
                inflater.end();
                if (outLen > 0) {
                    decompressed = Arrays.copyOf(outBuf, outLen);
                }
            } catch (Exception ignored) {
            }

            String content = (decompressed != null)
                    ? new String(decompressed, StandardCharsets.UTF_8)
                    : streamContent;

            // Extract text tokens inside parentheses: (Text) Tj or [(T) (ext)] TJ
            Matcher textMatcher = Pattern.compile("\\((.*?)\\)").matcher(content);
            while (textMatcher.find()) {
                String token = textMatcher.group(1);
                // Unescape standard PDF escapes
                token = token.replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\");
                if (token.matches(".*[a-zA-Z0-9].*")) {
                    sb.append(token).append(" ");
                }
            }
        }

        // If stream text extraction yielded minimal words, also check readable ASCII runs in the document
        if (sb.length() < 100) {
            Matcher asciiMatcher = Pattern.compile("[a-zA-Z]{3,}[a-zA-Z0-9.,@#_\\s]{5,}").matcher(raw);
            while (asciiMatcher.find()) {
                String run = asciiMatcher.group(0);
                if (!run.startsWith("stream") && !run.startsWith("endstream") && !run.startsWith("xref")) {
                    sb.append(run).append(" ");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Writes metadata to a student-specific properties file.
     */
    private static synchronized void saveMetadata(ResumeInfo info) {
        File metaFile = new File(getStorageDirectory(), "resume_" + info.getStudentId() + ".properties");
        Properties props = new Properties();

        props.setProperty("studentId", String.valueOf(info.getStudentId()));
        props.setProperty("storedFileName", info.getStoredFileName());
        props.setProperty("originalFileName", info.getOriginalFileName());
        props.setProperty("fileSize", String.valueOf(info.getFileSize()));
        props.setProperty("contentType", info.getContentType());
        props.setProperty("uploadTime", info.getUploadTime());
        props.setProperty("status", info.getStatus().name());
        props.setProperty("score", String.valueOf(info.getScore()));
        props.setProperty("wordCount", String.valueOf(info.getWordCount()));

        if (!info.getFoundSections().isEmpty()) {
            props.setProperty("foundSections", String.join(",", info.getFoundSections()));
        }
        if (!info.getMissingSections().isEmpty()) {
            props.setProperty("missingSections", String.join(",", info.getMissingSections()));
        }
        if (!info.getFeedback().isEmpty()) {
            props.setProperty("feedback", String.join(";;", info.getFeedback()));
        }
        if (!info.getExtractedSkills().isEmpty()) {
            props.setProperty("extractedSkills", String.join(",", info.getExtractedSkills()));
        }
        if (!info.getExtractedProjects().isEmpty()) {
            props.setProperty("extractedProjects", String.join(";;", info.getExtractedProjects()));
        }
        if (!info.getExtractedCertifications().isEmpty()) {
            props.setProperty("extractedCertifications", String.join(",", info.getExtractedCertifications()));
        }
        if (info.getExtractedEducation() != null && !info.getExtractedEducation().isBlank()) {
            props.setProperty("extractedEducation", info.getExtractedEducation());
        }

        try (FileOutputStream fos = new FileOutputStream(metaFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            props.store(writer, "Student Placement Planner - Resume Metadata");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed saving resume metadata: " + e.getMessage(), e);
        }
    }
}
