<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Map" %>
<%@ include file="_header.jspf" %>

<%
    List<Map<String, Object>> internalApps = (List<Map<String, Object>>) request.getAttribute("internalApplications");
    if (internalApps == null) {
        internalApps = (List<Map<String, Object>>) request.getAttribute("applications");
    }
    List<Map<String, Object>> externalApps = (List<Map<String, Object>>) request.getAttribute("externalApplications");

    int internalCount = internalApps != null ? internalApps.size() : 0;
    int externalCount = externalApps != null ? externalApps.size() : 0;
    int totalCount = internalCount + externalCount;
%>

<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">APPLICATION TRACKER</span>
        <h1>My Job Applications</h1>
        <p>Monitor your active campus placement applications and tracked direct employer opportunities in one unified dashboard.</p>
    </div>
    <div class="hero-actions">
        <a href="${pageContext.request.contextPath}/jobs" class="btn primary">
            &#128188; Browse More Openings
        </a>
    </div>
</section>

<% if ("1".equals(request.getParameter("applied"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; Campus placement drive application submitted successfully and recorded in Oracle Database!</span>
    </div>
<% } %>

<% if ("1".equals(request.getParameter("externalRecorded"))) { %>
    <div class="alert success" role="status">
        <span>&#10003; External job application recorded! You can now track your submission progress here.</span>
    </div>
<% } %>

<!-- Overview Metric Cards -->
<section class="stats-grid">
    <div class="stat-card">
        <div class="stat-icon blue">&#128221;</div>
        <div class="stat-info">
            <div class="stat-value"><%= totalCount %></div>
            <div class="stat-label">Total Applications</div>
        </div>
    </div>
    <div class="stat-card">
        <div class="stat-icon teal">&#127891;</div>
        <div class="stat-info">
            <div class="stat-value"><%= internalCount %></div>
            <div class="stat-label">Campus Drives</div>
        </div>
    </div>
    <div class="stat-card">
        <div class="stat-icon purple">&#127760;</div>
        <div class="stat-info">
            <div class="stat-value"><%= externalCount %></div>
            <div class="stat-label">External Direct Openings</div>
        </div>
    </div>
</section>

<!-- Section 1: Campus Placement Drives (Oracle Database) -->
<section class="panel">
    <div class="panel-title">
        <span>&#127891; Campus Placement Drives (<%= internalCount %>)</span>
        <span style="font-size: 0.8rem; color: var(--text-muted); font-weight: 500;">Directly connected to college placement office</span>
    </div>

    <% if (internalApps == null || internalApps.isEmpty()) { %>
        <div class="empty-state" style="padding: 24px 16px;">
            <div class="empty-icon" style="font-size: 2rem;">&#127891;</div>
            <h3 style="font-size: 1.05rem;">No campus drive applications yet</h3>
            <p style="font-size: 0.85rem; max-width: 480px; margin: 0 auto 14px;">
                You haven't submitted any internal campus placement drive applications yet.
            </p>
            <a href="${pageContext.request.contextPath}/jobs" class="btn primary sm">
                Explore Campus Drives
            </a>
        </div>
    <% } else { %>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Job Role</th>
                        <th>Recruiting Company</th>
                        <th>Location</th>
                        <th>Applied Date</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Map<String, Object> app : internalApps) { %>
                        <tr>
                            <td>
                                <strong><%= app.get("title") != null ? app.get("title") : "Job Position" %></strong>
                            </td>
                            <td>
                                <span style="font-weight: 600; color: var(--text-secondary);">
                                    <%= app.get("company") != null ? app.get("company") : "Campus Recruiter" %>
                                </span>
                            </td>
                            <td>
                                <span class="tag-badge location">
                                    &#128205; <%= app.get("location") != null ? app.get("location") : "On-Site" %>
                                </span>
                            </td>
                            <td>
                                <span style="color: var(--text-muted); font-size: 0.88rem;">
                                    <%= app.get("appliedAt") != null ? app.get("appliedAt") : "Recent" %>
                                </span>
                            </td>
                            <td>
                                <span class="badge badge-applied">
                                    &#10003; <%= app.get("status") != null ? app.get("status") : "Applied" %>
                                </span>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    <% } %>
</section>

<!-- Section 2: Tracked External Opportunities (Employer Websites) -->
<section class="panel" style="margin-top: 24px;">
    <div class="panel-title">
        <span>&#127760; External Employer Portal Applications (<%= externalCount %>)</span>
        <span style="font-size: 0.8rem; color: var(--text-muted); font-weight: 500;">Recorded submissions on official career portals</span>
    </div>

    <% if (externalApps == null || externalApps.isEmpty()) { %>
        <div class="empty-state" style="padding: 24px 16px;">
            <div class="empty-icon" style="font-size: 2rem;">&#127760;</div>
            <h3 style="font-size: 1.05rem;">No external applications tracked yet</h3>
            <p style="font-size: 0.85rem; max-width: 480px; margin: 0 auto 14px;">
                When you apply for verified company openings (such as Oracle, Microsoft, Amazon, TCS), record your submission to monitor everything in one place.
            </p>
            <a href="${pageContext.request.contextPath}/jobs" class="btn secondary sm">
                Browse Verified Openings
            </a>
        </div>
    <% } else { %>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Job Role</th>
                        <th>Company Portal</th>
                        <th>Official Career Link</th>
                        <th>Recorded Date</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (Map<String, Object> ext : externalApps) { %>
                        <tr>
                            <td>
                                <strong><%= ext.get("title") != null ? ext.get("title") : "Software Engineer" %></strong>
                            </td>
                            <td>
                                <span style="font-weight: 600; color: var(--text-secondary);">
                                    <%= ext.get("company") != null ? ext.get("company") : "Employer Career Portal" %>
                                </span>
                            </td>
                            <td>
                                <%
                                    String url = (String) ext.get("officialUrl");
                                    if (url != null && !url.isBlank()) {
                                %>
                                    <a href="<%= url %>" target="_blank" rel="noopener noreferrer" style="font-size: 0.85rem; color: var(--primary); text-decoration: underline; font-weight: 600;">
                                        &#128279; View Portal Listing &rarr;
                                    </a>
                                <% } else { %>
                                    <span style="color: var(--text-muted); font-size: 0.85rem;">Official Portal</span>
                                <% } %>
                            </td>
                            <td>
                                <span style="color: var(--text-muted); font-size: 0.88rem;">
                                    <%= ext.get("appliedAt") != null ? ext.get("appliedAt") : "Recent" %>
                                </span>
                            </td>
                            <td>
                                <span class="badge" style="background: var(--accent-teal-light); color: var(--accent-teal); font-weight: 700;">
                                    &#10003; <%= ext.get("status") != null ? ext.get("status") : "Applied" %>
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
