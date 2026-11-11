<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map" %>
<%@ include file="_header.jspf" %>

<%
    List<Map<String, Object>> jobs =
            (List<Map<String, Object>>) request.getAttribute("jobs");
    int jobTotal = jobs != null ? jobs.size() : 0;

    List<Map<String, Object>> applications =
            (List<Map<String, Object>>) request.getAttribute("applications");
    int appTotal = applications != null ? applications.size() : 0;
%>

<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">ADMINISTRATOR CONSOLE</span>
        <h1>Placement Drive &amp; Applicant Manager</h1>
        <p>Publish campus recruitment opportunities, review student applications, inspect candidate academic CGPA, and manage candidate hiring pipelines.</p>
    </div>
    <div class="hero-actions">
        <a href="#applicantsSection" class="btn secondary">
            &#128221; Review Applications (<%= appTotal %>)
        </a>
    </div>
</section>

<% if ("1".equals(request.getParameter("added"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Job opportunity published successfully and stored in Oracle Database!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("statusUpdated"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Candidate application status updated successfully!</span>
    </div>
<% } %>

<% if ("statusfailed".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Failed to update candidate status in Oracle Database.</span>
    </div>
<% } %>

<!-- Publish New Job Panel -->
<section class="panel">
    <div class="panel-title">
        <span>Publish a New Recruitment Opportunity</span>
    </div>

    <form method="post" action="${pageContext.request.contextPath}/admin/jobs">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 18px;">
            <div class="form-group">
                <label for="title">Job Title *</label>
                <input
                    type="text"
                    id="title"
                    name="title"
                    placeholder="e.g. Software Engineer Trainee"
                    required>
            </div>

            <div class="form-group">
                <label for="company">Company Name *</label>
                <input
                    type="text"
                    id="company"
                    name="company"
                    placeholder="e.g. TechNova Solutions"
                    required>
            </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 18px;">
            <div class="form-group">
                <label for="location">Location</label>
                <input
                    type="text"
                    id="location"
                    name="location"
                    placeholder="e.g. Hyderabad / Remote">
            </div>

            <div class="form-group">
                <label for="eligibility">Eligible Branches</label>
                <input
                    type="text"
                    id="eligibility"
                    name="eligibility"
                    placeholder="e.g. B.Tech (CSE, IT, ECE)">
            </div>

            <div class="form-group">
                <label for="minCgpa">Minimum CGPA (0 - 10)</label>
                <input
                    type="number"
                    id="minCgpa"
                    name="minCgpa"
                    min="0"
                    max="10"
                    step="0.01"
                    placeholder="e.g. 6.5">
            </div>
        </div>

        <div class="form-group">
            <label for="deadline">Application Deadline</label>
            <input
                type="date"
                id="deadline"
                name="deadline">
        </div>

        <div class="form-group">
            <label for="description">Job Description &amp; Role Details</label>
            <textarea
                id="description"
                name="description"
                rows="4"
                placeholder="Key responsibilities, technical requirements, and interview rounds..."></textarea>
        </div>

        <button type="submit" class="btn primary">
            &#128640; Publish Opportunity
        </button>
    </form>
</section>

<!-- Student Applicants Review Section -->
<section class="panel" id="applicantsSection">
    <div class="panel-title">
        <span>Candidate Applications Pipeline (<%= appTotal %>)</span>
    </div>

    <% if (applications == null || applications.isEmpty()) { %>
        <div class="empty-state">
            <div class="empty-icon">&#128221;</div>
            <h3>No student applications submitted yet</h3>
            <p>Student job applications will appear here in real-time as students apply for active placement openings.</p>
        </div>
    <% } else { %>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Candidate Name</th>
                        <th>College Email</th>
                        <th>Branch / CGPA</th>
                        <th>Job Applied For</th>
                        <th>Company</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Map<String, Object> app : applications) { 
                        String curStatus = (String) app.get("status");
                        if (curStatus == null) curStatus = "APPLIED";
                    %>
                        <tr>
                            <td>
                                <strong><%= app.get("studentName") %></strong>
                            </td>
                            <td>
                                <span style="font-size: 0.88rem; color: var(--text-muted);"><%= app.get("studentEmail") %></span>
                            </td>
                            <td>
                                <span class="tag-badge branch">&#128218; <%= app.get("department") %></span>
                                <% if (app.get("cgpa") != null) { %>
                                    <span class="tag-badge cgpa">&#11088; CGPA: <%= app.get("cgpa") %></span>
                                <% } %>
                            </td>
                            <td>
                                <strong><%= app.get("jobTitle") %></strong>
                            </td>
                            <td>
                                <span style="color: var(--text-secondary); font-weight: 600;"><%= app.get("company") %></span>
                            </td>
                            <td>
                                <span class="badge <%= "OFFERED".equalsIgnoreCase(curStatus) || "PLACED".equalsIgnoreCase(curStatus) ? "badge-completed" : "badge-applied" %>">
                                    <%= curStatus %>
                                </span>
                            </td>
                            <td>
                                <form method="post" action="${pageContext.request.contextPath}/admin/jobs" style="display: flex; gap: 6px; align-items: center;">
                                    <input type="hidden" name="action" value="updateStatus">
                                    <input type="hidden" name="applicationId" value="<%= app.get("id") %>">
                                    <select name="status" style="padding: 4px 8px; font-size: 0.82rem; height: 32px; border-radius: var(--radius-sm);">
                                        <option value="APPLIED" <%= "APPLIED".equalsIgnoreCase(curStatus) ? "selected" : "" %>>Applied</option>
                                        <option value="UNDER_REVIEW" <%= "UNDER_REVIEW".equalsIgnoreCase(curStatus) ? "selected" : "" %>>Reviewing</option>
                                        <option value="SHORTLISTED" <%= "SHORTLISTED".equalsIgnoreCase(curStatus) ? "selected" : "" %>>Shortlisted</option>
                                        <option value="INTERVIEW" <%= "INTERVIEW".equalsIgnoreCase(curStatus) ? "selected" : "" %>>Interview</option>
                                        <option value="OFFERED" <%= "OFFERED".equalsIgnoreCase(curStatus) ? "selected" : "" %>>Offered</option>
                                        <option value="REJECTED" <%= "REJECTED".equalsIgnoreCase(curStatus) ? "selected" : "" %>>Rejected</option>
                                    </select>
                                    <button type="submit" class="btn sm secondary" style="height: 32px; padding: 0 10px;">
                                        Save
                                    </button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    <% } %>
</section>

<!-- Published Opportunities Table -->
<section class="panel">
    <div class="panel-title">
        <span>Published Vacancies (<%= jobTotal %>)</span>
    </div>

    <% if (jobs == null || jobs.isEmpty()) { %>
        <div class="empty-state">
            <div class="empty-icon">&#128188;</div>
            <h3>No jobs published yet</h3>
            <p>Use the form above to add your first campus recruitment drive.</p>
        </div>
    <% } else { %>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Job Title</th>
                        <th>Company</th>
                        <th>Location</th>
                        <th>Eligible Branches</th>
                        <th>Min CGPA</th>
                        <th>Deadline</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Map<String, Object> job : jobs) { %>
                        <tr>
                            <td>
                                <strong><%= job.get("title") %></strong>
                            </td>
                            <td>
                                <span style="font-weight: 600; color: var(--text-secondary);">
                                    <%= job.get("company") %>
                                </span>
                            </td>
                            <td>
                                <span class="tag-badge location">
                                    &#128205; <%= job.get("location") != null ? job.get("location") : "On-Site" %>
                                </span>
                            </td>
                            <td><%= job.get("eligibility") != null ? job.get("eligibility") : "All Branches" %></td>
                            <td>
                                <span class="tag-badge cgpa">
                                    &#127891; <%= job.get("minCgpa") != null ? job.get("minCgpa") : "0.0" %>
                                </span>
                            </td>
                            <td>
                                <span style="font-size: 0.85rem; color: var(--text-muted);">
                                    <%= job.get("deadline") != null ? job.get("deadline") : "Ongoing" %>
                                </span>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    <% } %>
</section>

<%@ include file="_footer.jspf" %>
