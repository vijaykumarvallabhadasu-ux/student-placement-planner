<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.placementplanner.model.Student, com.placementplanner.model.ResumeInfo, com.placementplanner.service.ResumeService" %>
<%@ include file="_header.jspf" %>

<%
    Student current = (Student) request.getAttribute("student");
    if (current == null) {
        current = loggedInStudent;
    }

    ResumeInfo resumeInfo = (ResumeInfo) request.getAttribute("resumeInfo");
    if (resumeInfo == null && current != null) {
        resumeInfo = ResumeService.getResumeInfo(current.getId());
    }

    String name = (current != null && current.getName() != null) ? current.getName() : "";
    String email = (current != null && current.getEmail() != null) ? current.getEmail() : "";
    String role = (current != null && current.getRole() != null) ? current.getRole() : "STUDENT";
    String dept = (current != null && current.getDepartment() != null) ? current.getDepartment() : "CSE";
    int year = (current != null && current.getYearOfStudy() > 0) ? current.getYearOfStudy() : 4;
    Double cgpa = (current != null) ? current.getCgpa() : null;

    String phone = (current != null && current.getPhone() != null) ? current.getPhone() : "";
    String collegeName = (current != null && current.getCollegeName() != null) ? current.getCollegeName() : "";
    String bio = (current != null && current.getBio() != null) ? current.getBio() : "";
    String linkedIn = (current != null && current.getLinkedIn() != null) ? current.getLinkedIn() : "";
    String gitHub = (current != null && current.getGitHub() != null) ? current.getGitHub() : "";

    int skillCount = request.getAttribute("skillCount") != null ? (Integer) request.getAttribute("skillCount") : 0;
    int applicationCount = request.getAttribute("applicationCount") != null ? (Integer) request.getAttribute("applicationCount") : 0;
    int taskCount = request.getAttribute("taskCount") != null ? (Integer) request.getAttribute("taskCount") : 0;
    int completedTaskCount = request.getAttribute("completedTaskCount") != null ? (Integer) request.getAttribute("completedTaskCount") : 0;

    String initial = (!name.isBlank()) ? name.substring(0, 1).toUpperCase() : "S";

    boolean hasResume = resumeInfo != null && resumeInfo.isUploaded();
    int atsScore = hasResume ? resumeInfo.getScore() : 0;
    String resumeStatusName = hasResume ? resumeInfo.getStatus().name() : "NOT_UPLOADED";
%>

<!-- Hero Section -->
<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">ACADEMIC &amp; CAREER PROFILE</span>
        <h1>My Student Profile &amp; Resume</h1>
        <p>Keep your academic credentials, technical qualifications, and verified resume updated so top employers can review your campus placement eligibility.</p>
    </div>
    <div class="hero-actions">
        <a href="${pageContext.request.contextPath}/dashboard" class="btn secondary">
            &larr; Back to Dashboard
        </a>
    </div>
</section>

