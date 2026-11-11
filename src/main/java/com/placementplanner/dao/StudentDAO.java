package com.placementplanner.dao;

import com.placementplanner.model.Student;
import com.placementplanner.service.ProfileService;
import com.placementplanner.util.DBUtil;
import com.placementplanner.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StudentDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

    /**
     * Checks whether an account with the specified email already exists.
     */
    public boolean emailExists(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM students WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Registers a new student account using SHA-256 password hashing.
     * Compatible with Oracle 10g sequence STUDENTS_SEQ, NVL(MAX), and dual column schemas.
     */
    public void register(String name, String email, String password) throws SQLException {
        if (name == null || name.isBlank() || email == null || email.isBlank()
                || password == null || password.length() < 6) {
            throw new IllegalArgumentException("Valid name, email, and password (at least 6 characters) are required.");
        }

        if (emailExists(email)) {
            throw new SQLException("An account with this email address already exists. Please sign in instead.");
        }

        String hashedPassword = PasswordUtil.hash(password);
        String trimmedName = name.trim();
        String trimmedEmail = email.trim().toLowerCase();

        // 1. Primary approach: Oracle 10g with STUDENTS_SEQ, populating both full_name and name
        String seqSql = "INSERT INTO students (student_id, full_name, name, email, password_hash, role, created_at) "
                      + "VALUES (STUDENTS_SEQ.NEXTVAL, ?, ?, ?, ?, 'STUDENT', SYSDATE)";

        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(seqSql)) {
                ps.setString(1, trimmedName);
                ps.setString(2, trimmedName);
                ps.setString(3, trimmedEmail);
                ps.setString(4, hashedPassword);
                ps.executeUpdate();
                return;
            } catch (SQLException e1) {
                LOGGER.log(Level.INFO, "Primary sequence insert attempt yielded (ORA-" + e1.getErrorCode() + "): " + e1.getMessage());

                // 2. Fallback: Oracle 10g compatible INSERT SELECT NVL(MAX(student_id), 0) + 1
                String maxStudentIdSql = "INSERT INTO students (student_id, full_name, name, email, password_hash, role, created_at) "
                                       + "SELECT NVL(MAX(student_id), 0) + 1, ?, ?, ?, ?, 'STUDENT', SYSDATE FROM students";
                try (PreparedStatement psMax = con.prepareStatement(maxStudentIdSql)) {
                    psMax.setString(1, trimmedName);
                    psMax.setString(2, trimmedName);
                    psMax.setString(3, trimmedEmail);
                    psMax.setString(4, hashedPassword);
                    psMax.executeUpdate();
                    return;
                } catch (SQLException e2) {
                    throw e1;
                }
            }
        }
    }

    /**
     * Authenticates a user by email and password.
     * Supports SHA-256 hashed passwords and legacy plaintext matches.
     */
    public Student authenticate(String email, String password) throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        String sql = "SELECT * FROM students WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbHash = rs.getString("password_hash");
                    String hashedInput = PasswordUtil.hash(password);

                    // Check SHA-256 match, or plaintext match for legacy test records
                    boolean isMatch = (dbHash != null && (dbHash.equalsIgnoreCase(hashedInput) || dbHash.equals(password)));

                    if (isMatch) {
                        return mapStudentRow(rs);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds a student by their unique primary key ID.
     */
    public Student findById(long id) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findById(con, id);
        }
    }

    /**
     * Finds a student using an existing connection to avoid connection churn.
     */
    public Student findById(Connection con, long id) throws SQLException {
        // Query using student_id (Oracle 10g schema)
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapStudentRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates student profile information in Oracle Database and application-managed store.
     * Preserves existing schema: updates FULL_NAME, NAME, DEPARTMENT, YEAR_OF_STUDY in Oracle STUDENTS,
     * and persists extended attributes (CGPA, phone, college) via ProfileService.
     */
    public boolean updateProfile(long id, String name, String department, int yearOfStudy, Double cgpa, String phone, String collegeName, String bio, String linkedIn, String gitHub) throws SQLException {
        if (id <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Valid student ID and name are required.");
        }

        String trimmedName = name.trim();
        String dept = (department != null && !department.isBlank()) ? department.trim() : "CSE";
        int year = yearOfStudy > 0 ? yearOfStudy : 4;

        // Update database-backed columns in STUDENTS (student_id is PK)
        String sql = "UPDATE students SET full_name = ?, name = ?, department = ?, year_of_study = ? WHERE student_id = ?";

        try (Connection con = DBUtil.getConnection()) {
            int rowsUpdated = 0;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, trimmedName);
                ps.setString(2, trimmedName);
                ps.setString(3, dept);
                ps.setInt(4, year);
                ps.setLong(5, id);
                rowsUpdated = ps.executeUpdate();
            } catch (SQLException e) {
                // Fallback if one of full_name / name is missing
                if (e.getErrorCode() == 904) {
                    String fallbackSql = "UPDATE students SET name = ?, department = ?, year_of_study = ? WHERE student_id = ?";
                    try (PreparedStatement psFallback = con.prepareStatement(fallbackSql)) {
                        psFallback.setString(1, trimmedName);
                        psFallback.setString(2, dept);
                        psFallback.setInt(3, year);
                        psFallback.setLong(4, id);
                        rowsUpdated = psFallback.executeUpdate();
                    }
                } else {
                    throw e;
                }
            }

            // Save extended profile attributes (CGPA, phone, college, bio, linkedIn, gitHub) to application storage
            ProfileService.saveExtendedProfile(id, cgpa, phone, collegeName, bio, linkedIn, gitHub);

            return rowsUpdated > 0;
        }
    }

    public boolean updateProfile(long id, String name, String department, int yearOfStudy, Double cgpa, String phone, String collegeName, String bio) throws SQLException {
        return updateProfile(id, name, department, yearOfStudy, cgpa, phone, collegeName, bio, null, null);
    }

    /**
     * Overload for backward compatibility.
     */
    public boolean updateProfile(long id, String name, String department, int yearOfStudy, Double cgpa) throws SQLException {
        return updateProfile(id, name, department, yearOfStudy, cgpa, null, null, null, null, null);
    }

    /**
     * Helper to map a database row to a Student model instance.
     */
    private Student mapStudentRow(ResultSet rs) throws SQLException {
        long studentId = DBUtil.hasColumn(rs, "student_id")
                ? rs.getLong("student_id")
                : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 1L);

        String studentName = DBUtil.hasColumn(rs, "full_name") && rs.getString("full_name") != null
                ? rs.getString("full_name")
                : (DBUtil.hasColumn(rs, "name") && rs.getString("name") != null ? rs.getString("name") : "Student");

        String studentEmail = rs.getString("email");
        String role = DBUtil.hasColumn(rs, "role") ? rs.getString("role") : "STUDENT";

        Student student = new Student(studentId, studentName, studentEmail, role);

        if (DBUtil.hasColumn(rs, "department")) {
            student.setDepartment(rs.getString("department"));
        } else if (DBUtil.hasColumn(rs, "branch")) {
            student.setDepartment(rs.getString("branch"));
        }

        if (DBUtil.hasColumn(rs, "year_of_study")) {
            student.setYearOfStudy(rs.getInt("year_of_study"));
        } else if (DBUtil.hasColumn(rs, "graduation_year")) {
            student.setYearOfStudy(rs.getInt("graduation_year"));
        }

        if (DBUtil.hasColumn(rs, "cgpa")) {
            double cgpa = rs.getDouble("cgpa");
            if (!rs.wasNull()) {
                student.setCgpa(cgpa);
            }
        }

        // Seamlessly merge extended profile attributes (phone, collegeName, cgpa, bio)
        ProfileService.populateExtendedProfile(student);

        return student;
    }
}
