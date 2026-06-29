package com.ieps.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.mapper.FileHubMapper;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.pojo.FileHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * Created by ljw
 */
@Service
public class FileAdminService {
    
    @Autowired
    private FileHubMapper fileHubMapper;
    
    @Autowired
    private UserInfoMapper userInfoMapper;
    
    public ServerResponse getFileListByUserNum(int pageNum, int pageSize, String userNumAdmin, int roleId, FileHub fileHub) {
        PageHelper.startPage(pageNum, pageSize);
        List<FileHub> fileHubList = Lists.newArrayList();
        if (Const.ROLEID_COLLEGE == roleId) {
            fileHubList = fileHubMapper.selectAllFileHub(fileHub);
        } else if (Const.ROLEID_ACADEMY == roleId) {
            fileHubList = fileHubMapper.selectAllFileByUserNum(Const.USERNUM_COLLEGE, userNumAdmin, fileHub);
        }
        if (fileHubList.size() == 0) {
            return ServerResponse.createByErrorMessage("对不起，可能暂无数据，请稍候再试！");
        }
        PageInfo pageInfo = new PageInfo(fileHubList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    private static ServerResponse deleteServerFile(String filePath, String[] fileNames) {
        File file = null;
        for (int i = 0; i < fileNames.length; i++) {
            file = new File(filePath + fileNames[i]);
            if (!file.exists()) {
                return ServerResponse.createByErrorMessage("文件不存在，请检查数据库！");
            }
            if (!file.isFile()) {
                return ServerResponse.createByErrorMessage("不是文件，未知是文件夹还是目录，请检查数据库！");
            }
            if (!file.delete()) {
                return ServerResponse.createByErrorMessage("删除文件失败！");
            }
        }
        return ServerResponse.createBySuccess("删除文件成功！");
    }
    
    public ServerResponse removeFileById(String filePath, String fileName, String userNum, Integer id, int roleId) {
        String[] fileNames = {fileName};
        if (Const.ROLEID_COLLEGE == roleId) {
            if (fileHubMapper.deleteByPrimaryKey(id) > 0) {
                return deleteServerFile(filePath, fileNames);
            }
        } else if (Const.ROLEID_ACADEMY == roleId) {
            if (!Const.USERNUM_COLLEGE.equals(userNum)) {
                if (fileHubMapper.deleteByPrimaryKey(id) > 0) {
                    return deleteServerFile(filePath, fileNames);
                }
            } else {
                return ServerResponse.createByErrorMessage("对不起，目前你没有权限删除该文件记录！");
            }
        }
        return ServerResponse.createByErrorMessage("删除该文件失败，请重试！");
    }
    
    public ServerResponse batchRemoveFile(String filePath, String[] fileNames, String[] userNums, Integer[] ids, int roleId) {
        if (Const.ROLEID_COLLEGE == roleId) {
            if (fileHubMapper.batchDeleteFileByIds(ids) > 0) {
                return deleteServerFile(filePath, fileNames);
            }
        } else if (Const.ROLEID_ACADEMY == roleId) {
            for (int i = 0; i < ids.length; i++) {
                if (!Const.USERNUM_COLLEGE.equals(userNums[i])) {
                    if (fileHubMapper.deleteByPrimaryKey(ids[i]) > 0) {
                        return deleteServerFile(filePath, fileNames);
                    }
                }
            }
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限删除该文件记录！");
        }
        return ServerResponse.createByErrorMessage("删除该文件失败，请重试！");
    }
    
    private boolean saveFile(MultipartFile file, String path, long currentTime) {
        if (!file.isEmpty()) {
            try {
                File filePath = new File(path);
                if (!filePath.exists())
                    filePath.mkdirs();
                String savePath = path + currentTime + "-" + file.getOriginalFilename();
                file.transferTo(new File(savePath));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public ServerResponse batchUploadFile(MultipartFile[] files, String userNum, String filePath, int fileKind, String typeNum) {
        String fileName = "";
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                long currentTime = System.currentTimeMillis();
                if (saveFile(file, filePath, currentTime)) {
                    FileHub fileHub = new FileHub();
                    if (typeNum == null || typeNum == "") {
                        typeNum = " -1";
                    }
                    fileHub.setFileKind(fileKind);
                    fileHub.setTypeNum(typeNum);
                    fileHub.setFileName(currentTime + "-" + file.getOriginalFilename());
                    fileHub.setFileSize((int) file.getSize());
                    fileHub.setUserNum(userNum);
                    fileHub.setAcademy(userInfoMapper.selectByUserNum(userNum).getAcademy());
                    fileName = fileHub.getFileName();
                    fileHubMapper.insertSelective(fileHub);
                }
            }
            return ServerResponse.createBySuccess(fileName);
        }
        return ServerResponse.createByErrorMessage("上传文件失败");
    }
    
    public ServerResponse modifyFileKindWithUserNum(int roleId, FileHub fileHub) {
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
    
    public ServerResponse getFileListByAdmin(int pageNum, int pageSize, String fileName, String updateTime) {
        PageHelper.startPage(pageNum, pageSize);
        List<FileHub> fileHubList = fileHubMapper.selectAllFileByAdminWithKind(Const.FIRST_FILE_KIND, fileName, updateTime);
        if (fileHubList.size() == 0) {
            return ServerResponse.createByErrorMessage("没有文件，数据为空！");
        }
        PageInfo pageInfo = new PageInfo(fileHubList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse getFileWithItemNum(String itemNum) {
        FileHub fileHub = fileHubMapper.selectFileWithItemNum(itemNum);
        if (fileHub == null) {
            return ServerResponse.createByErrorMessage("对不起，你还没有上传项目附件呢");
        }
        return ServerResponse.createBySuccess(fileHub);
    }
    
}
