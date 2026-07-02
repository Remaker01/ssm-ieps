package com.ieps.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieps.common.ServerResponse;
import com.ieps.dto.CkeditorUploadFileDto;
import com.ieps.pojo.FileHub;
import com.ieps.service.FileAdminService;
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
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.ieps.util.PreviewFileUtil.SubmitPost;
import static com.ieps.util.PreviewFileUtil.extractPreviewUrl;

/**
 * Created by ljw
 */
@Controller
public class FileAdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileAdminController.class);
    
    @Autowired
    private FileAdminService fileAdminService;

    @Autowired
    private ObjectMapper objectMapper;
    
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
    public ServerResponse getFileListByUserNum(@RequestParam(value = "page", defaultValue = "1") int page, FileHub fileHub,
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
    public ServerResponse removeFileById(@RequestParam("userNum") String userNum, HttpServletRequest request,
                                         @RequestParam("id") Integer id, @RequestParam("userNumAdmin") String userNumAdmin,
                                         @RequestParam("roleId") Integer roleId, @RequestParam("fileName") String fileName) {
        
        String filePath = request.getServletContext().getRealPath("/hub/");
        
        return fileAdminService.removeFileById(filePath, fileName, userNum, id, roleId);
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
    public ServerResponse batchRemoveFile(@RequestParam("userNum") String userNumAdmin, HttpServletRequest request,
                                          @RequestParam("userNums") String[] userNums, @RequestParam("ids") Integer[] ids,
                                          @RequestParam("roleId") Integer roleId, @RequestParam("fileNames") String[] fileNames) {
        
        String filePath = request.getServletContext().getRealPath("/hub/");
        
        return fileAdminService.batchRemoveFile(filePath, fileNames, userNums, ids, roleId);
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
    public ServerResponse batchUploadFile(@RequestParam("files") MultipartFile[] files, String userNum, String itemNum,
                                          HttpServletRequest request, int fileKind) {
        
        String filePath = request.getServletContext().getRealPath("/hub/");
        
        return fileAdminService.batchUploadFile(files, userNum, filePath, fileKind, itemNum);
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
        
        // http://127.0.0.1:8080/batchCkeditorUploadFile.do
        // String filePath = request.getServletContext().getContextPath();
        // String filePath = "../" + request.getRequestURL();
        
        // 绝对路经
        String filePath = request.getSession().getServletContext().getRealPath("/hub/");
    
        CkeditorUploadFileDto ckeditorUploadFileDto = new CkeditorUploadFileDto();
        
        ServerResponse response = fileAdminService.batchUploadFile(files, userNum, filePath, fileKind, itemNum);

        if(response.getStatus() == 0) {
            filePath = request.getRequestURL().toString();
            filePath = filePath.substring(0, filePath.lastIndexOf("/"));
            
            ckeditorUploadFileDto.setUploaded(1).setFileName(files[0].getOriginalFilename()).setUrl(filePath + "/hub/" +  response.getData());
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
    @ResponseBody
    public void downloadFile(String fileName, HttpServletRequest request, HttpServletResponse response) {
        String path = request.getServletContext().getRealPath("/hub/") + fileName;
        logger.info("Downloading file: {}", path);
        
        try (InputStream is = new BufferedInputStream(new FileInputStream(path));
             OutputStream os = response.getOutputStream()) {
            
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            response.setContentType("application/octet-stream");
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (IOException e) {
            logger.error("Failed to download file: {}", fileName, e);
        }
    }
    
    /**
     * 根据文件名预览文件
     * @param fileName
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = {"/previewFile", "/previewFile.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse previewFile(String fileName, HttpServletRequest request, HttpServletResponse response) {
    
        String path = request.getServletContext().getRealPath("/hub/") + fileName;
        logger.info("Previewing file: {}", path);
        
        String convertByFile = SubmitPost("http://dcs.yozosoft.com:80/upload", path, "1");
        try {
            JsonNode obj = objectMapper.readTree(convertByFile);
            if ("0".equals(obj.path("result").asText())) {
                String urlData = extractPreviewUrl(obj);
                logger.info("Preview URL: {}", urlData);
                return ServerResponse.createBySuccess("预览文件正在打开，请稍等！", urlData);
            }
        } catch (Exception e) {
            logger.error("Preview service response parse failed for file: {}", fileName, e);
            return ServerResponse.createByErrorMessage("预览服务响应解析失败");
        }

        logger.warn("File conversion failed, file may be too large: {}", fileName);
        return ServerResponse.createByErrorMessage("文档过大，打开失败");
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
    public ServerResponse modifyFileKindWithUserNum(FileHub fileHub, @RequestParam("userNumAdmin") String userNumAdmin,
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
    public ServerResponse getFileListByAdminWithKind(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
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
    public ServerResponse downloadFileWithItemNum(@RequestParam("itemNum") String itemNum, HttpServletRequest request,
                                                  HttpServletResponse response) {

        if (fileAdminService.getFileWithItemNum(itemNum).getStatus() != 0) {
            return ServerResponse.createByErrorMessage("对不起，你还没有上传项目附件呢!请关闭窗口再重试！");
        }
        
        FileHub fileHub = (FileHub) fileAdminService.getFileWithItemNum(itemNum).getData();
        String fileName = fileHub.getFileName();
        String path = request.getServletContext().getRealPath("/hub/") + fileName;
        logger.info("Downloading file by itemNum: {} -> {}", itemNum, path);
        
        try (InputStream is = new BufferedInputStream(new FileInputStream(path));
             OutputStream os = response.getOutputStream()) {
            
            String encodedFileName = URLEncoder.encode(fileHub.getFileName(), "UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            response.setContentType("application/octet-stream");
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (IOException e) {
            logger.error("Failed to download file with itemNum: {}", itemNum, e);
            return ServerResponse.createByErrorMessage("文件下载失败：" + e.getMessage());
        }
        
        return ServerResponse.createBySuccess();
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
    @RequestMapping(value = {"/onekeyDownloadFile", "/onekeyDownloadFile.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse onekeyDownloadFile(@RequestParam("userNum") String userNum, @RequestParam("roleId") int roleId,
                                             HttpServletRequest request, String[] fileNames, HttpServletResponse response) {

        String zipBasePath = request.getServletContext().getRealPath("/hub/");
        String zipName = System.currentTimeMillis() + ".zip";
        String zipFilePath = zipBasePath + File.separator + zipName;

        List<String> filePaths = new ArrayList<>();
        for (String fileName : fileNames) {
            filePaths.add(zipBasePath + File.separator + fileName);
        }
        logger.info("Packaging {} files into {}", filePaths.size(), zipName);

        try {
            File zip = new File(zipFilePath);
            //noinspection ResultOfMethodCallIgnored
            zip.createNewFile();

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
                zipFile(filePaths, zos);
            }

            response.setHeader("Content-disposition", "attachment;filename=" + zipName);
            response.setContentType("application/octet-stream");

            try (InputStream is = new BufferedInputStream(new FileInputStream(zipFilePath));
                 OutputStream os = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
                os.flush();
            }
        } catch (Exception e) {
            logger.error("Failed to package and download files", e);
            return ServerResponse.createByErrorMessage("文件打包下载失败：" + e.getMessage());
        } finally {
            // 清理临时 zip 文件
            try {
                Files.deleteIfExists(new File(zipFilePath).toPath());
            } catch (IOException e) {
                logger.warn("Failed to delete temporary zip file: {}", zipFilePath, e);
            }
        }

        return ServerResponse.createBySuccess("文件已打包下载成功!");
    }
    
    /**
     * 将文件列表压缩到已打开的 ZipOutputStream 中
     *
     * @param filePaths 需要压缩的文件路径集合
     * @param zos       已打开的 ZipOutputStream
     */
    private void zipFile(List<String> filePaths, ZipOutputStream zos) {
        
        for (String filePath : filePaths) {
            File inputFile = new File(filePath);
            if (!inputFile.exists()) {
                logger.warn("File not found for zipping: {}", filePath);
                continue;
            }

            if (inputFile.isFile()) {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile))) {
                    zos.putNextEntry(new ZipEntry(inputFile.getName()));
                    byte[] buffer = new byte[8192];
                    int size;
                    while ((size = bis.read(buffer)) > 0) {
                        zos.write(buffer, 0, size);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    logger.error("Failed to add file to zip: {}", filePath, e);
                }
            } else {
                // 如果是文件夹，递归处理
                File[] files = inputFile.listFiles();
                if (files != null) {
                    List<String> subFilePaths = new ArrayList<>();
                    for (File subFile : files) {
                        subFilePaths.add(subFile.getAbsolutePath());
                    }
                    zipFile(subFilePaths, zos);
                }
            }
        }
    }
    
}
