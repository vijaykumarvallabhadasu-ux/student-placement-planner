<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map, java.util.Set, com.placementplanner.model.Student, com.placementplanner.model.ResumeInfo, com.placementplanner.service.PlacementIntelligenceService.ReadinessReport, com.placementplanner.service.PlacementIntelligenceService.JobMatchResult, com.placementplanner.service.PlacementIntelligenceService.RecommendedTask" %>
<%@ include file="_header.jspf" %>

<%
    Student currentStudent = (Student) session.getAttribute("student");
    String studentName = (currentStudent != null && currentStudent.getName() != null)
            ? currentStudent.getName() : "Student";

    int totalJobs = request.getAttribute("totalJobs") != null ? (Integer) request.getAttribute("totalJobs") : 0;
    int appliedCount = request.getAttribute("appliedCount") != null ? (Integer) request.getAttribute("appliedCount") : 0;
    int totalSkills = request.getAttribute("totalSkills") != null ? (Integer) request.getAttribute("totalSkills") : 0;
    int totalTasks = request.getAttribute("totalTasks") != null ? (Integer) request.getAttribute("totalTasks") : 0;
    int completedTasks = request.getAttribute("completedTasks") != null ? (Integer) request.getAttribute("completedTasks") : 0;
    int progressPercent = request.getAttribute("progressPercent") != null ? (Integer) request.getAttribute("progressPercent") : 0;

    List<Map<String, Object>> recentJobs = (List<Map<String, Object>>) request.getAttribute("recentJobs");
    List<Map<String, Object>> skills = (List<Map<String, Object>>) request.getAttribute("skills");
    List<Map<String, Object>> tasks = (List<Map<String, Object>>) request.getAttribute("tasks");
    Set<Long> appliedJobIds = (Set<Long>) request.getAttribute("appliedJobIds");

    ResumeInfo resumeInfo = (ResumeInfo) request.getAttribute("resumeInfo");
    boolean hasResume = resumeInfo != null && resumeInfo.isUploaded();
    int atsScore = hasResume ? resumeInfo.getScore() : 0;

    ReadinessReport report = (ReadinessReport) request.getAttribute("readinessReport");
    int overallReadiness = report != null ? report.getOverallScore() : 0;
    int academicScore = report != null ? report.getAcademicScore() : 0;
    int skillsCompScore = report != null ? report.getSkillsScore() : 0;
    int resumeCompScore = report != null ? report.getResumeScore() : 0;
    int tasksCompScore = report != null ? report.getTasksScore() : 0;

    List<String> possessedSkills = report != null ? report.getPossessedSkills() : java.util.Collections.emptyList();
    List<String> missingSkills = report != null ? report.getMissingInDemandSkills() : java.util.Collections.emptyList();
    List<RecommendedTask> suggestedTasks = report != null ? report.getSuggestedTasks() : java.util.Collections.emptyList();
    List<String> nextSteps = report != null ? report.getNextSteps() : java.util.Collections.emptyList();
    List<JobMatchResult> rankedJobs = report != null ? report.getRankedJobs() : java.util.Collections.emptyList();
%>

<!-- Dashboard Welcome Banner -->
<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">CAREER COMMAND CENTER</span>
        <h1>Welcome back, <%= studentName %>! &#128075;</h1>
        <p>Monitor your placement readiness, complete interview preparation milestones, and discover matching campus hiring opportunities.</p>
        
        <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 14px; align-items: center;">
            <span class="tag-badge branch">
                &#128218; Branch: <%= (currentStudent != null && currentStudent.getDepartment() != null) ? currentStudent.getDepartment() : "Not Specified" %>
            </span>
            <span class="tag-badge location">
                &#127979; <%= (currentStudent != null && currentStudent.getYearOfStudy() > 0) ? "Year " + currentStudent.getYearOfStudy() : "Year 4" %>
            </span>
            <span class="tag-badge cgpa">
                &#11088; CGPA: <%= (currentStudent != null && currentStudent.getCgpa() != null && currentStudent.getCgpa() > 0.0) ? String.format("%.2f", currentStudent.getCgpa()) : "Not Set" %>
            </span>
            <a href="${pageContext.request.contextPath}/profile" style="font-size: 0.82rem; font-weight: 700; color: var(--primary); text-decoration: underline; margin-left: 6px;">
                &#9998; Edit Academic Info
            </a>
        </div>
    </div>
    <div class="hero-actions">
        <a href="${pageContext.request.contextPath}/jobs" class="btn primary">
            &#128188; Explore Jobs
        </a>
        <a href="${pageContext.request.contextPath}/profile" class="btn secondary">
            &#128100; My Profile
        </a>
    </div>
