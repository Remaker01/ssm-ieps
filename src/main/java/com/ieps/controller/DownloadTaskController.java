package com.ieps.controller;

import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.dto.DownloadTaskDto;
import com.ieps.pojo.User;
import com.ieps.service.DownloadTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class DownloadTaskController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadTaskController.class);

    @Autowired
    private DownloadTaskService downloadTaskService;

    @RequestMapping(value = {"/downloadTasks", "/onekeyDownloadFile", "/onekeyDownloadFile.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<DownloadTaskDto> createDownloadTask(@RequestParam("fileNames") String[] fileNames,
                                                              HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        return downloadTaskService.createDownloadTask(currentUser, fileNames);
    }

    @RequestMapping(value = "/downloadTasks/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<DownloadTaskDto> getDownloadTask(@PathVariable("taskId") String taskId,
                                                           HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        return downloadTaskService.getDownloadTask(currentUser, taskId);
    }

    @RequestMapping(value = "/downloadTasks/{taskId}/download", method = RequestMethod.GET)
    public void downloadTaskArchive(@PathVariable("taskId") String taskId,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        ServerResponse<String> result = downloadTaskService.resolveDownloadUrl(currentUser, taskId);
        if (result.getStatus() != 0 || result.getData() == null) {
            logger.warn("Reject async task archive download. taskId={}, requestUser={}, message={}",
                    taskId, currentUser == null ? "anonymous" : currentUser.getUserNum(), result.getMsg());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, result.getMsg());
            return;
        }
        response.sendRedirect(result.getData());
    }
}
