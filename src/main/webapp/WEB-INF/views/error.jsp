<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ include file="_header.jspf" %>

<%
    int statusCode = response.getStatus();
    if (statusCode == 200 && request.getAttribute("javax.servlet.error.status_code") != null) {
        statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    }

    String errorMessage = "An unexpected error occurred while processing your request.";
    if (statusCode == 404) {
        errorMessage = "The page or placement resource you requested could not be found.";
    } else if (statusCode == 403) {
        errorMessage = "You do not have administrative authorization to access this placement resource.";
    } else if (statusCode == 500) {
        errorMessage = "A server error occurred. Our placement services have safely logged the incident.";
    }
    Throwable rootEx = (Throwable) request.getAttribute("javax.servlet.error.exception");
    if (rootEx == null) {
        rootEx = exception;
    }
    String detailNotice = null;
    if (rootEx != null) {
        Throwable cause = rootEx;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            detailNotice = cause.getMessage().replaceAll(":[^:@]+@", ":***@");
        }
    }
%>

<section class="hero-banner">
    <div class="hero-content">
        <span class="hero-eyebrow">APPLICATION NOTICE</span>
        <h1><%= (statusCode > 0 ? "Error " + statusCode : "Notice") %></h1>
        <p><%= errorMessage %></p>
    </div>
</section>

<section class="panel" style="text-align: center; max-width: 650px; margin: 0 auto 36px; padding: 48px 24px;">
    <div style="font-size: 3.5rem; margin-bottom: 16px;">
        <%= statusCode == 404 ? "&#128269;" : (statusCode == 403 ? "&#128274;" : "&#9888;") %>
    </div>

    <h2 style="font-size: 1.4rem; font-weight: 700; color: var(--text-primary); margin-bottom: 12px;">
        <%= statusCode == 404 ? "Resource Not Found" : (statusCode == 403 ? "Access Restricted" : "Temporary Service Interruption") %>
    </h2>

    <p style="color: var(--text-muted); font-size: 0.95rem; line-height: 1.6; margin-bottom: 20px;">
        <%= statusCode == 404
            ? "The URL you navigated to does not exist or may have been relocated. Please check the address or return to the main placement portal."
            : "If you believe this is an error, please ensure your database connection and Oracle listener service (XE) are active, or contact your campus placement cell." %>
    </p>

    <% if (detailNotice != null) { %>
        <div style="background: var(--bg-subtle); border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 10px 14px; margin-bottom: 24px; font-size: 0.82rem; color: var(--text-secondary); text-align: left; word-break: break-word;">
            <strong>Diagnostic Detail:</strong> <%= detailNotice %>
        </div>
    <% } %>

    <div style="display: flex; gap: 12px; justify-content: center; flex-wrap: wrap;">
        <a href="${pageContext.request.contextPath}/dashboard" class="btn primary">
            &#127891; Return to Dashboard
        </a>
        <a href="${pageContext.request.contextPath}/jobs" class="btn secondary">
            &#128188; View Available Jobs
        </a>
    </div>
</section>

<%@ include file="_footer.jspf" %>
