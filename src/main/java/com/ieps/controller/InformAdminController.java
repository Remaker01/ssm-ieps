package com.ieps.controller;

import com.github.pagehelper.PageInfo;
import com.ieps.common.ServerResponse;
import com.ieps.pojo.Inform;
import com.ieps.pojo.User;
import com.ieps.service.InformService;
import com.ieps.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by ljw
 */
@Controller
public class InformAdminController {
    
    @Autowired
    private InformService informService;
    
    @Autowired
    private UserInfoService userInfoService;
    
    /**
     * 根据userNum获取通知公告
     * @param userNum
     * @return
     */
    @RequestMapping({"/getInformByUserNum", "/getInformByUserNum.do"})
    @ResponseBody
    public ServerResponse<List<Inform>> getInformByUserNum(String userNum) {
        return informService.getInformByUserNum(userNum);
    }
    
    /**
     * 获取全部的通知公告列表并分页显示
     * @param page
     * @param limit
     * @param session
     * @param userNum
     * @param roleId
     * @return
     */
    @RequestMapping({"/getAllInformList", "/getAllInformList.do"})
    @ResponseBody
    public ServerResponse<PageInfo<Inform>> getAllInformList(@RequestParam(value = "page",defaultValue = "1") int page,
                                 @RequestParam(value = "limit",defaultValue = "10") int limit,
                                 @RequestParam(value = "userNum", required = false) String userNum, @RequestParam("roleId") Integer roleId) {
    
        return informService.getAllInformList(page, limit, userNum, roleId);
        
    }
    
    /**
     * 模糊搜索文件并分页显示
     * @param page
     * @param limit
     * @param session
     * @param userNum
     * @param roleId
     * @param inform
     * @return
     */
    @RequestMapping({"/searchInformListWithCondition", "/searchInformListWithCondition.do"})
    @ResponseBody
    public ServerResponse<PageInfo<Inform>> searchInformListWithCondition(@RequestParam(value = "page", defaultValue = "1") int page,
                                                         @RequestParam(value = "limit", defaultValue = "5") int limit,
                                                         @RequestParam("userNum") String userNum, @RequestParam("roleId") Integer roleId,
                                                         Inform inform) {
    
        return informService.searchInformListWithCondition(page, limit, userNum, roleId, inform);
    }
    
    /**
     * 根据id删除通知公告
     * @param userNum
     * @param id
     * @param roleId
     * @param session
     * @return
     */
    @RequestMapping(value = {"/removeInformById", "/removeInformById.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> removeInformById(@RequestParam("userNum") String userNum, @RequestParam("id") Integer id,
                                           @RequestParam("roleId") Integer roleId) {
        
        return informService.removeInformById(id, roleId);
    }
    
    /**
     * 通过id修改通知公告
     * @param userNum
     * @param inform
     * @param session
     * @return
     */
    @RequestMapping(value = {"/modifyInformById", "/modifyInformById.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> modifyInformById(@RequestParam("userNum") String userNum, Inform inform) {
        
        return informService.modifyInformById(inform);
    }
    
    /**
     * 发布通知公告
     * @param userNum
     * @param roleId
     * @param inform
     * @param session
     * @return
     */
    @RequestMapping(value = {"/addInform", "/addInform.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> addInform(@RequestParam("userNum") String userNum, @RequestParam("roleId") Integer roleId,
                                    Inform inform) {
        
        inform.setPublisher(userNum);
        inform.setRoleId(roleId);
        inform.setSubject(userInfoService.findByUserNum(userNum).getData().getAcademy());
        
        return informService.addInform(inform);
    }
    
    /**
     * 批量删除通知公告
     * @param ids
     * @param roleId
     * @param userNum
     * @param session
     * @return
     */
    @RequestMapping({"/batchRemoveInform", "/batchRemoveInform.do"})
    @ResponseBody
    public ServerResponse<String> batchRemoveInform(@RequestParam("ids") Integer[] ids, @RequestParam("roleId") Integer roleId,
                                            @RequestParam("userNum") String userNum) {
        
        return informService.batchRemoveInform(ids, roleId);
    }
    
    /**
     * 根据userNum获取通知公告列表
     * @param pageNum
     * @param pageSize
     * @param session
     * @param head
     * @param pubdate
     * @return
     */
    @RequestMapping(value = {"/getInformListByAdminWithUserNum", "/getInformListByAdminWithUserNum.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo<Inform>> getInformListByAdminWithUserNum(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                                          @RequestParam(value = "pageSize",defaultValue = "7") int pageSize,
                                                          String head, String pubdate) {
        return informService.getInformListByAdmin(pageNum, pageSize, head, pubdate);
    }
    
    /**
     * 根据标题模糊查找通知公告
     * @param id
     * @return
     */
    @RequestMapping(value = {"/goInformListWithHead", "/goInformListWithHead.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Inform> goInformListWithHead(Integer id) {
        return informService.getInformListByAdminWithHead(id);
    }
    
    /**
     * 通过id获取通知公告详情
     * @param id
     * @return
     */
    @RequestMapping(value = {"/getInformDetailById", "/getInformDetailById.do"}, method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<Inform> getInformDetailById(Integer id) {
        return informService.getInformDetailById(id);
    }
    
}
