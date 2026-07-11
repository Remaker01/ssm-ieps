package com.ieps.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.mapper.FileHubMapper;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.pojo.FileHub;
import com.ieps.pojo.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by ljw
 */
@Service
public class FileAdminService {

    private static final Logger logger = LoggerFactory.getLogger(FileAdminService.class);

    @Autowired
    private FileHubMapper fileHubMapper;
    
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private StoragePathService storagePathService;

    @Autowired
    private CosStorageService cosStorageService;
    
    public ServerResponse<PageInfo<FileHub>> getFileListByUserNum(int pageNum, int pageSize, String userNumAdmin, int roleId, FileHub fileHub) {
        PageHelper.startPage(pageNum, pageSize);
        List<FileHub> fileHubList = new ArrayList<>();
        if (Const.ROLEID_COLLEGE == roleId) {
            fileHubList = fileHubMapper.selectAllFileHub(fileHub);
        } else if (Const.ROLEID_ACADEMY == roleId) {
            fileHubList = fileHubMapper.selectAllFileByUserNum(Const.USERNUM_COLLEGE, userNumAdmin, fileHub);
        }
        if (fileHubList.size() == 0) {
            return ServerResponse.createByErrorMessage("对不起，可能暂无数据，请稍候再试！");
        }
        PageInfo<FileHub> pageInfo = new PageInfo<>(fileHubList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    private ServerResponse<String> deleteServerFile(String[] fileNames) {
        for (String fileName : fileNames) {
            FileHub fileHub = fileHubMapper.selectByFileName(fileName);
            if (fileHub != null) {
                logger.info("Deleting COS object for fileName={}, objectKey={}", fileName, fileHub.getObjectKey());
                cosStorageService.deleteObjectQuietly(fileHub.getObjectKey());
            } else {
                logger.warn("Skip deleting COS object because file record was not found, fileName={}", fileName);
            }
        }
        return ServerResponse.createBySuccess("删除文件成功！");
    }
    
    public ServerResponse<String> removeFileById(String filePath, String fileName, String userNum, Integer id, int roleId) {
        String[] fileNames = {fileName};
        if (Const.ROLEID_COLLEGE == roleId) {
            if (fileHubMapper.deleteByPrimaryKey(id) > 0) {
                return deleteServerFile(fileNames);
            }
        } else if (Const.ROLEID_ACADEMY == roleId) {
            if (!Const.USERNUM_COLLEGE.equals(userNum)) {
                if (fileHubMapper.deleteByPrimaryKey(id) > 0) {
                    return deleteServerFile(fileNames);
                }
            } else {
                return ServerResponse.createByErrorMessage("对不起，目前你没有权限删除该文件记录！");
            }
        }
        return ServerResponse.createByErrorMessage("删除该文件失败，请重试！");
    }
    
    public ServerResponse<String> batchRemoveFile(String filePath, String[] fileNames, String[] userNums, Integer[] ids, int roleId) {
        if (Const.ROLEID_COLLEGE == roleId) {
            if (fileHubMapper.batchDeleteFileByIds(ids) > 0) {
                return deleteServerFile(fileNames);
            }
        } else if (Const.ROLEID_ACADEMY == roleId) {
            for (int i = 0; i < ids.length; i++) {
                if (!Const.USERNUM_COLLEGE.equals(userNums[i])) {
                    if (fileHubMapper.deleteByPrimaryKey(ids[i]) > 0) {
                        return deleteServerFile(fileNames);
                    }
                }
            }
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限删除该文件记录！");
        }
        return ServerResponse.createByErrorMessage("删除该文件失败，请重试！");
    }
    
    public ServerResponse<String> batchUploadFile(MultipartFile[] files, String userNum, String filePath, int fileKind, String typeNum) {
        String fileName = "";
        int fileCount = files == null ? 0 : files.length;
        logger.info("Start backend relay upload. userNum={}, fileKind={}, typeNum={}, fileCount={}",
                userNum, fileKind, typeNum, fileCount);
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file.isEmpty()) {
                    logger.warn("Skip empty multipart file during upload. userNum={}, fileKind={}, typeNum={}, index={}",
                            userNum, fileKind, typeNum, i);
                    continue;
                }
                String normalizedTypeNum = typeNum == null || typeNum.trim().isEmpty() ? "-1" : typeNum.trim();
                String storedFileName = storagePathService.safeStoredFileName(file.getOriginalFilename());
                String objectKey = storagePathService.buildAttachmentObjectKey(fileKind, normalizedTypeNum, userNum, storedFileName);
                try {
                    cosStorageService.uploadMultipartFile(file, objectKey);
                } catch (Exception e) {
                    logger.error("Failed backend relay upload. userNum={}, fileKind={}, typeNum={}, originalFileName={}, objectKey={}",
                            userNum, fileKind, normalizedTypeNum, file.getOriginalFilename(), objectKey, e);
                    return ServerResponse.createByErrorMessage("上传文件失败：" + e.getMessage());
                }

                FileHub fileHub = new FileHub();
                fileHub.setFileKind(fileKind);
                fileHub.setTypeNum(normalizedTypeNum);
                fileHub.setFileName(storedFileName);
                fileHub.setObjectKey(objectKey);
                fileHub.setStorageProvider("cos");
                fileHub.setContentType(file.getContentType());
                fileHub.setFileSize((int) file.getSize());
                fileHub.setUserNum(userNum);
                UserInfo userInfo = userInfoMapper.selectByUserNum(userNum);
                fileHub.setAcademy(userInfo == null ? null : userInfo.getAcademy());
                fileName = fileHub.getFileName();
                fileHubMapper.insertSelective(fileHub);
                logger.info("Backend relay upload succeeded. userNum={}, fileKind={}, typeNum={}, originalFileName={}, storedFileName={}, objectKey={}, size={}",
                        userNum, fileKind, normalizedTypeNum, file.getOriginalFilename(), storedFileName, objectKey, file.getSize());
            }
            logger.info("Backend relay upload finished. userNum={}, fileKind={}, typeNum={}, lastStoredFileName={}",
                    userNum, fileKind, typeNum, fileName);
            return ServerResponse.createBySuccess(fileName);
        }
        logger.warn("Backend relay upload failed because no files were provided. userNum={}, fileKind={}, typeNum={}",
                userNum, fileKind, typeNum);
        return ServerResponse.createByErrorMessage("上传文件失败");
    }
    
    public ServerResponse<?> modifyFileKindWithUserNum(int roleId, FileHub fileHub) {
        if (Const.ROLEID_COLLEGE == roleId) {
            if (fileHubMapper.updateByPrimaryKeySelective(fileHub) > 0) {
                return ServerResponse.createBySuccess();
            }
        } else if (Const.ROLEID_ACADEMY == roleId) {
            if (!Const.USERNUM_COLLEGE.equals(fileHub.getUserNum())) {
                if (fileHubMapper.updateByPrimaryKeySelective(fileHub) > 0) {
                    return ServerResponse.createBySuccess();
                }
            } else {
                return ServerResponse.createByErrorMessage("对不起，目前你没有权限删除该文件记录！");
            }
        }
        return ServerResponse.createByErrorMessage("修改该文件的类型属性失败，请重试！");
    }
    
    public ServerResponse<PageInfo<FileHub>> getFileListByAdmin(int pageNum, int pageSize, String fileName, String updateTime) {
        PageHelper.startPage(pageNum, pageSize);
        List<FileHub> fileHubList = fileHubMapper.selectAllFileByAdminWithKind(Const.FIRST_FILE_KIND, fileName, updateTime);
        if (fileHubList.size() == 0) {
            return ServerResponse.createByErrorMessage("没有文件，数据为空！");
        }
        PageInfo<FileHub> pageInfo = new PageInfo<>(fileHubList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse<FileHub> getFileWithItemNum(String itemNum) {
        FileHub fileHub = fileHubMapper.selectFileWithItemNum(itemNum);
        if (fileHub == null) {
            return ServerResponse.createByErrorMessage("对不起，你还没有上传项目附件呢");
        }
        return ServerResponse.createBySuccess(fileHub);
    }

    public FileHub getFileByFileName(String fileName) {
        return fileHubMapper.selectByFileName(fileName);
    }
    
}
