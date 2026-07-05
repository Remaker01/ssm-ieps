package com.ieps.service;

import com.ieps.common.Const;
import com.ieps.config.IepsCosProperties;
import com.ieps.mapper.FileHubMapper;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.mapper.UserItemMapper;
import com.ieps.pojo.FileHub;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private IepsCosProperties iepsCosProperties;

    @Autowired
    private StoragePathService storagePathService;

    @Autowired
    private CosStorageService cosStorageService;

    @Autowired
    private FileHubMapper fileHubMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserItemMapper userItemMapper;

    public String resolveUserImageUrl(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return "/static/images/default.jpg";
        }
        if (storedValue.startsWith("avatars/")) {
            return cosStorageService.generateDownloadUrl(storedValue, iepsCosProperties.getDownloadUrlTtlSeconds()).toString();
        }
        if (storedValue.startsWith("http://") || storedValue.startsWith("https://") || storedValue.startsWith("/")) {
            return storedValue;
        }
        return "/" + storedValue;
    }

    public boolean canAccessFile(User currentUser, FileHub fileHub) {
        if (fileHub == null) {
            return false;
        }
        if (isPublicFile(fileHub)) {
            return true;
        }
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRoleId() == Const.ROLEID_COLLEGE
                || currentUser.getRoleId() == Const.ROLEID_ACADEMY
                || currentUser.getRoleId() == Const.ROLEID_COLLEGE_EXPERT
                || currentUser.getRoleId() == Const.ROLEID_ACADEMY_EXPERT
                || currentUser.getRoleId() == Const.ROLEID_TUTOR) {
            return true;
        }
        if (currentUser.getUserNum() != null && currentUser.getUserNum().equals(fileHub.getUserNum())) {
            return true;
        }
        return StringUtils.hasText(fileHub.getTypeNum())
                && userItemMapper.countByItemNumAndUserNum(fileHub.getTypeNum(), currentUser.getUserNum()) > 0;
    }

    public boolean isPublicFile(FileHub fileHub) {
        return fileHub.getFileKind() != null && fileHub.getFileKind() <= Const.FIRST_FILE_KIND;
    }

    public void migrateLocalFilesToCos() {
        logger.info("Starting local file migration to COS...");

        List<FileHub> fileHubs = fileHubMapper.selectAllStorageFiles();
        int migratedFileCount = 0;
        for (FileHub fileHub : fileHubs) {
            if (StringUtils.hasText(fileHub.getObjectKey())) {
                continue;
            }
            Path localFile = resolveLegacyFilePath(fileHub.getFileName());
            if (localFile == null) {
                logger.warn("Skip migrating file {}, local source not found", fileHub.getFileName());
                continue;
            }
            String objectKey = storagePathService.buildMigratedObjectKey(fileHub);
            try (InputStream inputStream = Files.newInputStream(localFile)) {
                cosStorageService.putObject(objectKey, inputStream, Files.size(localFile), Files.probeContentType(localFile));
                FileHub updateRecord = new FileHub();
                updateRecord.setId(fileHub.getId());
                updateRecord.setObjectKey(objectKey);
                updateRecord.setStorageProvider("cos");
                updateRecord.setContentType(Files.probeContentType(localFile));
                fileHubMapper.updateByPrimaryKeySelective(updateRecord);
                migratedFileCount++;
            } catch (Exception e) {
                logger.error("Failed to migrate file {}", fileHub.getFileName(), e);
            }
        }

        List<UserInfo> userInfos = userInfoMapper.selectAllUserInfoForMigration();
        int migratedAvatarCount = 0;
        for (UserInfo userInfo : userInfos) {
            String userImg = userInfo.getUserImg();
            if (!StringUtils.hasText(userImg) || userImg.startsWith("avatars/")
                    || "/static/images/default.jpg".equals(userImg)) {
                continue;
            }

            Path localAvatar = resolveLegacyAvatarPath(userImg);
            if (localAvatar == null) {
                continue;
            }
            String objectKey = storagePathService.buildAvatarObjectKey(userInfo.getUserNum(), localAvatar.getFileName().toString());
            try (InputStream inputStream = Files.newInputStream(localAvatar)) {
                cosStorageService.putObject(objectKey, inputStream, Files.size(localAvatar), Files.probeContentType(localAvatar));
                UserInfo updateUser = new UserInfo();
                updateUser.setUserNum(userInfo.getUserNum());
                updateUser.setUserImg(objectKey);
                userInfoMapper.updateByUserNumSelective(updateUser);
                migratedAvatarCount++;
            } catch (Exception e) {
                logger.error("Failed to migrate avatar for user {}", userInfo.getUserNum(), e);
            }
        }

        logger.info("Local file migration completed. migratedFiles={}, migratedAvatars={}",
                migratedFileCount, migratedAvatarCount);
    }

    private Path resolveLegacyFilePath(String fileName) {
        List<Path> candidates = Arrays.asList(
                Paths.get("upload", fileName),
                Paths.get("upload", "hub", fileName),
                Paths.get("src", "main", "resources", "static", "hub", fileName)
        );
        return findFirstExisting(candidates);
    }

    private Path resolveLegacyAvatarPath(String userImg) {
        String normalized = userImg.replace("\\", "/");
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get("upload", "hub", "images", fileName));
        candidates.add(Paths.get("src", "main", "resources", "static", "hub", "images", fileName));
        candidates.add(Paths.get("src", "main", "resources", "static", "hub", fileName));
        return findFirstExisting(candidates);
    }

    private Path findFirstExisting(List<Path> candidates) {
        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(absolute)) {
                return absolute;
            }
        }
        return null;
    }
}
