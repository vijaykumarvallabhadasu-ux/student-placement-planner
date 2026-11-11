package com.placementplanner.web;

import com.placementplanner.model.ResumeInfo;
import com.placementplanner.model.Student;
import com.placementplanner.service.ResumeService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet(urlPatterns = {
    "/resume",
    "/resume/upload",
    "/resume/download",
    "/resume/view",
    "/resume/delete"
})
@MultipartConfig(
    maxFileSize = 5 * 1024 * 1024,      // 5 MB max file size
    maxRequestSize = 6 * 1024 * 1024,   // 6 MB max request size
    fileSizeThreshold = 1024 * 1024     // 1 MB memory threshold before disk buffer
)
public class ResumeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ResumeServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Student student = (session != null) ? (Student) session.getAttribute("student") : null;

        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String path = request.getServletPath();

        // Support admin viewing with explicit studentId parameter
        long targetStudentId = student.getId();
        String studentIdParam = request.getParameter("studentId");
        if (studentIdParam != null && !studentIdParam.isBlank()) {
            if (student.isAdmin()) {
                try {
                    targetStudentId = Long.parseLong(studentIdParam.trim());
                } catch (NumberFormatException ignored) {
                }
            } else {
                // Prevent unauthorized privilege escalation / IDOR
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
                return;
            }
        }

        if ("/resume/download".equals(path) || "/resume/view".equals(path)) {
            serveResumeFile(request, response, targetStudentId, "/resume/view".equals(path));
            return;
        }

        // Default: redirect back to dashboard
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Student student = (session != null) ? (Student) session.getAttribute("student") : null;

        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String path = request.getServletPath();
        String referer = request.getHeader("Referer");
        String redirectTarget = (referer != null && referer.contains("/profile")) ? "/profile" : "/dashboard";

        if ("/resume/upload".equals(path) || "/resume".equals(path)) {
            try {
                Part filePart = request.getPart("resumeFile");
                if (filePart == null || filePart.getSize() == 0) {
                    redirectWithError(response, request.getContextPath() + redirectTarget, "Please choose a PDF or DOCX resume to upload.");
                    return;
                }

                if (filePart.getSize() > ResumeService.MAX_FILE_SIZE) {
                    redirectWithError(response, request.getContextPath() + redirectTarget, "File size exceeds the 5 MB limit. Please select a smaller document.");
                    return;
                }

                String submittedFileName = getFileName(filePart);
                byte[] fileBytes = filePart.getInputStream().readAllBytes();
                String declaredContentType = filePart.getContentType();

                ResumeInfo info = ResumeService.saveResume(
                        student.getId(),
                        submittedFileName,
                        fileBytes,
                        declaredContentType
                );

                String msg = "Resume uploaded successfully. " + info.getStatus().getLabel() + " (Score: " + info.getScore() + "/100).";
                response.sendRedirect(request.getContextPath() + redirectTarget + "?resumeSuccess=1&status=" + URLEncoder.encode(info.getStatus().name(), StandardCharsets.UTF_8));
                return;

            } catch (IllegalArgumentException e) {
                redirectWithError(response, request.getContextPath() + redirectTarget, e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Resume upload encountered error: " + e.getMessage(), e);
                redirectWithError(response, request.getContextPath() + redirectTarget, "An error occurred while processing the file. Ensure the document is not corrupted.");
            }
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void serveResumeFile(HttpServletRequest request, HttpServletResponse response, long studentId, boolean inline)
            throws IOException {

        ResumeInfo info = ResumeService.getResumeInfo(studentId);
        File file = ResumeService.getResumeFile(studentId);

        if (!info.isUploaded() || file == null || !file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No resume has been uploaded yet for this account.");
            return;
        }

        String contentType = info.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = info.getStoredFileName().endsWith(".pdf")
                    ? "application/pdf"
                    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        // Security headers to prevent MIME sniffing and unsafe script execution
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String dispositionType = inline ? "inline" : "attachment";
        String downloadName = info.getOriginalFileName();
        if (downloadName == null || downloadName.isBlank()) {
            downloadName = "Resume_" + studentId + (info.getStoredFileName().endsWith(".pdf") ? ".pdf" : ".docx");
        }
        // Sanitize header value
        downloadName = downloadName.replaceAll("[\r\n\"]", "");

        response.setContentType(contentType);
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition", dispositionType + "; filename=\"" + downloadName + "\"");

        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String token : contentDisposition.split(";")) {
                if (token.trim().startsWith("filename")) {
                    String fileName = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                    // Handle IE browser sending full file paths
                    return new File(fileName).getName();
                }
            }
        }
        return "resume.pdf";
    }

    private void redirectWithError(HttpServletResponse response, String targetBase, String errorMsg)
            throws IOException {
        String encoded = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);
        String sep = targetBase.contains("?") ? "&" : "?";
        response.sendRedirect(targetBase + sep + "resumeError=" + encoded);
    }
}
