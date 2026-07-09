package com.ieps.service;

import com.ieps.common.ServerResponse;
import com.ieps.config.IepsCosProperties;
import com.ieps.dto.DownloadTaskDto;
import com.ieps.enums.DownloadTaskStatus;
import com.ieps.mapper.DownloadTaskMapper;
import com.ieps.mapper.FileHubMapper;
import com.ieps.pojo.DownloadTask;
import com.ieps.pojo.FileHub;
import com.ieps.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadTaskService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadTaskService.class);

    @Autowired
    private DownloadTaskMapper downloadTaskMapper;

    @Autowired
    private FileHubMapper fileHubMapper;

    @Autowired
    private StorageService storageService;

    @Autowired
    private StoragePathService storagePathService;

    @Autowired
    private CosStorageService cosStorageService;

    @Autowired
    private IepsCosProperties iepsCosProperties;

    @Autowired
    private DownloadTaskWorkerService downloadTaskWorkerService;

    public ServerResponse<DownloadTaskDto> createDownloadTask(User currentUser, String[] fileNames) {
        if (currentUser == null || !StringUtils.hasText(currentUser.getUserNum())) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }

        List<String> normalizedFileNames = normalizeFileNames(fileNames);
        if (normalizedFileNames.isEmpty()) {
            return ServerResponse.createByErrorMessage("请先选择需要下载的文件！");
        }

        Map<String, FileHub> accessibleFiles = resolveAccessibleFiles(currentUser, normalizedFileNames);
        if (accessibleFiles == null) {
            return ServerResponse.createByErrorMessage("存在不可下载或不存在的文件，请刷新页面后重试！");
        }

        String taskId = generateTaskId();
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.setTaskId(taskId);
        downloadTask.setUserNum(currentUser.getUserNum());
        downloadTask.setStatus(DownloadTaskStatus.PENDING.getValue());
        downloadTask.setFileCount(normalizedFileNames.size());
        downloadTask.setSourceFiles(String.join("\n", normalizedFileNames));
        downloadTask.setCreatedTime(new Date());
        downloadTask.setUpdateTime(new Date());
        downloadTaskMapper.insertSelective(downloadTask);

        logger.info("Created async download task. taskId={}, requestUser={}, fileCount={}",
                taskId, currentUser.getUserNum(), normalizedFileNames.size());
        downloadTaskWorkerService.processDownloadTask(taskId);
        return ServerResponse.createBySuccess("批量下载任务已创建，请稍候！", toDto(downloadTaskMapper.selectByTaskId(taskId)));
    }

    public ServerResponse<DownloadTaskDto> getDownloadTask(User currentUser, String taskId) {
        DownloadTask downloadTask = downloadTaskMapper.selectByTaskId(taskId);
        if (downloadTask == null) {
            return ServerResponse.createByErrorMessage("下载任务不存在或已被清理！");
        }
        if (!canAccessTask(currentUser, downloadTask)) {
            return ServerResponse.createByErrorMessage("你没有权限查看该下载任务！");
        }

        if (shouldExpire(downloadTask)) {
            expireTask(downloadTask);
            downloadTask = downloadTaskMapper.selectByTaskId(taskId);
        }

        return ServerResponse.createBySuccess(toDto(downloadTask));
    }

    public ServerResponse<String> resolveDownloadUrl(User currentUser, String taskId) {
        DownloadTask downloadTask = downloadTaskMapper.selectByTaskId(taskId);
        if (downloadTask == null) {
            return ServerResponse.createByErrorMessage("下载任务不存在或已被清理！");
        }
        if (!canAccessTask(currentUser, downloadTask)) {
            return ServerResponse.createByErrorMessage("你没有权限下载该压缩文件！");
        }
        if (shouldExpire(downloadTask)) {
            expireTask(downloadTask);
            return ServerResponse.createByErrorMessage("下载任务已过期，请重新创建！");
        }
        if (!DownloadTaskStatus.SUCCESS.getValue().equals(downloadTask.getStatus())) {
            return ServerResponse.createByErrorMessage("下载任务尚未完成，请稍后重试！");
        }
        return ServerResponse.createBySuccess(
                cosStorageService.generateDownloadUrl(downloadTask.getZipObjectKey(),
                        iepsCosProperties.getDownloadUrlTtlSeconds()).toString());
    }

    public void processDownloadTaskInternal(String taskId) {
        DownloadTask currentTask = downloadTaskMapper.selectByTaskId(taskId);
        if (currentTask == null) {
            return;
        }

        Date startedAt = new Date();
        DownloadTask runningUpdate = new DownloadTask();
        runningUpdate.setTaskId(taskId);
        runningUpdate.setStatus(DownloadTaskStatus.RUNNING.getValue());
        runningUpdate.setStartedTime(startedAt);
        runningUpdate.setFinishedTime(null);
        runningUpdate.setErrorMessage(null);
        runningUpdate.setUpdateTime(startedAt);
        downloadTaskMapper.updateByTaskIdSelective(runningUpdate);

        File tempZipFile = null;
        try {
            List<String> sourceFiles = splitSourceFiles(currentTask.getSourceFiles());
            Map<String, FileHub> fileHubMap = resolveFilesByNames(sourceFiles);
            if (fileHubMap == null || fileHubMap.isEmpty()) {
                throw new IOException("任务文件已失效，无法继续打包！");
            }

            String zipFileName = "download-task-" + taskId + ".zip";
            String objectKey = storagePathService.buildArchiveObjectKey(currentTask.getUserNum(), taskId, zipFileName);
            tempZipFile = Files.createTempFile("ieps-download-task-", ".zip").toFile();
            writeZipArchive(tempZipFile, sourceFiles, fileHubMap, currentTask.getUserNum(), taskId);
            cosStorageService.uploadLocalFile(tempZipFile, objectKey, "application/zip");

            Date finishedAt = new Date();
            DownloadTask successUpdate = new DownloadTask();
            successUpdate.setTaskId(taskId);
            successUpdate.setStatus(DownloadTaskStatus.SUCCESS.getValue());
            successUpdate.setZipFileName(zipFileName);
            successUpdate.setZipObjectKey(objectKey);
            successUpdate.setFinishedTime(finishedAt);
            successUpdate.setExpireTime(new Date(finishedAt.getTime() + iepsCosProperties.getDownloadTaskExpireSeconds() * 1000L));
            successUpdate.setErrorMessage(null);
            successUpdate.setUpdateTime(finishedAt);
            downloadTaskMapper.updateByTaskIdSelective(successUpdate);
            logger.info("Async download task succeeded. taskId={}, requestUser={}, zipObjectKey={}, fileCount={}",
                    taskId, currentTask.getUserNum(), objectKey, sourceFiles.size());
        } catch (Exception e) {
            Date finishedAt = new Date();
            DownloadTask failedUpdate = new DownloadTask();
            failedUpdate.setTaskId(taskId);
            failedUpdate.setStatus(DownloadTaskStatus.FAILED.getValue());
            failedUpdate.setFinishedTime(finishedAt);
            failedUpdate.setErrorMessage(buildErrorMessage(e));
            failedUpdate.setUpdateTime(finishedAt);
            downloadTaskMapper.updateByTaskIdSelective(failedUpdate);
            logger.error("Async download task failed. taskId={}, requestUser={}", taskId, currentTask.getUserNum(), e);
        } finally {
            if (tempZipFile != null && tempZipFile.exists() && !tempZipFile.delete()) {
                logger.warn("Failed to delete temp zip file. taskId={}, tempFile={}", taskId, tempZipFile.getAbsolutePath());
            }
        }
    }

    @Scheduled(fixedDelayString = "${ieps.storage.cos.download-task-cleanup-interval-ms:3600000}")
    public void cleanupExpiredTasks() {
        Date now = new Date();
        List<DownloadTask> expiredSuccessTasks = downloadTaskMapper.selectSuccessTasksExpiredBefore(now);
        for (DownloadTask downloadTask : expiredSuccessTasks) {
            expireTask(downloadTask);
        }

        Date historyCutoff = new Date(now.getTime() - iepsCosProperties.getDownloadTaskHistoryRetentionSeconds() * 1000L);
        List<DownloadTask> historyTasks = downloadTaskMapper.selectHistoryTasksBefore(historyCutoff);
        if (!historyTasks.isEmpty()) {
            String[] taskIds = historyTasks.stream().map(DownloadTask::getTaskId).toArray(String[]::new);
            downloadTaskMapper.batchDeleteByTaskIds(taskIds);
            logger.info("Cleaned historical download tasks. taskCount={}", taskIds.length);
        }
    }

    private void expireTask(DownloadTask downloadTask) {
        if (downloadTask == null || !DownloadTaskStatus.SUCCESS.getValue().equals(downloadTask.getStatus())) {
            return;
        }
        if (StringUtils.hasText(downloadTask.getZipObjectKey())) {
            cosStorageService.deleteObjectQuietly(downloadTask.getZipObjectKey());
        }
        DownloadTask expiredUpdate = new DownloadTask();
        expiredUpdate.setTaskId(downloadTask.getTaskId());
        expiredUpdate.setStatus(DownloadTaskStatus.EXPIRED.getValue());
        expiredUpdate.setUpdateTime(new Date());
        downloadTaskMapper.updateByTaskIdSelective(expiredUpdate);
        logger.info("Expired async download task. taskId={}, requestUser={}",
                downloadTask.getTaskId(), downloadTask.getUserNum());
    }

    private void writeZipArchive(File tempZipFile, List<String> sourceFiles, Map<String, FileHub> fileHubMap,
                                 String requestUser, String taskId) throws IOException {
        Set<String> usedEntryNames = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipFile), StandardCharsets.UTF_8)) {
            for (String fileName : sourceFiles) {
                FileHub fileHub = fileHubMap.get(fileName);
                if (fileHub == null) {
                    throw new IOException("打包文件不存在：" + fileName);
                }
                String entryName = resolveUniqueEntryName(fileHub.getFileName(), usedEntryNames);
                logger.info("Adding file to async zip. taskId={}, requestUser={}, fileName={}, objectKey={}",
                        taskId, requestUser, fileHub.getFileName(), fileHub.getObjectKey());
                zos.putNextEntry(new ZipEntry(entryName));
                cosStorageService.writeObjectTo(fileHub.getObjectKey(), zos);
                zos.closeEntry();
            }
            zos.finish();
        }
    }

    private String resolveUniqueEntryName(String fileName, Set<String> usedEntryNames) {
        String safeFileName = storagePathService.sanitizeFileName(fileName);
        if (usedEntryNames.add(safeFileName)) {
            return safeFileName;
        }

        String baseName = safeFileName;
        String extension = "";
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = safeFileName.substring(0, dotIndex);
            extension = safeFileName.substring(dotIndex);
        }

        int suffix = 1;
        while (true) {
            String candidate = baseName + "(" + suffix + ")" + extension;
            if (usedEntryNames.add(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    private List<String> normalizeFileNames(String[] fileNames) {
        if (fileNames == null) {
            return List.of();
        }
        return Arrays.stream(fileNames)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableList());
    }

    private Map<String, FileHub> resolveAccessibleFiles(User currentUser, List<String> fileNames) {
        List<FileHub> fileHubList = fileHubMapper.selectByFileNames(fileNames.toArray(new String[0]));
        Map<String, FileHub> fileHubMap = new HashMap<>();
        for (FileHub fileHub : fileHubList) {
            fileHubMap.put(fileHub.getFileName(), fileHub);
        }

        for (String fileName : fileNames) {
            FileHub fileHub = fileHubMap.get(fileName);
            if (fileHub == null) {
                logger.warn("Reject download task because file record was not found. requestUser={}, fileName={}",
                        currentUser == null ? "anonymous" : currentUser.getUserNum(), fileName);
                return null;
            }
            if (!storageService.canAccessFile(currentUser, fileHub)) {
                logger.warn("Reject download task because file access was denied. requestUser={}, fileName={}, objectKey={}",
                        currentUser == null ? "anonymous" : currentUser.getUserNum(), fileName, fileHub.getObjectKey());
                return null;
            }
        }
        return fileHubMap;
    }

    private List<String> splitSourceFiles(String sourceFiles) {
        if (!StringUtils.hasText(sourceFiles)) {
            return List.of();
        }
        return normalizeFileNames(sourceFiles.split("\\n"));
    }

    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean canAccessTask(User currentUser, DownloadTask downloadTask) {
        return currentUser != null
                && StringUtils.hasText(currentUser.getUserNum())
                && currentUser.getUserNum().equals(downloadTask.getUserNum());
    }

    private boolean shouldExpire(DownloadTask downloadTask) {
        return DownloadTaskStatus.SUCCESS.getValue().equals(downloadTask.getStatus())
                && downloadTask.getExpireTime() != null
                && downloadTask.getExpireTime().getTime() <= System.currentTimeMillis();
    }

    private String buildErrorMessage(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return "打包失败，请稍后重试！";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private Map<String, FileHub> resolveFilesByNames(List<String> fileNames) {
        List<FileHub> fileHubList = fileHubMapper.selectByFileNames(fileNames.toArray(new String[0]));
        Map<String, FileHub> fileHubMap = new HashMap<>();
        for (FileHub fileHub : fileHubList) {
            fileHubMap.put(fileHub.getFileName(), fileHub);
        }

        for (String fileName : fileNames) {
            if (!fileHubMap.containsKey(fileName)) {
                logger.warn("Download task source file missing when processing. fileName={}", fileName);
                return null;
            }
        }
        return fileHubMap;
    }

    private DownloadTaskDto toDto(DownloadTask downloadTask) {
        if (downloadTask == null) {
            return null;
        }
        DownloadTaskDto dto = new DownloadTaskDto()
                .setTaskId(downloadTask.getTaskId())
                .setStatus(downloadTask.getStatus())
                .setFileCount(downloadTask.getFileCount())
                .setZipObjectKey(downloadTask.getZipObjectKey())
                .setZipFileName(downloadTask.getZipFileName())
                .setErrorMessage(downloadTask.getErrorMessage())
                .setCreatedTime(downloadTask.getCreatedTime())
                .setStartedTime(downloadTask.getStartedTime())
                .setFinishedTime(downloadTask.getFinishedTime())
                .setExpireTime(downloadTask.getExpireTime())
                .setDownloadPath("/downloadTasks/" + downloadTask.getTaskId() + "/download");

        if (DownloadTaskStatus.SUCCESS.getValue().equals(downloadTask.getStatus())
                && StringUtils.hasText(downloadTask.getZipObjectKey())
                && !shouldExpire(downloadTask)) {
            dto.setDownloadUrl(cosStorageService.generateDownloadUrl(
                    downloadTask.getZipObjectKey(), iepsCosProperties.getDownloadUrlTtlSeconds()).toString());
        }
        return dto;
    }
}
