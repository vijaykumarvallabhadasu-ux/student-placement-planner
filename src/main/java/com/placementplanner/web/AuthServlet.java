package com.placementplanner.web;

import com.placementplanner.dao.StudentDAO;
import com.placementplanner.model.Student;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final StudentDAO dao = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/logout".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login?loggedOut=1");
            return;
        }

        // If already authenticated, redirect to dashboard
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("student") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String page = "/register".equals(path) ? "register.jsp" : "login.jsp";
        request.getRequestDispatcher("/WEB-INF/views/" + page).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();

        try {
            if ("/register".equals(path)) {
                String name = request.getParameter("name");
                String email = request.getParameter("email");
                String password = request.getParameter("password");
                String confirmPassword = request.getParameter("confirmPassword");

                if (name == null || name.isBlank()
                        || email == null || email.isBlank()
                        || password == null || password.length() < 6) {
                    request.setAttribute("error", "Please provide a valid name, email, and password of at least 6 characters.");
                    request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
                    return;
                }

                String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
                if (!email.trim().matches(emailRegex)) {
                    request.setAttribute("error", "Please provide a valid email address format (e.g. name@college.edu).");
                    request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
                    return;
                }

                if (confirmPassword != null && !password.equals(confirmPassword)) {
                    request.setAttribute("error", "Passwords do not match.");
                    request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
                    return;
                }

                if (dao.emailExists(email.trim())) {
                    request.setAttribute("error", "An account with this email address already exists. Please sign in instead.");
                    request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
                    return;
                }

                dao.register(name.trim(), email.trim(), password);
                response.sendRedirect(request.getContextPath() + "/login?registered=1");
                return;
            }

            if ("/login".equals(path)) {
                String email = request.getParameter("email");
                String password = request.getParameter("password");

                if (email == null || email.isBlank() || password == null || password.isBlank()) {
                    request.setAttribute("error", "Please enter both email and password.");
                    request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                    return;
                }

                Student student = dao.authenticate(email.trim(), password);

                if (student == null) {
                    request.setAttribute("error", "Invalid email address or password. Please try again.");
                    request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                    return;
                }

                // Invalidate any pre-existing session to prevent session fixation
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                // Create fresh authenticated session
                HttpSession session = request.getSession(true);
                session.setAttribute("student", student);

                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (SQLException e) {
            request.setAttribute("error", "Database operation failed: " + e.getMessage());
            String target = "/register".equals(path) ? "register.jsp" : "login.jsp";
            request.getRequestDispatcher("/WEB-INF/views/" + target).forward(request, response);
        }
    }
}
