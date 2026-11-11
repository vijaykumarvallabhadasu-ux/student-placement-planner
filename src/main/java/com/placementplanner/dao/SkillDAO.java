package com.placementplanner.dao;

import com.placementplanner.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkillDAO {

    private static final Logger LOGGER = Logger.getLogger(SkillDAO.class.getName());

    /**
     * Find all skills belonging to a particular student.
     */
    public List<Map<String, Object>> findByStudent(long studentId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return findByStudent(con, studentId);
        }
    }

    /**
     * Find all skills belonging to a student using an existing connection.
     */
    public List<Map<String, Object>> findByStudent(Connection con, long studentId) throws SQLException {
        List<Map<String, Object>> skills = new ArrayList<>();
        // Oracle 10g schema order by skill_id descending
        String sql = "SELECT * FROM skills WHERE student_id = ? ORDER BY 1 DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> skill = new LinkedHashMap<>();

                    long id = DBUtil.hasColumn(rs, "skill_id")
                            ? rs.getLong("skill_id")
                            : (DBUtil.hasColumn(rs, "id") ? rs.getLong("id") : 0L);

                    String name = DBUtil.hasColumn(rs, "skill_name") ? rs.getString("skill_name")
                            : (DBUtil.hasColumn(rs, "name") ? rs.getString("name") : "Skill");
                    String proficiency = DBUtil.hasColumn(rs, "proficiency") ? rs.getString("proficiency") : "Proficient";

                    skill.put("id", id);
                    skill.put("skill_id", id);
                    skill.put("skillName", name);
                    skill.put("skill_name", name);
                    skill.put("proficiency", proficiency != null ? proficiency : "Proficient");

                    skills.add(skill);
                }
            }
        }

        return skills;
    }

    /**
     * Checks whether a student already has a skill recorded (case-insensitive).
     */
    public boolean hasSkill(long studentId, String skillName) throws SQLException {
        if (studentId <= 0 || skillName == null || skillName.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM skills WHERE student_id = ? AND LOWER(TRIM(skill_name)) = LOWER(TRIM(?))";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setString(2, skillName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Add a new skill for a student with duplicate prevention.
     */
    public void add(long studentId, String skillName) throws SQLException {
        add(studentId, skillName, "Proficient");
    }

    public void add(long studentId, String skillName, String proficiency) throws SQLException {
        if (skillName == null || skillName.trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name cannot be empty.");
        }
        String prof = (proficiency == null || proficiency.isBlank()) ? "Proficient" : proficiency.trim();
        String trimmedSkill = skillName.trim();

        // Prevent duplicate skill entries for the student
        if (hasSkill(studentId, trimmedSkill)) {
            return;
        }

        // 1. Primary approach: Oracle 10g sequence SKILLS_SEQ with correct primary key 'skill_id'
        String seqSql = "INSERT INTO skills (skill_id, student_id, skill_name, proficiency, created_at) "
                      + "VALUES (SKILLS_SEQ.NEXTVAL, ?, ?, ?, SYSDATE)";

        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(seqSql)) {
                ps.setLong(1, studentId);
                ps.setString(2, trimmedSkill);
                ps.setString(3, prof);
                ps.executeUpdate();
                return;
            } catch (SQLException e1) {
                // Ignore unique constraint violation (ORA-00001)
                if (e1.getErrorCode() == 1) {
                    return;
                }

                LOGGER.log(Level.INFO, "Primary sequence insert for skill yielded ORA-" + e1.getErrorCode() + ", attempting fallback");

                // 2. Fallback: Oracle 10g compatible INSERT SELECT NVL(MAX(skill_id), 0) + 1
                String maxSql = "INSERT INTO skills (skill_id, student_id, skill_name, proficiency, created_at) "
                              + "SELECT NVL(MAX(skill_id), 0) + 1, ?, ?, ?, SYSDATE FROM skills";
                try (PreparedStatement psMax = con.prepareStatement(maxSql)) {
                    psMax.setLong(1, studentId);
                    psMax.setString(2, trimmedSkill);
                    psMax.setString(3, prof);
                    psMax.executeUpdate();
                    return;
                } catch (SQLException e2) {
                    if (e2.getErrorCode() == 1) return;
                    throw e1;
                }
            }
        }
    }

    /**
     * Updates skill proficiency level.
     * Enforces student ownership and uses real Oracle column 'skill_id'.
     */
    public boolean updateProficiency(long studentId, long skillId, String proficiency) throws SQLException {
        if (studentId <= 0 || skillId <= 0 || proficiency == null || proficiency.isBlank()) {
            return false;
        }

        String sql = "UPDATE skills SET proficiency = ? WHERE student_id = ? AND skill_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, proficiency.trim());
            ps.setLong(2, studentId);
            ps.setLong(3, skillId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Delete a skill only if it belongs to the specified student.
     * Enforces student ownership and uses real Oracle column 'skill_id'.
     */
    public boolean delete(long studentId, long skillId) throws SQLException {
        if (studentId <= 0 || skillId <= 0) {
            return false;
        }

        String sql = "DELETE FROM skills WHERE student_id = ? AND skill_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, skillId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Count total skills added by a student.
     */
    public int countByStudent(long studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM skills WHERE student_id = ?";
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
