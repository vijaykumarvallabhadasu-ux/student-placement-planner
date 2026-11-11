package com.placementplanner.web;

import com.placementplanner.dao.ApplicationDAO;
import com.placementplanner.dao.SkillDAO;
import com.placementplanner.dao.StudentDAO;
import com.placementplanner.dao.TaskDAO;
import com.placementplanner.model.ResumeInfo;
import com.placementplanner.model.Student;
import com.placementplanner.service.ExternalApplicationService;
import com.placementplanner.service.JobDiscoveryService;
import com.placementplanner.service.ResumeService;
import com.placementplanner.util.DBUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Student student = (Student) session.getAttribute("student");
        long studentId = student.getId();

        try (Connection con = DBUtil.getConnection()) {
            // Refresh student record from Oracle Database if available
            try {
                Student freshStudent = studentDAO.findById(con, studentId);
                if (freshStudent != null) {
                    student = freshStudent;
                    session.setAttribute("student", freshStudent);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Non-fatal: could not refresh student profile from DB: " + e.getMessage());
            }

            // Consolidated retrieval
            List<Map<String, Object>> allJobs = JobDiscoveryService.getAllOpportunities(con);
            int totalJobs = allJobs.size();
            List<Map<String, Object>> recentJobs = JobDiscoveryService.getRecentOpportunities(con, 3);

            List<Map<String, Object>> skills = skillDAO.findByStudent(con, studentId);
            List<Map<String, Object>> tasks = taskDAO.findByStudent(con, studentId);
            Set<Long> appliedJobIds = applicationDAO.getAppliedJobIds(con, studentId);

            // Fetch resume metadata and verification status
            ResumeInfo resumeInfo = ResumeService.getResumeInfo(studentId);

            // Compute statistics in-memory
            int totalSkills = skills.size();
            int totalTasks = tasks.size();
            int completedTasks = 0;
            for (Map<String, Object> task : tasks) {
                if ("Completed".equalsIgnoreCase((String) task.get("status"))) {
                    completedTasks++;
                }
            }
            int externalAppliedCount = ExternalApplicationService.getTrackedApplications(studentId).size();
            int appliedCount = appliedJobIds.size() + externalAppliedCount;

            int progressPercent = 0;
            if (totalTasks > 0) {
                progressPercent = (int) Math.round((completedTasks * 100.0) / totalTasks);
            }

            // Generate Placement Readiness Intelligence Report
            com.placementplanner.service.PlacementIntelligenceService.ReadinessReport readinessReport =
                    com.placementplanner.service.PlacementIntelligenceService.generateReadinessReport(
                            student, skills, resumeInfo, tasks, allJobs
                    );

            // Expose attributes to dashboard.jsp
            request.setAttribute("student", student);
            request.setAttribute("resumeInfo", resumeInfo);
            request.setAttribute("totalJobs", totalJobs);
            request.setAttribute("appliedCount", appliedCount);
            request.setAttribute("totalSkills", totalSkills);
            request.setAttribute("totalTasks", totalTasks);
            request.setAttribute("completedTasks", completedTasks);
            request.setAttribute("progressPercent", progressPercent);
            request.setAttribute("readinessReport", readinessReport);

            request.setAttribute("recentJobs", recentJobs);
            request.setAttribute("jobs", recentJobs);
            request.setAttribute("skills", skills);
            request.setAttribute("tasks", tasks);
            request.setAttribute("appliedJobIds", appliedJobIds);

            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Dashboard database query failed (ORA-" + e.getErrorCode() + "): " + e.getMessage(), e);

            ResumeInfo resumeInfo = ResumeService.getResumeInfo(studentId);
            List<Map<String, Object>> verifiedRecent = JobDiscoveryService.getRecentOpportunities(null, 3);
            com.placementplanner.service.PlacementIntelligenceService.ReadinessReport fallbackReport =
                    com.placementplanner.service.PlacementIntelligenceService.generateReadinessReport(
                            student, Collections.emptyList(), resumeInfo, Collections.emptyList(), verifiedRecent
                    );

            request.setAttribute("student", student);
            request.setAttribute("resumeInfo", resumeInfo);
            request.setAttribute("totalJobs", verifiedRecent.size());
            request.setAttribute("appliedCount", 0);
            request.setAttribute("totalSkills", 0);
            request.setAttribute("totalTasks", 0);
            request.setAttribute("completedTasks", 0);
            request.setAttribute("progressPercent", 0);
            request.setAttribute("readinessReport", fallbackReport);

            request.setAttribute("recentJobs", verifiedRecent);
            request.setAttribute("jobs", verifiedRecent);
            request.setAttribute("skills", Collections.emptyList());
            request.setAttribute("tasks", Collections.emptyList());
            request.setAttribute("appliedJobIds", Collections.emptySet());

            request.setAttribute("dbError", "Notice: Oracle database session busy. Displaying cached verified opportunities.");

            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
        }
    }
}
