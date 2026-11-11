package com.placementplanner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class DbDiagnostic {
    public static void main(String[] args) {
        String url = System.getProperty("db.url", "jdbc:oracle:thin:@//127.0.0.1:1521/XE");
        String user = System.getProperty("db.user", "system");
        String pass = System.getProperty("db.password", "system");

        System.out.println("Testing connection to: " + url + " as " + user);
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            System.out.println("OracleDriver class loaded successfully: " + oracle.jdbc.OracleDriver.class.getName());
        } catch (Exception e) {
            System.out.println("Driver load failed: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("CONNECTED SUCCESSFULLY!");
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("Database Product Name: " + meta.getDatabaseProductName());
            System.out.println("Database Product Version: " + meta.getDatabaseProductVersion());
            System.out.println("Driver Name: " + meta.getDriverName());
            System.out.println("Driver Version: " + meta.getDriverVersion());



            // Inspect application tables and counts
            String[] appTables = {"STUDENTS", "JOBS", "SKILLS", "TASKS", "APPLICATIONS", "COMPANIES"};
            for (String tbl : appTables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM " + tbl)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    System.out.println("\n=== TABLE " + tbl + " (Columns: " + cols + ") ===");
                    for (int c = 1; c <= cols; c++) {
                        System.out.print(md.getColumnName(c) + " (" + md.getColumnTypeName(c) + ")\t");
                    }
                    System.out.println();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        for (int c = 1; c <= cols; c++) {
                            System.out.print(rs.getString(c) + "\t");
                        }
                        System.out.println();
                    }
                    System.out.println("Total rows in " + tbl + ": " + count);
                } catch (Exception ex) {
                    System.out.println("Table " + tbl + " error/not found: " + ex.getMessage());
                }
            }

            // Inspect triggers
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT trigger_name, table_name, status FROM user_triggers")) {
                System.out.println("\n=== TRIGGERS ===");
                while (rs.next()) {
                    System.out.println("Trigger: " + rs.getString(1) + " ON " + rs.getString(2) + " (" + rs.getString(3) + ")");
                }
            } catch (Exception ex) {
                System.out.println("Triggers check: " + ex.getMessage());
            }

            // Inspect all user tables
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name")) {
                System.out.println("\n=== ALL USER TABLES ===");
                while (rs.next()) {
                    System.out.println("Table: " + rs.getString(1));
                }
            } catch (Exception ex) {
                System.out.println("User tables check: " + ex.getMessage());
            }

            // Inspect all user sequences
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT sequence_name, min_value, max_value, increment_by, last_number FROM user_sequences ORDER BY sequence_name")) {
                System.out.println("\n=== ALL USER SEQUENCES ===");
                while (rs.next()) {
                    System.out.println("Sequence: " + rs.getString(1) + " (last_number=" + rs.getLong(5) + ", inc=" + rs.getLong(4) + ")");
                }
            } catch (Exception ex) {
                System.out.println("User sequences check: " + ex.getMessage());
            }

            // Inspect constraints
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT table_name, constraint_name, constraint_type, search_condition FROM user_constraints WHERE table_name IN ('STUDENTS','JOBS','SKILLS','TASKS','APPLICATIONS') ORDER BY table_name, constraint_type")) {
                System.out.println("\n=== CONSTRAINTS ===");
                while (rs.next()) {
                    System.out.println(rs.getString(1) + ": " + rs.getString(2) + " (" + rs.getString(3) + ") " + rs.getString(4));
                }
            } catch (Exception ex) {
                System.out.println("Constraints check: " + ex.getMessage());
            }

            // Test task insert dry-run (transaction rolled back immediately)
            try {
                conn.setAutoCommit(false);
                String testSql = "INSERT INTO tasks (task_id, student_id, title, due_date, status, created_at) VALUES (TASKS_SEQ.NEXTVAL, 1, 'Test Task', SYSDATE, 'Pending', SYSDATE)";
                try (Statement stmt = conn.createStatement()) {
                    int rows = stmt.executeUpdate(testSql);
                    System.out.println("Dry-run task insert rows affected: " + rows);
                }
                conn.rollback();
                conn.setAutoCommit(true);
                System.out.println("Dry-run task insert succeeded and rolled back!");
            } catch (Exception ex) {
                System.out.println("Dry-run task insert failed: " + ex.getClass().getName() + ": " + ex.getMessage());
                try { conn.rollback(); conn.setAutoCommit(true); } catch (Exception ignored) {}
            }

            // Inspect session / process counts if accessible (read-only)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count(*) FROM v$session")) {
                if (rs.next()) {
                    System.out.println("\nCurrent v$session count: " + rs.getInt(1));
                }
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            System.err.println("CONNECTION FAILED: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
