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

/**
 * 异步下载任务控制器
 *
 * <p><b>功能概述：</b>提供异步文件打包下载的 REST API。前端选中多个文件后一键打包，
 * 后端异步处理 ZIP 压缩（避免长时间阻塞 HTTP 请求），前端轮询任务状态后获取临时下载链接。</p>
 *
 * <p><b>异步下载流程（三步）：</b></p>
 * <ol>
 *   <li><b>创建任务</b> → POST {@code /downloadTasks}：接收文件列表，创建下载任务记录，
 *       立即返回任务 ID 和初始状态（PENDING），同时触发 {@link DownloadTaskWorkerService} 异步执行</li>
 *   <li><b>轮询状态</b> → GET {@code /downloadTasks/{taskId}}：前端每隔数秒轮询此接口，
 *       直至返回 SUCCESS（已完成）或 FAILED（打包失败）</li>
 *   <li><b>下载文件</b> → GET {@code /downloadTasks/{taskId}/download}：重定向到 COS 预签名 URL
 *       （有时效，默认 5 分钟）直接下载 ZIP 包</li>
 * </ol>
 *
 * <p><b>兼容历史路由：</b>{@code /onekeyDownloadFile} 和 {@code /onekeyDownloadFile.do}
 * 保留为旧版前端调用的兼容别名。</p>
 */
@Controller
public class DownloadTaskController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadTaskController.class);

    @Autowired
    private DownloadTaskService downloadTaskService;

    /**
     * 【步骤1】创建异步下载任务
     *
     * <p>接收前端传入的文件名列表，创建 {@link com.ieps.pojo.DownloadTask} 记录（状态 PENDING），
     * 然后异步启动 ZIP 打包流程。立即返回任务信息（含 taskId），前端据此进行后续轮询。</p>
     *
     * @param fileNames 需要打包下载的文件名数组
     * @param request   HTTP 请求（从中获取当前登录用户）
     * @return 包含任务 ID 和初始状态的任务 DTO
     */
    @RequestMapping(value = {"/downloadTasks", "/onekeyDownloadFile", "/onekeyDownloadFile.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<DownloadTaskDto> createDownloadTask(@RequestParam("fileNames") String[] fileNames,
                                                              HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        return downloadTaskService.createDownloadTask(currentUser, fileNames);
    }

    /**
     * 【步骤2】查询下载任务状态（供前端轮询）
     *
     * <p>前端每隔数秒调用此接口轮询打包进度。可能的状态：</p>
     * <ul>
     *   <li>{@code pending} — 等待执行</li>
     *   <li>{@code running} — 正在打包</li>
     *   <li>{@code success} — 打包完成，可下载（DTO 中包含 downloadUrl）</li>
     *   <li>{@code failed} — 打包失败（DTO 中包含 errorMessage）</li>
     *   <li>{@code expired} — 下载链接已过期</li>
     * </ul>
     *
     * @param taskId  任务 ID（由 createDownloadTask 返回）
     * @param request HTTP 请求
     * @return 当前任务状态及可下载信息
     */
    @RequestMapping(value = "/downloadTasks/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<DownloadTaskDto> getDownloadTask(@PathVariable("taskId") String taskId,
                                                           HttpServletRequest request) {
        User currentUser = (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
        return downloadTaskService.getDownloadTask(currentUser, taskId);
    }

    /**
     * 【步骤3】重定向到 COS 预签名下载 URL
     *
     * <p>前端轮询到 SUCCESS 状态后，调用此接口获取真正的下载地址。
     * 后端生成有时效（默认 5 分钟）的 COS 预签名 URL，通过 302 重定向方式引导浏览器下载。</p>
     *
     * <p>不直接返回 URL 字符串而是重定向，是为了避免前端额外处理下载逻辑，
     * 同时也隐藏了 COS 地址细节。</p>
     *
     * @param taskId  任务 ID
     * @param request HTTP 请求
     * @param response HTTP 响应（302 重定向到 COS 预签名 URL）
     */
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
