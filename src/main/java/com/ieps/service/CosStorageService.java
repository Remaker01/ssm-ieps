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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CosStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CosStorageService.class);

    @Autowired
    private IepsCosProperties iepsCosProperties;

    private volatile COSClient cosClient;
    private volatile TransferManager transferManager;
    private volatile ExecutorService transferExecutor;

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
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return;
        }
        try {
            getCosClient().deleteObject(iepsCosProperties.getResolvedBucket(), objectKey);
        } catch (Exception ignore) {
            // 历史脏数据或已删除对象不阻断主流程
        }
    }

    public URL generateDownloadUrl(String objectKey, long ttlSeconds) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                iepsCosProperties.getResolvedBucket(), objectKey);
        request.setExpiration(new Date(System.currentTimeMillis() + ttlSeconds * 1000L));
        return getCosClient().generatePresignedUrl(request);
    }

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
        if (contentType != null && !contentType.trim().isEmpty()) {
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
            ExecutorService initializedTransferExecutor = Executors.newFixedThreadPool(
                    Math.max(2, iepsCosProperties.getTransferThreadPoolSize()),
                    new CosTransferThreadFactory());
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
