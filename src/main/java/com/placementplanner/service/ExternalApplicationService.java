package com.placementplanner.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to record and manage external job applications submitted directly on official employer websites.
 * Operates in protected application file storage without altering the Oracle Database schema.
 */
public final class ExternalApplicationService {

    private static final Logger LOGGER = Logger.getLogger(ExternalApplicationService.class.getName());
    private static File storageDir;

    static {
        initStorageDir();
    }

    private static synchronized void initStorageDir() {
        if (storageDir != null && storageDir.exists()) {
            return;
        }

        String userHome = System.getProperty("user.home", ".");
        storageDir = new File(userHome + File.separator + ".placementplanner" + File.separator + "applications");

        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (created) {
                LOGGER.info("Initialized external applications storage: " + storageDir.getAbsolutePath());
            }
        }
    }

    public static File getStorageDirectory() {
        if (storageDir == null || !storageDir.exists()) {
            initStorageDir();
        }
        return storageDir;
    }

    public static synchronized boolean recordExternalApplication(
            long studentId,
            long jobId,
            String jobTitle,
            String company,
            String officialUrl,
            String status
    ) {
        if (studentId <= 0 || jobId <= 0) {
            return false;
        }

        File file = new File(getStorageDirectory(), "external_" + studentId + ".properties");
        Properties props = new Properties();

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException ignored) {
            }
        }

        String prefix = "job_" + jobId + ".";
        props.setProperty(prefix + "jobId", String.valueOf(jobId));
        props.setProperty(prefix + "title", jobTitle != null ? jobTitle : "Software Opportunity");
        props.setProperty(prefix + "company", company != null ? company : "Employer Portal");
        props.setProperty(prefix + "officialUrl", officialUrl != null ? officialUrl : "");
        props.setProperty(prefix + "status", status != null ? status : "Applied on Employer Website");

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        if (!props.containsKey(prefix + "appliedAt")) {
            props.setProperty(prefix + "appliedAt", time);
        }
        props.setProperty(prefix + "lastUpdated", time);

        // Keep index of jobIds
        String allIds = props.getProperty("allJobIds", "");
        if (!allIds.contains(String.valueOf(jobId))) {
            allIds = allIds.isEmpty() ? String.valueOf(jobId) : allIds + "," + jobId;
            props.setProperty("allJobIds", allIds);
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            props.store(writer, "Student Placement Planner - External Applications for Student #" + studentId);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed saving external application: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean hasTracked(long studentId, long jobId) {
        if (studentId <= 0 || jobId <= 0) return false;
        File file = new File(getStorageDirectory(), "external_" + studentId + ".properties");
        if (!file.exists()) return false;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            props.load(reader);
            return props.containsKey("job_" + jobId + ".jobId");
        } catch (IOException e) {
            return false;
        }
    }

    public static List<Map<String, Object>> getTrackedApplications(long studentId) {
        if (studentId <= 0) return Collections.emptyList();

        File file = new File(getStorageDirectory(), "external_" + studentId + ".properties");
        if (!file.exists()) return Collections.emptyList();

        List<Map<String, Object>> list = new ArrayList<>();
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            props.load(reader);

            String allIds = props.getProperty("allJobIds", "");
            if (allIds.isBlank()) return list;

            String[] idArr = allIds.split(",");
            for (int i = idArr.length - 1; i >= 0; i--) { // Reverse order (recent first)
                String idStr = idArr[i].trim();
                if (idStr.isEmpty()) continue;

                String prefix = "job_" + idStr + ".";
                if (props.containsKey(prefix + "jobId")) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("jobId", Long.parseLong(idStr));
                    map.put("title", props.getProperty(prefix + "title", "Software Role"));
                    map.put("company", props.getProperty(prefix + "company", "Employer Portal"));
                    map.put("officialUrl", props.getProperty(prefix + "officialUrl", ""));
                    map.put("status", props.getProperty(prefix + "status", "Applied on Employer Website"));
                    map.put("appliedAt", props.getProperty(prefix + "appliedAt", "Recent"));
                    map.put("isExternal", true);
                    list.add(map);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read external applications: " + e.getMessage());
        }

        return list;
    }
}
