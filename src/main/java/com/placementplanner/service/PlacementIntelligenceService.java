package com.placementplanner.service;

import com.placementplanner.model.ResumeInfo;
import com.placementplanner.model.Student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core placement readiness intelligence and career matching engine.
 * Computes transparent, auditable placement readiness scores and job skill match percentages.
 * Adheres strictly to the database preservation rule: computes all intelligence in memory.
 */
public final class PlacementIntelligenceService {

    private PlacementIntelligenceService() {
    }

    /**
     * DTO containing recommended learning milestone tasks.
     */
    public static class RecommendedTask {
        private final String skill;
        private final String title;
        private final String description;
        private final String suggestedDueDate;

        public RecommendedTask(String skill, String title, String description, String suggestedDueDate) {
            this.skill = skill;
            this.title = title;
            this.description = description;
            this.suggestedDueDate = suggestedDueDate;
        }

        public String getSkill() {
            return skill;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getSuggestedDueDate() {
            return suggestedDueDate;
        }
    }

    /**
     * DTO representing individual job skill match computation.
     */
    public static class JobMatchResult {
        private final long jobId;
        private final Map<String, Object> job;
        private final List<String> requiredSkills;
        private final List<String> preferredSkills;
        private final List<String> matchedRequiredSkills;
        private final List<String> missingRequiredSkills;
        private final List<String> matchedPreferredSkills;
        private final List<String> missingPreferredSkills;
        private final List<String> resumeEvidencedSkills;
        private final int matchPercent;
        private final String calculationExplanation;
        private final List<RecommendedTask> recommendedTasks;

        public JobMatchResult(
                long jobId,
                Map<String, Object> job,
                List<String> requiredSkills,
                List<String> preferredSkills,
                List<String> matchedRequiredSkills,
                List<String> missingRequiredSkills,
                List<String> matchedPreferredSkills,
                List<String> missingPreferredSkills,
                List<String> resumeEvidencedSkills,
                int matchPercent,
                String calculationExplanation,
                List<RecommendedTask> recommendedTasks
        ) {
            this.jobId = jobId;
            this.job = job;
            this.requiredSkills = requiredSkills;
            this.preferredSkills = preferredSkills;
            this.matchedRequiredSkills = matchedRequiredSkills;
            this.missingRequiredSkills = missingRequiredSkills;
            this.matchedPreferredSkills = matchedPreferredSkills;
            this.missingPreferredSkills = missingPreferredSkills;
            this.resumeEvidencedSkills = resumeEvidencedSkills;
            this.matchPercent = matchPercent;
            this.calculationExplanation = calculationExplanation;
            this.recommendedTasks = recommendedTasks;
        }

        public long getJobId() {
            return jobId;
        }

        public Map<String, Object> getJob() {
            return job;
        }

        public List<String> getRequiredSkills() {
            return requiredSkills;
        }

        public List<String> getPreferredSkills() {
            return preferredSkills;
        }

        public List<String> getMatchedRequiredSkills() {
            return matchedRequiredSkills;
        }

        public List<String> getMissingRequiredSkills() {
            return missingRequiredSkills;
        }

        public List<String> getMatchedPreferredSkills() {
            return matchedPreferredSkills;
        }

        public List<String> getMissingPreferredSkills() {
            return missingPreferredSkills;
        }

        public List<String> getResumeEvidencedSkills() {
            return resumeEvidencedSkills;
        }

        public int getMatchPercent() {
            return matchPercent;
        }

        public String getCalculationExplanation() {
            return calculationExplanation;
        }

        public List<RecommendedTask> getRecommendedTasks() {
            return recommendedTasks;
        }
    }

    /**
     * DTO representing comprehensive placement readiness report.
     */
    public static class ReadinessReport {
        private final int overallScore; // 0 - 100%
        private final int academicScore; // max 25%
        private final String academicNote;
        private final int skillsScore; // max 25%
        private final String skillsNote;
        private final int resumeScore; // max 25%
        private final String resumeNote;
        private final int tasksScore; // max 25%
        private final String tasksNote;

        private final List<String> possessedSkills;
        private final List<String> missingInDemandSkills;
        private final List<RecommendedTask> suggestedTasks;
        private final List<String> nextSteps;
        private final List<JobMatchResult> rankedJobs;

