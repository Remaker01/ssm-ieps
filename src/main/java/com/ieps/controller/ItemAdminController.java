package com.ieps.controller;

import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.dto.ItemAdminDto;
import com.ieps.pojo.Review;
import com.ieps.pojo.User;
import com.ieps.pojo.UserInfo;
import com.ieps.service.ItemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by ljw
 */
@Controller
public class ItemAdminController {
    
    @Autowired
    private ItemAdminService itemAdminService;

    private User getCurrentUser(HttpServletRequest request) {
        return (User) request.getAttribute(Const.REQUEST_CURRENT_USER);
    }

    private String resolveCurrentUserNum(HttpServletRequest request, String fallbackUserNum) {
        User currentUser = getCurrentUser(request);
        if (currentUser != null && currentUser.getUserNum() != null && !currentUser.getUserNum().trim().isEmpty()) {
            return currentUser.getUserNum();
        }
        return fallbackUserNum;
    }

    private int resolveCurrentRoleId(HttpServletRequest request, Integer fallbackRoleId) {
        User currentUser = getCurrentUser(request);
        if (currentUser != null && currentUser.getRoleId() != null) {
            return currentUser.getRoleId();
        }
        return fallbackRoleId == null ? 0 : fallbackRoleId;
    }
    
    /**
     * 根据userNum获取项目，分页显示
     * @param page
     * @param itemAdminDto
     * @param limit
     * @param session
     * @param userNumAdmin
     * @param roleId
     * @param itemStatusF
     * @param itemStatusS
     * @param itemStatusT
     * @param itemStatusFF
     * @return
     */
    @RequestMapping({"/getItemListByUserNum", "/getItemListByUserNum.do"})
    @ResponseBody
    public ServerResponse<PageInfo<ItemAdminDto>> getItemListByUserNum(@RequestParam(value = "page", defaultValue = "1") int page, ItemAdminDto itemAdminDto,
                                               @RequestParam(value = "limit", defaultValue = "10") int limit,
                                               @RequestParam(value = "userNumAdmin", required = false) String userNumAdmin,
                                               @RequestParam(value = "roleId", required = false) Integer roleId,
                                               @RequestParam(value = "itemStatusF", required = false) Integer itemStatusF,
                                               @RequestParam(value = "itemStatusS", required = false) Integer itemStatusS,
                                               @RequestParam(value = "itemStatusT", required = false) Integer itemStatusT,
                                               @RequestParam(value = "itemStatusFF", required = false) Integer itemStatusFF, HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNumAdmin);
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);

        // <!-- 项目状态：1：申请中；2：立项评审；3：已立项；4：立项失败；5：中期检查; 6: 待结题；7：结题评审；8：结题成功；9：结题失败-->
    
        List<Integer> integerList = new ArrayList<>();
        if (itemStatusF != null) {
            integerList.add(itemStatusF);
        }
        if (itemStatusS != null) {
            integerList.add(itemStatusS);
        }
        if (itemStatusT != null) {
            integerList.add(itemStatusT);
        }
        if (itemStatusFF != null) {
            integerList.add(itemStatusFF);
        }
        
        return itemAdminService.getItemListByUserNum(page, limit, effectiveUserNum, effectiveRoleId, itemAdminDto, integerList);
    }
    
    /**
     * 根据userNum和itemStatus获取项目列表
     * @param page
     * @param itemAdminDto
     * @param limit
     * @param session
     * @param userNumAdmin
     * @param roleId
     * @return
     */
    @RequestMapping({"/getItemListByUserNumAndItemStatus", "/getItemListByUserNumAndItemStatus.do"})
    @ResponseBody
    public ServerResponse<PageInfo<ItemAdminDto>> getItemListByUserNumAndItemStatus(@RequestParam(value = "page", defaultValue = "1") int page, ItemAdminDto itemAdminDto,
                                               @RequestParam(value = "limit", defaultValue = "5") int limit,
                                               @RequestParam(value = "userNumAdmin", required = false) String userNumAdmin,
                                               @RequestParam(value = "roleId", required = false) Integer roleId, HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNumAdmin);
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.getItemListByUserNumAndItemStatus(page, limit, effectiveUserNum, effectiveRoleId, itemAdminDto);
    }
    
    /**
     * 根据项目编号删除项目
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNum
     * @return
     */
    @RequestMapping({"/removeItemById", "/removeItemById.do"})
    @ResponseBody
    public ServerResponse<String> removeItemById(@RequestParam(value = "userNum", required = false) String userNum,
                                         @RequestParam(value = "roleId", required = false) Integer roleId,
                                         HttpServletRequest request, String itemNum) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.removeItemByItemNum(effectiveRoleId, itemNum);
    }
    
    /**
     * 根据项目编号组，批量删除项目
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNums
     * @return
     */
    @RequestMapping({"/batchRemoveItem", "/batchRemoveItem.do"})
    @ResponseBody
    public ServerResponse<String> batchRemoveItem(@RequestParam(value = "userNum", required = false) String userNum,
                                          @RequestParam(value = "roleId", required = false) Integer roleId,
                                          HttpServletRequest request, String[] itemNums) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.batchRemoveItemByItemNums(effectiveRoleId, itemNums);
    }
    
    /**
     * 根据项目编号 修改项目级别
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNum
     * @param itemlevel
     * @return
     */
    @RequestMapping({"/modifyItemLevelByItemNum", "/modifyItemLevelByItemNum.do"})
    @ResponseBody
    public ServerResponse<Integer> modifyItemLevelByItemNum(@RequestParam(value = "userNum", required = false) String userNum,
                                                   @RequestParam(value = "roleId", required = false) Integer roleId,
                                          HttpServletRequest request, String itemNum, @RequestParam("itemLevel") int itemlevel) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.modifyItemLevelByItemNum(effectiveRoleId, itemNum, itemlevel);
    }
    
    /**
     * 根据项目编号 修改项目类型
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNum
     * @param itemType
     * @return
     */
    @RequestMapping({"/modifyItemTypeByItemNum", "/modifyItemTypeByItemNum.do"})
    @ResponseBody
    public ServerResponse<Integer> modifyItemTypeByItemNum(@RequestParam(value = "userNum", required = false) String userNum,
                                                   @RequestParam(value = "roleId", required = false) Integer roleId,
                                                   HttpServletRequest request, String itemNum, @RequestParam("itemType") int itemType) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.modifyItemTypeByItemNum(effectiveRoleId, itemNum, itemType);
    }
    
    /**
     * 根据项目编号 修改项目状态
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNum
     * @param itemStatus
     * @return
     */
    @RequestMapping({"/modifyItemStatusByItemNum", "/modifyItemStatusByItemNum.do"})
    @ResponseBody
    public ServerResponse<Integer> modifyItemStatusByItemNum(@RequestParam(value = "userNum", required = false) String userNum,
                                                  @RequestParam(value = "roleId", required = false) Integer roleId,
                                                  HttpServletRequest request, String itemNum, @RequestParam("itemStatus") int itemStatus) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.modifyItemStatusByItemNum(effectiveRoleId, itemNum, itemStatus);
    }
    
    /**
     * 根据项目编号 修改项目信息
     * @param userNum
     * @param roleId
     * @param session
     * @param itemAdminDto
     * @return
     */
    @RequestMapping({"/modifyItemByItemNum", "/modifyItemByItemNum.do"})
    @ResponseBody
    public ServerResponse<String> modifyItemByItemNum(@RequestParam(value = "userNum", required = false) String userNum,
                                              @RequestParam(value = "roleId", required = false) Integer roleId,
                                                    HttpServletRequest request, ItemAdminDto itemAdminDto) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.modifyItemByItemNum(effectiveRoleId, itemAdminDto);
    }
    
    /**
     * 根据项目编号组一键修改项目类型
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNums
     * @param itemType
     * @return
     */
    @RequestMapping({"/onekeyModifyItemTypeWithItemNums", "/onekeyModifyItemTypeWithItemNums.do"})
    @ResponseBody
    public ServerResponse<String> onekeyModifyItemTypeWithItemNums(@RequestParam(value = "userNum", required = false) String userNum,
                                                           @RequestParam(value = "roleId", required = false) Integer roleId,
                                              HttpServletRequest request, String[] itemNums, int itemType) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.onekeyModifyItemTypeWithItemNums(effectiveRoleId, itemType, itemNums);
    }
    
    /**
     * 根据项目编号组一键修改项目状态
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNums
     * @param itemStatus
     * @return
     */
    @RequestMapping({"/onekeyModifyItemStatusWithItemNums", "/onekeyModifyItemStatusWithItemNums.do"})
    @ResponseBody
    public ServerResponse<String> onekeyModifyItemStatusWithItemNums(@RequestParam(value = "userNum", required = false) String userNum,
                                                             @RequestParam(value = "roleId", required = false) Integer roleId,
                                                           HttpServletRequest request, String[] itemNums, Integer itemStatus) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.onekeyModifyItemStatusWithItemNums(effectiveRoleId, itemStatus, itemNums);
    }
    
    /**
     * 根据项目编号组一键修改项目级别
     * @param userNum
     * @param roleId
     * @param session
     * @param itemNums
     * @param itemLevel
     * @return
     */
    @RequestMapping({"/onekeyModifyItemLevelWithItemNums", "/onekeyModifyItemLevelWithItemNums.do"})
    @ResponseBody
    public ServerResponse<String> onekeyModifyItemLevelWithItemNums(@RequestParam(value = "userNum", required = false) String userNum,
                                                            @RequestParam(value = "roleId", required = false) Integer roleId,
                                                           HttpServletRequest request, String[] itemNums, int itemLevel) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNum)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.onekeyModifyItemLevelWithItemNums(effectiveRoleId, itemLevel, itemNums);
    }
    
    /**
     * 添加项目和用户信息   立即申请项目
     * @param userNum
     * @param roleId
     * @param session
     * @param userNums
     * @param userNames
     * @param itemAdminDto
     * @return
     */
    @RequestMapping({"/addItemAndUserInfo", "/addItemAndUserInfo.do"})
    @ResponseBody
    public ServerResponse<String> addItemAndUserInfo(@RequestParam(value = "userNum", required = false) String userNum,
                                             @RequestParam(value = "roleId", required = false) Integer roleId, HttpServletRequest request,
                                             @RequestParam("userNums") String[] userNums, @RequestParam("userNames") String[] userNames, ItemAdminDto itemAdminDto) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);

        if (effectiveRoleId != Const.ROLEID_STU && effectiveRoleId != Const.ROLEID_ACADEMY && effectiveRoleId != Const.ROLEID_COLLEGE) {
            return ServerResponse.createByErrorMessage("对不起，目前你暂时没有权限提交项目申请！");
        }

        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.addItemAndUserInfo(itemAdminDto, userNums, userNames);
    }
    
    /**
     * 根据roleId和userNum获取评审小组组员
     * @param userNum
     * @param roleId
     * @param session
     * @return
     */
    @RequestMapping({"/getReviewLeaderByRoleIdAndUserNum", "/getReviewLeaderByRoleIdAndUserNum.do"})
    @ResponseBody
    public ServerResponse<List<UserInfo>> getReviewLeaderByRoleIdAndUserNum(@RequestParam(value = "userNum", required = false) String userNum,
                                                            @RequestParam(value = "roleId", required = false) Integer roleId,
                                                            HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNum);
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.getReviewLeaderByRoleIdAndUserNum(effectiveUserNum, effectiveRoleId);
    }
    
    /**
     * 指派评委
     * @param userNumAdmin
     * @param roleId
     * @param userNum
     * @param userName
     * @param itemNum
     * @param itemStatus
     * @param session
     * @return
     */
    @RequestMapping({"/chooseUserItemWithLeader", "/chooseUserItemWithLeader.do"})
    @ResponseBody
    public ServerResponse<String> chooseUserItemWithLeader(@RequestParam(value = "userNumAdmin", required = false) String userNumAdmin,
                                                   @RequestParam(value = "roleId", required = false) Integer roleId,
                                                   @RequestParam("userNum") String userNum, @RequestParam("userName") String userName,
                                                   @RequestParam("itemNum") String itemNum, @RequestParam("itemStatus") int itemStatus,
                                                   HttpServletRequest request) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.addUserItemWithLeader(effectiveRoleId, userNum, userName, itemNum, itemStatus);
    }
    
    
    @RequestMapping(value = {"/batchAddReviewTeam", "/batchAddReviewTeam.do"}, method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> batchAddReviewTeam(@RequestParam(value = "userNumAdmin", required = false) String userNumAdmin,
                                             @RequestParam(value = "roleId", required = false) Integer roleId,
                                             @RequestParam("userNum") String userNum, @RequestParam("userName") String userName,
                                             @RequestParam("itemNums") String[] itemNums, @RequestParam("itemStatus") int itemStatus,
                                             HttpServletRequest request) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
    
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
    
        return itemAdminService.batchAddReviewTeam(effectiveRoleId, userNum, userName, itemNums, itemStatus);
    }
    
    /**
     * 专家提交评审结果
     * @param roleId
     * @param review
     * @param session
     * @return
     */
    @RequestMapping({"/addReviewByExport", "/addReviewByExport.do"})
    @ResponseBody
    public ServerResponse<String> addReviewByExport(@RequestParam(value = "roleId", required = false) Integer roleId, Review review, HttpServletRequest request) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        
        // if (!user.getUserNum().equals(userNumAdmin)) {
        //     return ServerResponse.createByErrorMessage("安全检查不通过，用户已过时或不存在！");
        // }
        
        return itemAdminService.addReviewByExport(effectiveRoleId, review);
    }
    
    // 首页中的项目展览
    
    /**
     * 首页中历届项目展示，已完成的
     * @param pageNum
     * @param pageSize
     * @param session
     * @param itemName
     * @param itemDate
     * @return
     */
    @RequestMapping({"/getFinishedItemList", "/getFinishedItemList.do"})
    @ResponseBody
    public ServerResponse<PageInfo<ItemAdminDto>> getFinishedItemList(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                              @RequestParam(value = "pageSize",defaultValue = "10") int pageSize,
                                              String itemName, String itemDate) {
        return itemAdminService.getFinishedItem(pageNum, pageSize, itemName, itemDate);
    }
    
    /**
     * 根据ItemNum和userNum检查项目是否评审
     * @param itemNum
     * @param userNum
     * @return
     */
    @RequestMapping({"/checkIsReviewWithItemNumAndUserNum", "/checkIsReviewWithItemNumAndUserNum.do"})
    @ResponseBody
    public ServerResponse<?> checkIsReviewWithItemNumAndUserNum(String itemNum,
                                                             @RequestParam(value = "userNum", required = false) String userNum,
                                                             int reviewLevel, HttpServletRequest request) {
        String effectiveUserNum = resolveCurrentUserNum(request, userNum);
        return itemAdminService.checkIsReviewWithItemNum(itemNum, effectiveUserNum, reviewLevel);
    }
    
    /**
     * 根据itemNum获取项目状态
     * @param itemNum
     * @return
     */
    @RequestMapping({"/getItemStatusWithItemNum", "/getItemStatusWithItemNum.do"})
    @ResponseBody
    public ServerResponse<ItemAdminDto> getItemStatusWithItemNum(String itemNum) {
        return itemAdminService.getItemStatusWithItemNum(itemNum);
    }
    
    /**
     * 获取历届项目年份
     * @return
     */
    @RequestMapping({"/getItemDate", "/getItemDate.do"})
    @ResponseBody
    public ServerResponse<List<String>> getItemDate() {
        return itemAdminService.getItemDate();
    }
    
    /**
     * 根据itemNum获取项目详情
     * @param itemNum
     * @return
     */
    @RequestMapping({"/getItemDetailWithItemNum", "/getItemDetailWithItemNum.do"})
    @ResponseBody
    public ServerResponse<ItemAdminDto> getItemDetailWithItemNum(String itemNum) {
        return itemAdminService.getItemDetailWithItemId(itemNum);
    }
    
    
    @RequestMapping({"/removeUserItemWithReview", "/removeUserItemWithReview.do"})
    @ResponseBody
    public ServerResponse<?> removeUserItemWithReview(String itemNum,
                                                   @RequestParam(value = "roleId", required = false) Integer roleId,
                                                   HttpServletRequest request) {
        int effectiveRoleId = resolveCurrentRoleId(request, roleId);
        return itemAdminService.removeUserItemWithReview(itemNum, effectiveRoleId);
    }
    
    
    
    
    
    
    
    
    
    
}
