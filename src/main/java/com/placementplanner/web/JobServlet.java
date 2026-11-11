package com.placementplanner.web;

import com.placementplanner.dao.ApplicationDAO;
import com.placementplanner.dao.JobDAO;
import com.placementplanner.model.Student;
import com.placementplanner.service.ExternalApplicationService;
import com.placementplanner.service.JobDiscoveryService;
import com.placementplanner.util.DBUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

@WebServlet(urlPatterns = {"/jobs", "/apply"})
public class JobServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(JobServlet.class.getName());

    private final JobDAO jobDAO = new JobDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final com.placementplanner.dao.SkillDAO skillDAO = new com.placementplanner.dao.SkillDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Student student = (session != null) ? (Student) session.getAttribute("student") : null;

        try (Connection con = DBUtil.getConnection()) {
            List<Map<String, Object>> jobs = JobDiscoveryService.getAllOpportunities(con);

            Set<Long> appliedJobIds = new HashSet<>();
            Map<Long, com.placementplanner.service.PlacementIntelligenceService.JobMatchResult> jobMatches = new LinkedHashMap<>();

            if (student != null) {
                try {
                    appliedJobIds.addAll(applicationDAO.getAppliedJobIds(con, student.getId()));
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed reading internal applied job IDs: " + e.getMessage());
                }

                // Add external tracked application IDs
                List<Map<String, Object>> externalApps = ExternalApplicationService.getTrackedApplications(student.getId());
                for (Map<String, Object> ext : externalApps) {
                    Long jId = (Long) ext.get("jobId");
                    if (jId != null) {
                        appliedJobIds.add(jId);
                    }
                }

                // Load confirmed skills
                List<String> confirmedSkills = new ArrayList<>();
                try {
                    List<Map<String, Object>> skills = skillDAO.findByStudent(con, student.getId());
                    for (Map<String, Object> s : skills) {
                        String sName = (String) s.get("skillName");
                        if (sName != null && !sName.isBlank()) {
                            confirmedSkills.add(sName.trim());
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed loading student skills for job matching: " + e.getMessage());
                }

                // Load resume detected skills
                Set<String> resumeSkills = new HashSet<>();
                com.placementplanner.model.ResumeInfo resumeInfo = com.placementplanner.service.ResumeService.getResumeInfo(student.getId());
                if (resumeInfo != null && resumeInfo.getExtractedSkills() != null) {
                    for (String rs : resumeInfo.getExtractedSkills()) {
                        resumeSkills.add(rs.trim().toLowerCase());
                    }
                }

                // Calculate matching for every job
                for (Map<String, Object> job : jobs) {
                    long jId = (job.get("id") instanceof Number) ? ((Number) job.get("id")).longValue() : 0L;
                    com.placementplanner.service.PlacementIntelligenceService.JobMatchResult match =
                            com.placementplanner.service.PlacementIntelligenceService.evaluateJobMatch(job, confirmedSkills, resumeSkills);
                    jobMatches.put(jId, match);
                }
            }

            request.setAttribute("jobs", jobs);
            request.setAttribute("appliedJobIds", appliedJobIds);
            request.setAttribute("jobMatches", jobMatches);

            request.getRequestDispatcher("/WEB-INF/views/jobs.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load jobs from database, serving verified catalog", e);
            List<Map<String, Object>> verified = JobDiscoveryService.getAllOpportunities(null);
            request.setAttribute("jobs", verified);
            request.setAttribute("appliedJobIds", Collections.emptySet());
            request.setAttribute("jobMatches", Collections.emptyMap());
            request.getRequestDispatcher("/WEB-INF/views/jobs.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Student student = (Student) session.getAttribute("student");
        String action = request.getParameter("action");
        String jobIdParam = request.getParameter("jobId");

        if (jobIdParam == null || jobIdParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/jobs?error=missingid");
            return;
        }

        try {
            long jobId = Long.parseLong(jobIdParam.trim());
            Map<String, Object> job = JobDiscoveryService.findById(jobId);

            if (job == null) {
                response.sendRedirect(request.getContextPath() + "/jobs?error=notfound");
                return;
            }

            // Handle External Opportunity Tracking
            if (Boolean.TRUE.equals(job.get("isExternal")) || "recordExternal".equalsIgnoreCase(action)) {
                String title = (String) job.get("title");
                String company = (String) job.get("company");
                String url = (String) job.get("officialUrl");

                ExternalApplicationService.recordExternalApplication(
                        student.getId(),
                        jobId,
                        title,
                        company,
                        url,
                        "Applied on Employer Website"
                );

                response.sendRedirect(request.getContextPath() + "/jobs?externalRecorded=1");
                return;
            }

            // Handle Internal Campus Drive Application (saved in Oracle Database)
            if (applicationDAO.hasApplied(student.getId(), jobId)) {
                response.sendRedirect(request.getContextPath() + "/jobs?duplicate=1");
                return;
            }

            boolean applied = applicationDAO.apply(student.getId(), jobId);
            if (applied) {
                response.sendRedirect(request.getContextPath() + "/jobs?applied=1");
            } else {
                response.sendRedirect(request.getContextPath() + "/jobs?duplicate=1");
            }

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/jobs?error=invalidid");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Internal application failed: " + e.getMessage(), e);
            throw new ServletException("Application processing failed in Oracle Database: " + e.getMessage(), e);
        }
    }
}