        public ReadinessReport(
                int overallScore,
                int academicScore,
                String academicNote,
                int skillsScore,
                String skillsNote,
                int resumeScore,
                String resumeNote,
                int tasksScore,
                String tasksNote,
                List<String> possessedSkills,
                List<String> missingInDemandSkills,
                List<RecommendedTask> suggestedTasks,
                List<String> nextSteps,
                List<JobMatchResult> rankedJobs
        ) {
            this.overallScore = overallScore;
            this.academicScore = academicScore;
            this.academicNote = academicNote;
            this.skillsScore = skillsScore;
            this.skillsNote = skillsNote;
            this.resumeScore = resumeScore;
            this.resumeNote = resumeNote;
            this.tasksScore = tasksScore;
            this.tasksNote = tasksNote;
            this.possessedSkills = possessedSkills;
            this.missingInDemandSkills = missingInDemandSkills;
            this.suggestedTasks = suggestedTasks;
            this.nextSteps = nextSteps;
            this.rankedJobs = rankedJobs;
        }

        public int getOverallScore() {
            return overallScore;
        }

        public int getAcademicScore() {
            return academicScore;
        }

        public String getAcademicNote() {
            return academicNote;
        }

        public int getSkillsScore() {
            return skillsScore;
        }

        public String getSkillsNote() {
            return skillsNote;
        }

        public int getResumeScore() {
            return resumeScore;
        }

        public String getResumeNote() {
            return resumeNote;
        }

        public int getTasksScore() {
            return tasksScore;
        }

        public String getTasksNote() {
            return tasksNote;
        }

        public List<String> getPossessedSkills() {
            return possessedSkills;
        }

        public List<String> getMissingInDemandSkills() {
            return missingInDemandSkills;
        }

        public List<RecommendedTask> getSuggestedTasks() {
            return suggestedTasks;
        }

        public List<String> getNextSteps() {
            return nextSteps;
        }

        public List<JobMatchResult> getRankedJobs() {
            return rankedJobs;
        }
    }

