<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map, java.util.Set, java.util.HashSet, com.placementplanner.model.ResumeInfo" %>
<%@ include file="_header.jspf" %>

<%
    List<Map<String, Object>> skills = (List<Map<String, Object>>) request.getAttribute("skills");
    int skillTotal = skills != null ? skills.size() : 0;

    ResumeInfo resumeInfo = (ResumeInfo) request.getAttribute("resumeInfo");
    boolean hasResume = resumeInfo != null && resumeInfo.isUploaded();
    List<String> resumeDetectedSkills = hasResume ? resumeInfo.getExtractedSkills() : java.util.Collections.emptyList();

    Set<String> existingSkillNamesLower = new HashSet<>();
    if (skills != null) {
        for (Map<String, Object> s : skills) {
            String sName = (String) s.get("skillName");
            if (sName != null) {
                existingSkillNamesLower.add(sName.trim().toLowerCase());
            }
        }
    }

    List<String> pendingResumeSkills = new java.util.ArrayList<>();
    List<String> confirmedResumeSkills = new java.util.ArrayList<>();
    for (String rSkill : resumeDetectedSkills) {
        if (existingSkillNamesLower.contains(rSkill.trim().toLowerCase())) {
            confirmedResumeSkills.add(rSkill.trim());
        } else {
            pendingResumeSkills.add(rSkill.trim());
        }
    }
%>

<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">CAREER PROFILE BUILDER</span>
        <h1>Technical Skills Portfolio</h1>
        <p>Record your technical proficiencies, programming languages, and framework competencies to match company recruitment requirements.</p>
    </div>
    <div class="hero-actions">
        <a href="${pageContext.request.contextPath}/profile" class="btn secondary">
            &#128100; View Full Profile
        </a>
    </div>
</section>

