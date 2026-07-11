package com.ieps.controller;

import com.github.pagehelper.PageInfo;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.config.IepsCosProperties;
import com.ieps.dto.CkeditorUploadFileDto;
import com.ieps.dto.DownloadTaskDto;
import com.ieps.pojo.FileHub;
import com.ieps.pojo.User;
import com.ieps.service.CosStorageService;
import com.ieps.service.DownloadTaskService;
import com.ieps.service.FileAdminService;
import com.ieps.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by ljw
 */
@Controller
public class FileAdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileAdminController.class);
    
    @Autowired
    private FileAdminService fileAdminService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private CosStorageService cosStorageService;

    @Autowired
    private IepsCosProperties iepsCosProperties;

    @Autowired
    private DownloadTaskService downloadTaskService;

    private String resolveCurrentUserNum(HttpServletRequest request, String fallbackUserNum) {
        User user = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        if (user != null && user.getUserNum() != null && !user.getUserNum().trim().isEmpty()) {
            return user.getUserNum();
        }
        return fallbackUserNum;
    }
    
    /**
     * 通过userNum获取全部的文件列表信息
     * @param page
     * @param fileHub
     * @param limit
     * @param session
     * @param userNumAdmin
     * @param roleId
     * @return
     */
    @RequestMapping({"/getFileListByUserNum", "/getFileListByUserNum.do"})
    @ResponseBody
    public ServerResponse<PageInfo<FileHub>> getFileListByUserNum(@RequestParam(value = "page", defaultValue = "1") int page, FileHub fileHub,
                                               @RequestParam(value = "limit", defaultValue = "5") int limit,
                                               @RequestParam("userNumAdmin") String userNumAdmin, @RequestParam("roleId") Integer roleId) {
        
        return fileAdminService.getFileListByUserNum(page, limit, userNumAdmin, roleId, fileHub);
    }
    
    /**
     * 根据文件id删除文件
     * @param userNum
     * @param session
     * @param request
     * @param id
     * @param userNumAdmin
     * @param roleId
     * @param fileName
     * @return
     */
    @RequestMapping({"/removeFileById", "/removeFileById.do"})
    @ResponseBody
    public ServerResponse<String> removeFileById(@RequestParam("userNum") String userNum, HttpServletRequest request,
                                         @RequestParam("id") Integer id, @RequestParam("userNumAdmin") String userNumAdmin,
                                         @RequestParam("roleId") Integer roleId, @RequestParam("fileName") String fileName) {
        return fileAdminService.removeFileById("", fileName, userNum, id, roleId);
    }
    
    /**
     * 批量删除文件
     * @param userNumAdmin
     * @param session
     * @param request
     * @param userNums
     * @param ids
     * @param roleId
     * @param fileNames
     * @return
     */
    @RequestMapping({"/batchRemoveFile", "/batchRemoveFile.do"})
    @ResponseBody
    public ServerResponse<String> batchRemoveFile(@RequestParam("userNum") String userNumAdmin, HttpServletRequest request,
                                          @RequestParam("userNums") String[] userNums, @RequestParam("ids") Integer[] ids,
                                          @RequestParam("roleId") Integer roleId, @RequestParam("fileNames") String[] fileNames) {
        return fileAdminService.batchRemoveFile("", fileNames, userNums, ids, roleId);
    }
    
    
    /**
     * 批量删除文件
     * @param files
     * @param userNum
     * @param itemNum
     * @param session
     * @param request
     * @param fileKind
     * @return
     */
    @RequestMapping(value = {"/batchUploadFile", "/batchUploadFile.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> batchUploadFile(@RequestParam("files") MultipartFile[] files, String userNum, String itemNum,
                                          HttpServletRequest request, int fileKind) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNum);
        if (effectiveUserNum == null || effectiveUserNum.trim().isEmpty()) {
            logger.warn("Reject upload request because current user could not be resolved. itemNum={}, fileKind={}", itemNum, fileKind);
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }
        int fileCount = files == null ? 0 : files.length;
        logger.info("Receive upload request. userNum={}, itemNum={}, fileKind={}, fileCount={}",
                effectiveUserNum, itemNum, fileKind, fileCount);
        return fileAdminService.batchUploadFile(files, effectiveUserNum, "", fileKind, itemNum);
    }
    
    /**
     * CKeditor批量上传文件
     * @param files
     * @param userNum
     * @param itemNum
     * @param session
     * @param request
     * @param fileKind
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/batchCkeditorUploadFile", "/batchCkeditorUploadFile.do"}, method = RequestMethod.POST)
    @ResponseBody
    public CkeditorUploadFileDto batchCkeditorUploadFile(@RequestParam("upload") MultipartFile[] files, String userNum, String itemNum,
                                          HttpServletRequest request, Integer fileKind) throws Exception  {
        CkeditorUploadFileDto ckeditorUploadFileDto = new CkeditorUploadFileDto();
        String effectiveUserNum = resolveCurrentUserNum(request, userNum);
        int fileCount = files == null ? 0 : files.length;
        if (effectiveUserNum == null || effectiveUserNum.trim().isEmpty()) {
            logger.warn("Reject CKEditor upload request because current user could not be resolved. itemNum={}, fileKind={}", itemNum, fileKind);
            return ckeditorUploadFileDto;
        }
        logger.info("Receive CKEditor upload request. userNum={}, itemNum={}, fileKind={}, fileCount={}",
                effectiveUserNum, itemNum, fileKind, fileCount);

        ServerResponse<?> response = fileAdminService.batchUploadFile(files, effectiveUserNum, "", fileKind, itemNum);

        if(response.getStatus() == 0) {
            String fileName = String.valueOf(response.getData());
            ckeditorUploadFileDto.setUploaded(1)
                    .setFileName(files[0].getOriginalFilename())
                    .setUrl("/downloadFile?fileName=" + URLEncoder.encode(fileName, "UTF-8"));
            logger.info("CKEditor upload succeeded. userNum={}, itemNum={}, fileKind={}, storedFileName={}",
                    effectiveUserNum, itemNum, fileKind, fileName);
        } else {
            logger.warn("CKEditor upload failed. userNum={}, itemNum={}, fileKind={}, message={}",
                    effectiveUserNum, itemNum, fileKind, response.getMsg());
        }

        return ckeditorUploadFileDto;
    }
    
    /**
     * 根据文件名下载文件
     * @param fileName
     * @param request
     * @param response
     */
    @RequestMapping(value = {"/downloadFile", "/downloadFile.do"}, method = RequestMethod.GET)
    public void downloadFile(String fileName, HttpServletRequest request, HttpServletResponse response) {
        FileHub fileHub = fileAdminService.getFileByFileName(fileName);
        if (fileHub == null) {
            logger.warn("Download rejected because file record was not found. fileName={}", fileName);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        if (!storageService.canAccessFile(currentUser, fileHub)) {
            logger.warn("Download rejected because access was denied. fileName={}, objectKey={}, requestUser={}",
                    fileName, fileHub.getObjectKey(), currentUser == null ? "anonymous" : currentUser.getUserNum());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        URL downloadUrl = cosStorageService.generateDownloadUrl(fileHub.getObjectKey(), iepsCosProperties.getDownloadUrlTtlSeconds());
        try {
            logger.info("Redirecting file download. fileName={}, objectKey={}, requestUser={}",
                    fileName, fileHub.getObjectKey(), currentUser == null ? "anonymous" : currentUser.getUserNum());
            response.sendRedirect(downloadUrl.toString());
        } catch (IOException e) {
            logger.error("Failed to redirect download. fileName={}, objectKey={}, requestUser={}",
                    fileName, fileHub.getObjectKey(), currentUser == null ? "anonymous" : currentUser.getUserNum(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 预览功能已下线，统一提示用户直接下载。
     * @param fileName
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = {"/previewFile", "/previewFile.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<?> previewFile(String fileName, HttpServletRequest request, HttpServletResponse response) {
        return ServerResponse.createByErrorMessage("预览功能已下线，请直接下载文件查看！");
    }
    
    
    // // 不能以ajax下载
    // @RequestMapping(value = "/downloadFile.do", method = RequestMethod.GET)
    // @ResponseBody
    // public ResponseEntity<byte[]> downloadFile(String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException {
    //     HttpHeaders headers = new HttpHeaders();
    //
    //     // getRealPath(“/”) 获取实际路径,“/”指代项目根目录,所以代码返回的是项目在容器中的实际发布运行的根路径。
    //     String filePath = request.getServletContext().getRealPath("/hub/") + fileName;
    //     File file = new File(filePath);
    //
    //     headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    //     headers.setContentDispositionFormData("attachment", fileName);
    //
    //     return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file),
    //             headers, HttpStatus.CREATED);
    // }
    
    /**
     * 根据userNum修改文件类型
     * @param fileHub
     * @param userNumAdmin
     * @param session
     * @param roleId
     * @return
     */
    @RequestMapping(value = {"/modifyFileKindWithUserNum", "/modifyFileKindWithUserNum.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<?> modifyFileKindWithUserNum(FileHub fileHub, @RequestParam("userNumAdmin") String userNumAdmin,
                                                    @RequestParam("roleId") int roleId) {
        
        return fileAdminService.modifyFileKindWithUserNum(roleId, fileHub);
    }
    
    /**
     * 管理员根据fileKind获取文件列表
     * @param pageNum
     * @param pageSize
     * @param session
     * @param fileName
     * @param updateTime
     * @return
     */
    @RequestMapping(value = {"/getFileListByAdminWithKind", "/getFileListByAdminWithKind.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo<FileHub>> getFileListByAdminWithKind(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                                     @RequestParam(value = "pageSize",defaultValue = "8") int pageSize,
                                                     String fileName, String updateTime) {
        return fileAdminService.getFileListByAdmin(pageNum, pageSize, fileName, updateTime);
    }
    
    /**
     * 根据项目编号itemNum下载文件
     * @param itemNum
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = {"/downloadFileWithItemNum", "/downloadFileWithItemNum.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<FileHub> downloadFileWithItemNum(@RequestParam("itemNum") String itemNum, HttpServletRequest request,
                                                  HttpServletResponse response) {
        ServerResponse<FileHub> fileResponse = fileAdminService.getFileWithItemNum(itemNum);
        if (fileResponse.getStatus() != 0) {
            logger.warn("Item attachment download rejected because file was not found. itemNum={}", itemNum);
            return ServerResponse.createByErrorMessage("对不起，你还没有上传项目附件呢!请关闭窗口再重试！");
        }

        FileHub fileHub = (FileHub) fileResponse.getData();
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        if (!storageService.canAccessFile(currentUser, fileHub)) {
            logger.warn("Item attachment download rejected because access was denied. itemNum={}, fileName={}, objectKey={}, requestUser={}",
                    itemNum, fileHub.getFileName(), fileHub.getObjectKey(), currentUser == null ? "anonymous" : currentUser.getUserNum());
            return ServerResponse.createByErrorCodeMessage(HttpServletResponse.SC_UNAUTHORIZED, "你没有权限下载该文件！");
        }

        try {
            logger.info("Redirecting item attachment download. itemNum={}, fileName={}, objectKey={}, requestUser={}",
                    itemNum, fileHub.getFileName(), fileHub.getObjectKey(), currentUser == null ? "anonymous" : currentUser.getUserNum());
            response.sendRedirect(cosStorageService.generateDownloadUrl(fileHub.getObjectKey(), iepsCosProperties.getDownloadUrlTtlSeconds()).toString());
        } catch (IOException e) {
            logger.error("Failed to redirect item attachment download. itemNum={}, fileName={}, objectKey={}, requestUser={}",
                    itemNum, fileHub.getFileName(), fileHub.getObjectKey(), currentUser == null ? "anonymous" : currentUser.getUserNum(), e);
            return ServerResponse.createByErrorMessage("文件下载失败：" + e.getMessage());
        }
        return null;
    }
    
    /**
     * 一键下载文件
     * @param userNum
     * @param roleId
     * @param request
     * @param fileNames
     * @param response
     * @return
     */
    @RequestMapping(value = {"/onekeyDownloadFileCompat"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<DownloadTaskDto> onekeyDownloadFile(@RequestParam("userNum") String userNum, @RequestParam("roleId") int roleId,
                                   HttpServletRequest request, String[] fileNames, HttpServletResponse response) throws IOException {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        return downloadTaskService.createDownloadTask(currentUser, fileNames);
    }
}
