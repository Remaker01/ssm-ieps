package com.ieps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConfigurationProperties(prefix = "ieps.storage.cos")
public class IepsCosProperties {

    private static final Pattern BUCKET_APP_ID_PATTERN = Pattern.compile("-(\\d{6,})$");

    private String secretId;
    private String secretKey;
    private String bucket;
    private String region;
    private String appId;
    private long stsDurationSeconds = 600L;
    private long downloadUrlTtlSeconds = 300L;
    private boolean migrationEnabled = false;

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