<!-- Feedback Alerts -->
<% if ("1".equals(request.getParameter("added"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Technical skill added successfully to your portfolio in Oracle Database!</span>
    </div>
<% } %>

<% if (request.getParameter("detectedAdded") != null) { %>
    <div class="alert success" role="status">
        <span>&#10003; <%= request.getParameter("detectedAdded") %> skills extracted from your resume were reviewed and added to your portfolio!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("updated"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Skill proficiency level updated successfully in Oracle Database!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("deleted"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Skill removed from your profile.</span>
    </div>
<% } %>

<% if ("empty".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Skill name cannot be empty.</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("duplicate"))) { %>
    <div class="alert info" role="status">
        <span>&#8505; This skill is already recorded in your portfolio. Duplicate entries are prevented.</span>
    </div>
<% } %>

<% if ("dbfailed".equals(request.getParameter("error"))) { %>
    <div class="alert error" role="alert">
        <span>&#9888; Database operation could not be completed. Please try again.</span>
    </div>
<% } %>

<!-- RESUME SKILL EXTRACTION REVIEW PANEL (Requirement 6) -->
<% if (hasResume) { %>
    <section class="panel" style="border: 1px solid var(--accent-purple-light); background: var(--bg-subtle);">
        <div class="panel-title">
            <div style="display: flex; align-items: center; gap: 8px;">
                <span style="font-size: 1.2rem;">&#128196;</span>
                <span>Resume Skill Extraction (Automated Text Analysis)</span>
            </div>
            <a href="${pageContext.request.contextPath}/profile#resume-hub" class="btn sm secondary">Manage Resume</a>
        </div>

        <p style="font-size: 0.82rem; color: var(--text-muted); margin-bottom: 14px; line-height: 1.4;">
            We analyzed your uploaded resume (<strong style="color: var(--text-primary);"><%= resumeInfo.getOriginalFileName() %></strong>).
            Review the skills detected in your document before confirming them in your official placement portfolio.
            <em>Note: Automated text extraction assists your review and does not constitute official employer verification.</em>
        </p>

        <% if (resumeDetectedSkills.isEmpty()) { %>
            <p style="font-size: 0.84rem; color: var(--text-secondary);">
                No standard technical keywords were detected in the text of your uploaded resume. You can add skills manually using the form below.
            </p>
        <% } else { %>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                <!-- Pending Review -->
                <div style="background: var(--card-bg); padding: 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                        <strong style="font-size: 0.86rem; color: var(--text-primary);">
                            &#128065; Detected in Resume (Not in Profile Yet): <%= pendingResumeSkills.size() %>
                        </strong>
                        <% if (!pendingResumeSkills.isEmpty()) { %>
                            <form method="post" action="${pageContext.request.contextPath}/skills" style="display: inline;">
                                <input type="hidden" name="action" value="addAllDetected">
                                <input type="hidden" name="skills" value="<%= String.join(",", pendingResumeSkills) %>">
                                <button type="submit" class="btn sm primary" style="font-size: 0.74rem; padding: 2px 8px;">
                                    + Add All (<%= pendingResumeSkills.size() %>)
                                </button>
                            </form>
                        <% } %>
                    </div>

                    <% if (pendingResumeSkills.isEmpty()) { %>
                        <p style="font-size: 0.8rem; color: var(--text-muted); margin: 0;">
                            &#10003; All skills detected in your resume have already been added to your profile!
                        </p>
                    <% } else { %>
                        <div style="display: flex; flex-direction: column; gap: 8px;">
                            <% for (String pSkill : pendingResumeSkills) { %>
                                <div style="display: flex; justify-content: space-between; align-items: center; background: var(--bg-subtle); padding: 6px 10px; border-radius: var(--radius-sm);">
                                    <span style="font-size: 0.82rem; font-weight: 700; color: var(--text-primary);"><%= pSkill %></span>
                                    <form method="post" action="${pageContext.request.contextPath}/skills" style="display: inline;">
                                        <input type="hidden" name="skill" value="<%= pSkill %>">
                                        <input type="hidden" name="proficiency" value="Proficient">
                                        <button type="submit" class="btn sm secondary" style="font-size: 0.74rem; padding: 2px 8px;">
                                            + Add to Profile
                                        </button>
                                    </form>
                                </div>
                            <% } %>
                        </div>
                    <% } %>
                </div>

                <!-- Already Confirmed -->
                <div style="background: var(--card-bg); padding: 14px; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
                    <strong style="font-size: 0.86rem; color: var(--text-primary); display: block; margin-bottom: 10px;">
                        &#9989; Evidenced in Both Resume &amp; Profile (<%= confirmedResumeSkills.size() %>)
                    </strong>

                    <% if (confirmedResumeSkills.isEmpty()) { %>
                        <p style="font-size: 0.8rem; color: var(--text-muted); margin: 0;">
                            No overlapping skills yet. Review the detected skills on the left to add them.
                        </p>
                    <% } else { %>
                        <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                            <% for (String cSkill : confirmedResumeSkills) { %>
                                <span class="tag-badge" style="background: #e8f5e9; color: #2e7d32; font-size: 0.78rem; font-weight: 700;">
                                    &#10003; <%= cSkill %>
                                </span>
                            <% } %>
                        </div>
                    <% } %>
                </div>
            </div>
        <% } %>
    </section>
<% } else { %>
    <section class="panel" style="background: var(--bg-subtle); border: 1px dashed var(--border-color); padding: 14px;">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
            <div>
                <strong style="font-size: 0.9rem; color: var(--text-primary);">&#128196; Automated Resume Skill Extraction Available</strong>
                <p style="font-size: 0.8rem; color: var(--text-muted); margin: 2px 0 0 0;">
                    Upload your PDF or DOCX resume to automatically detect skills, certifications, and project keywords.
                </p>
            </div>
            <a href="${pageContext.request.contextPath}/profile#resume-hub" class="btn sm primary">
                Upload Resume Now
            </a>
        </div>
    </section>
<% } %>

<!-- Add New Skill Panel -->
<section class="panel">
    <div class="panel-title">
        <span>Add a Technical Skill</span>
    </div>

    <form method="post" action="${pageContext.request.contextPath}/skills">
        <div style="display: grid; grid-template-columns: 2fr 1fr auto; gap: 14px; align-items: flex-end;">
            <div class="form-group" style="margin-bottom: 0;">
                <label for="skill">Skill Name *</label>
                <input
                    type="text"
                    id="skill"
                    name="skill"
                    placeholder="e.g. Core Java, Oracle SQL, Python, Spring Boot, Data Structures..."
                    maxlength="100"
                    required>
            </div>

            <div class="form-group" style="margin-bottom: 0;">
                <label for="proficiency">Proficiency Level</label>
                <select id="proficiency" name="proficiency">
                    <option value="Beginner">Beginner (Foundational)</option>
                    <option value="Intermediate">Intermediate (Hands-on)</option>
                    <option value="Proficient" selected>Proficient (Competent)</option>
                    <option value="Advanced">Advanced (Extensive)</option>
                    <option value="Expert">Expert (Production-ready)</option>
                </select>
            </div>

            <button type="submit" class="btn primary" style="height: 44px;">
                + Add Skill
            </button>
        </div>

        <div style="margin-top: 14px;">
            <span style="font-size: 0.82rem; font-weight: 700; color: var(--text-muted);">Quick Add Suggestions:</span>
            <div class="suggested-tags">
                <button type="button" class="tag-btn" data-skill="Core Java">Core Java</button>
                <button type="button" class="tag-btn" data-skill="Oracle SQL">Oracle SQL</button>
                <button type="button" class="tag-btn" data-skill="Spring Boot">Spring Boot</button>
                <button type="button" class="tag-btn" data-skill="Python">Python</button>
                <button type="button" class="tag-btn" data-skill="Data Structures &amp; Algorithms">Data Structures &amp; Algorithms</button>
                <button type="button" class="tag-btn" data-skill="JavaScript">JavaScript</button>
                <button type="button" class="tag-btn" data-skill="React.js">React.js</button>
                <button type="button" class="tag-btn" data-skill="Git &amp; GitHub">Git &amp; GitHub</button>
                <button type="button" class="tag-btn" data-skill="REST APIs">REST APIs</button>
                <button type="button" class="tag-btn" data-skill="AWS Cloud">AWS Cloud</button>
            </div>
        </div>
    </form>
</section>

<!-- My Skills List -->
<section class="panel">
    <div class="panel-title">
        <span>My Confirmed Skills Portfolio (<%= skillTotal %>)</span>
    </div>

    <% if (skills == null || skills.isEmpty()) { %>
        <div class="empty-state">
            <div class="empty-icon">&#128218;</div>
            <h3>No skills recorded yet</h3>
            <p>Add your technical skills using the form above or pick from the quick suggestion chips to show recruiters your technical background.</p>
        </div>
    <% } else { %>
        <div class="skill-grid">
            <% for (Map<String, Object> skill : skills) { 
                String prof = (String) skill.get("proficiency");
                if (prof == null) prof = "Proficient";
                long skillId = (skill.get("id") instanceof Number) ? ((Number) skill.get("id")).longValue() : 0L;
            %>
                <div class="skill-chip">
                    <div class="skill-info">
                        <div class="skill-dot"></div>
                        <div style="flex: 1;">
                            <div class="skill-name"><%= skill.get("skillName") %></div>
                            
                            <!-- Inline Proficiency Updater -->
                            <form method="post" action="${pageContext.request.contextPath}/skills" style="margin-top: 6px; display: flex; align-items: center; gap: 6px;">
                                <input type="hidden" name="action" value="proficiency">
                                <input type="hidden" name="id" value="<%= skillId %>">
                                <select name="proficiency" onchange="this.form.submit()" style="padding: 2px 8px; font-size: 0.78rem; height: 26px; border-radius: var(--radius-sm); width: auto;">
                                    <option value="Beginner" <%= "Beginner".equalsIgnoreCase(prof) ? "selected" : "" %>>Beginner</option>
                                    <option value="Intermediate" <%= "Intermediate".equalsIgnoreCase(prof) ? "selected" : "" %>>Intermediate</option>
                                    <option value="Proficient" <%= "Proficient".equalsIgnoreCase(prof) ? "selected" : "" %>>Proficient</option>
                                    <option value="Advanced" <%= "Advanced".equalsIgnoreCase(prof) ? "selected" : "" %>>Advanced</option>
                                    <option value="Expert" <%= "Expert".equalsIgnoreCase(prof) ? "selected" : "" %>>Expert</option>
                                </select>
                            </form>
                        </div>
                    </div>

                    <form method="post" action="${pageContext.request.contextPath}/skills" data-confirm="Are you sure you want to remove <%= skill.get("skillName") %> from your skills portfolio?">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id" value="<%= skillId %>">
                        <button type="submit" class="btn sm outline-danger" title="Remove this skill">
                            Remove
                        </button>
                    </form>
                </div>
            <% } %>
        </div>
    <% } %>
</section>

<%@ include file="_footer.jspf" %>
