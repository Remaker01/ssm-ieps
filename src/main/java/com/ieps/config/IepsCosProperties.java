package com.ieps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯云 COS 配置属性
 *
 * <p><b>异步下载相关配置项说明：</b></p>
 * <ul>
 *   <li>{@code download-url-ttl-seconds} — COS 预签名下载链接有效期（默认 300 秒 / 5 分钟）</li>
 *   <li>{@code download-task-executor-pool-size} — 异步打包线程池大小（默认 2）</li>
 *   <li>{@code download-task-expire-seconds} — 成功打包后的 ZIP 保留时长（默认 86400 秒 / 24 小时）</li>
 *   <li>{@code download-task-history-retention-seconds} — 任务历史记录保留时长（默认 604800 秒 / 7 天）</li>
 *   <li>{@code multipart-upload-threshold} — 分片上传阈值（默认 16MB），超过此大小使用分片上传</li>
 * </ul>
 *
 * <p>所有配置项以 {@code ieps.storage.cos.*} 为前缀，在 application.yml 中配置。</p>
 */
@Component
@ConfigurationProperties(prefix = "ieps.storage.cos")
public class IepsCosProperties {

    private static final Pattern BUCKET_APP_ID_PATTERN = Pattern.compile("-(\\d{6,})$");

    /** COS 密钥 ID */
    private String secretId;
    /** COS 密钥 Key */
    private String secretKey;
    /** COS Bucket 名称（不含 appId 后缀，系统自动拼接） */
    private String bucket;
    /** COS 地域（如 ap-guangzhou） */
    private String region;
    /** COS APPID（自动从 bucket 名提取或手动指定） */
    private String appId;
    /** STS 临时密钥有效期（秒），默认 600 秒 */
    private long stsDurationSeconds = 600L;
    /** 【异步下载】预签名下载 URL 有效期（秒），默认 300 秒（5 分钟） */
    private long downloadUrlTtlSeconds = 300L;
    /** 启动时是否迁移本地文件到 COS，默认 false */
    private boolean migrationEnabled = false;
    /** 分片上传阈值，超过此大小使用分片上传，默认 16MB */
    private DataSize multipartUploadThreshold = DataSize.ofMegabytes(16);
    /** 分片上传的最小分片大小，默认 8MB */
    private DataSize minimumUploadPartSize = DataSize.ofMegabytes(8);
    /** COS TransferManager 分片上传线程池大小，默认 4 */
    private int transferThreadPoolSize = 4;
    /** 【异步下载】下载任务线程池大小，默认 2 */
    private int downloadTaskExecutorPoolSize = 2;
    /** 【异步下载】ZIP 包保留时长（秒），默认 86400 秒（24 小时） */
    private long downloadTaskExpireSeconds = 86400L;
    /** 【异步下载】任务历史记录保留时长（秒），默认 604800 秒（7 天） */
    private long downloadTaskHistoryRetentionSeconds = 604800L;

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public long getStsDurationSeconds() {
        return stsDurationSeconds;
    }

    public void setStsDurationSeconds(long stsDurationSeconds) {
        this.stsDurationSeconds = stsDurationSeconds;
    }

    public long getDownloadUrlTtlSeconds() {
        return downloadUrlTtlSeconds;
    }

    public void setDownloadUrlTtlSeconds(long downloadUrlTtlSeconds) {
        this.downloadUrlTtlSeconds = downloadUrlTtlSeconds;
    }

    public boolean isMigrationEnabled() {
        return migrationEnabled;
    }

    public void setMigrationEnabled(boolean migrationEnabled) {
        this.migrationEnabled = migrationEnabled;
    }

    public DataSize getMultipartUploadThreshold() {
        return multipartUploadThreshold;
    }

    public void setMultipartUploadThreshold(DataSize multipartUploadThreshold) {
        this.multipartUploadThreshold = multipartUploadThreshold;
    }

    public DataSize getMinimumUploadPartSize() {
        return minimumUploadPartSize;
    }

    public void setMinimumUploadPartSize(DataSize minimumUploadPartSize) {
        this.minimumUploadPartSize = minimumUploadPartSize;
    }

    public int getTransferThreadPoolSize() {
        return transferThreadPoolSize;
    }

    public void setTransferThreadPoolSize(int transferThreadPoolSize) {
        this.transferThreadPoolSize = transferThreadPoolSize;
    }

    public long getMultipartUploadThresholdBytes() {
        return multipartUploadThreshold == null ? DataSize.ofMegabytes(16).toBytes() : multipartUploadThreshold.toBytes();
    }

    public long getMinimumUploadPartSizeBytes() {
        return minimumUploadPartSize == null ? DataSize.ofMegabytes(8).toBytes() : minimumUploadPartSize.toBytes();
    }

    public int getDownloadTaskExecutorPoolSize() {
        return downloadTaskExecutorPoolSize;
    }

    public void setDownloadTaskExecutorPoolSize(int downloadTaskExecutorPoolSize) {
        this.downloadTaskExecutorPoolSize = downloadTaskExecutorPoolSize;
    }

    public long getDownloadTaskExpireSeconds() {
        return downloadTaskExpireSeconds;
    }

    public void setDownloadTaskExpireSeconds(long downloadTaskExpireSeconds) {
        this.downloadTaskExpireSeconds = downloadTaskExpireSeconds;
    }

    public long getDownloadTaskHistoryRetentionSeconds() {
        return downloadTaskHistoryRetentionSeconds;
    }

    public void setDownloadTaskHistoryRetentionSeconds(long downloadTaskHistoryRetentionSeconds) {
        this.downloadTaskHistoryRetentionSeconds = downloadTaskHistoryRetentionSeconds;
    }

    public String getResolvedBucket() {
        String normalizedBucket = normalize(bucket);
        if (normalizedBucket == null) {
            return null;
        }

        String bucketAppId = extractAppIdFromBucket(normalizedBucket);
        if (bucketAppId != null) {
            return normalizedBucket;
        }

        String resolvedAppId = getResolvedAppId();
        if (resolvedAppId == null) {
            return normalizedBucket;
        }
        return normalizedBucket + "-" + resolvedAppId;
    }

    public String getResolvedAppId() {
        String normalizedBucket = normalize(bucket);
        if (normalizedBucket != null) {
            String bucketAppId = extractAppIdFromBucket(normalizedBucket);
            if (bucketAppId != null) {
                return bucketAppId;
            }
        }
        return normalize(appId);
    }

    public boolean hasBucketAppIdMismatch() {
        String normalizedBucket = normalize(bucket);
        String normalizedAppId = normalize(appId);
        if (normalizedBucket == null || normalizedAppId == null) {
            return false;
        }

        String bucketAppId = extractAppIdFromBucket(normalizedBucket);
        return bucketAppId != null && !bucketAppId.equals(normalizedAppId);
    }

    public boolean isConfigured() {
        return isNotBlank(secretId) && isNotBlank(secretKey) && isNotBlank(bucket)
                && isNotBlank(region) && isNotBlank(getResolvedAppId());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractAppIdFromBucket(String bucketName) {
        Matcher matcher = BUCKET_APP_ID_PATTERN.matcher(bucketName);
        return matcher.find() ? matcher.group(1) : null;
    }
}
