package com.ieps.controller;

import com.ieps.common.ServerResponse;
import com.ieps.dto.UserAdminDto;
import com.ieps.pojo.Item;
import com.ieps.service.ShowAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by ljw
 */
@Controller
public class ShowAdminController {

    @Autowired
    private ShowAdminService showAdminService;
    
    /**
     * 各学院历届的参赛人数
     * @param itemDate
     * @return
     */
    @RequestMapping({"/getAcademyStuSex", "/getAcademyStuSex.do"})
    @ResponseBody
    public ServerResponse<List<UserAdminDto>> getAcademyStuSex(String itemDate) {
    
        return showAdminService.getAcademyStuSex(itemDate);
    }
    
    /**
     * 每年的参赛项目数量
     * @return
     */
    @RequestMapping({"/getItemsWithYear", "/getItemsWithYear.do"})
    @ResponseBody
    public ServerResponse<List<Item>> getItemsWithYear() {
        
        return showAdminService.getItemsWithYear();
    }
    
    
    
}
