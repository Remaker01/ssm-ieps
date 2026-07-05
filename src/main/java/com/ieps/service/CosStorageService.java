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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

@Service
public class CosStorageService {

    @Autowired
    private IepsCosProperties iepsCosProperties;

    public void uploadMultipartFile(MultipartFile file, String objectKey) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            putObject(objectKey, inputStream, file.getSize(), file.getContentType());
        }
    }

    public void putObject(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        COSClient cosClient = buildClient();
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(contentLength);
            if (contentType != null && !contentType.trim().isEmpty()) {
                objectMetadata.setContentType(contentType);
            }

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    iepsCosProperties.getResolvedBucket(), objectKey, inputStream, objectMetadata);
            cosClient.putObject(putObjectRequest);
        } finally {
            cosClient.shutdown();
        }
    }

    public ObjectMetadata getObjectMetadata(String objectKey) {
        COSClient cosClient = buildClient();
        try {
            return cosClient.getObjectMetadata(iepsCosProperties.getResolvedBucket(), objectKey);
        } finally {
            cosClient.shutdown();
        }
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
        COSClient cosClient = buildClient();
        try {
            cosClient.deleteObject(iepsCosProperties.getResolvedBucket(), objectKey);
        } catch (Exception ignore) {
            // 历史脏数据或已删除对象不阻断主流程
        } finally {
            cosClient.shutdown();
        }
    }

    public URL generateDownloadUrl(String objectKey, long ttlSeconds) {
        COSClient cosClient = buildClient();
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    iepsCosProperties.getResolvedBucket(), objectKey);
            request.setExpiration(new Date(System.currentTimeMillis() + ttlSeconds * 1000L));
            return cosClient.generatePresignedUrl(request);
        } finally {
            cosClient.shutdown();
        }
    }

    public void writeObjectTo(String objectKey, OutputStream outputStream) throws IOException {
        COSClient cosClient = buildClient();
        try {
            COSObject cosObject = cosClient.getObject(new GetObjectRequest(iepsCosProperties.getResolvedBucket(), objectKey));
            try (InputStream inputStream = cosObject.getObjectContent()) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            }
        } finally {
            cosClient.shutdown();
        }
    }

    private COSClient buildClient() {
        COSCredentials credentials = new BasicCOSCredentials(
                iepsCosProperties.getSecretId(), iepsCosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(iepsCosProperties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
    }
}
