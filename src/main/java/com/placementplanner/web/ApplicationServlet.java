package com.placementplanner.web;

import com.placementplanner.dao.ApplicationDAO;
import com.placementplanner.model.Student;
import com.placementplanner.service.ExternalApplicationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/applications")
public class ApplicationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ApplicationServlet.class.getName());

    private final ApplicationDAO dao = new ApplicationDAO();

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

        List<Map<String, Object>> internalApps = Collections.emptyList();
        try {
            internalApps = dao.findByStudent(studentId);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed reading internal applications: " + e.getMessage());
        }

        List<Map<String, Object>> externalApps = ExternalApplicationService.getTrackedApplications(studentId);

        request.setAttribute("applications", internalApps);
        request.setAttribute("internalApplications", internalApps);
        request.setAttribute("externalApplications", externalApps);
        request.setAttribute("totalApplicationsCount", internalApps.size() + externalApps.size());

        request.getRequestDispatcher("/WEB-INF/views/applications.jsp").forward(request, response);
    }
}