    /**
     * Calculates transparent placement readiness score (0-100%) based strictly on available student data.
     * Formula Breakdown:
     * 1. Academic & Profile Eligibility: up to 25% (Department, Year, CGPA recorded)
     * 2. Confirmed Skills Portfolio: up to 25% (breadth and depth of confirmed skills)
     * 3. Resume Completeness & ATS Checks: up to 25% (automated structural score)
     * 4. Milestone Task Execution: up to 25% (completion ratio of preparation goals)
     */
    public static ReadinessReport generateReadinessReport(
            Student student,
            List<Map<String, Object>> skillsList,
            ResumeInfo resumeInfo,
            List<Map<String, Object>> tasksList,
            List<Map<String, Object>> allJobs
    ) {
        // --- 1. Academic & Profile Readiness (Max 25%) ---
        int academic = 0;
        List<String> academicItems = new ArrayList<>();
        if (student != null) {
            if (student.getDepartment() != null && !student.getDepartment().isBlank()) {
                academic += 8;
                academicItems.add("Branch declared (" + student.getDepartment() + ")");
            }
            if (student.getYearOfStudy() > 0) {
                academic += 7;
                academicItems.add("Year of study declared (Year " + student.getYearOfStudy() + ")");
            }
            if (student.getCgpa() != null && student.getCgpa() > 0.0) {
                academic += 10;
                academicItems.add(String.format("CGPA recorded (%.2f)", student.getCgpa()));
            }
        }
        String academicNote = academicItems.isEmpty()
                ? "Academic profile incomplete. Update branch, year, and CGPA to verify campus drive eligibility."
                : String.join(", ", academicItems);

        // --- 2. Technical Skills Portfolio Readiness (Max 25%) ---
        List<String> confirmedSkills = new ArrayList<>();
        if (skillsList != null) {
            for (Map<String, Object> s : skillsList) {
                String sName = (String) s.get("skillName");
                if (sName != null && !sName.isBlank()) {
                    confirmedSkills.add(sName.trim());
                }
            }
        }
        int skillsScore = Math.min(25, confirmedSkills.size() * 5); // 5 skills = 25%
        String skillsNote = confirmedSkills.isEmpty()
                ? "No technical skills recorded yet. Add your programming languages and frameworks."
                : confirmedSkills.size() + " technical skills verified in your profile portfolio.";

        // --- 3. Resume Completeness & ATS Screening (Max 25%) ---
        int resumeScore = 0;
        String resumeNote;
        if (resumeInfo != null && resumeInfo.isUploaded()) {
            resumeScore = (int) Math.round((resumeInfo.getScore() / 100.0) * 25.0);
            resumeNote = "Resume verified (" + resumeInfo.getOriginalFileName() + ") with ATS score " + resumeInfo.getScore() + "/100.";
        } else {
            resumeNote = "No resume uploaded. Upload a PDF or DOCX resume to verify document structure and earn up to 25%.";
        }

        // --- 4. Milestone Task Execution (Max 25%) ---
        int totalTasks = tasksList != null ? tasksList.size() : 0;
        int completedTasks = 0;
        if (tasksList != null) {
            for (Map<String, Object> t : tasksList) {
                if ("Completed".equalsIgnoreCase((String) t.get("status"))) {
                    completedTasks++;
                }
            }
        }
        int tasksScore = 0;
        String tasksNote;
        if (totalTasks > 0) {
            tasksScore = (int) Math.round((completedTasks * 25.0) / totalTasks);
            tasksNote = completedTasks + " of " + totalTasks + " preparation milestones completed.";
        } else {
            tasksNote = "No milestones scheduled. Add practice goals (e.g. coding problems, mock tests) to earn 25%.";
        }

        int overall = Math.min(100, academic + skillsScore + resumeScore + tasksScore);

        // --- 5. Job Matching & Skills Gap Analysis ---
        Set<String> resumeSkills = new HashSet<>();
        if (resumeInfo != null && resumeInfo.getExtractedSkills() != null) {
            for (String rSkill : resumeInfo.getExtractedSkills()) {
                resumeSkills.add(rSkill.trim().toLowerCase());
            }
        }

        List<JobMatchResult> matchResults = new ArrayList<>();
        Map<String, Integer> inDemandMissingMap = new LinkedHashMap<>();

        if (allJobs != null) {
            for (Map<String, Object> job : allJobs) {
                JobMatchResult res = evaluateJobMatch(job, confirmedSkills, resumeSkills);
                matchResults.add(res);

                for (String missing : res.getMissingRequiredSkills()) {
                    inDemandMissingMap.put(missing, inDemandMissingMap.getOrDefault(missing, 0) + 1);
                }
            }
        }

        // Sort jobs by match percentage descending
        matchResults.sort(Comparator.comparingInt(JobMatchResult::getMatchPercent).reversed());

        // Identify top missing in-demand skills
        List<Map.Entry<String, Integer>> sortedMissing = new ArrayList<>(inDemandMissingMap.entrySet());
        sortedMissing.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        List<String> topMissingSkills = new ArrayList<>();
        List<RecommendedTask> suggestedTasks = new ArrayList<>();

        for (int i = 0; i < Math.min(5, sortedMissing.size()); i++) {
            String skill = sortedMissing.get(i).getKey();
            topMissingSkills.add(skill);
            suggestedTasks.addAll(generateTasksForSkill(skill));
        }

        // --- 6. Formulate Actionable Next Steps ---
        List<String> nextSteps = new ArrayList<>();
        if (student == null || student.getCgpa() == null || student.getCgpa() == 0.0) {
            nextSteps.add("Update your CGPA in your profile to verify minimum cut-off eligibility for top recruiters.");
        }
        if (resumeInfo == null || !resumeInfo.isUploaded()) {
            nextSteps.add("Upload your latest technical resume (PDF/DOCX) for automated screening analysis.");
        } else if (resumeInfo.getScore() < 70) {
            nextSteps.add("Improve your resume structure: ensure all standard sections (Projects, Technical Skills, Education) are evident.");
        }
        if (confirmedSkills.size() < 3) {
            nextSteps.add("Add at least 3 to 5 core technical skills to increase recruiter matching across active job drives.");
        }
        if (!topMissingSkills.isEmpty()) {
            nextSteps.add("Prioritize learning in-demand skills for target jobs: " + String.join(", ", topMissingSkills) + ".");
        }
        if (totalTasks == 0 || completedTasks < totalTasks) {
            nextSteps.add("Complete your pending preparation tasks before scheduled campus hiring deadlines.");
        }

        return new ReadinessReport(
                overall,
                academic,
                academicNote,
                skillsScore,
                skillsNote,
                resumeScore,
                resumeNote,
                tasksScore,
                tasksNote,
                confirmedSkills,
                topMissingSkills,
                suggestedTasks,
                nextSteps,
                matchResults
        );
    }

