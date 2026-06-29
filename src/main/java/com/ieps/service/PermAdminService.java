package com.ieps.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.mapper.PermMapper;
import com.ieps.mapper.UserRoleMapper;
import com.ieps.pojo.Perm;
import com.ieps.pojo.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by ljw
 */
@Service
public class PermAdminService {
    
    @Autowired
    private PermMapper permMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    public ServerResponse getPermList(int pageNum, int pageSize, Integer roleId, Perm perm) {
        
        PageHelper.startPage(pageNum, pageSize);
        
        List<Perm> permList = permMapper.selectAllPerm();
        
        if (permList.size() == 0) {
            return ServerResponse.createByErrorMessage("加载菜单出错，请重试！");
        }
        
        PageInfo pageInfo = new PageInfo(permList);
        pageInfo.setList(permList);
        
        return ServerResponse.createBySuccess(pageInfo);
    }
    
    public ServerResponse getPerm() {
        List<Perm> permList = permMapper.selectAllPerm();
        
        if (permList.size() == 0) {
            return ServerResponse.createByErrorMessage("加载菜单出错，请重试！");
        }
        
        return ServerResponse.createBySuccess(permList);
    }
    
    public ServerResponse removePermById(int roleId, Perm perm) {
        if (roleId == Const.ROLEID_COLLEGE) {
            if (permMapper.deletePermById(perm.getPermId(), perm.getPermType()) > 0) {
                return ServerResponse.createBySuccessMessage("删除" + perm.getPermId() + "成功");
            } else {
                return ServerResponse.createByErrorMessage("删除permId：" + perm.getParentId() + "失败!");
            }
        }
        
        return ServerResponse.createByErrorMessage("对不起，你没有权限操作！");
        
    }
    
    public ServerResponse<List<Perm>> getAllMenu() {
        return ServerResponse.createBySuccess(permMapper.selectAllMenu());
    }
    
    public ServerResponse getPermByPermId(Integer permId) {
        Perm perm = permMapper.selectByPrimaryKey(permId);
        if (perm == null) {
            return ServerResponse.createByErrorMessage("查找父节点信息失败，请重新尝试！");
        }
        
        return ServerResponse.createBySuccess(perm);
    }
    
    public ServerResponse modifyPermById(Integer roleId, Perm perm) {
        if (roleId == Const.ROLEID_COLLEGE) {
            if (permMapper.updateByPrimaryKeySelective(perm) > 0) {
                return ServerResponse.createBySuccess();
            } else {
                return ServerResponse.createByErrorMessage("修改permId：" + perm.getPermId() + "失败!");
            }
        }
        
        return ServerResponse.createByErrorMessage("对不起，你没有权限操作！");
        
    }
    
    public ServerResponse addPerm(Integer roleId, Perm perm) {
        
        System.out.println(perm);
        
        perm.setPermDesc(perm.getPermName());
        
        if (roleId == Const.ROLEID_COLLEGE) {
            
            if (perm.getIcon() == "" || perm.getIcon() == null) {
                perm.setIcon("&#xe63c;");
            }
            
            if (permMapper.insertSelective(perm) > 0) {
                return ServerResponse.createBySuccess();
            } else {
                return ServerResponse.createByErrorMessage("新增permId：" + perm.getPermId() + "失败!");
            }
        }
        
        return ServerResponse.createByErrorMessage("对不起，你没有权限操作！");
    }
    
    public ServerResponse batchRemovePerm(Integer roleId, Integer[] permIds) {
        if (roleId == Const.ROLEID_COLLEGE) {
            if (permMapper.batchDeletePerm(permIds) > 0) {
                return ServerResponse.createBySuccess();
            } else {
                return ServerResponse.createByErrorMessage("对不起，新增失败，请重新尝试！");
            }
        }
        
        return ServerResponse.createByErrorMessage("对不起，你没有权限操作！");
    }
    
    public ServerResponse addRolePerm(Integer[] permIds, Integer roleId, int roleAdminId) {
        
        if (roleAdminId == Const.ROLEID_COLLEGE) {
            
            for (int i = 0; i < permIds.length; i++) {
                if (permMapper.insertRolePerm(permIds[i], roleId) <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，新增角色权限关联失败，请重新尝试！");
                }
            }
            
            return ServerResponse.createBySuccess();
        }
        
        return ServerResponse.createByErrorMessage("对不起，你没有权限操作！");
        
    }
    
    public ServerResponse addRolePermWithUserNum(Integer[] permIds, Integer roleId, String userNum) {
        
        if (roleId == Const.ROLEID_COLLEGE) {
            
            UserRole userRole = userRoleMapper.selectRoleWithUserNum(userNum);
            if (userRole == null) {
                return ServerResponse.createByErrorMessage("该用户没有角色，请马上分配角色后再试！");
            }
            
            for (int i = 0; i < permIds.length; i++) {
                if (permMapper.insertRolePerm(permIds[i], roleId) <= 0) {
                    return ServerResponse.createByErrorMessage("对不起，新增角色权限关联失败，请重新尝试！");
                }
            }
            
            return ServerResponse.createBySuccess();
        }
        
        return ServerResponse.createByErrorMessage("对不起，你没有权限操作！");
        
    }
}