</section>

<!-- Feedback Alerts -->
<% if ("1".equals(request.getParameter("resumeSuccess"))) { %>
    <div class="alert success" role="status" style="margin-bottom: 24px;">
        <span>&#10003; Resume verified successfully! Your automated ATS readiness score is <strong><%= atsScore %>/100</strong>.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("taskAdded"))) { %>
    <div class="alert success" role="status" style="margin-bottom: 24px;">
        <span>&#10003; Preparation milestone scheduled in Oracle Database! Track it in your preparation milestones below.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("taskDuplicate"))) { %>
    <div class="alert info" role="status" style="margin-bottom: 24px;">
        <span>&#8505; This task is already scheduled in your list. Duplicate milestones are prevented.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("taskUpdated"))) { %>
    <div class="alert success" role="status" style="margin-bottom: 24px;">
        <span>&#10003; Milestone status updated successfully! Your progress score has been refreshed.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("taskDeleted"))) { %>
    <div class="alert success" role="status" style="margin-bottom: 24px;">
        <span>&#10003; Milestone removed from your preparation plan.</span>
    </div>
<% } %>

<% if (request.getParameter("resumeError") != null && !request.getParameter("resumeError").isBlank()) { %>
    <div class="alert error" role="alert" style="margin-bottom: 24px;">
        <span>&#9888; <%= request.getParameter("resumeError") %></span>
    </div>
<% } %>

<% if (request.getAttribute("dbError") != null) { %>
    <div class="alert error" role="alert" style="margin-bottom: 24px;">
        <span>&#9888; <%= request.getAttribute("dbError") %></span>
    </div>
<% } %>

<% if (currentStudent != null && (currentStudent.getCgpa() == null || currentStudent.getCgpa() == 0.0)) { %>
    <div class="alert info" role="status" style="margin-bottom: 24px;">
        <span>
            &#128161; <strong>Academic Profile Tip:</strong> You haven't added your CGPA yet. 
            <a href="${pageContext.request.contextPath}/profile" style="color: var(--primary); text-decoration: underline; font-weight: 700;">Update your CGPA now</a> 
            to verify if you meet the eligibility cut-off for active campus drives.
        </span>
    </div>
<% } %>

<!-- 4 Key Database Metric Cards -->
<section class="stats-grid">
    <div class="stat-card">
        <div class="stat-icon blue">&#128188;</div>
        <div class="stat-info">
            <div class="stat-value"><%= totalJobs %></div>
            <div class="stat-label">Available Jobs</div>
        </div>
    </div>

    <div class="stat-card">
        <div class="stat-icon teal">&#128221;</div>
        <div class="stat-info">
            <div class="stat-value"><%= appliedCount %></div>
            <div class="stat-label">Jobs Applied</div>
        </div>
    </div>

    <div class="stat-card">
        <div class="stat-icon purple">&#9989;</div>
        <div class="stat-info">
            <div class="stat-value"><%= completedTasks %> / <%= totalTasks %></div>
            <div class="stat-label">Tasks Completed</div>
        </div>
    </div>

    <div class="stat-card">
        <div class="stat-icon amber">&#128218;</div>
        <div class="stat-info">
            <div class="stat-value"><%= totalSkills %></div>
            <div class="stat-label">Skills Tracked</div>
        </div>
    </div>
</section>