<!-- Feedback Alerts -->
<% if ("1".equals(request.getParameter("updated"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Profile and academic credentials updated successfully!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("resumeSuccess"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Resume uploaded and parsed successfully! Automated structural analysis score: <strong><%= atsScore %>/100</strong>.</span>
    </div>
<% } %>

<% if (request.getParameter("resumeError") != null && !request.getParameter("resumeError").isBlank()) { %>
    <div class="alert error" role="alert">
        <span>&#9888; <%= request.getParameter("resumeError") %></span>
    </div>
<% } %>

<% if ("noname".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Full name is required and cannot be blank.</span>
    </div>
<% } %>

<% if ("invalidcgpa".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; CGPA must be a valid number between 0.00 and 10.00.</span>
    </div>
<% } %>

<% if ("failed".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Failed to update profile details. Please try again.</span>
    </div>
<% } %>

<div class="dashboard-columns">

    <!-- Left Column: Academic Info Form & Resume Hub -->
    <div style="display: flex; flex-direction: column; gap: 24px;">

        <!-- Panel 1: Edit Profile Form -->
        <section class="panel">
            <div class="panel-title">
                <span>&#128100; Academic &amp; Personal Credentials</span>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/profile">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 18px;">
                    <div class="form-group">
                        <label for="name">Full Name *</label>
                        <input
                            type="text"
                            id="name"
                            name="name"
                            value="<%= name %>"
                            placeholder="e.g. Vijay Kumar"
                            maxlength="100"
                            required>
                    </div>

                    <div class="form-group">
                        <label for="email">College Email (Read-Only)</label>
                        <input
                            type="email"
                            id="email"
                            value="<%= email %>"
                            disabled
                            style="opacity: 0.75; cursor: not-allowed; background: var(--bg-subtle);">
                        <small style="font-size: 0.78rem; color: var(--text-muted); margin-top: 4px; display: block;">
                            Official campus identity managed by the placement office.
                        </small>
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 18px; margin-top: 14px;">
                    <div class="form-group">
                        <label for="department">Department / Branch</label>
                        <select id="department" name="department">
                            <option value="CSE" <%= "CSE".equalsIgnoreCase(dept) ? "selected" : "" %>>CSE - Computer Science</option>
                            <option value="IT" <%= "IT".equalsIgnoreCase(dept) ? "selected" : "" %>>IT - Information Technology</option>
                            <option value="ECE" <%= "ECE".equalsIgnoreCase(dept) ? "selected" : "" %>>ECE - Electronics &amp; Comm.</option>
                            <option value="EEE" <%= "EEE".equalsIgnoreCase(dept) ? "selected" : "" %>>EEE - Electrical &amp; Electronics</option>
                            <option value="MECH" <%= "MECH".equalsIgnoreCase(dept) ? "selected" : "" %>>Mechanical Engineering</option>
                            <option value="CIVIL" <%= "CIVIL".equalsIgnoreCase(dept) ? "selected" : "" %>>Civil Engineering</option>
                            <option value="AI &amp; DS" <%= ("AI & DS".equalsIgnoreCase(dept) || "AIDS".equalsIgnoreCase(dept)) ? "selected" : "" %>>AI &amp; Data Science</option>
                            <option value="Other" <%= (!"CSE".equalsIgnoreCase(dept) && !"IT".equalsIgnoreCase(dept) && !"ECE".equalsIgnoreCase(dept) && !"EEE".equalsIgnoreCase(dept) && !"MECH".equalsIgnoreCase(dept) && !"CIVIL".equalsIgnoreCase(dept) && !"AI & DS".equalsIgnoreCase(dept) && !"AIDS".equalsIgnoreCase(dept)) ? "selected" : "" %>>Other Engineering</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="yearOfStudy">Year of Study</label>
                        <select id="yearOfStudy" name="yearOfStudy">
                            <option value="1" <%= year == 1 ? "selected" : "" %>>1st Year (Freshman)</option>
                            <option value="2" <%= year == 2 ? "selected" : "" %>>2nd Year (Sophomore)</option>
                            <option value="3" <%= year == 3 ? "selected" : "" %>>3rd Year (Junior)</option>
                            <option value="4" <%= year == 4 ? "selected" : "" %>>4th Year (Final Year)</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="cgpa">Current CGPA (0.0 - 10.0)</label>
                        <input
                            type="number"
                            id="cgpa"
                            name="cgpa"
                            value="<%= cgpa != null ? String.format(java.util.Locale.US, "%.2f", cgpa) : "" %>"
                            step="0.01"
                            min="0.0"
                            max="10.0"
                            placeholder="e.g. 8.45">
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 14px;">
                    <div class="form-group">
                        <label for="phone">Phone Number</label>
                        <input
                            type="tel"
                            id="phone"
                            name="phone"
                            value="<%= phone %>"
                            placeholder="e.g. +91 98765 43210"
                            maxlength="20">
                    </div>

                    <div class="form-group">
                        <label for="collegeName">College / University Name</label>
                        <input
                            type="text"
                            id="collegeName"
                            name="collegeName"
                            value="<%= collegeName %>"
                            placeholder="e.g. National Institute of Technology"
                            maxlength="150">
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 14px;">
                    <div class="form-group">
                        <label for="linkedIn">LinkedIn Profile URL</label>
                        <input
                            type="url"
                            id="linkedIn"
                            name="linkedIn"
                            value="<%= linkedIn %>"
                            placeholder="https://linkedin.com/in/username"
                            maxlength="200">
                    </div>

                    <div class="form-group">
                        <label for="gitHub">GitHub / Portfolio URL</label>
                        <input
                            type="url"
                            id="gitHub"
                            name="gitHub"
                            value="<%= gitHub %>"
                            placeholder="https://github.com/username"
                            maxlength="200">
                    </div>
                </div>

                <div class="form-group" style="margin-top: 14px;">
                    <label for="bio">Career Objective &amp; Technical Summary</label>
                    <textarea
                        id="bio"
                        name="bio"
                        rows="3"
                        placeholder="e.g. Aspiring Full-Stack Software Engineer with strong foundations in Data Structures, Java, and Relational Databases. Actively seeking campus opportunities in product-based companies."
                        maxlength="500"
                        style="width: 100%; border-radius: var(--radius-sm); border: 1px solid var(--border-color); padding: 10px 14px; background: var(--bg-card); color: var(--text-primary); font-family: inherit; font-size: 0.9rem;"><%= bio %></textarea>
                </div>

                <div style="margin-top: 24px; display: flex; gap: 14px; align-items: center;">
                    <button type="submit" class="btn primary">
                        &#10003; Save Profile Changes
                    </button>
                    <a href="${pageContext.request.contextPath}/dashboard" class="btn secondary">
                        Cancel
                    </a>
                </div>
            </form>
        </section>

        <!-- Panel 2: Resume Hub & Automated ATS Verification -->
        <section class="panel" id="resume-hub">
            <div class="panel-title">
                <span>&#128196; Resume Hub &amp; ATS Structural Analysis</span>
                <% if (hasResume) { %>
                    <span class="badge <%= "PASSED".equals(resumeStatusName) ? "badge-completed" : "badge-pending" %>">
                        <%= resumeInfo.getStatus().getLabel() %>
                    </span>
                <% } %>
            </div>

            <% if (hasResume) { %>
                <!-- Active Resume Snapshot & Structural Analysis -->
                <div class="resume-score-card">
                    <div class="resume-score-circle" title="ATS Structural Completeness Score">
                        <span><%= atsScore %></span>
                        <small>ATS</small>
                    </div>
                    <div style="flex: 1;">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 8px;">
                            <div>
                                <h3 style="font-size: 1.05rem; font-weight: 700; color: var(--text-primary); margin-bottom: 2px;">
                                    &#128196; <%= resumeInfo.getOriginalFileName() %>
                                </h3>
                                <div style="font-size: 0.82rem; color: var(--text-muted); display: flex; gap: 12px; flex-wrap: wrap;">
                                    <span>&#128197; Uploaded: <%= resumeInfo.getUploadTime() %></span>
                                    <span>&#128190; Size: <%= resumeInfo.getFormattedFileSize() %></span>
                                    <span>&#128221; <%= resumeInfo.getWordCount() %> Words Extracted</span>
                                </div>
                            </div>
                            <div style="display: flex; gap: 8px;">
                                <a href="${pageContext.request.contextPath}/resume/view" target="_blank" class="btn sm secondary" title="Open document in browser">
                                    &#128065; View Document
                                </a>
                                <a href="${pageContext.request.contextPath}/resume/download" class="btn sm secondary" title="Download saved file">
                                    &#11015; Download
                                </a>
                            </div>
                        </div>

                        <!-- Detected & Missing Sections -->
                        <div style="margin-top: 14px;">
                            <div style="font-size: 0.78rem; font-weight: 700; text-transform: uppercase; color: var(--text-muted); letter-spacing: 0.5px; margin-bottom: 6px;">
                                Document Structure Verification:
                            </div>
                            <div class="resume-sections-pills">
                                <% for (String sec : resumeInfo.getFoundSections()) { %>
                                    <span class="section-pill present" title="Section detected">
                                        &#10004; <%= sec %>
                                    </span>
                                <% } %>
                                <% for (String sec : resumeInfo.getMissingSections()) { %>
                                    <span class="section-pill missing" title="Section not detected">
                                        &#9888; Missing: <%= sec %>
                                    </span>
                                <% } %>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Actionable Structural Feedback Box -->
                <% if (!resumeInfo.getFeedback().isEmpty()) { %>
                    <div class="resume-suggestions-box">
                        <h4>&#128161; Automated Recommendations for Campus Hiring</h4>
                        <ul>
                            <% for (String tip : resumeInfo.getFeedback()) { %>
                                <li><%= tip %></li>
                            <% } %>
                        </ul>
                    </div>
                <% } %>

                <!-- Replace / Re-upload Resume Form -->
                <div style="margin-top: 20px; border-top: 1px solid var(--border-color); padding-top: 16px;">
                    <div style="font-size: 0.88rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 10px;">
                        &#128260; Upload Updated Resume Document:
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/resume/upload" enctype="multipart/form-data" class="resume-upload-form">
                        <input
                            type="file"
                            name="resumeFile"
                            accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            required
                            style="font-size: 0.88rem;">
                        <button type="submit" class="btn primary sm">
                            &#8679; Re-Analyze &amp; Update Resume
                        </button>
                    </form>
                    <small style="font-size: 0.78rem; color: var(--text-muted); margin-top: 6px; display: block;">
                        Supported formats: PDF (.pdf) and MS Word (.docx). Maximum file size: 5 MB.
                    </small>
                </div>

            <% } else { %>
                <!-- Empty State: No Resume Uploaded -->
                <div class="empty-state" style="padding: 32px 20px;">
                    <div class="empty-icon">&#128196;</div>
                    <h3>No Resume Uploaded Yet</h3>
                    <p style="max-width: 540px; margin: 0 auto 18px;">
                        Campus placement recruiters screen candidates using ATS-compatible resumes. Upload your resume to run an instant automated structural audit, verify critical sections (Education, Skills, Projects, Experience), and calculate your readiness score.
                    </p>

                    <form method="post" action="${pageContext.request.contextPath}/resume/upload" enctype="multipart/form-data" class="resume-upload-form" style="max-width: 520px; margin: 0 auto; justify-content: center;">
                        <input
                            type="file"
                            name="resumeFile"
                            accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            required
                            style="font-size: 0.88rem;">
                        <button type="submit" class="btn primary">
                            &#8679; Upload &amp; Verify Resume
                        </button>
                    </form>

                    <div style="display: flex; justify-content: center; gap: 16px; margin-top: 14px; font-size: 0.78rem; color: var(--text-muted); flex-wrap: wrap;">
                        <span>&#10003; PDF &amp; DOCX formats supported</span>
                        <span>&#10003; Max size: 5 MB</span>
                        <span>&#10003; Pure Java text extraction</span>
                        <span>&#10003; Instant ATS audit</span>
                    </div>
                </div>
            <% } %>
        </section>

    </div>

    <!-- Right Column: Profile Snapshot & Placement Portfolio Card -->
    <div>

        <!-- Profile Identity Card -->
        <section class="panel" style="text-align: center;">
            <div style="width: 84px; height: 84px; border-radius: 50%; background: var(--gradient-primary); color: white; display: flex; align-items: center; justify-content: center; font-size: 2.3rem; font-weight: 800; margin: 0 auto 16px; box-shadow: var(--shadow-md);">
                <%= initial %>
            </div>

            <h2 style="font-size: 1.35rem; font-weight: 800; color: var(--text-primary); margin-bottom: 4px;">
                <%= name %>
            </h2>
            <div style="color: var(--text-muted); font-size: 0.88rem; margin-bottom: 12px;">
                <%= email %>
            </div>

            <% if (!collegeName.isBlank()) { %>
                <div style="font-size: 0.86rem; color: var(--text-secondary); font-weight: 600; margin-bottom: 10px;">
                    &#127979; <%= collegeName %>
                </div>
            <% } %>

            <div style="display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; margin-bottom: 16px;">
                <span class="role-tag <%= "ADMIN".equalsIgnoreCase(role) ? "admin" : "student" %>">
                    <%= role %>
                </span>
                <span class="tag-badge branch">
                    &#128218; <%= dept %>
                </span>
                <span class="tag-badge location">
                    &#127979; Year <%= year %>
                </span>
                <span class="tag-badge cgpa">
                    &#11088; CGPA: <%= cgpa != null ? String.format(java.util.Locale.US, "%.2f", cgpa) : "Not Set" %>
                </span>
            </div>

            <!-- Social Links & Phone -->
            <div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; margin-bottom: 18px;">
                <% if (!phone.isBlank()) { %>
                    <span class="tag-badge" style="background: var(--bg-subtle);">
                        &#128222; <%= phone %>
                    </span>
                <% } %>
                <% if (!linkedIn.isBlank()) { %>
                    <a href="<%= linkedIn %>" target="_blank" rel="noopener noreferrer" class="tag-badge" style="background: var(--primary-light); color: var(--primary); text-decoration: none; font-weight: 600;">
                        &#128279; LinkedIn
                    </a>
                <% } %>
                <% if (!gitHub.isBlank()) { %>
                    <a href="<%= gitHub %>" target="_blank" rel="noopener noreferrer" class="tag-badge" style="background: var(--bg-subtle); color: var(--text-primary); text-decoration: none; font-weight: 600;">
                        &#128187; GitHub
                    </a>
                <% } %>
            </div>

            <% if (!bio.isBlank()) { %>
                <p style="font-size: 0.85rem; color: var(--text-secondary); line-height: 1.45; text-align: left; background: var(--bg-subtle); padding: 12px 14px; border-radius: var(--radius-sm); margin-bottom: 16px; border-left: 3px solid var(--primary);">
                    "<%= bio %>"
                </p>
            <% } %>

            <!-- Eligibility Status Bar -->
            <div style="border-top: 1px solid var(--border-color); padding-top: 16px; text-align: left;">
                <div style="font-size: 0.82rem; font-weight: 700; color: var(--text-muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.5px;">
                    Drive Eligibility Assessment
                </div>
                <% if (cgpa == null || cgpa == 0.0) { %>
                    <div style="font-size: 0.88rem; color: var(--accent-amber); font-weight: 600;">
                        &#9888; CGPA not updated. Enter your CGPA to verify drive cut-offs.
                    </div>
                <% } else if (cgpa >= 8.5) { %>
                    <div style="font-size: 0.88rem; color: var(--success); font-weight: 600;">
                        &#10004; Excellent! Eligible for Top Product &amp; Super Dream hiring drives (CGPA &ge; 8.5).
                    </div>
                <% } else if (cgpa >= 7.0) { %>
                    <div style="font-size: 0.88rem; color: var(--primary); font-weight: 600;">
                        &#10004; Strong standing! Meets eligibility for majority of IT &amp; Core engineering drives.
                    </div>
                <% } else { %>
                    <div style="font-size: 0.88rem; color: var(--text-secondary); font-weight: 600;">
                        &#8505; Meets criteria for organizations with no strict cut-off or &le; 6.5 threshold.
                    </div>
                <% } %>
            </div>
        </section>

        <!-- Placement Portfolio Overview -->
        <section class="panel">
            <div class="panel-title">
                <span>&#128202; Placement Portfolio</span>
            </div>

            <div style="display: flex; flex-direction: column; gap: 12px;">
                <!-- Resume Hub Link -->
                <a href="#resume-hub" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: var(--bg-subtle); border-radius: var(--radius-md); text-decoration: none; color: inherit; transition: var(--transition);">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="font-size: 1.2rem;">&#128196;</span>
                        <span style="font-weight: 600;">Resume ATS Score</span>
                    </div>
                    <% if (hasResume) { %>
                        <span class="badge" style="background: var(--success-light); color: var(--success);">
                            <%= atsScore %>/100 &#10003;
                        </span>
                    <% } else { %>
                        <span class="badge" style="background: var(--warning-light); color: var(--warning);">
                            Upload Needed &rarr;
                        </span>
                    <% } %>
                </a>

                <a href="${pageContext.request.contextPath}/skills" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: var(--bg-subtle); border-radius: var(--radius-md); text-decoration: none; color: inherit; transition: var(--transition);">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="font-size: 1.2rem;">&#128218;</span>
                        <span style="font-weight: 600;">Technical Skills</span>
                    </div>
                    <span class="badge" style="background: var(--primary-light); color: var(--primary);">
                        <%= skillCount %> Tracked &rarr;
                    </span>
                </a>

                <a href="${pageContext.request.contextPath}/applications" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: var(--bg-subtle); border-radius: var(--radius-md); text-decoration: none; color: inherit; transition: var(--transition);">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="font-size: 1.2rem;">&#128221;</span>
                        <span style="font-weight: 600;">Job Applications</span>
                    </div>
                    <span class="badge" style="background: var(--accent-teal-light); color: var(--accent-teal);">
                        <%= applicationCount %> Submitted &rarr;
                    </span>
                </a>

                <a href="${pageContext.request.contextPath}/tasks" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: var(--bg-subtle); border-radius: var(--radius-md); text-decoration: none; color: inherit; transition: var(--transition);">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="font-size: 1.2rem;">&#9989;</span>
                        <span style="font-weight: 600;">Preparation Milestones</span>
                    </div>
                    <span class="badge" style="background: var(--accent-purple-light); color: var(--accent-purple);">
                        <%= completedTaskCount %> / <%= taskCount %> Done &rarr;
                    </span>
                </a>
            </div>
        </section>

    </div>

</div>

<%@ include file="_footer.jspf" %>