    /**
     * Evaluates a single job's skill matching against student's confirmed skills and resume text.
     */
    public static JobMatchResult evaluateJobMatch(
            Map<String, Object> job,
            List<String> studentConfirmedSkills,
            Set<String> resumeDetectedSkillsLower
    ) {
        long jobId = job.get("id") instanceof Number ? ((Number) job.get("id")).longValue() : 0L;

        // Parse required skills
        List<String> required = parseSkillList(job.get("requiredSkills"));
        if (required.isEmpty()) {
            required = parseSkillList(job.get("skills"));
        }

        // Parse preferred skills
        List<String> preferred = parseSkillList(job.get("preferredSkills"));

        // Build confirmed skill lookup set
        Set<String> confirmedLower = new HashSet<>();
        for (String s : studentConfirmedSkills) {
            if (s != null) confirmedLower.add(s.trim().toLowerCase());
        }

        List<String> matchedReq = new ArrayList<>();
        List<String> missingReq = new ArrayList<>();
        List<String> resumeEvidenced = new ArrayList<>();

        for (String req : required) {
            if (isSkillMatched(req, confirmedLower)) {
                matchedReq.add(req);
            } else {
                missingReq.add(req);
                if (isSkillMatched(req, resumeDetectedSkillsLower)) {
                    resumeEvidenced.add(req);
                }
            }
        }

        List<String> matchedPref = new ArrayList<>();
        List<String> missingPref = new ArrayList<>();
        for (String pref : preferred) {
            if (isSkillMatched(pref, confirmedLower)) {
                matchedPref.add(pref);
            } else {
                missingPref.add(pref);
            }
        }

        int matchPercent;
        String explanation;
        if (required.isEmpty()) {
            matchPercent = 100;
            explanation = "No specific required skills specified by recruiter; eligible for general screening.";
        } else {
            matchPercent = (int) Math.round((matchedReq.size() * 100.0) / required.size());
            explanation = matchedReq.size() + " of " + required.size() + " required skills matched (" + matchPercent + "%).";
        }

        // Recommend actionable milestones for missing required skills
        List<RecommendedTask> tasks = new ArrayList<>();
        for (String missing : missingReq) {
            tasks.addAll(generateTasksForSkill(missing));
        }

        return new JobMatchResult(
                jobId,
                job,
                required,
                preferred,
                matchedReq,
                missingReq,
                matchedPref,
                missingPref,
                resumeEvidenced,
                matchPercent,
                explanation,
                tasks
        );
    }

