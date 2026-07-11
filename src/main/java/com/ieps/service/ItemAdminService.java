package com.ieps.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.dto.ItemAdminDto;
import com.ieps.mapper.*;
import com.ieps.pojo.Review;
import com.ieps.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ljw
 */
@Service
public class ItemAdminService {
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private ItemInfoMapper itemInfoMapper;
    
    @Autowired
    private UserItemMapper userItemMapper;
    
    @Autowired
    private UserInfoMapper userInfoMapper;
    
    @Autowired
    private ReviewMapper reviewMapper;
    
    @Autowired
    private FileHubMapper fileHubMapper;

    public ServerResponse<Map<String, Object>> getItemOptions() {
        Map<String, String> itemLevel = Map.of(
                "1", "无",
                "2", "校级",
                "3", "省区级",
                "4", "国家级"
        );

        Map<String, String> itemType = Map.of("1", "创新训练", "2", "创业训练", "3", "创业实践");

        Map<String, String> itemStatus = Map.of(
                "1", "申请中",
                "2", "立项评审",
                "3", "已立项",
                "4", "立项失败",
                "5", "中期检查",
                "6", "待结题",
                "7", "结题评审",
                "8", "结题成功",
                "9", "结题失败"
        );

        Map<String, Object> result = Map.of("itemLevel", itemLevel, "itemType", itemType, "itemStatus", itemStatus);
        return ServerResponse.createBySuccess(result);
    }
    