<!-- INTELLIGENT PLACEMENT READINESS DASHBOARD (Auditable Formula Breakdown) -->
<section class="panel" style="border: 2px solid var(--primary-light); background: linear-gradient(135deg, var(--card-bg) 0%, var(--bg-subtle) 100%);">
    <div class="panel-title" style="margin-bottom: 16px;">
        <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-size: 1.25rem;">&#127919;</span>
            <span style="font-size: 1.1rem; font-weight: 800; color: var(--text-primary);">Placement Readiness Intelligence</span>
        </div>
        <span class="badge <%= overallReadiness >= 75 ? "badge-completed" : (overallReadiness >= 50 ? "badge-workmode" : "badge-expired") %>" style="font-size: 0.85rem; padding: 4px 12px;">
            <%= overallReadiness >= 75 ? "High Placement Readiness" : (overallReadiness >= 50 ? "Moderate Placement Readiness" : "Action Required") %>
        </span>
    </div>

    <div style="display: grid; grid-template-columns: 200px 1fr; gap: 24px; align-items: center; margin-bottom: 20px;">
        <!-- Circular / Big Percentage Display -->
        <div style="text-align: center; padding: 18px; background: var(--card-bg); border-radius: var(--radius-lg); border: 1px solid var(--border-color); box-shadow: var(--shadow-sm);">
            <div style="font-size: 2.8rem; font-weight: 900; line-height: 1; color: var(--primary);">
                <%= overallReadiness %>%
            </div>
            <div style="font-size: 0.82rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; margin-top: 6px; letter-spacing: 0.5px;">
                Readiness Score
            </div>
            <div style="font-size: 0.72rem; color: var(--text-muted); margin-top: 4px;">
                Based on 4 Verified Pillars
            </div>
        </div>

        <!-- Transparent Formula Documentation -->
        <div>
            <h3 style="font-size: 0.98rem; margin-bottom: 6px; color: var(--text-primary);">Documented Readiness Calculation Formula</h3>
            <p style="font-size: 0.84rem; color: var(--text-secondary); margin-bottom: 12px; line-height: 1.5;">
                Your readiness index is calculated dynamically without artificial estimation:
                <br>
                <code>Readiness = Academic Eligibility (max 25%) + Confirmed Skills (max 25%) + Resume Completeness (max 25%) + Milestones Completed (max 25%)</code>.
            </p>

            <div class="progress-bar-track" style="height: 10px; margin-bottom: 10px;">
                <div class="progress-bar-fill" style="width: <%= overallReadiness %>%; background: linear-gradient(90deg, var(--primary), var(--accent-teal));"></div>
            </div>
            <div style="font-size: 0.78rem; color: var(--text-muted);">
                Note: Numerical readiness indicates profile completeness for campus screening and does not guarantee job selection.
            </div>
        </div>
    </div>

    <!-- 4 Transparent Component Cards -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 14px;">
        <!-- Pillar 1: Academic & Profile -->
        <div style="background: var(--card-bg); padding: 12px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <strong style="font-size: 0.86rem; color: var(--text-primary);">&#127891; 1. Academic Eligibility</strong>
                <span style="font-weight: 800; font-size: 0.88rem; color: var(--primary);"><%= academicScore %>/25%</span>
            </div>
            <p style="font-size: 0.76rem; color: var(--text-secondary); margin: 0; line-height: 1.4;">
                <%= report != null ? report.getAcademicNote() : "Branch, year, and CGPA." %>
            </p>
        </div>

        <!-- Pillar 2: Technical Skills -->
        <div style="background: var(--card-bg); padding: 12px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <strong style="font-size: 0.86rem; color: var(--text-primary);">&#128218; 2. Skills Portfolio</strong>
                <span style="font-weight: 800; font-size: 0.88rem; color: var(--accent-purple);"><%= skillsCompScore %>/25%</span>
            </div>
            <p style="font-size: 0.76rem; color: var(--text-secondary); margin: 0; line-height: 1.4;">
                <%= report != null ? report.getSkillsNote() : "Confirmed technical skills." %>
            </p>
        </div>

        <!-- Pillar 3: Resume Screening -->
        <div style="background: var(--card-bg); padding: 12px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <strong style="font-size: 0.86rem; color: var(--text-primary);">&#128196; 3. Resume ATS Screening</strong>
                <span style="font-weight: 800; font-size: 0.88rem; color: var(--accent-teal);"><%= resumeCompScore %>/25%</span>
            </div>
            <p style="font-size: 0.76rem; color: var(--text-secondary); margin: 0; line-height: 1.4;">
                <%= report != null ? report.getResumeNote() : "Automated structure check." %>
            </p>
        </div>

        <!-- Pillar 4: Preparation Milestones -->
        <div style="background: var(--card-bg); padding: 12px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <strong style="font-size: 0.86rem; color: var(--text-primary);">&#9989; 4. Preparation Milestones</strong>
                <span style="font-weight: 800; font-size: 0.88rem; color: var(--accent-amber);"><%= tasksCompScore %>/25%</span>
            </div>
            <p style="font-size: 0.76rem; color: var(--text-secondary); margin: 0; line-height: 1.4;">
                <%= report != null ? report.getTasksNote() : "Milestones completed." %>
            </p>
        </div>
    </div>
