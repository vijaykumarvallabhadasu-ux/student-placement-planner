<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map, java.util.Set, com.placementplanner.service.PlacementIntelligenceService.JobMatchResult, com.placementplanner.service.PlacementIntelligenceService.RecommendedTask" %>
<%@ include file="_header.jspf" %>

<%
    List<Map<String, Object>> jobs = (List<Map<String, Object>>) request.getAttribute("jobs");
    Set<Long> appliedJobIds = (Set<Long>) request.getAttribute("appliedJobIds");
    Map<Long, JobMatchResult> jobMatches = (Map<Long, JobMatchResult>) request.getAttribute("jobMatches");
    int jobTotal = jobs != null ? jobs.size() : 0;
%>

<!-- Hero Section -->
<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">CAMPUS &amp; INDUSTRY HIRING</span>
        <h1>Current Software Engineering Opportunities</h1>
        <p>Explore verified early-career and graduate roles across leading technology employers. Inspect required skills, verify your match percentage, and schedule preparation tasks.</p>
    </div>
    <div class="hero-actions">
        <a href="${pageContext.request.contextPath}/applications" class="btn secondary">
            &#128221; Track My Applications
        </a>
    </div>
</section>

<!-- Feedback Alerts -->
<% if ("1".equals(request.getParameter("applied"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Your application has been submitted successfully and saved to Oracle Database!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("externalRecorded"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Application submission tracked! You can view and update its recruitment status in the Applications section.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("duplicate"))) { %>
    <div class="alert info" role="status">
        <span>&#8505; You have already recorded an application for this position. Check your Applications page for updates.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("taskAdded"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Preparation milestone scheduled in your task list! View it on your Dashboard or Tasks page.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("taskDuplicate"))) { %>
    <div class="alert info" role="status">
        <span>&#8505; This learning milestone is already in your schedule. Duplicate tasks are prevented.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("expired"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Applications for this position are closed because the deadline has passed.</span>
    </div>
<% } %>

<!-- Search & Filtering Toolbar -->
<div class="search-filter-bar" style="flex-direction: column; align-items: stretch; gap: 14px;">
    <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center;">
        <div class="search-input-wrap" style="flex: 1; min-width: 240px;">
            <input
                type="text"
                id="jobSearchInput"
                placeholder="Search by role title, company name, required skill (e.g. Java, Python, React)..."
                aria-label="Search jobs">
        </div>

        <div class="filter-select-wrap">
            <select id="jobLocationFilter" aria-label="Filter by location">
                <option value="">All Locations</option>
                <option value="Hyderabad">Hyderabad</option>
                <option value="Bengaluru">Bengaluru</option>
                <option value="Pune">Pune</option>
                <option value="Chennai">Chennai</option>
                <option value="Noida">Noida / Gurugram</option>
                <option value="Pan-India">Pan-India</option>
            </select>
        </div>

        <div class="filter-select-wrap">
            <select id="jobWorkModeFilter" aria-label="Filter by work mode">
                <option value="">All Work Modes</option>
                <option value="Hybrid">Hybrid</option>
                <option value="On-Site">On-Site</option>
                <option value="Remote">Remote</option>
            </select>
        </div>

        <div id="jobsCountDisplay" style="font-size: 0.88rem; font-weight: 700; color: var(--text-muted); margin-left: auto;">
            Showing <%= jobTotal %> opportunities
        </div>
    </div>

    <!-- Category / Role Filter Pills -->
    <div style="display: flex; gap: 8px; flex-wrap: wrap; align-items: center;">
        <span style="font-size: 0.8rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Filter by Role:</span>
        <button type="button" class="btn sm secondary role-filter-btn active" data-role="all">All Roles</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="java">Java Developer</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="python">Python Developer</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="full-stack">Full-Stack</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="frontend">Frontend</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="backend">Backend</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="software developer">Software Engineer</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="qa">QA / Testing</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="data analyst">Data Analyst</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="ai">AI / ML</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="cybersecurity">Cybersecurity</button>
        <button type="button" class="btn sm secondary role-filter-btn" data-role="cloud">Cloud / DevOps</button>
    </div>
</div>

<!-- Empty State if no jobs in database -->
<% if (jobs == null || jobs.isEmpty()) { %>
    <div class="empty-state">
        <div class="empty-icon">&#128188;</div>
        <h2>No job listings available</h2>
        <p>There are currently no job postings in the database. Please check back later or contact your placement administrator.</p>
    </div>
<% } else { %>

    <!-- Filter Zero-Results Message -->
    <div id="noFilterResults" class="empty-state" style="display: none;">
        <div class="empty-icon">&#128269;</div>
        <h3>No matching opportunities found</h3>
        <p>Try adjusting your search keywords, role selection, or clearing location filters.</p>
    </div>

    <!-- Jobs Grid -->
    <div class="jobs-grid">
        <%
            for (Map<String, Object> job : jobs) {
                long jobId = (job.get("id") instanceof Number) ? ((Number) job.get("id")).longValue() : 0L;
                String title = (String) job.get("title");
                String company = (String) job.get("company");
                String location = (String) job.get("location");
                String description = (String) job.get("description");
                String eligibility = (String) job.get("eligibility");
                String category = (String) job.get("category");
                Object minCgpa = job.get("minCgpa");
                Object deadline = job.get("deadline");
                boolean isExpired = Boolean.TRUE.equals(job.get("isExpired"));
                boolean hasApplied = appliedJobIds != null && appliedJobIds.contains(jobId);

                boolean isExternal = Boolean.TRUE.equals(job.get("isExternal"));
                boolean isRecent = Boolean.TRUE.equals(job.get("isRecent"));
                String workMode = (String) job.get("workMode");
                String salary = (String) job.get("salary");
                String officialUrl = (String) job.get("officialUrl");
                String source = (String) job.get("source");
                String lastChecked = (String) job.get("lastChecked");

                String requiredSkills = (String) job.get("requiredSkills");
                String preferredSkills = (String) job.get("preferredSkills");
                String allSkills = (String) job.get("skills");

                JobMatchResult match = (jobMatches != null) ? jobMatches.get(jobId) : null;
                int matchPercent = (match != null) ? match.getMatchPercent() : 0;

                String logoInitials = "CO";
                if (company != null && !company.isBlank()) {
                    logoInitials = company.substring(0, Math.min(2, company.length())).toUpperCase();
                }
        %>
            <article
                class="job-card"
                data-title="<%= title != null ? title.toLowerCase() : "" %>"
                data-company="<%= company != null ? company.toLowerCase() : "" %>"
                data-location="<%= location != null ? location.toLowerCase() : "" %>"
                data-workmode="<%= workMode != null ? workMode.toLowerCase() : "" %>"
                data-category="<%= category != null ? category.toLowerCase() : (title != null ? title.toLowerCase() : "") %>"
                data-skills="<%= allSkills != null ? allSkills.toLowerCase() : "" %>"
                data-description="<%= description != null ? description.toLowerCase() : "" %>">

                <div>
                    <!-- Job Header -->
                    <div class="job-card-header">
                        <div class="company-logo"><%= logoInitials %></div>
                        <div class="job-card-title-group" style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px;">
                                <h2><%= title != null ? title : "Position" %></h2>
                                <div style="display: flex; gap: 6px; align-items: center;">
                                    <% if (match != null && loggedInStudent != null) { %>
                                        <span class="badge <%= matchPercent >= 75 ? "badge-completed" : (matchPercent >= 40 ? "badge-pending" : "badge-expired") %>" style="font-size: 0.8rem; font-weight: 800;">
                                            <%= matchPercent %>% Match
                                        </span>
                                    <% } %>
                                    <% if (isRecent) { %>
                                        <span class="badge-recent">&#9889; New</span>
                                    <% } %>
                                </div>
                            </div>
                            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                                <span class="company-name"><%= company != null ? company : "Campus Recruiter" %></span>
                                <% if (category != null && !category.isBlank()) { %>
                                    <span class="tag-badge branch" style="font-size: 0.74rem; padding: 2px 8px;"><%= category %></span>
                                <% } %>
                                <% if (isExternal) { %>
                                    <span class="job-portal-badge">&#127760; Direct Career Portal</span>
                                <% } else { %>
                                    <span class="job-portal-badge">&#127891; Campus Drive</span>
                                <% } %>
                            </div>
                        </div>
                    </div>

                    <!-- Job Body -->
                    <div class="job-card-body">
                        <div class="job-tags">
                            <span class="tag-badge location">&#128205; <%= location != null ? location : "On-Site" %></span>
                            <% if (workMode != null && !workMode.isBlank()) { %>
                                <span class="badge-workmode <%= workMode.toLowerCase() %>">&#128187; <%= workMode %></span>
                            <% } %>
                            <% if (salary != null && !salary.isBlank()) { %>
                                <span class="badge-salary">&#128176; <%= salary %></span>
                            <% } %>
                            <% if (minCgpa != null) { %>
                                <span class="tag-badge cgpa">&#127891; Min CGPA: <%= minCgpa %></span>
                            <% } %>
                            <% if (eligibility != null && !eligibility.isBlank()) { %>
                                <span class="tag-badge branch">&#128218; <%= eligibility %></span>
                            <% } %>
                        </div>

                        <!-- Required & Preferred Skills Layout -->
                        <div style="margin: 12px 0 10px 0; background: var(--bg-subtle); padding: 10px 12px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
                            <div style="font-size: 0.82rem; margin-bottom: 6px;">
                                <strong style="color: var(--text-primary);">&#128204; Required Skills:</strong>
                                <span style="color: var(--text-secondary);"><%= requiredSkills != null && !requiredSkills.isBlank() ? requiredSkills : allSkills %></span>
                            </div>
                            <% if (preferredSkills != null && !preferredSkills.isBlank()) { %>
                                <div style="font-size: 0.8rem; color: var(--text-muted);">
                                    <strong>Preferred Skills:</strong> <%= preferredSkills %>
                                </div>
                            <% } %>
                        </div>

                        <!-- Student Match Intelligence (when signed in) -->
                        <% if (match != null && loggedInStudent != null) { %>
                            <div style="margin-bottom: 12px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 10px 12px;">
                                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                                    <span style="font-size: 0.8rem; font-weight: 700; color: var(--text-primary);">
                                        Skill Match Analysis: <%= match.getCalculationExplanation() %>
                                    </span>
                                </div>

                                <div style="display: flex; flex-direction: column; gap: 6px; font-size: 0.78rem;">
                                    <!-- Matching skills -->
                                    <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap;">
                                        <span style="color: #2e7d32; font-weight: 700;">Matching:</span>
                                        <% if (match.getMatchedRequiredSkills().isEmpty()) { %>
                                            <span style="color: var(--text-muted); font-style: italic;">None of the required skills currently verified in your profile.</span>
                                        <% } else { %>
                                            <% for (String mSkill : match.getMatchedRequiredSkills()) { %>
                                                <span style="background: #e8f5e9; color: #2e7d32; padding: 2px 6px; border-radius: var(--radius-sm); font-weight: 700;">
                                                    &#10003; <%= mSkill %>
                                                </span>
                                            <% } %>
                                        <% } %>
                                    </div>

                                    <!-- Missing skills & 1-click learning plan -->
                                    <% if (!match.getMissingRequiredSkills().isEmpty()) { %>
                                        <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-top: 2px;">
                                            <span style="color: #c62828; font-weight: 700;">Missing:</span>
                                            <% for (String misSkill : match.getMissingRequiredSkills()) { %>
                                                <span style="background: #ffebee; color: #c62828; padding: 2px 6px; border-radius: var(--radius-sm);">
                                                    &#9888; <%= misSkill %>
                                                </span>
                                            <% } %>
                                        </div>

                                        <!-- Suggested Action Tasks -->
                                        <% if (!match.getRecommendedTasks().isEmpty()) { %>
                                            <div style="margin-top: 6px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap;">
                                                <span style="font-weight: 700; color: var(--text-muted);">Suggested Actions:</span>
                                                <% for (RecommendedTask rTask : match.getRecommendedTasks()) { %>
                                                    <form method="post" action="${pageContext.request.contextPath}/tasks" style="display: inline;">
                                                        <input type="hidden" name="action" value="add">
                                                        <input type="hidden" name="title" value="<%= rTask.getTitle() %>">
                                                        <input type="hidden" name="dueDate" value="<%= rTask.getSuggestedDueDate() %>">
                                                        <input type="hidden" name="redirect" value="jobs">
                                                        <button type="submit" class="btn sm secondary" style="font-size: 0.74rem; padding: 2px 8px;" title="<%= rTask.getDescription() %>">
                                                            + Add Task: <%= rTask.getSkill() %>
                                                        </button>
                                                    </form>
                                                <% } %>
                                            </div>
                                        <% } %>
                                    <% } %>
                                </div>
                            </div>
                        <% } %>

                        <p class="job-description">
                            <%= description != null && !description.isBlank()
                                    ? description
                                    : "No detailed job description provided. Contact recruiter for full JD." %>
                        </p>

                        <% if (source != null && !source.isBlank()) { %>
                            <div style="font-size: 0.74rem; color: var(--text-muted); margin-top: 8px;">
                                Source: <%= source %> &bull; Status: <%= lastChecked != null ? lastChecked : "Verified" %>
                            </div>
                        <% } %>
                    </div>
                </div>

                <!-- Job Footer Actions -->
                <div class="job-card-footer">
                    <span class="deadline-text">
                        &#9200; <%= deadline != null ? deadline : "Rolling Applications" %>
                    </span>

                    <div>
                        <% if (hasApplied) { %>
                            <span class="badge badge-applied">&#10003; Applied</span>
                        <% } else if (isExpired) { %>
                            <span class="badge badge-expired">Deadline Passed</span>
                        <% } else if (loggedInStudent == null) { %>
                            <a href="${pageContext.request.contextPath}/login" class="btn primary sm">
                                Sign In to Apply &rarr;
                            </a>
                        <% } else if (isExternal) { %>
                            <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                                <% if (officialUrl != null && !officialUrl.isBlank()) { %>
                                    <a href="<%= officialUrl %>" target="_blank" rel="noopener noreferrer" class="btn sm secondary" title="Open employer's official careers portal">
                                        &#128279; Open Official Portal &rarr;
                                    </a>
                                <% } %>
                                <form method="post" action="${pageContext.request.contextPath}/apply" style="display: inline;">
                                    <input type="hidden" name="jobId" value="<%= jobId %>">
                                    <input type="hidden" name="action" value="recordExternal">
                                    <button type="submit" class="btn primary sm" title="Track this external application in your dashboard">
                                        &#10003; Track Submission
                                    </button>
                                </form>
                            </div>
                        <% } else { %>
                            <form method="post" action="${pageContext.request.contextPath}/apply">
                                <input type="hidden" name="jobId" value="<%= jobId %>">
                                <button type="submit" class="btn primary sm">
                                    Apply Now &rarr;
                                </button>
                            </form>
                        <% } %>
                    </div>
                </div>

            </article>
        <% } %>
    </div>

<% } %>

<%@ include file="_footer.jspf" %>
