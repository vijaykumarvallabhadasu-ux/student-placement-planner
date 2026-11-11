package com.placementplanner.web;

import com.placementplanner.dao.SkillDAO;
import com.placementplanner.model.Student;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/skills")
public class SkillServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SkillServlet.class.getName());

    private final SkillDAO dao = new SkillDAO();

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
            request.setAttribute("skills", dao.findByStudent(student.getId()));
            request.setAttribute("resumeInfo", com.placementplanner.service.ResumeService.getResumeInfo(student.getId()));
            request.getRequestDispatcher("/WEB-INF/views/skills.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Could not load skills from Oracle Database: " + e.getMessage(), e);
            request.setAttribute("skills", java.util.Collections.emptyList());
            request.setAttribute("resumeInfo", com.placementplanner.service.ResumeService.getResumeInfo(student.getId()));
            request.getRequestDispatcher("/WEB-INF/views/skills.jsp").forward(request, response);
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

        try {
            if ("delete".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                if (idParam == null || idParam.isBlank()) {
                    idParam = request.getParameter("skill_id");
                }
                if (idParam == null || idParam.isBlank()) {
                    response.sendRedirect(request.getContextPath() + "/skills?error=missingid");
                    return;
                }

                long skillId = Long.parseLong(idParam.trim());
                dao.delete(student.getId(), skillId);
                response.sendRedirect(request.getContextPath() + "/skills?deleted=1");

            } else if ("proficiency".equalsIgnoreCase(action)) {
                String idParam = request.getParameter("id");
                if (idParam == null || idParam.isBlank()) {
                    idParam = request.getParameter("skill_id");
                }
                String proficiency = request.getParameter("proficiency");
                if (idParam != null && proficiency != null && !proficiency.isBlank()) {
                    long skillId = Long.parseLong(idParam.trim());
                    dao.updateProficiency(student.getId(), skillId, proficiency.trim());
                    response.sendRedirect(request.getContextPath() + "/skills?updated=1");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/skills?error=invalidparam");

            } else if ("addAllDetected".equalsIgnoreCase(action)) {
                String skillsParam = request.getParameter("skills");
                if (skillsParam != null && !skillsParam.isBlank()) {
                    String[] detectedSkills = skillsParam.split(",");
                    int addedCount = 0;
                    for (String s : detectedSkills) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty() && !dao.hasSkill(student.getId(), trimmed)) {
                            dao.add(student.getId(), trimmed, "Proficient");
                            addedCount++;
                        }
                    }
                    response.sendRedirect(request.getContextPath() + "/skills?detectedAdded=" + addedCount);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/skills");

            } else {
                String skillName = request.getParameter("skill");
                String proficiency = request.getParameter("proficiency");
                if (skillName == null || skillName.isBlank()) {
                    response.sendRedirect(request.getContextPath() + "/skills?error=empty");
                    return;
                }

                String trimmedSkill = skillName.trim();
                if (dao.hasSkill(student.getId(), trimmedSkill)) {
                    if ("dashboard".equalsIgnoreCase(redirect)) {
                        response.sendRedirect(request.getContextPath() + "/dashboard?skillDuplicate=1");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/skills?duplicate=1");
                    }
                    return;
                }

                dao.add(student.getId(), trimmedSkill, (proficiency != null && !proficiency.isBlank()) ? proficiency.trim() : "Proficient");
                if ("dashboard".equalsIgnoreCase(redirect)) {
                    response.sendRedirect(request.getContextPath() + "/dashboard?skillAdded=1");
                } else {
                    response.sendRedirect(request.getContextPath() + "/skills?added=1");
                }
            }

        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Skill operation failed: " + e.getMessage(), e);
            response.sendRedirect(request.getContextPath() + "/skills?error=dbfailed");
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/skills?error=invalidid");
        }
    }
}