    public ServerResponse<PageInfo<ItemAdminDto>> getItemListByUserNum(int pageNum, int pageSize, String userNumAdmin, int roleId,
                                               ItemAdminDto itemAdminDto, List<Integer> integerList) {
        PageHelper.startPage(pageNum, pageSize);
        List<ItemAdminDto> itemAdminDtoList = new ArrayList<>();
        if (Const.USERNUM_COLLEGE.equals(userNumAdmin)) {
            itemAdminDtoList = itemMapper.selectAllItem(itemAdminDto, integerList);
        } else if (Const.ROLEID_ACADEMY == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByAdminWithUserNum(userNumAdmin, itemAdminDto, integerList);
        } else if (Const.ROLEID_COLLEGE_EXPERT == roleId || Const.ROLEID_ACADEMY_EXPERT == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByExportWithUserNum(userNumAdmin, itemAdminDto, integerList);
        } else if (Const.ROLEID_TUTOR == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByTutorWithUserNum(userNumAdmin, itemAdminDto, integerList);
        } else if (Const.ROLEID_STU == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByStuWithUserNum(userNumAdmin, itemAdminDto, integerList);
        }
        PageInfo<ItemAdminDto> pageInfo = new PageInfo<>(itemAdminDtoList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse<PageInfo<ItemAdminDto>> getItemListByUserNumAndItemStatus(int pageNum, int pageSize, String userNumAdmin, int roleId, ItemAdminDto itemAdminDto) {
        PageHelper.startPage(pageNum, pageSize);
        List<ItemAdminDto> itemAdminDtoList = new ArrayList<>();
        if (Const.USERNUM_COLLEGE.equals(userNumAdmin)) {
            itemAdminDtoList = itemMapper.selectAllItemWithItemStatus(itemAdminDto);
        } else if (Const.ROLEID_ACADEMY == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByAdminWithUserNumAndItemStatus(userNumAdmin, itemAdminDto);
        } else if (Const.ROLEID_COLLEGE_EXPERT == roleId || Const.ROLEID_ACADEMY_EXPERT == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByExportWithUserNumAndItemStatus(userNumAdmin, itemAdminDto);
        } else if (Const.ROLEID_TUTOR == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByTutorWithUserNumAndItemStatus(userNumAdmin, itemAdminDto);
        } else if (Const.ROLEID_STU == roleId) {
            itemAdminDtoList = itemMapper.selectAllItemByStuWithUserNumAndItemStatus(userNumAdmin, itemAdminDto);
        }
        PageInfo<ItemAdminDto> pageInfo = new PageInfo<>(itemAdminDtoList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse<String> removeItemByItemNum(int roleId, String itemNum) {
        if (Const.ROLEID_ACADEMY == roleId || Const.ROLEID_COLLEGE == roleId) {
            String[] itemNums = new String[1];
            itemNums[0] = itemNum;
            int result = itemMapper.deleteItemByItemNums(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除Item表中项目编号：" + itemNum + "失败");
            }
            result = itemInfoMapper.deleteItemInfoByItemNums(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除ItemInfo表中项目编号：" + itemNum + "失败");
            }
            result = userItemMapper.deleteUserItemByItemNums(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除UserItem表中项目编号：" + itemNum + "失败");
            }
            result = fileHubMapper.deleteFileHubByItemNum(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除FileHub表中项目编号：" + itemNum + "失败");
            }
            return ServerResponse.createBySuccessMessage("删除项目编号：" + itemNum + "及其关联表信息成功");
        }
        return ServerResponse.createByErrorMessage("对不起，目前你暂时没有权限执行删除操作！");
    }
    
    public ServerResponse<String> batchRemoveItemByItemNums(int roleId, String[] itemNums) {
        if (Const.ROLEID_ACADEMY == roleId || Const.ROLEID_COLLEGE == roleId) {
            int result = itemMapper.deleteItemByItemNums(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除Item表失败");
            }
            result = itemInfoMapper.deleteItemInfoByItemNums(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除ItemInfo表失败");
            }
            result = userItemMapper.deleteUserItemByItemNums(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除UserItem表失败");
            }
            result = fileHubMapper.deleteFileHubByItemNum(itemNums);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("删除FileHub表失败");
            }
            return ServerResponse.createBySuccessMessage("批量删除项目成功！");
        }
        return ServerResponse.createByErrorMessage("对不起，目前你暂时没有权限执行删除操作！");
    }
    
    public ServerResponse<Integer> modifyItemLevelByItemNum(int roleId, String itemNum, int itemLevel) {
        if (Const.ROLEID_COLLEGE != roleId) {
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限修改项目的级别！");
        }
        return ServerResponse.createBySuccess("恭喜你，修改项目编号：" + itemNum + "的级别成功！", itemInfoMapper.updateItemLevelByItemNum(itemNum, itemLevel));
    }
    
    public ServerResponse<Integer> modifyItemTypeByItemNum(int roleId, String itemNum, int itemType) {
        if (Const.ROLEID_COLLEGE != roleId && Const.ROLEID_ACADEMY != roleId) {
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限修改项目的类型！");
        }
        return ServerResponse.createBySuccess("恭喜你，修改项目编号：" + itemNum + "的类型成功！", itemInfoMapper.updateItemTypeByItemNum(itemNum, itemType));
    }
    
    public ServerResponse<Integer> modifyItemStatusByItemNum(int roleId, String itemNum, int itemStatus) {
        if (Const.ROLEID_COLLEGE != roleId && Const.ROLEID_ACADEMY != roleId) {
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限修改项目的状态！");
        }
        return ServerResponse.createBySuccess("恭喜你，修改项目编号：" + itemNum + "的状态成功！", itemMapper.updateItemStatusByItemNum(itemNum, itemStatus));
    }
    
    public ServerResponse<String> modifyItemByItemNum(int roleId, ItemAdminDto itemAdminDto) {
        int result = 0;
        ItemAdminDto itemAdminDtoTmp = itemMapper.selectItemByItemNum(itemAdminDto.getItemNum());
        if (itemAdminDtoTmp.getLeaderName() != itemAdminDto.getLeaderName() && itemAdminDtoTmp.getTutorName() != itemAdminDto.getTutorName()) {
            UserInfo userInfoLeader = userInfoMapper.selectUserInfoByUserName(itemAdminDto.getLeaderName());
            UserInfo userInfoTutor = userInfoMapper.selectUserInfoByUserName(itemAdminDto.getLeaderName());
            itemAdminDto.setLeaderNum(userInfoLeader.getUserNum());
            itemAdminDto.setTutorNum(userInfoTutor.getUserNum());
            result = itemMapper.updateItemByItemNum(itemAdminDto);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，修改项目Item表信息错误！");
            }
            result = itemInfoMapper.updateItemByItemNum(itemAdminDto);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，修改项目ItemInfo表信息错误！");
            }
            result = userItemMapper.updateUserNumByItemNumAndUserNum(itemAdminDto.getItemNum(), itemAdminDtoTmp.getLeaderNum());
            result = userItemMapper.updateUserNumByItemNumAndUserNum(itemAdminDto.getItemNum(), itemAdminDtoTmp.getTutorNum());
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，修改项目UserItem表信息错误！");
            }
        } else {
            result = itemInfoMapper.updateItemByItemNum(itemAdminDto);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，修改项目ItemInfo表信息错误！");
            }
        }
        return ServerResponse.createBySuccessMessage("恭喜你，修改项目信息成功！");
    }
    
    public ServerResponse<String> onekeyModifyItemTypeWithItemNums(int roleId, int itemType, String[] itemNums) {
        if (Const.ROLEID_COLLEGE != roleId && Const.ROLEID_ACADEMY != roleId) {
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限一键修改项目类型！");
        }
        if (itemInfoMapper.updateItemTypeWithItemNums(itemType, itemNums) <= 0) {
            return ServerResponse.createByErrorMessage("对不起，一键修改项目类型错误，请重试！");
        }
        return ServerResponse.createBySuccessMessage("恭喜你，一键修改项目类型成功！");
    }
    
    public ServerResponse<String> onekeyModifyItemStatusWithItemNums(int roleId, int itemStatus, String[] itemNums) {
        if (Const.ROLEID_COLLEGE != roleId && Const.ROLEID_ACADEMY != roleId) {
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限一键修改项目状态！");
        }
        if (itemMapper.updateItemStatusWithItemNums(itemStatus, itemNums) <= 0) {
            return ServerResponse.createByErrorMessage("对不起，一键修改项目状态错误，请重试！");
        }
        return ServerResponse.createBySuccessMessage("恭喜你，一键修改项目状态成功！");
    }
    
    public ServerResponse<String> onekeyModifyItemLevelWithItemNums(int roleId, int itemLevel, String[] itemNums) {
        if (Const.ROLEID_COLLEGE != roleId && Const.ROLEID_ACADEMY != roleId) {
            return ServerResponse.createByErrorMessage("对不起，目前你没有权限一键修改项目级别！");
        }
        if (itemInfoMapper.updateItemLevelWithItemNums(itemLevel, itemNums) <= 0) {
            return ServerResponse.createByErrorMessage("对不起，一键修改项目级别错误，请重试！");
        }
        return ServerResponse.createBySuccessMessage("恭喜你，一键修改项目级别成功！");
    }
    
    public ServerResponse<String> addItemAndUserInfo(ItemAdminDto itemAdminDto, String[] userNums, String[] userNams) {
        itemAdminDto.setItemStatus(1);
        int result = itemMapper.insertItemWithItemAdminDto(itemAdminDto);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("申请表Item插入失败");
        }
        result = itemInfoMapper.insertItemInfoWithSummary(itemAdminDto.getItemNum(), itemAdminDto.getItemType(), itemAdminDto.getSummary());
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("申请表ItemInfo插入失败");
        }
        result = userItemMapper.insertUserItemWithItemNumAndUserNum(itemAdminDto.getLeaderNum(), itemAdminDto.getItemNum(), 2);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("申请表UserItem插入失败");
        }
        result = userItemMapper.insertUserItemWithItemNumAndUserNum(itemAdminDto.getTutorNum(), itemAdminDto.getItemNum(), 3);
        for (int i = 0; i < userNums.length; i++) {
            result = userItemMapper.insertUserItemWithItemNumAndUserNum(userNums[i], itemAdminDto.getItemNum(), 1);
        }
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("申请表UserItem插入失败");
        }
        return ServerResponse.createBySuccessMessage("插入申请表信息成功！");
    }
    
    public ServerResponse<List<UserInfo>> getReviewLeaderByRoleIdAndUserNum(String userNum, int roleId) {
        if (roleId == Const.ROLEID_ACADEMY || roleId == Const.ROLEID_COLLEGE) {
            UserInfo userInfo = userInfoMapper.selectByUserNum(userNum);
            if (userInfo == null) {
                return ServerResponse.createByErrorMessage("对不起，可能网络出错了，暂时无法找到你的信息！");
            }
            List<UserInfo> userInfoList = userInfoMapper.selectUserInfoByAcademy(userInfo.getAcademy());
            if (userInfoList.size() == 0) {
                return ServerResponse.createByErrorMessage("对不起，你所在单位没有院评审组，请先创建评审组！");
            }
            return ServerResponse.createBySuccess(userInfoList);
        }
        return ServerResponse.createByErrorMessage("对不起，目前你还没有权限指派评审！");
    }
    
    public ServerResponse<String> addUserItemWithLeader(int roleId, String userNum, String userName, String itemNum, Integer itemStatus) {
        if (roleId == Const.ROLEID_ACADEMY) {
            String userNameTemp = userName.substring(0, userName.lastIndexOf("组员"));
            List<UserInfo> userInfoList = userInfoMapper.selectUserInfoLikeUserName(userNum, userNameTemp);
            if (userInfoList == null) {
                return ServerResponse.createByErrorMessage("对不起，可能网络出错了，暂时无法找到你的信息！");
            }
            int result = userItemMapper.insertUserItemWithItemNumAndUserNum(userNum, itemNum, 5);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，选择学院评审组长数据错误！");
            }
            for (UserInfo userInfo : userInfoList) {
                result = userItemMapper.insertUserItemWithItemNumAndUserNum(userInfo.getUserNum(), itemNum, 4);
            }
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，选择学院评审委员数据错误！");
            }
            result = itemMapper.updateItemStatusByItemNum(itemNum, itemStatus);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，更新项目状态数据错误！");
            }
        } else if (roleId == Const.ROLEID_COLLEGE) {
            String userNameTemp = userName.substring(0, userName.lastIndexOf("组员"));
            List<UserInfo> userInfoList = userInfoMapper.selectUserInfoLikeUserName(userNum, userNameTemp);
            if (userInfoList == null) {
                return ServerResponse.createByErrorMessage("对不起，可能网络出错了，暂时无法找到你的信息！");
            }
            int result = userItemMapper.insertUserItemWithItemNumAndUserNum(userNum, itemNum, 7);
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，选择学校评审组长数据错误！");
            }
            for (UserInfo userInfo : userInfoList) {
                result = userItemMapper.insertUserItemWithItemNumAndUserNum(userInfo.getUserNum(), itemNum, 6);
            }
            if (result <= 0) {
                return ServerResponse.createByErrorMessage("对不起，选择学校评审委员数据错误！");
            }
        }
        return ServerResponse.createBySuccessMessage("恭喜您，选择评审组长及其组员成功！");
    }
    
    public ServerResponse<String> batchAddReviewTeam(int roleId, String userNum, String userName, String[] itemNums, int itemStatus) {
        if (roleId == Const.ROLEID_ACADEMY) {
            String userNameTemp = userName.substring(0, userName.lastIndexOf("组员"));
            List<UserInfo> userInfoList = userInfoMapper.selectUserInfoLikeUserName(userNum, userNameTemp);
            if (userInfoList == null) {
                return ServerResponse.createByErrorMessage("对不起，可能网络出错了，暂时无法找到你的信息！");
            }
            for (int i = 0; i < itemNums.length; i++) {
                int result = userItemMapper.insertUserItemWithItemNumAndUserNum(userNum, itemNums[i], 5);
                if (result <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，选择学院评审组长数据错误！");
                }
                for (UserInfo userInfo : userInfoList) {
                    result = userItemMapper.insertUserItemWithItemNumAndUserNum(userInfo.getUserNum(), itemNums[i], 4);
                }
                if (result <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，选择学院评审委员数据错误！");
                }
                result = itemMapper.updateItemStatusByItemNum(itemNums[i], itemStatus);
                if (result <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，更新项目状态数据错误！");
                }
            }
        } else if (roleId == Const.ROLEID_COLLEGE) {
            String userNameTemp = userName.substring(0, userName.lastIndexOf("组员"));
            List<UserInfo> userInfoList = userInfoMapper.selectUserInfoLikeUserName(userNum, userNameTemp);
            if (userInfoList == null) {
                return ServerResponse.createByErrorMessage("对不起，可能网络出错了，暂时无法找到你的信息！");
            }
            for (int i = 0; i < itemNums.length; i++) {
                int result = userItemMapper.insertUserItemWithItemNumAndUserNum(userNum, itemNums[i], 7);
                if (result <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，选择学校评审组长数据错误！");
                }
                for (UserInfo userInfo : userInfoList) {
                    result = userItemMapper.insertUserItemWithItemNumAndUserNum(userInfo.getUserNum(), itemNums[i], 6);
                }
                if (result <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，选择学校评审委员数据错误！");
                }
            }
        }
        return ServerResponse.createBySuccessMessage("恭喜您，批量指派评审组长及其组员成功！");
    }
    
    public ServerResponse<String> addReviewByExport(int roleId, Review review) {
        if (Const.ROLEID_ACADEMY_EXPERT == roleId || Const.ROLEID_COLLEGE_EXPERT == roleId) {
            if (reviewMapper.insert(review) <= 0) {
                return ServerResponse.createByErrorMessage("对不起，插入评审结果失败，请重新填写！");
            }
            return ServerResponse.createBySuccessMessage("恭喜你，插入评审结果成功，请继续操作！");
        }
        return ServerResponse.createByErrorMessage("对不起，目前你没有权限进行评审操作！");
    }
    
    public ServerResponse<PageInfo<ItemAdminDto>> getFinishedItem(int pageNum, int pageSize, String itemName, String itemDate) {
        PageHelper.startPage(pageNum, pageSize);
        List<ItemAdminDto> itemAdminDtoList = itemMapper.selectFinishedItemWithItemStatus("1", itemName, itemDate);
        if (itemAdminDtoList.size() == 0) {
            return ServerResponse.createByErrorMessage("暂无数据！");
        }
        PageInfo<ItemAdminDto> pageInfo = new PageInfo<>(itemAdminDtoList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse<?> checkIsReviewWithItemNum(String itemNum, String userNum, int reviewLevel) {
        if (userNum == null || userNum.trim().isEmpty()) {
            return ServerResponse.createByErrorMessage("登录状态已失效，请重新登录后再试！");
        }
        int result = userItemMapper.selectUserItemWithItemNumAndUserNum(itemNum, userNum, reviewLevel);
        if (result <= 0) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("对不起，不能再重复评审同状态的同一项目，请及时上传评审结果！");
    }
    
    public ServerResponse<ItemAdminDto> getItemStatusWithItemNum(String itemNum) {
        ItemAdminDto itemAdminDto = itemMapper.selectItemByItemNum(itemNum);
        if (itemAdminDto == null) {
            return ServerResponse.createByErrorMessage("对不起哦，该项目不能被修改状态！");
        }
        return ServerResponse.createBySuccess(itemAdminDto);
    }
    
    public ServerResponse<List<String>> getItemDate() {
        List<String> itemDateList = itemMapper.selectAllItemDate();
        if (itemDateList.size() == 0) {
            return ServerResponse.createByError();
        }
        return ServerResponse.createBySuccess(itemDateList);
    }
    
    public ServerResponse<ItemAdminDto> getItemDetailWithItemId(String itemNum) {
        ItemAdminDto itemAdminDto = itemMapper.selectItemDetailWithItemId(itemNum);
        return ServerResponse.createBySuccess(itemAdminDto);
    }
    
    public ServerResponse<?> removeUserItemWithReview(String itemNum, int roleId) {
        if (roleId != Const.ROLEID_COLLEGE && roleId != Const.ROLEID_ACADEMY) {
            return ServerResponse.createByErrorMessage("对不起，身份验证失败！");
        }
        int result = userItemMapper.deleteUsetItemWithReviews(itemNum);
        if (result <= 0) {
            return ServerResponse.createByError();
        }
        return ServerResponse.createBySuccess();
    }
    
}
