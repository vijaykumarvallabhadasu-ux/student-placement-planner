package com.placementplanner.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing student resume metadata and automated verification analysis.
 */
public class ResumeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        NOT_UPLOADED("Not Uploaded"),
        PASSED("Resume Checks Passed"),
        NEEDS_IMPROVEMENT("Needs Improvement"),
        UNABLE_TO_VERIFY("Unable to Verify File");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private long studentId;
    private String storedFileName;
    private String originalFileName;
    private long fileSize;
    private String contentType;
    private String uploadTime;
    private Status status = Status.NOT_UPLOADED;
    private int score;
    private int wordCount;
    private List<String> foundSections = new ArrayList<>();
    private List<String> missingSections = new ArrayList<>();
    private List<String> feedback = new ArrayList<>();
    private List<String> extractedSkills = new ArrayList<>();
    private List<String> extractedProjects = new ArrayList<>();
    private List<String> extractedCertifications = new ArrayList<>();
    private String extractedEducation = "";

    public ResumeInfo() {
    }

    public ResumeInfo(long studentId) {
        this.studentId = studentId;
        this.status = Status.NOT_UPLOADED;
    }

    public boolean isUploaded() {
        return storedFileName != null && !storedFileName.isBlank() && status != Status.NOT_UPLOADED;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFormattedFileSize() {
        if (fileSize <= 0) return "0 KB";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public List<String> getFoundSections() {
        return foundSections;
    }

    public void setFoundSections(List<String> foundSections) {
        this.foundSections = foundSections;
    }

    public List<String> getMissingSections() {
        return missingSections;
    }

    public void setMissingSections(List<String> missingSections) {
        this.missingSections = missingSections;
    }

    public List<String> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<String> feedback) {
        this.feedback = feedback;
    }

    public List<String> getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(List<String> extractedSkills) {
        this.extractedSkills = extractedSkills != null ? extractedSkills : new ArrayList<>();
    }

    public List<String> getExtractedProjects() {
        return extractedProjects;
    }

    public void setExtractedProjects(List<String> extractedProjects) {
        this.extractedProjects = extractedProjects != null ? extractedProjects : new ArrayList<>();
    }

    public List<String> getExtractedCertifications() {
        return extractedCertifications;
    }

    public void setExtractedCertifications(List<String> extractedCertifications) {
        this.extractedCertifications = extractedCertifications != null ? extractedCertifications : new ArrayList<>();
    }

    public String getExtractedEducation() {
        return extractedEducation;
    }

    public void setExtractedEducation(String extractedEducation) {
        this.extractedEducation = extractedEducation != null ? extractedEducation : "";
    }
}
