package com.ieps.service;

import com.ieps.common.Const;
import com.ieps.pojo.FileHub;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * COS 对象键（存储路径）生成服务
 *
 * <p><b>异步下载链路中的角色：</b></p>
 * <ul>
 *   <li>{@link #buildArchiveObjectKey(String, String, String)} — 生成 ZIP 归档包的 COS 路径，
 *       格式：{@code archives/{userNum}/{yyyyMM}/{taskId}-{zipFileName}}</li>
 *   <li>{@link #buildMigratedObjectKey(FileHub)} — 生成迁移文件的 COS 路径</li>
 *   <li>{@link #sanitizeFileName(String)} — 清理文件名中的非法字符，
 *       在 ZIP 打包 {@link DownloadTaskService#resolveUniqueEntryName} 中调用</li>
 * </ul>
 *
 * <p>文件路径规则：</p>
 * <ul>
 *   <li>头像：{@code avatars/{userNum}/{yyyyMM}/{uuid}-{fileName}}</li>
 *   <li>附件：{@code files/{fileKind}/{typeNum}/{userNum}/{yyyyMM}/{uuid}-{fileName}}</li>
 *   <li>归档：{@code archives/{userNum}/{yyyyMM}/{taskId}-{zipFileName}}</li>
 *   <li>公开文件：{@code files/public/{yyyyMM}/{uuid}-{fileName}}</li>
 * </ul>
 */
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

    /**
     * 生成异步下载任务的 ZIP 归档包 COS 对象键
     *
     * <p><b>异步下载场景：</b>{@link DownloadTaskService#processDownloadTaskInternal(String)}
     * 在将多文件打包为 ZIP 后，调用此方法生成归档路径，
     * 然后将本地 ZIP 上传到该路径下。</p>
     *
     * <p>路径格式：{@code archives/{userNum}/{yyyyMM}/{taskId}-{zipFileName}}</p>
     *
     * @param userNum     发起下载的用户编号
     * @param taskId      下载任务 ID
     * @param zipFileName ZIP 包文件名
     * @return COS 对象键
     */
    public String buildArchiveObjectKey(String userNum, String taskId, String zipFileName) {
        return "archives/" + safeSegment(userNum) + "/" + currentMonth()
                + "/" + safeSegment(taskId) + "-" + sanitizeFileName(zipFileName);
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