    /**
     * Splits comma or semicolon separated skills into a trimmed, distinct list.
     */
    public static List<String> parseSkillList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String str = raw.toString().trim();
        if (str.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> set = new LinkedHashSet<>();
        String[] parts = str.split("[,;/]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * Flexible matching between job requirement and student skill set.
     */
    private static boolean isSkillMatched(String targetSkill, Set<String> studentSkillsLower) {
        if (targetSkill == null || targetSkill.isBlank()) {
            return false;
        }
        String tLower = targetSkill.toLowerCase().trim();

        // Direct containment
        if (studentSkillsLower.contains(tLower)) {
            return true;
        }

        for (String s : studentSkillsLower) {
            if (s.equals(tLower)) return true;
            if (tLower.contains(s) || s.contains(tLower)) return true;

            // Common aliases
            if (isAliasMatch(tLower, s)) return true;
        }
        return false;
    }

    private static boolean isAliasMatch(String a, String b) {
        if (a.contains("java") && !a.contains("javascript") && b.contains("java") && !b.contains("javascript")) return true;
        if (a.contains("sql") && b.contains("sql")) return true;
        if (a.contains("react") && b.contains("react")) return true;
        if (a.contains("python") && b.contains("python")) return true;
        if (a.contains("spring") && b.contains("spring")) return true;
        if (a.contains("git") && b.contains("git")) return true;
        if (a.contains("rest") && b.contains("rest")) return true;
        if (a.contains("dsa") || a.contains("data structure") || a.contains("algorithm")) {
            if (b.contains("dsa") || b.contains("data structure") || b.contains("algorithm")) return true;
        }
        if (a.contains("test") || a.contains("qa") || a.contains("selenium")) {
            if (b.contains("test") || b.contains("qa") || b.contains("selenium")) return true;
        }
        if (a.contains("docker") && b.contains("docker")) return true;
        if (a.contains("aws") && b.contains("aws")) return true;
        return false;
    }

    /**
     * Generates practical, actionable learning tasks for a missing skill.
     */
    public static List<RecommendedTask> generateTasksForSkill(String skill) {
        List<RecommendedTask> tasks = new ArrayList<>();
        if (skill == null || skill.isBlank()) {
            return tasks;
        }

        String sLower = skill.toLowerCase().trim();
        LocalDate due = LocalDate.now().plusDays(7);
        String dueStr = due.toString();

        if (sLower.contains("java") && !sLower.contains("javascript")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Master Java Collections Framework & Stream API",
                    "Practice ArrayList, HashMap, ConcurrentHashMap and functional programming using Streams.",
                    dueStr
            ));
            tasks.add(new RecommendedTask(
                    skill,
                    "Implement Java Multithreading & Synchronization",
                    "Build a producer-consumer model using ExecutorService and BlockingQueue.",
                    due.plusDays(7).toString()
            ));
        } else if (sLower.contains("sql") || sLower.contains("database")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Practice Advanced SQL Joins and Aggregations",
                    "Solve 10 SQL practice problems covering INNER/OUTER joins, GROUP BY, and HAVING clauses.",
                    dueStr
            ));
            tasks.add(new RecommendedTask(
                    skill,
                    "Study Database Indexing and Query Tuning",
                    "Understand B-tree indexes, execution plans, and normalization principles.",
                    due.plusDays(7).toString()
            ));
        } else if (sLower.contains("spring")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Build a RESTful CRUD Microservice with Spring Boot",
                    "Create a Spring Boot service with JPA repository, dependency injection, and Swagger documentation.",
                    dueStr
            ));
        } else if (sLower.contains("rest")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Design and Document REST API Endpoints",
                    "Create RESTful resources conforming to HTTP status code conventions and test via Postman.",
                    dueStr
            ));
        } else if (sLower.contains("python")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Build Python Data Processing & Automation Script",
                    "Implement Python file parsing, data transformations, and REST API integration.",
                    dueStr
            ));
        } else if (sLower.contains("react") || sLower.contains("frontend")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Build Interactive Web Dashboard using React Hooks",
                    "Create a modern user interface utilizing useState, useEffect, and component props.",
                    dueStr
            ));
        } else if (sLower.contains("dsa") || sLower.contains("algorithm") || sLower.contains("structures")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Solve 10 Core Data Structures Problems",
                    "Practice Binary Trees, Dynamic Programming, and Graph traversal on LeetCode.",
                    dueStr
            ));
        } else if (sLower.contains("git")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Practice Git Branching & Merge Conflict Resolution",
                    "Simulate multi-developer feature branches, pull requests, and rebasing in GitHub.",
                    dueStr
            ));
        } else if (sLower.contains("docker") || sLower.contains("devops") || sLower.contains("cloud") || sLower.contains("aws")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Containerize and Deploy Application using Docker",
                    "Write a production Dockerfile and containerize a web application locally.",
                    dueStr
            ));
        } else if (sLower.contains("qa") || sLower.contains("test") || sLower.contains("selenium")) {
            tasks.add(new RecommendedTask(
                    skill,
                    "Write Automated Regression Test Suite with Selenium",
                    "Automate end-to-end user login and form validation using Selenium WebDriver and JUnit.",
                    dueStr
            ));
        } else {
            tasks.add(new RecommendedTask(
                    skill,
                    "Hands-on Practice & Project for " + skill,
                    "Review official documentation, complete code exercises, and build a working mini-project with " + skill + ".",
                    dueStr
            ));
        }

        return tasks;
    }
}
