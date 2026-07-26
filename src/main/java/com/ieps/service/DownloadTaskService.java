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

/**
 * 异步下载任务核心业务服务
 *
 * <p><b>异步下载完整流程（由该类编排）：</b></p>
 * <pre>
 * ┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
 * │ 创建任务      │     │ 异步打包 ZIP      │     │ 查询状态 → 下载  │
 * │ (同步)       │ ──→ │ (异步线程池)      │ ──→ │ (同步轮询)       │
 * └──────────────┘     └──────────────────┘     └─────────────────┘
 * Controller 调用       WorkerService 调用       前端每 3-5 秒轮询
 * </pre>
 *
 * <p><b>状态流转：</b>{@code PENDING → RUNNING → SUCCESS / FAILED → (超时) → EXPIRED}</p>
 *
 * <p><b>关键设计点：</b></p>
 * <ul>
 *   <li><b>异步切换：</b>通过 {@link DownloadTaskWorkerService#processDownloadTask(String)}
 *       触发 {@code @Async}，避免 ZIP 打包阻塞 HTTP 响应</li>
 *   <li><b>ZIP 打包：</b>在本地临时文件完成压缩，然后上传到 COS，避免将大文件保留在内存中</li>
 *   <li><b>过期清理：</b>通过 {@code @Scheduled} 定时任务自动清理过期 ZIP 包和历史记录</li>
 *   <li><b>权限校验：</b>创建、查询、下载三个阶段均校验用户所有权</li>
 *   <li><b>文件名去重：</b>打包时自动处理同名文件，添加 (1)、(2) 等后缀</li>
 * </ul>
 */
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

    /**
     * 【步骤1】创建异步下载任务（同步方法，快速返回）
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>校验用户登录状态</li>
     *   <li>校验文件列表非空且全部可访问</li>
     *   <li>在数据库插入 PENDING 状态的任务记录</li>
     *   <li>通过 {@link DownloadTaskWorkerService} 触发异步 ZIP 打包</li>
     *   <li>立即返回任务 DTO（含 taskId），前端据此轮询</li>
     * </ol>
     *
     * <p><b>注意：</b>此处不等待 ZIP 打包完成，HTTP 请求会快速返回（通常在 10ms 内），
     * 真正的耗时工作在 {@code downloadTaskExecutor} 线程池中异步执行。</p>
     *
     * @param currentUser 当前登录用户
     * @param fileNames   待打包的文件名数组
     * @return 包含任务 ID 和初始状态的任务 DTO
     */
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
        // 触发异步打包：此调用立即返回，ZIP 打包在独立线程池中执行
        downloadTaskWorkerService.processDownloadTask(taskId);
        return ServerResponse.createBySuccess("批量下载任务已创建，请稍候！", toDto(downloadTaskMapper.selectByTaskId(taskId)));
    }

    /**
     * 【步骤2】查询下载任务状态（供前端轮询）
     *
     * <p>前端每隔数秒调用此接口查询打包进度。查询时自动检测是否过期：</p>
     * <ul>
     *   <li>若状态为 SUCCESS 但已超过 expireTime，则先执行过期（清理 COS 的 ZIP 包），再返回 EXPIRED 状态</li>
     *   <li>若状态为 SUCCESS 且未过期，返回 SUCCESS 并在 DTO 中附带下载地址</li>
     *   <li>其他状态原样返回</li>
     * </ul>
     *
     * @param currentUser 当前登录用户
     * @param taskId      任务 ID
     * @return 当前任务状态及可下载信息
     */
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

    /**
     * 【步骤3】解析下载 URL（302 重定向到 COS 预签名地址）
     *
     * <p>校验用户权限、任务状态和过期情况后，调用 {@link CosStorageService#generateDownloadUrl}
     * 生成有时效（默认 300 秒）的 COS 预签名下载链接。</p>
     *
     * @param currentUser 当前登录用户
     * @param taskId      任务 ID
     * @return COS 预签名下载 URL
     */
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

    /**
     * 异步下载任务的实际执行体（在 {@code downloadTaskExecutor} 线程池中运行）
     *
     * <p>完整流程：</p>
     * <ol>
     *   <li>将任务状态从 PENDING 更新为 RUNNING</li>
     *   <li>解析源文件列表，从 {@link FileHub} 表获取文件元信息</li>
     *   <li>遍历每个文件，通过 {@link CosStorageService#writeObjectTo} 从 COS 读取内容，
     *       写入本地临时 ZIP 文件（处理同名文件去重）</li>
     *   <li>将生成的 ZIP 文件通过 {@link CosStorageService#uploadLocalFile} 上传到 COS</li>
     *   <li>更新任务状态为 SUCCESS，记录 ZIP 的 COS 路径和过期时间</li>
     *   <li>任意步骤失败则更新为 FAILED 并记录错误信息</li>
     *   <li>finally 中清理本地临时 ZIP 文件</li>
     * </ol>
     *
     * @param taskId 下载任务 ID
     */
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

    /**
     * 定时清理过期下载任务（默认每 1 小时执行一次）
     *
     * <p>清理策略：</p>
     * <ul>
     *   <li><b>过期处理：</b>查询所有已超时的 SUCCESS 任务，删除 COS 上的 ZIP 包并将状态改为 EXPIRED</li>
     *   <li><b>历史清理：</b>删除超过历史保留期（默认 7 天）的所有记录（含 FAILED、EXPIRED 等）</li>
     * </ul>
     *
     * <p>通过 {@code ieps.storage.cos.download-task-cleanup-interval-ms} 配置执行间隔，默认 3600000ms。</p>
     */
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

    /**
     * 过期单个下载任务：删除 COS 上的 ZIP 包，数据库状态改为 EXPIRED
     */
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

    /**
     * 将多个文件打包为 ZIP
     *
     * <p>遍历源文件列表，通过 {@link CosStorageService#writeObjectTo} 从 COS 读取每个文件内容，
     * 写入本地临时 ZIP 文件。同名文件自动添加 (1)、(2) 后缀以避免 ZIP 条目覆盖。</p>
     *
     * @param tempZipFile  本地临时 ZIP 文件
     * @param sourceFiles  源文件名列表
     * @param fileHubMap   文件名到 FileHub 实体的映射
     * @param requestUser  请求用户编号（仅日志用）
     * @param taskId       任务 ID（仅日志用）
     */
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

    /**
     * 生成唯一的 ZIP 条目名
     *
     * <p>如果文件名已被之前添加的文件占用，则在文件名后添加 (1)、(2) 等数字后缀。
     * 例如：report.docx 与另一个 report.docx 共存时变为 report.docx 和 report(1).docx。</p>
     *
     * @param fileName       原始文件名
     * @param usedEntryNames 已使用的 ZIP 条目名集合
     * @return 唯一的 ZIP 条目名
     */
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
