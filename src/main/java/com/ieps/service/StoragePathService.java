package com.ieps.service;

import com.ieps.common.Const;
import com.ieps.pojo.FileHub;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class StoragePathService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    public String buildObjectKeyForSts(String uploadType, Integer fileKind, String typeNum, String userNum, String fileName) {
        if ("avatar".equalsIgnoreCase(uploadType)) {
            return buildAvatarObjectKey(userNum, fileName);
        }
        return buildAttachmentObjectKey(fileKind, typeNum, userNum, fileName);
    }

    public String buildAvatarObjectKey(String userNum, String fileName) {
        return "avatars/" + safeSegment(userNum) + "/" + currentMonth() + "/" + uniqueFileName(fileName);
    }

    public String buildAttachmentObjectKey(Integer fileKind, String typeNum, String userNum, String fileName) {
        int actualFileKind = fileKind == null ? Const.ZERO_FILE_KIND : fileKind;
        String safeFileName = uniqueFileName(fileName);
        if (actualFileKind <= Const.FIRST_FILE_KIND) {
            return "files/public/" + currentMonth() + "/" + safeFileName;
        }
        return "files/" + actualFileKind + "/" + safeSegment(typeNum) + "/" + safeSegment(userNum)
                + "/" + currentMonth() + "/" + safeFileName;
    }

    public String buildMigratedObjectKey(FileHub fileHub) {
        return buildAttachmentObjectKey(fileHub.getFileKind(), fileHub.getTypeNum(), fileHub.getUserNum(), fileHub.getFileName());
    }

    public String safeStoredFileName(String fileName) {
        String normalized = sanitizeFileName(fileName);
        return System.currentTimeMillis() + "-" + normalized;
    }

    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "file";
        }
        String normalized = fileName.replace("\\", "/");
        int lastSlashIndex = normalized.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            normalized = normalized.substring(lastSlashIndex + 1);
        }
        normalized = normalized.replaceAll("[\\r\\n\\t]", "_");
        normalized = normalized.replaceAll("[<>:\"|?*]", "_");
        return normalized.trim().isEmpty() ? "file" : normalized.trim();
    }

    private String uniqueFileName(String fileName) {
        return UUID.randomUUID().toString().replace("-", "") + "-" + sanitizeFileName(fileName);
    }

    private String safeSegment(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-1";
        }
        return value.trim().replaceAll("[\\\\/\\s]+", "_");
    }

    private String currentMonth() {
        return LocalDate.now().format(MONTH_FORMATTER);
    }
}
