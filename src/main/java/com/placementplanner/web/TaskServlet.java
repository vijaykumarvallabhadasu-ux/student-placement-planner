package com.placementplanner.web;

import com.placementplanner.dao.TaskDAO;
import com.placementplanner.model.Student;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(TaskServlet.class.getName());

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
            request.setAttribute("tasks", taskDAO.findByStudent(student.getId()));
            request.getRequestDispatcher("/WEB-INF/views/tasks.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load tasks from Oracle Database (ORA-" + e.getErrorCode() + "): " + e.getMessage(), e);
            request.setAttribute("tasks", Collections.emptyList());
            request.setAttribute("dbError", "Notice: Oracle database session busy or temporarily unavailable. Please refresh in a moment.");
            request.getRequestDispatcher("/WEB-INF/views/tasks.jsp").forward(request, response);
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
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String redirect = request.getParameter("redirect");
        String referer = request.getHeader("Referer");
        boolean fromDashboard = "dashboard".equalsIgnoreCase(redirect) || (referer != null && referer.contains("/dashboard"));

        try {
            if ("status".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                if (idParam == null || idParam.isBlank()) {
                    idParam = request.getParameter("task_id");
                }
                String status = request.getParameter("status");

                if (idParam == null || idParam.isBlank() || status == null || status.isBlank()) {
                    response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?error=invalidstatus" : "/tasks?error=invalidstatus"));
                    return;
                }

                long taskId = Long.parseLong(idParam.trim());
                taskDAO.updateStatus(student.getId(), taskId, status.trim());
                response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?taskUpdated=1" : "/tasks?updated=1"));

            } else if ("delete".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                if (idParam == null || idParam.isBlank()) {
                    idParam = request.getParameter("task_id");
                }
                if (idParam == null || idParam.isBlank()) {
                    response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?error=missingid" : "/tasks?error=missingid"));
                    return;
                }

                long taskId = Long.parseLong(idParam.trim());
                taskDAO.delete(student.getId(), taskId);
                response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?taskDeleted=1" : "/tasks?deleted=1"));

            } else if ("edit".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                if (idParam == null || idParam.isBlank()) {
                    idParam = request.getParameter("task_id");
                }
                String title = request.getParameter("title");
                String dueDate = request.getParameter("dueDate");

                if (idParam == null || idParam.isBlank() || title == null || title.isBlank()) {
                    response.sendRedirect(request.getContextPath() + "/tasks?error=emptytitle");
                    return;
                }

                long taskId = Long.parseLong(idParam.trim());
                taskDAO.update(student.getId(), taskId, title.trim(), dueDate);
                response.sendRedirect(request.getContextPath() + "/tasks?updated=1");

            } else {
                String title = request.getParameter("title");
                String dueDate = request.getParameter("dueDate");

                if (title == null || title.isBlank()) {
                    response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?error=emptytitle" : "/tasks?error=emptytitle"));
                    return;
                }

                String trimmedTitle = title.trim();
                if (taskDAO.hasTask(student.getId(), trimmedTitle)) {
                    if ("jobs".equalsIgnoreCase(redirect)) {
                        response.sendRedirect(request.getContextPath() + "/jobs?taskDuplicate=1");
                    } else if (fromDashboard) {
                        response.sendRedirect(request.getContextPath() + "/dashboard?taskDuplicate=1");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/tasks?duplicate=1");
                    }
                    return;
                }

                taskDAO.add(student.getId(), trimmedTitle, dueDate);
                if ("jobs".equalsIgnoreCase(redirect)) {
                    response.sendRedirect(request.getContextPath() + "/jobs?taskAdded=1");
                } else if (fromDashboard) {
                    response.sendRedirect(request.getContextPath() + "/dashboard?taskAdded=1");
                } else {
                    response.sendRedirect(request.getContextPath() + "/tasks?added=1");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Task database operation failed: " + e.getMessage(), e);
            response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?error=dbfailed" : "/tasks?error=dbfailed"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + (fromDashboard ? "/dashboard?error=invalidid" : "/tasks?error=invalidid"));
        }
    }
}
