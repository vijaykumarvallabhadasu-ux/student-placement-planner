package com.placementplanner.web;

import com.placementplanner.model.Student;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(urlPatterns = {
    "/dashboard",
    "/profile",
    "/skills",
    "/tasks",
    "/apply",
    "/applications",
    "/resume",
    "/resume/*",
    "/admin/*"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        Student student = (session != null) ? (Student) session.getAttribute("student") : null;

        // Redirect unauthenticated requests to login page
        if (student == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        // Restrict /admin/* paths strictly to ADMIN users
        String servletPath = httpRequest.getServletPath();
        if (servletPath != null && servletPath.startsWith("/admin")) {
            if (!student.isAdmin()) {
                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Administrator access required to view this resource."
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
