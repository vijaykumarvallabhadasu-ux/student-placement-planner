package com.placementplanner.web;

import com.placementplanner.dao.ApplicationDAO;
import com.placementplanner.dao.SkillDAO;
import com.placementplanner.dao.StudentDAO;
import com.placementplanner.dao.TaskDAO;
import com.placementplanner.model.ResumeInfo;
import com.placementplanner.model.Student;
import com.placementplanner.service.ResumeService;

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

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());

    private final StudentDAO studentDAO = new StudentDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final TaskDAO taskDAO = new TaskDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Student student = (Student) session.getAttribute("student");

        try {
            // Retrieve fresh student data from Oracle Database and ProfileService
            Student freshStudent = studentDAO.findById(student.getId());
            if (freshStudent != null) {
                student = freshStudent;
                session.setAttribute("student", freshStudent);
            }

            int skillCount = skillDAO.countByStudent(student.getId());
            int applicationCount = applicationDAO.countByStudent(student.getId());
            int taskCount = taskDAO.countByStudent(student.getId());
            int completedTaskCount = taskDAO.countCompletedByStudent(student.getId());

            // Retrieve resume status
            ResumeInfo resumeInfo = ResumeService.getResumeInfo(student.getId());

            request.setAttribute("student", student);
            request.setAttribute("resumeInfo", resumeInfo);
            request.setAttribute("skillCount", skillCount);
            request.setAttribute("applicationCount", applicationCount);
            request.setAttribute("taskCount", taskCount);
            request.setAttribute("completedTaskCount", completedTaskCount);

            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load profile: " + e.getMessage(), e);
            throw new ServletException("Could not load student profile from Oracle Database", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Student student = (Student) session.getAttribute("student");
        String name = request.getParameter("name");
        String department = request.getParameter("department");
        String yearParam = request.getParameter("yearOfStudy");
        String cgpaParam = request.getParameter("cgpa");
        String phone = request.getParameter("phone");
        String collegeName = request.getParameter("collegeName");
        String bio = request.getParameter("bio");
        String linkedIn = request.getParameter("linkedIn");
        String gitHub = request.getParameter("gitHub");

        if (name == null || name.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/profile?error=noname");
            return;
        }

        int yearOfStudy = 4;
        if (yearParam != null && !yearParam.isBlank()) {
            try {
                yearOfStudy = Integer.parseInt(yearParam.trim());
                if (yearOfStudy < 1 || yearOfStudy > 5) {
                    yearOfStudy = 4;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        Double cgpa = null;
        if (cgpaParam != null && !cgpaParam.isBlank()) {
            try {
                cgpa = Double.parseDouble(cgpaParam.trim());
                if (cgpa < 0.0 || cgpa > 10.0) {
                    response.sendRedirect(request.getContextPath() + "/profile?error=invalidcgpa");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/profile?error=invalidcgpa");
                return;
            }
        }

        try {
            boolean updated = studentDAO.updateProfile(
                    student.getId(),
                    name.trim(),
                    department != null ? department.trim() : "CSE",
                    yearOfStudy,
                    cgpa,
                    phone != null ? phone.trim() : "",
                    collegeName != null ? collegeName.trim() : "",
                    bio != null ? bio.trim() : "",
                    linkedIn != null ? linkedIn.trim() : "",
                    gitHub != null ? gitHub.trim() : ""
            );

            if (updated) {
                // Update session instance
                Student refreshed = studentDAO.findById(student.getId());
                if (refreshed != null) {
                    session.setAttribute("student", refreshed);
                }
                response.sendRedirect(request.getContextPath() + "/profile?updated=1");
            } else {
                response.sendRedirect(request.getContextPath() + "/profile?error=failed");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update profile: " + e.getMessage(), e);
            throw new ServletException("Failed to update profile in Oracle Database: " + e.getMessage(), e);
        }
    }
}
