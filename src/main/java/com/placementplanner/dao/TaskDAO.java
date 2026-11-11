package com.placementplanner.dao;

import com.placementplanner.util.DBUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskDAO {

    private static final Logger LOGGER = Logger.getLogger(TaskDAO.class.getName());

    /**
     * Find all tasks belonging to a student, ordered by due date and ID.
     */
    public List<Map<String, Object>> findByStudent(long studentId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findByStudent(con, studentId);
        }
    }

    /**
     * Find all tasks belonging to a student using an existing connection.
     */
    public List<Map<String, Object>> findByStudent(Connection con, long studentId) throws SQLException {
        List<Map<String, Object>> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE student_id = ? ORDER BY due_date ASC NULLS LAST, 1 DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> task = new LinkedHashMap<>();

                    long id = DBUtil.hasColumn(rs, "task_id")
                            ? rs.getLong("task_id")
                            : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 0L);

                    String title = DBUtil.hasColumn(rs, "title") && rs.getString("title") != null
                            ? rs.getString("title")
                            : (DBUtil.hasColumn(rs, "task_title") ? rs.getString("task_title") : "Milestone");

                    String description = DBUtil.hasColumn(rs, "description") ? rs.getString("description") : "";
                    Date dueDate = rs.getDate("due_date");
                    String status = DBUtil.hasColumn(rs, "status") ? rs.getString("status") : "Pending";

                    task.put("id", id);
                    task.put("task_id", id);
                    task.put("title", title);
                    task.put("task_title", title);
                    task.put("description", description);
                    task.put("dueDate", dueDate);
                    task.put("due_date", dueDate);
                    task.put("status", status);

                    tasks.add(task);
                }
            }
        }

        return tasks;
    }

    /**
     * Checks whether a task with the given title already exists for the student.
     */
    public boolean hasTask(long studentId, String title) throws SQLException {
        if (studentId <= 0 || title == null || title.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM tasks WHERE student_id = ? AND LOWER(TRIM(title)) = LOWER(TRIM(?))";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, title.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                String fallbackSql = "SELECT COUNT(*) FROM tasks WHERE student_id = ? AND LOWER(TRIM(task_title)) = LOWER(TRIM(?))";
                try (Connection con = DBUtil.getConnection();
                     PreparedStatement ps = con.prepareStatement(fallbackSql)) {
                    ps.setLong(1, studentId);
                    ps.setString(2, title.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            } else {
                throw e;
            }
        }
        return false;
    }

    /**
     * Adds a new preparation task using TASKS_SEQ and correct column 'task_id'.
     */
    public void add(long studentId, String title, String dueDate) throws SQLException {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        Date sqlDate = null;
        if (dueDate != null && !dueDate.isBlank()) {
            try {
                sqlDate = Date.valueOf(LocalDate.parse(dueDate.trim()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String trimmedTitle = title.trim();

        // 1. Primary approach: Oracle 10g sequence TASKS_SEQ and column 'title'
        String seqSql = "INSERT INTO tasks (task_id, student_id, title, due_date, status, created_at) "
                      + "VALUES (TASKS_SEQ.NEXTVAL, ?, ?, ?, 'Pending', SYSDATE)";

        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(seqSql)) {
                ps.setLong(1, studentId);
                ps.setString(2, trimmedTitle);
                if (sqlDate != null) {
                    ps.setDate(3, sqlDate);
                } else {
                    ps.setNull(3, Types.DATE);
                }
                ps.executeUpdate();
                return;
            } catch (SQLException e1) {
                LOGGER.log(Level.INFO, "Primary sequence insert for task yielded ORA-" + e1.getErrorCode() + ", attempting fallback");

                // 2. Fallback: Oracle 10g compatible INSERT SELECT NVL(MAX(task_id), 0) + 1
                String maxSql = "INSERT INTO tasks (task_id, student_id, title, due_date, status, created_at) "
                              + "SELECT NVL(MAX(task_id), 0) + 1, ?, ?, ?, 'Pending', SYSDATE FROM tasks";
                try (PreparedStatement psMax = con.prepareStatement(maxSql)) {
                    psMax.setLong(1, studentId);
                    psMax.setString(2, trimmedTitle);
                    if (sqlDate != null) psMax.setDate(3, sqlDate); else psMax.setNull(3, Types.DATE);
                    psMax.executeUpdate();
                    return;
                } catch (SQLException e2) {
                    // 3. Fallback for schema variant with identity column 'id' and 'task_title'
                    if (e1.getErrorCode() == 904 || e2.getErrorCode() == 904) {
                        String idSql = "INSERT INTO tasks (student_id, task_title, due_date, status, created_at) "
                                     + "VALUES (?, ?, ?, 'Pending', SYSDATE)";
                        try (PreparedStatement psId = con.prepareStatement(idSql)) {
                            psId.setLong(1, studentId);
                            psId.setString(2, trimmedTitle);
                            if (sqlDate != null) psId.setDate(3, sqlDate); else psId.setNull(3, Types.DATE);
                            psId.executeUpdate();
                            return;
                        } catch (SQLException e3) {
                            throw e1;
                        }
                    }
                    throw e1;
                }
            }
        }
    }

    /**
     * Updates an existing task's title and target due date.
     * Enforces student ownership and uses real Oracle columns.
     */
    public boolean update(long studentId, long taskId, String title, String dueDate) throws SQLException {
        if (studentId <= 0 || taskId <= 0 || title == null || title.isBlank()) {
            return false;
        }

        Date sqlDate = null;
        if (dueDate != null && !dueDate.isBlank()) {
            try {
                sqlDate = Date.valueOf(LocalDate.parse(dueDate.trim()));
            } catch (DateTimeParseException ignored) {
            }
        }

        String trimmedTitle = title.trim();
        String sql = "UPDATE tasks SET title = ?, due_date = ? WHERE student_id = ? AND task_id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trimmedTitle);
            if (sqlDate != null) ps.setDate(2, sqlDate); else ps.setNull(2, Types.DATE);
            ps.setLong(3, studentId);
            ps.setLong(4, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                String fallbackSql = "UPDATE tasks SET task_title = ?, due_date = ? WHERE student_id = ? AND id = ?";
                try (Connection con = DBUtil.getConnection();
                     PreparedStatement psFallback = con.prepareStatement(fallbackSql)) {
                    psFallback.setString(1, trimmedTitle);
                    if (sqlDate != null) psFallback.setDate(2, sqlDate); else psFallback.setNull(2, Types.DATE);
                    psFallback.setLong(3, studentId);
                    psFallback.setLong(4, taskId);
                    return psFallback.executeUpdate() > 0;
                }
            }
            throw e;
        }
    }

    /**
     * Updates task completion status ('Completed' or 'Pending').
     * Enforces student ownership and uses real Oracle columns.
     */
    public boolean updateStatus(long studentId, long taskId, String status) throws SQLException {
        if (!"Completed".equalsIgnoreCase(status) && !"Pending".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid task status: " + status);
        }

        String normalizedStatus = "Completed".equalsIgnoreCase(status) ? "Completed" : "Pending";
        String sql = "UPDATE tasks SET status = ? WHERE student_id = ? AND task_id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, normalizedStatus);
            ps.setLong(2, studentId);
            ps.setLong(3, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                String fallbackSql = "UPDATE tasks SET status = ? WHERE student_id = ? AND id = ?";
                try (Connection con = DBUtil.getConnection();
                     PreparedStatement psFallback = con.prepareStatement(fallbackSql)) {
                    psFallback.setString(1, normalizedStatus);
                    psFallback.setLong(2, studentId);
                    psFallback.setLong(3, taskId);
                    return psFallback.executeUpdate() > 0;
                }
            }
            throw e;
        }
    }

    /**
     * Deletes a preparation task belonging to the student.
     * Enforces student ownership and uses real Oracle column 'task_id'.
     */
    public boolean delete(long studentId, long taskId) throws SQLException {
        if (studentId <= 0 || taskId <= 0) {
            return false;
        }

        String sql = "DELETE FROM tasks WHERE student_id = ? AND task_id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 904) {
                String fallbackSql = "DELETE FROM tasks WHERE student_id = ? AND id = ?";
                try (Connection con = DBUtil.getConnection();
                     PreparedStatement psFallback = con.prepareStatement(fallbackSql)) {
                    psFallback.setLong(1, studentId);
                    psFallback.setLong(2, taskId);
                    return psFallback.executeUpdate() > 0;
                }
            }
            throw e;
        }
    }

    /**
     * Returns total task count for student.
     */
    public int countByStudent(long studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE student_id = ?";
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
     * Returns count of completed tasks for student.
     */
    public int countCompletedByStudent(long studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE student_id = ? AND LOWER(status) = 'completed'";
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
}
