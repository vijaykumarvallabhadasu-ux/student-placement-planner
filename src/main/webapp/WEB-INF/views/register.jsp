<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>

<div class="auth-wrapper">
    <div class="auth-card">
        <div class="auth-header">
            <span class="hero-eyebrow">STUDENT PLACEMENT PLANNER</span>
            <h1>Create Account</h1>
            <p>Register as a student to track placement skills, prepare for drives, and apply for openings.</p>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert error" role="alert">
                <span><%= request.getAttribute("error") %></span>
            </div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/register" onsubmit="return validateRegistration()">
            <div class="form-group">
                <label for="name">Full Name</label>
                <input
                    type="text"
                    id="name"
                    name="name"
                    placeholder="e.g. Vijay Kumar"
                    maxlength="100"
                    autocomplete="name"
                    required>
            </div>

            <div class="form-group">
                <label for="email">College Email Address</label>
                <input
                    type="email"
                    id="email"
                    name="email"
                    placeholder="e.g. vijay@college.edu"
                    maxlength="150"
                    autocomplete="email"
                    required>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input
                    type="password"
                    id="password"
                    name="password"
                    placeholder="Minimum 6 characters"
                    minlength="6"
                    autocomplete="new-password"
                    required>
                <p id="passwordError" class="alert error" style="display: none; margin-top: 6px; padding: 6px 12px; font-size: 0.82rem;">
                    Password must contain at least 6 characters.
                </p>
            </div>

            <div class="form-group">
                <label for="confirmPassword">Confirm Password</label>
                <input
                    type="password"
                    id="confirmPassword"
                    name="confirmPassword"
                    placeholder="Re-enter password"
                    minlength="6"
                    autocomplete="new-password"
                    required>
                <p id="confirmError" class="alert error" style="display: none; margin-top: 6px; padding: 6px 12px; font-size: 0.82rem;">
                    Passwords do not match.
                </p>
            </div>

            <button type="submit" class="btn primary" style="width: 100%; margin-top: 8px;">
                Complete Registration &rarr;
            </button>
        </form>

        <div class="auth-footer">
            Already registered?
            <a href="${pageContext.request.contextPath}/login">Sign In</a>
        </div>
    </div>
</div>

<%@ include file="_footer.jspf" %>
