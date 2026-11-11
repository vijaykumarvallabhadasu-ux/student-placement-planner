<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="_header.jspf" %>

<div class="auth-wrapper">
    <div class="auth-card">
        <div class="auth-header">
            <span class="hero-eyebrow">STUDENT PLACEMENT PLANNER</span>
            <h1>Welcome Back</h1>
            <p>Sign in to manage your placement prep, track skills, and apply for campus opportunities.</p>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert error" role="alert">
                <span><%= request.getAttribute("error") %></span>
            </div>
        <% } %>

        <% if ("1".equals(request.getParameter("registered"))) { %>
            <div class="alert success" role="status">
                <span>&#10003; Account created successfully! Please sign in with your credentials.</span>
            </div>
        <% } %>

        <% if ("1".equals(request.getParameter("loggedOut"))) { %>
            <div class="alert info" role="status">
                <span>You have been signed out successfully.</span>
            </div>
        <% } %>



        <form method="post" action="${pageContext.request.contextPath}/login">
            <div class="form-group">
                <label for="email">Email Address</label>
                <input
                    type="email"
                    id="email"
                    name="email"
                    placeholder="name@college.edu"
                    autocomplete="email"
                    required>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input
                    type="password"
                    id="password"
                    name="password"
                    placeholder="Enter your password"
                    autocomplete="current-password"
                    required>
            </div>

            <button type="submit" class="btn primary" style="width: 100%; margin-top: 8px;">
                Sign In to Account &rarr;
            </button>
        </form>

        <div class="auth-footer">
            Don't have an account yet?
            <a href="${pageContext.request.contextPath}/register">Create Account</a>
        </div>
    </div>
</div>

<%@ include file="_footer.jspf" %>
