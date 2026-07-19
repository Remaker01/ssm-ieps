package com.ieps.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.mapper.InformMapper;
import com.ieps.mapper.UserInfoMapper;
import com.ieps.pojo.Inform;
import com.ieps.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by ljw
 */
@Service
public class InformService {
    
    @Autowired
    private InformMapper informMapper;
    
    @Autowired
    private UserInfoMapper userInfoMapper;
    
    public ServerResponse<List<Inform>> getInformByUserNum(String userNum) {
        List<Inform> informList = informMapper.selectByUserNum(userNum);
        if (informList.size() == 0) {
            return ServerResponse.createByErrorMessage("目前该用户尚未发布任何通知，请重新输入查找！");
        }
        return ServerResponse.createBySuccess(informList);
    }
    
    public ServerResponse<PageInfo<Inform>> getAllInformList(int pageNum, int pageSize, String userNum, Integer roleId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Inform> informList = new ArrayList<>();
        if (roleId == Const.ROLEID_COLLEGE) {
            informList = informMapper.selectAll();
        } else {
            UserInfo userInfo = userInfoMapper.selectByUserNum(userNum);
            List<String> subjectList = List.of(userInfo.getAcademy(), Const.ACADEMY_COLLEGE);
            informList = informMapper.selectAllInformList(subjectList);
        }
        PageInfo<Inform> pageInfo = new PageInfo<>(informList);
        pageInfo.setList(informList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse<PageInfo<Inform>> searchInformListWithCondition(int pageNum, int pageSize, String userNum, Integer roleId, Inform inform) {
        UserInfo userInfo = userInfoMapper.selectByUserNum(userNum);
        PageHelper.startPage(pageNum, pageSize);
        List<Inform> informList = new ArrayList<>();
        if (roleId == Const.ROLEID_COLLEGE) {
            informList = informMapper.selectAllWithCondition(inform);
        } else {
            List<String> subjectList = List.of(userInfo.getAcademy(), Const.ACADEMY_COLLEGE);
            informList = informMapper.selectAllInformListWithCondition(subjectList, inform);
        }
        PageInfo<Inform> pageInfo = new PageInfo<>(informList);
        pageInfo.setList(informList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    @CacheEvict(value = "ieps-inform", allEntries = true)
    public ServerResponse<String> removeInformById(Integer id, Integer roleId) {
        if (roleId < informMapper.selectByPrimaryKey(id).getRoleId()) {
            return ServerResponse.createByErrorMessage("对不起，目前你还没有权限执行该行的删除操作！");
        }
        int result = informMapper.deleteByPrimaryKey(id);
        if (result == 0) {
            return ServerResponse.createByErrorMessage("删除当前行：id" + id + " 失败!");
        }
        return ServerResponse.createBySuccessMessage("删除当前行：id" + id + " 成功!");
    }
    
    @CacheEvict(value = "ieps-inform", allEntries = true)
    public ServerResponse<String> modifyInformById(Inform inform) {
        int result = informMapper.updateByPrimaryKeySelective(inform);
        if (result == 0) {
            return ServerResponse.createByErrorMessage("更新当前行：id" + inform.getId() + " 失败!");
        }
        return ServerResponse.createBySuccessMessage("更新当前行：id" + inform.getId() + " 成功!");
    }
    
    @CacheEvict(value = "ieps-inform", allEntries = true)
    public ServerResponse<String> addInform(Inform inform) {
        int result = informMapper.insert(inform);
        if (result == 0) {
            return ServerResponse.createByErrorMessage("插入数据失败，请重新填写!");
        }
        return ServerResponse.createBySuccessMessage("插入数据成功!");
    }
    
    @CacheEvict(value = "ieps-inform", allEntries = true)
    public ServerResponse<String> batchRemoveInform(Integer[] ids, Integer roleId) {
        for (int i = 0; i < ids.length; i++) {
            if (roleId < informMapper.selectByPrimaryKey(ids[i]).getRoleId()) {
                return ServerResponse.createByErrorMessage("对不起，目前你还没有权限执行该行的删除操作！");
            }
        }
        if (informMapper.deleteInformByIds(ids) < 1) {
            return ServerResponse.createByErrorMessage("删除通知公告失败!");
        }
        return ServerResponse.createBySuccessMessage("删除通知公告成功!");
    }
    
    public ServerResponse<PageInfo<Inform>> getInformListByAdmin(int pageNum, int pageSize, String head, String pubdate) {
        PageHelper.startPage(pageNum, pageSize);
        List<Inform> informList = informMapper.selectAllInformByAdminWithUserNum(Const.USERNUM_COLLEGE, head, pubdate);
        if (informList.size() == 0) {
            return ServerResponse.createByErrorMessage("没有通知，数据为空！");
        }
        PageInfo<Inform> pageInfo = new PageInfo<>(informList);
        pageInfo.setList(informList);
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse<Inform> getInformListByAdminWithHead(Integer id) {
        Inform inform = informMapper.selectByPrimaryKey(id);
        if (inform == null) {
            return ServerResponse.createByErrorMessage("");
        }
        return ServerResponse.createBySuccess(inform);
    }
    
    @Cacheable(value = "ieps-inform", key = "#id")
    public ServerResponse<Inform> getInformDetailById(Integer id) {
        Inform inform = informMapper.selectByPrimaryKey(id);
        if (inform == null) {
            return ServerResponse.createByErrorMessage("");
        }
        return ServerResponse.createBySuccess(inform);
    }
    
}
