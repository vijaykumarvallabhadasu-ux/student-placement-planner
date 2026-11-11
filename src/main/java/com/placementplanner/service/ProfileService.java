package com.placementplanner.service;

import com.placementplanner.model.Student;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File-based profile storage service for extended student information
 * that is not stored in the existing Oracle Database schema (e.g. CGPA, phone, college name, bio).
 * Conforms strictly to the database preservation rule: does not alter Oracle tables or columns.
 */
public final class ProfileService {

    private static final Logger LOGGER = Logger.getLogger(ProfileService.class.getName());

    private static File storageDir;

    static {
        initStorageDir();
    }

    private static synchronized void initStorageDir() {
        if (storageDir != null && storageDir.exists()) {
            return;
        }

        // Check custom property, then user.home/.placementplanner/profiles, or fallback to current dir
        String customDir = System.getProperty("placementplanner.profile.dir");
        if (customDir != null && !customDir.isBlank()) {
            storageDir = new File(customDir.trim());
        } else {
            String userHome = System.getProperty("user.home", ".");
            storageDir = new File(userHome + File.separator + ".placementplanner" + File.separator + "profiles");
        }

        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (created) {
                LOGGER.info("Created profile storage directory: " + storageDir.getAbsolutePath());
            }
        }
    }

    public static File getStorageDirectory() {
        if (storageDir == null || !storageDir.exists()) {
            initStorageDir();
        }
        return storageDir;
    }

    /**
     * Loads extended profile attributes into the Student model instance.
     */
    public static void populateExtendedProfile(Student student) {
        if (student == null || student.getId() <= 0) {
            return;
        }

        File file = new File(getStorageDirectory(), "student_" + student.getId() + ".properties");
        if (!file.exists()) {
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

            props.load(reader);

            String phone = props.getProperty("phone");
            if (phone != null && !phone.isBlank()) {
                student.setPhone(phone.trim());
            }

            String college = props.getProperty("collegeName");
            if (college != null && !college.isBlank()) {
                student.setCollegeName(college.trim());
            }

            String cgpaStr = props.getProperty("cgpa");
            if (cgpaStr != null && !cgpaStr.isBlank()) {
                try {
                    student.setCgpa(Double.parseDouble(cgpaStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }

            String bio = props.getProperty("bio");
            if (bio != null && !bio.isBlank()) {
                student.setBio(bio.trim());
            }

            String linkedIn = props.getProperty("linkedIn");
            if (linkedIn != null && !linkedIn.isBlank()) {
                student.setLinkedIn(linkedIn.trim());
            }

            String gitHub = props.getProperty("gitHub");
            if (gitHub != null && !gitHub.isBlank()) {
                student.setGitHub(gitHub.trim());
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read extended profile for student " + student.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Saves extended profile attributes for a student.
     */
    public static synchronized boolean saveExtendedProfile(long studentId, Double cgpa, String phone, String collegeName, String bio, String linkedIn, String gitHub) {
        if (studentId <= 0) {
            return false;
        }

        File file = new File(getStorageDirectory(), "student_" + studentId + ".properties");
        Properties props = new Properties();

        // Load existing first to preserve other attributes
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException ignored) {
            }
        }

        if (cgpa != null) {
            props.setProperty("cgpa", String.format(java.util.Locale.US, "%.2f", cgpa));
        }
        if (phone != null) {
            props.setProperty("phone", phone.trim());
        }
        if (collegeName != null) {
            props.setProperty("collegeName", collegeName.trim());
        }
        if (bio != null) {
            props.setProperty("bio", bio.trim());
        }
        if (linkedIn != null) {
            props.setProperty("linkedIn", linkedIn.trim());
        }
        if (gitHub != null) {
            props.setProperty("gitHub", gitHub.trim());
        }

        props.setProperty("updatedAt", java.time.Instant.now().toString());

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            props.store(writer, "Student Placement Planner - Extended Profile (Student #" + studentId + ")");
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save extended profile for student " + studentId + ": " + e.getMessage(), e);
            return false;
        }
    }
}
