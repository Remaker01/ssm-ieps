package com.ieps.controller;

import com.ieps.common.ServerResponse;
import com.ieps.service.ItemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 前端运行时元数据接口
 */
@Controller
public class MetaController {

    @Autowired
    private ItemAdminService itemAdminService;

    @RequestMapping(value = "/meta/item-options", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getItemOptions() {
        return itemAdminService.getItemOptions();
    }
}