</section>

<!-- SKILLS GAP ANALYSIS & 1-CLICK LEARNING RECOMMENDATIONS -->
<section class="panel" style="margin-top: 24px;">
    <div class="panel-title">
        <span>&#128269; Skills Portfolio vs. Target Jobs Gap Analysis</span>
        <a href="${pageContext.request.contextPath}/skills" class="btn sm secondary">Manage Skills</a>
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
        <!-- Left: Possessed Skills -->
        <div>
            <h4 style="font-size: 0.88rem; color: var(--text-muted); text-transform: uppercase; margin-bottom: 10px;">
                &#9989; Skills You Possess (<%= possessedSkills.size() %>)
            </h4>
            <% if (possessedSkills.isEmpty()) { %>
                <div class="empty-state" style="padding: 16px;">
                    <p style="font-size: 0.85rem; margin-bottom: 8px;">No technical skills recorded yet in your profile.</p>
                    <a href="${pageContext.request.contextPath}/skills" class="btn sm primary">+ Add Skills</a>
                </div>
            <% } else { %>
                <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                    <% for (String pSkill : possessedSkills) { %>
                        <span class="tag-badge" style="background: #e8f5e9; color: #2e7d32; border: 1px solid #c8e6c9; font-weight: 700;">
                            &#10003; <%= pSkill %>
                        </span>
                    <% } %>
                </div>
            <% } %>
        </div>

        <!-- Right: Missing In-Demand Skills -->
        <div>
            <h4 style="font-size: 0.88rem; color: var(--text-muted); text-transform: uppercase; margin-bottom: 10px;">
                &#9888; Top Skills In-Demand for Target Roles
            </h4>
            <% if (missingSkills.isEmpty()) { %>
                <p style="font-size: 0.85rem; color: var(--text-secondary);">
                    Great job! You have verified skills covering all currently active recruitment requirements.
                </p>
            <% } else { %>
                <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                    <% for (String mSkill : missingSkills) { %>
                        <span class="tag-badge" style="background: #fff3e0; color: #e65100; border: 1px solid #ffe0b2; font-weight: 700;">
                            + <%= mSkill %>
                        </span>
                    <% } %>
                </div>
            <% } %>
        </div>
    </div>

    <!-- Recommended Actionable Learning Tasks with 1-Click Addition -->
    <% if (suggestedTasks != null && !suggestedTasks.isEmpty()) { %>
        <div style="margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--border-color);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <div>
                    <h4 style="font-size: 0.94rem; margin: 0; color: var(--text-primary);">&#128640; Recommended Learning Milestones</h4>
                    <p style="font-size: 0.8rem; color: var(--text-muted); margin: 2px 0 0 0;">
                        Targeted preparation goals generated specifically for your missing skills. Add them directly to your schedule with 1 click.
                    </p>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 12px;">
                <% for (RecommendedTask rTask : suggestedTasks) { %>
                    <div style="background: var(--bg-subtle); padding: 12px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color); display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 4px;">
                                <strong style="font-size: 0.88rem; color: var(--text-primary);"><%= rTask.getTitle() %></strong>
                                <span class="tag-badge branch" style="font-size: 0.72rem; padding: 2px 6px;"><%= rTask.getSkill() %></span>
                            </div>
                            <p style="font-size: 0.8rem; color: var(--text-secondary); margin: 0 0 10px 0; line-height: 1.4;">
                                <%= rTask.getDescription() %>
                            </p>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: auto;">
                            <span style="font-size: 0.74rem; color: var(--text-muted);">Due: <%= rTask.getSuggestedDueDate() %></span>
                            <form method="post" action="${pageContext.request.contextPath}/tasks" style="display: inline;">
                                <input type="hidden" name="action" value="add">
                                <input type="hidden" name="title" value="<%= rTask.getTitle() %>">
                                <input type="hidden" name="dueDate" value="<%= rTask.getSuggestedDueDate() %>">
                                <input type="hidden" name="redirect" value="dashboard">
                                <button type="submit" class="btn sm primary" style="font-size: 0.78rem; padding: 4px 10px;">
                                    + Add to My Tasks
                                </button>
                            </form>
                        </div>
                    </div>
                <% } %>
            </div>
        </div>
    <% } %>
