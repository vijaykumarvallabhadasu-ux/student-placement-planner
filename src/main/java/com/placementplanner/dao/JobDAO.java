package com.placementplanner.dao;

import com.placementplanner.util.DBUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobDAO {

    private static final Logger LOGGER = Logger.getLogger(JobDAO.class.getName());

    /**
     * Retrieve all jobs ordered by primary key descending.
     */
    public List<Map<String, Object>> findAll() throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findAll(con);
        }
    }

    /**
     * Retrieve all jobs using an existing connection to avoid connection churn.
     */
    /**
     * Retrieve all jobs using an existing connection to avoid connection churn.
     */
    public List<Map<String, Object>> findAll(Connection con) throws SQLException {
        List<Map<String, Object>> jobs = new ArrayList<>();
        String sql = "SELECT j.*, c.company_name "
                   + "FROM jobs j "
                   + "LEFT JOIN companies c ON j.company_id = c.company_id "
                   + "ORDER BY 1 DESC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                jobs.add(mapJobRow(rs));
            }
        } catch (SQLException e) {
            // Fallback if companies join fails
            String simpleSql = "SELECT * FROM jobs ORDER BY 1 DESC";
            try (PreparedStatement psSimple = con.prepareStatement(simpleSql);
                 ResultSet rsSimple = psSimple.executeQuery()) {
                while (rsSimple.next()) {
                    jobs.add(mapJobRow(rsSimple));
                }
            }
        }
        return jobs;
    }

    /**
     * Retrieve up to limit recent jobs.
     */
    public List<Map<String, Object>> findRecent(int limit) throws SQLException {
        List<Map<String, Object>> all = findAll();
        if (all.size() <= limit) {
            return all;
        }
        return new ArrayList<>(all.subList(0, limit));
    }

    /**
     * Retrieve a specific job by ID.
     */
    public Map<String, Object> findById(long jobId) throws SQLException {
        String sql = "SELECT j.*, c.company_name "
                   + "FROM jobs j "
                   + "LEFT JOIN companies c ON j.company_id = c.company_id "
                   + "WHERE j.job_id = ?";
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, jobId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapJobRow(rs);
                    }
                }
            } catch (SQLException e) {
                String simpleSql = "SELECT * FROM jobs WHERE job_id = ?";
                try (PreparedStatement psSimple = con.prepareStatement(simpleSql)) {
                    psSimple.setLong(1, jobId);
                    try (ResultSet rs = psSimple.executeQuery()) {
                        if (rs.next()) {
                            return mapJobRow(rs);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Count total available jobs in database.
     */
    public int countTotalJobs() throws SQLException {
        String sql = "SELECT COUNT(*) FROM jobs";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Finds or creates a company entry in COMPANIES table, returning company_id.
     */
    private long findOrCreateCompany(Connection con, String companyName, String location) throws SQLException {
        String trimmedComp = (companyName != null && !companyName.isBlank()) ? companyName.trim() : "Campus Recruiter";
        String findSql = "SELECT company_id FROM companies WHERE LOWER(TRIM(company_name)) = LOWER(TRIM(?))";
        try (PreparedStatement psFind = con.prepareStatement(findSql)) {
            psFind.setString(1, trimmedComp);
            try (ResultSet rs = psFind.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("company_id");
                }
            }
        }

        // Insert new company using COMPANIES_SEQ
        String insSql = "INSERT INTO companies (company_id, company_name, location) "
                      + "VALUES (COMPANIES_SEQ.NEXTVAL, ?, ?)";
        try (PreparedStatement psIns = con.prepareStatement(insSql)) {
            psIns.setString(1, trimmedComp);
            psIns.setString(2, location != null ? location.trim() : "On-Site");
            psIns.executeUpdate();
        } catch (SQLException e) {
            String insFallback = "INSERT INTO companies (company_id, company_name, location) "
                               + "SELECT NVL(MAX(company_id), 0) + 1, ?, ? FROM companies";
            try (PreparedStatement psFall = con.prepareStatement(insFallback)) {
                psFall.setString(1, trimmedComp);
                psFall.setString(2, location != null ? location.trim() : "On-Site");
                psFall.executeUpdate();
            }
        }

        // Re-read generated ID
        try (PreparedStatement psFind = con.prepareStatement(findSql)) {
            psFind.setString(1, trimmedComp);
            try (ResultSet rs = psFind.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("company_id");
                }
            }
        }
        return 1L;
    }

    /**
     * Adds a new job listing to Oracle Database using JOBS_SEQ and correct column names.
     */
    public void add(
            String title,
            String company,
            String location,
            String description,
            String eligibility,
            Double minCgpa,
            String deadline
    ) throws SQLException {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Job title is required.");
        }
        String comp = (company != null && !company.isBlank()) ? company.trim() : "Campus Recruiter";
        String loc = (location != null && !location.isBlank()) ? location.trim() : "On-Site";
        String desc = (description != null) ? description.trim() : "";
        String elig = (eligibility != null && !eligibility.isBlank()) ? eligibility.trim() : "All Engineering Branches";
        double cgpa = (minCgpa != null) ? minCgpa : 0.0;
        Date deadlineDate = parseDate(deadline);
        if (deadlineDate == null) {
            deadlineDate = Date.valueOf(LocalDate.now().plusDays(30));
        }

        try (Connection con = DBUtil.getConnection()) {
            long companyId = findOrCreateCompany(con, comp, loc);

            // 1. Primary approach: Oracle 10g schema with job_id, company_id, job_title, JOBS_SEQ
            String seqSql = "INSERT INTO jobs (job_id, company_id, job_title, location, description, eligibility, min_cgpa, deadline, last_date, created_at) "
                          + "VALUES (JOBS_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";

            try (PreparedStatement ps = con.prepareStatement(seqSql)) {
                ps.setLong(1, companyId);
                ps.setString(2, title.trim());
                ps.setString(3, loc);
                ps.setString(4, desc);
                ps.setString(5, elig);
                ps.setDouble(6, cgpa);
                ps.setDate(7, deadlineDate);
                ps.setDate(8, deadlineDate);
                ps.executeUpdate();
                return;
            } catch (SQLException e1) {
                LOGGER.log(Level.INFO, "Primary job sequence insert yielded ORA-" + e1.getErrorCode() + ", trying Oracle 10g MAX fallback");

                // 2. Fallback: Oracle 10g compatible INSERT SELECT NVL(MAX(job_id), 0) + 1
                String maxJobSql = "INSERT INTO jobs (job_id, company_id, job_title, location, description, eligibility, min_cgpa, deadline, last_date, created_at) "
                                 + "SELECT NVL(MAX(job_id), 0) + 1, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE FROM jobs";
                try (PreparedStatement psMax = con.prepareStatement(maxJobSql)) {
                    psMax.setLong(1, companyId);
                    psMax.setString(2, title.trim());
                    psMax.setString(3, loc);
                    psMax.setString(4, desc);
                    psMax.setString(5, elig);
                    psMax.setDouble(6, cgpa);
                    psMax.setDate(7, deadlineDate);
                    psMax.setDate(8, deadlineDate);
                    psMax.executeUpdate();
                    return;
                } catch (SQLException e2) {
                    throw e1;
                }
            }
        }
    }

    private Map<String, Object> mapJobRow(ResultSet rs) throws SQLException {
        Map<String, Object> job = new LinkedHashMap<>();

        long id = DBUtil.hasColumn(rs, "job_id")
                ? rs.getLong("job_id")
                : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 1L);

        String title = DBUtil.hasColumn(rs, "job_title") && rs.getString("job_title") != null
                ? rs.getString("job_title")
                : (DBUtil.hasColumn(rs, "title") ? rs.getString("title") : "Software Engineer");

        String company = "Campus Recruiter";
        if (DBUtil.hasColumn(rs, "company") && rs.getString("company") != null) {
            company = rs.getString("company");
        } else if (DBUtil.hasColumn(rs, "company_id")) {
            long compId = rs.getLong("company_id");
            if (!rs.wasNull()) {
                company = "Enterprise Partner #" + compId;
            }
        }

        String location = DBUtil.hasColumn(rs, "location") && rs.getString("location") != null
                ? rs.getString("location") : "Flexible / On-Site";
        String description = DBUtil.hasColumn(rs, "description") ? rs.getString("description") : "";

        String eligibility = "All Engineering Branches";
        if (DBUtil.hasColumn(rs, "eligibility") && rs.getString("eligibility") != null) {
            eligibility = rs.getString("eligibility");
        }

        Double minCgpa = 0.0;
        if (DBUtil.hasColumn(rs, "min_cgpa")) {
            minCgpa = rs.getDouble("min_cgpa");
        }

        Date deadline = null;
        if (DBUtil.hasColumn(rs, "deadline")) {
            deadline = rs.getDate("deadline");
        } else if (DBUtil.hasColumn(rs, "last_date")) {
            deadline = rs.getDate("last_date");
        }

        boolean isExpired = false;
        if (deadline != null) {
            LocalDate today = LocalDate.now();
            LocalDate dl = deadline.toLocalDate();
            isExpired = dl.isBefore(today);
        }

        job.put("id", id);
        job.put("job_id", id);
        job.put("title", title);
        job.put("job_title", title);
        job.put("jobTitle", title);
        job.put("company", company);
        job.put("location", location);
        job.put("description", description);
        job.put("eligibility", eligibility);
        job.put("minCgpa", minCgpa);
        job.put("min_cgpa", minCgpa);
        job.put("deadline", deadline);
        job.put("isExpired", isExpired);

        return job;
    }

    private Date parseDate(String dateValue) {
        if (dateValue == null || dateValue.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(dateValue.trim());
            return Date.valueOf(localDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
