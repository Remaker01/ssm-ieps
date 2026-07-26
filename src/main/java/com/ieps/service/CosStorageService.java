package com.ieps.service;

import com.ieps.config.IepsCosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import com.qcloud.cos.transfer.Upload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 腾讯云 COS 对象存储服务
 *
 * <p>异步文件下载链路中的存储层核心组件。提供以下能力：</p>
 * <ul>
 *   <li>文件上传：支持普通流式上传和大文件分片上传两种模式</li>
 *   <li>文件下载：将 COS 中的文件内容写入 OutputStream（ZIP 打包时使用）</li>
 *   <li>预签名 URL 生成：为下载任务生成有时效的临时下载链接</li>
 *   <li>对象删除：清理过期任务的 ZIP 包</li>
 * </ul>
 *
 * <p><b>异步下载中的角色：</b></p>
 * <ol>
 *   <li>{@link DownloadTaskWorkerService} 异步执行 ZIP 打包时，
 *       通过 {@link #writeObjectTo(String, java.io.OutputStream)} 读取 COS 中的各文件流到本地 ZIP</li>
 *   <li>ZIP 打包完成后，通过 {@link #uploadLocalFile(File, String, String)} 回传 ZIP 到 COS</li>
 *   <li>前端轮询到 SUCCESS 状态后，通过 {@link #generateDownloadUrl(String, long)}
 *       获得临时可用的下载 URL 直接下载</li>
 * </ol>
 *
 * <p><b>分片上传策略：</b>当文件大小超过 {@code multipart-upload-threshold} 时自动使用分片上传，
 * 分片大小由 {@code minimum-upload-part-size} 控制。</p>
 *
 * <p><b>线程安全：</b>COS 客户端和 TransferManager 采用 DCL 双重检查懒加载，
 * TransferManager 使用有界队列 + CallerRunsPolicy 反压策略，队列满时由调用线程执行上传，
 * 防止内存溢出。</p>
 */
@Service
public class CosStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CosStorageService.class);

    @Autowired
    private IepsCosProperties iepsCosProperties;

    private volatile COSClient cosClient;
    private volatile TransferManager transferManager;
    private volatile ExecutorService transferExecutor;

    /**
     * 上传 Multipart 文件到 COS
     *
     * <p>根据文件大小自动选择上传方式：</p>
     * <ul>
     *   <li>小文件（< threshold）：直接流式 putObject 上传</li>
     *   <li>大文件（>= threshold）：暂存本地临时文件后使用 TransferManager 分片上传</li>
     * </ul>
     *
     * @param file      上传的文件
     * @param objectKey COS 中的对象键（路径）
     */
    public void uploadMultipartFile(MultipartFile file, String objectKey) throws IOException {
        long contentLength = file.getSize();
        if (shouldUseMultipartUpload(contentLength)) {
            logger.info("Use COS multipart upload. objectKey={}, size={}, threshold={}, partSize={}",
                    objectKey, contentLength, iepsCosProperties.getMultipartUploadThresholdBytes(),
                    iepsCosProperties.getMinimumUploadPartSizeBytes());
            uploadLargeMultipartFile(file, objectKey, contentLength, file.getContentType());
            return;
        }

        logger.info("Use streaming putObject upload. objectKey={}, size={}", objectKey, contentLength);
        try (InputStream inputStream = file.getInputStream()) {
            putObject(objectKey, inputStream, contentLength, file.getContentType());
        }
    }

    public void putObject(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        ObjectMetadata objectMetadata = buildObjectMetadata(contentLength, contentType);
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                iepsCosProperties.getResolvedBucket(), objectKey, inputStream, objectMetadata);
        getCosClient().putObject(putObjectRequest);
    }

    /**
     * 上传本地文件到 COS
     *
     * <p><b>异步下载场景：</b>{@link DownloadTaskService#processDownloadTaskInternal(String)}
     * 在将多文件打包为本地 ZIP 后，调用此方法将 ZIP 文件上传到 COS 存储，
     * 之后前端才能通过预签名 URL 下载。</p>
     *
     * @param file        本地临时 ZIP 文件
     * @param objectKey   COS 中的对象键
     * @param contentType 文件 MIME 类型
     */
    public void uploadLocalFile(File file, String objectKey, String contentType) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("待上传的本地文件不存在！");
        }
        long contentLength = file.length();
        if (shouldUseMultipartUpload(contentLength)) {
            logger.info("Use COS multipart upload for local file. objectKey={}, size={}, threshold={}, partSize={}",
                    objectKey, contentLength, iepsCosProperties.getMultipartUploadThresholdBytes(),
                    iepsCosProperties.getMinimumUploadPartSizeBytes());
            uploadTempFileWithTransferManager(file, objectKey, contentLength, contentType);
            return;
        }

        logger.info("Use local file putObject upload. objectKey={}, size={}", objectKey, contentLength);
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                iepsCosProperties.getResolvedBucket(), objectKey, file).withMetadata(buildObjectMetadata(contentLength, contentType));
        getCosClient().putObject(putObjectRequest);
    }

    public ObjectMetadata getObjectMetadata(String objectKey) {
        return getCosClient().getObjectMetadata(iepsCosProperties.getResolvedBucket(), objectKey);
    }

    public boolean doesObjectExist(String objectKey) {
        try {
            return getObjectMetadata(objectKey) != null;
        } catch (CosServiceException ex) {
            if (ex.getStatusCode() == 404) {
                return false;
            }
            throw ex;
        }
    }

    public void deleteObjectQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            getCosClient().deleteObject(iepsCosProperties.getResolvedBucket(), objectKey);
        } catch (Exception ignore) {
            // 历史脏数据或已删除对象不阻断主流程
        }
    }

    /**
     * 生成预签名下载 URL
     *
     * <p><b>异步下载场景：</b>ZIP 包上传到 COS 后，后端通过此方法生成有时效的临时下载链接返回给前端。
     * 前端无需直接访问 COS 密钥即可下载文件。</p>
     *
     * @param objectKey  COS 对象键
     * @param ttlSeconds URL 有效时长（秒），默认 300 秒（5 分钟）
     * @return 预签名下载 URL
     */
    public URL generateDownloadUrl(String objectKey, long ttlSeconds) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                iepsCosProperties.getResolvedBucket(), objectKey);
        request.setExpiration(new Date(System.currentTimeMillis() + ttlSeconds * 1000L));
        return getCosClient().generatePresignedUrl(request);
    }

    /**
     * 将 COS 对象的内容写入指定的输出流
     *
     * <p><b>异步下载场景：</b>{@link DownloadTaskService#processDownloadTaskInternal(String)}
     * 在异步 ZIP 打包时，对每个文件调用此方法，从 COS 读取文件内容并写入 ZIP 输出流中。</p>
     *
     * <p>读取完成后自动关闭 COS 对象内容流。</p>
     *
     * @param objectKey    COS 对象键
     * @param outputStream 目标输出流（ZIP 输出流的 ZipEntry）
     */
    public void writeObjectTo(String objectKey, OutputStream outputStream) throws IOException {
        COSObject cosObject = getCosClient().getObject(new GetObjectRequest(iepsCosProperties.getResolvedBucket(), objectKey));
        try (InputStream inputStream = cosObject.getObjectContent()) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    /**
     * 优雅关闭 COS 客户端相关资源
     *
     * <p>应用关闭时按顺序释放：TransferManager → 传输线程池 → COSClient。
     * 先关闭传输管理器确保正在进行的传输被终止，再关闭底层客户端连接。</p>
     */
    @PreDestroy
    public void shutdown() {
        TransferManager localTransferManager = this.transferManager;
        if (localTransferManager != null) {
            localTransferManager.shutdownNow(false);
        }

        ExecutorService localTransferExecutor = this.transferExecutor;
        if (localTransferExecutor != null) {
            localTransferExecutor.shutdown();
        }

        COSClient localCosClient = this.cosClient;
        if (localCosClient != null) {
            localCosClient.shutdown();
        }
    }

    private boolean shouldUseMultipartUpload(long contentLength) {
        return contentLength >= iepsCosProperties.getMultipartUploadThresholdBytes();
    }

    private void uploadLargeMultipartFile(MultipartFile file, String objectKey, long contentLength, String contentType) throws IOException {
        Path tempFile = Files.createTempFile("ieps-cos-upload-", buildTempFileSuffix(file.getOriginalFilename()));
        try {
            file.transferTo(tempFile);
            uploadTempFileWithTransferManager(tempFile.toFile(), objectKey, contentLength, contentType);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void uploadTempFileWithTransferManager(File file, String objectKey, long contentLength, String contentType) throws IOException {
        PutObjectRequest putObjectRequest = new PutObjectRequest(iepsCosProperties.getResolvedBucket(), objectKey, file)
                .withMetadata(buildObjectMetadata(contentLength, contentType));
        try {
            Upload upload = getTransferManager().upload(putObjectRequest);
            upload.waitForUploadResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("分片上传被中断", e);
        }
    }

    private ObjectMetadata buildObjectMetadata(long contentLength, String contentType) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);
        if (contentType != null && !contentType.isBlank()) {
            objectMetadata.setContentType(contentType);
        }
        return objectMetadata;
    }

    private String buildTempFileSuffix(String originalFileName) {
        if (originalFileName == null) {
            return ".tmp";
        }
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFileName.length() - 1) {
            return ".tmp";
        }
        String extension = originalFileName.substring(lastDot);
        return extension.length() > 20 ? ".tmp" : extension;
    }

    private COSClient getCosClient() {
        ensureClientInitialized();
        return cosClient;
    }

    private TransferManager getTransferManager() {
        ensureClientInitialized();
        return transferManager;
    }

    private void ensureClientInitialized() {
        if (cosClient != null && transferManager != null) {
            return;
        }

        synchronized (this) {
            if (cosClient != null && transferManager != null) {
                return;
            }
            COSClient initializedCosClient = buildClient();
            int threadCount = Math.max(2, iepsCosProperties.getTransferThreadPoolSize());
            // 有界队列 + CallerRunsPolicy：队列满时由调用线程执行，天然反压，避免 OOM
            ExecutorService initializedTransferExecutor = new ThreadPoolExecutor(
                    threadCount, threadCount,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(32),
                    new CosTransferThreadFactory(),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            TransferManager initializedTransferManager = new TransferManager(initializedCosClient, initializedTransferExecutor);
            TransferManagerConfiguration configuration = new TransferManagerConfiguration();
            configuration.setMultipartUploadThreshold(iepsCosProperties.getMultipartUploadThresholdBytes());
            configuration.setMinimumUploadPartSize(iepsCosProperties.getMinimumUploadPartSizeBytes());
            initializedTransferManager.setConfiguration(configuration);

            this.cosClient = initializedCosClient;
            this.transferExecutor = initializedTransferExecutor;
            this.transferManager = initializedTransferManager;
        }
    }

    private COSClient buildClient() {
        COSCredentials credentials = new BasicCOSCredentials(
                iepsCosProperties.getSecretId(), iepsCosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(iepsCosProperties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
    }

    private static class CosTransferThreadFactory implements ThreadFactory {

        private final AtomicInteger threadCounter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ieps-cos-transfer-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
