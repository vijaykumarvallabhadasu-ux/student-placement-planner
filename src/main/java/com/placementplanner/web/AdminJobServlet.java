package com.placementplanner.web;

import com.placementplanner.dao.ApplicationDAO;
import com.placementplanner.dao.JobDAO;
import com.placementplanner.model.Student;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/admin/jobs")
public class AdminJobServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AdminJobServlet.class.getName());

    private final JobDAO jobDAO = new JobDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Student student = (session != null) ? (Student) session.getAttribute("student") : null;
        if (student == null || !student.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access required.");
            return;
        }

        try {
            request.setAttribute("jobs", jobDAO.findAll());
            request.setAttribute("applications", applicationDAO.findAllForAdmin());
            request.getRequestDispatcher("/WEB-INF/views/admin-jobs.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load admin jobs/applications data", e);
            throw new ServletException("Could not load admin data from Oracle Database", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        Student student = (session != null) ? (Student) session.getAttribute("student") : null;
        if (student == null || !student.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access required.");
            return;
        }

        String action = request.getParameter("action");

        // Handle updating application status (Shortlist, Interview, Placed, Rejected)
        if ("updateStatus".equalsIgnoreCase(action)) {
            String appIdParam = request.getParameter("applicationId");
            String status = request.getParameter("status");

            if (appIdParam != null && status != null && !status.isBlank()) {
                try {
                    long appId = Long.parseLong(appIdParam.trim());
                    applicationDAO.updateStatus(appId, status.trim());
                    response.sendRedirect(request.getContextPath() + "/admin/jobs?statusUpdated=1");
                    return;
                } catch (SQLException | NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, "Failed to update application status", e);
                    response.sendRedirect(request.getContextPath() + "/admin/jobs?error=statusfailed");
                    return;
                }
            }
        }

        // Handle publishing new job opportunity
        String title = request.getParameter("title");
        String company = request.getParameter("company");
        String companyIdParam = request.getParameter("companyId");
        String location = request.getParameter("location");
        String description = request.getParameter("description");
        String eligibility = request.getParameter("eligibility");
        String minCgpaParam = request.getParameter("minCgpa");
        String deadline = request.getParameter("deadline");
        String lastDate = request.getParameter("lastDate");

        if (deadline == null || deadline.isBlank()) {
            deadline = lastDate;
        }

        if (company == null || company.isBlank()) {
            if (companyIdParam != null && !companyIdParam.isBlank()) {
                company = "Company #" + companyIdParam.trim();
            } else {
                company = "Campus Recruiter";
            }
        }

        try {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Job title is required.");
            }

            Double minCgpa = null;
            if (minCgpaParam != null && !minCgpaParam.isBlank()) {
                minCgpa = Double.parseDouble(minCgpaParam.trim());
            }

            jobDAO.add(
                    title.trim(),
                    company.trim(),
                    location,
                    description,
                    eligibility,
                    minCgpa,
                    deadline
            );

            response.sendRedirect(request.getContextPath() + "/admin/jobs?added=1");

        } catch (SQLException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Failed to add job to Oracle Database", e);
            throw new ServletException("Could not add job to Oracle Database: " + e.getMessage(), e);
        }
    }
}