</section>

<!-- Multi-Column Layout -->
<div class="dashboard-columns">

    <!-- Left Column: Upcoming Tasks & Best Matching Jobs -->
    <div>
        <!-- Upcoming Tasks Section -->
        <div class="panel">
            <div class="panel-title">
                <span>&#128221; Scheduled Preparation Milestones</span>
                <a href="${pageContext.request.contextPath}/tasks" class="btn sm secondary">View All Tasks (<%= totalTasks %>)</a>
            </div>

            <% if (tasks == null || tasks.isEmpty()) { %>
                <div class="empty-state">
                    <div class="empty-icon">&#128221;</div>
                    <h3>No milestones scheduled yet</h3>
                    <p>Start organizing your coding practice, mock interviews, and resume reviews.</p>
                    <a href="${pageContext.request.contextPath}/tasks" class="btn primary sm">Add First Task</a>
                </div>
            <% } else { %>
                <div class="task-list">
                    <%
                        int displayedTaskCount = 0;
                        for (Map<String, Object> task : tasks) {
                            if (displayedTaskCount >= 4) break;
                            displayedTaskCount++;
                            boolean isDone = "Completed".equalsIgnoreCase((String) task.get("status"));
                    %>
                        <div class="task-item <%= isDone ? "completed" : "" %>">
                            <div class="task-left">
                                <form method="post" action="${pageContext.request.contextPath}/tasks" style="display:inline;">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="id" value="<%= task.get("id") %>">
                                    <input type="hidden" name="status" value="<%= isDone ? "Pending" : "Completed" %>">
                                    <input type="hidden" name="redirect" value="dashboard">
                                    <button type="submit" class="btn sm <%= isDone ? "secondary" : "primary" %>" title="Toggle completion">
                                        <%= isDone ? "&#10003; Done" : "Mark Done" %>
                                    </button>
                                </form>
                                <div>
                                    <div class="task-title"><%= task.get("title") %></div>
                                    <div class="task-meta">
                                        Due: <%= task.get("dueDate") != null ? task.get("dueDate") : "Flexible" %>
                                    </div>
                                </div>
                            </div>
                            <span class="badge <%= isDone ? "badge-completed" : "badge-pending" %>">
                                <%= isDone ? "Completed" : "Pending" %>
                            </span>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>

        <!-- Matching Opportunities Section (Ranked by Skill Match %) -->
        <div class="panel">
            <div class="panel-title">
                <span>&#128188; Top Matching Opportunities</span>
                <a href="${pageContext.request.contextPath}/jobs" class="btn sm secondary">View All Jobs (<%= totalJobs %>)</a>
            </div>

            <% if (rankedJobs == null || rankedJobs.isEmpty()) { %>
                <div class="empty-state">
                    <div class="empty-icon">&#128188;</div>
                    <h3>No job postings available</h3>
                    <p>Job listings will appear here once published by the placement cell.</p>
                </div>
            <% } else { %>
                <div class="jobs-grid">
                    <%
                        int jobLimit = 0;
                        for (JobMatchResult jMatch : rankedJobs) {
                            if (jobLimit >= 4) break;
                            jobLimit++;
                            Map<String, Object> job = jMatch.getJob();
                            long jId = jMatch.getJobId();
                            boolean hasApplied = appliedJobIds != null && appliedJobIds.contains(jId);
                            boolean isExpired = Boolean.TRUE.equals(job.get("isExpired"));
                            String jTitle = job.get("title") != null ? job.get("title").toString() : "Placement Opportunity";
                            String jCompany = job.get("company") != null ? job.get("company").toString() : "Campus Recruiter";
                            String jCategory = job.get("category") != null ? job.get("category").toString() : "Engineering";
                            String jLogo = "CO";
                            if (jCompany != null && !jCompany.isBlank()) {
                                jLogo = jCompany.substring(0, Math.min(2, jCompany.length())).toUpperCase();
                            }
                            int matchPct = jMatch.getMatchPercent();
                    %>
                        <div class="job-card">
                            <div>
                                <div class="job-card-header">
                                    <div class="company-logo"><%= jLogo %></div>
                                    <div class="job-card-title-group">
                                        <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px;">
                                            <h3><%= jTitle %></h3>
                                            <span class="badge <%= matchPct >= 75 ? "badge-completed" : (matchPct >= 40 ? "badge-pending" : "badge-expired") %>" style="font-size: 0.78rem;">
                                                <%= matchPct %>% Match
                                            </span>
                                        </div>
                                        <div style="display: flex; align-items: center; gap: 8px;">
                                            <span class="company-name"><%= jCompany %></span>
                                            <span class="tag-badge branch" style="font-size: 0.72rem; padding: 2px 6px;"><%= jCategory %></span>
                                        </div>
                                    </div>
                                </div>

                                <div class="job-tags" style="margin-top: 10px;">
                                    <span class="tag-badge location">&#128205; <%= job.get("location") %></span>
                                    <span class="tag-badge cgpa">&#127891; Min CGPA: <%= job.get("minCgpa") != null ? job.get("minCgpa") : "6.0" %></span>
                                </div>

                                <!-- Skill matching pill breakdown -->
                                <div style="margin: 10px 0; font-size: 0.78rem;">
                                    <div style="color: var(--text-muted); margin-bottom: 4px;">Skills Match Status:</div>
                                    <div style="display: flex; flex-wrap: wrap; gap: 4px;">
                                        <% for (String mSkill : jMatch.getMatchedRequiredSkills()) { %>
                                            <span style="background: #e8f5e9; color: #2e7d32; padding: 2px 6px; border-radius: var(--radius-sm); font-size: 0.74rem; font-weight: 700;">
                                                &#10003; <%= mSkill %>
                                            </span>
                                        <% } %>
                                        <% for (String misSkill : jMatch.getMissingRequiredSkills()) { %>
                                            <span style="background: #ffebee; color: #c62828; padding: 2px 6px; border-radius: var(--radius-sm); font-size: 0.74rem;">
                                                &#9888; <%= misSkill %>
                                            </span>
                                        <% } %>
                                    </div>
                                </div>
                            </div>

                            <div class="job-card-footer">
                                <span class="deadline-text">
                                    &#9200; <%= job.get("deadline") != null ? job.get("deadline") : "Ongoing" %>
                                </span>

                                <% if (hasApplied) { %>
                                    <span class="badge badge-applied">&#10003; Applied</span>
                                <% } else if (isExpired) { %>
                                    <span class="badge badge-expired">Closed</span>
                                <% } else if (Boolean.TRUE.equals(job.get("isExternal"))) { %>
                                    <a href="${pageContext.request.contextPath}/jobs" class="btn primary sm">
                                        View &amp; Apply &rarr;
                                    </a>
                                <% } else { %>
                                    <form method="post" action="${pageContext.request.contextPath}/apply">
                                        <input type="hidden" name="jobId" value="<%= jId %>">
                                        <button type="submit" class="btn primary sm">Apply Now</button>
                                    </form>
                                <% } %>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>
    </div>

    <!-- Right Column: Resume, Skills & Actionable Next Steps -->
    <div>
        <!-- Resume & ATS Readiness Card -->
        <div class="panel">
            <div class="panel-title">
                <span>&#128196; Resume &amp; ATS Score</span>
                <a href="${pageContext.request.contextPath}/profile#resume-hub" class="btn sm secondary">
                    <%= hasResume ? "Manage" : "Upload" %>
                </a>
            </div>

            <% if (hasResume) { %>
                <div style="display: flex; align-items: center; gap: 14px; background: var(--bg-subtle); padding: 12px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color); margin-bottom: 12px;">
                    <div class="resume-score-circle" style="width: 44px; height: 44px; font-size: 0.95rem;">
                        <span><%= atsScore %></span>
                        <small style="font-size: 0.55rem;">ATS</small>
                    </div>
                    <div style="flex: 1; min-width: 0;">
                        <div style="font-size: 0.9rem; font-weight: 700; color: var(--text-primary); text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">
                            <%= resumeInfo.getOriginalFileName() %>
                        </div>
                        <div style="display: flex; gap: 8px; align-items: center; margin-top: 2px;">
                            <span class="badge <%= "PASSED".equals(resumeInfo.getStatus().name()) ? "badge-completed" : "badge-pending" %>" style="font-size: 0.72rem; padding: 2px 8px;">
                                <%= resumeInfo.getStatus().getLabel() %>
                            </span>
                            <span style="font-size: 0.75rem; color: var(--text-muted);"><%= resumeInfo.getFormattedFileSize() %></span>
                        </div>
                    </div>
                </div>

                <div style="display: flex; gap: 8px;">
                    <a href="${pageContext.request.contextPath}/resume/view" target="_blank" class="btn sm secondary" style="flex: 1; text-align: center; justify-content: center;">
                        &#128065; View Resume
                    </a>
                    <a href="${pageContext.request.contextPath}/profile#resume-hub" class="btn sm primary" style="flex: 1; text-align: center; justify-content: center;">
                        &#8679; Replace
                    </a>
                </div>
            <% } else { %>
                <div class="empty-state" style="padding: 16px 12px;">
                    <div style="font-size: 1.8rem; margin-bottom: 6px;">&#128196;</div>
                    <h3 style="font-size: 0.95rem; margin-bottom: 4px;">No Resume Uploaded</h3>
                    <p style="font-size: 0.8rem; margin-bottom: 12px;">Upload PDF/DOCX to verify your placement screening readiness.</p>
                    <a href="${pageContext.request.contextPath}/profile#resume-hub" class="btn primary sm">
                        &#8679; Upload Resume Now
                    </a>
                </div>
            <% } %>
        </div>

        <!-- Actionable Next Steps to Improve Employability -->
        <div class="panel">
            <div class="panel-title">
                <span>&#128506; Employability Next Steps</span>
            </div>
            <% if (nextSteps == null || nextSteps.isEmpty()) { %>
                <p style="font-size: 0.85rem; color: var(--text-secondary);">
                    Your profile and preparation milestones are completely up-to-date! Continue applying for active campus drives.
                </p>
            <% } else { %>
                <div style="display: flex; flex-direction: column; gap: 12px;">
                    <%
                        int stepIdx = 0;
                        for (String step : nextSteps) {
                            stepIdx++;
                    %>
                        <div style="display: flex; gap: 12px; align-items: flex-start;">
                            <div style="width: 24px; height: 24px; border-radius: 50%; background: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.8rem; flex-shrink: 0;">
                                <%= stepIdx %>
                            </div>
                            <div style="font-size: 0.84rem; color: var(--text-primary); line-height: 1.4;">
                                <%= step %>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>

        <!-- Quick Links Panel -->
        <div class="panel">
            <div class="panel-title">
                <span>&#9881; Placement Navigation</span>
            </div>
            <div style="display: flex; flex-direction: column; gap: 8px;">
                <a href="${pageContext.request.contextPath}/skills" class="btn secondary sm" style="text-align: left; justify-content: flex-start;">
                    &#128218; Manage Technical Skills Portfolio
                </a>
                <a href="${pageContext.request.contextPath}/tasks" class="btn secondary sm" style="text-align: left; justify-content: flex-start;">
                    &#128221; Manage Daily Preparation Milestones
                </a>
                <a href="${pageContext.request.contextPath}/applications" class="btn secondary sm" style="text-align: left; justify-content: flex-start;">
                    &#128188; Track Submitted Applications (<%= appliedCount %>)
                </a>
            </div>
        </div>
    </div>

</div>

<%@ include file="_footer.jspf" %>
