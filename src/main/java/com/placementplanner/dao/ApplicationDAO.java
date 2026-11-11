package com.placementplanner.dao;

import com.placementplanner.util.DBUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationDAO {

    private static final Logger LOGGER = Logger.getLogger(ApplicationDAO.class.getName());

    /**
     * Checks whether the student has already applied for the specified job.
     */
    public boolean hasApplied(long studentId, long jobId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE student_id = ? AND job_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Submits a job application in Oracle Database.
     */
    public boolean apply(long studentId, long jobId) throws SQLException {
        if (studentId <= 0 || jobId <= 0) {
            throw new IllegalArgumentException("Valid student ID and job ID are required.");
        }

        if (hasApplied(studentId, jobId)) {
            return false;
        }

        // 1. Primary approach: Oracle 10g schema with APPLICATIONS_SEQ and application_date
        String seqSql = "INSERT INTO applications (application_id, student_id, job_id, status, application_date) "
                      + "VALUES (APPLICATIONS_SEQ.NEXTVAL, ?, ?, 'APPLIED', SYSDATE)";

        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(seqSql)) {
                ps.setLong(1, studentId);
                ps.setLong(2, jobId);
                ps.executeUpdate();
                return true;
            } catch (SQLException e1) {
                if (e1.getErrorCode() == 1) {
                    return false; // Unique constraint violation
                }

                LOGGER.log(Level.INFO, "Primary applications sequence insert yielded ORA-" + e1.getErrorCode() + ", trying Oracle 10g fallback");

                // 2. Fallback: Oracle 10g compatible INSERT SELECT NVL(MAX(application_id), 0) + 1
                String maxSql = "INSERT INTO applications (application_id, student_id, job_id, status, application_date) "
                              + "SELECT NVL(MAX(application_id), 0) + 1, ?, ?, 'APPLIED', SYSDATE FROM applications";
                try (PreparedStatement psMax = con.prepareStatement(maxSql)) {
                    psMax.setLong(1, studentId);
                    psMax.setLong(2, jobId);
                    psMax.executeUpdate();
                    return true;
                } catch (SQLException e2) {
                    if (e2.getErrorCode() == 1) return false;
                    throw e1;
                }
            }
        }
    }

    /**
     * Retrieves all job IDs that the student has applied for.
     */
    public Set<Long> getAppliedJobIds(long studentId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return getAppliedJobIds(con, studentId);
        }
    }

    /**
     * Retrieves all job IDs that the student has applied for using an existing connection.
     */
    public Set<Long> getAppliedJobIds(Connection con, long studentId) throws SQLException {
        Set<Long> set = new HashSet<>();
        String sql = "SELECT job_id FROM applications WHERE student_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getLong("job_id"));
                }
            }
        }
        return set;
    }

    /**
     * Returns total number of applications submitted by a specific student.
     */
    public int countByStudent(long studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE student_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves all job applications submitted by the student with job details in a single query.
     */
    public List<Map<String, Object>> findByStudent(long studentId) throws SQLException {
        List<Map<String, Object>> applications = new ArrayList<>();

        String joinSql = "SELECT a.*, "
                       + "       j.job_title AS j_title_alt, "
                       + "       c.company_name AS j_comp, j.location AS j_loc, j.deadline AS j_deadline "
                       + "FROM applications a "
                       + "LEFT JOIN jobs j ON a.job_id = j.job_id "
                       + "LEFT JOIN companies c ON j.company_id = c.company_id "
                       + "WHERE a.student_id = ? "
                       + "ORDER BY 1 DESC";

        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(joinSql)) {
                ps.setLong(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        applications.add(mapJoinedApplicationRow(rs));
                    }
                    return applications;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.INFO, "Join query for applications yielded ORA-" + e.getErrorCode() + ", falling back to simple query");
                return findByStudentSimple(con, studentId);
            }
        }
    }

    private List<Map<String, Object>> findByStudentSimple(Connection con, long studentId) throws SQLException {
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications WHERE student_id = ? ORDER BY 1 DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> app = new LinkedHashMap<>();
                    long appId = DBUtil.hasColumn(rs, "application_id")
                            ? rs.getLong("application_id")
                            : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 0L);
                    long jobId = rs.getLong("job_id");

                    Date appDate = null;
                    if (DBUtil.hasColumn(rs, "application_date")) {
                        appDate = rs.getDate("application_date");
                    } else if (DBUtil.hasColumn(rs, "applied_at")) {
                        appDate = rs.getDate("applied_at");
                    }

                    String status = DBUtil.hasColumn(rs, "status") ? rs.getString("status") : "APPLIED";

                    app.put("id", appId);
                    app.put("applicationId", appId);
                    app.put("jobId", jobId);
                    app.put("appliedAt", appDate != null ? appDate.toString() : "Recent");
                    app.put("status", status != null ? status : "Applied");
                    app.put("title", "Campus Drive #" + jobId);
                    app.put("company", "Campus Recruiter");
                    app.put("location", "On-Site");
                    applications.add(app);
                }
            }
        }
        return applications;
    }

    /**
     * Retrieves all applications with candidate details for Administrators.
     */
    public List<Map<String, Object>> findAllForAdmin() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT a.*, "
                   + "       s.full_name AS s_fullname, s.name AS s_name, s.email AS s_email, s.department AS s_dept, "
                   + "       j.job_title AS j_title_alt, j.location AS j_loc, "
                   + "       c.company_name AS j_comp "
                   + "FROM applications a "
                   + "LEFT JOIN students s ON a.student_id = s.student_id "
                   + "LEFT JOIN jobs j ON a.job_id = j.job_id "
                   + "LEFT JOIN companies c ON j.company_id = c.company_id "
                   + "ORDER BY 1 DESC";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                long appId = DBUtil.hasColumn(rs, "application_id")
                        ? rs.getLong("application_id")
                        : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 0L);

                String studentName = DBUtil.hasColumn(rs, "s_fullname") && rs.getString("s_fullname") != null
                        ? rs.getString("s_fullname")
                        : (DBUtil.hasColumn(rs, "s_name") && rs.getString("s_name") != null ? rs.getString("s_name") : "Student");

                String jobTitle = DBUtil.hasColumn(rs, "j_title_alt") && rs.getString("j_title_alt") != null
                        ? rs.getString("j_title_alt")
                        : ("Opportunity #" + rs.getLong("job_id"));

                Date appDate = null;
                if (DBUtil.hasColumn(rs, "application_date")) {
                    appDate = rs.getDate("application_date");
                } else if (DBUtil.hasColumn(rs, "applied_at")) {
                    appDate = rs.getDate("applied_at");
                }

                String comp = (DBUtil.hasColumn(rs, "j_comp") && rs.getString("j_comp") != null)
                        ? rs.getString("j_comp") : "Campus Recruiter";

                row.put("id", appId);
                row.put("studentId", rs.getLong("student_id"));
                row.put("studentName", studentName);
                row.put("studentEmail", rs.getString("s_email"));
                row.put("department", rs.getString("s_dept") != null ? rs.getString("s_dept") : "CSE");
                row.put("jobId", rs.getLong("job_id"));
                row.put("jobTitle", jobTitle);
                row.put("company", comp);
                row.put("location", rs.getString("j_loc") != null ? rs.getString("j_loc") : "On-Site");
                row.put("status", DBUtil.hasColumn(rs, "status") ? rs.getString("status") : "APPLIED");
                row.put("appliedAt", appDate);

                list.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Admin applications query fallback: " + e.getMessage());
        }

        return list;
    }

    /**
     * Updates application status by Administrator.
     */
    public boolean updateStatus(long applicationId, String newStatus) throws SQLException {
        if (applicationId <= 0 || newStatus == null || newStatus.isBlank()) {
            return false;
        }

        String sql = "UPDATE applications SET status = ? WHERE application_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus.trim().toUpperCase());
            ps.setLong(2, applicationId);
            return ps.executeUpdate() > 0;
        }
    }

    private Map<String, Object> mapJoinedApplicationRow(ResultSet rs) throws SQLException {
        Map<String, Object> app = new LinkedHashMap<>();

        long appId = DBUtil.hasColumn(rs, "application_id")
                ? rs.getLong("application_id")
                : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 0L);
        long jobId = rs.getLong("job_id");

        Date appDate = null;
        if (DBUtil.hasColumn(rs, "application_date")) {
            appDate = rs.getDate("application_date");
        } else if (DBUtil.hasColumn(rs, "applied_at")) {
            appDate = rs.getDate("applied_at");
        }

        String status = DBUtil.hasColumn(rs, "status") ? rs.getString("status") : "APPLIED";

        String title = "Campus Drive #" + jobId;
        if (DBUtil.hasColumn(rs, "j_title_alt") && rs.getString("j_title_alt") != null) {
            title = rs.getString("j_title_alt");
        } else if (DBUtil.hasColumn(rs, "j_title_std") && rs.getString("j_title_std") != null) {
            title = rs.getString("j_title_std");
        }

        String company = (DBUtil.hasColumn(rs, "j_comp") && rs.getString("j_comp") != null)
                ? rs.getString("j_comp") : "Campus Recruiter";
        String location = (DBUtil.hasColumn(rs, "j_loc") && rs.getString("j_loc") != null)
                ? rs.getString("j_loc") : "On-Site";
        Date deadline = DBUtil.hasColumn(rs, "j_deadline") ? rs.getDate("j_deadline") : null;

        app.put("id", appId);
        app.put("applicationId", appId);
        app.put("jobId", jobId);
        app.put("appliedAt", appDate != null ? appDate.toString() : "Recent");
        app.put("status", status != null ? status : "Applied");
        app.put("title", title);
        app.put("company", company);
        app.put("location", location);
        app.put("deadline", deadline);

        return app;
    }
}